package com.sgeg.dao;

import com.sgeg.modelo.Usuario;

/** Contrato de acceso a datos de usuarios (RNF13: la lógica de negocio depende de esta interfaz, no de MySQL). */
public interface IUsuarioDAO {

    /** @return el usuario con su rol cargado, o null si no existe. */
    Usuario buscarPorNombreUsuario(String nombreUsuario);

    /** Inserta el usuario y deja asignado su id generado. */
    void guardar(Usuario usuario);
}
