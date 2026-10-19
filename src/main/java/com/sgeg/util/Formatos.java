package com.sgeg.util;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.Locale;

/** Formato de importes para mostrar en pantalla (Argentina). */
public final class Formatos {

    private static final Locale AR = Locale.forLanguageTag("es-AR");

    private Formatos() {
    }

    public static String moneda(BigDecimal monto) {
        // NumberFormat no es seguro entre hilos, por eso se crea uno en cada llamada.
        return NumberFormat.getCurrencyInstance(AR).format(monto);
    }
}
