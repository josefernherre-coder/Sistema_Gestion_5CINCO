/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package interfaz;

import java.awt.Color;
import java.awt.Font;
import java.awt.GridBagLayout;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;

/**
 *
 * @author JOSE FERNANDO
 */
public class PanelModulo extends JPanel {
    
    public PanelModulo(String titulo, String descripcion) {
        super(new GridBagLayout());
        setBackground(Estilos.FONDO);
 
        JPanel tarjeta = new JPanel();
        tarjeta.setLayout(new BoxLayout(tarjeta, BoxLayout.Y_AXIS));
        tarjeta.setBackground(Color.WHITE);
        tarjeta.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Estilos.BORDE, 1),
                BorderFactory.createCompoundBorder(
                        BorderFactory.createMatteBorder(4, 0, 0, 0, Estilos.VERDE),
                        BorderFactory.createEmptyBorder(28, 36, 28, 36))));
 
        JLabel lblTitulo = new JLabel(titulo);
        lblTitulo.setFont(Estilos.fuente(Font.BOLD, 24));
        lblTitulo.setForeground(Estilos.CARBON);
        lblTitulo.setAlignmentX(LEFT_ALIGNMENT);
 
        JLabel lblDescripcion = new JLabel(descripcion);
        lblDescripcion.setFont(Estilos.fuente(Font.PLAIN, 15));
        lblDescripcion.setForeground(Estilos.GRIS_TEXTO);
        lblDescripcion.setAlignmentX(LEFT_ALIGNMENT);
 
        JLabel lblNota = new JLabel("En construcción: se desarrollará cuando esté lista la base de datos.");
        lblNota.setFont(Estilos.fuente(Font.ITALIC, 13));
        lblNota.setForeground(Estilos.VERDE_OSCURO);
        lblNota.setAlignmentX(LEFT_ALIGNMENT);
 
        tarjeta.add(lblTitulo);
        tarjeta.add(Box.createVerticalStrut(8));
        tarjeta.add(lblDescripcion);
        tarjeta.add(Box.createVerticalStrut(20));
        tarjeta.add(lblNota);
 
        add(tarjeta);
    }
    
}
