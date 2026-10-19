package com.sgeg.modelo;

import java.math.BigDecimal;

/** Insumo (materia prima) que se descuenta del stock al confirmar pedidos. Tabla: insumo. */
public class Insumo {

    private int idInsumo;
    private String nombre;
    private BigDecimal stockActual = BigDecimal.ZERO;
    private BigDecimal stockMinimo = BigDecimal.ZERO;
    private String unidadMedida;

    public Insumo() {
    }

    public Insumo(int idInsumo, String nombre, BigDecimal stockActual, BigDecimal stockMinimo, String unidadMedida) {
        this.idInsumo = idInsumo;
        this.nombre = nombre;
        this.stockActual = stockActual;
        this.stockMinimo = stockMinimo;
        this.unidadMedida = unidadMedida;
    }

    public int getIdInsumo() {
        return idInsumo;
    }

    public void setIdInsumo(int idInsumo) {
        this.idInsumo = idInsumo;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public BigDecimal getStockActual() {
        return stockActual;
    }

    public void setStockActual(BigDecimal stockActual) {
        this.stockActual = stockActual;
    }

    public BigDecimal getStockMinimo() {
        return stockMinimo;
    }

    public void setStockMinimo(BigDecimal stockMinimo) {
        this.stockMinimo = stockMinimo;
    }

    public String getUnidadMedida() {
        return unidadMedida;
    }

    public void setUnidadMedida(String unidadMedida) {
        this.unidadMedida = unidadMedida;
    }

    public boolean estaAgotado() {
        return stockActual.signum() <= 0;
    }

    /** RF24: alerta cuando el stock llega al mínimo o queda por debajo. */
    public boolean estaBajoMinimo() {
        return stockActual.compareTo(stockMinimo) <= 0;
    }

    /** Estado para mostrar en las listas. */
    public String getEstado() {
        if (estaAgotado()) {
            return "Agotado";
        }
        return estaBajoMinimo() ? "Bajo el mínimo" : "Normal";
    }

    @Override
    public String toString() {
        return nombre + " (" + unidadMedida + ")";
    }
}
