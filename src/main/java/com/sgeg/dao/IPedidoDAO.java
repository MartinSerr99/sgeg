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

    /** El pedido con su detalle, o null si no existe (se usa para abrirlo y modificarlo, CU07). */
    Pedido buscarPorId(int idPedido);

    /**
     * CU07: reemplaza el detalle de un pedido que sigue "Pendiente" y ajusta el stock por la
     * diferencia (descuenta lo que se agregó, devuelve lo que se quitó), todo en una transacción.
     *
     * @throws NegocioException si la cocina ya lo tomó, ya no existe o el stock no alcanza.
     */
    void modificar(Pedido pedido);

    /**
     * CU07: cancela un pedido que sigue "Pendiente": devuelve todo el stock, libera la mesa y
     * borra el pedido, en una transacción.
     *
     * @throws NegocioException si la cocina ya lo tomó o ya no existe.
     */
    void cancelar(int idPedido);
}
