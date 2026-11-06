package pkg5cinco;
 
import interfaz.Login;
import java.awt.EventQueue;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;
 
/**
 * Clase principal del proyecto 5CINCO.
 * Configura el aspecto de la ventana y abre el Login.
 */
public class Main {
 
    private static final java.util.logging.Logger logger =
            java.util.logging.Logger.getLogger(Main.class.getName());
 
    public static void main(String[] args) {
        try {
            // Look & Feel estándar de Java: respeta los colores definidos en la interfaz
            UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());
        } catch (ReflectiveOperationException | UnsupportedLookAndFeelException ex) {
            logger.log(java.util.logging.Level.SEVERE, null, ex);
        }
        EventQueue.invokeLater(() -> new Login().setVisible(true));
    }
}
