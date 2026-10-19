package com.sgeg.dao;

import com.sgeg.excepcion.NegocioException;
import com.sgeg.modelo.EstadoPedido;
import com.sgeg.modelo.Pedido;
import java.util.List;

public interface IPedidoDAO {

    /**
     * Registra el pedido como confirmado en UNA sola transacción: ocupa la mesa, guarda el
     * pedido y su detalle y descuenta el stock de insumos (RF23). Si algo falla no queda
     * nada a medias. Deja asignado el id del pedido.
     *
     * @throws NegocioException si la mesa ya no está libre o el stock no alcanza.
     */
    void registrarConfirmado(Pedido pedido);

    /** Pedidos en alguno de esos estados con su detalle, del más antiguo al más nuevo (RF18). */
    List<Pedido> listarPorEstados(EstadoPedido... estados);

    /**
     * Cambia el estado solo si el pedido sigue en {@code estadoActual}.
     *
     * @throws NegocioException si otro usuario ya lo cambió.
     */
    void cambiarEstado(int idPedido, EstadoPedido estadoActual, EstadoPedido nuevoEstado);
}
