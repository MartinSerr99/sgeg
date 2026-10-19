package com.sgeg.servicio;

import com.sgeg.dao.IPedidoDAO;
import com.sgeg.dao.IVentaDAO;
import com.sgeg.excepcion.NegocioException;
import com.sgeg.modelo.EstadoPedido;
import com.sgeg.modelo.FormaPago;
import com.sgeg.modelo.Pago;
import com.sgeg.modelo.Pedido;
import com.sgeg.modelo.Rol;
import com.sgeg.modelo.Usuario;
import com.sgeg.modelo.Venta;
import java.math.BigDecimal;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Cobro de pedidos entregados (CU13 comprobante, CU14 forma de pago). */
public class VentaService {

    private final IVentaDAO ventaDAO;
    private final IPedidoDAO pedidoDAO;

    public VentaService(IVentaDAO ventaDAO, IPedidoDAO pedidoDAO) {
        this.ventaDAO = ventaDAO;
        this.pedidoDAO = pedidoDAO;
    }

    /** Pedidos que el mozo ya entregó y todavía no se cobraron, del más antiguo al más nuevo. */
    public List<Pedido> listarPedidosACobrar() {
        return pedidoDAO.listarPorEstados(EstadoPedido.ENTREGADO);
    }

    public List<FormaPago> listarFormasDePago() {
        return ventaDAO.listarFormasDePago();
    }

    /**
     * Cobra el pedido con una o más formas de pago que deben sumar exactamente el total (RF26, RF27).
     * Devuelve la venta ya registrada, lista para armar el comprobante.
     */
    public Venta cobrar(Usuario usuario, Pedido pedido, List<Pago> pagos) {
        if (usuario == null || !usuario.tieneRol(Rol.CAJERO)) {
            throw new NegocioException("Solo un cajero puede cobrar pedidos.");
        }
        if (pedido.getEstado() != EstadoPedido.ENTREGADO) {
            throw new NegocioException("El pedido #" + pedido.getIdPedido() + " no está entregado todavía.");
        }
        validarPagos(pagos, pedido.calcularTotal());

        int idVenta = ventaDAO.registrarCobro(pedido.getIdPedido(), pagos);
        Venta venta = ventaDAO.buscarVenta(idVenta);
        if (venta == null) {
            throw new IllegalStateException("La venta #" + idVenta + " no se encontró después de registrarla");
        }
        return venta;
    }

    /** Vuelve a leer una venta ya cobrada, por ejemplo para reimprimir el comprobante. */
    public Venta obtenerVenta(int idVenta) {
        Venta venta = ventaDAO.buscarVenta(idVenta);
        if (venta == null) {
            throw new NegocioException("La venta #" + idVenta + " no existe.");
        }
        return venta;
    }

    static void validarPagos(List<Pago> pagos, BigDecimal total) {
        if (pagos == null || pagos.isEmpty()) {
            throw new NegocioException("Indicá al menos una forma de pago.");
        }
        Set<Integer> usadas = new HashSet<>();
        BigDecimal suma = BigDecimal.ZERO;
        for (Pago pago : pagos) {
            if (pago.getFormaPago() == null) {
                throw new NegocioException("Elegí la forma de pago de cada importe.");
            }
            if (!usadas.add(pago.getFormaPago().getIdFormaPago())) {
                throw new NegocioException("«" + pago.getFormaPago().getDescripcion()
                        + "» está repetida. Sumá los importes en una sola línea.");
            }
            if (pago.getMonto() == null || pago.getMonto().signum() <= 0) {
                throw new NegocioException("Cada importe debe ser mayor a cero.");
            }
            if (pago.getMonto().scale() > 2 && pago.getMonto().stripTrailingZeros().scale() > 2) {
                throw new NegocioException("Los importes admiten hasta dos decimales.");
            }
            suma = suma.add(pago.getMonto());
        }
        if (suma.compareTo(total) != 0) {
            throw new NegocioException("Los pagos no coinciden con el total del pedido: faltan o sobran "
                    + total.subtract(suma).abs().toPlainString() + ".");
        }
    }
}
