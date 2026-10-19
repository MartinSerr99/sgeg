package com.sgeg.modelo;

import java.math.BigDecimal;

/** Una fila de los listados de ventas (cierre de caja y reporte de ventas). */
public record ResumenVenta(int idVenta, String fechaHora, String destino, String mozo, BigDecimal total) {
}
