package web;

import java.sql.SQLException;
import java.util.*;

/** Módulo CRUD genérico: define tabla, campos del formulario y columnas del listado. */
public class Mod {
    record Campo(String col, String etiqueta, String tipo, boolean req, String[] opciones, String refSql, String defecto, boolean sens) {}
    record Col(String clave, String etiqueta, String formato, boolean sens) {}

    final String id, titulo, tabla, pk, listaSql;
    final List<Campo> campos = new ArrayList<>();
    final List<Col> cols = new ArrayList<>();
    final Map<String, String> defectoSql = new HashMap<>();
    java.util.function.Consumer<Map<String, String>> antes = null;

    Mod(String id, String titulo, String tabla, String pk, String listaSql) {
        this.id = id; this.titulo = titulo; this.tabla = tabla; this.pk = pk; this.listaSql = listaSql;
    }

    Mod txt(String c, String e, boolean r) { campos.add(new Campo(c, e, "text", r, null, null, null, false)); return this; }
    Mod area(String c, String e, boolean r) { campos.add(new Campo(c, e, "area", r, null, null, null, false)); return this; }
    Mod num(String c, String e, boolean r, String def) { campos.add(new Campo(c, e, "number", r, null, null, def, false)); return this; }
    Mod fecha(String c, String e) { campos.add(new Campo(c, e, "date", false, null, null, null, false)); return this; }
    Mod sel(String c, String e, boolean r, String def, String... op) { campos.add(new Campo(c, e, "sel", r, op, null, def, false)); return this; }
    Mod ref(String c, String e, boolean r, String sql) { campos.add(new Campo(c, e, "ref", r, null, sql, null, false)); return this; }
    Mod ent(String c, String e, boolean r, String def) { campos.add(new Campo(c, e, "int", r, null, null, def, false)); return this; }
    Mod col(String k, String e, String f) { cols.add(new Col(k, e, f, false)); return this; }
    /** Marca el último campo y la última columna como sensibles (solo el superusuario los ve y edita). */
    Mod sens() {
        Campo c = campos.remove(campos.size() - 1);
        campos.add(new Campo(c.col(), c.etiqueta(), c.tipo(), c.req(), c.opciones(), c.refSql(), c.defecto(), true));
        return this;
    }
    Mod sensCol() { Col c = cols.remove(cols.size() - 1); cols.add(new Col(c.clave(), c.etiqueta(), c.formato(), true)); return this; }
    Mod defSql(String col, String sql) { defectoSql.put(col, sql); return this; }
    Mod antes(java.util.function.Consumer<Map<String, String>> a) { antes = a; return this; }
    List<Campo> vis(Auth.Sesion s) { List<Campo> l = new ArrayList<>(); for (Campo c : campos) if (s.esSuper() || !c.sens()) l.add(c); return l; }
    static boolean hay(String v) { return v != null && !v.isBlank(); }

    static final String REF_CLIENTES = "SELECT id_cliente, nombre FROM clientes ORDER BY nombre";
    static final String[] DOC = {"CC", "CE", "TI", "PAS"};

    static final Map<String, Mod> TODOS = new LinkedHashMap<>();
    static {
        add(new Mod("clientes", "Clientes", "clientes", "id_cliente", "SELECT * FROM clientes ORDER BY nombre")
            .txt("nombre", "Nombre o razón social", true)
            .sel("tipo_cliente", "Tipo de cliente", false, null, "publica", "privada", "persona_natural", "consorcio_union_temporal")
            .sel("tipo_documento", "Tipo de documento", true, "NIT", "NIT", "CC")
            .txt("numero_documento", "Número (sin puntos ni dígito de verificación)", false).txt("digito_verificacion", "Dígito de verificación", false)
            .txt("telefono", "Teléfono", false).txt("correo", "Correo", false).txt("direccion", "Dirección", false)
            .col("nombre", "Nombre", "t").col("tipo_cliente", "Tipo", "t").col("tipo_documento", "Doc.", "t")
            .col("numero_documento", "Número", "t").col("digito_verificacion", "DV", "t").col("telefono", "Teléfono", "t").col("correo", "Correo", "t"));

        add(new Mod("proyectos", "Proyectos (propuestas)", "proyectos", "id_proyecto",
            "SELECT p.*, c.nombre AS cliente, ct.codigo_interno AS contrato FROM proyectos p LEFT JOIN clientes c ON c.id_cliente=p.id_cliente "
            + "LEFT JOIN contratos ct ON ct.id_proyecto=p.id_proyecto ORDER BY p.id_proyecto DESC")
            .txt("codigo", "Código", false).txt("nombre", "Nombre del proyecto", true).area("descripcion", "Descripción", false)
            .sel("tipo_proyecto", "Tipo", true, "formal", "formal", "independiente")
            .ref("id_cliente", "Cliente", false, REF_CLIENTES)
            .fecha("fecha_presentacion", "Fecha de presentación").txt("ubicacion", "Ubicación", false)
            .sel("estado", "Estado", true, "propuesta", "propuesta", "ganado", "no_ganado", "en_ejecucion", "terminado")
            .defSql("codigo", "SELECT 'PRY-' || printf('%04d', COALESCE(MAX(CAST(substr(codigo,5) AS INTEGER)),0)+1) FROM proyectos WHERE codigo GLOB 'PRY-[0-9]*'")
            .col("codigo", "Código", "t").col("nombre", "Nombre", "t").col("tipo_proyecto", "Tipo", "t")
            .col("cliente", "Cliente", "t").col("estado", "Estado", "t").col("fecha_presentacion", "Presentación", "t").col("contrato", "Contrato", "t"));

        add(new Mod("contratos", "Contratos", "contratos", "id_contrato",
            "SELECT c.*, cl.nombre AS cliente, v.valor_smmlv_participacion AS smmlv, "
            + "(SELECT GROUP_CONCAT(cu.codigo, ', ') FROM contrato_codigos cc JOIN codigos_compra cu ON cu.id_codigo=cc.id_codigo WHERE cc.id_contrato=c.id_contrato) AS codigos "
            + "FROM contratos c JOIN clientes cl ON cl.id_cliente=c.id_cliente LEFT JOIN v_contratos v ON v.id_contrato=c.id_contrato ORDER BY c.id_contrato")
            .txt("codigo_interno", "Código interno", true).txt("numero_contrato", "Número del contrato (si lo tiene)", false)
            .ent("consecutivo_rup", "Consecutivo RUP", false, null)
            .ref("id_cliente", "Cliente", true, REF_CLIENTES)
            .ref("id_proyecto", "Proyecto de origen", false, "SELECT id_proyecto, COALESCE(codigo,'') || ' - ' || nombre FROM proyectos ORDER BY codigo")
            .txt("tipo_contrato", "Tipo de contrato (libre)", false).area("objeto", "Objeto del contrato", true)
            .txt("figura_contratista", "Contratista / figura (consorcio, unión temporal)", false)
            .num("porcentaje_participacion", "Participación (0 a 1; 1 = 100 %)", true, "1")
            .num("valor_contrato", "Valor del contrato (vacío = se calcula del proyecto de origen)", false, null)
            .fecha("fecha_inicio", "Fecha de inicio").fecha("fecha_terminacion", "Fecha de terminación")
            .txt("ubicacion_obra", "Ubicación de la obra", false)
            .sel("estado", "Estado", true, "en_ejecucion", "en_ejecucion", "terminado")
            .defSql("codigo_interno", "SELECT '5CINCO-' || printf('%04d', COALESCE(MAX(CAST(substr(codigo_interno,8) AS INTEGER)),0)+1) FROM contratos")
            .antes(f -> {
                String pct = f.get("porcentaje_participacion");
                try {
                    double d = Double.parseDouble(pct == null ? "" : pct.trim());
                    if (d <= 0 || d > 1) throw new NumberFormatException();
                } catch (NumberFormatException e) { throw new IllegalArgumentException("La participación debe ser mayor que 0 y máximo 1."); }
                if (!hay(f.get("valor_contrato")) && hay(f.get("id_proyecto"))) {
                    try {
                        Map<String, Object> r = Db.fila("SELECT total_con_iva FROM v_totales_proyecto WHERE id_proyecto=?", Long.parseLong(f.get("id_proyecto").trim()));
                        if (r != null && r.get("total_con_iva") != null) f.put("valor_contrato", String.valueOf(Math.round(((Number) r.get("total_con_iva")).doubleValue())));
                    } catch (SQLException | NumberFormatException e) { throw new IllegalArgumentException("No se pudo calcular el valor desde el proyecto."); }
                }
                if (!hay(f.get("valor_contrato")))
                    throw new IllegalArgumentException("Falta el valor del contrato: escríbelo o elige un proyecto de origen que tenga líneas en Calcular valores.");
            })
            .col("codigo_interno", "Código", "t").col("numero_contrato", "N.º contrato", "t").col("cliente", "Cliente", "t").col("objeto", "Objeto", "t")
            .col("valor_contrato", "Valor", "$").col("porcentaje_participacion", "Part.", "n")
            .col("valor_participacion", "Valor participación", "$").col("smmlv", "SMMLV", "n")
            .col("fecha_inicio", "Inicio", "t").col("fecha_terminacion", "Fin", "t").col("estado", "Estado", "t").col("codigos", "Códigos", "t"));

        add(new Mod("codigos", "Códigos de compra", "codigos_compra", "id_codigo",
            "SELECT cu.*, (SELECT COUNT(*) FROM contrato_codigos cc WHERE cc.id_codigo=cu.id_codigo) AS contratos FROM codigos_compra cu ORDER BY cu.codigo")
            .txt("codigo", "Código (UNSPSC, por ejemplo 72101500)", true).txt("descripcion", "Descripción", true)
            .sel("tipo", "Tipo (opcional)", false, null, "bien", "material", "servicio").txt("grupo", "Grupo (opcional)", false)
            .col("codigo", "Código", "t").col("descripcion", "Descripción", "t").col("tipo", "Tipo", "t").col("grupo", "Grupo", "t").col("contratos", "Contratos", "n"));

        add(new Mod("productos", "Productos", "productos", "id_producto",
            "SELECT p.*, c.codigo AS codigo_compra FROM productos p LEFT JOIN codigos_compra c ON c.id_codigo=p.id_codigo ORDER BY p.nombre")
            .txt("codigo", "Código", false).txt("nombre", "Nombre", true).area("descripcion", "Descripción", false)
            .sel("tipo", "Tipo", true, "mueble", "mueble", "material", "servicio")
            .txt("unidad_medida", "Unidad de medida (unidad, m², metro lineal…)", true).num("precio_referencia", "Precio de referencia", false, null)
            .ref("id_codigo", "Código de compra (se asigna solo a los contratos que lo usen)", false, "SELECT id_codigo, codigo || ' - ' || descripcion FROM codigos_compra ORDER BY codigo")
            .sel("estado", "Estado", true, "activo", "activo", "inactivo")
            .defSql("codigo", "SELECT 'PRD-' || printf('%04d', COALESCE(MAX(CAST(substr(codigo,5) AS INTEGER)),0)+1) FROM productos WHERE codigo GLOB 'PRD-[0-9]*'")
            .col("codigo", "Código", "t").col("nombre", "Nombre", "t").col("tipo", "Tipo", "t").col("unidad_medida", "Unidad", "t")
            .col("precio_referencia", "Precio ref.", "$").col("codigo_compra", "Cód. compra", "t").col("estado", "Estado", "t"));

        add(new Mod("proveedores", "Proveedores", "proveedores", "id_proveedor", "SELECT * FROM proveedores ORDER BY nombre")
            .txt("nombre", "Nombre o razón social", true).sel("tipo_documento", "Tipo de documento", true, "NIT", "NIT", "CC")
            .txt("numero_documento", "Número (sin puntos ni dígito de verificación)", true).txt("digito_verificacion", "Dígito de verificación", false)
            .txt("telefono", "Teléfono", false).txt("direccion", "Dirección", false).txt("ubicacion", "Ciudad o departamento", false)
            .sel("tipo_ubicacion", "La ubicación es", true, "ciudad", "ciudad", "departamento")
            .txt("correo", "Correo", false).txt("descripcion_suministro", "Qué suministra", false)
            .sel("estado", "Estado", true, "activo", "activo", "inactivo")
            .col("nombre", "Nombre", "t").col("tipo_documento", "Doc.", "t").col("numero_documento", "Número", "t").col("telefono", "Teléfono", "t")
            .col("ubicacion", "Ubicación", "t").col("descripcion_suministro", "Qué suministra", "t").col("estado", "Estado", "t"));

        add(new Mod("empleados", "Empleados", "empleados", "id_empleado", "SELECT * FROM empleados ORDER BY nombre")
            .txt("nombre", "Nombre completo", true).sel("tipo_documento", "Tipo de documento", true, "CC", DOC)
            .txt("numero_documento", "Número de documento", true).txt("cargo", "Cargo", false)
            .txt("telefono", "Teléfono", false).txt("correo", "Correo", false).txt("direccion", "Dirección", false)
            .fecha("fecha_ingreso", "Fecha de ingreso")
            .num("salario", "Salario", false, null).sens().txt("eps", "EPS", false).sens().txt("arl", "ARL", false).sens()
            .txt("fondo_pension", "Fondo de pensión", false).sens().txt("caja_compensacion", "Caja de compensación", false).sens()
            .sel("estado", "Estado", true, "activo", "activo", "inactivo")
            .col("nombre", "Nombre", "t").col("numero_documento", "Documento", "t").col("cargo", "Cargo", "t").col("telefono", "Teléfono", "t")
            .col("correo", "Correo", "t").col("fecha_ingreso", "Ingreso", "t")
            .col("salario", "Salario", "$").sensCol().col("eps", "EPS", "t").sensCol().col("arl", "ARL", "t").sensCol()
            .col("fondo_pension", "Pensión", "t").sensCol().col("caja_compensacion", "Caja de comp.", "t").sensCol().col("estado", "Estado", "t"));

        add(new Mod("contratistas", "Contratistas", "contratistas", "id_contratista", "SELECT * FROM contratistas ORDER BY nombre")
            .txt("nombre", "Nombre completo", true).sel("tipo_documento", "Tipo de documento", true, "CC", DOC)
            .txt("numero_documento", "Número de documento", true).txt("actividad", "Actividad", false)
            .txt("telefono", "Teléfono", false).txt("correo", "Correo", false)
            .sel("estado", "Estado", true, "activo", "activo", "inactivo")
            .col("nombre", "Nombre", "t").col("numero_documento", "Documento", "t").col("actividad", "Actividad", "t")
            .col("telefono", "Teléfono", "t").col("correo", "Correo", "t").col("estado", "Estado", "t"));

        add(new Mod("asignaciones", "Asignaciones", "asignaciones", "id_asignacion",
            "SELECT a.*, ct.codigo_interno AS contrato, cl.nombre AS cliente, COALESCE(e.nombre, c.nombre) AS persona, "
            + "CASE WHEN a.id_empleado IS NOT NULL THEN 'Empleado' ELSE 'Contratista' END AS vinculacion FROM asignaciones a "
            + "JOIN contratos ct ON ct.id_contrato=a.id_contrato JOIN clientes cl ON cl.id_cliente=ct.id_cliente "
            + "LEFT JOIN empleados e ON e.id_empleado=a.id_empleado LEFT JOIN contratistas c ON c.id_contratista=a.id_contratista ORDER BY a.id_asignacion DESC")
            .ref("id_contrato", "Contrato", true, "SELECT id_contrato, codigo_interno || ' - ' || substr(objeto,1,60) FROM contratos ORDER BY id_contrato")
            .ref("id_empleado", "Empleado (elige empleado O contratista)", false, "SELECT id_empleado, nombre FROM empleados ORDER BY nombre")
            .ref("id_contratista", "Contratista", false, "SELECT id_contratista, nombre FROM contratistas ORDER BY nombre")
            .txt("actividad", "Actividad", false).fecha("fecha_inicio", "Fecha de inicio").fecha("fecha_fin", "Fecha de fin")
            .num("valor_pagado", "Valor pagado (solo contratistas)", false, null)
            .antes(f -> {
                boolean e = hay(f.get("id_empleado")), c = hay(f.get("id_contratista"));
                if (e == c) throw new IllegalArgumentException("Debes elegir un empleado O un contratista (no ambos).");
                if (hay(f.get("valor_pagado")) && !c) throw new IllegalArgumentException("El valor pagado solo aplica a contratistas.");
            })
            .col("contrato", "Contrato", "t").col("cliente", "Cliente", "t").col("persona", "Persona", "t").col("vinculacion", "Vinculación", "t")
            .col("actividad", "Actividad", "t").col("fecha_inicio", "Inicio", "t").col("fecha_fin", "Fin", "t").col("valor_pagado", "Valor pagado", "$"));

        add(new Mod("salario", "Salario mínimo", "salario_minimo", "anio", "SELECT * FROM salario_minimo ORDER BY anio DESC")
            .ent("anio", "Año", true, null).num("valor", "Salario mínimo mensual (sin auxilio de transporte)", true, null)
            .antes(f -> { try { if (Double.parseDouble(f.getOrDefault("valor", "").trim()) <= 0) throw new NumberFormatException(); }
                          catch (NumberFormatException e) { throw new IllegalArgumentException("El valor del salario mínimo debe ser mayor que 0."); } })
            .col("anio", "Año", "t").col("valor", "Salario mínimo mensual", "$"));
    }

    private static void add(Mod m) { TODOS.put(m.id, m); }

    // ---------- Vistas ----------
    String valor(Map<String, Object> f, Col c) {
        Object v = f.get(c.clave());
        return switch (c.formato()) { case "$" -> Html.dinero(v); case "n" -> Html.numero(v); default -> Html.e(v); };
    }

    String lista(Auth.Sesion s, String q) throws SQLException {
        List<Map<String, Object>> filas = Db.consultar(listaSql);
        List<Col> vc = new ArrayList<>();
        for (Col c : cols) if (s.esSuper() || !c.sens()) vc.add(c);
        String qq = q == null ? "" : q.trim().toLowerCase();
        StringBuilder sb = new StringBuilder("<h1>" + Html.e(titulo) + "</h1><div class=\"barra\">")
            .append("<form method=\"get\" action=\"/m/").append(id).append("\"><input name=\"q\" placeholder=\"Buscar…\" value=\"").append(Html.e(q)).append("\">")
            .append("<button class=\"btn sec\">Buscar</button></form><a class=\"btn\" href=\"/m/").append(id).append("/nuevo\">+ Nuevo</a></div>")
            .append("<div class=\"tabla\"><table><tr>");
        for (Col c : vc) sb.append("<th>").append(Html.e(c.etiqueta())).append("</th>");
        sb.append("<th></th></tr>");
        int n = 0;
        for (Map<String, Object> f : filas) {
            if (!qq.isEmpty()) {
                boolean ok = false;
                for (Col c : vc) { Object v = f.get(c.clave()); if (v != null && v.toString().toLowerCase().contains(qq)) { ok = true; break; } }
                if (!ok) continue;
            }
            n++;
            sb.append("<tr>");
            for (Col c : vc) sb.append(c.formato().equals("t") ? "<td>" : "<td class=\"d\">").append(valor(f, c)).append("</td>");
            long idr = ((Number) f.get(pk)).longValue();
            sb.append("<td class=\"acc\">");
            if (id.equals("contratos"))
                sb.append("<a class=\"btn sec\" href=\"/m/contratos/").append(idr).append("/codigos\">Códigos</a> ");
            sb.append("<a class=\"btn sec\" href=\"/m/").append(id).append("/").append(idr).append("/editar\">Editar</a> ");
            if (s.esSuper())
                sb.append("<form method=\"post\" action=\"/m/").append(id).append("/").append(idr)
                  .append("/eliminar\" onsubmit=\"return confirm('¿Eliminar este registro?')\"><button class=\"btn pel\">Eliminar</button></form>");
            sb.append("</td></tr>");
        }
        sb.append("</table></div><p class=\"pie\">").append(n).append(" registros").append(s.esSuper() ? "" : " · Eliminar solo está disponible para el superusuario").append("</p>");
        if (!s.esSuper() && id.equals("empleados"))
            sb.append("<p class=\"pie\">Los datos sensibles (salario, EPS, ARL, pensión y caja de compensación) solo los ve el superusuario.</p>");
        return sb.toString();
    }

    String formulario(Auth.Sesion s, Map<String, Object> fila, String accion) throws SQLException {
        StringBuilder sb = new StringBuilder("<h1>" + (fila == null ? "Nuevo registro" : "Editar registro") + " · " + Html.e(titulo)
            + "</h1><form class=\"form\" method=\"post\" action=\"" + accion + "\">");
        for (Campo c : vis(s)) {
            Object v = fila == null ? c.defecto() : fila.get(c.col());
            if (fila == null && defectoSql.containsKey(c.col())) { Map<String, Object> d = Db.fila(defectoSql.get(c.col())); if (d != null) v = d.values().iterator().next(); }
            boolean ancho = c.tipo().equals("area");
            sb.append("<label").append(ancho ? " class=\"ancho\"" : "").append(">").append(Html.e(c.etiqueta())).append(c.req() ? " <span class=\"req\">*</span>" : "");
            String r = c.req() ? " required" : "";
            switch (c.tipo()) {
                case "area" -> sb.append("<textarea name=\"").append(c.col()).append("\" rows=\"3\"").append(r).append(">").append(Html.e(v)).append("</textarea>");
                case "number" -> sb.append("<input type=\"number\" step=\"any\" min=\"0\" name=\"").append(c.col()).append("\" value=\"").append(Html.e(v)).append("\"").append(r).append(">");
                case "int" -> sb.append("<input type=\"number\" step=\"1\" min=\"0\" name=\"").append(c.col()).append("\" value=\"").append(Html.e(v)).append("\"").append(r).append(">");
                case "date" -> sb.append("<input type=\"date\" name=\"").append(c.col()).append("\" value=\"").append(Html.e(v)).append("\">");
                case "sel" -> {
                    sb.append("<select name=\"").append(c.col()).append("\"").append(r).append(">");
                    if (!c.req()) sb.append("<option value=\"\"></option>");
                    for (String o : c.opciones()) sb.append("<option").append(o.equals(String.valueOf(v)) ? " selected" : "").append(">").append(Html.e(o)).append("</option>");
                    sb.append("</select>");
                }
                case "ref" -> {
                    sb.append("<select name=\"").append(c.col()).append("\"").append(r).append(">");
                    if (!c.req()) sb.append("<option value=\"\"></option>");
                    for (Map<String, Object> o : Db.consultar(c.refSql())) {
                        Iterator<Object> it = o.values().iterator();
                        Object k = it.next(), t = it.next();
                        sb.append("<option value=\"").append(Html.e(k)).append("\"").append(String.valueOf(k).equals(String.valueOf(v)) ? " selected" : "").append(">").append(Html.e(t)).append("</option>");
                    }
                    sb.append("</select>");
                }
                default -> sb.append("<input name=\"").append(c.col()).append("\" value=\"").append(Html.e(v)).append("\"").append(r).append(">");
            }
            sb.append("</label>");
        }
        sb.append("<div class=\"acciones\"><button class=\"btn\">Guardar</button><a class=\"btn sec\" href=\"/m/").append(id).append("\">Cancelar</a></div></form>");
        return sb.toString();
    }

    /** Valida y arma los parámetros; lanza IllegalArgumentException con un mensaje claro. */
    Object[] leer(Auth.Sesion s, Map<String, String> entrada) {
        Map<String, String> f = new HashMap<>(entrada);
        if (antes != null) antes.accept(f);
        List<Campo> cs = vis(s);
        Object[] p = new Object[cs.size()];
        for (int i = 0; i < p.length; i++) {
            Campo c = cs.get(i);
            String v = f.get(c.col());
            v = v == null ? "" : v.trim();
            if (v.isEmpty()) {
                if (c.req()) throw new IllegalArgumentException("Falta el campo obligatorio: " + c.etiqueta());
                p[i] = null; continue;
            }
            if (v.length() > 1000) throw new IllegalArgumentException("El campo " + c.etiqueta() + " es demasiado largo.");
            switch (c.tipo()) {
                case "number" -> {
                    try { double d = Double.parseDouble(v); if (d < 0 || Double.isNaN(d) || Double.isInfinite(d)) throw new NumberFormatException(); p[i] = d; }
                    catch (NumberFormatException e) { throw new IllegalArgumentException("Número no válido en " + c.etiqueta()); }
                }
                case "int" -> {
                    try { long d = Long.parseLong(v); if (d < 0) throw new NumberFormatException(); p[i] = d; }
                    catch (NumberFormatException e) { throw new IllegalArgumentException("Número entero no válido en " + c.etiqueta()); }
                }
                case "ref" -> {
                    try { p[i] = Long.parseLong(v); } catch (NumberFormatException e) { throw new IllegalArgumentException("Selección no válida en " + c.etiqueta()); }
                }
                case "sel" -> {
                    if (!Arrays.asList(c.opciones()).contains(v)) throw new IllegalArgumentException("Opción no válida en " + c.etiqueta());
                    p[i] = v;
                }
                case "date" -> {
                    if (!v.matches("\\d{4}-\\d{2}-\\d{2}")) throw new IllegalArgumentException("Fecha no válida en " + c.etiqueta());
                    p[i] = v;
                }
                default -> p[i] = v;
            }
        }
        return p;
    }

    void insertar(Auth.Sesion s, Map<String, String> f) throws SQLException {
        Object[] p = leer(s, f);
        StringJoiner cs = new StringJoiner(","), qs = new StringJoiner(",");
        for (Campo c : vis(s)) { cs.add(c.col()); qs.add("?"); }
        Db.ejecutar("INSERT INTO " + tabla + "(" + cs + ") VALUES(" + qs + ")", p);
    }

    void actualizar(Auth.Sesion s, long idr, Map<String, String> f) throws SQLException {
        Object[] p = leer(s, f);
        StringJoiner sj = new StringJoiner(",");
        for (Campo c : vis(s)) sj.add(c.col() + "=?");
        Object[] q = Arrays.copyOf(p, p.length + 1);
        q[p.length] = idr;
        Db.ejecutar("UPDATE " + tabla + " SET " + sj + " WHERE " + pk + "=?", q);
    }

    void eliminar(long idr) throws SQLException { Db.ejecutar("DELETE FROM " + tabla + " WHERE " + pk + "=?", idr); }
    Map<String, Object> buscar(long idr) throws SQLException { return Db.fila("SELECT * FROM " + tabla + " WHERE " + pk + "=?", idr); }
}
