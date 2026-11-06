package web;

import java.sql.SQLException;
import java.time.LocalDate;
import java.util.*;

/** Pantallas que no son un CRUD simple: códigos de un contrato, reportes y usuarios. */
public class Extras {

    // ================= Códigos de compra de un contrato =================
    static String codigosContrato(long idc) throws SQLException {
        Map<String, Object> c = Db.fila("SELECT codigo_interno, objeto FROM contratos WHERE id_contrato=?", idc);
        if (c == null) return null;
        Set<Long> marcados = new HashSet<>();
        for (Map<String, Object> r : Db.consultar("SELECT id_codigo FROM contrato_codigos WHERE id_contrato=?", idc))
            marcados.add(((Number) r.get("id_codigo")).longValue());
        StringBuilder sb = new StringBuilder("<h1>Códigos de compra · " + Html.e(c.get("codigo_interno")) + "</h1><p>" + Html.e(c.get("objeto"))
            + "</p><p class=\"pie\">Los códigos de los productos usados en las líneas de cálculo se asignan solos; aquí puedes agregar o quitar los que quieras.</p>"
            + "<form class=\"form\" method=\"post\" action=\"/m/contratos/" + idc + "/codigos\">");
        for (Map<String, Object> r : Db.consultar("SELECT id_codigo, codigo, descripcion, tipo FROM codigos_compra ORDER BY codigo")) {
            long id = ((Number) r.get("id_codigo")).longValue();
            sb.append("<label class=\"ancho\" style=\"flex-direction:row;align-items:center;gap:10px;color:inherit\"><input type=\"checkbox\" style=\"width:auto\" name=\"c_")
              .append(id).append("\" value=\"1\"").append(marcados.contains(id) ? " checked" : "").append("> <span><b>").append(Html.e(r.get("codigo"))).append("</b> · ")
              .append(Html.e(r.get("descripcion"))).append(r.get("tipo") == null ? "" : " (" + Html.e(r.get("tipo")) + ")").append("</span></label>");
        }
        sb.append("<div class=\"acciones\"><button class=\"btn\">Guardar</button><a class=\"btn sec\" href=\"/m/contratos\">Cancelar</a></div></form>");
        return sb.toString();
    }

    static void guardarCodigos(long idc, Map<String, String> f) throws SQLException {
        try (var cn = Db.abrir()) {
            cn.setAutoCommit(false);
            try (var del = cn.prepareStatement("DELETE FROM contrato_codigos WHERE id_contrato=?");
                 var ins = cn.prepareStatement("INSERT OR IGNORE INTO contrato_codigos(id_contrato, id_codigo) VALUES(?,?)")) {
                del.setLong(1, idc); del.executeUpdate();
                for (String k : f.keySet()) {
                    if (!k.matches("c_\\d{1,12}")) continue;
                    ins.setLong(1, idc); ins.setLong(2, Long.parseLong(k.substring(2))); ins.executeUpdate();
                }
                cn.commit();
            } catch (SQLException e) { cn.rollback(); throw e; }
        }
    }

    // ================= Reportes =================
    static LocalDate[] periodo(Map<String, String> q) throws SQLException {
        String anio = q.get("anio");
        if (Mod.hay(anio)) {
            int a = Integer.parseInt(anio.trim());
            return new LocalDate[]{LocalDate.of(a, 1, 1), LocalDate.of(a, 12, 31)};
        }
        LocalDate d, h;
        if (Mod.hay(q.get("desde")) && Mod.hay(q.get("hasta"))) {
            d = LocalDate.parse(q.get("desde").trim());
            h = LocalDate.parse(q.get("hasta").trim());
        } else {
            Map<String, Object> m = Db.fila("SELECT MIN(COALESCE(fecha_terminacion, fecha_inicio)) AS a, MAX(COALESCE(fecha_terminacion, fecha_inicio)) AS b FROM contratos");
            d = m != null && m.get("a") != null ? LocalDate.parse(m.get("a").toString()) : LocalDate.now().withDayOfYear(1);
            h = m != null && m.get("b") != null ? LocalDate.parse(m.get("b").toString()) : LocalDate.now();
        }
        if (h.isBefore(d)) throw new IllegalArgumentException("La fecha final no puede ser anterior a la inicial.");
        return new LocalDate[]{d, h};
    }

    static String reporte(Map<String, String> q) throws SQLException {
        LocalDate[] p;
        String aviso = "";
        try { p = periodo(q); }
        catch (NumberFormatException | java.time.DateTimeException ex) {
            aviso = "<div class=\"err\">Fechas no válidas.</div>";
            p = new LocalDate[]{LocalDate.now().withDayOfYear(1), LocalDate.now()};
        } catch (IllegalArgumentException ex) {
            aviso = "<div class=\"err\">" + Html.e(ex.getMessage()) + "</div>";
            p = new LocalDate[]{LocalDate.now().withDayOfYear(1), LocalDate.now()};
        }
        Res r = Reporte.consultar(p[0], p[1]);
        Res resumen = Reporte.resumen(p[0], p[1]);
        StringBuilder sb = new StringBuilder("<h1>Reporte de contratos</h1>").append(aviso)
            .append("<form class=\"form\" method=\"get\" action=\"/reporte\"><label>Desde<input type=\"date\" name=\"desde\" value=\"").append(p[0]).append("\" required></label>")
            .append("<label>Hasta<input type=\"date\" name=\"hasta\" value=\"").append(p[1]).append("\" required></label>")
            .append("<label>O elige un año<select name=\"anio\" onchange=\"this.form.submit()\"><option value=\"\"></option>");
        for (Map<String, Object> a : Db.consultar("SELECT DISTINCT CAST(strftime('%Y', COALESCE(fecha_terminacion, fecha_inicio)) AS INTEGER) AS a FROM contratos "
                + "WHERE COALESCE(fecha_terminacion, fecha_inicio) IS NOT NULL ORDER BY 1 DESC"))
            sb.append("<option>").append(a.get("a")).append("</option>");
        sb.append("</select></label><div class=\"acciones\"><button class=\"btn\">Ver reporte</button><a class=\"btn sec\" href=\"/reporte/excel?desde=")
          .append(p[0]).append("&hasta=").append(p[1]).append("\">Descargar Excel</a></div></form>")
          .append("<p class=\"pie\">Entran los contratos cuya fecha de terminación (o de inicio, si no tiene terminación) está dentro del periodo. "
              + "El valor de participación es el valor del contrato por su % de participación.</p>");
        sb.append("<h2>Contratos del periodo (").append(r.filas.size()).append(")</h2><div class=\"tabla\"><table><tr>");
        for (String c : r.columnas) sb.append("<th>").append(Html.e(c)).append("</th>");
        sb.append("</tr>");
        for (Object[] f : r.filas) {
            sb.append("<tr>");
            for (int i = 0; i < f.length; i++) {
                String v = i == Reporte.COL_VALOR_CONTRATO || i == Reporte.COL_VALOR_PARTICIPACION ? Html.dinero(f[i])
                    : (i == 7 || i == 10) ? Html.numero(f[i]) : Html.e(f[i]);
                sb.append(i == 6 || i == 7 || i == 8 || i == 10 ? "<td class=\"d\">" : "<td>").append(v).append("</td>");
            }
            sb.append("</tr>");
        }
        if (r.filas.isEmpty()) sb.append("<tr><td colspan=\"").append(r.columnas.isEmpty() ? 1 : r.columnas.size()).append("\">No hay contratos en el periodo seleccionado.</td></tr>");
        else sb.append("<tr><td colspan=\"6\"><b>TOTAL</b></td><td class=\"d\"><b>").append(Html.dinero(Reporte.sumar(r, Reporte.COL_VALOR_CONTRATO)))
               .append("</b></td><td></td><td class=\"d\"><b>").append(Html.dinero(Reporte.sumar(r, Reporte.COL_VALOR_PARTICIPACION))).append("</b></td><td></td><td></td><td></td></tr>");
        sb.append("</table></div><h2>Total por año</h2><div class=\"tabla\"><table><tr><th>Año</th><th>Contratos</th><th>Valor de los contratos</th><th>Valor participación</th></tr>");
        for (Object[] f : resumen.filas)
            sb.append("<tr><td>").append(Html.e(f[0])).append("</td><td class=\"d\">").append(Html.numero(f[1])).append("</td><td class=\"d\">")
              .append(Html.dinero(f[2])).append("</td><td class=\"d\">").append(Html.dinero(f[3])).append("</td></tr>");
        if (resumen.filas.isEmpty()) sb.append("<tr><td colspan=\"4\">Sin contratos en el periodo.</td></tr>");
        return sb.append("</table></div>").toString();
    }

    // ================= Usuarios (solo superusuario) =================
    static String usuarios(Auth.Sesion yo) throws SQLException {
        StringBuilder sb = new StringBuilder("<h1>Usuarios</h1><p class=\"pie\">Solo el superusuario ve esta sección. Las claves se guardan como hash PBKDF2; nunca en texto plano.</p>"
            + "<h2>Nuevo usuario</h2><form class=\"form\" method=\"post\" action=\"/usuarios/nuevo\">"
            + "<label>Usuario (mínimo 3 caracteres, sin espacios)<input name=\"usuario\" required autocomplete=\"off\"></label>"
            + "<label>Contraseña (mínimo 8 caracteres)<input name=\"clave\" type=\"password\" required autocomplete=\"new-password\"></label>"
            + "<label>Repetir contraseña<input name=\"clave2\" type=\"password\" required autocomplete=\"new-password\"></label>"
            + "<label>Rol<select name=\"rol\"><option value=\"administrador\">Administrador</option><option value=\"superusuario\">Superusuario</option></select></label>"
            + "<div class=\"acciones\"><button class=\"btn\">Crear usuario</button></div></form>"
            + "<h2>Usuarios registrados</h2><div class=\"tabla\"><table><tr><th>Usuario</th><th>Rol</th><th>Estado</th><th>Creado</th><th>Acciones</th></tr>");
        for (Map<String, Object> u : Db.consultar("SELECT id_usuario, usuario, rol, activo, fecha_creacion FROM usuarios ORDER BY usuario")) {
            long id = ((Number) u.get("id_usuario")).longValue();
            boolean yoMismo = u.get("usuario").equals(yo.usuario()), act = ((Number) u.get("activo")).intValue() == 1;
            String base = "/usuarios/" + id;
            sb.append("<tr><td>").append(Html.e(u.get("usuario"))).append(yoMismo ? " (tú)" : "").append("</td><td>").append(Html.e(u.get("rol")))
              .append("</td><td>").append(act ? "activo" : "inactivo").append("</td><td>").append(Html.e(u.get("fecha_creacion"))).append("</td><td class=\"acc\">");
            if (!yoMismo) {
                String nuevoRol = u.get("rol").equals("superusuario") ? "administrador" : "superusuario";
                sb.append("<form method=\"post\" action=\"").append(base).append("/rol\"><input type=\"hidden\" name=\"rol\" value=\"").append(nuevoRol)
                  .append("\"><button class=\"btn sec\">Pasar a ").append(nuevoRol).append("</button></form> ")
                  .append("<form method=\"post\" action=\"").append(base).append("/activo\"><button class=\"btn sec\">").append(act ? "Desactivar" : "Activar").append("</button></form> ")
                  .append("<form method=\"post\" action=\"").append(base).append("/eliminar\" onsubmit=\"return confirm('¿Eliminar este usuario?')\"><button class=\"btn pel\">Eliminar</button></form> ");
            }
            sb.append("<form method=\"post\" action=\"").append(base).append("/clave\" style=\"display:inline-flex;gap:6px\"><input type=\"password\" name=\"clave\" placeholder=\"Nueva clave\" minlength=\"8\" required autocomplete=\"new-password\" style=\"width:140px;padding:6px\">")
              .append("<button class=\"btn sec\">Cambiar clave</button></form></td></tr>");
        }
        return sb.append("</table></div>").toString();
    }

    /** Aplica una acción sobre usuarios; devuelve el mensaje de éxito o lanza IllegalArgumentException. */
    static String accionUsuario(Auth.Sesion yo, String ruta, Map<String, String> f) throws SQLException {
        String[] p = ruta.split("/"); // "", "usuarios", "nuevo" | id, accion
        if (p.length == 3 && p[2].equals("nuevo")) {
            String u = f.getOrDefault("usuario", "").trim(), c = f.getOrDefault("clave", ""), c2 = f.getOrDefault("clave2", "");
            String rol = f.getOrDefault("rol", "");
            if (u.length() < 3 || u.length() > 40 || u.matches(".*\\s.*")) throw new IllegalArgumentException("El usuario debe tener mínimo 3 caracteres y no puede llevar espacios.");
            validarClave(c, c2);
            if (!rol.equals("superusuario") && !rol.equals("administrador")) throw new IllegalArgumentException("Rol no válido.");
            try { Db.ejecutar("INSERT INTO usuarios (usuario, clave_hash, rol) VALUES (?,?,?)", u, Auth.hashear(c), rol); }
            catch (SQLException e) { throw new IllegalArgumentException(String.valueOf(e.getMessage()).contains("UNIQUE") ? "Ya existe un usuario con ese nombre." : "No se pudo crear el usuario."); }
            return "Usuario creado.";
        }
        if (p.length != 4 || !p[2].matches("\\d{1,12}")) throw new IllegalArgumentException("Acción no válida.");
        long id = Long.parseLong(p[2]);
        Map<String, Object> u = Db.fila("SELECT usuario FROM usuarios WHERE id_usuario=?", id);
        if (u == null) throw new IllegalArgumentException("El usuario no existe.");
        boolean yoMismo = u.get("usuario").equals(yo.usuario());
        switch (p[3]) {
            case "rol" -> {
                if (yoMismo) throw new IllegalArgumentException("No puedes cambiar tu propio rol (así siempre queda al menos un superusuario).");
                String rol = f.getOrDefault("rol", "");
                if (!rol.equals("superusuario") && !rol.equals("administrador")) throw new IllegalArgumentException("Rol no válido.");
                Db.ejecutar("UPDATE usuarios SET rol=? WHERE id_usuario=?", rol, id); return "Rol actualizado.";
            }
            case "activo" -> {
                if (yoMismo) throw new IllegalArgumentException("No puedes desactivar tu propio usuario.");
                Db.ejecutar("UPDATE usuarios SET activo = 1 - activo WHERE id_usuario=?", id); return "Estado actualizado.";
            }
            case "clave" -> {
                String c = f.getOrDefault("clave", "");
                validarClave(c, c);
                Db.ejecutar("UPDATE usuarios SET clave_hash=? WHERE id_usuario=?", Auth.hashear(c), id); return "Clave restablecida.";
            }
            case "eliminar" -> {
                if (yoMismo) throw new IllegalArgumentException("No puedes eliminar tu propio usuario.");
                Db.ejecutar("DELETE FROM usuarios WHERE id_usuario=?", id); return "Usuario eliminado.";
            }
            default -> throw new IllegalArgumentException("Acción no válida.");
        }
    }

    private static void validarClave(String c1, String c2) {
        if (c1.length() < 8) throw new IllegalArgumentException("La contraseña debe tener mínimo 8 caracteres.");
        if (c1.length() > 100) throw new IllegalArgumentException("La contraseña es demasiado larga.");
        if (!c1.equals(c2)) throw new IllegalArgumentException("Las contraseñas no coinciden.");
    }
}
