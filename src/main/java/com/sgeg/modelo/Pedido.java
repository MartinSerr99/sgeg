package com.sgeg.modelo;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

/** Pedido de una mesa o para llevar. Tabla: pedido (+ detalle_pedido). */
public class Pedido {

    private int idPedido;
    private Mesa mesa;
    private Cliente cliente;
    private Usuario usuario;
    private EstadoPedido estado = EstadoPedido.PENDIENTE;
    private String horaCreacion;
    private long minutosEspera;
    private final List<DetallePedido> detalles = new ArrayList<>();

    public Pedido() {
    }

    /** @param mesa mesa del pedido, o null si es para llevar. */
    public Pedido(Mesa mesa) {
        this.mesa = mesa;
    }

    /** Suma la cantidad a la línea existente del producto, o crea una línea nueva. */
    public void agregarProducto(Producto producto, int cantidad) {
        if (cantidad < 1) {
            throw new IllegalArgumentException("La cantidad debe ser al menos 1");
        }
        for (DetallePedido detalle : detalles) {
            if (detalle.getProducto().getIdProducto() == producto.getIdProducto()) {
                detalle.setCantidad(detalle.getCantidad() + cantidad);
                return;
            }
        }
        detalles.add(new DetallePedido(producto, cantidad, producto.getPrecio()));
    }

    /** Agrega una línea ya armada (se usa al cargar pedidos desde la base). */
    public void agregarDetalle(DetallePedido detalle) {
        detalles.add(detalle);
    }

    /** Resta una unidad del producto; si era la última, elimina la línea. */
    public void quitarUnidad(int idProducto) {
        for (Iterator<DetallePedido> it = detalles.iterator(); it.hasNext();) {
            DetallePedido detalle = it.next();
            if (detalle.getProducto().getIdProducto() == idProducto) {
                if (detalle.getCantidad() > 1) {
                    detalle.setCantidad(detalle.getCantidad() - 1);
                } else {
                    it.remove();
                }
                return;
            }
        }
    }

    public void quitarProducto(int idProducto) {
        detalles.removeIf(detalle -> detalle.getProducto().getIdProducto() == idProducto);
    }

    public int cantidadDe(int idProducto) {
        for (DetallePedido detalle : detalles) {
            if (detalle.getProducto().getIdProducto() == idProducto) {
                return detalle.getCantidad();
            }
        }
        return 0;
    }

    /** RF16: total parcial calculado a partir de las líneas. */
    public BigDecimal calcularTotal() {
        BigDecimal total = BigDecimal.ZERO;
        for (DetallePedido detalle : detalles) {
            total = total.add(detalle.getSubtotal());
        }
        return total;
    }

    public boolean estaVacio() {
        return detalles.isEmpty();
    }

    public boolean esParaLlevar() {
        return mesa == null;
    }

    public List<DetallePedido> getDetalles() {
        return Collections.unmodifiableList(detalles);
    }

    public int getIdPedido() {
        return idPedido;
    }

    public void setIdPedido(int idPedido) {
        this.idPedido = idPedido;
    }

    public Mesa getMesa() {
        return mesa;
    }

    public void setMesa(Mesa mesa) {
        this.mesa = mesa;
    }

    public Cliente getCliente() {
        return cliente;
    }

    public void setCliente(Cliente cliente) {
        this.cliente = cliente;
    }

    public Usuario getUsuario() {
        return usuario;
    }

    public void setUsuario(Usuario usuario) {
        this.usuario = usuario;
    }

    public EstadoPedido getEstado() {
        return estado;
    }

    public void setEstado(EstadoPedido estado) {
        this.estado = estado;
    }

    /** Hora de creación (HH:mm) según el reloj del servidor de base de datos. Solo para pedidos leídos de la base. */
    public String getHoraCreacion() {
        return horaCreacion;
    }

    public void setHoraCreacion(String horaCreacion) {
        this.horaCreacion = horaCreacion;
    }

    /** Minutos transcurridos desde la creación, según el reloj del servidor de base de datos. */
    public long getMinutosEspera() {
        return minutosEspera;
    }

    public void setMinutosEspera(long minutosEspera) {
        this.minutosEspera = minutosEspera;
    }
}
