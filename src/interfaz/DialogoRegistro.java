package interfaz;

import datos.Repositorio;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Window;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.LinkedHashMap;
import java.util.Map;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;

/** Formulario para crear o editar un registro, generado a partir de los Campos del módulo. */
public class DialogoRegistro extends JDialog {

    /** Elemento de una lista desplegable que guarda un id y muestra un texto. */
    public static final class Opcion {

        public final Long id;
        public final String texto;

        Opcion(Long id, String texto) {
            this.id = id;
            this.texto = texto;
        }

        @Override
        public String toString() {
            return texto;
        }
    }

    private final Modulo modulo;
    private final Long idEditar;
    private final Map<String, Object> fijos;
    private final Map<Campo, JComponent> componentes = new LinkedHashMap<>();
    private boolean guardado = false;
    private Long idGuardado = null;

    /**
     * @param idEditar null para crear un registro nuevo
     * @param fijos valores que se agregan siempre al guardar (por ejemplo el id del proyecto de una línea)
     */
    public DialogoRegistro(Window dueno, Modulo modulo, Long idEditar, boolean esSuper, Map<String, Object> fijos) {
        super(dueno, (idEditar == null ? "Nuevo " : "Editar ") + modulo.nombreSingular, ModalityType.APPLICATION_MODAL);
        this.modulo = modulo;
        this.idEditar = idEditar;
        this.fijos = fijos == null ? Map.of() : fijos;

        JPanel formulario = new JPanel(new GridBagLayout());
        formulario.setBackground(Estilos.FONDO);
        formulario.setBorder(BorderFactory.createEmptyBorder(20, 24, 12, 24));

        Map<String, Object> actual = null;
        try {
            if (idEditar != null) {
                actual = Repositorio.fila(modulo.tabla, modulo.pk, idEditar);
            }
        } catch (SQLException ex) {
            mostrarError("No se pudo leer el registro: " + ex.getMessage());
        }

        int fila = 0;
        for (Campo campo : modulo.campos) {
            if (campo.sensible && !esSuper) {
                continue;
            }
            JLabel etiqueta = new JLabel(campo.etiqueta + (campo.obligatorio ? " *" : ""));
            etiqueta.setFont(Estilos.fuente(Font.BOLD, 13));
            etiqueta.setForeground(Estilos.CARBON);

            JComponent comp = crearComponente(campo);
            Object valor = null;
            if (actual != null) {
                valor = actual.get(campo.columna);
            } else {
                valor = valorInicial(campo);
            }
            asignarValor(campo, comp, valor);
            componentes.put(campo, comp);

            GridBagConstraints g = new GridBagConstraints();
            g.gridx = 0;
            g.gridy = fila;
            g.anchor = GridBagConstraints.NORTHWEST;
            g.insets = new Insets(8, 0, 2, 0);
            g.weightx = 1;
            g.fill = GridBagConstraints.HORIZONTAL;
            formulario.add(etiqueta, g);
            GridBagConstraints g2 = new GridBagConstraints();
            g2.gridx = 0;
            g2.gridy = fila + 1;
            g2.fill = GridBagConstraints.HORIZONTAL;
            g2.weightx = 1;
            formulario.add(comp instanceof JTextArea ? new JScrollPane(comp) : comp, g2);
            fila += 2;
        }

        if ("detalle".equals(modulo.tabla)) {
            precioAutomatico();
        }

        JScrollPane scroll = new JScrollPane(formulario);
        scroll.setBorder(null);
        scroll.getVerticalScrollBar().setUnitIncrement(16);

        JButton btnGuardar = Estilos.boton("Guardar", true);
        JButton btnCancelar = Estilos.boton("Cancelar", false);
        btnGuardar.addActionListener(e -> guardar());
        btnCancelar.addActionListener(e -> dispose());
        JPanel botones = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 10));
        botones.setBackground(Estilos.FONDO);
        JLabel nota = new JLabel("* obligatorio");
        nota.setFont(Estilos.fuente(Font.ITALIC, 12));
        nota.setForeground(Estilos.GRIS_TEXTO);
        botones.add(nota);
        botones.add(btnCancelar);
        botones.add(btnGuardar);

        getContentPane().setBackground(Estilos.FONDO);
        getContentPane().add(scroll, BorderLayout.CENTER);
        getContentPane().add(botones, BorderLayout.SOUTH);
        getRootPane().setDefaultButton(btnGuardar);

        int alto = Math.min(150 + fila * 30, 720);
        setSize(new Dimension(560, alto));
        setMinimumSize(new Dimension(460, 300));
        setLocationRelativeTo(dueno);
    }

    /** En una línea de detalle: al elegir el producto o servicio se sugiere su precio de referencia. */
    @SuppressWarnings("unchecked")
    private void precioAutomatico() {
        JComponent cbp = null;
        JComponent tp = null;
        for (Map.Entry<Campo, JComponent> e : componentes.entrySet()) {
            if (e.getKey().columna.equals("id_producto")) {
                cbp = e.getValue();
            } else if (e.getKey().columna.equals("precio_unitario")) {
                tp = e.getValue();
            }
        }
        if (!(cbp instanceof JComboBox) || !(tp instanceof JTextField)) {
            return;
        }
        final JComboBox<Opcion> cb = (JComboBox<Opcion>) cbp;
        final JTextField precio = (JTextField) tp;
        Runnable poner = () -> {
            Opcion o = (Opcion) cb.getSelectedItem();
            if (o == null || o.id == null) {
                return;
            }
            if (idEditar != null && !precio.getText().trim().isEmpty()) {
                return; // al editar una línea no se pisa el precio pactado
            }
            try {
                Repositorio.Resultado r = Repositorio.consultar(
                        "SELECT precio_referencia FROM productos WHERE id_producto = ?", o.id);
                if (!r.filas.isEmpty() && r.filas.get(0)[0] instanceof Number) {
                    precio.setText(BigDecimal.valueOf(((Number) r.filas.get(0)[0]).doubleValue())
                            .stripTrailingZeros().toPlainString());
                }
            } catch (SQLException ex) {
                // sin precio sugerido: se escribe a mano
            }
        };
        cb.addActionListener(e -> poner.run());
        if (idEditar == null) {
            poner.run();
        }
    }

    public boolean fueGuardado() {
        return guardado;
    }

    /** Id del registro que se acaba de crear o editar (null si no se guardó). */
    public Long idGuardado() {
        return idGuardado;
    }

    // ---------------------------------------------------------------- componentes
    private JComponent crearComponente(Campo c) {
        switch (c.tipo) {
            case AREA: {
                JTextArea a = new JTextArea(3, 20);
                a.setLineWrap(true);
                a.setWrapStyleWord(true);
                a.setFont(Estilos.fuente(Font.PLAIN, 14));
                a.setBorder(BorderFactory.createEmptyBorder(4, 6, 4, 6));
                a.setEditable(!c.soloLectura);
                return a;
            }
            case LISTA: {
                JComboBox<String> cb = new JComboBox<>();
                if (!c.obligatorio) {
                    cb.addItem("");
                }
                for (String o : c.opciones) {
                    cb.addItem(o);
                }
                cb.setFont(Estilos.fuente(Font.PLAIN, 14));
                return cb;
            }
            case REFERENCIA: {
                JComboBox<Opcion> cb = new JComboBox<>();
                if (!c.obligatorio) {
                    cb.addItem(new Opcion(null, "— ninguno —"));
                }
                try {
                    Repositorio.Resultado r = Repositorio.consultar(c.sqlReferencia);
                    for (Object[] f : r.filas) {
                        cb.addItem(new Opcion(((Number) f[0]).longValue(), String.valueOf(f[1])));
                    }
                } catch (SQLException ex) {
                    mostrarError("No se pudo cargar la lista de " + c.etiqueta + ": " + ex.getMessage());
                }
                cb.setFont(Estilos.fuente(Font.PLAIN, 14));
                return cb;
            }
            default: {
                JTextField t = new JTextField();
                t.setFont(Estilos.fuente(Font.PLAIN, 14));
                t.setEditable(!c.soloLectura);
                t.setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(Estilos.BORDE, 1),
                        BorderFactory.createEmptyBorder(6, 8, 6, 8)));
                return t;
            }
        }
    }

    private Object valorInicial(Campo c) {
        if (c.sqlValorInicial != null) {
            try {
                return Repositorio.escalar(c.sqlValorInicial);
            } catch (SQLException ex) {
                return null;
            }
        }
        return c.valorInicial;
    }

    @SuppressWarnings("unchecked")
    private void asignarValor(Campo c, JComponent comp, Object v) {
        if (comp instanceof JComboBox) {
            JComboBox<Object> cb = (JComboBox<Object>) comp;
            if (v == null) {
                if (cb.getItemCount() > 0) {
                    cb.setSelectedIndex(0);
                }
                return;
            }
            for (int i = 0; i < cb.getItemCount(); i++) {
                Object it = cb.getItemAt(i);
                if (it instanceof Opcion ? v instanceof Number && ((Opcion) it).id != null
                        && ((Opcion) it).id == ((Number) v).longValue() : it.equals(v.toString())) {
                    cb.setSelectedIndex(i);
                    return;
                }
            }
            return;
        }
        String texto = "";
        if (v != null) {
            texto = v instanceof Double ? BigDecimal.valueOf((Double) v).stripTrailingZeros().toPlainString()
                    : v.toString();
        }
        if (comp instanceof JTextArea) {
            ((JTextArea) comp).setText(texto);
        } else {
            ((JTextField) comp).setText(texto);
        }
    }

    private String leerTexto(JComponent comp) {
        String t = comp instanceof JTextArea ? ((JTextArea) comp).getText() : ((JTextField) comp).getText();
        t = t.trim();
        return t.isEmpty() ? null : t;
    }

    // ---------------------------------------------------------------- guardar
    private void guardar() {
        Map<String, Object> valores = new LinkedHashMap<>();
        for (Map.Entry<Campo, JComponent> e : componentes.entrySet()) {
            Campo c = e.getKey();
            JComponent comp = e.getValue();
            Object v;
            try {
                v = leer(c, comp);
            } catch (IllegalArgumentException ex) {
                mostrarError(c.etiqueta.replace(" (AAAA-MM-DD)", "") + ": " + ex.getMessage());
                comp.requestFocusInWindow();
                return;
            }
            if (v == null && c.obligatorio) {
                mostrarError("Falta el campo obligatorio: " + c.etiqueta.replace(" (AAAA-MM-DD)", "") + ".");
                comp.requestFocusInWindow();
                return;
            }
            valores.put(c.columna, v);
        }
        valores.putAll(fijos);
        if (modulo.antesDeGuardar != null) {
            try {
                modulo.antesDeGuardar.accept(valores);
            } catch (IllegalArgumentException ex) {
                mostrarError(ex.getMessage());
                return;
            }
        }
        try {
            if (idEditar == null) {
                idGuardado = Repositorio.insertar(modulo.tabla, valores);
            } else {
                Repositorio.actualizar(modulo.tabla, modulo.pk, idEditar, valores);
                idGuardado = idEditar;
            }
            guardado = true;
            dispose();
        } catch (SQLException ex) {
            mostrarError(Repositorio.mensajeAmigable(ex, modulo.mensajeCheck));
        }
    }

    @SuppressWarnings("unchecked")
    private Object leer(Campo c, JComponent comp) {
        switch (c.tipo) {
            case LISTA: {
                String s = (String) ((JComboBox<String>) comp).getSelectedItem();
                return s == null || s.isEmpty() ? null : s;
            }
            case REFERENCIA: {
                Opcion o = (Opcion) ((JComboBox<Opcion>) comp).getSelectedItem();
                return o == null ? null : o.id;
            }
            case ENTERO: {
                String t = leerTexto(comp);
                if (t == null) {
                    return null;
                }
                try {
                    return Long.parseLong(t.replace(".", "").replace(" ", ""));
                } catch (NumberFormatException ex) {
                    throw new IllegalArgumentException("escribe un número entero.");
                }
            }
            case DECIMAL: {
                String t = leerTexto(comp);
                if (t == null) {
                    return null;
                }
                BigDecimal bd = parsearDecimal(t);
                if (bd.scale() <= 0 || bd.stripTrailingZeros().scale() <= 0) {
                    return bd.longValueExact();
                }
                return bd.doubleValue();
            }
            case FECHA: {
                String t = leerTexto(comp);
                if (t == null) {
                    return null;
                }
                try {
                    return LocalDate.parse(t).toString();
                } catch (DateTimeParseException ex) {
                    throw new IllegalArgumentException("usa el formato AAAA-MM-DD (ejemplo: 2026-10-29).");
                }
            }
            default:
                return leerTexto(comp);
        }
    }

    /** Acepta 1500000, 1.500.000, 1.500.000,50 o 0.45 / 0,45. */
    static BigDecimal parsearDecimal(String t) {
        String s = t.replace("$", "").replace(" ", "");
        boolean punto = s.contains(".");
        boolean coma = s.contains(",");
        if (punto && coma) {
            s = s.replace(".", "").replace(",", ".");
        } else if (coma) {
            s = s.replace(",", ".");
        } else if (punto && s.matches("-?\\d{1,3}(\\.\\d{3})+")) {
            s = s.replace(".", "");
        }
        try {
            return new BigDecimal(s);
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException("escribe un número válido.");
        }
    }

    private void mostrarError(String mensaje) {
        JOptionPane.showMessageDialog(this, mensaje, "Revisa los datos", JOptionPane.WARNING_MESSAGE);
    }
}
