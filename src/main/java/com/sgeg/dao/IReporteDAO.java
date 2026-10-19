package com.sgeg.dao;

import com.sgeg.modelo.ProductoVendido;
import com.sgeg.modelo.ResumenVenta;
import com.sgeg.modelo.TotalFormaPago;
import java.time.LocalDate;
import java.util.List;

/** Consultas de solo lectura sobre las ventas ya cobradas (cierre de caja y reportes). Los límites son días completos. */
public interface IReporteDAO {

    /** Ventas entre {@code desde} y {@code hasta} (ambos días incluidos), de la más antigua a la más nueva. */
    List<ResumenVenta> ventasDelPeriodo(LocalDate desde, LocalDate hasta);

    /** Lo cobrado por cada forma de pago en el período (RF28). */
    List<TotalFormaPago> totalesPorFormaDePago(LocalDate desde, LocalDate hasta);

    /** Productos con más unidades vendidas en el período (RF31). */
    List<ProductoVendido> productosMasVendidos(LocalDate desde, LocalDate hasta, int limite);
}
