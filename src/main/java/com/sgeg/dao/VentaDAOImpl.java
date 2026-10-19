package com.sgeg.dao;

import com.sgeg.excepcion.NegocioException;
import com.sgeg.modelo.DetallePedido;
import com.sgeg.modelo.EstadoMesa;
import com.sgeg.modelo.EstadoPedido;
import com.sgeg.modelo.FormaPago;
import com.sgeg.modelo.Pago;
import com.sgeg.modelo.Producto;
import com.sgeg.modelo.Venta;
import com.sgeg.util.ConexionBD;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class VentaDAOImpl implements IVentaDAO {

    private static final Logger LOG = Logger.getLogger(VentaDAOImpl.class.getName());

    private static final String SQL_FORMAS_DE_PAGO =
            "SELECT id_forma_pago, descripcion FROM forma_pago ORDER BY id_forma_pago";

    private static final String SQL_MESA_DEL_PEDIDO =
            "SELECT id_mesa FROM pedido WHERE id_pedido = ?";

    private static final String SQL_LIBERAR_MESA =
            "UPDATE mesa SET id_estado_mesa = (SELECT id_estado_mesa FROM estado_mesa WHERE descripcion = ?) "
            + "WHERE id_mesa = ? "
            + "AND id_estado_mesa = (SELECT id_estado_mesa FROM estado_mesa WHERE descripcion = ?)";

    // "FOR UPDATE OF p" bloquea solo la fila del pedido (no la del catálogo estado_pedido, que comparten todos los pedidos). Requiere MySQL 8.
    private static final String SQL_BLOQUEAR_PEDIDO =
            "SELECT p.total, ep.descripcion AS estado FROM pedido p "
            + "JOIN estado_pedido ep ON ep.id_estado_pedido = p.id_estado_pedido "
            + "WHERE p.id_pedido = ? FOR UPDATE OF p";

    // La fecha y hora de la venta la pone MySQL (DEFAULT CURRENT_TIMESTAMP).
    private static final String SQL_INSERTAR_VENTA =
            "INSERT INTO venta (id_pedido, total) VALUES (?, ?)";

    private static final String SQL_INSERTAR_PAGO =
            "INSERT INTO pago (id_venta, id_forma_pago, monto) VALUES (?, ?, ?)";

    private static final String SQL_FACTURAR_PEDIDO =
            "UPDATE pedido SET id_estado_pedido = (SELECT id_estado_pedido FROM estado_pedido WHERE descripcion = ?) "
            + "WHERE id_pedido = ? "
            + "AND id_estado_pedido = (SELECT id_estado_pedido FROM estado_pedido WHERE descripcion = ?)";

    private static final String SQL_VENTA =
            "SELECT v.id_venta, v.id_pedido, v.total, DATE_FORMAT(v.fecha_hora, '%d/%m/%Y %H:%i') AS fecha, "
            + "       m.numero AS numero_mesa, u.nombre AS mozo, c.nombre AS cliente "
            + "FROM venta v "
            + "JOIN pedido p ON p.id_pedido = v.id_pedido "
            + "JOIN usuario u ON u.id_usuario = p.id_usuario "
            + "LEFT JOIN mesa m ON m.id_mesa = p.id_mesa "
            + "LEFT JOIN cliente c ON c.id_cliente = p.id_cliente "
            + "WHERE v.id_venta = ?";

    private static final String SQL_DETALLES_DE_LA_VENTA =
            "SELECT dp.id_detalle, dp.id_producto, pr.nombre, dp.cantidad, dp.precio_unitario "
            + "FROM detalle_pedido dp "
            + "JOIN producto pr ON pr.id_producto = dp.id_producto "
            + "WHERE dp.id_pedido = ? ORDER BY dp.id_detalle";

    private static final String SQL_PAGOS_DE_LA_VENTA =
            "SELECT fp.id_forma_pago, fp.descripcion, pa.monto "
            + "FROM pago pa "
            + "JOIN forma_pago fp ON fp.id_forma_pago = pa.id_forma_pago "
            + "WHERE pa.id_venta = ? ORDER BY pa.id_pago";

    @Override
    public List<FormaPago> listarFormasDePago() {
        try {
            Connection conexion = ConexionBD.getInstancia().getConexion();
            try (PreparedStatement consulta = conexion.prepareStatement(SQL_FORMAS_DE_PAGO);
                 ResultSet filas = consulta.executeQuery()) {
                List<FormaPago> formas = new ArrayList<>();
                while (filas.next()) {
                    formas.add(new FormaPago(filas.getInt("id_forma_pago"), filas.getString("descripcion")));
                }
                return formas;
            }
        } catch (SQLException e) {
            throw new DAOException("No se pudieron leer las formas de pago", e);
        }
    }

    // ------------------------------------------------------------------ cobro (CU13, CU14)

    @Override
    public int registrarCobro(int idPedido, List<Pago> pagos) {
        Connection conexion = null;
        try {
            conexion = ConexionBD.getInstancia().abrirConexionNueva();
            conexion.setAutoCommit(false);

            // Mismo orden de bloqueos que al registrar o cancelar un pedido: mesa -> pedido.
            Integer idMesa = mesaDelPedido(conexion, idPedido);
            if (idMesa != null) {
                liberarMesa(conexion, idMesa);
            }
            BigDecimal total = bloquearPedidoEntregado(conexion, idPedido);

            BigDecimal pagado = BigDecimal.ZERO;
            for (Pago pago : pagos) {
                pagado = pagado.add(pago.getMonto());
            }
            if (pagado.compareTo(total) != 0) {
                throw new NegocioException("Los pagos suman " + pagado.toPlainString()
                        + " y el pedido cuesta " + total.toPlainString() + ". Revisá los importes.");
            }

            int idVenta = insertarVenta(conexion, idPedido, total);
            insertarPagos(conexion, idVenta, pagos);
            facturarPedido(conexion, idPedido);

            conexion.commit();
            return idVenta;
        } catch (NegocioException e) {
            revertir(conexion);
            throw e;
        } catch (SQLException e) {
            revertir(conexion);
            throw new DAOException("No se pudo registrar el cobro del pedido #" + idPedido, e);
        } catch (RuntimeException e) {
            revertir(conexion);
            throw e;
        } finally {
            cerrar(conexion);
        }
    }

    private Integer mesaDelPedido(Connection conexion, int idPedido) throws SQLException {
        try (PreparedStatement consulta = conexion.prepareStatement(SQL_MESA_DEL_PEDIDO)) {
            consulta.setInt(1, idPedido);
            try (ResultSet fila = consulta.executeQuery()) {
                if (!fila.next()) {
                    throw new NegocioException("El pedido #" + idPedido + " ya no existe.");
                }
                int idMesa = fila.getInt("id_mesa");
                return fila.wasNull() ? null : idMesa;
            }
        }
    }

    private void liberarMesa(Connection conexion, int idMesa) throws SQLException {
        try (PreparedStatement sentencia = conexion.prepareStatement(SQL_LIBERAR_MESA)) {
            sentencia.setString(1, EstadoMesa.LIBRE.getDescripcion());
            sentencia.setInt(2, idMesa);
            sentencia.setString(3, EstadoMesa.OCUPADA.getDescripcion());
            sentencia.executeUpdate();
        }
    }

    /** Bloquea el pedido, verifica que siga "Entregado" y devuelve su total guardado. */
    private BigDecimal bloquearPedidoEntregado(Connection conexion, int idPedido) throws SQLException {
        try (PreparedStatement consulta = conexion.prepareStatement(SQL_BLOQUEAR_PEDIDO)) {
            consulta.setInt(1, idPedido);
            try (ResultSet fila = consulta.executeQuery()) {
                if (!fila.next()) {
                    throw new NegocioException("El pedido #" + idPedido + " ya no existe.");
                }
                EstadoPedido estado = EstadoPedido.desdeDescripcion(fila.getString("estado"));
                if (estado != EstadoPedido.ENTREGADO) {
                    throw new NegocioException("El pedido #" + idPedido + " no se puede cobrar: está «"
                            + estado.getDescripcion() + "»."
                            + (estado == EstadoPedido.FACTURADO ? " Ya lo cobró otro cajero." : ""));
                }
                return fila.getBigDecimal("total");
            }
        }
    }

    private int insertarVenta(Connection conexion, int idPedido, BigDecimal total) throws SQLException {
        try (PreparedStatement sentencia = conexion.prepareStatement(SQL_INSERTAR_VENTA, Statement.RETURN_GENERATED_KEYS)) {
            sentencia.setInt(1, idPedido);
            sentencia.setBigDecimal(2, total);
            sentencia.executeUpdate();
            try (ResultSet claves = sentencia.getGeneratedKeys()) {
                if (!claves.next()) {
                    throw new SQLException("MySQL no devolvió el id de la venta");
                }
                return claves.getInt(1);
            }
        }
    }

    private void insertarPagos(Connection conexion, int idVenta, List<Pago> pagos) throws SQLException {
        try (PreparedStatement sentencia = conexion.prepareStatement(SQL_INSERTAR_PAGO)) {
            for (Pago pago : pagos) {
                sentencia.setInt(1, idVenta);
                sentencia.setInt(2, pago.getFormaPago().getIdFormaPago());
                sentencia.setBigDecimal(3, pago.getMonto());
                sentencia.addBatch();
            }
            sentencia.executeBatch();
        }
    }

    private void facturarPedido(Connection conexion, int idPedido) throws SQLException {
        try (PreparedStatement sentencia = conexion.prepareStatement(SQL_FACTURAR_PEDIDO)) {
            sentencia.setString(1, EstadoPedido.FACTURADO.getDescripcion());
            sentencia.setInt(2, idPedido);
            sentencia.setString(3, EstadoPedido.ENTREGADO.getDescripcion());
            if (sentencia.executeUpdate() != 1) {
                throw new NegocioException("El pedido #" + idPedido + " cambió de estado mientras se cobraba.");
            }
        }
    }

    // ------------------------------------------------------------------ comprobante (CU13)

    @Override
    public Venta buscarVenta(int idVenta) {
        try {
            Connection conexion = ConexionBD.getInstancia().getConexion();
            Venta venta;
            try (PreparedStatement consulta = conexion.prepareStatement(SQL_VENTA)) {
                consulta.setInt(1, idVenta);
                try (ResultSet fila = consulta.executeQuery()) {
                    if (!fila.next()) {
                        return null;
                    }
                    venta = new Venta();
                    venta.setIdVenta(fila.getInt("id_venta"));
                    venta.setIdPedido(fila.getInt("id_pedido"));
                    venta.setTotal(fila.getBigDecimal("total"));
                    venta.setFechaHora(fila.getString("fecha"));
                    int mesa = fila.getInt("numero_mesa");
                    venta.setNumeroMesa(fila.wasNull() ? null : mesa);
                    venta.setMozo(fila.getString("mozo"));
                    venta.setCliente(fila.getString("cliente"));
                }
            }
            try (PreparedStatement consulta = conexion.prepareStatement(SQL_DETALLES_DE_LA_VENTA)) {
                consulta.setInt(1, venta.getIdPedido());
                try (ResultSet fila = consulta.executeQuery()) {
                    while (fila.next()) {
                        Producto producto = new Producto();
                        producto.setIdProducto(fila.getInt("id_producto"));
                        producto.setNombre(fila.getString("nombre"));
                        DetallePedido detalle = new DetallePedido(producto, fila.getInt("cantidad"),
                                fila.getBigDecimal("precio_unitario"));
                        detalle.setIdDetalle(fila.getInt("id_detalle"));
                        venta.agregarDetalle(detalle);
                    }
                }
            }
            try (PreparedStatement consulta = conexion.prepareStatement(SQL_PAGOS_DE_LA_VENTA)) {
                consulta.setInt(1, venta.getIdVenta());
                try (ResultSet fila = consulta.executeQuery()) {
                    while (fila.next()) {
                        venta.agregarPago(new Pago(
                                new FormaPago(fila.getInt("id_forma_pago"), fila.getString("descripcion")),
                                fila.getBigDecimal("monto")));
                    }
                }
            }
            return venta;
        } catch (SQLException e) {
            throw new DAOException("No se pudo leer la venta #" + idVenta, e);
        }
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
