package com.sgeg.dao;

import com.sgeg.excepcion.NegocioException;
import com.sgeg.excepcion.StockInsuficienteException;
import com.sgeg.modelo.DetallePedido;
import com.sgeg.modelo.EstadoMesa;
import com.sgeg.modelo.EstadoPedido;
import com.sgeg.modelo.Mesa;
import com.sgeg.modelo.Pedido;
import com.sgeg.modelo.Producto;
import com.sgeg.modelo.Usuario;
import com.sgeg.util.ConexionBD;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.logging.Level;
import java.util.logging.Logger;

public class PedidoDAOImpl implements IPedidoDAO {

    private static final Logger LOG = Logger.getLogger(PedidoDAOImpl.class.getName());

    // La fecha y hora la pone MySQL (DEFAULT CURRENT_TIMESTAMP): con varias PCs el reloj del servidor es el único común.
    private static final String SQL_INSERTAR_PEDIDO =
            "INSERT INTO pedido (id_mesa, id_cliente, id_usuario, id_estado_pedido, total) "
            + "VALUES (?, ?, ?, (SELECT id_estado_pedido FROM estado_pedido WHERE descripcion = ?), ?)";

    private static final String SQL_INSERTAR_DETALLE =
            "INSERT INTO detalle_pedido (id_pedido, id_producto, cantidad, precio_unitario, subtotal) "
            + "VALUES (?, ?, ?, ?, ?)";

    private static final String SQL_RECETA_DEL_PRODUCTO =
            "SELECT id_insumo, cantidad_necesaria FROM producto_insumo WHERE id_producto = ?";

    private static final String SQL_BLOQUEAR_INSUMO =
            "SELECT nombre, unidad_medida, stock_actual FROM insumo WHERE id_insumo = ? FOR UPDATE";

    private static final String SQL_DESCONTAR_INSUMO =
            "UPDATE insumo SET stock_actual = stock_actual - ? WHERE id_insumo = ?";

    // Solo ocupa la mesa si TODAVÍA está libre: si otro mozo se la ganó, no actualiza ninguna fila.
    private static final String SQL_OCUPAR_MESA =
            "UPDATE mesa SET id_estado_mesa = (SELECT id_estado_mesa FROM estado_mesa WHERE descripcion = ?) "
            + "WHERE id_mesa = ? "
            + "AND id_estado_mesa = (SELECT id_estado_mesa FROM estado_mesa WHERE descripcion = ?)";

    private static final String SQL_LISTAR_PEDIDOS =
            "SELECT p.id_pedido, p.id_mesa, m.numero AS numero_mesa, ep.descripcion AS estado, "
            + "       u.nombre AS mozo, DATE_FORMAT(p.fecha_hora, '%H:%i') AS hora, "
            + "       TIMESTAMPDIFF(MINUTE, p.fecha_hora, NOW()) AS minutos "
            + "FROM pedido p "
            + "JOIN estado_pedido ep ON ep.id_estado_pedido = p.id_estado_pedido "
            + "JOIN usuario u ON u.id_usuario = p.id_usuario "
            + "LEFT JOIN mesa m ON m.id_mesa = p.id_mesa ";

    private static final String SQL_LISTAR_DETALLES =
            "SELECT dp.id_detalle, dp.id_pedido, dp.id_producto, pr.nombre, dp.cantidad, dp.precio_unitario "
            + "FROM detalle_pedido dp "
            + "JOIN producto pr ON pr.id_producto = dp.id_producto ";

    private static final String SQL_CAMBIAR_ESTADO =
            "UPDATE pedido SET id_estado_pedido = (SELECT id_estado_pedido FROM estado_pedido WHERE descripcion = ?) "
            + "WHERE id_pedido = ? "
            + "AND id_estado_pedido = (SELECT id_estado_pedido FROM estado_pedido WHERE descripcion = ?)";

    // ------------------------------------------------------------------ registrar (CU06)

    @Override
    public void registrarConfirmado(Pedido pedido) {
        Connection conexion = null;
        try {
            // Conexión propia: el setAutoCommit(false) no debe afectar a la conexión compartida.
            conexion = ConexionBD.getInstancia().abrirConexionNueva();
            conexion.setAutoCommit(false);

            // Orden fijo (mesa -> pedido -> insumos por id ascendente) para que dos pedidos
            // simultáneos nunca se bloqueen entre sí.
            if (pedido.getMesa() != null) {
                ocuparMesa(conexion, pedido.getMesa());
            }
            insertarPedido(conexion, pedido);
            insertarDetalles(conexion, pedido);
            descontarStock(conexion, pedido);

            conexion.commit();
        } catch (NegocioException e) {
            revertir(conexion);
            throw e;
        } catch (SQLException e) {
            revertir(conexion);
            throw new DAOException("No se pudo registrar el pedido", e);
        } catch (RuntimeException e) {
            revertir(conexion);
            throw e;
        } finally {
            cerrar(conexion);
        }
    }

    private void ocuparMesa(Connection conexion, Mesa mesa) throws SQLException {
        try (PreparedStatement sentencia = conexion.prepareStatement(SQL_OCUPAR_MESA)) {
            sentencia.setString(1, EstadoMesa.OCUPADA.getDescripcion());
            sentencia.setInt(2, mesa.getIdMesa());
            sentencia.setString(3, EstadoMesa.LIBRE.getDescripcion());
            if (sentencia.executeUpdate() != 1) {
                throw new NegocioException("La mesa " + mesa.getNumero()
                        + " ya no está libre. Actualizá la pantalla de mesas y elegí otra.");
            }
        }
    }

    private void insertarPedido(Connection conexion, Pedido pedido) throws SQLException {
        try (PreparedStatement sentencia = conexion.prepareStatement(SQL_INSERTAR_PEDIDO, Statement.RETURN_GENERATED_KEYS)) {
            if (pedido.getMesa() == null) {
                sentencia.setNull(1, Types.INTEGER);
            } else {
                sentencia.setInt(1, pedido.getMesa().getIdMesa());
            }
            if (pedido.getCliente() == null) {
                sentencia.setNull(2, Types.INTEGER);
            } else {
                sentencia.setInt(2, pedido.getCliente().getIdCliente());
            }
            sentencia.setInt(3, pedido.getUsuario().getIdUsuario());
            sentencia.setString(4, EstadoPedido.PENDIENTE.getDescripcion());
            sentencia.setBigDecimal(5, pedido.calcularTotal());
            sentencia.executeUpdate();
            try (ResultSet claves = sentencia.getGeneratedKeys()) {
                if (!claves.next()) {
                    throw new SQLException("MySQL no devolvió el id del pedido");
                }
                pedido.setIdPedido(claves.getInt(1));
            }
            pedido.setEstado(EstadoPedido.PENDIENTE);
        }
    }

    private void insertarDetalles(Connection conexion, Pedido pedido) throws SQLException {
        try (PreparedStatement sentencia = conexion.prepareStatement(SQL_INSERTAR_DETALLE)) {
            for (DetallePedido detalle : pedido.getDetalles()) {
                sentencia.setInt(1, pedido.getIdPedido());
                sentencia.setInt(2, detalle.getProducto().getIdProducto());
                sentencia.setInt(3, detalle.getCantidad());
                sentencia.setBigDecimal(4, detalle.getPrecioUnitario());
                sentencia.setBigDecimal(5, detalle.getSubtotal());
                sentencia.addBatch();
            }
            sentencia.executeBatch();
        }
    }

    /**
     * RF23: descuenta de una sola vez los insumos de TODO el pedido. Primero junta cuánto
     * se necesita de cada insumo (varios productos pueden compartir uno), después bloquea
     * cada fila de insumo, verifica que alcance y recién entonces descuenta.
     */
    private void descontarStock(Connection conexion, Pedido pedido) throws SQLException {
        Map<Integer, BigDecimal> requerido = new TreeMap<>();
        try (PreparedStatement receta = conexion.prepareStatement(SQL_RECETA_DEL_PRODUCTO)) {
            for (DetallePedido detalle : pedido.getDetalles()) {
                receta.setInt(1, detalle.getProducto().getIdProducto());
                try (ResultSet filas = receta.executeQuery()) {
                    while (filas.next()) {
                        BigDecimal necesario = filas.getBigDecimal("cantidad_necesaria")
                                .multiply(BigDecimal.valueOf(detalle.getCantidad()));
                        requerido.merge(filas.getInt("id_insumo"), necesario, BigDecimal::add);
                    }
                }
            }
        }

        try (PreparedStatement bloquear = conexion.prepareStatement(SQL_BLOQUEAR_INSUMO);
             PreparedStatement descontar = conexion.prepareStatement(SQL_DESCONTAR_INSUMO)) {
            for (Map.Entry<Integer, BigDecimal> insumo : requerido.entrySet()) {
                bloquear.setInt(1, insumo.getKey());
                try (ResultSet fila = bloquear.executeQuery()) {
                    if (!fila.next()) {
                        throw new NegocioException("Un insumo de la receta ya no existe en el sistema. "
                                + "Avisá al administrador.");
                    }
                    BigDecimal disponible = fila.getBigDecimal("stock_actual");
                    if (disponible.compareTo(insumo.getValue()) < 0) {
                        throw new StockInsuficienteException(fila.getString("nombre"),
                                fila.getString("unidad_medida"), insumo.getValue(), disponible);
                    }
                }
                descontar.setBigDecimal(1, insumo.getValue());
                descontar.setInt(2, insumo.getKey());
                descontar.executeUpdate();
            }
        }
    }

    // ------------------------------------------------------------------ listar (CU08, RF18)

    @Override
    public List<Pedido> listarPorEstados(EstadoPedido... estados) {
        if (estados == null || estados.length == 0) {
            return new ArrayList<>();
        }
        try {
            Connection conexion = ConexionBD.getInstancia().getConexion();
            Map<Integer, Pedido> pedidos = leerPedidos(conexion, estados);
            if (!pedidos.isEmpty()) {
                leerDetalles(conexion, pedidos);
            }
            return new ArrayList<>(pedidos.values());
        } catch (SQLException e) {
            throw new DAOException("No se pudieron leer los pedidos", e);
        }
    }

    private Map<Integer, Pedido> leerPedidos(Connection conexion, EstadoPedido[] estados) throws SQLException {
        String sql = SQL_LISTAR_PEDIDOS
                + "WHERE ep.descripcion IN (" + marcadores(estados.length) + ") "
                + "ORDER BY p.fecha_hora ASC, p.id_pedido ASC";
        Map<Integer, Pedido> pedidos = new LinkedHashMap<>();
        try (PreparedStatement consulta = conexion.prepareStatement(sql)) {
            for (int i = 0; i < estados.length; i++) {
                consulta.setString(i + 1, estados[i].getDescripcion());
            }
            try (ResultSet fila = consulta.executeQuery()) {
                while (fila.next()) {
                    Pedido pedido = new Pedido();
                    pedido.setIdPedido(fila.getInt("id_pedido"));
                    int idMesa = fila.getInt("id_mesa");
                    if (!fila.wasNull()) {
                        pedido.setMesa(new Mesa(idMesa, fila.getInt("numero_mesa"), null));
                    }
                    pedido.setEstado(EstadoPedido.desdeDescripcion(fila.getString("estado")));
                    Usuario mozo = new Usuario();
                    mozo.setNombre(fila.getString("mozo"));
                    pedido.setUsuario(mozo);
                    pedido.setHoraCreacion(fila.getString("hora"));
                    pedido.setMinutosEspera(Math.max(fila.getLong("minutos"), 0L));
                    pedidos.put(pedido.getIdPedido(), pedido);
                }
            }
        }
        return pedidos;
    }

    private void leerDetalles(Connection conexion, Map<Integer, Pedido> pedidos) throws SQLException {
        String sql = SQL_LISTAR_DETALLES
                + "WHERE dp.id_pedido IN (" + marcadores(pedidos.size()) + ") "
                + "ORDER BY dp.id_pedido, dp.id_detalle";
        try (PreparedStatement consulta = conexion.prepareStatement(sql)) {
            int posicion = 1;
            for (Integer idPedido : pedidos.keySet()) {
                consulta.setInt(posicion++, idPedido);
            }
            try (ResultSet fila = consulta.executeQuery()) {
                while (fila.next()) {
                    Producto producto = new Producto();
                    producto.setIdProducto(fila.getInt("id_producto"));
                    producto.setNombre(fila.getString("nombre"));
                    DetallePedido detalle = new DetallePedido(producto, fila.getInt("cantidad"),
                            fila.getBigDecimal("precio_unitario"));
                    detalle.setIdDetalle(fila.getInt("id_detalle"));
                    pedidos.get(fila.getInt("id_pedido")).agregarDetalle(detalle);
                }
            }
        }
    }

    // ------------------------------------------------------------------ cambiar estado (CU08)

    @Override
    public void cambiarEstado(int idPedido, EstadoPedido estadoActual, EstadoPedido nuevoEstado) {
        try {
            Connection conexion = ConexionBD.getInstancia().getConexion();
            try (PreparedStatement sentencia = conexion.prepareStatement(SQL_CAMBIAR_ESTADO)) {
                sentencia.setString(1, nuevoEstado.getDescripcion());
                sentencia.setInt(2, idPedido);
                sentencia.setString(3, estadoActual.getDescripcion());
                if (sentencia.executeUpdate() != 1) {
                    throw new NegocioException("El pedido #" + idPedido
                            + " ya cambió de estado desde otra pantalla. Se actualizó la lista.");
                }
            }
        } catch (SQLException e) {
            throw new DAOException("No se pudo cambiar el estado del pedido #" + idPedido, e);
        }
    }

    // ------------------------------------------------------------------ auxiliares

    private static String marcadores(int cantidad) {
        return String.join(", ", Collections.nCopies(cantidad, "?"));
    }

    private static void revertir(Connection conexion) {
        if (conexion != null) {
            try {
                conexion.rollback();
            } catch (SQLException e) {
                LOG.log(Level.SEVERE, "No se pudo revertir la transacción", e);
            }
        }
    }

    private static void cerrar(Connection conexion) {
        if (conexion != null) {
            try {
                conexion.close();
            } catch (SQLException e) {
                LOG.log(Level.WARNING, "No se pudo cerrar la conexión de la transacción", e);
            }
        }
    }
}
