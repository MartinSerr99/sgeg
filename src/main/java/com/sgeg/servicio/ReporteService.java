package com.sgeg.servicio;

import com.sgeg.dao.IPedidoDAO;
import com.sgeg.dao.IReporteDAO;
import com.sgeg.excepcion.NegocioException;
import com.sgeg.modelo.EstadoPedido;
import com.sgeg.modelo.ProductoVendido;
import com.sgeg.modelo.ResumenPeriodo;
import com.sgeg.modelo.ResumenVenta;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/** Cierre de caja (CU15) y reportes de ventas y de productos más vendidos (CU16, CU17). */
public class ReporteService {

    private static final int LIMITE_MAXIMO = 100;

    private final IReporteDAO reporteDAO;
    private final IPedidoDAO pedidoDAO;

    public ReporteService(IReporteDAO reporteDAO, IPedidoDAO pedidoDAO) {
        this.reporteDAO = reporteDAO;
        this.pedidoDAO = pedidoDAO;
    }

    /** CU15 / RF28: ventas del día y lo cobrado por cada forma de pago. */
    public ResumenPeriodo cierreDeCaja(LocalDate dia) {
        if (dia == null) {
            throw new NegocioException("Elegí el día del cierre.");
        }
        return ventasDelPeriodo(dia, dia);
    }

    /** CU16 / RF29-RF30: ventas del período con el total por forma de pago. */
    public ResumenPeriodo ventasDelPeriodo(LocalDate desde, LocalDate hasta) {
        validarPeriodo(desde, hasta);
        List<ResumenVenta> ventas = reporteDAO.ventasDelPeriodo(desde, hasta);
        BigDecimal total = BigDecimal.ZERO;
        for (ResumenVenta venta : ventas) {
            total = total.add(venta.total());
        }
        return new ResumenPeriodo(ventas, reporteDAO.totalesPorFormaDePago(desde, hasta), total);
    }

    /** CU17 / RF31: los productos más vendidos del período. */
    public List<ProductoVendido> productosMasVendidos(LocalDate desde, LocalDate hasta, int limite) {
        validarPeriodo(desde, hasta);
        if (limite < 1 || limite > LIMITE_MAXIMO) {
            throw new NegocioException("La cantidad de productos a mostrar debe estar entre 1 y " + LIMITE_MAXIMO + ".");
        }
        return reporteDAO.productosMasVendidos(desde, hasta, limite);
    }

    /** Pedidos entregados que todavía no se cobraron: el cierre no estaría completo mientras haya alguno. */
    public int contarPedidosSinCobrar() {
        return pedidoDAO.listarPorEstados(EstadoPedido.ENTREGADO).size();
    }

    private static void validarPeriodo(LocalDate desde, LocalDate hasta) {
        if (desde == null || hasta == null) {
            throw new NegocioException("Elegí la fecha desde y la fecha hasta.");
        }
        if (desde.isAfter(hasta)) {
            throw new NegocioException("La fecha «desde» no puede ser posterior a la fecha «hasta».");
        }
    }
}
