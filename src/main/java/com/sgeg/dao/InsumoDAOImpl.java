package com.sgeg.dao;

import com.sgeg.excepcion.NegocioException;
import com.sgeg.modelo.Insumo;
import com.sgeg.util.ConexionBD;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class InsumoDAOImpl implements IInsumoDAO {

    private static final String SQL_LISTAR =
            "SELECT id_insumo, nombre, stock_actual, stock_minimo, unidad_medida FROM insumo ORDER BY nombre";

    private static final String SQL_INSERTAR =
            "INSERT INTO insumo (nombre, stock_actual, stock_minimo, unidad_medida) VALUES (?, ?, ?, ?)";

    private static final String SQL_ACTUALIZAR =
            "UPDATE insumo SET nombre = ?, stock_minimo = ?, unidad_medida = ? WHERE id_insumo = ?";

    private static final String SQL_SUMAR_STOCK =
            "UPDATE insumo SET stock_actual = stock_actual + ? WHERE id_insumo = ?";

    private static final String SQL_EXISTE_NOMBRE =
            "SELECT COUNT(*) FROM insumo WHERE nombre = ? AND id_insumo <> ?";

    private static final String SQL_CONTAR_BAJO_MINIMO =
            "SELECT COUNT(*) FROM insumo WHERE stock_actual <= stock_minimo";

    @Override
    public List<Insumo> listarTodos() {
        try {
            Connection conexion = ConexionBD.getInstancia().getConexion();
            try (PreparedStatement consulta = conexion.prepareStatement(SQL_LISTAR);
                 ResultSet filas = consulta.executeQuery()) {
                List<Insumo> insumos = new ArrayList<>();
                while (filas.next()) {
                    insumos.add(new Insumo(filas.getInt("id_insumo"), filas.getString("nombre"),
                            filas.getBigDecimal("stock_actual"), filas.getBigDecimal("stock_minimo"),
                            filas.getString("unidad_medida")));
                }
                return insumos;
            }
        } catch (SQLException e) {
            throw new DAOException("No se pudieron leer los insumos", e);
        }
    }

    @Override
    public void guardar(Insumo insumo) {
        try {
            Connection conexion = ConexionBD.getInstancia().getConexion();
            try (PreparedStatement sentencia = conexion.prepareStatement(SQL_INSERTAR, Statement.RETURN_GENERATED_KEYS)) {
                sentencia.setString(1, insumo.getNombre());
                sentencia.setBigDecimal(2, insumo.getStockActual());
                sentencia.setBigDecimal(3, insumo.getStockMinimo());
                sentencia.setString(4, insumo.getUnidadMedida());
                sentencia.executeUpdate();
                try (ResultSet claves = sentencia.getGeneratedKeys()) {
                    if (claves.next()) {
                        insumo.setIdInsumo(claves.getInt(1));
                    }
                }
            }
        } catch (SQLException e) {
            throw new DAOException("No se pudo guardar el insumo '" + insumo.getNombre() + "'", e);
        }
    }

    @Override
    public void actualizar(Insumo insumo) {
        try {
            Connection conexion = ConexionBD.getInstancia().getConexion();
            try (PreparedStatement sentencia = conexion.prepareStatement(SQL_ACTUALIZAR)) {
                sentencia.setString(1, insumo.getNombre());
                sentencia.setBigDecimal(2, insumo.getStockMinimo());
                sentencia.setString(3, insumo.getUnidadMedida());
                sentencia.setInt(4, insumo.getIdInsumo());
                sentencia.executeUpdate();
            }
        } catch (SQLException e) {
            throw new DAOException("No se pudo actualizar el insumo #" + insumo.getIdInsumo(), e);
        }
    }

    @Override
    public void sumarStock(int idInsumo, BigDecimal cantidad) {
        try {
            Connection conexion = ConexionBD.getInstancia().getConexion();
            try (PreparedStatement sentencia = conexion.prepareStatement(SQL_SUMAR_STOCK)) {
                sentencia.setBigDecimal(1, cantidad);
                sentencia.setInt(2, idInsumo);
                if (sentencia.executeUpdate() != 1) {
                    throw new NegocioException("El insumo ya no existe. Actualizá la pantalla.");
                }
            }
        } catch (SQLException e) {
            throw new DAOException("No se pudo registrar el ingreso de stock del insumo #" + idInsumo, e);
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
            throw new DAOException("No se pudo verificar el nombre del insumo", e);
        }
    }

    @Override
    public int contarBajoMinimo() {
        try {
            Connection conexion = ConexionBD.getInstancia().getConexion();
            try (PreparedStatement consulta = conexion.prepareStatement(SQL_CONTAR_BAJO_MINIMO);
                 ResultSet fila = consulta.executeQuery()) {
                return fila.next() ? fila.getInt(1) : 0;
            }
        } catch (SQLException e) {
            throw new DAOException("No se pudo contar los insumos bajo el mínimo", e);
        }
    }
}
