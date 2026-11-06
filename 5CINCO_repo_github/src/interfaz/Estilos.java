/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package interfaz;

import java.awt.Color;
import java.awt.Font;

/**
 *
 * @author JOSE FERNANDO
 */
public class Estilos {
    
     public static final Color VERDE = new Color(166, 206, 58);        // color corporativo
    public static final Color VERDE_OSCURO = new Color(92, 124, 18);
    public static final Color CARBON = new Color(36, 40, 33);          // negro del logo
    public static final Color CARBON_HOVER = new Color(56, 62, 51);
    public static final Color TEXTO_CLARO = new Color(232, 236, 226);
    public static final Color GRIS_TEXTO = new Color(98, 104, 92);
    public static final Color FONDO = new Color(246, 247, 243);
    public static final Color BORDE = new Color(200, 205, 194);
 
    private static final String FUENTE = "Segoe UI";
 
    public static Font fuente(int estilo, int tamano) {
        return new Font(FUENTE, estilo, tamano);
    }
    

    /** Botón con el estilo corporativo (principal = verde, secundario = gris). */
    public static javax.swing.JButton boton(String texto, boolean principal) {
        javax.swing.JButton b = new javax.swing.JButton(texto);
        b.setFont(fuente(Font.BOLD, 14));
        b.setFocusPainted(false);
        b.setOpaque(true);
        b.setBorderPainted(false);
        b.setBackground(principal ? VERDE : new Color(226, 229, 220));
        b.setForeground(CARBON);
        b.setCursor(java.awt.Cursor.getPredefinedCursor(java.awt.Cursor.HAND_CURSOR));
        b.setBorder(javax.swing.BorderFactory.createEmptyBorder(9, 18, 9, 18));
        return b;
    }

    /** Aplica el aspecto corporativo a una tabla. */
    public static void estilizarTabla(javax.swing.JTable t) {
        t.setRowHeight(28);
        t.setFont(fuente(Font.PLAIN, 13));
        t.setGridColor(new Color(228, 231, 222));
        t.setSelectionBackground(new Color(214, 236, 160));
        t.setSelectionForeground(CARBON);
        t.setFillsViewportHeight(true);
        t.setAutoResizeMode(javax.swing.JTable.AUTO_RESIZE_OFF);
        javax.swing.table.JTableHeader h = t.getTableHeader();
        h.setFont(fuente(Font.BOLD, 13));
        h.setBackground(CARBON);
        h.setForeground(Color.WHITE);
        h.setReorderingAllowed(false);
        ((javax.swing.table.DefaultTableCellRenderer) h.getDefaultRenderer()).setHorizontalAlignment(javax.swing.SwingConstants.LEFT);
    }

    /** Ajusta el ancho de cada columna al contenido (con un máximo). */
    public static void ajustarColumnas(javax.swing.JTable t) {
        for (int c = 0; c < t.getColumnCount(); c++) {
            int ancho = 60;
            java.awt.Component hc = t.getTableHeader().getDefaultRenderer().getTableCellRendererComponent(
                    t, t.getColumnModel().getColumn(c).getHeaderValue(), false, false, 0, c);
            ancho = Math.max(ancho, hc.getPreferredSize().width + 16);
            for (int r = 0; r < Math.min(t.getRowCount(), 200); r++) {
                java.awt.Component comp = t.prepareRenderer(t.getCellRenderer(r, c), r, c);
                ancho = Math.max(ancho, comp.getPreferredSize().width + 16);
            }
            t.getColumnModel().getColumn(c).setPreferredWidth(Math.min(ancho, 380));
        }
    }

    /** Formato de números con la convención colombiana (punto de miles, coma decimal). */
    public static String numero(Object n) {
        java.text.NumberFormat f = java.text.NumberFormat.getNumberInstance(new java.util.Locale("es", "CO"));
        f.setMaximumFractionDigits(2);
        return f.format(n);
    }
}
