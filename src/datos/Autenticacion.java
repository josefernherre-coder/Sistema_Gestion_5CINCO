package datos;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

/**
 * Validación de usuario y contraseña contra la tabla "usuarios".
 * La base guarda pbkdf2_sha256$iteraciones$sal$hash (hex); aquí se recalcula y compara.
 */
public final class Autenticacion {

    private Autenticacion() {
    }

    /** @return la sesión si las credenciales son correctas y el usuario está activo; si no, null. */
    public static Sesion autenticar(String usuario, char[] clave) throws SQLException {
        String guardado = null;
        String rol = null;
        try (Connection cn = Conexion.obtener();
                PreparedStatement ps = cn.prepareStatement(
                        "SELECT clave_hash, rol FROM usuarios WHERE usuario = ? AND activo = 1")) {
            ps.setString(1, usuario);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    guardado = rs.getString(1);
                    rol = rs.getString(2);
                }
            }
        }
        if (guardado == null || !coincide(guardado, clave)) {
            return null;
        }
        return new Sesion(usuario, rol);
    }

    private static final int ITERACIONES = 600000;

    /** Cifra una clave nueva: pbkdf2_sha256$iteraciones$sal$hash (hex). La clave nunca se guarda en texto plano. */
    public static String hashear(char[] clave) {
        try {
            byte[] sal = new byte[16];
            new java.security.SecureRandom().nextBytes(sal);
            PBEKeySpec spec = new PBEKeySpec(clave, sal, ITERACIONES, 256);
            try {
                byte[] h = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).getEncoded();
                return "pbkdf2_sha256$" + ITERACIONES + "$" + aHex(sal) + "$" + aHex(h);
            } finally {
                spec.clearPassword();
            }
        } catch (NoSuchAlgorithmException | InvalidKeySpecException ex) {
            throw new IllegalStateException("No se pudo cifrar la clave", ex);
        }
    }

    private static String aHex(byte[] b) {
        StringBuilder sb = new StringBuilder();
        for (byte x : b) {
            sb.append(String.format("%02x", x));
        }
        return sb.toString();
    }

    private static boolean coincide(String guardado, char[] clave) {
        String[] p = guardado.split("\\$");
        if (p.length != 4 || !p[0].equals("pbkdf2_sha256")) {
            return false;
        }
        try {
            byte[] sal = hexABytes(p[2]);
            byte[] esperado = hexABytes(p[3]);
            PBEKeySpec spec = new PBEKeySpec(clave, sal, Integer.parseInt(p[1]), esperado.length * 8);
            try {
                byte[] calculado = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
                        .generateSecret(spec).getEncoded();
                return MessageDigest.isEqual(esperado, calculado); // tiempo constante
            } finally {
                spec.clearPassword();
            }
        } catch (NoSuchAlgorithmException | InvalidKeySpecException | NumberFormatException ex) {
            return false;
        }
    }

    private static byte[] hexABytes(String hex) {
        byte[] b = new byte[hex.length() / 2];
        for (int i = 0; i < b.length; i++) {
            b[i] = (byte) Integer.parseInt(hex.substring(2 * i, 2 * i + 2), 16);
        }
        return b;
    }
}
