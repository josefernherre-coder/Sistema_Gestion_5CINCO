package web;

import com.sun.net.httpserver.*;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.Executors;

/** Servidor web de la demo 5CINCO (solo JDK + sqlite-jdbc). */
public class Main {
    public static void main(String[] args) throws Exception {
        Class.forName("org.sqlite.JDBC");
        Db.reiniciar();
        int puerto = Integer.parseInt(System.getenv().getOrDefault("PORT", "8080"));
        HttpServer srv = HttpServer.create(new InetSocketAddress(puerto), 0);
        srv.createContext("/", Main::atender);
        srv.setExecutor(Executors.newFixedThreadPool(8));
        srv.start();
        System.out.println("5CINCO demo web en el puerto " + puerto);
    }

    // ---------- utilidades ----------
    static Map<String, String> parsear(String s) {
        Map<String, String> m = new HashMap<>();
        if (s == null || s.isEmpty()) return m;
        for (String par : s.split("&")) {
            int i = par.indexOf('=');
            String k = i < 0 ? par : par.substring(0, i), v = i < 0 ? "" : par.substring(i + 1);
            m.put(URLDecoder.decode(k, StandardCharsets.UTF_8), URLDecoder.decode(v, StandardCharsets.UTF_8));
        }
        return m;
    }

    static Map<String, String> cuerpo(HttpExchange x) throws IOException {
        byte[] b = x.getRequestBody().readNBytes(65536);
        return parsear(new String(b, StandardCharsets.UTF_8));
    }

    static void enviar(HttpExchange x, int codigo, String tipo, String texto) throws IOException {
        byte[] b = texto.getBytes(StandardCharsets.UTF_8);
        x.getResponseHeaders().set("Content-Type", tipo + "; charset=utf-8");
        x.getResponseHeaders().set("X-Content-Type-Options", "nosniff");
        x.getResponseHeaders().set("Cache-Control", "no-store");
        x.sendResponseHeaders(codigo, b.length);
        try (OutputStream o = x.getResponseBody()) { o.write(b); }
    }

    static void ir(HttpExchange x, String destino) throws IOException {
        x.getResponseHeaders().set("Location", destino);
        x.sendResponseHeaders(303, -1);
        x.close();
    }

    static String enc(String s) { return URLEncoder.encode(s, StandardCharsets.UTF_8); }

    static String sid(HttpExchange x) {
        List<String> cs = x.getRequestHeaders().get("Cookie");
        if (cs == null) return null;
        for (String c : cs) for (String p : c.split(";")) { p = p.trim(); if (p.startsWith("sid=")) return p.substring(4); }
        return null;
    }

    static String sufijo(String ok, String err) {
        StringBuilder sb = new StringBuilder();
        if (ok != null) sb.append("ok=").append(enc(ok));
        if (err != null) sb.append(sb.length() > 0 ? "&" : "").append("err=").append(enc(err));
        return sb.toString();
    }

    // ---------- enrutamiento ----------
    static void atender(HttpExchange x) throws IOException {
        try {
            String ruta = x.getRequestURI().getPath(), metodo = x.getRequestMethod();
            Map<String, String> q = parsear(x.getRequestURI().getRawQuery());
            if (ruta.equals("/estilo.css")) { enviar(x, 200, "text/css", Html.CSS); return; }
            if (ruta.equals("/salud")) { enviar(x, 200, "text/plain", "ok"); return; }
            if (ruta.equals("/")) { ir(x, "/inicio"); return; }
            if (ruta.equals("/login")) { login(x, metodo, q); return; }
            Auth.Sesion s = Auth.de(sid(x));
            if (s == null) { ir(x, "/login"); return; }
            if (ruta.equals("/salir") && metodo.equals("POST")) {
                Auth.cerrar(sid(x));
                x.getResponseHeaders().add("Set-Cookie", "sid=; Path=/; Max-Age=0; HttpOnly; SameSite=Lax");
                ir(x, "/login"); return;
            }
            String ok = q.get("ok"), err = q.get("err");
            if (ruta.equals("/inicio")) { enviar(x, 200, "text/html", Html.pagina("Inicio", s, "/inicio", ok, err, inicio(s))); return; }
            if (ruta.equals("/reiniciar") && metodo.equals("POST")) {
                if (!s.esSuper()) { ir(x, "/inicio?" + sufijo(null, "Solo el superusuario puede reiniciar la demo.")); return; }
                Db.reiniciar();
                ir(x, "/inicio?" + sufijo("Datos de ejemplo restaurados.", null)); return;
            }
            if (ruta.equals("/reporte")) { enviar(x, 200, "text/html", Html.pagina("Reportes", s, "/reporte", ok, err, Extras.reporte(q))); return; }
            if (ruta.equals("/reporte/excel")) { excel(x, q); return; }
            if (ruta.startsWith("/usuarios")) { usuarios(x, s, ruta, metodo, ok, err); return; }
            if (ruta.startsWith("/calcular")) { calcular(x, s, ruta, metodo, q, ok, err); return; }
            if (ruta.startsWith("/m/")) { modulo(x, s, ruta, metodo, q, ok, err); return; }
            enviar(x, 404, "text/html", Html.pagina("No encontrado", s, "", null, null, "<h1>Página no encontrada</h1><p><a href=\"/inicio\">Volver al inicio</a></p>"));
        } catch (Exception e) {
            e.printStackTrace();
            try { enviar(x, 500, "text/html", Html.pagina("Error", null, "", null, null, "<h1>Ocurrió un error</h1><p><a href=\"/inicio\">Volver al inicio</a></p>")); } catch (IOException ignorar) { }
        }
    }

    static void login(HttpExchange x, String metodo, Map<String, String> q) throws IOException {
        String err = null;
        if (metodo.equals("POST")) {
            Map<String, String> f = cuerpo(x);
            Auth.Sesion s = Auth.autenticar(f.getOrDefault("usuario", "").trim(), f.getOrDefault("clave", ""));
            if (s != null) {
                x.getResponseHeaders().add("Set-Cookie", "sid=" + Auth.crear(s) + "; Path=/; HttpOnly; SameSite=Lax; Max-Age=28800");
                ir(x, "/inicio"); return;
            }
            err = "Usuario o clave incorrectos.";
        }
        String cuerpo = "<div class=\"login\"><h1>5CINCO · demo</h1><form class=\"form\" method=\"post\" action=\"/login\">"
            + "<label>Usuario<input name=\"usuario\" autocomplete=\"username\" required autofocus></label>"
            + "<label>Clave<input name=\"clave\" type=\"password\" autocomplete=\"current-password\" required></label>"
            + "<div class=\"acciones\"><button class=\"btn\" style=\"flex:1\">Ingresar</button></div></form>"
            + "<div class=\"cred\"><b>Datos de prueba (ficticios)</b><br>Superusuario: <code>demo</code> / <code>demo1234</code><br>Administrador: <code>admin_demo</code> / <code>admin1234</code></div></div>";
        enviar(x, 200, "text/html", Html.pagina("Ingresar", null, "", null, err, cuerpo));
    }

    static String inicio(Auth.Sesion s) throws SQLException {
        StringBuilder sb = new StringBuilder("<h1>Bienvenido, " + Html.e(s.usuario()) + "</h1><p>Esta es una demo web de 5CINCO con <b>datos ficticios</b>. La aplicación real es de escritorio.</p><div class=\"tarjetas\">");
        for (Mod mo : Mod.TODOS.values())
            sb.append("<a class=\"tarjeta\" style=\"text-decoration:none;color:inherit\" href=\"/m/").append(mo.id).append("\"><b>").append(Db.contar("SELECT COUNT(*) FROM " + mo.tabla)).append("</b><span>").append(Html.e(mo.titulo.replace(" (propuestas)", ""))).append("</span></a>");
        sb.append("</div>");
        if (s.esSuper()) sb.append("<form method=\"post\" action=\"/reiniciar\" onsubmit=\"return confirm('Se perderán los cambios hechos en la demo. ¿Continuar?')\"><button class=\"btn sec\">Restaurar datos de ejemplo</button></form>");
        return sb.toString();
    }

    static void calcular(HttpExchange x, Auth.Sesion s, String ruta, String metodo, Map<String, String> q, String ok, String err) throws IOException, SQLException {
        if (metodo.equals("POST")) {
            Map<String, String> f = cuerpo(x);
            String obj = f.getOrDefault("obj", "");
            String destino = "/calcular?obj=" + enc(obj) + "&";
            try {
                switch (ruta) {
                    case "/calcular/linea" -> { Calc.guardar(f); ir(x, destino + sufijo("Línea guardada.", null)); }
                    case "/calcular/linea/eliminar" -> { Calc.eliminar(f); ir(x, destino + sufijo("Línea eliminada.", null)); }
                    case "/calcular/copiar" -> { Calc.copiar(f); ir(x, destino + sufijo("Líneas copiadas al contrato.", null)); }
                    default -> enviar(x, 404, "text/plain", "No encontrado");
                }
            } catch (IllegalArgumentException e) { ir(x, destino + sufijo(null, e.getMessage())); }
            catch (SQLException e) { ir(x, destino + sufijo(null, Db.mensaje(e))); }
            return;
        }
        enviar(x, 200, "text/html", Html.pagina("Calcular valores", s, "/calcular", ok, err, Calc.pagina(q.get("obj"), q.get("editar"))));
    }

    static void modulo(HttpExchange x, Auth.Sesion s, String ruta, String metodo, Map<String, String> q, String ok, String err) throws IOException, SQLException {
        String[] p = ruta.substring(3).split("/");
        Mod m = Mod.TODOS.get(p[0]);
        if (m == null) { enviar(x, 404, "text/html", Html.pagina("No encontrado", s, "", null, null, "<h1>Módulo no encontrado</h1>")); return; }
        String base = "/m/" + m.id;
        String activo = base;
        if (p.length == 1 && metodo.equals("GET")) { enviar(x, 200, "text/html", Html.pagina(m.titulo, s, activo, ok, err, m.lista(s, q.get("q")))); return; }
        if (p.length == 2 && p[1].equals("nuevo")) {
            if (metodo.equals("GET")) { enviar(x, 200, "text/html", Html.pagina(m.titulo, s, activo, null, err, m.formulario(s, null, base + "/nuevo"))); return; }
            try { m.insertar(s, cuerpo(x)); ir(x, base + "?" + sufijo("Registro creado.", null)); }
            catch (IllegalArgumentException e) { ir(x, base + "/nuevo?" + sufijo(null, e.getMessage())); }
            catch (SQLException e) { ir(x, base + "/nuevo?" + sufijo(null, Db.mensaje(e))); }
            return;
        }
        if (p.length == 3 && p[1].matches("\\d{1,12}")) {
            long id = Long.parseLong(p[1]);
            if (p[2].equals("editar")) {
                if (metodo.equals("GET")) {
                    Map<String, Object> f = m.buscar(id);
                    if (f == null) { ir(x, base + "?" + sufijo(null, "El registro no existe.")); return; }
                    enviar(x, 200, "text/html", Html.pagina(m.titulo, s, activo, null, err, m.formulario(s, f, base + "/" + id + "/editar"))); return;
                }
                try { m.actualizar(s, id, cuerpo(x)); ir(x, base + "?" + sufijo("Registro actualizado.", null)); }
                catch (IllegalArgumentException e) { ir(x, base + "/" + id + "/editar?" + sufijo(null, e.getMessage())); }
                catch (SQLException e) { ir(x, base + "/" + id + "/editar?" + sufijo(null, Db.mensaje(e))); }
                return;
            }
            if (p[2].equals("codigos") && m.id.equals("contratos")) {
                if (metodo.equals("GET")) {
                    String h = Extras.codigosContrato(id);
                    if (h == null) { ir(x, base + "?" + sufijo(null, "El contrato no existe.")); return; }
                    enviar(x, 200, "text/html", Html.pagina(m.titulo, s, activo, null, err, h)); return;
                }
                try { Extras.guardarCodigos(id, cuerpo(x)); ir(x, base + "?" + sufijo("Códigos del contrato actualizados.", null)); }
                catch (SQLException e) { ir(x, base + "/" + id + "/codigos?" + sufijo(null, Db.mensaje(e))); }
                return;
            }
            if (p[2].equals("eliminar") && metodo.equals("POST")) {
                if (!s.esSuper()) { ir(x, base + "?" + sufijo(null, "Solo el superusuario puede eliminar.")); return; }
                try { m.eliminar(id); ir(x, base + "?" + sufijo("Registro eliminado.", null)); }
                catch (SQLException e) { ir(x, base + "?" + sufijo(null, Db.mensaje(e))); }
                return;
            }
        }
        enviar(x, 404, "text/html", Html.pagina("No encontrado", s, "", null, null, "<h1>Página no encontrada</h1>"));
    }

    static void excel(HttpExchange x, Map<String, String> q) throws IOException, SQLException {
        java.time.LocalDate[] p;
        try { p = Extras.periodo(q); } catch (RuntimeException e) { ir(x, "/reporte?" + sufijo(null, "Fechas no válidas.")); return; }
        java.io.ByteArrayOutputStream bo = new java.io.ByteArrayOutputStream();
        Reporte.exportar(bo, p[0], p[1]);
        byte[] b = bo.toByteArray();
        x.getResponseHeaders().set("Content-Type", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        x.getResponseHeaders().set("Content-Disposition", "attachment; filename=\"reporte_contratos_" + p[0] + "_a_" + p[1] + ".xlsx\"");
        x.getResponseHeaders().set("Cache-Control", "no-store");
        x.sendResponseHeaders(200, b.length);
        try (OutputStream o = x.getResponseBody()) { o.write(b); }
    }

    static void usuarios(HttpExchange x, Auth.Sesion s, String ruta, String metodo, String ok, String err) throws IOException, SQLException {
        if (!s.esSuper()) { ir(x, "/inicio?" + sufijo(null, "Solo el superusuario puede gestionar usuarios.")); return; }
        if (metodo.equals("POST")) {
            try { ir(x, "/usuarios?" + sufijo(Extras.accionUsuario(s, ruta, cuerpo(x)), null)); }
            catch (IllegalArgumentException e) { ir(x, "/usuarios?" + sufijo(null, e.getMessage())); }
            return;
        }
        if (!ruta.equals("/usuarios")) { ir(x, "/usuarios"); return; }
        enviar(x, 200, "text/html", Html.pagina("Usuarios", s, "/usuarios", ok, err, Extras.usuarios(s)));
    }
}
