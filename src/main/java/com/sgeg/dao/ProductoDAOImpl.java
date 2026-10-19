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
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ProductoDAOImpl implements IProductoDAO {

    private static final Logger LOG = Logger.getLogger(ProductoDAOImpl.class.getName());

    /** Un producto sin receta queda con "unidades" en NULL (no depende de ningún insumo). */
    private static final String SQL_PRODUCTOS =
            "SELECT p.id_producto, p.nombre, p.precio, p.disponible, "
            + "       c.id_categoria, c.nombre AS categoria, "
            + "       MIN(FLOOR(i.stock_actual / rec.cantidad_necesaria)) AS unidades "
            + "FROM producto p "
            + "JOIN categoria c ON c.id_categoria = p.id_categoria "
            + "LEFT JOIN producto_insumo rec ON rec.id_producto = p.id_producto "
            + "LEFT JOIN insumo i ON i.id_insumo = rec.id_insumo ";

    private static final String SQL_AGRUPAR_Y_ORDENAR =
            "GROUP BY p.id_producto, p.nombre, p.precio, p.disponible, c.id_categoria, c.nombre "
            + "ORDER BY c.nombre, p.nombre";

    private static final String SQL_MENU = SQL_PRODUCTOS + "WHERE p.disponible = TRUE " + SQL_AGRUPAR_Y_ORDENAR;

    private static final String SQL_TODOS = SQL_PRODUCTOS + SQL_AGRUPAR_Y_ORDENAR;

    private static final String SQL_RECETA =
            "SELECT rec.id_insumo, i.nombre, i.unidad_medida, rec.cantidad_necesaria, i.stock_actual "
            + "FROM producto_insumo rec "
            + "JOIN insumo i ON i.id_insumo = rec.id_insumo "
            + "WHERE rec.id_producto = ? "
            + "ORDER BY rec.id_insumo";

    private static final String SQL_INSERTAR =
            "INSERT INTO producto (nombre, precio, disponible, id_categoria) VALUES (?, ?, ?, ?)";

    private static final String SQL_ACTUALIZAR =
            "UPDATE producto SET nombre = ?, precio = ?, disponible = ?, id_categoria = ? WHERE id_producto = ?";

    private static final String SQL_BORRAR_RECETA = "DELETE FROM producto_insumo WHERE id_producto = ?";

    private static final String SQL_INSERTAR_RECETA =
            "INSERT INTO producto_insumo (id_producto, id_insumo, cantidad_necesaria) VALUES (?, ?, ?)";

    private static final String SQL_DISPONIBILIDAD = "UPDATE producto SET disponible = ? WHERE id_producto = ?";

    private static final String SQL_EXISTE_NOMBRE =
            "SELECT COUNT(*) FROM producto WHERE nombre = ? AND id_producto <> ?";

    private static final int TOPE_UNIDADES_MOSTRADAS = 9999;

    @Override
    public List<Producto> listarMenu() {
        return leerProductos(SQL_MENU, "No se pudo leer el menú");
    }

    @Override
    public List<Producto> listarTodos() {
        return leerProductos(SQL_TODOS, "No se pudieron leer los productos");
    }

    private List<Producto> leerProductos(String sql, String mensajeError) {
        try {
            Connection conexion = ConexionBD.getInstancia().getConexion();
            try (PreparedStatement consulta = conexion.prepareStatement(sql);
                 ResultSet resultado = consulta.executeQuery()) {
                List<Producto> productos = new ArrayList<>();
                while (resultado.next()) {
                    productos.add(mapear(resultado));
                }
                return productos;
            }
        } catch (SQLException e) {
            throw new DAOException(mensajeError, e);
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

    // ------------------------------------------------------------------ alta y modificación (CU03)

    @Override
    public void guardar(Producto producto, List<IngredienteReceta> receta) {
        Connection conexion = null;
        try {
            conexion = ConexionBD.getInstancia().abrirConexionNueva();
            conexion.setAutoCommit(false);

            try (PreparedStatement sentencia = conexion.prepareStatement(SQL_INSERTAR, Statement.RETURN_GENERATED_KEYS)) {
                sentencia.setString(1, producto.getNombre());
                sentencia.setBigDecimal(2, producto.getPrecio());
                sentencia.setBoolean(3, producto.isDisponible());
                sentencia.setInt(4, producto.getCategoria().getIdCategoria());
                sentencia.executeUpdate();
                try (ResultSet claves = sentencia.getGeneratedKeys()) {
                    if (!claves.next()) {
                        throw new SQLException("MySQL no devolvió el id del producto");
                    }
                    producto.setIdProducto(claves.getInt(1));
                }
            }
            insertarReceta(conexion, producto.getIdProducto(), receta);

            conexion.commit();
        } catch (SQLException e) {
            revertir(conexion);
            throw new DAOException("No se pudo guardar el producto '" + producto.getNombre() + "'", e);
        } catch (RuntimeException e) {
            revertir(conexion);
            throw e;
        } finally {
            cerrar(conexion);
        }
    }

    @Override
    public void actualizar(Producto producto, List<IngredienteReceta> receta) {
        Connection conexion = null;
        try {
            conexion = ConexionBD.getInstancia().abrirConexionNueva();
            conexion.setAutoCommit(false);

            try (PreparedStatement sentencia = conexion.prepareStatement(SQL_ACTUALIZAR)) {
                sentencia.setString(1, producto.getNombre());
                sentencia.setBigDecimal(2, producto.getPrecio());
                sentencia.setBoolean(3, producto.isDisponible());
                sentencia.setInt(4, producto.getCategoria().getIdCategoria());
                sentencia.setInt(5, producto.getIdProducto());
                sentencia.executeUpdate();
            }
            try (PreparedStatement borrar = conexion.prepareStatement(SQL_BORRAR_RECETA)) {
                borrar.setInt(1, producto.getIdProducto());
                borrar.executeUpdate();
            }
            insertarReceta(conexion, producto.getIdProducto(), receta);

            conexion.commit();
        } catch (SQLException e) {
            revertir(conexion);
            throw new DAOException("No se pudo actualizar el producto #" + producto.getIdProducto(), e);
        } catch (RuntimeException e) {
            revertir(conexion);
            throw e;
        } finally {
            cerrar(conexion);
        }
    }

    private void insertarReceta(Connection conexion, int idProducto, List<IngredienteReceta> receta) throws SQLException {
        if (receta.isEmpty()) {
            return;
        }
        try (PreparedStatement sentencia = conexion.prepareStatement(SQL_INSERTAR_RECETA)) {
            for (IngredienteReceta ingrediente : receta) {
                sentencia.setInt(1, idProducto);
                sentencia.setInt(2, ingrediente.getIdInsumo());
                sentencia.setBigDecimal(3, ingrediente.getCantidadNecesaria());
                sentencia.addBatch();
            }
            sentencia.executeBatch();
        }
    }

    // ------------------------------------------------------------------ baja lógica (CU04)

    @Override
    public void cambiarDisponibilidad(int idProducto, boolean disponible) {
        try {
            Connection conexion = ConexionBD.getInstancia().getConexion();
            try (PreparedStatement sentencia = conexion.prepareStatement(SQL_DISPONIBILIDAD)) {
                sentencia.setBoolean(1, disponible);
                sentencia.setInt(2, idProducto);
                sentencia.executeUpdate();
            }
        } catch (SQLException e) {
            throw new DAOException("No se pudo cambiar la disponibilidad del producto #" + idProducto, e);
        }
    }

    @Override
    public boolean existeNombre(String nombre, int idExcluido) {
        try {
            Connection conexion = ConexionBD.getInstancia().getConexion();
            try (PreparedStatement consulta = conexion.prepareStatement(SQL_EXISTE_NOMBRE)) {
                consulta.setString(1, nombre);
                consulta.setInt(2, idExcluido);
                try (ResultSet fila = consulta.executeQuery()) {
                    return fila.next() && fila.getInt(1) > 0;
                }
            }
        } catch (SQLException e) {
            throw new DAOException("No se pudo verificar el nombre del producto", e);
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
