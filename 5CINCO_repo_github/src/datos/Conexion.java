package datos;

import java.io.File;
import java.net.URISyntaxException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Conexión con la base de datos SQLite 5CINCO (archivo 5CINCO.db).
 *
 * Busca el archivo en la carpeta del proyecto (donde NetBeans ejecuta la
 * aplicación) y, si se ejecuta desde el .jar, junto al .jar o en su carpeta
 * superior. Uso:
 *
 *   try (Connection cn = Conexion.obtener()) { ... }
 */
public final class Conexion {

    public static final String NOMBRE_BD = "5CINCO.db";

    private Conexion() {
    }

    /** Abre una conexión nueva. Quien la pide es responsable de cerrarla. */
    public static Connection obtener() throws SQLException {
        File bd = ubicarBD();
        if (bd == null) {
            throw new SQLException("No se encontró el archivo " + NOMBRE_BD
                    + ". Debe estar en la carpeta del proyecto.");
        }
        Connection cn = DriverManager.getConnection("jdbc:sqlite:" + bd.getAbsolutePath());
        try (Statement st = cn.createStatement()) {
            st.execute("PRAGMA foreign_keys = ON"); // SQLite la trae apagada por defecto
            asegurarTablasNuevas(st);
        }
        return cn;
    }

    private static boolean tablasListas = false;

    /**
     * Crea (solo si no existen) las tablas de códigos de compra y de PDFs de contratos.
     * Así la aplicación funciona con cualquier copia de 5CINCO.db, nueva o antigua.
     */
    private static synchronized void asegurarTablasNuevas(Statement st) throws SQLException {
        if (tablasListas) {
            return;
        }
        // Catálogo de códigos (clasificador UNSPSC que se usa en el RUP / SECOP)
        st.execute("CREATE TABLE IF NOT EXISTS codigos_compra ("
                + "id_codigo INTEGER PRIMARY KEY AUTOINCREMENT, "
                + "codigo VARCHAR NOT NULL UNIQUE, "
                + "descripcion VARCHAR NOT NULL, "
                + "tipo VARCHAR CHECK (tipo IS NULL OR tipo IN ('bien','material','servicio')), "
                + "grupo VARCHAR)");
        // Copias antiguas de la base: agregar la columna "grupo" si falta
        if (!tieneColumna(st, "codigos_compra", "grupo")) {
            st.execute("ALTER TABLE codigos_compra ADD COLUMN grupo VARCHAR");
        }
        // Qué códigos tiene cada contrato (muchos a muchos)
        st.execute("CREATE TABLE IF NOT EXISTS contrato_codigos ("
                + "id_contrato INTEGER NOT NULL REFERENCES contratos(id_contrato) ON DELETE CASCADE, "
                + "id_codigo INTEGER NOT NULL REFERENCES codigos_compra(id_codigo), "
                + "PRIMARY KEY (id_contrato, id_codigo))");
        // PDFs guardados dentro de la base de datos (un contrato puede tener varios)
        st.execute("CREATE TABLE IF NOT EXISTS contrato_documentos ("
                + "id_documento INTEGER PRIMARY KEY AUTOINCREMENT, "
                + "id_contrato INTEGER NOT NULL REFERENCES contratos(id_contrato) ON DELETE CASCADE, "
                + "nombre_archivo VARCHAR NOT NULL, "
                + "descripcion VARCHAR, "
                + "tamano INTEGER NOT NULL, "
                + "fecha_carga DATE NOT NULL DEFAULT (date('now')), "
                + "pdf BLOB NOT NULL)");
        st.execute("CREATE INDEX IF NOT EXISTS ix_contrato_codigos_codigo ON contrato_codigos(id_codigo)");
        st.execute("CREATE INDEX IF NOT EXISTS ix_contrato_docs_contrato ON contrato_documentos(id_contrato)");
        migrarAsignaciones(st);
        asegurarCargaAutomatica(st);
        tablasListas = true;
    }

    /**
     * Carga automática de códigos de compra: cada producto puede tener un código (productos.id_codigo) y,
     * cuando se agrega una línea a un contrato (o a su proyecto de origen), o se crea el contrato con
     * un proyecto de origen, los códigos de esas líneas se asignan solos al contrato. Solo agrega,
     * nunca quita códigos que ya tenga el contrato.
     */
    private static void asegurarCargaAutomatica(Statement st) throws SQLException {
        if (!tieneColumna(st, "productos", "id_codigo")) {
            st.execute("ALTER TABLE productos ADD COLUMN id_codigo INTEGER REFERENCES codigos_compra(id_codigo)");
        }
        String deLinea = "BEGIN INSERT OR IGNORE INTO contrato_codigos(id_contrato, id_codigo) "
                + "SELECT c.id_contrato, p.id_codigo FROM productos p, contratos c "
                + "WHERE p.id_producto = NEW.id_producto AND p.id_codigo IS NOT NULL "
                + "AND (c.id_contrato = NEW.id_contrato OR (NEW.id_proyecto IS NOT NULL AND c.id_proyecto = NEW.id_proyecto)); END";
        st.execute("CREATE TRIGGER IF NOT EXISTS trg_detalle_codigos_ai AFTER INSERT ON detalle " + deLinea);
        st.execute("CREATE TRIGGER IF NOT EXISTS trg_detalle_codigos_au AFTER UPDATE OF id_producto ON detalle " + deLinea);
        String deContrato = "WHEN NEW.id_proyecto IS NOT NULL BEGIN INSERT OR IGNORE INTO contrato_codigos(id_contrato, id_codigo) "
                + "SELECT NEW.id_contrato, p.id_codigo FROM detalle d JOIN productos p ON p.id_producto = d.id_producto "
                + "WHERE d.id_proyecto = NEW.id_proyecto AND p.id_codigo IS NOT NULL; END";
        st.execute("CREATE TRIGGER IF NOT EXISTS trg_contrato_codigos_ai AFTER INSERT ON contratos " + deContrato);
        st.execute("CREATE TRIGGER IF NOT EXISTS trg_contrato_codigos_au AFTER UPDATE OF id_proyecto ON contratos " + deContrato);
    }

    private static boolean tieneColumna(Statement st, String tabla, String columna) throws SQLException {
        try (java.sql.ResultSet rs = st.executeQuery("PRAGMA table_info(" + tabla + ")")) {
            while (rs.next()) {
                if (columna.equalsIgnoreCase(rs.getString("name"))) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Las asignaciones de personal van a un CONTRATO (el contrato es lo que se ejecuta;
     * el proyecto solo es la propuesta). Si la base es antigua (asignaciones ligadas a proyecto),
     * se reconstruye la tabla. Solo se migra si cada asignación vieja tiene un contrato que
     * salió de su proyecto; si no, se deja la tabla como estaba.
     */
    private static void migrarAsignaciones(Statement st) throws SQLException {
        if (!tieneColumna(st, "asignaciones", "id_proyecto")) {
            return;
        }
        long sinContrato;
        try (java.sql.ResultSet rs = st.executeQuery("SELECT COUNT(*) FROM asignaciones a "
                + "WHERE NOT EXISTS (SELECT 1 FROM contratos c WHERE c.id_proyecto = a.id_proyecto)")) {
            sinContrato = rs.next() ? rs.getLong(1) : 0;
        }
        if (sinContrato > 0) {
            System.err.println("5CINCO: hay asignaciones ligadas a proyectos sin contrato; no se migraron.");
            return;
        }
        st.execute("ALTER TABLE asignaciones RENAME TO asignaciones_vieja");
        st.execute("CREATE TABLE asignaciones ("
                + "id_asignacion INTEGER PRIMARY KEY AUTOINCREMENT, "
                + "id_contrato INTEGER NOT NULL REFERENCES contratos(id_contrato) ON DELETE CASCADE, "
                + "id_empleado INTEGER REFERENCES empleados(id_empleado), "
                + "id_contratista INTEGER REFERENCES contratistas(id_contratista), "
                + "actividad VARCHAR, fecha_inicio DATE, fecha_fin DATE, "
                + "valor_pagado NUMERIC CHECK (valor_pagado IS NULL OR valor_pagado >= 0), "
                + "CHECK ((id_empleado IS NULL) <> (id_contratista IS NULL)), "
                + "CHECK (valor_pagado IS NULL OR id_contratista IS NOT NULL))");
        st.execute("INSERT INTO asignaciones (id_asignacion, id_contrato, id_empleado, id_contratista, actividad, "
                + "fecha_inicio, fecha_fin, valor_pagado) SELECT a.id_asignacion, "
                + "(SELECT c.id_contrato FROM contratos c WHERE c.id_proyecto = a.id_proyecto), a.id_empleado, "
                + "a.id_contratista, a.actividad, a.fecha_inicio, a.fecha_fin, a.valor_pagado FROM asignaciones_vieja a");
        st.execute("DROP TABLE asignaciones_vieja");
        st.execute("CREATE INDEX IF NOT EXISTS ix_asignaciones_contrato ON asignaciones(id_contrato)");
    }

    private static File ubicarBD() {
        File[] candidatos = {
            new File(System.getProperty("user.dir"), NOMBRE_BD),
            new File(carpetaDelCodigo(), NOMBRE_BD),
            new File(carpetaDelCodigo().getParentFile(), NOMBRE_BD),
            new File(carpetaDelCodigo().getParentFile() == null ? null
                    : carpetaDelCodigo().getParentFile().getParentFile(), NOMBRE_BD)
        };
        for (File f : candidatos) {
            if (f != null && f.isFile()) {
                return f;
            }
        }
        return null;
    }

    private static File carpetaDelCodigo() {
        try {
            File origen = new File(Conexion.class.getProtectionDomain()
                    .getCodeSource().getLocation().toURI());
            return origen.isFile() ? origen.getParentFile() : origen;
        } catch (URISyntaxException | RuntimeException ex) {
            return new File(System.getProperty("user.dir"));
        }
    }
}
