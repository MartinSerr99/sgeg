package com.sgeg.servicio;

/**
 * Agrupa los servicios de la aplicación para pasarlos de una vez al Navegador. Cada pantalla
 * recibe solo el servicio que necesita; esto evita una lista larga de parámetros en Main y Navegador.
 */
public record Servicios(
        SesionService sesion,
        PedidoService pedidos,
        VentaService ventas,
        ReporteService reportes,
        ProductoService productos,
        InsumoService insumos,
        UsuarioService usuarios) {
}
