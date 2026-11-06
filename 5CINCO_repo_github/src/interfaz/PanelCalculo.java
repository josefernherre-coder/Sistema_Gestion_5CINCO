package interfaz;

import datos.Repositorio;
import datos.Sesion;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.Window;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import javax.swing.BorderFactory;
import javax.swing.DefaultCellEditor;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.JTable;
import javax.swing.SwingUtilities;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;

/**
 * Calcular valores (RF_016): líneas de productos y servicios de un proyecto o contrato
 * con subtotal, IVA, retenciones y total. Los cálculos los hacen las vistas de la base.
 */
public class PanelCalculo extends JPanel {

    private static final String PROYECTO = "Proyecto";
    private static final String CONTRATO = "Contrato";

    private final Sesion sesion;
    private final JComboBox<String> cbTipo = new JComboBox<>(new String[]{PROYECTO, CONTRATO});
    private final JComboBox<DialogoRegistro.Opcion> cbElemento = new JComboBox<>();
    private final JTable tabla = new JTable();
    private DefaultTableModel modelo;
    private final JLabel lblInfo = new JLabel(" ");
    private final Map<String, JLabel> totales = new LinkedHashMap<>();
    private boolean cargando = false;

    /** Columnas del modelo (la 0 es el id de la línea y no se muestra). */
    private static final int C_CANTIDAD = 3, C_PRECIO = 4, C_DESC_PCT = 6, C_IVA_PCT = 8,
            C_RETEFUENTE_PCT = 10, C_RETEICA_PCT = 12, C_RETEIVA_PCT = 14;

    /** Columnas que se pueden editar directamente en la tabla -> columna de la tabla detalle. */
    private static final Map<Integer, String> COLUMNA_BD = new HashMap<>();

    static {
        COLUMNA_BD.put(C_CANTIDAD, "cantidad");
        COLUMNA_BD.put(C_PRECIO, "precio_unitario");
        COLUMNA_BD.put(C_DESC_PCT, "descuento_pct");
        COLUMNA_BD.put(C_IVA_PCT, "iva_pct");
        COLUMNA_BD.put(C_RETEFUENTE_PCT, "retefuente_pct");
        COLUMNA_BD.put(C_RETEICA_PCT, "reteica_pct");
        COLUMNA_BD.put(C_RETEIVA_PCT, "reteiva_pct");
    }

    private static final Color COLOR_EDITABLE = new Color(255, 252, 228);

    /** A qué se le guardan las líneas: un proyecto o un contrato (o el proyecto de origen del contrato). */
    private static final class Objetivo {

        final String col;
        final long id;
        final String nota;

        Objetivo(String col, long id, String nota) {
            this.col = col;
            this.id = id;
            this.nota = nota;
        }
    }

    private static final String[] ETIQUETAS_TOTALES = {
        "Subtotal", "Descuento", "IVA", "Total con IVA", "Retefuente", "ReteICA", "ReteIVA", "Neto a recibir"
    };

    public PanelCalculo(Sesion sesion) {
        super(new BorderLayout(0, 12));
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

            @Override
            public Component getTableCellRendererComponent(JTable t, Object v, boolean sel, boolean foco, int fila, int col) {
                Component c = super.getTableCellRendererComponent(t, v, sel, foco, fila, col);
                if (!sel) {
                    // las celdas que se pueden editar van en un amarillo suave
                    c.setBackground(COLUMNA_BD.containsKey(t.convertColumnIndexToModel(col)) ? COLOR_EDITABLE : Color.WHITE);
                }
                return c;
            }
        });
        DefaultCellEditor editor = new DefaultCellEditor(new JTextField()) {
            @Override
            public Component getTableCellEditorComponent(JTable t, Object v, boolean sel, int fila, int col) {
                Component c = super.getTableCellEditorComponent(t, v instanceof Number ? plano((Number) v) : v, sel, fila, col);
                ((JTextField) c).setHorizontalAlignment(JTextField.RIGHT);
                ((JTextField) c).selectAll();
                return c;
            }
        };
        editor.setClickCountToStart(1);
        tabla.setDefaultEditor(Object.class, editor);
        JScrollPane scroll = new JScrollPane(tabla);
        scroll.setBorder(BorderFactory.createLineBorder(Estilos.BORDE));
        scroll.getViewport().setBackground(java.awt.Color.WHITE);

        JPanel centro = new JPanel(new BorderLayout(0, 12));
        centro.setOpaque(false);
        centro.add(scroll, BorderLayout.CENTER);
        centro.add(crearTotales(), BorderLayout.SOUTH);
        add(centro, BorderLayout.CENTER);

        lblInfo.setFont(Estilos.fuente(Font.PLAIN, 13));
        lblInfo.setForeground(Estilos.GRIS_TEXTO);
        add(lblInfo, BorderLayout.SOUTH);

        cbTipo.addActionListener(e -> cargarElementos());
        cbElemento.addActionListener(e -> {
            if (!cargando) {
                cargarLineas();
            }
        });
        cargarElementos();
    }

    private JPanel crearBarra() {
        JPanel barra = new JPanel(new BorderLayout(12, 0));
        barra.setOpaque(false);
        JPanel izq = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        izq.setOpaque(false);
        JLabel l1 = new JLabel("Calcular sobre:");
        l1.setFont(Estilos.fuente(Font.BOLD, 13));
        cbTipo.setFont(Estilos.fuente(Font.PLAIN, 14));
        cbElemento.setFont(Estilos.fuente(Font.PLAIN, 14));
        cbElemento.setPrototypeDisplayValue(new DialogoRegistro.Opcion(0L, "PRY-0000 - nombre de proyecto largo para el cuadro"));
        izq.add(l1);
        izq.add(cbTipo);
        izq.add(cbElemento);

        JPanel der = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        der.setOpaque(false);
        JButton btnAgregar = Estilos.boton("Agregar línea", true);
        JButton btnEditar = Estilos.boton("Editar línea", false);
        JButton btnEliminar = Estilos.boton("Eliminar línea", false);
        JButton btnActualizar = Estilos.boton("Actualizar", false);
        btnAgregar.addActionListener(e -> agregar());
        btnEditar.addActionListener(e -> editar());
        btnEliminar.addActionListener(e -> eliminar());
        btnActualizar.addActionListener(e -> cargarLineas());
        der.add(btnAgregar);
        der.add(btnEditar);
        der.add(btnEliminar);
        der.add(btnActualizar);
        barra.add(izq, BorderLayout.CENTER);
        barra.add(der, BorderLayout.EAST);
        return barra;
    }

    private JPanel crearTotales() {
        JPanel p = new JPanel(new GridLayout(2, 4, 12, 12));
        p.setOpaque(false);
        for (String et : ETIQUETAS_TOTALES) {
            JLabel valor = new JLabel("0");
            valor.setFont(Estilos.fuente(Font.BOLD, 20));
            valor.setForeground(Estilos.CARBON);
            JLabel etiqueta = new JLabel(et);
            etiqueta.setFont(Estilos.fuente(Font.PLAIN, 13));
            etiqueta.setForeground(Estilos.GRIS_TEXTO);
            JPanel t = new JPanel();
            t.setLayout(new javax.swing.BoxLayout(t, javax.swing.BoxLayout.Y_AXIS));
            t.setBackground(java.awt.Color.WHITE);
            t.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(Estilos.BORDE, 1),
                    BorderFactory.createCompoundBorder(
                            BorderFactory.createMatteBorder(3, 0, 0, 0, et.equals("Neto a recibir") ? Estilos.VERDE_OSCURO : Estilos.VERDE),
                            BorderFactory.createEmptyBorder(8, 14, 8, 14))));
            t.add(valor);
            t.add(etiqueta);
            totales.put(et, valor);
            p.add(t);
        }
        return p;
    }

    // ---------------------------------------------------------------- datos
    private boolean esProyecto() {
        return PROYECTO.equals(cbTipo.getSelectedItem());
    }

    private void cargarElementos() {
        cargando = true;
        cbElemento.removeAllItems();
        try {
            String sql = esProyecto() ? Modulos.REF_PROYECTOS_PUBLICO : Modulos.REF_CONTRATOS;
            for (Object[] f : Repositorio.consultar(sql).filas) {
                cbElemento.addItem(new DialogoRegistro.Opcion(((Number) f[0]).longValue(), String.valueOf(f[1])));
            }
        } catch (SQLException ex) {
            error("No se pudo leer la lista: " + ex.getMessage());
        }
        cargando = false;
        cargarLineas();
    }

    private Long idElemento() {
        DialogoRegistro.Opcion o = (DialogoRegistro.Opcion) cbElemento.getSelectedItem();
        return o == null ? null : o.id;
    }

    /**
     * Resuelve dónde viven las líneas. En un proyecto son las suyas. En un contrato: si salió de un proyecto
     * (campo "Proyecto de origen") se muestran las líneas de ese proyecto, igual que al elegir el proyecto;
     * si no, el contrato usa sus propias líneas.
     */
    private Objetivo objetivo() throws SQLException {
        Long id = idElemento();
        if (id == null) {
            return null;
        }
        if (esProyecto()) {
            return new Objetivo("id_proyecto", id, null);
        }
        Repositorio.Resultado r = Repositorio.consultar(
                "SELECT c.id_proyecto, p.codigo, p.nombre, "
                + "(SELECT COUNT(*) FROM detalle d WHERE d.id_contrato = c.id_contrato) "
                + "FROM contratos c LEFT JOIN proyectos p ON p.id_proyecto = c.id_proyecto WHERE c.id_contrato = ?", id);
        if (!r.filas.isEmpty() && r.filas.get(0)[0] != null) {
            Object[] f = r.filas.get(0);
            String nota = "Líneas del proyecto " + (f[1] == null ? "" : f[1] + " - ") + f[2]
                    + " (proyecto de origen de este contrato; los cambios se guardan en el proyecto).";
            long propias = ((Number) f[3]).longValue();
            if (propias > 0) {
                nota += " El contrato también tiene " + propias + " línea(s) propia(s) que no se muestran.";
            }
            return new Objetivo("id_proyecto", ((Number) f[0]).longValue(), nota);
        }
        return new Objetivo("id_contrato", id, null);
    }

    public final void cargarLineas() {
        for (JLabel l : totales.values()) {
            l.setText("0");
        }
        Long seleccionada = lineaSeleccionada();
        Objetivo o;
        try {
            o = objetivo();
        } catch (SQLException ex) {
            error("No se pudo leer el " + (esProyecto() ? "proyecto" : "contrato") + ": " + ex.getMessage());
            return;
        }
        if (o == null) {
            tabla.setModel(new DefaultTableModel());
            lblInfo.setText("No hay " + (esProyecto() ? "proyectos" : "contratos") + " registrados.");
            return;
        }
        try {
            Repositorio.Resultado r = Repositorio.consultar(
                    "SELECT d.id_detalle, v.producto AS \"Producto o servicio\", v.unidad_medida AS \"Unidad\", "
                    + "d.cantidad AS \"Cantidad\", d.precio_unitario AS \"Precio unitario\", v.subtotal AS \"Subtotal\", "
                    + "d.descuento_pct AS \"Desc. %\", v.descuento AS \"Descuento\", "
                    + "d.iva_pct AS \"IVA %\", v.iva AS \"IVA\", "
                    + "d.retefuente_pct AS \"Retef. %\", v.retefuente AS \"Retefuente\", "
                    + "d.reteica_pct AS \"ReteICA %\", v.reteica AS \"ReteICA\", "
                    + "d.reteiva_pct AS \"ReteIVA %\", v.reteiva AS \"ReteIVA\" "
                    + "FROM detalle d JOIN v_detalle v ON v.id_detalle = d.id_detalle "
                    + "WHERE d." + o.col + " = ? ORDER BY d.id_detalle", o.id);
            modelo = new DefaultTableModel(r.columnas.toArray(), 0) {
                @Override
                public boolean isCellEditable(int row, int c) {
                    return COLUMNA_BD.containsKey(c);
                }

                /** No cambia la celda directamente: guarda en la base y vuelve a calcular todo. */
                @Override
                public void setValueAt(Object nuevo, int fila, int c) {
                    if (COLUMNA_BD.containsKey(c)) {
                        guardarCelda(this, fila, c, nuevo);
                    } else {
                        super.setValueAt(nuevo, fila, c);
                    }
                }
            };
            for (Object[] f : r.filas) {
                modelo.addRow(f);
            }
            tabla.setModel(modelo);
            tabla.removeColumn(tabla.getColumnModel().getColumn(0));
            Estilos.ajustarColumnas(tabla);
            if (seleccionada != null) {
                for (int i = 0; i < modelo.getRowCount(); i++) {
                    if (((Number) modelo.getValueAt(i, 0)).longValue() == seleccionada) {
                        tabla.setRowSelectionInterval(i, i);
                    }
                }
            }

            String vista = o.col.equals("id_proyecto") ? "v_totales_proyecto" : "v_totales_contrato";
            Repositorio.Resultado t = Repositorio.consultar("SELECT subtotal, descuento, iva, total_con_iva, retefuente, "
                    + "reteica, reteiva, neto_a_recibir FROM " + vista + " WHERE " + o.col + " = ?", o.id);
            if (!t.filas.isEmpty()) {
                int i = 0;
                for (String et : ETIQUETAS_TOTALES) {
                    Object v = t.filas.get(0)[i++];
                    totales.get(et).setText(v == null ? "0" : "$ " + Estilos.numero(v));
                }
            }
            boolean contratoSinLineas = !esProyecto() && o.col.equals("id_contrato") && t.filas.isEmpty();
            if (contratoSinLineas) {
                // sin detalle de líneas: los totales muestran el valor registrado del contrato (no 0)
                Repositorio.Resultado vr = Repositorio.consultar(
                        "SELECT valor_contrato FROM contratos WHERE id_contrato = ?", o.id);
                if (!vr.filas.isEmpty() && vr.filas.get(0)[0] != null) {
                    String v = "$ " + Estilos.numero(vr.filas.get(0)[0]);
                    totales.get("Total con IVA").setText(v);
                    totales.get("Neto a recibir").setText(v);
                }
            }
            String info;
            if (r.filas.isEmpty()) {
                info = contratoSinLineas
                        ? "Este contrato no tiene detalle de líneas: los totales muestran su valor registrado. "
                        + "Usa «Agregar línea» para detallar productos o servicios."
                        : "Sin líneas todavía. Usa «Agregar línea» para registrar productos o servicios."
                        + (!esProyecto() && o.nota == null
                        ? " (También puedes vincular un proyecto en Contratos > «Proyecto de origen».)" : "");
            } else {
                info = r.filas.size() + " líneas. Haz clic en una celda amarilla para editarla.";
            }
            if (o.nota != null) {
                info = o.nota + "  " + info;
            }
            if (!esProyecto()) {
                Repositorio.Resultado c = Repositorio.consultar(
                        "SELECT valor_contrato, valor_smmlv_participacion, anio_smmlv FROM v_contratos WHERE id_contrato = ?", idElemento());
                if (!c.filas.isEmpty()) {
                    Object[] f = c.filas.get(0);
                    info += "   Valor registrado del contrato: $ " + Estilos.numero(f[0])
                            + (f[1] != null ? "   |   Valor en SMMLV (sobre la participación, año " + f[2] + "): "
                            + Estilos.numero(f[1]) : "");
                }
            }
            lblInfo.setText(info);
        } catch (SQLException ex) {
            error("No se pudieron calcular los valores: " + ex.getMessage());
        }
    }

    /** id_detalle de la línea seleccionada (o null). */
    private Long lineaSeleccionada() {
        int fila = tabla.getSelectedRow();
        if (fila < 0 || modelo == null || fila >= modelo.getRowCount()) {
            return null;
        }
        Object v = modelo.getValueAt(tabla.convertRowIndexToModel(fila), 0);
        return v instanceof Number ? ((Number) v).longValue() : null;
    }

    // ------------------------------------------------- edición en la tabla
    private void guardarCelda(DefaultTableModel m, int fila, int colModelo, Object nuevo) {
        String columna = COLUMNA_BD.get(colModelo);
        Double valor = leerNumero(String.valueOf(nuevo));
        if (valor == null) {
            aviso("Escribe un número válido (por ejemplo 12, 850000 o 19,5).");
            return;
        }
        Object viejo = m.getValueAt(fila, colModelo);
        if (viejo instanceof Number && Math.abs(((Number) viejo).doubleValue() - valor) < 1e-9) {
            return; // no cambió nada
        }
        String problema = validar(columna, valor);
        if (problema != null) {
            aviso(problema);
            return;
        }
        try {
            long idDetalle = ((Number) m.getValueAt(fila, 0)).longValue();
            Repositorio.ejecutar("UPDATE detalle SET " + columna + " = ? WHERE id_detalle = ?", valor, idDetalle);
            SwingUtilities.invokeLater(this::cargarLineas); // recalcula subtotal, impuestos y totales
        } catch (SQLException ex) {
            error(Repositorio.mensajeAmigable(ex, "La cantidad debe ser mayor que 0 y los porcentajes deben estar entre 0 y 100."));
        }
    }

    private static String validar(String columna, double v) {
        if (columna.equals("cantidad")) {
            return v > 0 ? null : "La cantidad debe ser mayor que 0.";
        }
        if (columna.equals("precio_unitario")) {
            return v >= 0 ? null : "El precio no puede ser negativo.";
        }
        return v >= 0 && v <= 100 ? null : "El porcentaje debe estar entre 0 y 100.";
    }

    /** Lee un número escrito a la colombiana (1.500.000 o 19,5) o a la inglesa (19.5). */
    static Double leerNumero(String texto) {
        String t = texto == null ? "" : texto.trim().replace("$", "").replace(" ", "");
        if (t.isEmpty()) {
            return null;
        }
        if (t.contains(",")) {
            t = t.replace(".", "").replace(",", ".");
        } else if (t.matches("\\d{1,3}(\\.\\d{3})+")) {
            t = t.replace(".", "");
        }
        try {
            return Double.valueOf(t);
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    /** Número sin ceros ni notación científica, para mostrarlo en el editor. */
    private static String plano(Number n) {
        return java.math.BigDecimal.valueOf(n.doubleValue()).stripTrailingZeros().toPlainString();
    }

    private void aviso(String m) {
        JOptionPane.showMessageDialog(this, m, "Calcular valores", JOptionPane.WARNING_MESSAGE);
    }

    private Window ventana() {
        return SwingUtilities.getWindowAncestor(this);
    }

    private void agregar() {
        Objetivo o = objetivoSeguro();
        if (o == null) {
            JOptionPane.showMessageDialog(this, "Primero elige un proyecto o contrato.", "Calcular valores",
                    JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        Map<String, Object> fijos = new LinkedHashMap<>();
        fijos.put(o.col, o.id);
        DialogoRegistro d = new DialogoRegistro(ventana(), Modulos.lineaDetalle(), null, sesion.esSuperusuario(), fijos);
        d.setVisible(true);
        if (d.fueGuardado()) {
            cargarLineas();
        }
    }

    /** Edita la línea seleccionada en un formulario (sirve también para cambiar el producto o servicio). */
    private void editar() {
        Long idDetalle = lineaSeleccionada();
        if (idDetalle == null) {
            JOptionPane.showMessageDialog(this, "Selecciona primero una línea.", "Calcular valores",
                    JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        Objetivo o = objetivoSeguro();
        Map<String, Object> fijos = new LinkedHashMap<>();
        if (o != null) {
            fijos.put(o.col, o.id);
        }
        DialogoRegistro d = new DialogoRegistro(ventana(), Modulos.lineaDetalle(), idDetalle, sesion.esSuperusuario(), fijos);
        d.setVisible(true);
        if (d.fueGuardado()) {
            cargarLineas();
        }
    }

    private Objetivo objetivoSeguro() {
        try {
            return objetivo();
        } catch (SQLException ex) {
            error("No se pudo leer el " + (esProyecto() ? "proyecto" : "contrato") + ": " + ex.getMessage());
            return null;
        }
    }

    private void eliminar() {
        int fila = tabla.getSelectedRow();
        if (fila < 0) {
            JOptionPane.showMessageDialog(this, "Selecciona primero una línea.", "Calcular valores",
                    JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        int op = JOptionPane.showConfirmDialog(this, "¿Eliminar esta línea?", "Eliminar", JOptionPane.YES_NO_OPTION);
        if (op != JOptionPane.YES_OPTION) {
            return;
        }
        try {
            long idDetalle = ((Number) modelo.getValueAt(tabla.convertRowIndexToModel(fila), 0)).longValue();
            Repositorio.eliminar("detalle", "id_detalle", idDetalle);
            cargarLineas();
        } catch (SQLException ex) {
            error("No se pudo eliminar: " + ex.getMessage());
        }
    }

    private void error(String m) {
        JOptionPane.showMessageDialog(this, m, "Calcular valores", JOptionPane.ERROR_MESSAGE);
    }
}
