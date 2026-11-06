/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/GUIForms/JFrame.java to edit this template
 */
package interfaz;

import java.awt.Color;
import java.awt.Cursor;
import java.awt.Image;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import datos.Autenticacion;
import datos.Sesion;
import java.sql.SQLException;
import java.util.Arrays;
import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JOptionPane;
import javax.swing.JTextField;
import javax.swing.Timer;
import javax.swing.border.Border;

/**
 *
 * @author JOSE FERNANDO
 */
public class Login extends javax.swing.JFrame {
    
    private static final java.util.logging.Logger logger = java.util.logging.Logger.getLogger(Login.class.getName());

    /**
     * Creates new form Login
     */
    public Login() {
        initComponents();
        personalizar();
    }

    // ---------- Paleta (basada en el logo) ----------
    private static final Color VERDE = new Color(166, 206, 58);
    private static final Color VERDE_HOVER = new Color(150, 190, 44);
    private static final Color VERDE_PRESIONADO = new Color(132, 170, 30);
    private static final Color VERDE_OSCURO = new Color(92, 124, 18);
    private static final Color BORDE = new Color(200, 205, 194);
    private static final Color DESHABILITADO = new Color(206, 210, 200);
 
    // ---------- Reglas de seguridad ----------
    private static final int MAX_INTENTOS = 3;
    private static final int BLOQUEO_SEGUNDOS = 30;
 
    private int intentos = 0;
    private int segundosRestantes = 0;
 
    /** Ajustes que el diseñador visual no hace: bordes, logo, eventos, etc. */
    private void personalizar() {
        setLocationRelativeTo(null);                 // centrar en pantalla
        getRootPane().setDefaultButton(btnIngresar); // Enter = iniciar sesión
        txtClave.setEchoChar('\u2022');
        lblError.setText(" ");                       // reserva la altura del mensaje
 
        estilizarCampo(txtUsuario);
        estilizarCampo(txtClave);
        estilizarBoton();
        cargarLogo();
 
        // Enter en el usuario pasa a la contraseña
        txtUsuario.addActionListener(e -> txtClave.requestFocusInWindow());
        // Mostrar / ocultar contraseña
        chkMostrar.addActionListener(e
                -> txtClave.setEchoChar(chkMostrar.isSelected() ? (char) 0 : '\u2022'));
        // Botón iniciar sesión (no hace falta doble clic en el diseñador)
        btnIngresar.addActionListener(e -> iniciarSesion());
    }
 
    private void estilizarCampo(JTextField campo) {
        campo.setBorder(bordeCampo(false));
        campo.addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent e) {
                campo.setBorder(bordeCampo(true));
            }
 
            @Override
            public void focusLost(FocusEvent e) {
                campo.setBorder(bordeCampo(false));
            }
        });
    }
 
    /** Los dos bordes miden lo mismo en total: el campo no cambia de tamaño al enfocarse. */
    private Border bordeCampo(boolean enfocado) {
        return enfocado
                ? BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(VERDE_OSCURO, 2),
                        BorderFactory.createEmptyBorder(8, 11, 8, 11))
                : BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(BORDE, 1),
                        BorderFactory.createEmptyBorder(9, 12, 9, 12));
    }
 
    private void estilizarBoton() {
        btnIngresar.setOpaque(true);
        btnIngresar.setBackground(VERDE);
        btnIngresar.setBorderPainted(false);
        btnIngresar.setFocusPainted(false);
        btnIngresar.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btnIngresar.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                if (btnIngresar.isEnabled()) {
                    btnIngresar.setBackground(VERDE_HOVER);
                }
            }
 
            @Override
            public void mouseExited(MouseEvent e) {
                if (btnIngresar.isEnabled()) {
                    btnIngresar.setBackground(VERDE);
                }
            }
 
            @Override
            public void mousePressed(MouseEvent e) {
                if (btnIngresar.isEnabled()) {
                    btnIngresar.setBackground(VERDE_PRESIONADO);
                }
            }
 
            @Override
            public void mouseReleased(MouseEvent e) {
                if (btnIngresar.isEnabled()) {
                    btnIngresar.setBackground(VERDE_HOVER);
                }
            }
        });
    }
 
    /** Carga /imagenes/Logo.png escalado a 270 px de ancho. Si no lo encuentra, muestra texto. */
    private void cargarLogo() {
        java.net.URL url = getClass().getResource("/imagenes/Logo.png");
        if (url == null) {
            lblLogo.setText("5CINCO");
            return;
        }
        ImageIcon original = new ImageIcon(url);
        int ancho = 270;
        int alto = original.getIconHeight() * ancho / original.getIconWidth();
        Image escalada = original.getImage().getScaledInstance(ancho, alto, Image.SCALE_SMOOTH);
        lblLogo.setIcon(new ImageIcon(escalada));
        lblLogo.setText("");
    }
 
    // ---------- Inicio de sesión ----------
    private void iniciarSesion() {
        String usuario = txtUsuario.getText().trim();
        char[] clave = txtClave.getPassword();
 
        try {
            if (usuario.isEmpty()) {
                lblError.setText("Escribe tu usuario.");
                txtUsuario.requestFocusInWindow();
                return;
            }
            if (clave.length == 0) {
                lblError.setText("Escribe tu contraseña.");
                txtClave.requestFocusInWindow();
                return;
            }
 
            Sesion sesion;
            try {
                sesion = Autenticacion.autenticar(usuario, clave);
            } catch (SQLException ex) {
                logger.log(java.util.logging.Level.SEVERE, null, ex);
                lblError.setText("No se pudo conectar con la base de datos.");
                return;
            }
            if (sesion != null) {
                intentos = 0;
                lblError.setText(" ");
                new MenuPrincipal(sesion).setVisible(true);
                dispose();
            } else {
                intentos++;
                txtClave.setText("");
                int restantes = MAX_INTENTOS - intentos;
                if (restantes <= 0) {
                    bloquear();
                } else {
                    lblError.setText("Usuario o contraseña incorrectos. Intentos restantes: " + restantes);
                    txtClave.requestFocusInWindow();
                }
            }
        } finally {
            Arrays.fill(clave, '\0'); // no dejar la contraseña en memoria
        }
    }
 
    /** Bloquea el formulario BLOQUEO_SEGUNDOS tras superar el máximo de intentos. */
    private void bloquear() {
        cambiarEstadoFormulario(false);
        segundosRestantes = BLOQUEO_SEGUNDOS;
        lblError.setText("Demasiados intentos. Espera " + segundosRestantes + " s.");
 
        Timer timer = new Timer(1000, null);
        timer.addActionListener(e -> {
            segundosRestantes--;
            if (segundosRestantes <= 0) {
                timer.stop();
                intentos = 0;
                cambiarEstadoFormulario(true);
                lblError.setText(" ");
                txtUsuario.requestFocusInWindow();
            } else {
                lblError.setText("Demasiados intentos. Espera " + segundosRestantes + " s.");
            }
        });
        timer.start();
    }
 
    private void cambiarEstadoFormulario(boolean habilitado) {
        txtUsuario.setEnabled(habilitado);
        txtClave.setEnabled(habilitado);
        chkMostrar.setEnabled(habilitado);
        btnIngresar.setEnabled(habilitado);
        btnIngresar.setBackground(habilitado ? VERDE : DESHABILITADO);
    }
    
    
    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {
        java.awt.GridBagConstraints gridBagConstraints;

        panelIzq = new javax.swing.JPanel();
        panelMarca = new javax.swing.JPanel();
        panelLogo = new javax.swing.JPanel();
        lblLogo = new javax.swing.JLabel();
        lblDescripcion = new javax.swing.JLabel();
        panelDer = new javax.swing.JPanel();
        panelForm = new javax.swing.JPanel();
        lblTitulo = new javax.swing.JLabel();
        lblSubtitulo = new javax.swing.JLabel();
        lblUsuario = new javax.swing.JLabel();
        txtUsuario = new javax.swing.JTextField();
        lblClave = new javax.swing.JLabel();
        txtClave = new javax.swing.JPasswordField();
        chkMostrar = new javax.swing.JCheckBox();
        lblError = new javax.swing.JLabel();
        btnIngresar = new javax.swing.JButton();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        setTitle("5CINCO - Iniciar Sesión");
        setMinimumSize(new java.awt.Dimension(780, 480));

        panelIzq.setBackground(new java.awt.Color(166, 206, 58));
        panelIzq.setPreferredSize(new java.awt.Dimension(380, 540));
        panelIzq.setLayout(new java.awt.GridBagLayout());

        panelMarca.setOpaque(false);
        panelMarca.setLayout(new org.netbeans.lib.awtextra.AbsoluteLayout());

        panelLogo.setBackground(new java.awt.Color(255, 255, 255));
        panelLogo.setLayout(new org.netbeans.lib.awtextra.AbsoluteLayout());
        panelLogo.add(lblLogo, new org.netbeans.lib.awtextra.AbsoluteConstraints(25, 27, 270, 106));

        panelMarca.add(panelLogo, new org.netbeans.lib.awtextra.AbsoluteConstraints(0, 0, 320, 160));

        lblDescripcion.setFont(new java.awt.Font("Segoe UI", 3, 24)); // NOI18N
        lblDescripcion.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        lblDescripcion.setText(" BASE DE DATOS 5CINCO");
        panelMarca.add(lblDescripcion, new org.netbeans.lib.awtextra.AbsoluteConstraints(10, 185, 300, 50));

        panelIzq.add(panelMarca, new java.awt.GridBagConstraints());

        getContentPane().add(panelIzq, java.awt.BorderLayout.WEST);

        panelDer.setBackground(new java.awt.Color(246, 247, 243));
        panelDer.setPreferredSize(new java.awt.Dimension(520, 540));
        panelDer.setLayout(new java.awt.GridBagLayout());

        panelForm.setOpaque(false);
        panelForm.setLayout(new org.netbeans.lib.awtextra.AbsoluteLayout());

        lblTitulo.setBackground(new java.awt.Color(255, 255, 255));
        lblTitulo.setFont(new java.awt.Font("Segoe UI", 1, 30)); // NOI18N
        lblTitulo.setForeground(new java.awt.Color(36, 40, 33));
        lblTitulo.setText("Iniciar Sesión");
        panelForm.add(lblTitulo, new org.netbeans.lib.awtextra.AbsoluteConstraints(0, 0, 340, 40));

        lblSubtitulo.setFont(new java.awt.Font("Segoe UI", 0, 14)); // NOI18N
        lblSubtitulo.setForeground(new java.awt.Color(98, 104, 92));
        lblSubtitulo.setText("Ingresa con tu usuario y contraseña");
        panelForm.add(lblSubtitulo, new org.netbeans.lib.awtextra.AbsoluteConstraints(0, 44, 340, 22));

        lblUsuario.setFont(new java.awt.Font("Segoe UI", 1, 13)); // NOI18N
        lblUsuario.setText("Usuario");
        panelForm.add(lblUsuario, new org.netbeans.lib.awtextra.AbsoluteConstraints(0, 100, 340, 22));

        txtUsuario.setFont(new java.awt.Font("Segoe UI", 0, 16)); // NOI18N
        txtUsuario.setForeground(new java.awt.Color(36, 40, 33));
        panelForm.add(txtUsuario, new org.netbeans.lib.awtextra.AbsoluteConstraints(0, 124, 340, 44));

        lblClave.setFont(new java.awt.Font("Segoe UI", 1, 13)); // NOI18N
        lblClave.setForeground(new java.awt.Color(36, 40, 33));
        lblClave.setText("Contraseña");
        panelForm.add(lblClave, new org.netbeans.lib.awtextra.AbsoluteConstraints(0, 186, 340, 20));

        txtClave.setFont(new java.awt.Font("Segoe UI", 0, 16)); // NOI18N
        txtClave.setForeground(new java.awt.Color(36, 40, 33));
        panelForm.add(txtClave, new org.netbeans.lib.awtextra.AbsoluteConstraints(0, 210, 340, 44));

        chkMostrar.setFont(new java.awt.Font("Segoe UI", 0, 13)); // NOI18N
        chkMostrar.setForeground(new java.awt.Color(98, 104, 92));
        chkMostrar.setText("Mostrar Contraseña");
        panelForm.add(chkMostrar, new org.netbeans.lib.awtextra.AbsoluteConstraints(0, 262, 340, 24));

        lblError.setFont(new java.awt.Font("Segoe UI", 0, 13)); // NOI18N
        lblError.setForeground(new java.awt.Color(192, 40, 40));
        panelForm.add(lblError, new org.netbeans.lib.awtextra.AbsoluteConstraints(0, 294, 340, 22));

        btnIngresar.setBackground(new java.awt.Color(166, 206, 58));
        btnIngresar.setFont(new java.awt.Font("Segoe UI", 1, 16)); // NOI18N
        btnIngresar.setForeground(new java.awt.Color(36, 40, 33));
        btnIngresar.setText("Iniciar Sesión ");
        panelForm.add(btnIngresar, new org.netbeans.lib.awtextra.AbsoluteConstraints(0, 326, 340, 44));

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.insets = new java.awt.Insets(80, 70, 0, 0);
        panelDer.add(panelForm, gridBagConstraints);

        getContentPane().add(panelDer, java.awt.BorderLayout.CENTER);

        pack();
    }// </editor-fold>//GEN-END:initComponents

    /**
     * @param args the command line arguments
     */

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton btnIngresar;
    private javax.swing.JCheckBox chkMostrar;
    private javax.swing.JLabel lblClave;
    private javax.swing.JLabel lblDescripcion;
    private javax.swing.JLabel lblError;
    private javax.swing.JLabel lblLogo;
    private javax.swing.JLabel lblSubtitulo;
    private javax.swing.JLabel lblTitulo;
    private javax.swing.JLabel lblUsuario;
    private javax.swing.JPanel panelDer;
    private javax.swing.JPanel panelForm;
    private javax.swing.JPanel panelIzq;
    private javax.swing.JPanel panelLogo;
    private javax.swing.JPanel panelMarca;
    private javax.swing.JPasswordField txtClave;
    private javax.swing.JTextField txtUsuario;
    // End of variables declaration//GEN-END:variables
}
