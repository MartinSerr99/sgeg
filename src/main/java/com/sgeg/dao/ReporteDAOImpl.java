package com.sgeg.dao;

import com.sgeg.modelo.ProductoVendido;
import com.sgeg.modelo.ResumenVenta;
import com.sgeg.modelo.TotalFormaPago;
import com.sgeg.util.ConexionBD;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class ReporteDAOImpl implements IReporteDAO {

    // Las fechas viajan como texto ISO (yyyy-MM-dd): así no influye la zona horaria de la PC.
    // Rango [desde 00:00, hasta + 1 día 00:00): incluye el día "hasta" completo y puede usar índices.
    private static final String FILTRO_PERIODO =
            "v.fecha_hora >= CAST(? AS DATE) AND v.fecha_hora < DATE_ADD(CAST(? AS DATE), INTERVAL 1 DAY) ";

    private static final String SQL_VENTAS =
            "SELECT v.id_venta, DATE_FORMAT(v.fecha_hora, '%d/%m/%Y %H:%i') AS fecha, "
            + "       m.numero AS numero_mesa, u.nombre AS mozo, v.total "
            + "FROM venta v "
            + "JOIN pedido p ON p.id_pedido = v.id_pedido "
            + "JOIN usuario u ON u.id_usuario = p.id_usuario "
            + "LEFT JOIN mesa m ON m.id_mesa = p.id_mesa "
            + "WHERE " + FILTRO_PERIODO
            + "ORDER BY v.fecha_hora ASC, v.id_venta ASC";

    private static final String SQL_TOTALES_POR_FORMA =
            "SELECT fp.descripcion AS forma, COUNT(*) AS cantidad, SUM(pa.monto) AS total "
            + "FROM pago pa "
            + "JOIN venta v ON v.id_venta = pa.id_venta "
            + "JOIN forma_pago fp ON fp.id_forma_pago = pa.id_forma_pago "
            + "WHERE " + FILTRO_PERIODO
            + "GROUP BY fp.id_forma_pago, fp.descripcion "
            + "ORDER BY fp.id_forma_pago";

    private static final String SQL_PRODUCTOS_MAS_VENDIDOS =
            "SELECT pr.nombre, SUM(dp.cantidad) AS unidades, SUM(dp.subtotal) AS monto "
            + "FROM detalle_pedido dp "
            + "JOIN venta v ON v.id_pedido = dp.id_pedido "
            + "JOIN producto pr ON pr.id_producto = dp.id_producto "
            + "WHERE " + FILTRO_PERIODO
            + "GROUP BY pr.id_producto, pr.nombre "
            + "ORDER BY unidades DESC, pr.nombre ASC "
            + "LIMIT ?";

    @Override
    public List<ResumenVenta> ventasDelPeriodo(LocalDate desde, LocalDate hasta) {
        try {
            Connection conexion = ConexionBD.getInstancia().getConexion();
            try (PreparedStatement consulta = conexion.prepareStatement(SQL_VENTAS)) {
                consulta.setString(1, desde.toString());
                consulta.setString(2, hasta.toString());
                try (ResultSet fila = consulta.executeQuery()) {
                    List<ResumenVenta> ventas = new ArrayList<>();
                    while (fila.next()) {
                        int mesa = fila.getInt("numero_mesa");
                        String destino = fila.wasNull() ? "Para llevar" : "Mesa " + mesa;
                        ventas.add(new ResumenVenta(fila.getInt("id_venta"), fila.getString("fecha"), destino,
                                fila.getString("mozo"), fila.getBigDecimal("total")));
                    }
                    return ventas;
                }
            }
        } catch (SQLException e) {
            throw new DAOException("No se pudieron leer las ventas del período", e);
        }
    }

    @Override
    public List<TotalFormaPago> totalesPorFormaDePago(LocalDate desde, LocalDate hasta) {
        try {
            Connection conexion = ConexionBD.getInstancia().getConexion();
            try (PreparedStatement consulta = conexion.prepareStatement(SQL_TOTALES_POR_FORMA)) {
                consulta.setString(1, desde.toString());
                consulta.setString(2, hasta.toString());
                try (ResultSet fila = consulta.executeQuery()) {
                    List<TotalFormaPago> totales = new ArrayList<>();
                    while (fila.next()) {
                        totales.add(new TotalFormaPago(fila.getString("forma"), fila.getInt("cantidad"),
                                fila.getBigDecimal("total")));
                    }
                    return totales;
                }
            }
        } catch (SQLException e) {
            throw new DAOException("No se pudieron leer los totales por forma de pago", e);
        }
    }

    @Override
    public List<ProductoVendido> productosMasVendidos(LocalDate desde, LocalDate hasta, int limite) {
        try {
            Connection conexion = ConexionBD.getInstancia().getConexion();
            try (PreparedStatement consulta = conexion.prepareStatement(SQL_PRODUCTOS_MAS_VENDIDOS)) {
                consulta.setString(1, desde.toString());
                consulta.setString(2, hasta.toString());
                consulta.setInt(3, limite);
                try (ResultSet fila = consulta.executeQuery()) {
                    List<ProductoVendido> productos = new ArrayList<>();
                    while (fila.next()) {
                        productos.add(new ProductoVendido(fila.getString("nombre"), fila.getLong("unidades"),
                                fila.getBigDecimal("monto")));
                    }
                    return productos;
                }
            }
        } catch (SQLException e) {
            throw new DAOException("No se pudo leer el reporte de productos más vendidos", e);
        }
    }
}
