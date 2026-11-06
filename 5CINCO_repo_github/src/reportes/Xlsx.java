package reportes;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * Generador mínimo de libros de Excel (.xlsx) sin librerías externas.
 * Un .xlsx es un ZIP con varios XML; aquí se escriben solo las partes necesarias:
 * hojas con texto, números, fechas y fórmulas, con un juego fijo de estilos.
 */
public final class Xlsx {

    // Índices de estilo (ver estilos() más abajo)
    public static final int E_NINGUNO = 0;
    public static final int E_TITULO = 1;
    public static final int E_NOTA = 2;
    public static final int E_ENCABEZADO = 3;
    public static final int E_TEXTO = 4;
    public static final int E_FECHA = 5;
    public static final int E_DINERO = 6;
    public static final int E_PORCENTAJE = 7;
    public static final int E_DECIMAL = 8;
    public static final int E_ENTERO = 9;
    public static final int E_TOTAL_TEXTO = 10;
    public static final int E_TOTAL_DINERO = 11;
    public static final int E_TOTAL_ENTERO = 12;

    private Xlsx() {
    }

    /** Una celda: vacía, texto, número, fecha o fórmula (con su valor ya calculado). */
    public static final class Celda {

        private final int tipo; // 0 vacía, 1 texto, 2 número, 3 fórmula
        private final String valor;
        private final String formula;
        private final int estilo;

        private Celda(int tipo, String valor, String formula, int estilo) {
            this.tipo = tipo;
            this.valor = valor;
            this.formula = formula;
            this.estilo = estilo;
        }

        public static Celda vacia(int estilo) {
            return new Celda(0, null, null, estilo);
        }

        public static Celda texto(String t, int estilo) {
            return t == null ? vacia(estilo) : new Celda(1, t, null, estilo);
        }

        /** Acepta cualquier número (o texto numérico). Si no es un número, se escribe como texto. */
        public static Celda numero(Object v, int estilo) {
            if (v == null) {
                return vacia(estilo);
            }
            try {
                return new Celda(2, new BigDecimal(v.toString().trim()).toPlainString(), null, estilo);
            } catch (NumberFormatException ex) {
                return texto(v.toString(), estilo);
            }
        }

        /** Fecha en formato AAAA-MM-DD; si no se puede leer, se escribe tal cual como texto. */
        public static Celda fecha(Object v, int estilo) {
            if (v == null || v.toString().isBlank()) {
                return vacia(estilo);
            }
            try {
                long serie = ChronoUnit.DAYS.between(LocalDate.of(1899, 12, 30), LocalDate.parse(v.toString().trim()));
                return new Celda(2, Long.toString(serie), null, estilo);
            } catch (RuntimeException ex) {
                return texto(v.toString(), E_TEXTO);
            }
        }

        /** Fórmula de Excel (sin el "="); se guarda también su resultado para visores que no recalculan. */
        public static Celda formula(String formula, BigDecimal resultado, int estilo) {
            return new Celda(3, resultado.toPlainString(), formula, estilo);
        }

        String xml(String ref) {
            String s = estilo > 0 ? " s=\"" + estilo + "\"" : "";
            switch (tipo) {
                case 1:
                    return "<c r=\"" + ref + "\"" + s + " t=\"inlineStr\"><is><t xml:space=\"preserve\">"
                            + esc(valor) + "</t></is></c>";
                case 2:
                    return "<c r=\"" + ref + "\"" + s + "><v>" + valor + "</v></c>";
                case 3:
                    return "<c r=\"" + ref + "\"" + s + "><f>" + esc(formula) + "</f><v>" + valor + "</v></c>";
                default:
                    return "<c r=\"" + ref + "\"" + s + "/>";
            }
        }
    }

    /** Una hoja del libro. */
    public static final class Hoja {

        private final String nombre;
        private final List<List<Celda>> filas = new ArrayList<>();
        private double[] anchos = new double[0];
        private int congelar = 0;

        public Hoja(String nombre) {
            this.nombre = nombre;
        }

        public Hoja anchos(double... a) {
            this.anchos = a;
            return this;
        }

        /** Deja fijas las primeras n filas al desplazarse. */
        public Hoja congelar(int n) {
            this.congelar = n;
            return this;
        }

        /** Agrega una fila al final y devuelve su número (1 = primera fila). */
        public int agregar(Celda... celdas) {
            List<Celda> fila = new ArrayList<>();
            for (Celda c : celdas) {
                fila.add(c);
            }
            filas.add(fila);
            return filas.size();
        }

        public int numeroDeFilas() {
            return filas.size();
        }

        String xml(boolean seleccionada) {
            StringBuilder sb = new StringBuilder();
            sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n");
            sb.append("<worksheet xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\">");
            sb.append("<sheetPr><pageSetUpPr fitToPage=\"1\"/></sheetPr>");
            sb.append("<sheetViews><sheetView workbookViewId=\"0\"").append(seleccionada ? " tabSelected=\"1\"" : "").append(">");
            if (congelar > 0) {
                sb.append("<pane ySplit=\"").append(congelar).append("\" topLeftCell=\"A").append(congelar + 1)
                        .append("\" activePane=\"bottomLeft\" state=\"frozen\"/>")
                        .append("<selection pane=\"bottomLeft\"/>");
            }
            sb.append("</sheetView></sheetViews>");
            sb.append("<sheetFormatPr defaultRowHeight=\"15\"/>");
            if (anchos.length > 0) {
                sb.append("<cols>");
                for (int i = 0; i < anchos.length; i++) {
                    sb.append("<col min=\"").append(i + 1).append("\" max=\"").append(i + 1)
                            .append("\" width=\"").append(anchos[i]).append("\" customWidth=\"1\"/>");
                }
                sb.append("</cols>");
            }
            sb.append("<sheetData>");
            for (int r = 0; r < filas.size(); r++) {
                List<Celda> fila = filas.get(r);
                if (fila.isEmpty()) {
                    sb.append("<row r=\"").append(r + 1).append("\"/>");
                    continue;
                }
                sb.append("<row r=\"").append(r + 1).append("\">");
                for (int c = 0; c < fila.size(); c++) {
                    sb.append(fila.get(c).xml(letra(c) + (r + 1)));
                }
                sb.append("</row>");
            }
            sb.append("</sheetData>");
            sb.append("<pageMargins left=\"0.5\" right=\"0.5\" top=\"0.75\" bottom=\"0.75\" header=\"0.3\" footer=\"0.3\"/>");
            sb.append("<pageSetup orientation=\"landscape\" fitToWidth=\"1\" fitToHeight=\"0\"/>");
            sb.append("</worksheet>");
            return sb.toString();
        }
    }

    /** Letra de columna: 0 = A, 25 = Z, 26 = AA. */
    public static String letra(int indice) {
        StringBuilder sb = new StringBuilder();
        for (int i = indice + 1; i > 0; i = (i - 1) / 26) {
            sb.insert(0, (char) ('A' + (i - 1) % 26));
        }
        return sb.toString();
    }

    /** Escribe el libro completo en el archivo indicado (lo reemplaza si existe). */
    public static void guardar(File destino, List<Hoja> hojas) throws IOException {
        try (ZipOutputStream zip = new ZipOutputStream(new FileOutputStream(destino), StandardCharsets.UTF_8)) {
            StringBuilder tipos = new StringBuilder();
            tipos.append("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n");
            tipos.append("<Types xmlns=\"http://schemas.openxmlformats.org/package/2006/content-types\">");
            tipos.append("<Default Extension=\"rels\" ContentType=\"application/vnd.openxmlformats-package.relationships+xml\"/>");
            tipos.append("<Default Extension=\"xml\" ContentType=\"application/xml\"/>");
            tipos.append("<Override PartName=\"/xl/workbook.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml\"/>");
            tipos.append("<Override PartName=\"/xl/styles.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.styles+xml\"/>");
            for (int i = 1; i <= hojas.size(); i++) {
                tipos.append("<Override PartName=\"/xl/worksheets/sheet").append(i)
                        .append(".xml\" ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml\"/>");
            }
            tipos.append("</Types>");
            poner(zip, "[Content_Types].xml", tipos.toString());

            poner(zip, "_rels/.rels",
                    "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n"
                    + "<Relationships xmlns=\"http://schemas.openxmlformats.org/package/2006/relationships\">"
                    + "<Relationship Id=\"rId1\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument\" Target=\"xl/workbook.xml\"/>"
                    + "</Relationships>");

            StringBuilder libro = new StringBuilder();
            libro.append("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n");
            libro.append("<workbook xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\" ")
                    .append("xmlns:r=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships\"><sheets>");
            StringBuilder rels = new StringBuilder();
            rels.append("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n");
            rels.append("<Relationships xmlns=\"http://schemas.openxmlformats.org/package/2006/relationships\">");
            for (int i = 0; i < hojas.size(); i++) {
                libro.append("<sheet name=\"").append(esc(hojas.get(i).nombre)).append("\" sheetId=\"").append(i + 1)
                        .append("\" r:id=\"rId").append(i + 1).append("\"/>");
                rels.append("<Relationship Id=\"rId").append(i + 1)
                        .append("\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet\" Target=\"worksheets/sheet")
                        .append(i + 1).append(".xml\"/>");
            }
            libro.append("</sheets></workbook>");
            rels.append("<Relationship Id=\"rId").append(hojas.size() + 1)
                    .append("\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/styles\" Target=\"styles.xml\"/>");
            rels.append("</Relationships>");
            poner(zip, "xl/workbook.xml", libro.toString());
            poner(zip, "xl/_rels/workbook.xml.rels", rels.toString());
            poner(zip, "xl/styles.xml", estilos());
            for (int i = 0; i < hojas.size(); i++) {
                poner(zip, "xl/worksheets/sheet" + (i + 1) + ".xml", hojas.get(i).xml(i == 0));
            }
        }
    }

    private static void poner(ZipOutputStream zip, String nombre, String contenido) throws IOException {
        zip.putNextEntry(new ZipEntry(nombre));
        zip.write(contenido.getBytes(StandardCharsets.UTF_8));
        zip.closeEntry();
    }

    /** Estilos del libro, en el mismo orden que las constantes E_*. Colores de la marca 5CINCO. */
    private static String estilos() {
        String borde = " borderId=\"1\" applyBorder=\"1\"";
        String arriba = "<alignment vertical=\"top\"/>";
        return "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n"
                + "<styleSheet xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\">"
                + "<numFmts count=\"2\">"
                + "<numFmt numFmtId=\"164\" formatCode=\"&quot;$&quot;#,##0.00\"/>"
                + "<numFmt numFmtId=\"165\" formatCode=\"yyyy\\-mm\\-dd\"/>"
                + "</numFmts>"
                + "<fonts count=\"5\">"
                + "<font><sz val=\"11\"/><name val=\"Calibri\"/></font>"
                + "<font><b/><sz val=\"11\"/><name val=\"Calibri\"/></font>"
                + "<font><b/><sz val=\"11\"/><color rgb=\"FFFFFFFF\"/><name val=\"Calibri\"/></font>"
                + "<font><b/><sz val=\"16\"/><color rgb=\"FF242821\"/><name val=\"Calibri\"/></font>"
                + "<font><i/><sz val=\"10\"/><color rgb=\"FF62685C\"/><name val=\"Calibri\"/></font>"
                + "</fonts>"
                + "<fills count=\"4\">"
                + "<fill><patternFill patternType=\"none\"/></fill>"
                + "<fill><patternFill patternType=\"gray125\"/></fill>"
                + "<fill><patternFill patternType=\"solid\"><fgColor rgb=\"FF242821\"/><bgColor indexed=\"64\"/></patternFill></fill>"
                + "<fill><patternFill patternType=\"solid\"><fgColor rgb=\"FFA6CE3A\"/><bgColor indexed=\"64\"/></patternFill></fill>"
                + "</fills>"
                + "<borders count=\"2\">"
                + "<border><left/><right/><top/><bottom/><diagonal/></border>"
                + "<border>"
                + "<left style=\"thin\"><color rgb=\"FFC8CDC2\"/></left><right style=\"thin\"><color rgb=\"FFC8CDC2\"/></right>"
                + "<top style=\"thin\"><color rgb=\"FFC8CDC2\"/></top><bottom style=\"thin\"><color rgb=\"FFC8CDC2\"/></bottom><diagonal/>"
                + "</border>"
                + "</borders>"
                + "<cellStyleXfs count=\"1\"><xf numFmtId=\"0\" fontId=\"0\" fillId=\"0\" borderId=\"0\"/></cellStyleXfs>"
                + "<cellXfs count=\"13\">"
                + "<xf numFmtId=\"0\" fontId=\"0\" fillId=\"0\" borderId=\"0\" xfId=\"0\"/>" // 0
                + "<xf numFmtId=\"0\" fontId=\"3\" fillId=\"0\" borderId=\"0\" xfId=\"0\" applyFont=\"1\"/>" // 1 título
                + "<xf numFmtId=\"0\" fontId=\"4\" fillId=\"0\" borderId=\"0\" xfId=\"0\" applyFont=\"1\"/>" // 2 nota
                + "<xf numFmtId=\"0\" fontId=\"2\" fillId=\"2\"" + borde + " xfId=\"0\" applyFont=\"1\" applyFill=\"1\" applyAlignment=\"1\">"
                + "<alignment horizontal=\"center\" vertical=\"center\" wrapText=\"1\"/></xf>" // 3 encabezado
                + "<xf numFmtId=\"0\" fontId=\"0\" fillId=\"0\"" + borde + " xfId=\"0\" applyAlignment=\"1\">"
                + "<alignment vertical=\"top\" wrapText=\"1\"/></xf>" // 4 texto
                + "<xf numFmtId=\"165\" fontId=\"0\" fillId=\"0\"" + borde + " xfId=\"0\" applyNumberFormat=\"1\" applyAlignment=\"1\">"
                + "<alignment horizontal=\"center\" vertical=\"top\"/></xf>" // 5 fecha
                + "<xf numFmtId=\"164\" fontId=\"0\" fillId=\"0\"" + borde + " xfId=\"0\" applyNumberFormat=\"1\" applyAlignment=\"1\">" + arriba + "</xf>" // 6 dinero
                + "<xf numFmtId=\"10\" fontId=\"0\" fillId=\"0\"" + borde + " xfId=\"0\" applyNumberFormat=\"1\" applyAlignment=\"1\">" + arriba + "</xf>" // 7 porcentaje
                + "<xf numFmtId=\"4\" fontId=\"0\" fillId=\"0\"" + borde + " xfId=\"0\" applyNumberFormat=\"1\" applyAlignment=\"1\">" + arriba + "</xf>" // 8 decimal
                + "<xf numFmtId=\"1\" fontId=\"0\" fillId=\"0\"" + borde + " xfId=\"0\" applyNumberFormat=\"1\" applyAlignment=\"1\">"
                + "<alignment horizontal=\"center\" vertical=\"top\"/></xf>" // 9 entero
                + "<xf numFmtId=\"0\" fontId=\"1\" fillId=\"3\"" + borde + " xfId=\"0\" applyFont=\"1\" applyFill=\"1\"/>" // 10 total texto
                + "<xf numFmtId=\"164\" fontId=\"1\" fillId=\"3\"" + borde + " xfId=\"0\" applyNumberFormat=\"1\" applyFont=\"1\" applyFill=\"1\"/>" // 11 total dinero
                + "<xf numFmtId=\"1\" fontId=\"1\" fillId=\"3\"" + borde + " xfId=\"0\" applyNumberFormat=\"1\" applyFont=\"1\" applyFill=\"1\" applyAlignment=\"1\">"
                + "<alignment horizontal=\"center\"/></xf>" // 12 total entero
                + "</cellXfs>"
                + "<cellStyles count=\"1\"><cellStyle name=\"Normal\" xfId=\"0\" builtinId=\"0\"/></cellStyles>"
                + "</styleSheet>";
    }

    /** Escapa texto para XML y elimina caracteres de control no permitidos. */
    static String esc(String s) {
        StringBuilder sb = new StringBuilder(s.length() + 8);
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '&':
                    sb.append("&amp;");
                    break;
                case '<':
                    sb.append("&lt;");
                    break;
                case '>':
                    sb.append("&gt;");
                    break;
                case '"':
                    sb.append("&quot;");
                    break;
                default:
                    if (c >= 0x20 || c == '\t' || c == '\n' || c == '\r') {
                        sb.append(c);
                    }
            }
        }
        return sb.toString();
    }
}
