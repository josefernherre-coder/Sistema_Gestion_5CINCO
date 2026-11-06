package web;

import java.sql.SQLException;
import java.util.*;

/** Calcular valores: líneas de un proyecto o contrato, con IVA y retenciones. */
public class Calc {
    static final String SEL = "SELECT d.id_detalle, d.id_producto, p.nombre AS producto, p.unidad_medida, d.cantidad, d.precio_unitario, d.subtotal, "
        + "d.descuento_pct, d.iva_pct, d.retefuente_pct, d.reteica_pct, d.reteiva_pct, v.descuento, v.base_gravable, v.iva, v.retefuente, v.reteica, v.reteiva "
        + "FROM detalle d JOIN productos p ON p.id_producto=d.id_producto JOIN v_detalle v ON v.id_detalle=d.id_detalle ";

    /** obj = "p:ID" (proyecto) o "c:ID" (contrato). */
    static boolean esContrato(String obj) { return obj != null && obj.startsWith("c:"); }
    static long idDe(String obj) { return Long.parseLong(obj.substring(2)); }
    static boolean valido(String obj) { return obj != null && obj.matches("[pc]:\\d{1,12}"); }

    static String pagina(String obj, String editar) throws SQLException {
        StringBuilder sb = new StringBuilder("<h1>Calcular valores</h1><form class=\"barra\" method=\"get\" action=\"/calcular\"><select name=\"obj\" onchange=\"this.form.submit()\"><option value=\"\">Elige un proyecto o contrato…</option><optgroup label=\"Proyectos\">");
        for (Map<String, Object> r : Db.consultar("SELECT id_proyecto, codigo, nombre FROM proyectos ORDER BY codigo"))
            opt(sb, "p:" + r.get("id_proyecto"), r.get("codigo") + " - " + r.get("nombre"), obj);
        sb.append("</optgroup><optgroup label=\"Contratos\">");
        for (Map<String, Object> r : Db.consultar("SELECT id_contrato, codigo_interno, objeto FROM contratos ORDER BY codigo_interno"))
            opt(sb, "c:" + r.get("id_contrato"), r.get("codigo_interno") + " - " + r.get("objeto"), obj);
        sb.append("</optgroup></select><noscript><button class=\"btn\">Ver</button></noscript></form>");
        if (!valido(obj)) return sb.append("<p>Elige un proyecto o un contrato para ver y editar sus líneas de cálculo.</p>").toString();

        long id = idDe(obj);
        boolean contrato = esContrato(obj);
        String donde = contrato ? "WHERE d.id_contrato=?" : "WHERE d.id_proyecto=?";
        List<Map<String, Object>> lineas = Db.consultar(SEL + donde + " ORDER BY d.id_detalle", id);
        boolean heredado = false;
        String nota = null;
        if (contrato && lineas.isEmpty()) {
            Map<String, Object> c = Db.fila("SELECT c.id_proyecto, p.codigo, p.nombre FROM contratos c LEFT JOIN proyectos p ON p.id_proyecto=c.id_proyecto WHERE c.id_contrato=?", id);
            if (c == null) return sb.append("<p>Contrato no encontrado.</p>").toString();
            if (c.get("id_proyecto") != null) {
                lineas = Db.consultar(SEL + "WHERE d.id_proyecto=? ORDER BY d.id_detalle", ((Number) c.get("id_proyecto")).longValue());
                if (!lineas.isEmpty()) { heredado = true; nota = "Líneas del proyecto " + c.get("codigo") + " - " + c.get("nombre") + " (proyecto de origen de este contrato). Para editarlas en el contrato, cópialas."; }
            }
        }
        if (heredado) {
            sb.append("<div class=\"aviso\">").append(Html.e(nota)).append("</div><form method=\"post\" action=\"/calcular/copiar\"><input type=\"hidden\" name=\"obj\" value=\"")
              .append(Html.e(obj)).append("\"><button class=\"btn\">Copiar líneas del proyecto al contrato</button></form>");
        }
        double sub = 0, desc = 0, base = 0, iva = 0, rf = 0, ri = 0, riv = 0;
        sb.append("<h2>Líneas</h2><div class=\"tabla\"><table><tr><th>Producto o servicio</th><th>Unidad</th><th>Cant.</th><th>Precio unit.</th><th>Subtotal</th><th>Desc. %</th><th>IVA %</th><th>IVA</th><th>Retef. %</th><th>ReteICA %</th><th>ReteIVA %</th><th></th></tr>");
        for (Map<String, Object> l : lineas) {
            sub += n(l.get("subtotal")); desc += n(l.get("descuento")); base += n(l.get("base_gravable")); iva += n(l.get("iva"));
            rf += n(l.get("retefuente")); ri += n(l.get("reteica")); riv += n(l.get("reteiva"));
            sb.append("<tr><td>").append(Html.e(l.get("producto"))).append("</td><td>").append(Html.e(l.get("unidad_medida"))).append("</td>")
              .append("<td class=\"d\">").append(Html.numero(l.get("cantidad"))).append("</td><td class=\"d\">").append(Html.dinero(l.get("precio_unitario")))
              .append("</td><td class=\"d\">").append(Html.dinero(l.get("subtotal"))).append("</td><td class=\"d\">").append(Html.numero(l.get("descuento_pct")))
              .append("</td><td class=\"d\">").append(Html.numero(l.get("iva_pct"))).append("</td><td class=\"d\">").append(Html.dinero(l.get("iva")))
              .append("</td><td class=\"d\">").append(Html.numero(l.get("retefuente_pct"))).append("</td><td class=\"d\">").append(Html.numero(l.get("reteica_pct")))
              .append("</td><td class=\"d\">").append(Html.numero(l.get("reteiva_pct"))).append("</td><td class=\"acc\">");
            if (!heredado) {
                sb.append("<a class=\"btn sec\" href=\"/calcular?obj=").append(obj).append("&editar=").append(l.get("id_detalle")).append("\">Editar</a> ")
                  .append("<form method=\"post\" action=\"/calcular/linea/eliminar\" onsubmit=\"return confirm('¿Eliminar la línea?')\"><input type=\"hidden\" name=\"obj\" value=\"")
                  .append(Html.e(obj)).append("\"><input type=\"hidden\" name=\"id_detalle\" value=\"").append(l.get("id_detalle")).append("\"><button class=\"btn pel\">Eliminar</button></form>");
            }
            sb.append("</td></tr>");
        }
        if (lineas.isEmpty()) sb.append("<tr><td colspan=\"12\">Sin líneas todavía.</td></tr>");
        sb.append("</table></div>");
        double total = base + iva, neto = total - rf - ri - riv;
        sb.append("<div class=\"tot\">");
        tarj(sb, "Subtotal", sub); tarj(sb, "Descuento", desc); tarj(sb, "IVA", iva); tarj(sb, "Total con IVA", total);
        tarj(sb, "Retefuente", rf); tarj(sb, "ReteICA", ri); tarj(sb, "ReteIVA", riv); tarj(sb, "Neto a recibir", neto);
        sb.append("</div>");
        if (!heredado) sb.append(formulario(obj, editar));
        return sb.toString();
    }

    private static double n(Object o) { return o == null ? 0 : ((Number) o).doubleValue(); }
    private static void tarj(StringBuilder sb, String t, double v) { sb.append("<div class=\"tarjeta\"><b>").append(Html.dinero(v)).append("</b><span>").append(t).append("</span></div>"); }
    private static void opt(StringBuilder sb, String v, String t, String sel) {
        sb.append("<option value=\"").append(Html.e(v)).append("\"").append(v.equals(sel) ? " selected" : "").append(">").append(Html.e(t)).append("</option>");
    }

    static String formulario(String obj, String editar) throws SQLException {
        Map<String, Object> l = null;
        if (editar != null && editar.matches("\\d{1,12}")) {
            l = Db.fila("SELECT * FROM detalle WHERE id_detalle=? AND " + (esContrato(obj) ? "id_contrato" : "id_proyecto") + "=?", Long.parseLong(editar), idDe(obj));
        }
        StringBuilder sb = new StringBuilder("<h2>").append(l == null ? "Agregar línea" : "Editar línea").append("</h2><form class=\"form\" method=\"post\" action=\"/calcular/linea\">")
            .append("<input type=\"hidden\" name=\"obj\" value=\"").append(Html.e(obj)).append("\">");
        if (l != null) sb.append("<input type=\"hidden\" name=\"id_detalle\" value=\"").append(l.get("id_detalle")).append("\">");
        sb.append("<label class=\"ancho\">Producto o servicio <span class=\"req\">*</span><select name=\"id_producto\" id=\"prod\" required><option value=\"\"></option>");
        for (Map<String, Object> p : Db.consultar("SELECT id_producto, nombre, precio_referencia FROM productos WHERE estado='activo' ORDER BY nombre")) {
            boolean sel = l != null && String.valueOf(l.get("id_producto")).equals(String.valueOf(p.get("id_producto")));
            sb.append("<option value=\"").append(p.get("id_producto")).append("\" data-precio=\"").append(p.get("precio_referencia") == null ? "" : Html.e(p.get("precio_referencia")))
              .append("\"").append(sel ? " selected" : "").append(">").append(Html.e(p.get("nombre"))).append("</option>");
        }
        sb.append("</select></label>");
        campo(sb, "cantidad", "Cantidad", l, "1", true); campo(sb, "precio_unitario", "Precio unitario", l, "", true);
        campo(sb, "descuento_pct", "Descuento %", l, "0", false); campo(sb, "iva_pct", "IVA %", l, "19", false);
        campo(sb, "retefuente_pct", "Retefuente %", l, "0", false); campo(sb, "reteica_pct", "ReteICA %", l, "0", false);
        campo(sb, "reteiva_pct", "ReteIVA % (sobre el IVA)", l, "0", false);
        sb.append("<div class=\"acciones\"><button class=\"btn\">Guardar línea</button>");
        if (l != null) sb.append("<a class=\"btn sec\" href=\"/calcular?obj=").append(obj).append("\">Cancelar</a>");
        sb.append("</div></form><script>var p=document.getElementById('prod'),pr=document.querySelector('[name=precio_unitario]');"
            + "p.addEventListener('change',function(){var o=p.options[p.selectedIndex];if(o&&o.dataset.precio&&!pr.value)pr.value=o.dataset.precio;});</script>");
        return sb.toString();
    }

    private static void campo(StringBuilder sb, String n, String et, Map<String, Object> l, String def, boolean req) {
        Object v = l != null ? l.get(n) : def;
        sb.append("<label>").append(et).append(req ? " <span class=\"req\">*</span>" : "").append("<input type=\"number\" step=\"any\" min=\"0\" name=\"").append(n)
          .append("\" value=\"").append(Html.e(v instanceof Double d && d == Math.rint(d) ? (Object) (long) d.doubleValue() : v)).append("\"").append(req ? " required" : "").append("></label>");
    }

    private static double num(Map<String, String> f, String k, boolean req, double def, double max, String et) {
        String v = f.get(k);
        if (v == null || v.isBlank()) { if (req) throw new IllegalArgumentException("Falta: " + et); return def; }
        try {
            double d = Double.parseDouble(v.trim());
            if (d < 0 || d > max || Double.isNaN(d)) throw new NumberFormatException();
            return d;
        } catch (NumberFormatException e) { throw new IllegalArgumentException("Valor no válido en " + et); }
    }

    static void guardar(Map<String, String> f) throws SQLException {
        String obj = f.get("obj");
        if (!valido(obj)) throw new IllegalArgumentException("Elige un proyecto o contrato.");
        long id = idDe(obj);
        long prod;
        try { prod = Long.parseLong(f.getOrDefault("id_producto", "")); } catch (NumberFormatException e) { throw new IllegalArgumentException("Elige un producto."); }
        double cant = num(f, "cantidad", true, 0, 1e9, "Cantidad"), pu = num(f, "precio_unitario", true, 0, 1e12, "Precio unitario");
        if (cant <= 0) throw new IllegalArgumentException("La cantidad debe ser mayor que 0.");
        double d = num(f, "descuento_pct", false, 0, 100, "Descuento"), iva = num(f, "iva_pct", false, 19, 100, "IVA"),
               rf = num(f, "retefuente_pct", false, 0, 100, "Retefuente"), ri = num(f, "reteica_pct", false, 0, 100, "ReteICA"), riv = num(f, "reteiva_pct", false, 0, 100, "ReteIVA");
        String col = esContrato(obj) ? "id_contrato" : "id_proyecto";
        String idd = f.get("id_detalle");
        if (idd != null && !idd.isBlank()) {
            int n = Db.ejecutar("UPDATE detalle SET id_producto=?,cantidad=?,precio_unitario=?,descuento_pct=?,iva_pct=?,retefuente_pct=?,reteica_pct=?,reteiva_pct=? WHERE id_detalle=? AND " + col + "=?",
                prod, cant, pu, d, iva, rf, ri, riv, Long.parseLong(idd), id);
            if (n == 0) throw new IllegalArgumentException("La línea no existe.");
        } else {
            Db.ejecutar("INSERT INTO detalle(" + col + ",id_producto,cantidad,precio_unitario,descuento_pct,iva_pct,retefuente_pct,reteica_pct,reteiva_pct) VALUES(?,?,?,?,?,?,?,?,?)",
                id, prod, cant, pu, d, iva, rf, ri, riv);
        }
    }

    static void eliminar(Map<String, String> f) throws SQLException {
        String obj = f.get("obj");
        if (!valido(obj) || f.get("id_detalle") == null || !f.get("id_detalle").matches("\\d{1,12}")) throw new IllegalArgumentException("Solicitud no válida.");
        Db.ejecutar("DELETE FROM detalle WHERE id_detalle=? AND " + (esContrato(obj) ? "id_contrato" : "id_proyecto") + "=?", Long.parseLong(f.get("id_detalle")), idDe(obj));
    }

    static void copiar(Map<String, String> f) throws SQLException {
        String obj = f.get("obj");
        if (!valido(obj) || !esContrato(obj)) throw new IllegalArgumentException("Solicitud no válida.");
        long id = idDe(obj);
        if (Db.contar("SELECT COUNT(*) FROM detalle WHERE id_contrato=?", id) > 0) throw new IllegalArgumentException("El contrato ya tiene líneas propias.");
        Map<String, Object> c = Db.fila("SELECT id_proyecto FROM contratos WHERE id_contrato=?", id);
        if (c == null || c.get("id_proyecto") == null) throw new IllegalArgumentException("El contrato no tiene proyecto de origen.");
        Db.ejecutar("INSERT INTO detalle(id_contrato,id_producto,cantidad,precio_unitario,descuento_pct,iva_pct,retefuente_pct,reteica_pct,reteiva_pct) "
            + "SELECT ?,id_producto,cantidad,precio_unitario,descuento_pct,iva_pct,retefuente_pct,reteica_pct,reteiva_pct FROM detalle WHERE id_proyecto=?", id, ((Number) c.get("id_proyecto")).longValue());
    }
}
