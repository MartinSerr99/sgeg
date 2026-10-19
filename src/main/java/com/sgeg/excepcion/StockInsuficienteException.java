package com.sgeg.excepcion;

import java.math.BigDecimal;
import java.util.Locale;

/** Un insumo de la receta no alcanza para preparar lo pedido (RF19). */
public class StockInsuficienteException extends NegocioException {

    private static final long serialVersionUID = 1L;
    private static final Locale AR = Locale.forLanguageTag("es-AR");

    public StockInsuficienteException(String nombreInsumo, String unidad, BigDecimal necesario, BigDecimal disponible) {
        super(String.format(AR, "No hay stock suficiente de «%s»: se necesitan %.2f %s y hay %.2f.",
                nombreInsumo, necesario, unidad, disponible));
    }
}
