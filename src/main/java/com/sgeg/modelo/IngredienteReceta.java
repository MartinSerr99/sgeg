package com.sgeg.modelo;

import java.math.BigDecimal;

/** Una línea de la receta de un producto, con el stock actual del insumo. Tablas: producto_insumo + insumo. */
public class IngredienteReceta {

    private final int idInsumo;
    private final String nombreInsumo;
    private final String unidadMedida;
    private final BigDecimal cantidadNecesaria;
    private final BigDecimal stockActual;

    public IngredienteReceta(int idInsumo, String nombreInsumo, String unidadMedida,
                             BigDecimal cantidadNecesaria, BigDecimal stockActual) {
        this.idInsumo = idInsumo;
        this.nombreInsumo = nombreInsumo;
        this.unidadMedida = unidadMedida;
        this.cantidadNecesaria = cantidadNecesaria;
        this.stockActual = stockActual;
    }

    public int getIdInsumo() {
        return idInsumo;
    }

    public String getNombreInsumo() {
        return nombreInsumo;
    }

    public String getUnidadMedida() {
        return unidadMedida;
    }

    /** Cantidad de insumo que consume UNA unidad del producto. */
    public BigDecimal getCantidadNecesaria() {
        return cantidadNecesaria;
    }

    public BigDecimal getStockActual() {
        return stockActual;
    }
}
