package com.sgeg.modelo;

/** Estados del pedido, en el orden del circuito. Coinciden con la tabla estado_pedido. */
public enum EstadoPedido {
    PENDIENTE("Pendiente"),
    EN_PREPARACION("En preparación"),
    LISTO("Listo"),
    ENTREGADO("Entregado"),
    FACTURADO("Facturado");

    private final String descripcion;

    EstadoPedido(String descripcion) {
        this.descripcion = descripcion;
    }

    public String getDescripcion() {
        return descripcion;
    }

    /** @return el estado que sigue en el circuito, o null si es el último. */
    public EstadoPedido siguiente() {
        int proximo = ordinal() + 1;
        return proximo < values().length ? values()[proximo] : null;
    }

    public static EstadoPedido desdeDescripcion(String descripcion) {
        for (EstadoPedido estado : values()) {
            if (estado.descripcion.equalsIgnoreCase(descripcion)) {
                return estado;
            }
        }
        throw new IllegalArgumentException("Estado de pedido desconocido: " + descripcion);
    }

    @Override
    public String toString() {
        return descripcion;
    }
}
