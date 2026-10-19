package com.sgeg.modelo;

/** Forma de pago aceptada (Efectivo, Tarjeta, Transferencia). Tabla: forma_pago. */
public class FormaPago {

    private final int idFormaPago;
    private final String descripcion;

    public FormaPago(int idFormaPago, String descripcion) {
        this.idFormaPago = idFormaPago;
        this.descripcion = descripcion;
    }

    public int getIdFormaPago() {
        return idFormaPago;
    }

    public String getDescripcion() {
        return descripcion;
    }

    @Override
    public String toString() {
        return descripcion;
    }
}
