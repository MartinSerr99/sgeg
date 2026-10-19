package com.sgeg.servicio;

import com.sgeg.dao.IClienteDAO;
import com.sgeg.dao.IMesaDAO;
import com.sgeg.dao.IPedidoDAO;
import com.sgeg.dao.IProductoDAO;
import com.sgeg.excepcion.NegocioException;
import com.sgeg.excepcion.StockInsuficienteException;
import com.sgeg.modelo.Cliente;
import com.sgeg.modelo.DetallePedido;
import com.sgeg.modelo.EstadoPedido;
import com.sgeg.modelo.IngredienteReceta;
import com.sgeg.modelo.Mesa;
import com.sgeg.modelo.Pedido;
import com.sgeg.modelo.Producto;
import com.sgeg.modelo.Rol;
import com.sgeg.modelo.Usuario;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Reglas de negocio del circuito de pedidos: toma del pedido (CU06) y cambio de estado (CU08).
 * El stock de insumos se descuenta AL CONFIRMAR el pedido (RF23), no al prepararlo.
 */
public class PedidoService {

    private static final int CANTIDAD_MAXIMA_POR_PRODUCTO = 99;

    /** Qué rol puede sacar un pedido de cada estado. ENTREGADO -> FACTURADO es del cajero (CU13, aún no implementado). */
    private static final Map<EstadoPedido, String> ROL_QUE_AVANZA = Map.of(
            EstadoPedido.PENDIENTE, Rol.COCINERO,
            EstadoPedido.EN_PREPARACION, Rol.COCINERO,
            EstadoPedido.LISTO, Rol.MOZO);

    private final IPedidoDAO pedidoDAO;
    private final IProductoDAO productoDAO;
    private final IMesaDAO mesaDAO;
    private final IClienteDAO clienteDAO;

    public PedidoService(IPedidoDAO pedidoDAO, IProductoDAO productoDAO, IMesaDAO mesaDAO, IClienteDAO clienteDAO) {
        this.pedidoDAO = pedidoDAO;
        this.productoDAO = productoDAO;
        this.mesaDAO = mesaDAO;
        this.clienteDAO = clienteDAO;
    }

    // ------------------------------------------------------------------ consultas para las pantallas

    public List<Mesa> listarMesas() {
        return mesaDAO.listarTodas();
    }

    public List<Producto> listarMenu() {
        return productoDAO.listarMenu();
    }

    public List<Cliente> listarClientes() {
        return clienteDAO.listarTodos();
    }

    /** Pedidos que todavía no se entregaron, del más antiguo al más nuevo (RF18). */
    public List<Pedido> listarPedidosActivos() {
        return pedidoDAO.listarPorEstados(EstadoPedido.PENDIENTE, EstadoPedido.EN_PREPARACION, EstadoPedido.LISTO);
    }

    // ------------------------------------------------------------------ CU06: registrar pedido

    /** @param mesa mesa elegida, o null si el pedido es para llevar (RF14). */
    public Pedido iniciarPedido(Mesa mesa) {
        if (mesa != null && !mesa.estaLibre()) {
            throw new NegocioException("La mesa " + mesa.getNumero() + " no está libre.");
        }
        return new Pedido(mesa);
    }

    /** RF15 y RF19: agrega el producto solo si el stock de insumos alcanza para todo lo pedido hasta ahora. */
    public void agregarProducto(Pedido pedido, Producto producto, int cantidad) {
        if (producto == null) {
            throw new NegocioException("Elegí un producto del menú.");
        }
        if (!producto.isDisponible()) {
            throw new NegocioException("«" + producto.getNombre() + "» no está disponible en el menú.");
        }
        if (cantidad < 1 || pedido.cantidadDe(producto.getIdProducto()) + cantidad > CANTIDAD_MAXIMA_POR_PRODUCTO) {
            throw new NegocioException("La cantidad de un mismo producto debe estar entre 1 y "
                    + CANTIDAD_MAXIMA_POR_PRODUCTO + ".");
        }

        Map<Integer, Integer> cantidades = cantidadesPorProducto(pedido);
        cantidades.merge(producto.getIdProducto(), cantidad, Integer::sum);
        verificarStock(cantidades);

        pedido.agregarProducto(producto, cantidad);
    }

    /**
     * Confirma el pedido: queda "Pendiente", visible para la cocina, con la mesa ocupada y
     * el stock descontado. La verificación final y el descuento ocurren dentro de la
     * transacción del DAO, así que dos mozos no pueden vender el mismo stock.
     */
    public void confirmarPedido(Pedido pedido, Usuario usuario) {
        if (usuario == null || !usuario.tieneRol(Rol.MOZO)) {
            throw new NegocioException("Solo un mozo puede registrar pedidos.");
        }
        if (pedido.estaVacio()) {
            throw new NegocioException("El pedido no tiene productos. Agregá al menos uno.");
        }
        if (pedido.getMesa() != null && !pedido.getMesa().estaLibre()) {
            throw new NegocioException("La mesa " + pedido.getMesa().getNumero() + " no está libre.");
        }

        verificarStock(cantidadesPorProducto(pedido));

        pedido.setUsuario(usuario);
        pedido.setEstado(EstadoPedido.PENDIENTE);
        pedidoDAO.registrarConfirmado(pedido);
    }

    // ------------------------------------------------------------------ CU08: cambiar estado

    /** Indica si ese usuario puede pasar al siguiente estado un pedido que está en {@code estado}. */
    public boolean puedeAvanzar(Usuario usuario, EstadoPedido estado) {
        String rolRequerido = ROL_QUE_AVANZA.get(estado);
        return rolRequerido != null && usuario != null && usuario.tieneRol(rolRequerido);
    }

    /** RF17: pasa el pedido al estado siguiente (Pendiente > En preparación > Listo > Entregado). */
    public EstadoPedido avanzarEstado(Usuario usuario, Pedido pedido) {
        EstadoPedido actual = pedido.getEstado();
        if (!puedeAvanzar(usuario, actual)) {
            throw new NegocioException("Tu rol no puede cambiar el estado de un pedido «" + actual + "».");
        }
        EstadoPedido nuevo = actual.siguiente();
        pedidoDAO.cambiarEstado(pedido.getIdPedido(), actual, nuevo);
        pedido.setEstado(nuevo);
        return nuevo;
    }

    // ------------------------------------------------------------------ auxiliares

    private Map<Integer, Integer> cantidadesPorProducto(Pedido pedido) {
        Map<Integer, Integer> cantidades = new HashMap<>();
        for (DetallePedido detalle : pedido.getDetalles()) {
            cantidades.merge(detalle.getProducto().getIdProducto(), detalle.getCantidad(), Integer::sum);
        }
        return cantidades;
    }

    /** Suma lo que piden todos los productos de cada insumo y lo compara con el stock actual. */
    private void verificarStock(Map<Integer, Integer> cantidadesPorProducto) {
        Map<Integer, BigDecimal> requerido = new HashMap<>();
        Map<Integer, IngredienteReceta> datosInsumo = new HashMap<>();

        for (Map.Entry<Integer, Integer> linea : cantidadesPorProducto.entrySet()) {
            for (IngredienteReceta ingrediente : productoDAO.obtenerReceta(linea.getKey())) {
                BigDecimal necesario = ingrediente.getCantidadNecesaria().multiply(BigDecimal.valueOf(linea.getValue()));
                requerido.merge(ingrediente.getIdInsumo(), necesario, BigDecimal::add);
                datosInsumo.putIfAbsent(ingrediente.getIdInsumo(), ingrediente);
            }
        }

        for (Map.Entry<Integer, BigDecimal> insumo : requerido.entrySet()) {
            IngredienteReceta datos = datosInsumo.get(insumo.getKey());
            if (insumo.getValue().compareTo(datos.getStockActual()) > 0) {
                throw new StockInsuficienteException(datos.getNombreInsumo(), datos.getUnidadMedida(),
                        insumo.getValue(), datos.getStockActual());
            }
        }
    }
}
