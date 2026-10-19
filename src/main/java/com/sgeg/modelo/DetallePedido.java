package com.sgeg.modelo;

import java.math.BigDecimal;
import java.math.RoundingMode;

/** Línea de un pedido: producto, cantidad y el precio vigente al momento de pedirlo. Tabla: detalle_pedido. */
public class DetallePedido {

    private int idDetalle;
    private Producto producto;
    private int cantidad;
    private BigDecimal precioUnitario;

    public DetallePedido(Producto producto, int cantidad, BigDecimal precioUnitario) {
        this.producto = producto;
        this.cantidad = cantidad;
        this.precioUnitario = precioUnitario;
    }

    public int getIdDetalle() {
        return idDetalle;
    }

    public void setIdDetalle(int idDetalle) {
        this.idDetalle = idDetalle;
    }

    public Producto getProducto() {
        return producto;
    }

    public int getCantidad() {
        return cantidad;
    }

    public void setCantidad(int cantidad) {
        this.cantidad = cantidad;
    }

    public BigDecimal getPrecioUnitario() {
        return precioUnitario;
    }

    public BigDecimal getSubtotal() {
        return precioUnitario.multiply(BigDecimal.valueOf(cantidad)).setScale(2, RoundingMode.HALF_UP);
    }
}
