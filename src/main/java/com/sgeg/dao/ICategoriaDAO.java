package com.sgeg.dao;

import com.sgeg.modelo.Categoria;
import java.util.List;

public interface ICategoriaDAO {

    List<Categoria> listarTodas();

    /** Inserta la categoría y deja asignado su id. */
    void guardar(Categoria categoria);

    void renombrar(int idCategoria, String nombre);

    /** Cantidad de productos (disponibles o dados de baja) que usan la categoría. */
    int contarProductos(int idCategoria);

    void eliminar(int idCategoria);

    /** true si ya existe otra categoría con ese nombre. */
    boolean existeNombre(String nombre, int idExcluida);
}
