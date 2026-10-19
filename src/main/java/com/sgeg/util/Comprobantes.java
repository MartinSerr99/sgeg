package com.sgeg.util;

import com.sgeg.modelo.DetallePedido;
import com.sgeg.modelo.Pago;
import com.sgeg.modelo.ProductoVendido;
import com.sgeg.modelo.ResumenPeriodo;
import com.sgeg.modelo.TotalFormaPago;
import com.sgeg.modelo.Venta;
import java.time.LocalDate;
import java.util.List;

/** Arma los textos imprimibles: comprobante de venta (CU13) y resumen de cierre de caja (CU15). */
public final class Comprobantes {

    private static final int ANCHO = 46;
    private static final String RAYA = "-".repeat(ANCHO);

    private Comprobantes() {
    }

    /** Comprobante de una venta. No es una factura fiscal: lo aclara el propio texto. */
    public static String deVenta(Venta venta) {
        StringBuilder texto = new StringBuilder();
        linea(texto, centrar("SGEG - GESTIÓN GASTRONÓMICA"));
        linea(texto, centrar("COMPROBANTE DE VENTA"));
        linea(texto, centrar("(documento no válido como factura)"));
        linea(texto, RAYA);
        linea(texto, columnas("Comprobante N° " + String.format("%06d", venta.getIdVenta()), venta.getFechaHora()));
        linea(texto, columnas("Pedido N° " + venta.getIdPedido(),
                venta.esParaLlevar() ? "Para llevar" : "Mesa " + venta.getNumeroMesa()));
        linea(texto, "Atendió: " + venta.getMozo());
        if (venta.getCliente() != null) {
            linea(texto, "Cliente: " + venta.getCliente());
        }
        linea(texto, RAYA);
        linea(texto, columnas("Cant. Descripción", "Importe"));
        for (DetallePedido detalle : venta.getDetalles()) {
            String izquierda = String.format("%4d  %s", detalle.getCantidad(), detalle.getProducto().getNombre());
            linea(texto, columnas(izquierda, Formatos.numero(detalle.getSubtotal())));
            if (detalle.getCantidad() > 1) {
                linea(texto, "      (" + detalle.getCantidad() + " x " + Formatos.numero(detalle.getPrecioUnitario()) + ")");
            }
        }
        linea(texto, RAYA);
        linea(texto, columnas("TOTAL", Formatos.moneda(venta.getTotal())));
        linea(texto, "");
        linea(texto, "Forma de pago:");
        for (Pago pago : venta.getPagos()) {
            linea(texto, columnas("  " + pago.getFormaPago().getDescripcion(), Formatos.moneda(pago.getMonto())));
        }
        linea(texto, RAYA);
        linea(texto, centrar("¡Gracias por su visita!"));
        return texto.toString();
    }

    /** Resumen de ventas de un día o un período, para guardar. */
    public static String resumenDeCaja(String titulo, LocalDate desde, LocalDate hasta, ResumenPeriodo resumen) {
        StringBuilder texto = new StringBuilder();
        linea(texto, centrar("SGEG - " + titulo.toUpperCase()));
        linea(texto, centrar(desde.equals(hasta)
                ? "Día " + Formatos.fecha(desde)
                : "Del " + Formatos.fecha(desde) + " al " + Formatos.fecha(hasta)));
        linea(texto, RAYA);
        linea(texto, columnas("Ventas realizadas", String.valueOf(resumen.cantidadVentas())));
        linea(texto, columnas("Total vendido", Formatos.moneda(resumen.totalVendido())));
        linea(texto, RAYA);
        linea(texto, "Cobrado por forma de pago:");
        if (resumen.totalesPorForma().isEmpty()) {
            linea(texto, "  (sin cobros en el período)");
        }
        for (TotalFormaPago total : resumen.totalesPorForma()) {
            linea(texto, columnas("  " + total.formaPago() + " (" + total.cantidadPagos() + ")",
                    Formatos.moneda(total.total())));
        }
        return texto.toString();
    }

    /** Productos más vendidos, para guardar junto con el reporte. */
    public static String productosVendidos(List<ProductoVendido> productos) {
        StringBuilder texto = new StringBuilder();
        linea(texto, columnas("Producto", "Unidades"));
        for (ProductoVendido producto : productos) {
            linea(texto, columnas(producto.producto(), String.valueOf(producto.unidades())));
        }
        return texto.toString();
    }

    private static void linea(StringBuilder texto, String contenido) {
        texto.append(contenido).append(System.lineSeparator());
    }

    private static String centrar(String contenido) {
        int espacios = Math.max((ANCHO - contenido.length()) / 2, 0);
        return " ".repeat(espacios) + contenido;
    }

    /** Texto a la izquierda y a la derecha, con relleno en el medio hasta completar el ancho. */
    private static String columnas(String izquierda, String derecha) {
        int relleno = ANCHO - izquierda.length() - derecha.length();
        if (relleno < 1) {
            int maximo = Math.max(ANCHO - derecha.length() - 1, 1);
            izquierda = izquierda.substring(0, Math.min(izquierda.length(), maximo));
            relleno = Math.max(ANCHO - izquierda.length() - derecha.length(), 1);
        }
        return izquierda + " ".repeat(relleno) + derecha;
    }
}
