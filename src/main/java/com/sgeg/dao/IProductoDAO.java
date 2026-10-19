package com.sgeg.dao;

import com.sgeg.modelo.IngredienteReceta;
import com.sgeg.modelo.Producto;
import java.util.List;

public interface IProductoDAO {

    /** Productos habilitados en el menú, con las unidades que alcanzan según el stock de insumos (RF11, RF12). */
    List<Producto> listarMenu();

    /** Todos los productos, también los dados de baja (para el administrador). */
    List<Producto> listarTodos();

    /** Receta del producto con el stock actual de cada insumo. Vacía si el producto no consume insumos. */
    List<IngredienteReceta> obtenerReceta(int idProducto);

    /** Inserta el producto y su receta en una transacción (RF08). Deja asignado el id. */
    void guardar(Producto producto, List<IngredienteReceta> receta);

    /** Actualiza el producto y reemplaza su receta, en una transacción (RF08). */
    void actualizar(Producto producto, List<IngredienteReceta> receta);

    /** Baja lógica (false) o reactivación (true) de un producto (RF09). */
    void cambiarDisponibilidad(int idProducto, boolean disponible);

    /** true si ya existe otro producto con ese nombre. */
    boolean existeNombre(String nombre, int idExcluido);
}
