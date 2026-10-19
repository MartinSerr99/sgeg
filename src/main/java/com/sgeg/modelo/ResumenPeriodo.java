package com.sgeg.modelo;

import java.math.BigDecimal;
import java.util.List;

/** Ventas de un período con lo cobrado por forma de pago: sirve para el cierre de caja (un día) y el reporte de ventas. */
public record ResumenPeriodo(List<ResumenVenta> ventas, List<TotalFormaPago> totalesPorForma, BigDecimal totalVendido) {

    public int cantidadVentas() {
        return ventas.size();
    }
}
