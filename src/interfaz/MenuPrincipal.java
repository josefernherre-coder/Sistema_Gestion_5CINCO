/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package interfaz;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.Image;
import datos.Repositorio;
import datos.Sesion;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingConstants;

/**
 *
 * @author JOSE FERNANDO
 */
public class MenuPrincipal extends JFrame {
    
    private static final java.util.logging.Logger logger =
            java.util.logging.Logger.getLogger(MenuPrincipal.class.getName());
 
    private static final String[] MODULOS = {
        "Inicio", "Clientes", "Proyectos", "Contratos", "Códigos de compra", "Productos",
        "Proveedores", "Empleados", "Contratistas", "Asignaciones", "Calcular valores", "Reportes", "Salario mínimo",
        "Usuarios"
    };
 
    private static final String[] DESCRIPCIONES = {
        "",
        "Registrar, consultar y actualizar los datos de los clientes.",
        "Registrar y consultar los proyectos de amueblamiento y adecuación de interiores.",
        "Registrar, consultar y actualizar los contratos, sus PDFs y sus códigos de compra.",
        "Catálogo de códigos de compra (UNSPSC) que se asignan a los contratos.",
        "Registrar y consultar los productos y servicios.",
        "Registrar y consultar los proveedores.",
        "Registrar y consultar los empleados.",
        "Registrar y consultar los contratistas.",
        "Asignar empleados y contratistas a los contratos.",
        "Calcular subtotales y el valor total por proyecto o contrato.",
        "Consultar los contratos de un año o de un rango de fechas y exportarlos a Excel.",
        "Registrar y actualizar el salario mínimo (SMMLV) de cada año.",
        "Crear usuarios, asignarles el rol y administrar su acceso."
    };
 
    private final List<BotonMenu> botones = new ArrayList<>();
    private CardLayout cardLayout;
    private JPanel contenido;
    private JLabel lblSeccion;
 
    // Totales de la pantalla de inicio (se llenarán al conectar la base de datos)
    private final JLabel lblTotalClientes = new JLabel("—");
    private final JLabel lblTotalProyectos = new JLabel("—");
    private final JLabel lblTotalContratos = new JLabel("—");
    private final JLabel lblTotalProductos = new JLabel("—");
 
    private final Sesion sesion;

    public MenuPrincipal(Sesion sesion) {
        this.sesion = sesion;
        String usuario = sesion.usuario;
        String rol = sesion.rolVisible();
        setTitle("5CINCO - Panel principal");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setMinimumSize(new Dimension(900, 560));
        setContentPane(construirRaiz(usuario, rol));
        mostrar("Inicio");
        setSize(1200, 700);
        setLocationRelativeTo(null);
        setExtendedState(JFrame.MAXIMIZED_BOTH);
    }
 
    // =====================================================================
    //  ESTRUCTURA
    // =====================================================================
    private JPanel construirRaiz(String usuario, String rol) {
        JPanel raiz = new JPanel(new BorderLayout());
        raiz.add(crearMenuLateral(), BorderLayout.WEST);
 
        JPanel derecha = new JPanel(new BorderLayout());
        derecha.add(crearEncabezado(usuario, rol), BorderLayout.NORTH);
        derecha.add(crearContenido(usuario), BorderLayout.CENTER);
        raiz.add(derecha, BorderLayout.CENTER);
        return raiz;
    }
 
    /** Menú lateral oscuro con el logo arriba y "Cerrar sesión" abajo. */
    private JPanel crearMenuLateral() {
        JPanel menu = new JPanel();
        menu.setLayout(new BoxLayout(menu, BoxLayout.Y_AXIS));
        menu.setBackground(Estilos.CARBON);
        menu.setPreferredSize(new Dimension(240, 600));
 
        JPanel tarjetaLogo = new JPanel(new BorderLayout());
        tarjetaLogo.setBackground(Color.WHITE);
        tarjetaLogo.setBorder(BorderFactory.createEmptyBorder(14, 14, 14, 14));
        tarjetaLogo.add(crearLogo(180), BorderLayout.CENTER);
 
        JPanel envoltura = new JPanel(new BorderLayout());
        envoltura.setOpaque(false);
        envoltura.setBorder(BorderFactory.createEmptyBorder(16, 16, 12, 16));
        envoltura.add(tarjetaLogo, BorderLayout.CENTER);
        envoltura.setAlignmentX(LEFT_ALIGNMENT);
        envoltura.setMaximumSize(new Dimension(Integer.MAX_VALUE, 130));
        menu.add(envoltura);
        menu.add(Box.createVerticalStrut(8));
 
        for (String modulo : MODULOS) {
            if (!visible(modulo)) {
                continue;
            }
            BotonMenu boton = new BotonMenu(modulo);
            boton.addActionListener(e -> mostrar(modulo));
            botones.add(boton);
            menu.add(boton);
        }
 
        menu.add(Box.createVerticalGlue());
 
        BotonMenu cerrar = new BotonMenu("Cerrar sesión");
        cerrar.addActionListener(e -> cerrarSesion());
        menu.add(cerrar);
        menu.add(Box.createVerticalStrut(12));
        return menu;
    }
 
    /** Barra superior blanca: nombre de la sección y datos del usuario. */
    private JPanel crearEncabezado(String usuario, String rol) {
        JPanel encabezado = new JPanel(new BorderLayout());
        encabezado.setBackground(Color.WHITE);
        encabezado.setPreferredSize(new Dimension(100, 72));
        encabezado.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, Estilos.BORDE),
                BorderFactory.createEmptyBorder(0, 32, 0, 32)));
 
        lblSeccion = new JLabel("Inicio");
        lblSeccion.setFont(Estilos.fuente(Font.BOLD, 22));
        lblSeccion.setForeground(Estilos.CARBON);
 
        JLabel lblUsuario = new JLabel("Usuario: " + usuario + "   |   Rol: " + rol);
        lblUsuario.setFont(Estilos.fuente(Font.PLAIN, 13));
        lblUsuario.setForeground(Estilos.GRIS_TEXTO);
 
        encabezado.add(lblSeccion, BorderLayout.WEST);
        encabezado.add(lblUsuario, BorderLayout.EAST);
        return encabezado;
    }
 
    /** Zona central: un panel por módulo, se muestra uno a la vez. */
    private JPanel crearContenido(String usuario) {
        cardLayout = new CardLayout();
        contenido = new JPanel(cardLayout);
        contenido.setBackground(Estilos.FONDO);
 
        contenido.add(crearInicio(usuario), MODULOS[0]);
        java.util.Map<String, Modulo> definidos = Modulos.porTitulo();
        for (int i = 1; i < MODULOS.length; i++) {
            String nombre = MODULOS[i];
            if (!visible(nombre)) {
                continue;
            }
            if (nombre.equals("Usuarios")) {
                contenido.add(new PanelUsuarios(sesion), nombre);
            } else if (definidos.containsKey(nombre)) {
                contenido.add(new PanelCrud(definidos.get(nombre), sesion), nombre);
            } else if (nombre.equals("Calcular valores")) {
                contenido.add(new PanelCalculo(sesion), nombre);
            } else if (nombre.equals("Reportes")) {
                contenido.add(new PanelReporte(), nombre);
            } else {
                contenido.add(new PanelModulo(nombre, DESCRIPCIONES[i]), nombre);
            }
        }
        return contenido;
    }
 
    /** Pantalla de bienvenida con tarjetas de resumen. */
    private JPanel crearInicio(String usuario) {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(Estilos.FONDO);
        panel.setBorder(BorderFactory.createEmptyBorder(32, 36, 32, 36));
 
        JPanel superior = new JPanel();
        superior.setOpaque(false);
        superior.setLayout(new BoxLayout(superior, BoxLayout.Y_AXIS));
 
        JLabel saludo = new JLabel("Bienvenido, " + usuario);
        saludo.setFont(Estilos.fuente(Font.BOLD, 28));
        saludo.setForeground(Estilos.CARBON);
        saludo.setAlignmentX(LEFT_ALIGNMENT);
 
        JLabel indicacion = new JLabel("Selecciona un módulo en el menú de la izquierda para empezar.");
        indicacion.setFont(Estilos.fuente(Font.PLAIN, 15));
        indicacion.setForeground(Estilos.GRIS_TEXTO);
        indicacion.setAlignmentX(LEFT_ALIGNMENT);
 
        JPanel tarjetas = new JPanel(new GridLayout(1, 4, 16, 0));
        tarjetas.setOpaque(false);
        tarjetas.setAlignmentX(LEFT_ALIGNMENT);
        tarjetas.setMaximumSize(new Dimension(Integer.MAX_VALUE, 120));
        tarjetas.add(crearTarjeta("Clientes", lblTotalClientes));
        tarjetas.add(crearTarjeta("Proyectos", lblTotalProyectos));
        tarjetas.add(crearTarjeta("Contratos", lblTotalContratos));
        tarjetas.add(crearTarjeta("Productos", lblTotalProductos));
 
        JLabel nota = new JLabel("Datos en vivo de la base de datos 5CINCO.");
        nota.setFont(Estilos.fuente(Font.ITALIC, 13));
        nota.setForeground(Estilos.GRIS_TEXTO);
        nota.setAlignmentX(LEFT_ALIGNMENT);
 
        superior.add(saludo);
        superior.add(Box.createVerticalStrut(6));
        superior.add(indicacion);
        superior.add(Box.createVerticalStrut(28));
        superior.add(tarjetas);
        superior.add(Box.createVerticalStrut(12));
        superior.add(nota);
 
        panel.add(superior, BorderLayout.NORTH);
        return panel;
    }
 
    private JPanel crearTarjeta(String titulo, JLabel valor) {
        JPanel tarjeta = new JPanel();
        tarjeta.setLayout(new BoxLayout(tarjeta, BoxLayout.Y_AXIS));
        tarjeta.setBackground(Color.WHITE);
        tarjeta.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Estilos.BORDE, 1),
                BorderFactory.createCompoundBorder(
                        BorderFactory.createMatteBorder(4, 0, 0, 0, Estilos.VERDE),
                        BorderFactory.createEmptyBorder(14, 18, 14, 18))));
 
        valor.setFont(Estilos.fuente(Font.BOLD, 32));
        valor.setForeground(Estilos.CARBON);
        valor.setAlignmentX(LEFT_ALIGNMENT);
 
        JLabel etiqueta = new JLabel(titulo);
        etiqueta.setFont(Estilos.fuente(Font.PLAIN, 14));
        etiqueta.setForeground(Estilos.GRIS_TEXTO);
        etiqueta.setAlignmentX(LEFT_ALIGNMENT);
 
        tarjeta.add(valor);
        tarjeta.add(etiqueta);
        return tarjeta;
    }
 
    /** Carga /imagenes/Logo.png escalado al ancho indicado. */
    private JLabel crearLogo(int ancho) {
        JLabel lbl = new JLabel();
        lbl.setHorizontalAlignment(SwingConstants.CENTER);
        java.net.URL url = getClass().getResource("/imagenes/Logo.png");
        if (url != null) {
            ImageIcon original = new ImageIcon(url);
            int alto = original.getIconHeight() * ancho / original.getIconWidth();
            lbl.setIcon(new ImageIcon(original.getImage().getScaledInstance(ancho, alto, Image.SCALE_SMOOTH)));
        } else {
            logger.warning("No se encontró /imagenes/Logo.png");
            lbl.setText("5CINCO");
            lbl.setFont(Estilos.fuente(Font.BOLD, 28));
            lbl.setForeground(Estilos.CARBON);
        }
        return lbl;
    }
 
    // =====================================================================
    //  NAVEGACIÓN
    // =====================================================================
    private void mostrar(String modulo) {
        if (modulo.equals("Inicio")) {
            actualizarTotales();
        }
        cardLayout.show(contenido, modulo);
        lblSeccion.setText(modulo);
        for (BotonMenu boton : botones) {
            boton.setSeleccionado(boton.getText().equals(modulo));
        }
    }
 
    /** Lee de la base los totales de la pantalla de inicio. */
    private void actualizarTotales() {
        try {
            lblTotalClientes.setText(String.valueOf(Repositorio.contar("clientes")));
            lblTotalProyectos.setText(String.valueOf(Repositorio.contar("proyectos")));
            lblTotalContratos.setText(String.valueOf(Repositorio.contar("contratos")));
            lblTotalProductos.setText(String.valueOf(Repositorio.contar("productos")));
        } catch (SQLException ex) {
            logger.log(java.util.logging.Level.WARNING, "No se pudieron leer los totales", ex);
        }
    }

    /** "Usuarios" solo lo ve el superusuario. */
    private boolean visible(String modulo) {
        return !modulo.equals("Usuarios") || sesion.esSuperusuario();
    }

    private void cerrarSesion() {
        int opcion = JOptionPane.showConfirmDialog(this,
                "¿Seguro que quieres cerrar sesión?", "Cerrar sesión",
                JOptionPane.YES_NO_OPTION);
        if (opcion == JOptionPane.YES_OPTION) {
            dispose();
            new Login().setVisible(true);
        }
    }
 
    // =====================================================================
    //  BOTÓN DEL MENÚ LATERAL
    // =====================================================================
    private static class BotonMenu extends JButton {
 
        private boolean seleccionado = false;
 
        BotonMenu(String texto) {
            super(texto);
            setFont(Estilos.fuente(Font.PLAIN, 15));
            setForeground(Estilos.TEXTO_CLARO);
            setHorizontalAlignment(SwingConstants.LEFT);
            setBorder(BorderFactory.createEmptyBorder(0, 28, 0, 12));
            setContentAreaFilled(false);
            setFocusPainted(false);
            setOpaque(false);
            setRolloverEnabled(true);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            setPreferredSize(new Dimension(240, 42));
            setMaximumSize(new Dimension(Integer.MAX_VALUE, 42));
            setAlignmentX(LEFT_ALIGNMENT);
        }
 
        void setSeleccionado(boolean seleccionado) {
            this.seleccionado = seleccionado;
            setForeground(seleccionado ? Estilos.CARBON : Estilos.TEXTO_CLARO);
            setFont(Estilos.fuente(seleccionado ? Font.BOLD : Font.PLAIN, 15));
            repaint();
        }
 
        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            if (seleccionado) {
                g2.setColor(Estilos.VERDE);
                g2.fillRect(0, 0, getWidth(), getHeight());
            } else if (getModel().isRollover()) {
                g2.setColor(Estilos.CARBON_HOVER);
                g2.fillRect(0, 0, getWidth(), getHeight());
            }
            g2.dispose();
            super.paintComponent(g);
        }
    }
    
}
