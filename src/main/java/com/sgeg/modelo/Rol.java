package com.sgeg.modelo;

/** Rol de un usuario del sistema (Administrador, Mozo, Cocinero, Cajero). Tabla: rol. */
public class Rol {

    public static final String ADMINISTRADOR = "Administrador";
    public static final String MOZO = "Mozo";
    public static final String COCINERO = "Cocinero";
    public static final String CAJERO = "Cajero";

    private int idRol;
    private String descripcion;

    public Rol() {
    }

    public Rol(int idRol, String descripcion) {
        this.idRol = idRol;
        this.descripcion = descripcion;
    }

    public int getIdRol() {
        return idRol;
    }

    public void setIdRol(int idRol) {
        this.idRol = idRol;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }

    @Override
    public String toString() {
        return descripcion;
    }
}
