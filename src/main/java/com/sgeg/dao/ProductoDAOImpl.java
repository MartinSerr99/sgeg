package com.sgeg.dao;

import com.sgeg.modelo.Categoria;
import com.sgeg.modelo.IngredienteReceta;
import com.sgeg.modelo.Producto;
import com.sgeg.util.ConexionBD;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class ProductoDAOImpl implements IProductoDAO {

    /** Un producto sin receta queda con "unidades" en NULL (no depende de ningún insumo). */
    private static final String SQL_MENU =
            "SELECT p.id_producto, p.nombre, p.precio, p.disponible, "
            + "       c.id_categoria, c.nombre AS categoria, "
            + "       MIN(FLOOR(i.stock_actual / rec.cantidad_necesaria)) AS unidades "
            + "FROM producto p "
            + "JOIN categoria c ON c.id_categoria = p.id_categoria "
            + "LEFT JOIN producto_insumo rec ON rec.id_producto = p.id_producto "
            + "LEFT JOIN insumo i ON i.id_insumo = rec.id_insumo "
            + "WHERE p.disponible = TRUE "
            + "GROUP BY p.id_producto, p.nombre, p.precio, p.disponible, c.id_categoria, c.nombre "
            + "ORDER BY c.nombre, p.nombre";

    private static final String SQL_RECETA =
            "SELECT rec.id_insumo, i.nombre, i.unidad_medida, rec.cantidad_necesaria, i.stock_actual "
            + "FROM producto_insumo rec "
            + "JOIN insumo i ON i.id_insumo = rec.id_insumo "
            + "WHERE rec.id_producto = ? "
            + "ORDER BY rec.id_insumo";

    private static final int TOPE_UNIDADES_MOSTRADAS = 9999;

    @Override
    public List<Producto> listarMenu() {
        try {
            Connection conexion = ConexionBD.getInstancia().getConexion();
            try (PreparedStatement consulta = conexion.prepareStatement(SQL_MENU);
                 ResultSet resultado = consulta.executeQuery()) {
                List<Producto> menu = new ArrayList<>();
                while (resultado.next()) {
                    menu.add(mapear(resultado));
                }
                return menu;
            }
        } catch (SQLException e) {
            throw new DAOException("No se pudo leer el menú", e);
        }
    }

    @Override
    public List<IngredienteReceta> obtenerReceta(int idProducto) {
        try {
            Connection conexion = ConexionBD.getInstancia().getConexion();
            try (PreparedStatement consulta = conexion.prepareStatement(SQL_RECETA)) {
                consulta.setInt(1, idProducto);
                try (ResultSet resultado = consulta.executeQuery()) {
                    List<IngredienteReceta> receta = new ArrayList<>();
                    while (resultado.next()) {
                        receta.add(new IngredienteReceta(
                                resultado.getInt("id_insumo"),
                                resultado.getString("nombre"),
                                resultado.getString("unidad_medida"),
                                resultado.getBigDecimal("cantidad_necesaria"),
                                resultado.getBigDecimal("stock_actual")));
                    }
                    return receta;
                }
            }
        } catch (SQLException e) {
            throw new DAOException("No se pudo leer la receta del producto " + idProducto, e);
        }
    }

    private Producto mapear(ResultSet fila) throws SQLException {
        Producto producto = new Producto();
        producto.setIdProducto(fila.getInt("id_producto"));
        producto.setNombre(fila.getString("nombre"));
        producto.setPrecio(fila.getBigDecimal("precio"));
        producto.setDisponible(fila.getBoolean("disponible"));
        producto.setCategoria(new Categoria(fila.getInt("id_categoria"), fila.getString("categoria")));

        BigDecimal unidades = fila.getBigDecimal("unidades");
        if (unidades == null) {
            producto.setUnidadesDisponibles(Producto.SIN_LIMITE);
        } else {
            long enteras = Math.max(unidades.longValue(), 0L);
            producto.setUnidadesDisponibles((int) Math.min(enteras, TOPE_UNIDADES_MOSTRADAS));
        }
        return producto;
    }
}
