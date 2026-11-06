package interfaz;

import datos.Repositorio;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.Window;
import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.Map;
import javax.swing.BorderFactory;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

/**
 * Códigos de compra (UNSPSC) de un contrato: a la izquierda el catálogo con buscador,
 * a la derecha los códigos que ya tiene el contrato.
 */
public class DialogoCodigosContrato extends JDialog {

    /** Un código del catálogo. */
    private static final class Item {

        final long id;
        final String codigo;
        final String descripcion;

        Item(long id, String codigo, String descripcion) {
            this.id = id;
            this.codigo = codigo;
            this.descripcion = descripcion;
        }

        @Override
        public String toString() {
            return codigo + "  -  " + descripcion;
        }
    }

    private final long idContrato;
    private final DefaultListModel<Item> modeloCatalogo = new DefaultListModel<>();
    private final DefaultListModel<Item> modeloContrato = new DefaultListModel<>();
    private final JList<Item> listaCatalogo = new JList<>(modeloCatalogo);
    private final JList<Item> listaContrato = new JList<>(modeloContrato);
    private final JTextField txtBuscar = new JTextField();

    public DialogoCodigosContrato(Window dueno, long idContrato, String nombreContrato) {
        super(dueno, "Códigos de compra - " + nombreContrato, ModalityType.APPLICATION_MODAL);
        this.idContrato = idContrato;

        JPanel raiz = new JPanel(new BorderLayout(0, 10));
        raiz.setBackground(Estilos.FONDO);
        raiz.setBorder(BorderFactory.createEmptyBorder(16, 20, 12, 20));

        JLabel ayuda = new JLabel("<html>Busca por código o por descripción, selecciona uno o varios y pulsa "
                + "<b>Agregar</b>. Si el código no existe en el catálogo, usa <b>Crear código nuevo</b>.</html>");
        ayuda.setFont(Estilos.fuente(Font.PLAIN, 13));
        ayuda.setForeground(Estilos.GRIS_TEXTO);
        raiz.add(ayuda, BorderLayout.NORTH);

        JPanel centro = new JPanel(new GridLayout(1, 2, 14, 0));
        centro.setOpaque(false);
        centro.add(panelCatalogo());
        centro.add(panelContrato());
        raiz.add(centro, BorderLayout.CENTER);

        JButton cerrar = Estilos.boton("Cerrar", true);
        cerrar.addActionListener(e -> dispose());
        JPanel pie = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        pie.setOpaque(false);
        pie.add(cerrar);
        raiz.add(pie, BorderLayout.SOUTH);

        setContentPane(raiz);
        setSize(new Dimension(980, 560));
        setMinimumSize(new Dimension(760, 420));
        setLocationRelativeTo(dueno);

        cargarCatalogo();
        cargarDelContrato();
    }

    private JPanel panelCatalogo() {
        JPanel p = new JPanel(new BorderLayout(0, 6));
        p.setOpaque(false);

        JLabel titulo = new JLabel("Catálogo de códigos");
        titulo.setFont(Estilos.fuente(Font.BOLD, 14));
        titulo.setForeground(Estilos.CARBON);

        txtBuscar.setFont(Estilos.fuente(Font.PLAIN, 14));
        txtBuscar.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Estilos.BORDE, 1), BorderFactory.createEmptyBorder(6, 8, 6, 8)));
        txtBuscar.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) { cargarCatalogo(); }
            public void removeUpdate(DocumentEvent e) { cargarCatalogo(); }
            public void changedUpdate(DocumentEvent e) { cargarCatalogo(); }
        });
        JPanel arriba = new JPanel(new BorderLayout(0, 6));
        arriba.setOpaque(false);
        arriba.add(titulo, BorderLayout.NORTH);
        arriba.add(txtBuscar, BorderLayout.CENTER);
        p.add(arriba, BorderLayout.NORTH);

        listaCatalogo.setFont(Estilos.fuente(Font.PLAIN, 13));
        listaCatalogo.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        p.add(new JScrollPane(listaCatalogo), BorderLayout.CENTER);

        JButton agregar = Estilos.boton("Agregar al contrato  >", true);
        JButton crear = Estilos.boton("Crear código nuevo", false);
        agregar.addActionListener(e -> agregarSeleccionados());
        crear.addActionListener(e -> crearCodigoNuevo());
        JPanel botones = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        botones.setOpaque(false);
        botones.add(agregar);
        botones.add(crear);
        p.add(botones, BorderLayout.SOUTH);
        return p;
    }

    private JPanel panelContrato() {
        JPanel p = new JPanel(new BorderLayout(0, 6));
        p.setOpaque(false);

        JLabel titulo = new JLabel("Códigos de este contrato");
        titulo.setFont(Estilos.fuente(Font.BOLD, 14));
        titulo.setForeground(Estilos.CARBON);
        p.add(titulo, BorderLayout.NORTH);

        listaContrato.setFont(Estilos.fuente(Font.PLAIN, 13));
        listaContrato.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        p.add(new JScrollPane(listaContrato), BorderLayout.CENTER);

        JButton quitar = Estilos.boton("<  Quitar del contrato", false);
        quitar.addActionListener(e -> quitarSeleccionados());
        JPanel botones = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        botones.setOpaque(false);
        botones.add(quitar);
        p.add(botones, BorderLayout.SOUTH);
        return p;
    }

    // ------------------------------------------------------------------ datos
    private void cargarCatalogo() {
        String t = txtBuscar.getText().trim();
        try {
            Repositorio.Resultado r = Repositorio.consultar(
                    "SELECT id_codigo, codigo, descripcion FROM codigos_compra "
                    + "WHERE codigo LIKE ? OR descripcion LIKE ? ORDER BY codigo LIMIT 500",
                    "%" + t + "%", "%" + t + "%");
            modeloCatalogo.clear();
            for (Object[] f : r.filas) {
                modeloCatalogo.addElement(new Item(((Number) f[0]).longValue(), String.valueOf(f[1]), String.valueOf(f[2])));
            }
        } catch (SQLException ex) {
            error("No se pudo leer el catálogo: " + ex.getMessage());
        }
    }

    private void cargarDelContrato() {
        try {
            Repositorio.Resultado r = Repositorio.consultar(
                    "SELECT cu.id_codigo, cu.codigo, cu.descripcion FROM contrato_codigos cc "
                    + "JOIN codigos_compra cu ON cu.id_codigo = cc.id_codigo "
                    + "WHERE cc.id_contrato = ? ORDER BY cu.codigo", idContrato);
            modeloContrato.clear();
            for (Object[] f : r.filas) {
                modeloContrato.addElement(new Item(((Number) f[0]).longValue(), String.valueOf(f[1]), String.valueOf(f[2])));
            }
        } catch (SQLException ex) {
            error("No se pudieron leer los códigos del contrato: " + ex.getMessage());
        }
    }

    private void agregarSeleccionados() {
        java.util.List<Item> sel = listaCatalogo.getSelectedValuesList();
        if (sel.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Selecciona primero uno o varios códigos del catálogo.",
                    "Códigos de compra", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        try {
            for (Item it : sel) {
                // INSERT OR IGNORE: si el contrato ya tenía el código, no pasa nada
                Repositorio.ejecutar("INSERT OR IGNORE INTO contrato_codigos (id_contrato, id_codigo) VALUES (?, ?)",
                        idContrato, it.id);
            }
            cargarDelContrato();
        } catch (SQLException ex) {
            error(Repositorio.mensajeAmigable(ex, null));
        }
    }

    private void quitarSeleccionados() {
        java.util.List<Item> sel = listaContrato.getSelectedValuesList();
        if (sel.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Selecciona primero el código que quieres quitar del contrato.",
                    "Códigos de compra", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        try {
            for (Item it : sel) {
                Repositorio.ejecutar("DELETE FROM contrato_codigos WHERE id_contrato = ? AND id_codigo = ?",
                        idContrato, it.id);
            }
            cargarDelContrato();
        } catch (SQLException ex) {
            error(Repositorio.mensajeAmigable(ex, null));
        }
    }

    /** Crea un código en el catálogo y lo deja agregado al contrato. */
    private void crearCodigoNuevo() {
        JTextField codigo = new JTextField(txtBuscar.getText().trim().matches("\\d+") ? txtBuscar.getText().trim() : "");
        JTextField descripcion = new JTextField();
        javax.swing.JComboBox<String> tipo = new javax.swing.JComboBox<>(new String[]{"", "bien", "material", "servicio"});
        Object[] campos = {"Código (por ejemplo 72101500):", codigo, "Descripción:", descripcion, "Tipo (opcional):", tipo};
        int op = JOptionPane.showConfirmDialog(this, campos, "Crear código nuevo",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (op != JOptionPane.OK_OPTION) {
            return;
        }
        String c = codigo.getText().trim();
        String d = descripcion.getText().trim();
        if (c.isEmpty() || d.isEmpty()) {
            error("El código y la descripción son obligatorios.");
            return;
        }
        Map<String, Object> v = new LinkedHashMap<>();
        v.put("codigo", c);
        v.put("descripcion", d);
        v.put("tipo", tipo.getSelectedItem() == null || tipo.getSelectedItem().toString().isEmpty() ? null : tipo.getSelectedItem());
        try {
            long id = Repositorio.insertar("codigos_compra", v);
            Repositorio.ejecutar("INSERT OR IGNORE INTO contrato_codigos (id_contrato, id_codigo) VALUES (?, ?)",
                    idContrato, id);
            cargarCatalogo();
            cargarDelContrato();
        } catch (SQLException ex) {
            error(Repositorio.mensajeAmigable(ex, null));
        }
    }

    private void error(String m) {
        JOptionPane.showMessageDialog(this, m, "Códigos de compra", JOptionPane.WARNING_MESSAGE);
    }
}
