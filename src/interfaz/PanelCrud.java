package interfaz;

import datos.Repositorio;
import datos.Sesion;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Window;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.sql.SQLException;
import java.util.regex.Pattern;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.RowFilter;
import javax.swing.SwingUtilities;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;

/** Panel de un módulo: buscar, listar, crear, editar y eliminar registros de una tabla. */
public class PanelCrud extends JPanel {

    private final Modulo modulo;
    private final Sesion sesion;
    private final JTable tabla = new JTable();
    private final JTextField txtBuscar = new JTextField();
    private final JLabel lblConteo = new JLabel(" ");
    private DefaultTableModel modelo;
    private TableRowSorter<DefaultTableModel> sorter;

    public PanelCrud(Modulo modulo, Sesion sesion) {
        super(new BorderLayout(0, 12));
        this.modulo = modulo;
        this.sesion = sesion;
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
        tabla.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2 && tabla.getSelectedRow() >= 0) {
                    editar();
                }
            }
        });
        JScrollPane scroll = new JScrollPane(tabla);
        scroll.setBorder(BorderFactory.createLineBorder(Estilos.BORDE));
        scroll.getViewport().setBackground(java.awt.Color.WHITE);
        add(scroll, BorderLayout.CENTER);

        lblConteo.setFont(Estilos.fuente(Font.PLAIN, 12));
        lblConteo.setForeground(Estilos.GRIS_TEXTO);
        add(lblConteo, BorderLayout.SOUTH);

        recargar();
    }

    private JPanel crearBarra() {
        JPanel barra = new JPanel(new BorderLayout(12, 0));
        barra.setOpaque(false);

        JPanel izq = new JPanel(new BorderLayout(8, 0));
        izq.setOpaque(false);
        JLabel lbl = new JLabel("Buscar:");
        lbl.setFont(Estilos.fuente(Font.BOLD, 13));
        lbl.setForeground(Estilos.CARBON);
        txtBuscar.setFont(Estilos.fuente(Font.PLAIN, 14));
        txtBuscar.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Estilos.BORDE, 1), BorderFactory.createEmptyBorder(6, 8, 6, 8)));
        txtBuscar.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) { filtrar(); }
            public void removeUpdate(DocumentEvent e) { filtrar(); }
            public void changedUpdate(DocumentEvent e) { filtrar(); }
        });
        izq.add(lbl, BorderLayout.WEST);
        izq.add(txtBuscar, BorderLayout.CENTER);

        JPanel der = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        der.setOpaque(false);
        JButton btnNuevo = Estilos.boton("Nuevo", true);
        JButton btnEditar = Estilos.boton("Editar", false);
        JButton btnEliminar = Estilos.boton("Eliminar", false);
        JButton btnActualizar = Estilos.boton("Actualizar", false);
        btnNuevo.addActionListener(e -> nuevo());
        btnEditar.addActionListener(e -> editar());
        btnEliminar.addActionListener(e -> eliminar());
        btnActualizar.addActionListener(e -> recargar());
        der.add(btnNuevo);
        der.add(btnEditar);
        if (esContratos()) {
            JButton btnCodigos = Estilos.boton("Códigos", false);
            JButton btnPdf = Estilos.boton("PDFs", false);
            btnCodigos.addActionListener(e -> codigosDelSeleccionado());
            btnPdf.addActionListener(e -> pdfsDelSeleccionado());
            der.add(btnCodigos);
            der.add(btnPdf);
        }
        if (modulo.puedeEliminar && sesion.esSuperusuario()) {
            der.add(btnEliminar);
        }
        der.add(btnActualizar);

        barra.add(izq, BorderLayout.CENTER);
        barra.add(der, BorderLayout.EAST);
        return barra;
    }

    /** Vuelve a leer los datos de la base. */
    public final void recargar() {
        String sql = (!sesion.esSuperusuario() && modulo.sqlListadoBasico != null)
                ? modulo.sqlListadoBasico : modulo.sqlListado;
        try {
            Repositorio.Resultado r = Repositorio.consultar(sql);
            modelo = new DefaultTableModel(r.columnas.toArray(), 0) {
                @Override
                public boolean isCellEditable(int row, int col) {
                    return false;
                }
            };
            for (Object[] fila : r.filas) {
                modelo.addRow(fila);
            }
            tabla.setModel(modelo);
            sorter = new TableRowSorter<>(modelo);
            tabla.setRowSorter(sorter);
            tabla.removeColumn(tabla.getColumnModel().getColumn(0)); // la llave primaria no se muestra
            Estilos.ajustarColumnas(tabla);
            filtrar();
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, "No se pudieron leer los datos: " + ex.getMessage(),
                    "Error de base de datos", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void filtrar() {
        if (sorter == null) {
            return;
        }
        String t = txtBuscar.getText().trim();
        sorter.setRowFilter(t.isEmpty() ? null : RowFilter.regexFilter("(?i)" + Pattern.quote(t)));
        int total = modelo.getRowCount();
        int vis = tabla.getRowCount();
        lblConteo.setText(t.isEmpty() ? total + " registros"
                : vis + " de " + total + " registros. Doble clic en una fila para editarla.");
    }

    private Window ventana() {
        return SwingUtilities.getWindowAncestor(this);
    }

    private Long idSeleccionado() {
        int fila = tabla.getSelectedRow();
        if (fila < 0) {
            JOptionPane.showMessageDialog(this, "Selecciona primero una fila de la tabla.",
                    modulo.titulo, JOptionPane.INFORMATION_MESSAGE);
            return null;
        }
        Object v = modelo.getValueAt(tabla.convertRowIndexToModel(fila), 0);
        return ((Number) v).longValue();
    }

    private boolean esContratos() {
        return "contratos".equals(modulo.tabla);
    }

    /** Texto «código - objeto» del contrato, para el título de los diálogos. */
    private String nombreContrato(long id) {
        try {
            Object o = Repositorio.escalar("SELECT codigo_interno FROM contratos WHERE id_contrato = ?", id);
            return o == null ? "contrato " + id : o.toString();
        } catch (SQLException ex) {
            return "contrato " + id;
        }
    }

    private void codigosDelSeleccionado() {
        Long id = idSeleccionado();
        if (id != null) {
            abrirCodigos(id);
        }
    }

    private void pdfsDelSeleccionado() {
        Long id = idSeleccionado();
        if (id != null) {
            abrirPdfs(id);
        }
    }

    private void abrirCodigos(long id) {
        new DialogoCodigosContrato(ventana(), id, nombreContrato(id)).setVisible(true);
        recargar();
    }

    private void abrirPdfs(long id) {
        new DialogoPdfContrato(ventana(), id, nombreContrato(id), sesion.esSuperusuario()).setVisible(true);
        recargar();
    }

    private void nuevo() {
        DialogoRegistro d = new DialogoRegistro(ventana(), modulo, null, sesion.esSuperusuario(), null);
        d.setVisible(true);
        if (d.fueGuardado()) {
            recargar();
            if (esContratos() && d.idGuardado() != null) {
                long id = d.idGuardado();
                if (JOptionPane.showConfirmDialog(this, "Contrato guardado.\n¿Quieres agregarle ahora los códigos de compra?",
                        "Códigos de compra", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
                    abrirCodigos(id);
                }
                if (JOptionPane.showConfirmDialog(this, "¿Quieres adjuntar ahora el PDF del contrato?",
                        "PDF del contrato", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
                    abrirPdfs(id);
                }
            }
        }
    }

    private void editar() {
        Long id = idSeleccionado();
        if (id == null) {
            return;
        }
        DialogoRegistro d = new DialogoRegistro(ventana(), modulo, id, sesion.esSuperusuario(), null);
        d.setVisible(true);
        if (d.fueGuardado()) {
            recargar();
        }
    }

    private void eliminar() {
        Long id = idSeleccionado();
        if (id == null) {
            return;
        }
        int op = JOptionPane.showConfirmDialog(this,
                "¿Eliminar este " + modulo.nombreSingular + "? Esta acción no se puede deshacer.",
                "Eliminar", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (op != JOptionPane.YES_OPTION) {
            return;
        }
        try {
            Repositorio.eliminar(modulo.tabla, modulo.pk, id);
            recargar();
        } catch (SQLException ex) {
            String m = ex.getMessage() != null && ex.getMessage().contains("FOREIGN KEY")
                    ? "No se puede eliminar: este " + modulo.nombreSingular + " tiene registros relacionados "
                    + "(por ejemplo contratos, proyectos o asignaciones)."
                    : Repositorio.mensajeAmigable(ex, null);
            JOptionPane.showMessageDialog(this, m, "Eliminar", JOptionPane.WARNING_MESSAGE);
        }
    }
}
