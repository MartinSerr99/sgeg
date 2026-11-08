package com.sgeg.dao;

import com.sgeg.util.ConexionBD;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Clase abstracta de la que heredan todos los DAO concretos (UsuarioDAOImpl, PedidoDAOImpl, etc.).
 *
 * <p>Reúne lo que todos necesitan y antes estaba repetido: pedir la conexión a {@link ConexionBD},
 * deshacer y cerrar una transacción, y convertir una {@link SQLException} en una {@link DAOException}.
 * Lo único que cada DAO debe aportar es {@link #entidad()}: el nombre de lo que administra, que se
 * usa para que los mensajes de error digan en qué DAO ocurrieron.
 *
 * <p>Se eligió una clase abstracta y no una interfaz porque tiene código compartido y no solo un
 * contrato. El contrato de cada DAO sigue estando en su interfaz (IUsuarioDAO, IPedidoDAO, etc.),
 * de la que depende la capa de servicio.
 */
public abstract class BaseDAO {

    private static final Logger LOG = Logger.getLogger(BaseDAO.class.getName());

    /** Nombre de la entidad que administra el DAO, por ejemplo "usuario" o "pedido". */
    protected abstract String entidad();

    /** Conexión compartida para consultas simples. Los DAO no deben cerrarla. */
    protected final Connection conexionCompartida() throws SQLException {
        return ConexionBD.getInstancia().getConexion();
    }

    /**
     * Conexión propia para una transacción (varias sentencias que se confirman o se deshacen juntas).
     * Quien la pide debe cerrarla con {@link #cerrar(Connection)}.
     */
    protected final Connection conexionPropia() throws SQLException {
        return ConexionBD.getInstancia().abrirConexionNueva();
    }

    /** Convierte la SQLException en una DAOException con el nombre del DAO delante del mensaje. */
    protected final DAOException fallo(String mensaje, SQLException causa) {
        return new DAOException(entidad() + ": " + mensaje, causa);
    }

    /** Deshace la transacción en curso. Si no hay conexión no hace nada. */
    protected static void revertir(Connection conexion) {
        if (conexion != null) {
            try {
                conexion.rollback();
            } catch (SQLException e) {
                LOG.log(Level.SEVERE, "No se pudo revertir la transacción", e);
            }
        }
    }

    /** Cierra la conexión propia de una transacción. Si no hay conexión no hace nada. */
    protected static void cerrar(Connection conexion) {
        if (conexion != null) {
            try {
                conexion.close();
            } catch (SQLException e) {
                LOG.log(Level.WARNING, "No se pudo cerrar la conexión de la transacción", e);
            }
        }
    }
}
