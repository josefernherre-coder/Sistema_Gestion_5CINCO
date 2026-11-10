package web;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

/** Utilidades de HTML: escape, formato y plantilla de página. */
public class Html {
    static String e(Object o) {
        if (o == null) return "";
        String s = o.toString();
        StringBuilder sb = new StringBuilder(s.length() + 16);
        for (char c : s.toCharArray()) {
            switch (c) {
                case '&' -> sb.append("&amp;");
                case '<' -> sb.append("&lt;");
                case '>' -> sb.append("&gt;");
                case '"' -> sb.append("&quot;");
                case '\'' -> sb.append("&#39;");
                default -> sb.append(c);
            }
        }
        return sb.toString();
    }

    private static final DecimalFormatSymbols SIM = new DecimalFormatSymbols(new Locale("es", "CO"));
    static { SIM.setGroupingSeparator('.'); SIM.setDecimalSeparator(','); }

    static String dinero(Object o) {
        if (o == null) return "";
        double v = ((Number) o).doubleValue();
        return "$ " + new DecimalFormat("#,##0", SIM).format(Math.round(v));
    }

    static String numero(Object o) {
        if (o == null) return "";
        if (!(o instanceof Number)) return e(o);
        return new DecimalFormat("#,##0.##", SIM).format(((Number) o).doubleValue());
    }

    static java.util.List<String[]> menu(Auth.Sesion s) {
        java.util.List<String[]> l = new java.util.ArrayList<>();
        l.add(new String[]{"/inicio", "Inicio"});
        for (Mod m : Mod.TODOS.values()) l.add(new String[]{"/m/" + m.id, m.titulo.replace(" (propuestas)", "")});
        l.add(new String[]{"/calcular", "Calcular valores"});
        l.add(new String[]{"/reporte", "Reportes"});
        if (s != null && s.esSuper()) l.add(new String[]{"/usuarios", "Usuarios"});
        return l;
    }

    static String pagina(String titulo, Auth.Sesion s, String activo, String msgOk, String msgErr, String cuerpo) {
        StringBuilder sb = new StringBuilder();
        sb.append("<!doctype html><html lang=\"es\"><head><meta charset=\"utf-8\">")
          .append("<meta name=\"viewport\" content=\"width=device-width,initial-scale=1\">")
          .append("<title>").append(e(titulo)).append(" · 5CINCO demo</title>")
          .append("<link rel=\"stylesheet\" href=\"/estilo.css\"></head><body>");
        if (s != null) {
            sb.append("<input type=\"checkbox\" id=\"mn\" class=\"mnchk\">")
              .append("<header class=\"top\"><div class=\"izq\"><label for=\"mn\" class=\"burger\" aria-label=\"Abrir menú\" title=\"Menú\"><span></span><span></span><span></span></label>")
              .append("<div class=\"marca\"><b>5CINCO</b> <span>demo</span></div></div>")
              .append("<div class=\"usr\">").append(e(s.usuario())).append(" · ").append(e(s.rol()))
              .append("<form method=\"post\" action=\"/salir\"><button class=\"link\">Salir</button></form></div></header>")
              .append("<label for=\"mn\" class=\"velo\"></label><nav class=\"menu\"><div class=\"mtit\">Menú<label for=\"mn\" class=\"cerrar\" aria-label=\"Cerrar\">&times;</label></div>");
            for (String[] m : menu(s))
                sb.append("<a href=\"").append(m[0]).append("\"").append(m[0].equals(activo) ? " class=\"act\"" : "").append(">").append(m[1]).append("</a>");
            sb.append("</nav>");
        }
        sb.append("<main>");
        if (s != null) sb.append("<p class=\"aviso\">Demo con datos ficticios. Lo que cambies se restaura al reiniciar la demo.</p>");
        if (msgOk != null && !msgOk.isBlank()) sb.append("<div class=\"ok\">").append(e(msgOk)).append("</div>");
        if (msgErr != null && !msgErr.isBlank()) sb.append("<div class=\"err\">").append(e(msgErr)).append("</div>");
        sb.append(cuerpo).append("</main></body></html>");
        return sb.toString();
    }

    static final String CSS = """
:root{--v:#8fb81e;--vo:#6f9114;--n:#252a24;--g:#f4f5f1;--b:#dfe2da;--t:#1d211c;--m:#6a7066}
*{box-sizing:border-box}body{margin:0;font-family:system-ui,-apple-system,Segoe UI,Roboto,sans-serif;background:var(--g);color:var(--t);font-size:16px}
.top{background:var(--n);color:#fff;display:flex;justify-content:space-between;align-items:center;padding:10px 16px;gap:12px;flex-wrap:wrap}
.marca b{font-size:1.4rem;color:var(--v)}.marca span{color:#c9cdc4;font-size:.85rem}
.usr{display:flex;align-items:center;gap:10px;font-size:.85rem;color:#c9cdc4}.usr form{margin:0}
.link{background:none;border:0;color:var(--v);font:inherit;cursor:pointer;text-decoration:underline;padding:0}
.izq{display:flex;align-items:center;gap:12px}
.mnchk{position:absolute;opacity:0;pointer-events:none}
.burger{display:flex;flex-direction:column;gap:5px;padding:8px;cursor:pointer;border-radius:8px}
.burger:hover{background:rgba(255,255,255,.12)}.burger span{display:block;width:24px;height:3px;background:#fff;border-radius:2px}
.velo{position:fixed;inset:0;background:rgba(0,0,0,.45);opacity:0;visibility:hidden;transition:opacity .2s;z-index:40}
.menu{position:fixed;top:0;left:0;bottom:0;width:260px;max-width:80vw;background:#fff;z-index:50;display:flex;flex-direction:column;overflow-y:auto;
  transform:translateX(-100%);transition:transform .25s;box-shadow:2px 0 12px rgba(0,0,0,.25)}
.mnchk:checked~.menu{transform:none}.mnchk:checked~.velo{opacity:1;visibility:visible}
.mtit{background:var(--n);color:var(--v);font-weight:700;font-size:1.2rem;padding:14px 16px;display:flex;justify-content:space-between;align-items:center}
.cerrar{color:#fff;font-size:1.8rem;line-height:1;cursor:pointer;padding:0 6px}
.menu a{padding:13px 18px;color:var(--t);text-decoration:none;border-left:4px solid transparent;border-bottom:1px solid var(--g)}
.menu a:hover{background:var(--g)}
.menu a.act{border-left-color:var(--v);font-weight:600;background:#f1f6e2}
main{max-width:1100px;margin:0 auto;padding:16px}
h1{font-size:1.4rem;margin:.2em 0 .6em}h2{font-size:1.1rem;margin:1.2em 0 .5em}
.aviso{background:#fff8dc;border:1px solid #ecdc8a;border-radius:8px;padding:8px 12px;font-size:.85rem;margin:0 0 12px}
.ok{background:#e8f5d0;border:1px solid #bcd97a;border-radius:8px;padding:10px 12px;margin-bottom:12px}
.err{background:#fde8e6;border:1px solid #f0aaa4;border-radius:8px;padding:10px 12px;margin-bottom:12px}
.barra{display:flex;gap:8px;flex-wrap:wrap;margin-bottom:12px}.barra form{display:flex;gap:8px;flex:1;min-width:200px;margin:0}
input,select,textarea{font:inherit;padding:10px;border:1px solid var(--b);border-radius:8px;background:#fff;width:100%}
.barra input{flex:1}
.btn{display:inline-block;background:var(--v);color:#fff;border:0;border-radius:8px;padding:10px 16px;font:inherit;font-weight:600;cursor:pointer;text-decoration:none;text-align:center}
.btn.sec{background:#e9ebe4;color:var(--t)}.btn.pel{background:#c0392b}.btn:hover{filter:brightness(.95)}
.tabla{overflow-x:auto;background:#fff;border:1px solid var(--b);border-radius:10px}
table{border-collapse:collapse;width:100%;font-size:.92rem}th{background:var(--n);color:#fff;text-align:left;padding:9px 10px;white-space:nowrap}
td{padding:8px 10px;border-top:1px solid var(--b);vertical-align:top}td.d{text-align:right;white-space:nowrap}tr:hover td{background:#f8faf2}
.acc{white-space:nowrap}.acc form{display:inline;margin:0}.acc .btn{padding:6px 10px;font-size:.85rem}
.form{background:#fff;border:1px solid var(--b);border-radius:10px;padding:16px;display:grid;grid-template-columns:repeat(auto-fit,minmax(240px,1fr));gap:12px}
.form label{display:flex;flex-direction:column;gap:4px;font-size:.85rem;color:var(--m)}.form .ancho{grid-column:1/-1}
.form .req{color:#c0392b}.acciones{grid-column:1/-1;display:flex;gap:8px;flex-wrap:wrap}
.tarjetas{display:grid;grid-template-columns:repeat(auto-fit,minmax(150px,1fr));gap:12px;margin:12px 0}
.tarjeta{background:#fff;border:1px solid var(--b);border-top:4px solid var(--v);border-radius:10px;padding:14px}
.tarjeta b{display:block;font-size:1.6rem}.tarjeta span{color:var(--m);font-size:.85rem}
.tot{display:grid;grid-template-columns:repeat(auto-fit,minmax(150px,1fr));gap:10px;margin:14px 0}
.tot .tarjeta b{font-size:1.15rem}.pie{color:var(--m);font-size:.85rem;margin-top:8px}
.login{max-width:380px;margin:8vh auto;background:#fff;border:1px solid var(--b);border-radius:12px;padding:24px}
.login h1{text-align:center;color:var(--vo)}.login .form{border:0;padding:0;grid-template-columns:1fr}
.cred{background:var(--g);border-radius:8px;padding:10px;font-size:.85rem;margin-top:12px}
@media(max-width:600px){main{padding:12px}table{font-size:.85rem}}
""";
}
