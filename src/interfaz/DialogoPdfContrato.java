package interfaz;

import datos.Conexion;
import datos.Repositorio;
import java.awt.BorderLayout;
import java.awt.Desktop;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Window;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.table.DefaultTableModel;

/** PDFs de un contrato guardados dentro de la base de datos: agregar, abrir, guardar copia y eliminar. */
public class DialogoPdfContrato extends JDialog {

    private static final long MAXIMO_BYTES = 50L * 1024 * 1024; // 50 MB por archivo

    private final long idContrato;
    private final boolean puedeEliminar;
    private final JTable tabla = new JTable();
    private DefaultTableModel modelo;

    public DialogoPdfContrato(Window dueno, long idContrato, String nombreContrato, boolean puedeEliminar) {
        super(dueno, "PDFs del contrato - " + nombreContrato, ModalityType.APPLICATION_MODAL);
        this.idContrato = idContrato;
        this.puedeEliminar = puedeEliminar;

        JPanel raiz = new JPanel(new BorderLayout(0, 10));
        raiz.setBackground(Estilos.FONDO);
        raiz.setBorder(BorderFactory.createEmptyBorder(16, 20, 12, 20));

        JLabel ayuda = new JLabel("Los PDFs quedan guardados dentro del archivo 5CINCO.db. "
                + "Puedes agregar varios por contrato (contrato, otrosí, actas...).");
        ayuda.setFont(Estilos.fuente(Font.PLAIN, 13));
        ayuda.setForeground(Estilos.GRIS_TEXTO);
        raiz.add(ayuda, BorderLayout.NORTH);

        Estilos.estilizarTabla(tabla);
        JScrollPane scroll = new JScrollPane(tabla);
        scroll.setBorder(BorderFactory.createLineBorder(Estilos.BORDE));
        scroll.getViewport().setBackground(java.awt.Color.WHITE);
        raiz.add(scroll, BorderLayout.CENTER);

        JButton btnAgregar = Estilos.boton("Agregar PDF", true);
        JButton btnAbrir = Estilos.boton("Abrir", false);
        JButton btnGuardar = Estilos.boton("Guardar copia", false);
        JButton btnEliminar = Estilos.boton("Eliminar", false);
        JButton btnCerrar = Estilos.boton("Cerrar", false);
        btnAgregar.addActionListener(e -> agregar());
        btnAbrir.addActionListener(e -> abrir());
        btnGuardar.addActionListener(e -> guardarCopia());
        btnEliminar.addActionListener(e -> eliminar());
        btnCerrar.addActionListener(e -> dispose());

        JPanel botones = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        botones.setOpaque(false);
        botones.add(btnAgregar);
        botones.add(btnAbrir);
        botones.add(btnGuardar);
        if (puedeEliminar) {
            botones.add(btnEliminar);
        }
        JPanel pie = new JPanel(new BorderLayout());
        pie.setOpaque(false);
        pie.add(botones, BorderLayout.WEST);
        pie.add(btnCerrar, BorderLayout.EAST);
        raiz.add(pie, BorderLayout.SOUTH);

        setContentPane(raiz);
        setSize(new Dimension(820, 460));
        setMinimumSize(new Dimension(640, 340));
        setLocationRelativeTo(dueno);
        recargar();
    }

    private void recargar() {
        try {
            Repositorio.Resultado r = Repositorio.consultar(
                    "SELECT id_documento, nombre_archivo AS \"Archivo\", "
                    + "ROUND(tamano / 1024.0, 1) AS \"Tamaño (KB)\", fecha_carga AS \"Cargado\" "
                    + "FROM contrato_documentos WHERE id_contrato = ? ORDER BY id_documento", idContrato);
            modelo = new DefaultTableModel(r.columnas.toArray(), 0) {
                @Override
                public boolean isCellEditable(int row, int col) {
                    return false;
                }
            };
            for (Object[] f : r.filas) {
                modelo.addRow(f);
            }
            tabla.setModel(modelo);
            tabla.removeColumn(tabla.getColumnModel().getColumn(0)); // el id no se muestra
            Estilos.ajustarColumnas(tabla);
        } catch (SQLException ex) {
            error("No se pudieron leer los PDFs: " + ex.getMessage());
        }
    }

    private Long idSeleccionado() {
        int fila = tabla.getSelectedRow();
        if (fila < 0) {
            JOptionPane.showMessageDialog(this, "Selecciona primero un PDF de la tabla.",
                    "PDFs del contrato", JOptionPane.INFORMATION_MESSAGE);
            return null;
        }
        return ((Number) modelo.getValueAt(tabla.convertRowIndexToModel(fila), 0)).longValue();
    }

    private void agregar() {
        JFileChooser fc = new JFileChooser();
        fc.setDialogTitle("Elige el PDF del contrato");
        fc.setMultiSelectionEnabled(true);
        fc.setFileFilter(new FileNameExtensionFilter("Documentos PDF (*.pdf)", "pdf"));
        if (fc.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) {
            return;
        }
        for (File f : fc.getSelectedFiles()) {
            try {
                if (f.length() > MAXIMO_BYTES) {
                    error(f.getName() + " pesa más de 50 MB y no se guardó.");
                    continue;
                }
                byte[] datos = Files.readAllBytes(f.toPath());
                if (datos.length < 5 || datos[0] != '%' || datos[1] != 'P' || datos[2] != 'D' || datos[3] != 'F') {
                    error(f.getName() + " no parece ser un PDF válido y no se guardó.");
                    continue;
                }
                Repositorio.ejecutar("INSERT INTO contrato_documentos (id_contrato, nombre_archivo, tamano, pdf) "
                        + "VALUES (?, ?, ?, ?)", idContrato, f.getName(), (long) datos.length, datos);
            } catch (IOException | SQLException ex) {
                error("No se pudo guardar " + f.getName() + ": " + ex.getMessage());
            }
        }
        recargar();
    }

    /** Lee el PDF de la base y lo escribe en el archivo indicado. */
    private void volcar(long idDocumento, File destino) throws SQLException, IOException {
        try (Connection cn = Conexion.obtener();
                PreparedStatement ps = cn.prepareStatement("SELECT pdf FROM contrato_documentos WHERE id_documento = ?")) {
            ps.setLong(1, idDocumento);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    throw new SQLException("El documento ya no existe.");
                }
                Files.write(destino.toPath(), rs.getBytes(1));
            }
        }
    }

    private String nombreSeleccionado() {
        return String.valueOf(modelo.getValueAt(tabla.convertRowIndexToModel(tabla.getSelectedRow()), 1));
    }

    private void abrir() {
        Long id = idSeleccionado();
        if (id == null) {
            return;
        }
        try {
            File temporal = File.createTempFile("contrato_", ".pdf");
            temporal.deleteOnExit();
            volcar(id, temporal);
            if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.OPEN)) {
                Desktop.getDesktop().open(temporal);
            } else {
                error("Este equipo no puede abrir PDFs desde la aplicación. Usa «Guardar copia».");
            }
        } catch (IOException | SQLException ex) {
            error("No se pudo abrir el PDF: " + ex.getMessage());
        }
    }

    private void guardarCopia() {
        Long id = idSeleccionado();
        if (id == null) {
            return;
        }
        JFileChooser fc = new JFileChooser();
        fc.setSelectedFile(new File(nombreSeleccionado()));
        if (fc.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) {
            return;
        }
        File destino = fc.getSelectedFile();
        if (!destino.getName().toLowerCase().endsWith(".pdf")) {
            destino = new File(destino.getParentFile(), destino.getName() + ".pdf");
        }
        if (destino.exists() && JOptionPane.showConfirmDialog(this, "El archivo ya existe. ¿Reemplazarlo?",
                "Guardar copia", JOptionPane.YES_NO_OPTION) != JOptionPane.YES_OPTION) {
            return;
        }
        try {
            volcar(id, destino);
            JOptionPane.showMessageDialog(this, "Copia guardada en:\n" + destino.getAbsolutePath(),
                    "Guardar copia", JOptionPane.INFORMATION_MESSAGE);
        } catch (IOException | SQLException ex) {
            error("No se pudo guardar la copia: " + ex.getMessage());
        }
    }

    private void eliminar() {
        Long id = idSeleccionado();
        if (id == null) {
            return;
        }
        if (JOptionPane.showConfirmDialog(this, "¿Eliminar este PDF de la base de datos? No se puede deshacer.",
                "Eliminar PDF", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE) != JOptionPane.YES_OPTION) {
            return;
        }
        try {
            Repositorio.eliminar("contrato_documentos", "id_documento", id);
            recargar();
        } catch (SQLException ex) {
            error(Repositorio.mensajeAmigable(ex, null));
        }
    }

    private void error(String m) {
        JOptionPane.showMessageDialog(this, m, "PDFs del contrato", JOptionPane.WARNING_MESSAGE);
    }
}
