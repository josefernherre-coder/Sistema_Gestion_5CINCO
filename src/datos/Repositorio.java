package datos;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Operaciones genéricas sobre la base 5CINCO. Todas usan PreparedStatement.
 * Los nombres de tabla y columna vienen del código de la aplicación (nunca del usuario).
 */
public final class Repositorio {

    private Repositorio() {
    }

    /** Resultado de una consulta: nombres de columna (alias) y filas. */
    public static final class Resultado {

        public final List<String> columnas = new ArrayList<>();
        public final List<Object[]> filas = new ArrayList<>();
    }

    private static void asignar(PreparedStatement ps, Object... params) throws SQLException {
        for (int i = 0; i < params.length; i++) {
            ps.setObject(i + 1, params[i]);
        }
    }

    public static Resultado consultar(String sql, Object... params) throws SQLException {
        Resultado r = new Resultado();
        try (Connection cn = Conexion.obtener(); PreparedStatement ps = cn.prepareStatement(sql)) {
            asignar(ps, params);
            try (ResultSet rs = ps.executeQuery()) {
                ResultSetMetaData md = rs.getMetaData();
                int n = md.getColumnCount();
                for (int i = 1; i <= n; i++) {
                    r.columnas.add(md.getColumnLabel(i));
                }
                while (rs.next()) {
                    Object[] fila = new Object[n];
                    for (int i = 0; i < n; i++) {
                        fila[i] = rs.getObject(i + 1);
                    }
                    r.filas.add(fila);
                }
            }
        }
        return r;
    }

    /** Ejecuta un INSERT, UPDATE o DELETE con parámetros. */
    public static void ejecutar(String sql, Object... params) throws SQLException {
        try (Connection cn = Conexion.obtener(); PreparedStatement ps = cn.prepareStatement(sql)) {
            asignar(ps, params);
            ps.executeUpdate();
        }
    }

    /** Primer valor de la primera fila (o null). */
    public static Object escalar(String sql, Object... params) throws SQLException {
        Resultado r = consultar(sql, params);
        return r.filas.isEmpty() ? null : r.filas.get(0)[0];
    }

    public static long contar(String tabla) throws SQLException {
        Object o = escalar("SELECT COUNT(*) FROM " + tabla);
        return o == null ? 0 : ((Number) o).longValue();
    }

    /** Una fila completa por su llave primaria, como columna -> valor. */
    public static Map<String, Object> fila(String tabla, String pk, long id) throws SQLException {
        Resultado r = consultar("SELECT * FROM " + tabla + " WHERE " + pk + " = ?", id);
        Map<String, Object> m = new LinkedHashMap<>();
        if (!r.filas.isEmpty()) {
            for (int i = 0; i < r.columnas.size(); i++) {
                m.put(r.columnas.get(i), r.filas.get(0)[i]);
            }
        }
        return m;
    }

    public static long insertar(String tabla, Map<String, Object> valores) throws SQLException {
        StringBuilder cols = new StringBuilder();
        StringBuilder marcas = new StringBuilder();
        for (String c : valores.keySet()) {
            if (cols.length() > 0) {
                cols.append(", ");
                marcas.append(", ");
            }
            cols.append(c);
            marcas.append("?");
        }
        String sql = "INSERT INTO " + tabla + " (" + cols + ") VALUES (" + marcas + ")";
        try (Connection cn = Conexion.obtener(); PreparedStatement ps = cn.prepareStatement(sql)) {
            asignar(ps, valores.values().toArray());
            ps.executeUpdate();
            try (Statement st = cn.createStatement(); ResultSet rs = st.executeQuery("SELECT last_insert_rowid()")) {
                return rs.next() ? rs.getLong(1) : -1;
            }
        }
    }

    public static void actualizar(String tabla, String pk, long id, Map<String, Object> valores) throws SQLException {
        StringBuilder set = new StringBuilder();
        for (String c : valores.keySet()) {
            if (set.length() > 0) {
                set.append(", ");
            }
            set.append(c).append(" = ?");
        }
        String sql = "UPDATE " + tabla + " SET " + set + " WHERE " + pk + " = ?";
        try (Connection cn = Conexion.obtener(); PreparedStatement ps = cn.prepareStatement(sql)) {
            Object[] params = new Object[valores.size() + 1];
            System.arraycopy(valores.values().toArray(), 0, params, 0, valores.size());
            params[valores.size()] = id;
            asignar(ps, params);
            ps.executeUpdate();
        }
    }

    public static void eliminar(String tabla, String pk, long id) throws SQLException {
        try (Connection cn = Conexion.obtener();
                PreparedStatement ps = cn.prepareStatement("DELETE FROM " + tabla + " WHERE " + pk + " = ?")) {
            ps.setLong(1, id);
            ps.executeUpdate();
        }
    }

    /** Traduce un error de SQLite a un mensaje entendible. */
    public static String mensajeAmigable(SQLException ex, String mensajeCheck) {
        String m = String.valueOf(ex.getMessage());
        if (m.contains("UNIQUE")) {
            return "Ya existe un registro con ese valor (documento, código, proyecto o año ya usado).";
        }
        if (m.contains("FOREIGN KEY")) {
            return "No se puede completar: el registro está relacionado con otros datos.";
        }
        if (m.contains("CHECK")) {
            return mensajeCheck != null ? mensajeCheck : "Algún valor no está permitido. Revisa los datos.";
        }
        if (m.contains("NOT NULL")) {
            return "Falta un dato obligatorio.";
        }
        return "Error de base de datos: " + m;
    }
}
