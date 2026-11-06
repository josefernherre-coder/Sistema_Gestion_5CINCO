package web;

import java.io.IOException;
import java.nio.file.*;
import java.sql.*;
import java.util.*;

/** Acceso a SQLite. La base de trabajo se copia desde la base ficticia de demo/ y se puede restaurar. */
public class Db {
    static final Path SEMILLA = Paths.get(System.getenv().getOrDefault("DEMO_DB", "demo/5CINCO_demo.db"));
    static final Path TRABAJO = Paths.get(System.getenv().getOrDefault("DATA_DIR", "data")).resolve("5CINCO.db");

    static synchronized void reiniciar() throws IOException {
        Files.createDirectories(TRABAJO.toAbsolutePath().getParent());
        Files.copy(SEMILLA, TRABAJO, StandardCopyOption.REPLACE_EXISTING);
    }

    static Connection abrir() throws SQLException {
        Connection c = DriverManager.getConnection("jdbc:sqlite:" + TRABAJO.toAbsolutePath());
        try (Statement s = c.createStatement()) {
            s.execute("PRAGMA foreign_keys=ON");
            s.execute("PRAGMA busy_timeout=5000");
        }
        return c;
    }

    private static void poner(PreparedStatement ps, Object[] p) throws SQLException {
        for (int i = 0; i < p.length; i++) ps.setObject(i + 1, p[i]);
    }

    static List<Map<String, Object>> consultar(String sql, Object... p) throws SQLException {
        try (Connection c = abrir(); PreparedStatement ps = c.prepareStatement(sql)) {
            poner(ps, p);
            try (ResultSet rs = ps.executeQuery()) {
                ResultSetMetaData md = rs.getMetaData();
                List<Map<String, Object>> filas = new ArrayList<>();
                while (rs.next()) {
                    Map<String, Object> f = new LinkedHashMap<>();
                    for (int i = 1; i <= md.getColumnCount(); i++) f.put(md.getColumnLabel(i), rs.getObject(i));
                    filas.add(f);
                }
                return filas;
            }
        }
    }

    static Map<String, Object> fila(String sql, Object... p) throws SQLException {
        List<Map<String, Object>> l = consultar(sql, p);
        return l.isEmpty() ? null : l.get(0);
    }

    static long contar(String sql, Object... p) throws SQLException {
        Map<String, Object> f = fila(sql, p);
        return f == null ? 0 : ((Number) f.values().iterator().next()).longValue();
    }

    static int ejecutar(String sql, Object... p) throws SQLException {
        try (Connection c = abrir(); PreparedStatement ps = c.prepareStatement(sql)) {
            poner(ps, p);
            return ps.executeUpdate();
        }
    }

    static String mensaje(SQLException e) {
        String m = String.valueOf(e.getMessage());
        if (m.contains("UNIQUE")) return "Ya existe un registro con ese valor (código o documento repetido).";
        if (m.contains("FOREIGN KEY")) return "No se puede: el registro está relacionado con otros datos.";
        if (m.contains("CHECK")) return "Hay un valor fuera del rango permitido.";
        if (m.contains("NOT NULL")) return "Falta un dato obligatorio.";
        return "No se pudo guardar el dato.";
    }
}
