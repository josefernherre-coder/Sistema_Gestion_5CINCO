package interfaz;

import datos.Repositorio;
import java.awt.BorderLayout;
import java.awt.Desktop;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.text.ParseException;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Calendar;
import java.util.Date;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTable;
import javax.swing.SpinnerDateModel;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import reportes.ReporteContratos;

/**
 * Reportes: el administrador elige un año o un rango de fechas, ve los contratos del periodo
 * con su total y puede exportarlos a un archivo de Excel.
 */
public class PanelReporte extends JPanel {

    private static final String PERSONALIZADO = "Personalizado";

    private final JSpinner spDesde = crearSelector();
    private final JSpinner spHasta = crearSelector();
    private final JComboBox<String> cbAnio = new JComboBox<>();
    private final JTable tabla = new JTable();
    private final JLabel lblResumen = new JLabel(" ");
    private boolean ajustando = false;

    public PanelReporte() {
        super(new BorderLayout(0, 12));
        setBackground(Estilos.FONDO);
        setBorder(BorderFactory.createEmptyBorder(20, 28, 20, 28));

        add(crearBarra(), BorderLayout.NORTH);

        Estilos.estilizarTabla(tabla);
        tabla.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override
            protected void setValue(Object v) {
                if (v instanceof Number) {
                    setText(Estilos.numero(v));
                    setHorizontalAlignment(RIGHT);
                } else {
                    setText(v == null ? "" : v.toString());
                    setHorizontalAlignment(LEFT);
                }
            }
        });
        JScrollPane scroll = new JScrollPane(tabla);
        scroll.setBorder(BorderFactory.createLineBorder(Estilos.BORDE));
        scroll.getViewport().setBackground(java.awt.Color.WHITE);
        add(scroll, BorderLayout.CENTER);

        JPanel pie = new JPanel(new BorderLayout(0, 4));
        pie.setOpaque(false);
        lblResumen.setFont(Estilos.fuente(Font.BOLD, 15));
        lblResumen.setForeground(Estilos.CARBON);
        JLabel nota = new JLabel("Entran los contratos cuya fecha de terminación (o de inicio, si no tienen terminación) "
                + "está dentro del periodo. El total usa el valor de participación.");
        nota.setFont(Estilos.fuente(Font.ITALIC, 12));
        nota.setForeground(Estilos.GRIS_TEXTO);
        pie.add(lblResumen, BorderLayout.NORTH);
        pie.add(nota, BorderLayout.SOUTH);
        add(pie, BorderLayout.SOUTH);

        cargarAnios();
    }

    private JPanel crearBarra() {
        JPanel barra = new JPanel(new BorderLayout(12, 0));
        barra.setOpaque(false);

        JPanel izq = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        izq.setOpaque(false);
        izq.add(etiqueta("Año:"));
        cbAnio.setFont(Estilos.fuente(Font.PLAIN, 14));
        izq.add(cbAnio);
        izq.add(etiqueta("Desde:"));
        izq.add(spDesde);
        izq.add(etiqueta("Hasta:"));
        izq.add(spHasta);

        JPanel der = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        der.setOpaque(false);
        JButton btnConsultar = Estilos.boton("Consultar", false);
        JButton btnExportar = Estilos.boton("Exportar a Excel", true);
        btnConsultar.addActionListener(e -> consultar());
        btnExportar.addActionListener(e -> exportar());
        der.add(btnConsultar);
        der.add(btnExportar);

        barra.add(izq, BorderLayout.CENTER);
        barra.add(der, BorderLayout.EAST);

        cbAnio.addActionListener(e -> {
            Object sel = cbAnio.getSelectedItem();
            if (ajustando || sel == null || PERSONALIZADO.equals(sel)) {
                return;
            }
            int anio = Integer.parseInt(sel.toString());
            ajustando = true;
            poner(spDesde, LocalDate.of(anio, 1, 1));
            poner(spHasta, LocalDate.of(anio, 12, 31));
            ajustando = false;
            consultar();
        });
        return barra;
    }

    private JLabel etiqueta(String texto) {
        JLabel l = new JLabel(texto);
        l.setFont(Estilos.fuente(Font.BOLD, 13));
        l.setForeground(Estilos.CARBON);
        return l;
    }

    private static JSpinner crearSelector() {
        JSpinner s = new JSpinner(new SpinnerDateModel(new Date(), null, null, Calendar.DAY_OF_MONTH));
        s.setEditor(new JSpinner.DateEditor(s, "yyyy-MM-dd"));
        s.setFont(Estilos.fuente(Font.PLAIN, 14));
        s.setPreferredSize(new Dimension(130, 32));
        return s;
    }

    /** Llena la lista de años (del primero con contratos hasta el actual) y elige el año actual. */
    private void cargarAnios() {
        int actual = LocalDate.now().getYear();
        int primero = actual;
        try {
            Object min = Repositorio.escalar("SELECT MIN(CAST(strftime('%Y', COALESCE(fecha_terminacion, fecha_inicio)) AS INTEGER)) "
                    + "FROM contratos");
            if (min instanceof Number) {
                primero = Math.min(actual, ((Number) min).intValue());
            }
        } catch (SQLException ex) {
            // si falla, queda solo el año actual
        }
        ajustando = true;
        cbAnio.addItem(PERSONALIZADO);
        for (int a = actual; a >= primero; a--) {
            cbAnio.addItem(String.valueOf(a));
        }
        ajustando = false;
        cbAnio.setSelectedItem(String.valueOf(actual)); // dispara la consulta del año actual
    }

    // ---------------------------------------------------------------- fechas
    private static LocalDate aFecha(Date d) {
        return d.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
    }

    private static void poner(JSpinner s, LocalDate f) {
        s.setValue(Date.from(f.atStartOfDay(ZoneId.systemDefault()).toInstant()));
    }

    /** Lee la fecha del selector (aceptando lo que se haya escrito a mano) o null si no es válida. */
    private LocalDate leer(JSpinner s, String nombre) {
        try {
            s.commitEdit();
        } catch (ParseException ex) {
            JOptionPane.showMessageDialog(this, "La fecha «" + nombre + "» no es válida. Usa el formato AAAA-MM-DD "
                    + "(ejemplo: 2026-01-31).", "Reportes", JOptionPane.WARNING_MESSAGE);
            return null;
        }
        return aFecha((Date) s.getValue());
    }

    /** Lee y valida el rango; devuelve {desde, hasta} o null si hay un problema (ya avisó al usuario). */
    private LocalDate[] leerRango() {
        LocalDate desde = leer(spDesde, "Desde");
        LocalDate hasta = desde == null ? null : leer(spHasta, "Hasta");
        if (desde == null || hasta == null) {
            return null;
        }
        if (hasta.isBefore(desde)) {
            JOptionPane.showMessageDialog(this, "La fecha «Hasta» no puede ser anterior a la fecha «Desde».",
                    "Reportes", JOptionPane.WARNING_MESSAGE);
            return null;
        }
        if (!ajustando && !esAnioCompleto(desde, hasta)) {
            ajustando = true;
            cbAnio.setSelectedItem(PERSONALIZADO);
            ajustando = false;
        }
        return new LocalDate[]{desde, hasta};
    }

    private static boolean esAnioCompleto(LocalDate d, LocalDate h) {
        return d.getYear() == h.getYear() && d.getDayOfYear() == 1 && h.getDayOfYear() == h.lengthOfYear();
    }

    // ---------------------------------------------------------------- consulta y exportación
    private void consultar() {
        LocalDate[] rango = leerRango();
        if (rango == null) {
            return;
        }
        try {
            Repositorio.Resultado r = ReporteContratos.consultar(rango[0], rango[1]);
            DefaultTableModel modelo = new DefaultTableModel(r.columnas.toArray(), 0) {
                @Override
                public boolean isCellEditable(int row, int col) {
                    return false;
                }
            };
            for (Object[] fila : r.filas) {
                modelo.addRow(fila);
            }
            tabla.setModel(modelo);
            Estilos.ajustarColumnas(tabla);

            int n = r.filas.size();
            if (n == 0) {
                lblResumen.setText("No hay contratos en el periodo " + rango[0] + " a " + rango[1] + ".");
            } else {
                BigDecimal contratos = ReporteContratos.sumar(r, ReporteContratos.COL_VALOR_CONTRATO);
                BigDecimal participacion = ReporteContratos.sumar(r, ReporteContratos.COL_VALOR_PARTICIPACION);
                lblResumen.setText(n + (n == 1 ? " contrato" : " contratos") + "   |   Valor de los contratos: $ "
                        + Estilos.numero(contratos) + "   |   Total valor participación: $ " + Estilos.numero(participacion));
            }
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, "No se pudo consultar el reporte: " + ex.getMessage(),
                    "Error de base de datos", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void exportar() {
        LocalDate[] rango = leerRango();
        if (rango == null) {
            return;
        }
        JFileChooser fc = new JFileChooser();
        fc.setDialogTitle("Guardar reporte en Excel");
        fc.setFileFilter(new FileNameExtensionFilter("Libro de Excel (*.xlsx)", "xlsx"));
        fc.setSelectedFile(new File("Reporte_contratos_" + rango[0] + "_a_" + rango[1] + ".xlsx"));
        if (fc.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) {
            return;
        }
        File destino = fc.getSelectedFile();
        if (!destino.getName().toLowerCase().endsWith(".xlsx")) {
            destino = new File(destino.getParentFile(), destino.getName() + ".xlsx");
        }
        if (destino.exists() && JOptionPane.showConfirmDialog(this,
                "El archivo ya existe. ¿Quieres reemplazarlo?", "Reportes",
                JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE) != JOptionPane.YES_OPTION) {
            return;
        }
        try {
            ReporteContratos.exportar(destino, rango[0], rango[1]);
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, "No se pudo leer la información: " + ex.getMessage(),
                    "Error de base de datos", JOptionPane.ERROR_MESSAGE);
            return;
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(this, "No se pudo guardar el archivo. Si lo tienes abierto en Excel, "
                    + "ciérralo e inténtalo de nuevo.\n\n" + ex.getMessage(), "Reportes", JOptionPane.ERROR_MESSAGE);
            return;
        }
        int abrir = JOptionPane.showConfirmDialog(this, "Reporte guardado en:\n" + destino.getAbsolutePath()
                + "\n\n¿Quieres abrirlo ahora?", "Reportes", JOptionPane.YES_NO_OPTION, JOptionPane.INFORMATION_MESSAGE);
        if (abrir == JOptionPane.YES_OPTION && Desktop.isDesktopSupported()
                && Desktop.getDesktop().isSupported(Desktop.Action.OPEN)) {
            try {
                Desktop.getDesktop().open(destino);
            } catch (IOException ex) {
                JOptionPane.showMessageDialog(this, "No se pudo abrir el archivo: " + ex.getMessage(),
                        "Reportes", JOptionPane.WARNING_MESSAGE);
            }
        }
    }
}
