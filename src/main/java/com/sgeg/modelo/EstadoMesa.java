package com.sgeg.modelo;

/** Estados de una mesa. Coinciden con la tabla estado_mesa. */
public enum EstadoMesa {
    LIBRE("Libre"),
    OCUPADA("Ocupada"),
    RESERVADA("Reservada");

    private final String descripcion;

    EstadoMesa(String descripcion) {
        this.descripcion = descripcion;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public static EstadoMesa desdeDescripcion(String descripcion) {
        for (EstadoMesa estado : values()) {
            if (estado.descripcion.equalsIgnoreCase(descripcion)) {
                return estado;
            }
        }
        throw new IllegalArgumentException("Estado de mesa desconocido: " + descripcion);
    }

    @Override
    public String toString() {
        return descripcion;
    }
}
