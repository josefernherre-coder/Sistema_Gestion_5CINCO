package interfaz;

/** Descripción de un campo de formulario ligado a una columna de la base. */
public final class Campo {

    public enum Tipo { TEXTO, AREA, ENTERO, DECIMAL, FECHA, LISTA, REFERENCIA }

    public final String columna;
    public final String etiqueta;
    public final Tipo tipo;
    public boolean obligatorio;
    public boolean sensible;          // solo lo ve el superusuario
    public boolean soloLectura;
    public String[] opciones;         // para LISTA
    public String sqlReferencia;      // para REFERENCIA: SELECT id, texto FROM ...
    public String valorInicial;       // valor sugerido al crear
    public String sqlValorInicial;    // consulta que calcula el valor sugerido al crear

    private Campo(String columna, String etiqueta, Tipo tipo) {
        this.columna = columna;
        this.etiqueta = etiqueta;
        this.tipo = tipo;
    }

    public static Campo texto(String col, String etq) { return new Campo(col, etq, Tipo.TEXTO); }
    public static Campo area(String col, String etq) { return new Campo(col, etq, Tipo.AREA); }
    public static Campo entero(String col, String etq) { return new Campo(col, etq, Tipo.ENTERO); }
    public static Campo decimal(String col, String etq) { return new Campo(col, etq, Tipo.DECIMAL); }
    public static Campo fecha(String col, String etq) { return new Campo(col, etq + " (AAAA-MM-DD)", Tipo.FECHA); }

    public static Campo lista(String col, String etq, String... opciones) {
        Campo c = new Campo(col, etq, Tipo.LISTA);
        c.opciones = opciones;
        return c;
    }

    public static Campo ref(String col, String etq, String sql) {
        Campo c = new Campo(col, etq, Tipo.REFERENCIA);
        c.sqlReferencia = sql;
        return c;
    }

    public Campo req() { this.obligatorio = true; return this; }
    public Campo sensible() { this.sensible = true; return this; }
    public Campo lectura() { this.soloLectura = true; return this; }
    public Campo inicial(String v) { this.valorInicial = v; return this; }
    public Campo inicialSql(String sql) { this.sqlValorInicial = sql; return this; }
}
