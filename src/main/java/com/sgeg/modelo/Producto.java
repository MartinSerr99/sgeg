package com.sgeg.modelo;

import java.math.BigDecimal;

/** Producto del menú. Tabla: producto. */
public class Producto {

    /** Valor de unidadesDisponibles para productos sin receta (no dependen del stock de insumos). */
    public static final int SIN_LIMITE = Integer.MAX_VALUE;

    private int idProducto;
    private String nombre;
    private BigDecimal precio;
    private boolean disponible = true;
    private Categoria categoria;
    private int unidadesDisponibles = SIN_LIMITE;

    public Producto() {
    }

    public int getIdProducto() {
        return idProducto;
    }

    public void setIdProducto(int idProducto) {
        this.idProducto = idProducto;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public BigDecimal getPrecio() {
        return precio;
    }

    public void setPrecio(BigDecimal precio) {
        this.precio = precio;
    }

    public boolean isDisponible() {
        return disponible;
    }

    public void setDisponible(boolean disponible) {
        this.disponible = disponible;
    }

    public Categoria getCategoria() {
        return categoria;
    }

    public void setCategoria(Categoria categoria) {
        this.categoria = categoria;
    }

    /**
     * Cuántas unidades se pueden preparar con el stock actual de insumos, mirando cada
     * producto por separado (RF12). Es informativo: la verificación real, que contempla
     * insumos compartidos entre productos, la hace PedidoService.
     */
    public int getUnidadesDisponibles() {
        return unidadesDisponibles;
    }

    public void setUnidadesDisponibles(int unidadesDisponibles) {
        this.unidadesDisponibles = unidadesDisponibles;
    }

    public boolean tieneStockIlimitado() {
        return unidadesDisponibles == SIN_LIMITE;
    }

    public boolean hayStock() {
        return unidadesDisponibles > 0;
    }

    @Override
    public String toString() {
        return nombre;
    }
}
