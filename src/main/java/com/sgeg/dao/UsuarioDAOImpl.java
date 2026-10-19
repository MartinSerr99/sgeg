package com.sgeg.dao;

import com.sgeg.modelo.Rol;
import com.sgeg.modelo.Usuario;
import com.sgeg.util.ConexionBD;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class UsuarioDAOImpl implements IUsuarioDAO {

    private static final String SQL_BUSCAR_POR_NOMBRE_USUARIO =
            "SELECT u.id_usuario, u.nombre, u.nombre_usuario, u.contrasenia_hash, u.activo, "
            + "       r.id_rol, r.descripcion AS rol_descripcion "
            + "FROM usuario u "
            + "JOIN rol r ON r.id_rol = u.id_rol "
            + "WHERE u.nombre_usuario = ?";

    private static final String SQL_INSERTAR =
            "INSERT INTO usuario (nombre, nombre_usuario, contrasenia_hash, id_rol, activo) "
            + "VALUES (?, ?, ?, ?, ?)";

    @Override
    public Usuario buscarPorNombreUsuario(String nombreUsuario) {
        try {
            Connection conexion = ConexionBD.getInstancia().getConexion();
            try (PreparedStatement consulta = conexion.prepareStatement(SQL_BUSCAR_POR_NOMBRE_USUARIO)) {
                consulta.setString(1, nombreUsuario);
                try (ResultSet resultado = consulta.executeQuery()) {
                    return resultado.next() ? mapear(resultado) : null;
                }
            }
        } catch (SQLException e) {
            throw new DAOException("No se pudo buscar el usuario '" + nombreUsuario + "'", e);
        }
    }

    @Override
    public void guardar(Usuario usuario) {
        try {
            Connection conexion = ConexionBD.getInstancia().getConexion();
            try (PreparedStatement sentencia = conexion.prepareStatement(SQL_INSERTAR, Statement.RETURN_GENERATED_KEYS)) {
                sentencia.setString(1, usuario.getNombre());
                sentencia.setString(2, usuario.getNombreUsuario());
                sentencia.setString(3, usuario.getContraseniaHash());
                sentencia.setInt(4, usuario.getRol().getIdRol());
                sentencia.setBoolean(5, usuario.isActivo());
                sentencia.executeUpdate();
                try (ResultSet claves = sentencia.getGeneratedKeys()) {
                    if (claves.next()) {
                        usuario.setIdUsuario(claves.getInt(1));
                    }
                }
            }
        } catch (SQLException e) {
            throw new DAOException("No se pudo guardar el usuario '" + usuario.getNombreUsuario() + "'", e);
        }
    }

    private Usuario mapear(ResultSet fila) throws SQLException {
        Rol rol = new Rol(fila.getInt("id_rol"), fila.getString("rol_descripcion"));
        Usuario usuario = new Usuario();
        usuario.setIdUsuario(fila.getInt("id_usuario"));
        usuario.setNombre(fila.getString("nombre"));
        usuario.setNombreUsuario(fila.getString("nombre_usuario"));
        usuario.setContraseniaHash(fila.getString("contrasenia_hash"));
        usuario.setActivo(fila.getBoolean("activo"));
        usuario.setRol(rol);
        return usuario;
    }
}
