package com.sgeg.modelo;

/** Persona del personal que opera el sistema. Tabla: usuario. */
public class Usuario {

    private int idUsuario;
    private String nombre;
    private String nombreUsuario;
    private String contraseniaHash;
    private Rol rol;
    private boolean activo = true;

    public Usuario() {
    }

    public Usuario(int idUsuario, String nombre, String nombreUsuario, String contraseniaHash, Rol rol, boolean activo) {
        this.idUsuario = idUsuario;
        this.nombre = nombre;
        this.nombreUsuario = nombreUsuario;
        this.contraseniaHash = contraseniaHash;
        this.rol = rol;
        this.activo = activo;
    }

    public int getIdUsuario() {
        return idUsuario;
    }

    public void setIdUsuario(int idUsuario) {
        this.idUsuario = idUsuario;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getNombreUsuario() {
        return nombreUsuario;
    }

    public void setNombreUsuario(String nombreUsuario) {
        this.nombreUsuario = nombreUsuario;
    }

    public String getContraseniaHash() {
        return contraseniaHash;
    }

    public void setContraseniaHash(String contraseniaHash) {
        this.contraseniaHash = contraseniaHash;
    }

    public Rol getRol() {
        return rol;
    }

    public void setRol(Rol rol) {
        this.rol = rol;
    }

    public boolean isActivo() {
        return activo;
    }

    public void setActivo(boolean activo) {
        this.activo = activo;
    }

    /** @return true si el usuario tiene exactamente ese rol (ver constantes de {@link Rol}). */
    public boolean tieneRol(String descripcionRol) {
        return rol != null && rol.getDescripcion() != null && rol.getDescripcion().equalsIgnoreCase(descripcionRol);
    }

    /** No incluye el hash de la contraseña para que nunca termine en un log. */
    @Override
    public String toString() {
        return nombre + " (" + nombreUsuario + ", " + rol + ")";
    }
}
