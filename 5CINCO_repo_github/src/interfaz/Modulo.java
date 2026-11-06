package interfaz;

import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/** Definición de un módulo CRUD: tabla, listado y campos del formulario. */
public final class Modulo {

    public final String titulo;
    public final String tabla;
    public final String pk;
    public final String sqlListado;          // la primera columna debe ser la llave primaria
    public String sqlListadoBasico;          // listado sin datos sensibles (para no superusuarios)
    public final List<Campo> campos;
    public boolean puedeEliminar = true;
    public String mensajeCheck;              // mensaje cuando falla una regla CHECK
    public Consumer<Map<String, Object>> antesDeGuardar;
    public String nombreSingular;

    public Modulo(String titulo, String nombreSingular, String tabla, String pk, String sqlListado, List<Campo> campos) {
        this.titulo = titulo;
        this.nombreSingular = nombreSingular;
        this.tabla = tabla;
        this.pk = pk;
        this.sqlListado = sqlListado;
        this.campos = campos;
    }
}
