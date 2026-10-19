package com.sgeg.dao;

import com.sgeg.modelo.IngredienteReceta;
import com.sgeg.modelo.Producto;
import java.util.List;

public interface IProductoDAO {

    /** Productos habilitados en el menú, con las unidades que alcanzan según el stock de insumos (RF11, RF12). */
    List<Producto> listarMenu();

    /** Receta del producto con el stock actual de cada insumo. Vacía si el producto no consume insumos. */
    List<IngredienteReceta> obtenerReceta(int idProducto);
}
