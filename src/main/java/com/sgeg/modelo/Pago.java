package com.sgeg.modelo;

import java.math.BigDecimal;

/** Parte de una venta cobrada con una forma de pago. Una venta puede pagarse con varias. Tabla: pago. */
public class Pago {

    private final FormaPago formaPago;
    private final BigDecimal monto;

    public Pago(FormaPago formaPago, BigDecimal monto) {
        this.formaPago = formaPago;
        this.monto = monto;
    }

    public FormaPago getFormaPago() {
        return formaPago;
    }

    public BigDecimal getMonto() {
        return monto;
    }
}
