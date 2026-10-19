package com.sgeg.modelo;

import java.math.BigDecimal;

/** Una fila del reporte de productos más vendidos (RF31). */
public record ProductoVendido(String producto, long unidades, BigDecimal monto) {
}
