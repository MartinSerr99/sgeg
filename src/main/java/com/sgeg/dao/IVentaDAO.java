package com.sgeg.dao;

import com.sgeg.excepcion.NegocioException;
import com.sgeg.modelo.FormaPago;
import com.sgeg.modelo.Pago;
import com.sgeg.modelo.Venta;
import java.util.List;

public interface IVentaDAO {

    /** Formas de pago disponibles (RF26). */
    List<FormaPago> listarFormasDePago();

    /**
     * CU13/CU14: cobra un pedido "Entregado" en UNA transacción: registra la venta y sus pagos,
     * pasa el pedido a "Facturado" y libera la mesa. Devuelve el id de la venta.
     *
     * @throws NegocioException si el pedido ya no está entregado (otro cajero lo cobró) o los pagos no suman el total.
     */
    int registrarCobro(int idPedido, List<Pago> pagos);

    /** La venta con el detalle del pedido y sus pagos (para el comprobante), o null si no existe. */
    Venta buscarVenta(int idVenta);
}
