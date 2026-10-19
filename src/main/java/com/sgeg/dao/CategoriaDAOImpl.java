package com.sgeg.dao;

import com.sgeg.modelo.Categoria;
import com.sgeg.util.ConexionBD;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class CategoriaDAOImpl implements ICategoriaDAO {

    private static final String SQL_LISTAR = "SELECT id_categoria, nombre FROM categoria ORDER BY nombre";
    private static final String SQL_INSERTAR = "INSERT INTO categoria (nombre) VALUES (?)";
    private static final String SQL_RENOMBRAR = "UPDATE categoria SET nombre = ? WHERE id_categoria = ?";
    private static final String SQL_CONTAR_PRODUCTOS = "SELECT COUNT(*) FROM producto WHERE id_categoria = ?";
    private static final String SQL_ELIMINAR = "DELETE FROM categoria WHERE id_categoria = ?";
    private static final String SQL_EXISTE_NOMBRE = "SELECT COUNT(*) FROM categoria WHERE nombre = ? AND id_categoria <> ?";

    @Override
    public List<Categoria> listarTodas() {
        try {
            Connection conexion = ConexionBD.getInstancia().getConexion();
            try (PreparedStatement consulta = conexion.prepareStatement(SQL_LISTAR);
                 ResultSet filas = consulta.executeQuery()) {
                List<Categoria> categorias = new ArrayList<>();
                while (filas.next()) {
                    categorias.add(new Categoria(filas.getInt("id_categoria"), filas.getString("nombre")));
                }
                return categorias;
            }
        } catch (SQLException e) {
            throw new DAOException("No se pudieron leer las categorías", e);
        }
    }

    @Override
    public void guardar(Categoria categoria) {
        try {
            Connection conexion = ConexionBD.getInstancia().getConexion();
            try (PreparedStatement sentencia = conexion.prepareStatement(SQL_INSERTAR, Statement.RETURN_GENERATED_KEYS)) {
                sentencia.setString(1, categoria.getNombre());
                sentencia.executeUpdate();
                try (ResultSet claves = sentencia.getGeneratedKeys()) {
                    if (claves.next()) {
                        categoria.setIdCategoria(claves.getInt(1));
                    }
                }
            }
        } catch (SQLException e) {
            throw new DAOException("No se pudo guardar la categoría '" + categoria.getNombre() + "'", e);
        }
    }

    @Override
    public void renombrar(int idCategoria, String nombre) {
        try {
            Connection conexion = ConexionBD.getInstancia().getConexion();
            try (PreparedStatement sentencia = conexion.prepareStatement(SQL_RENOMBRAR)) {
                sentencia.setString(1, nombre);
                sentencia.setInt(2, idCategoria);
                sentencia.executeUpdate();
            }
        } catch (SQLException e) {
            throw new DAOException("No se pudo renombrar la categoría #" + idCategoria, e);
        }
    }

    @Override
    public int contarProductos(int idCategoria) {
        try {
            Connection conexion = ConexionBD.getInstancia().getConexion();
            try (PreparedStatement consulta = conexion.prepareStatement(SQL_CONTAR_PRODUCTOS)) {
                consulta.setInt(1, idCategoria);
                try (ResultSet fila = consulta.executeQuery()) {
                    return fila.next() ? fila.getInt(1) : 0;
                }
            }
        } catch (SQLException e) {
            throw new DAOException("No se pudieron contar los productos de la categoría #" + idCategoria, e);
        }
    }

    @Override
    public void eliminar(int idCategoria) {
        try {
            Connection conexion = ConexionBD.getInstancia().getConexion();
            try (PreparedStatement sentencia = conexion.prepareStatement(SQL_ELIMINAR)) {
                sentencia.setInt(1, idCategoria);
                sentencia.executeUpdate();
            }
        } catch (SQLException e) {
            throw new DAOException("No se pudo eliminar la categoría #" + idCategoria, e);
        }
    }

    @Override
    public boolean existeNombre(String nombre, int idExcluida) {
        try {
            Connection conexion = ConexionBD.getInstancia().getConexion();
            try (PreparedStatement consulta = conexion.prepareStatement(SQL_EXISTE_NOMBRE)) {
                consulta.setString(1, nombre);
                consulta.setInt(2, idExcluida);
                try (ResultSet fila = consulta.executeQuery()) {
                    return fila.next() && fila.getInt(1) > 0;
                }
            }
        } catch (SQLException e) {
            throw new DAOException("No se pudo verificar el nombre de la categoría", e);
        }
    }
}
