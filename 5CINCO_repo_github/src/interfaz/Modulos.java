package interfaz;

import datos.Repositorio;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

/** Definición de todos los módulos de la aplicación y su relación con las tablas. */
public final class Modulos {

    private Modulos() {
    }

    private static final String[] ESTADO = {"activo", "inactivo"};
    private static final String[] DOC = {"CC", "CE", "TI", "PAS"};

    private static final String REF_CLIENTES = "SELECT id_cliente, nombre FROM clientes ORDER BY nombre";
    private static final String REF_PROYECTOS = "SELECT id_proyecto, COALESCE(codigo,'') || ' - ' || nombre FROM proyectos ORDER BY codigo";
    private static final String REF_EMPLEADOS = "SELECT id_empleado, nombre FROM empleados ORDER BY nombre";
    private static final String REF_CONTRATISTAS = "SELECT id_contratista, nombre FROM contratistas ORDER BY nombre";
    public static final String REF_CONTRATOS = "SELECT id_contrato, codigo_interno || ' - ' || substr(objeto,1,60) FROM contratos ORDER BY id_contrato";
    public static final String REF_PROYECTOS_PUBLICO = REF_PROYECTOS;

    public static Modulo clientes() {
        return new Modulo("Clientes", "cliente", "clientes", "id_cliente",
                "SELECT id_cliente, nombre AS \"Nombre\", tipo_cliente AS \"Tipo\", tipo_documento AS \"Doc.\", "
                + "numero_documento AS \"Número\", digito_verificacion AS \"DV\", telefono AS \"Teléfono\", "
                + "correo AS \"Correo\" FROM clientes ORDER BY nombre",
                List.of(
                        Campo.texto("nombre", "Nombre o razón social").req(),
                        Campo.lista("tipo_cliente", "Tipo de cliente", "publica", "privada", "persona_natural",
                                "consorcio_union_temporal"),
                        Campo.lista("tipo_documento", "Tipo de documento", "NIT", "CC").req().inicial("NIT"),
                        Campo.texto("numero_documento", "Número (sin puntos ni dígito de verificación)"),
                        Campo.texto("digito_verificacion", "Dígito de verificación"),
                        Campo.texto("telefono", "Teléfono"),
                        Campo.texto("correo", "Correo"),
                        Campo.texto("direccion", "Dirección")));
    }

    public static Modulo proyectos() {
        Modulo m = new Modulo("Proyectos", "proyecto", "proyectos", "id_proyecto",
                "SELECT p.id_proyecto, p.codigo AS \"Código\", p.nombre AS \"Nombre\", p.tipo_proyecto AS \"Tipo\", "
                + "cl.nombre AS \"Cliente\", p.estado AS \"Estado\", p.fecha_presentacion AS \"Presentación\", "
                + "ct.codigo_interno AS \"Contrato\" FROM proyectos p "
                + "LEFT JOIN clientes cl ON cl.id_cliente = p.id_cliente "
                + "LEFT JOIN contratos ct ON ct.id_proyecto = p.id_proyecto ORDER BY p.id_proyecto DESC",
                List.of(
                        Campo.texto("codigo", "Código").inicialSql(
                                "SELECT 'PRY-' || printf('%04d', COALESCE(MAX(CAST(substr(codigo,5) AS INTEGER)),0)+1) "
                                + "FROM proyectos WHERE codigo GLOB 'PRY-[0-9]*'"),
                        Campo.texto("nombre", "Nombre del proyecto").req(),
                        Campo.area("descripcion", "Descripción"),
                        Campo.lista("tipo_proyecto", "Tipo de proyecto", "formal", "independiente").req(),
                        Campo.ref("id_cliente", "Cliente", REF_CLIENTES),
                        Campo.fecha("fecha_presentacion", "Fecha de presentación"),
                        Campo.texto("ubicacion", "Ubicación"),
                        Campo.lista("estado", "Estado", "propuesta", "ganado", "no_ganado", "en_ejecucion",
                                "terminado").req().inicial("propuesta")));
        return m;
    }

    public static Modulo contratos() {
        Modulo m = new Modulo("Contratos", "contrato", "contratos", "id_contrato",
                "SELECT c.id_contrato, c.codigo_interno AS \"Código\", c.numero_contrato AS \"N.º contrato\", "
                + "cl.nombre AS \"Cliente\", c.tipo_contrato AS \"Tipo\", c.valor_contrato AS \"Valor\", "
                + "c.porcentaje_participacion AS \"Part.\", c.valor_participacion AS \"Valor participación\", "
                + "v.valor_smmlv_participacion AS \"SMMLV\", c.fecha_inicio AS \"Inicio\", "
                + "c.fecha_terminacion AS \"Fin\", c.estado AS \"Estado\", "
                + "(SELECT GROUP_CONCAT(cu.codigo, ', ') FROM contrato_codigos cc "
                + "JOIN codigos_compra cu ON cu.id_codigo = cc.id_codigo "
                + "WHERE cc.id_contrato = c.id_contrato) AS \"Códigos\", "
                + "(SELECT COUNT(*) FROM contrato_documentos d WHERE d.id_contrato = c.id_contrato) AS \"PDFs\" "
                + "FROM contratos c "
                + "JOIN clientes cl ON cl.id_cliente = c.id_cliente "
                + "LEFT JOIN v_contratos v ON v.id_contrato = c.id_contrato ORDER BY c.id_contrato",
                List.of(
                        Campo.texto("codigo_interno", "Código interno").req().inicialSql(
                                "SELECT '5CINCO-' || printf('%04d', COALESCE(MAX(CAST(substr(codigo_interno,8) AS INTEGER)),0)+1) "
                                + "FROM contratos"),
                        Campo.texto("numero_contrato", "Número del contrato (si lo tiene)"),
                        Campo.entero("consecutivo_rup", "Consecutivo RUP"),
                        Campo.ref("id_cliente", "Cliente", REF_CLIENTES).req(),
                        Campo.ref("id_proyecto", "Proyecto de origen", REF_PROYECTOS),
                        Campo.texto("tipo_contrato", "Tipo de contrato (libre)"),
                        Campo.area("objeto", "Objeto del contrato").req(),
                        Campo.texto("figura_contratista", "Contratista / figura (consorcio, unión temporal)"),
                        Campo.decimal("porcentaje_participacion", "Participación (0 a 1; 1 = 100 %)").req().inicial("1"),
                        Campo.decimal("valor_contrato", "Valor del contrato (incluye otrosí; vacío = se calcula del proyecto de origen)"),
                        Campo.fecha("fecha_inicio", "Fecha de inicio"),
                        Campo.texto("fecha_inicio_texto", "Fecha de inicio, como texto (opcional)"),
                        Campo.fecha("fecha_terminacion", "Fecha de terminación"),
                        Campo.texto("fecha_terminacion_texto", "Fecha de terminación, como texto (opcional)"),
                        Campo.texto("ubicacion_obra", "Ubicación de la obra"),
                        Campo.lista("estado", "Estado", "en_ejecucion", "terminado").req().inicial("en_ejecucion")));
        m.mensajeCheck = "La participación debe ser mayor que 0 y máximo 1, y el valor no puede ser negativo.";
        m.antesDeGuardar = v -> {
            if (v.get("valor_contrato") == null && v.get("id_proyecto") != null) {
                // el valor sale del total con IVA de las líneas del proyecto de origen
                try {
                    Repositorio.Resultado r = Repositorio.consultar(
                            "SELECT total_con_iva FROM v_totales_proyecto WHERE id_proyecto = ?", v.get("id_proyecto"));
                    if (!r.filas.isEmpty() && r.filas.get(0)[0] != null) {
                        v.put("valor_contrato", Math.round(((Number) r.filas.get(0)[0]).doubleValue()));
                    }
                } catch (java.sql.SQLException ex) {
                    throw new IllegalArgumentException("No se pudo calcular el valor desde el proyecto: " + ex.getMessage());
                }
            }
            if (v.get("valor_contrato") == null) {
                throw new IllegalArgumentException("Falta el valor del contrato: escríbelo o elige un proyecto de origen que tenga líneas en Calcular valores.");
            }
            if (v.get("fecha_inicio_texto") == null && v.get("fecha_inicio") != null) {
                v.put("fecha_inicio_texto", v.get("fecha_inicio"));
            }
            if (v.get("fecha_terminacion_texto") == null && v.get("fecha_terminacion") != null) {
                v.put("fecha_terminacion_texto", v.get("fecha_terminacion"));
            }
        };
        return m;
    }

    public static Modulo codigosCompra() {
        Modulo m = new Modulo("Códigos de compra", "código", "codigos_compra", "id_codigo",
                "SELECT cu.id_codigo, cu.codigo AS \"Código\", cu.descripcion AS \"Descripción\", cu.tipo AS \"Tipo\", "
                + "cu.grupo AS \"Grupo\", "
                + "(SELECT COUNT(*) FROM contrato_codigos cc WHERE cc.id_codigo = cu.id_codigo) AS \"Contratos\" "
                + "FROM codigos_compra cu ORDER BY cu.codigo",
                List.of(
                        Campo.texto("codigo", "Código (UNSPSC, por ejemplo 72101500)").req(),
                        Campo.texto("descripcion", "Descripción").req(),
                        Campo.lista("tipo", "Tipo (opcional)", "bien", "material", "servicio"),
                        Campo.texto("grupo", "Grupo (opcional)")));
        m.mensajeCheck = "El tipo debe ser bien, material o servicio.";
        return m;
    }

    public static Modulo productos() {
        return new Modulo("Productos", "producto", "productos", "id_producto",
                "SELECT id_producto, codigo AS \"Código\", nombre AS \"Nombre\", tipo AS \"Tipo\", "
                + "unidad_medida AS \"Unidad\", precio_referencia AS \"Precio de referencia\", "
                + "(SELECT cu.codigo FROM codigos_compra cu WHERE cu.id_codigo = productos.id_codigo) AS \"Código de compra\", "
                + "estado AS \"Estado\" FROM productos ORDER BY nombre",
                List.of(
                        Campo.texto("codigo", "Código").inicialSql(
                                "SELECT 'PRD-' || printf('%04d', COALESCE(MAX(CAST(substr(codigo,5) AS INTEGER)),0)+1) "
                                + "FROM productos WHERE codigo GLOB 'PRD-[0-9]*'"),
                        Campo.texto("nombre", "Nombre").req(),
                        Campo.area("descripcion", "Descripción"),
                        Campo.lista("tipo", "Tipo", "mueble", "material", "servicio").req(),
                        Campo.texto("unidad_medida", "Unidad de medida (unidad, m², metro lineal…)").req().inicial("unidad"),
                        Campo.decimal("precio_referencia", "Precio de referencia"),
                        Campo.ref("id_codigo", "Código de compra (se asigna solo a los contratos que lo usen)",
                                "SELECT id_codigo, codigo || ' - ' || descripcion FROM codigos_compra ORDER BY codigo"),
                        Campo.lista("estado", "Estado", ESTADO).req().inicial("activo")));
    }

    public static Modulo proveedores() {
        return new Modulo("Proveedores", "proveedor", "proveedores", "id_proveedor",
                "SELECT id_proveedor, nombre AS \"Nombre\", tipo_documento AS \"Doc.\", numero_documento AS \"Número\", "
                + "telefono AS \"Teléfono\", direccion AS \"Dirección\", ubicacion AS \"Ubicación\", "
                + "descripcion_suministro AS \"Qué suministra\", estado AS \"Estado\" FROM proveedores ORDER BY nombre",
                List.of(
                        Campo.texto("nombre", "Nombre o razón social").req(),
                        Campo.lista("tipo_documento", "Tipo de documento", "NIT", "CC").req().inicial("NIT"),
                        Campo.texto("numero_documento", "Número (sin puntos ni dígito de verificación)").req(),
                        Campo.texto("digito_verificacion", "Dígito de verificación"),
                        Campo.texto("telefono", "Teléfono"),
                        Campo.texto("direccion", "Dirección"),
                        Campo.texto("ubicacion", "Ciudad o departamento"),
                        Campo.lista("tipo_ubicacion", "La ubicación es", "ciudad", "departamento").req().inicial("ciudad"),
                        Campo.texto("correo", "Correo"),
                        Campo.texto("descripcion_suministro", "Qué suministra"),
                        Campo.lista("estado", "Estado", ESTADO).req().inicial("activo")));
    }

    public static Modulo empleados() {
        Modulo m = new Modulo("Empleados", "empleado", "empleados", "id_empleado",
                "SELECT id_empleado, nombre AS \"Nombre\", numero_documento AS \"Documento\", cargo AS \"Cargo\", "
                + "telefono AS \"Teléfono\", correo AS \"Correo\", fecha_ingreso AS \"Ingreso\", "
                + "salario AS \"Salario\", eps AS \"EPS\", arl AS \"ARL\", fondo_pension AS \"Pensión\", "
                + "caja_compensacion AS \"Caja de compensación\", estado AS \"Estado\" FROM empleados ORDER BY nombre",
                List.of(
                        Campo.texto("nombre", "Nombre completo").req(),
                        Campo.lista("tipo_documento", "Tipo de documento", DOC).req().inicial("CC"),
                        Campo.texto("numero_documento", "Número de documento").req(),
                        Campo.texto("cargo", "Cargo"),
                        Campo.texto("telefono", "Teléfono"),
                        Campo.texto("correo", "Correo"),
                        Campo.texto("direccion", "Dirección"),
                        Campo.fecha("fecha_ingreso", "Fecha de ingreso"),
                        Campo.decimal("salario", "Salario").sensible(),
                        Campo.texto("eps", "EPS").sensible(),
                        Campo.texto("arl", "ARL").sensible(),
                        Campo.texto("fondo_pension", "Fondo de pensión").sensible(),
                        Campo.texto("caja_compensacion", "Caja de compensación").sensible(),
                        Campo.lista("estado", "Estado", ESTADO).req().inicial("activo")));
        m.sqlListadoBasico = "SELECT e.id_empleado, e.nombre AS \"Nombre\", e.numero_documento AS \"Documento\", "
                + "e.cargo AS \"Cargo\", e.telefono AS \"Teléfono\", e.correo AS \"Correo\", "
                + "e.fecha_ingreso AS \"Ingreso\", e.estado AS \"Estado\" FROM v_empleados_basico e ORDER BY e.nombre";
        return m;
    }

    public static Modulo contratistas() {
        return new Modulo("Contratistas", "contratista", "contratistas", "id_contratista",
                "SELECT id_contratista, nombre AS \"Nombre\", numero_documento AS \"Documento\", "
                + "actividad AS \"Actividad\", telefono AS \"Teléfono\", correo AS \"Correo\", estado AS \"Estado\" "
                + "FROM contratistas ORDER BY nombre",
                List.of(
                        Campo.texto("nombre", "Nombre completo").req(),
                        Campo.lista("tipo_documento", "Tipo de documento", DOC).req().inicial("CC"),
                        Campo.texto("numero_documento", "Número de documento").req(),
                        Campo.texto("actividad", "Actividad"),
                        Campo.texto("telefono", "Teléfono"),
                        Campo.texto("correo", "Correo"),
                        Campo.lista("estado", "Estado", ESTADO).req().inicial("activo")));
    }

    public static Modulo asignaciones() {
        Modulo m = new Modulo("Asignaciones", "asignación", "asignaciones", "id_asignacion",
                "SELECT a.id_asignacion, ct.codigo_interno AS \"Contrato\", cl.nombre AS \"Cliente\", "
                + "COALESCE(e.nombre, c.nombre) AS \"Persona\", "
                + "CASE WHEN a.id_empleado IS NOT NULL THEN 'Empleado' ELSE 'Contratista' END AS \"Vinculación\", "
                + "a.actividad AS \"Actividad\", a.fecha_inicio AS \"Inicio\", a.fecha_fin AS \"Fin\", "
                + "a.valor_pagado AS \"Valor pagado\" FROM asignaciones a "
                + "JOIN contratos ct ON ct.id_contrato = a.id_contrato "
                + "JOIN clientes cl ON cl.id_cliente = ct.id_cliente "
                + "LEFT JOIN empleados e ON e.id_empleado = a.id_empleado "
                + "LEFT JOIN contratistas c ON c.id_contratista = a.id_contratista ORDER BY a.id_asignacion DESC",
                List.of(
                        Campo.ref("id_contrato", "Contrato", REF_CONTRATOS).req(),
                        Campo.ref("id_empleado", "Empleado (elige empleado O contratista)", REF_EMPLEADOS),
                        Campo.ref("id_contratista", "Contratista", REF_CONTRATISTAS),
                        Campo.texto("actividad", "Actividad"),
                        Campo.fecha("fecha_inicio", "Fecha de inicio"),
                        Campo.fecha("fecha_fin", "Fecha de fin"),
                        Campo.decimal("valor_pagado", "Valor pagado (solo contratistas)")));
        m.mensajeCheck = "Debes elegir un empleado O un contratista (no ambos). El valor pagado solo aplica a contratistas.";
        return m;
    }

    /** Líneas de productos y servicios (se usa desde "Calcular valores"). */
    public static Modulo lineaDetalle() {
        Modulo m = new Modulo("Línea de detalle", "línea", "detalle", "id_detalle", "SELECT 1", List.of(
                Campo.ref("id_producto", "Producto o servicio",
                        "SELECT id_producto, nombre || ' (' || unidad_medida || ')' FROM productos "
                        + "WHERE estado = 'activo' ORDER BY nombre").req(),
                Campo.decimal("cantidad", "Cantidad").req(),
                Campo.decimal("precio_unitario", "Precio unitario pactado").req(),
                Campo.decimal("descuento_pct", "Descuento (%)").req().inicial("0"),
                Campo.decimal("iva_pct", "IVA (%)").req().inicial("19"),
                Campo.decimal("retefuente_pct", "Retefuente (%)").req().inicial("0"),
                Campo.decimal("reteica_pct", "ReteICA (%)").req().inicial("0"),
                Campo.decimal("reteiva_pct", "ReteIVA (% sobre el IVA)").req().inicial("0")));
        m.mensajeCheck = "La cantidad debe ser mayor que 0 y los porcentajes deben estar entre 0 y 100.";
        return m;
    }

    /** Salario mínimo mensual (SMMLV) por año; lo usa la vista v_contratos para calcular el valor en SMMLV. */
    public static Modulo salarioMinimo() {
        Modulo m = new Modulo("Salario mínimo", "salario mínimo", "salario_minimo", "anio",
                "SELECT anio, anio AS \"Año\", valor AS \"Salario mínimo mensual\" FROM salario_minimo ORDER BY anio DESC",
                List.of(
                        Campo.entero("anio", "Año").req(),
                        Campo.decimal("valor", "Salario mínimo mensual (sin auxilio de transporte)").req()));
        m.mensajeCheck = "El valor del salario mínimo debe ser mayor que 0.";
        return m;
    }

    public static List<Modulo> todos() {
        return Arrays.asList(clientes(), proyectos(), contratos(), codigosCompra(), productos(), proveedores(), empleados(),
                contratistas(), asignaciones(), salarioMinimo());
    }

    static Map<String, Modulo> porTitulo() {
        Map<String, Modulo> m = new java.util.LinkedHashMap<>();
        for (Modulo x : todos()) {
            m.put(x.titulo, x);
        }
        return m;
    }
}
