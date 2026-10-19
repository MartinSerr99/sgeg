package com.sgeg.dao;

import com.sgeg.modelo.Rol;
import com.sgeg.modelo.Usuario;
import java.util.List;

/** Contrato de acceso a datos de usuarios (RNF13: la lógica de negocio depende de esta interfaz, no de MySQL). */
public interface IUsuarioDAO {

    /** @return el usuario con su rol cargado, o null si no existe. */
    Usuario buscarPorNombreUsuario(String nombreUsuario);

    /** Inserta el usuario y deja asignado su id generado. */
    void guardar(Usuario usuario);

    /** Todos los usuarios con su rol, activos o no, ordenados por nombre. */
    List<Usuario> listarTodos();

    /** Los roles disponibles (tabla de catálogo). */
    List<Rol> listarRoles();

    /** Actualiza el nombre y el rol. No toca el usuario de acceso ni la contraseña. */
    void actualizar(Usuario usuario);

    void cambiarContrasenia(int idUsuario, String nuevoHash);

    /** Alta o baja lógica de la cuenta (RF05). */
    void cambiarActivo(int idUsuario, boolean activo);

    /** Cuántos administradores activos hay: el último nunca se puede dar de baja ni cambiar de rol. */
    int contarAdministradoresActivos();
}
