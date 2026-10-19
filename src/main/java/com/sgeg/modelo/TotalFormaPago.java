package com.sgeg.modelo;

import java.math.BigDecimal;

/** Lo cobrado con una forma de pago en un período (cierre de caja, RF28). */
public record TotalFormaPago(String formaPago, int cantidadPagos, BigDecimal total) {
}
