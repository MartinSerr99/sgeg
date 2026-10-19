package com.sgeg.dao;

import com.sgeg.modelo.Insumo;
import java.math.BigDecimal;
import java.util.List;

public interface IInsumoDAO {

    /** Todos los insumos con su stock actual, ordenados por nombre (RF21). */
    List<Insumo> listarTodos();

    /** Inserta el insumo (con su stock inicial) y deja asignado el id. */
    void guardar(Insumo insumo);

    /** Actualiza nombre, unidad y stock mínimo. El stock actual NO se toca: cambia solo con ingresos y pedidos. */
    void actualizar(Insumo insumo);

    /** Suma la cantidad al stock en una única sentencia (segura aunque haya pedidos descontando a la vez). */
    void sumarStock(int idInsumo, BigDecimal cantidad);

    /** true si ya existe otro insumo con ese nombre. */
    boolean existeNombre(String nombre, int idExcluido);

    /** Cantidad de insumos en o por debajo de su stock mínimo (RF24). */
    int contarBajoMinimo();
}
