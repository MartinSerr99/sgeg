package com.sgeg.modelo;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Venta ya cobrada, con todo lo necesario para armar el comprobante (CU13).
 * Tablas: venta + pedido + detalle_pedido + pago. Se carga desde la base una vez registrada.
 */
public class Venta {

    private int idVenta;
    private int idPedido;
    /** null si el pedido era para llevar. */
    private Integer numeroMesa;
    private String mozo;
    /** null si no había cliente registrado. */
    private String cliente;
    /** Fecha y hora (dd/MM/yyyy HH:mm) según el reloj del servidor de base de datos. */
    private String fechaHora;
    private BigDecimal total = BigDecimal.ZERO;
    private final List<DetallePedido> detalles = new ArrayList<>();
    private final List<Pago> pagos = new ArrayList<>();

    public int getIdVenta() {
        return idVenta;
    }

    public void setIdVenta(int idVenta) {
        this.idVenta = idVenta;
    }

    public int getIdPedido() {
        return idPedido;
    }

    public void setIdPedido(int idPedido) {
        this.idPedido = idPedido;
    }

    public Integer getNumeroMesa() {
        return numeroMesa;
    }

    public void setNumeroMesa(Integer numeroMesa) {
        this.numeroMesa = numeroMesa;
    }

    public boolean esParaLlevar() {
        return numeroMesa == null;
    }

    public String getMozo() {
        return mozo;
    }

    public void setMozo(String mozo) {
        this.mozo = mozo;
    }

    public String getCliente() {
        return cliente;
    }

    public void setCliente(String cliente) {
        this.cliente = cliente;
    }

    public String getFechaHora() {
        return fechaHora;
    }

    public void setFechaHora(String fechaHora) {
        this.fechaHora = fechaHora;
    }

    public BigDecimal getTotal() {
        return total;
    }

    public void setTotal(BigDecimal total) {
        this.total = total;
    }

    public void agregarDetalle(DetallePedido detalle) {
        detalles.add(detalle);
    }

    public void agregarPago(Pago pago) {
        pagos.add(pago);
    }

    public List<DetallePedido> getDetalles() {
        return Collections.unmodifiableList(detalles);
    }

    public List<Pago> getPagos() {
        return Collections.unmodifiableList(pagos);
    }
}
