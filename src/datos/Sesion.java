package datos;

/** Datos del usuario que inició sesión. */
public final class Sesion {

    public final String usuario;
    public final String rol;

    public Sesion(String usuario, String rol) {
        this.usuario = usuario;
        this.rol = rol;
    }

    /** El superusuario ve datos sensibles (salarios, seguridad social) y puede eliminar. */
    public boolean esSuperusuario() {
        return "superusuario".equals(rol);
    }

    public String rolVisible() {
        return esSuperusuario() ? "Superusuario" : "Administrador";
    }
}
