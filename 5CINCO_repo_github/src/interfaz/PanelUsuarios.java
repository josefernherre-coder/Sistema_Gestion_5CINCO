package interfaz;

import datos.Autenticacion;
import datos.Repositorio;
import datos.Sesion;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Font;
import java.sql.SQLException;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.table.DefaultTableModel;

/**
 * Administración de usuarios (solo superusuario): crear usuarios, asignarles rol,
 * activarlos o desactivarlos, restablecer su clave y eliminarlos.
 * Las claves se cifran antes de guardarse; nunca se almacenan en texto plano.
 */
public class PanelUsuarios extends JPanel {

    private static final String SUPER = "superusuario";
    private static final String ADMIN = "administrador";
    private static final int MIN_CLAVE = 8;

    private final Sesion sesion;
    private final JTable tabla = new JTable();
    private final JLabel lblConteo = new JLabel(" ");
    private DefaultTableModel modelo;

    public PanelUsuarios(Sesion sesion) {
        super(new BorderLayout(0, 12));
        this.sesion = sesion;
        setBackground(Estilos.FONDO);
        setBorder(BorderFactory.createEmptyBorder(20, 28, 20, 28));

        add(crearBarra(), BorderLayout.NORTH);
        Estilos.estilizarTabla(tabla);
        JScrollPane scroll = new JScrollPane(tabla);
        scroll.setBorder(BorderFactory.createLineBorder(Estilos.BORDE));
        scroll.getViewport().setBackground(java.awt.Color.WHITE);
        add(scroll, BorderLayout.CENTER);

        JLabel ayuda = new JLabel("<html>Superusuario: ve datos sensibles (salarios, seguridad social), gestiona usuarios y puede eliminar. "
                + "Administrador: registra y consulta, sin datos sensibles y sin eliminar.</html>");
        ayuda.setFont(Estilos.fuente(Font.PLAIN, 12));
        ayuda.setForeground(Estilos.GRIS_TEXTO);
        lblConteo.setFont(Estilos.fuente(Font.PLAIN, 12));
        lblConteo.setForeground(Estilos.GRIS_TEXTO);
        JPanel pie = new JPanel(new BorderLayout());
        pie.setOpaque(false);
        pie.add(ayuda, BorderLayout.CENTER);
        pie.add(lblConteo, BorderLayout.EAST);
        add(pie, BorderLayout.SOUTH);
        recargar();
    }

    private JPanel crearBarra() {
        JPanel der = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        der.setOpaque(false);
        JButton nuevo = Estilos.boton("Nuevo usuario", true);
        JButton rol = Estilos.boton("Cambiar rol", false);
        JButton estado = Estilos.boton("Activar / Desactivar", false);
        JButton clave = Estilos.boton("Restablecer clave", false);
        JButton eliminar = Estilos.boton("Eliminar", false);
        JButton actualizar = Estilos.boton("Actualizar", false);
        nuevo.addActionListener(e -> nuevo());
        rol.addActionListener(e -> cambiarRol());
        estado.addActionListener(e -> cambiarEstado());
        clave.addActionListener(e -> restablecerClave());
        eliminar.addActionListener(e -> eliminar());
        actualizar.addActionListener(e -> recargar());
        der.add(nuevo);
        der.add(rol);
        der.add(estado);
        der.add(clave);
        der.add(eliminar);
        der.add(actualizar);
        JPanel barra = new JPanel(new BorderLayout());
        barra.setOpaque(false);
        barra.add(der, BorderLayout.EAST);
        return barra;
    }

    public final void recargar() {
        try {
            Repositorio.Resultado r = Repositorio.consultar(
                    "SELECT id_usuario, usuario AS \"Usuario\", "
                    + "CASE rol WHEN 'superusuario' THEN 'Superusuario' ELSE 'Administrador' END AS \"Rol\", "
                    + "CASE activo WHEN 1 THEN 'Activo' ELSE 'Inactivo' END AS \"Estado\", "
                    + "fecha_creacion AS \"Creado\" FROM usuarios ORDER BY usuario");
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
            tabla.removeColumn(tabla.getColumnModel().getColumn(0));
            Estilos.ajustarColumnas(tabla);
            lblConteo.setText(r.filas.size() + " usuarios");
        } catch (SQLException ex) {
            error("No se pudieron leer los usuarios: " + ex.getMessage());
        }
    }

    // ------------------------------------------------------------ selección
    private long[] seleccionado() {
        int fila = tabla.getSelectedRow();
        if (fila < 0) {
            JOptionPane.showMessageDialog(this, "Selecciona primero un usuario de la tabla.",
                    "Usuarios", JOptionPane.INFORMATION_MESSAGE);
            return null;
        }
        return new long[]{((Number) modelo.getValueAt(fila, 0)).longValue(), fila};
    }

    private String nombreEn(int fila) {
        return String.valueOf(modelo.getValueAt(fila, 1));
    }

    private boolean esElMismo(int fila) {
        return nombreEn(fila).equals(sesion.usuario);
    }

    // ------------------------------------------------------------ acciones
    private void nuevo() {
        JTextField usuario = new JTextField(20);
        JPasswordField clave = new JPasswordField(20);
        JPasswordField repetir = new JPasswordField(20);
        JComboBox<String> rol = new JComboBox<>(new String[]{"Administrador", "Superusuario"});
        Object[] campos = {"Usuario:", usuario, "Contraseña (mínimo " + MIN_CLAVE + " caracteres):", clave,
            "Repetir contraseña:", repetir, "Rol:", rol};
        int op = JOptionPane.showConfirmDialog(this, campos, "Nuevo usuario", JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.PLAIN_MESSAGE);
        char[] c1 = clave.getPassword();
        char[] c2 = repetir.getPassword();
        try {
            if (op != JOptionPane.OK_OPTION) {
                return;
            }
            String u = usuario.getText().trim();
            if (u.length() < 3 || u.contains(" ")) {
                aviso("El usuario debe tener mínimo 3 caracteres y no puede llevar espacios.");
                return;
            }
            String problema = validarClave(c1, c2);
            if (problema != null) {
                aviso(problema);
                return;
            }
            Repositorio.ejecutar("INSERT INTO usuarios (usuario, clave_hash, rol) VALUES (?, ?, ?)",
                    u, Autenticacion.hashear(c1), "Superusuario".equals(rol.getSelectedItem()) ? SUPER : ADMIN);
            recargar();
        } catch (SQLException ex) {
            aviso(ex.getMessage() != null && ex.getMessage().contains("UNIQUE")
                    ? "Ya existe un usuario con ese nombre." : Repositorio.mensajeAmigable(ex, null));
        } finally {
            java.util.Arrays.fill(c1, '\0');
            java.util.Arrays.fill(c2, '\0');
        }
    }

    private void cambiarRol() {
        long[] s = seleccionado();
        if (s == null) {
            return;
        }
        int fila = (int) s[1];
        if (esElMismo(fila)) {
            aviso("No puedes cambiar tu propio rol (así siempre queda al menos un superusuario).");
            return;
        }
        boolean eraSuper = "Superusuario".equals(modelo.getValueAt(fila, 2));
        JComboBox<String> rol = new JComboBox<>(new String[]{"Administrador", "Superusuario"});
        rol.setSelectedItem(eraSuper ? "Superusuario" : "Administrador");
        int op = JOptionPane.showConfirmDialog(this, new Object[]{"Rol de " + nombreEn(fila) + ":", rol},
                "Cambiar rol", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (op != JOptionPane.OK_OPTION) {
            return;
        }
        ejecutar("UPDATE usuarios SET rol = ? WHERE id_usuario = ?",
                "Superusuario".equals(rol.getSelectedItem()) ? SUPER : ADMIN, s[0]);
    }

    private void cambiarEstado() {
        long[] s = seleccionado();
        if (s == null) {
            return;
        }
        int fila = (int) s[1];
        if (esElMismo(fila)) {
            aviso("No puedes desactivar tu propio usuario.");
            return;
        }
        boolean activo = "Activo".equals(modelo.getValueAt(fila, 3));
        ejecutar("UPDATE usuarios SET activo = ? WHERE id_usuario = ?", activo ? 0 : 1, s[0]);
    }

    private void restablecerClave() {
        long[] s = seleccionado();
        if (s == null) {
            return;
        }
        JPasswordField clave = new JPasswordField(20);
        JPasswordField repetir = new JPasswordField(20);
        int op = JOptionPane.showConfirmDialog(this,
                new Object[]{"Nueva contraseña para " + nombreEn((int) s[1]) + " (mínimo " + MIN_CLAVE + " caracteres):",
                    clave, "Repetir contraseña:", repetir},
                "Restablecer clave", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        char[] c1 = clave.getPassword();
        char[] c2 = repetir.getPassword();
        try {
            if (op != JOptionPane.OK_OPTION) {
                return;
            }
            String problema = validarClave(c1, c2);
            if (problema != null) {
                aviso(problema);
                return;
            }
            ejecutar("UPDATE usuarios SET clave_hash = ? WHERE id_usuario = ?", Autenticacion.hashear(c1), s[0]);
        } finally {
            java.util.Arrays.fill(c1, '\0');
            java.util.Arrays.fill(c2, '\0');
        }
    }

    private void eliminar() {
        long[] s = seleccionado();
        if (s == null) {
            return;
        }
        int fila = (int) s[1];
        if (esElMismo(fila)) {
            aviso("No puedes eliminar tu propio usuario.");
            return;
        }
        int op = JOptionPane.showConfirmDialog(this, "¿Eliminar al usuario " + nombreEn(fila)
                + "? Esta acción no se puede deshacer.\n(Si solo quieres impedirle el acceso, mejor desactívalo.)",
                "Eliminar usuario", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (op == JOptionPane.YES_OPTION) {
            ejecutar("DELETE FROM usuarios WHERE id_usuario = ?", s[0]);
        }
    }

    // ------------------------------------------------------------ utilidades
    private String validarClave(char[] c1, char[] c2) {
        if (c1.length < MIN_CLAVE) {
            return "La contraseña debe tener mínimo " + MIN_CLAVE + " caracteres.";
        }
        if (!java.util.Arrays.equals(c1, c2)) {
            return "Las contraseñas no coinciden.";
        }
        return null;
    }

    private void ejecutar(String sql, Object... params) {
        try {
            Repositorio.ejecutar(sql, params);
            recargar();
        } catch (SQLException ex) {
            aviso(Repositorio.mensajeAmigable(ex, null));
        }
    }

    private void aviso(String m) {
        JOptionPane.showMessageDialog(this, m, "Usuarios", JOptionPane.WARNING_MESSAGE);
    }

    private void error(String m) {
        JOptionPane.showMessageDialog(this, m, "Usuarios", JOptionPane.ERROR_MESSAGE);
    }
}
