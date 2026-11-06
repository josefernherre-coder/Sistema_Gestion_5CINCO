package web;

import java.io.IOException;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import web.Xlsx.Celda;
import web.Xlsx.Hoja;

/**
 * Reporte de contratos en un periodo de fechas.
 *
 * Regla del periodo: un contrato entra si su fecha de terminación (o la de inicio, cuando no
 * tiene terminación) cae dentro del rango. Así cada contrato se cuenta una sola vez y un reporte
 * anual no repite contratos que atraviesan varios años.
 *
 * El valor que se suma es el de participación (valor del contrato x % de participación), que es
 * lo que le corresponde a 5CINCO cuando el contrato se ejecuta en consorcio o unión temporal.
 */
public final class Reporte {

    // Posiciones de las columnas del resultado (deben coincidir con SQL_DETALLE).
    public static final int COL_VALOR_CONTRATO = 6;
    public static final int COL_VALOR_PARTICIPACION = 8;

    private static final String FECHA_REF = "COALESCE(fecha_terminacion, fecha_inicio)";

    private static final String SQL_DETALLE
            = "SELECT codigo_interno AS \"Código\", numero_contrato AS \"N.º contrato\", cliente AS \"Cliente\", "
            + "objeto AS \"Contrato (objeto)\", fecha_inicio AS \"Inicio\", fecha_terminacion AS \"Terminación\", "
            + "valor_contrato AS \"Valor del contrato\", porcentaje_participacion AS \"Participación\", "
            + "valor_participacion AS \"Valor participación\", CAST(anio_smmlv AS TEXT) AS \"Año SMMLV\", "
            + "valor_smmlv_participacion AS \"Valor en SMMLV\", "
            + "(SELECT GROUP_CONCAT(cu.codigo, ', ') FROM contrato_codigos cc "
            + "JOIN codigos_compra cu ON cu.id_codigo = cc.id_codigo "
            + "WHERE cc.id_contrato = v_contratos.id_contrato) AS \"Códigos de compra\" "
            + "FROM v_contratos WHERE " + FECHA_REF + " BETWEEN ? AND ? "
            + "ORDER BY " + FECHA_REF + ", codigo_interno";

    private static final String SQL_RESUMEN
            = "SELECT CAST(strftime('%Y', " + FECHA_REF + ") AS INTEGER) AS anio, COUNT(*), "
            + "SUM(valor_contrato), SUM(valor_participacion) "
            + "FROM v_contratos WHERE " + FECHA_REF + " BETWEEN ? AND ? GROUP BY 1 ORDER BY 1";

    private static final String SQL_CODIGOS
            = "SELECT codigo_interno AS \"Código contrato\", numero_contrato AS \"N.º contrato\", cliente AS \"Cliente\", "
            + "cu.codigo AS \"Código de compra\", cu.descripcion AS \"Descripción\", cu.tipo AS \"Tipo\" "
            + "FROM contrato_codigos cc JOIN v_contratos v ON v.id_contrato = cc.id_contrato "
            + "JOIN codigos_compra cu ON cu.id_codigo = cc.id_codigo "
            + "WHERE " + FECHA_REF + " BETWEEN ? AND ? "
            + "ORDER BY " + FECHA_REF + ", codigo_interno, cu.codigo";

    private Reporte() {
    }

    /** Contratos del periodo (ambas fechas incluidas). */
    public static Res consultar(LocalDate desde, LocalDate hasta) throws SQLException {
        return Res.consultar(SQL_DETALLE, desde.toString(), hasta.toString());
    }

    /** Total por año del periodo: año, cantidad, suma de valores, suma de participación. */
    public static Res resumen(LocalDate desde, LocalDate hasta) throws SQLException {
        return Res.consultar(SQL_RESUMEN, desde.toString(), hasta.toString());
    }

    /** Suma de una columna numérica del resultado (los valores nulos cuentan como 0). */
    public static BigDecimal sumar(Res r, int columna) {
        BigDecimal total = BigDecimal.ZERO;
        for (Object[] fila : r.filas) {
            if (fila[columna] != null) {
                total = total.add(new BigDecimal(fila[columna].toString()));
            }
        }
        return total;
    }

    /** Crea el archivo de Excel con dos hojas: «Contratos» (detalle) y «Resumen por año». */
    public static void exportar(java.io.OutputStream destino, LocalDate desde, LocalDate hasta) throws SQLException, IOException {
        Res r = consultar(desde, hasta);
        Res resumen = Res.consultar(SQL_RESUMEN, desde.toString(), hasta.toString());

        List<Hoja> hojas = new ArrayList<>();
        hojas.add(hojaDetalle(r, desde, hasta));
        hojas.add(hojaResumen(resumen, desde, hasta));
        hojas.add(hojaCodigos(Res.consultar(SQL_CODIGOS, desde.toString(), hasta.toString()), desde, hasta));
        Xlsx.guardar(destino, hojas);
    }

    private static Hoja hojaDetalle(Res r, LocalDate desde, LocalDate hasta) {
        Hoja h = new Hoja("Contratos")
                .anchos(14, 14, 32, 60, 12, 13, 20, 14, 20, 10, 16, 34)
                .congelar(5);
        h.agregar(Celda.texto("Reporte de contratos", Xlsx.E_TITULO));
        h.agregar(Celda.texto("Periodo: " + desde + " a " + hasta, Xlsx.E_NOTA));
        h.agregar(Celda.texto("Incluye los contratos cuya fecha de terminación (o de inicio, si no tiene terminación) "
                + "está dentro del periodo. El valor de participación es el valor del contrato por su % de participación.",
                Xlsx.E_NOTA));
        h.agregar(Celda.texto("Generado: " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")),
                Xlsx.E_NOTA));

        Celda[] encabezado = new Celda[r.columnas.size()];
        for (int i = 0; i < encabezado.length; i++) {
            encabezado[i] = Celda.texto(r.columnas.get(i), Xlsx.E_ENCABEZADO);
        }
        h.agregar(encabezado); // fila 5

        if (r.filas.isEmpty()) {
            h.agregar(Celda.texto("No hay contratos en el periodo seleccionado.", Xlsx.E_TEXTO));
            return h;
        }

        for (Object[] f : r.filas) {
            h.agregar(
                    Celda.texto(texto(f[0]), Xlsx.E_TEXTO),
                    Celda.texto(texto(f[1]), Xlsx.E_TEXTO),
                    Celda.texto(texto(f[2]), Xlsx.E_TEXTO),
                    Celda.texto(texto(f[3]), Xlsx.E_TEXTO),
                    Celda.fecha(f[4], Xlsx.E_FECHA),
                    Celda.fecha(f[5], Xlsx.E_FECHA),
                    Celda.numero(f[6], Xlsx.E_DINERO),
                    Celda.numero(f[7], Xlsx.E_PORCENTAJE),
                    Celda.numero(f[8], Xlsx.E_DINERO),
                    Celda.numero(f[9], Xlsx.E_ENTERO),
                    Celda.numero(f[10], Xlsx.E_DECIMAL),
                    Celda.texto(texto(f[11]), Xlsx.E_TEXTO));
        }

        int primera = 6;
        int ultima = 5 + r.filas.size();
        int n = r.filas.size();
        h.agregar(
                Celda.texto("TOTAL (" + n + (n == 1 ? " contrato)" : " contratos)"), Xlsx.E_TOTAL_TEXTO),
                Celda.vacia(Xlsx.E_TOTAL_TEXTO),
                Celda.vacia(Xlsx.E_TOTAL_TEXTO),
                Celda.vacia(Xlsx.E_TOTAL_TEXTO),
                Celda.vacia(Xlsx.E_TOTAL_TEXTO),
                Celda.vacia(Xlsx.E_TOTAL_TEXTO),
                Celda.formula("SUM(G" + primera + ":G" + ultima + ")", sumar(r, COL_VALOR_CONTRATO), Xlsx.E_TOTAL_DINERO),
                Celda.vacia(Xlsx.E_TOTAL_TEXTO),
                Celda.formula("SUM(I" + primera + ":I" + ultima + ")", sumar(r, COL_VALOR_PARTICIPACION), Xlsx.E_TOTAL_DINERO),
                Celda.vacia(Xlsx.E_TOTAL_TEXTO),
                Celda.vacia(Xlsx.E_TOTAL_TEXTO),
                Celda.vacia(Xlsx.E_TOTAL_TEXTO));
        return h;
    }

    /** Una fila por cada código de compra de cada contrato del periodo. */
    private static Hoja hojaCodigos(Res r, LocalDate desde, LocalDate hasta) {
        Hoja h = new Hoja("Códigos por contrato").anchos(16, 14, 32, 16, 60, 12).congelar(3);
        h.agregar(Celda.texto("Códigos de compra por contrato", Xlsx.E_TITULO));
        h.agregar(Celda.texto("Periodo: " + desde + " a " + hasta, Xlsx.E_NOTA));
        Celda[] encabezado = new Celda[r.columnas.size()];
        for (int i = 0; i < encabezado.length; i++) {
            encabezado[i] = Celda.texto(r.columnas.get(i), Xlsx.E_ENCABEZADO);
        }
        h.agregar(encabezado);
        if (r.filas.isEmpty()) {
            h.agregar(Celda.texto("Ningún contrato del periodo tiene códigos de compra asignados.", Xlsx.E_TEXTO));
            return h;
        }
        for (Object[] f : r.filas) {
            h.agregar(
                    Celda.texto(texto(f[0]), Xlsx.E_TEXTO), Celda.texto(texto(f[1]), Xlsx.E_TEXTO),
                    Celda.texto(texto(f[2]), Xlsx.E_TEXTO), Celda.texto(texto(f[3]), Xlsx.E_TEXTO),
                    Celda.texto(texto(f[4]), Xlsx.E_TEXTO), Celda.texto(texto(f[5]), Xlsx.E_TEXTO));
        }
        return h;
    }

    private static Hoja hojaResumen(Res resumen, LocalDate desde, LocalDate hasta) {
        Hoja h = new Hoja("Resumen por año").anchos(10, 14, 22, 22).congelar(3);
        h.agregar(Celda.texto("Total por año", Xlsx.E_TITULO));
        h.agregar(Celda.texto("Periodo: " + desde + " a " + hasta, Xlsx.E_NOTA));
        h.agregar(
                Celda.texto("Año", Xlsx.E_ENCABEZADO),
                Celda.texto("Contratos", Xlsx.E_ENCABEZADO),
                Celda.texto("Valor de los contratos", Xlsx.E_ENCABEZADO),
                Celda.texto("Valor participación", Xlsx.E_ENCABEZADO));
        if (resumen.filas.isEmpty()) {
            h.agregar(Celda.texto("Sin contratos en el periodo.", Xlsx.E_TEXTO));
            return h;
        }
        for (Object[] f : resumen.filas) {
            h.agregar(
                    Celda.numero(f[0], Xlsx.E_ENTERO),
                    Celda.numero(f[1], Xlsx.E_ENTERO),
                    Celda.numero(f[2], Xlsx.E_DINERO),
                    Celda.numero(f[3], Xlsx.E_DINERO));
        }
        int primera = 4;
        int ultima = 3 + resumen.filas.size();
        h.agregar(
                Celda.texto("TOTAL", Xlsx.E_TOTAL_TEXTO),
                Celda.formula("SUM(B" + primera + ":B" + ultima + ")", sumar(resumen, 1), Xlsx.E_TOTAL_ENTERO),
                Celda.formula("SUM(C" + primera + ":C" + ultima + ")", sumar(resumen, 2), Xlsx.E_TOTAL_DINERO),
                Celda.formula("SUM(D" + primera + ":D" + ultima + ")", sumar(resumen, 3), Xlsx.E_TOTAL_DINERO));
        return h;
    }

    private static String texto(Object o) {
        return o == null ? null : o.toString();
    }
}
