package web;

import java.sql.SQLException;
import java.util.*;

/** Resultado de una consulta como lista de filas (para reportes y Excel). */
public final class Res {
    public final List<String> columnas = new ArrayList<>();
    public final List<Object[]> filas = new ArrayList<>();

    static Res consultar(String sql, Object... p) throws SQLException {
        Res r = new Res();
        for (Map<String, Object> f : Db.consultar(sql, p)) {
            if (r.columnas.isEmpty()) r.columnas.addAll(f.keySet());
            r.filas.add(f.values().toArray());
        }
        return r;
    }
}
