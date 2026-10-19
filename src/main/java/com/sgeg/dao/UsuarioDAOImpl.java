package com.sgeg.dao;

import com.sgeg.modelo.Rol;
import com.sgeg.modelo.Usuario;
import com.sgeg.util.ConexionBD;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

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

    private static final String SQL_LISTAR =
            "SELECT u.id_usuario, u.nombre, u.nombre_usuario, u.contrasenia_hash, u.activo, "
            + "       r.id_rol, r.descripcion AS rol_descripcion "
            + "FROM usuario u "
            + "JOIN rol r ON r.id_rol = u.id_rol "
            + "ORDER BY u.nombre";

    private static final String SQL_ROLES = "SELECT id_rol, descripcion FROM rol ORDER BY id_rol";

    private static final String SQL_ACTUALIZAR = "UPDATE usuario SET nombre = ?, id_rol = ? WHERE id_usuario = ?";

    private static final String SQL_CAMBIAR_CONTRASENIA =
            "UPDATE usuario SET contrasenia_hash = ? WHERE id_usuario = ?";

    private static final String SQL_CAMBIAR_ACTIVO = "UPDATE usuario SET activo = ? WHERE id_usuario = ?";

    private static final String SQL_CONTAR_ADMINISTRADORES =
            "SELECT COUNT(*) FROM usuario u JOIN rol r ON r.id_rol = u.id_rol "
            + "WHERE r.descripcion = 'Administrador' AND u.activo = TRUE";

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

    @Override
    public List<Usuario> listarTodos() {
        try {
            Connection conexion = ConexionBD.getInstancia().getConexion();
            try (PreparedStatement consulta = conexion.prepareStatement(SQL_LISTAR);
                 ResultSet filas = consulta.executeQuery()) {
                List<Usuario> usuarios = new ArrayList<>();
                while (filas.next()) {
                    usuarios.add(mapear(filas));
                }
                return usuarios;
            }
        } catch (SQLException e) {
            throw new DAOException("No se pudieron leer los usuarios", e);
        }
    }

    @Override
    public List<Rol> listarRoles() {
        try {
            Connection conexion = ConexionBD.getInstancia().getConexion();
            try (PreparedStatement consulta = conexion.prepareStatement(SQL_ROLES);
                 ResultSet filas = consulta.executeQuery()) {
                List<Rol> roles = new ArrayList<>();
                while (filas.next()) {
                    roles.add(new Rol(filas.getInt("id_rol"), filas.getString("descripcion")));
                }
                return roles;
            }
        } catch (SQLException e) {
            throw new DAOException("No se pudieron leer los roles", e);
        }
    }

    @Override
    public void actualizar(Usuario usuario) {
        try {
            Connection conexion = ConexionBD.getInstancia().getConexion();
            try (PreparedStatement sentencia = conexion.prepareStatement(SQL_ACTUALIZAR)) {
                sentencia.setString(1, usuario.getNombre());
                sentencia.setInt(2, usuario.getRol().getIdRol());
                sentencia.setInt(3, usuario.getIdUsuario());
                sentencia.executeUpdate();
            }
        } catch (SQLException e) {
            throw new DAOException("No se pudo actualizar el usuario #" + usuario.getIdUsuario(), e);
        }
    }

    @Override
    public void cambiarContrasenia(int idUsuario, String nuevoHash) {
        try {
            Connection conexion = ConexionBD.getInstancia().getConexion();
            try (PreparedStatement sentencia = conexion.prepareStatement(SQL_CAMBIAR_CONTRASENIA)) {
                sentencia.setString(1, nuevoHash);
                sentencia.setInt(2, idUsuario);
                sentencia.executeUpdate();
            }
        } catch (SQLException e) {
            throw new DAOException("No se pudo cambiar la contraseña del usuario #" + idUsuario, e);
        }
    }

    @Override
    public void cambiarActivo(int idUsuario, boolean activo) {
        try {
            Connection conexion = ConexionBD.getInstancia().getConexion();
            try (PreparedStatement sentencia = conexion.prepareStatement(SQL_CAMBIAR_ACTIVO)) {
                sentencia.setBoolean(1, activo);
                sentencia.setInt(2, idUsuario);
                sentencia.executeUpdate();
            }
        } catch (SQLException e) {
            throw new DAOException("No se pudo cambiar el estado del usuario #" + idUsuario, e);
        }
    }

    @Override
    public int contarAdministradoresActivos() {
        try {
            Connection conexion = ConexionBD.getInstancia().getConexion();
            try (PreparedStatement consulta = conexion.prepareStatement(SQL_CONTAR_ADMINISTRADORES);
                 ResultSet fila = consulta.executeQuery()) {
                return fila.next() ? fila.getInt(1) : 0;
            }
        } catch (SQLException e) {
            throw new DAOException("No se pudieron contar los administradores", e);
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
