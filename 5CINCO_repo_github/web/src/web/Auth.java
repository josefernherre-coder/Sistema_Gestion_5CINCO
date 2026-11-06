package web;

import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

/** Verifica claves PBKDF2 (mismo formato que la app de escritorio) y maneja las sesiones. */
public class Auth {
    record Sesion(String usuario, String rol) { boolean esSuper() { return "superusuario".equals(rol); } }

    private static final Map<String, Sesion> SESIONES = new ConcurrentHashMap<>();
    private static final SecureRandom RND = new SecureRandom();

    static Sesion autenticar(String usuario, String clave) {
        try {
            Map<String, Object> f = Db.fila("SELECT clave_hash, rol, activo FROM usuarios WHERE usuario = ?", usuario);
            boolean ok = false;
            if (f != null && ((Number) f.get("activo")).intValue() == 1) ok = verificar(clave, (String) f.get("clave_hash"));
            else verificar(clave, "pbkdf2_sha256$600000$00$00"); // tiempo similar si el usuario no existe
            return ok ? new Sesion(usuario, (String) f.get("rol")) : null;
        } catch (Exception e) {
            return null;
        }
    }

    static boolean verificar(String clave, String guardado) {
        try {
            String[] p = guardado.split("\\$");
            if (p.length != 4 || !p[0].equals("pbkdf2_sha256")) return false;
            byte[] sal = hex(p[2]), esperado = hex(p[3]);
            PBEKeySpec spec = new PBEKeySpec(clave.toCharArray(), sal, Integer.parseInt(p[1]), esperado.length * 8);
            byte[] calc = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).getEncoded();
            return MessageDigest.isEqual(calc, esperado);
        } catch (Exception e) {
            return false;
        }
    }

    /** Cifra una clave nueva con el mismo formato de la app de escritorio. */
    static String hashear(String clave) {
        try {
            byte[] sal = new byte[16];
            RND.nextBytes(sal);
            PBEKeySpec spec = new PBEKeySpec(clave.toCharArray(), sal, 600000, 256);
            byte[] h = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).getEncoded();
            return "pbkdf2_sha256$600000$" + aHex(sal) + "$" + aHex(h);
        } catch (Exception e) { throw new IllegalStateException("No se pudo cifrar la clave", e); }
    }

    private static String aHex(byte[] b) { StringBuilder sb = new StringBuilder(); for (byte x : b) sb.append(String.format("%02x", x)); return sb.toString(); }

    private static byte[] hex(String s) {
        byte[] b = new byte[s.length() / 2];
        for (int i = 0; i < b.length; i++) b[i] = (byte) Integer.parseInt(s.substring(2 * i, 2 * i + 2), 16);
        return b;
    }

    static String crear(Sesion s) {
        byte[] b = new byte[24];
        RND.nextBytes(b);
        StringBuilder sb = new StringBuilder();
        for (byte x : b) sb.append(String.format("%02x", x));
        SESIONES.put(sb.toString(), s);
        return sb.toString();
    }

    static Sesion de(String id) { return id == null ? null : SESIONES.get(id); }
    static void cerrar(String id) { if (id != null) SESIONES.remove(id); }
    static void limpiar() { SESIONES.clear(); }
}
