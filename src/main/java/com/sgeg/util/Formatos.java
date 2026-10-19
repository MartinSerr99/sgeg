package com.sgeg.util;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/** Formato de importes y fechas para mostrar en pantalla (Argentina). */
public final class Formatos {

    private static final Locale AR = Locale.forLanguageTag("es-AR");
    private static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private Formatos() {
    }

    public static String moneda(BigDecimal monto) {
        // NumberFormat no es seguro entre hilos, por eso se crea uno en cada llamada.
        return NumberFormat.getCurrencyInstance(AR).format(monto);
    }

    /** Número con dos decimales y separadores argentinos, sin símbolo de moneda (1.234,50). */
    public static String numero(BigDecimal valor) {
        NumberFormat formato = NumberFormat.getNumberInstance(AR);
        formato.setMinimumFractionDigits(2);
        formato.setMaximumFractionDigits(2);
        return formato.format(valor);
    }

    public static String fecha(LocalDate fecha) {
        return fecha.format(FECHA);
    }

    /**
     * Interpreta un importe escrito por una persona: acepta "6700", "6700,50", "6700.50" y "6.700,50".
     * Un único separador seguido de exactamente tres dígitos (con 1 a 3 dígitos distintos de cero al
     * inicio antes) se toma como separador de miles ("1.500" = 1500). Los separadores de miles repetidos
     * deben respetar grupos de tres dígitos ("1.000.000"). Devuelve null si el texto no es un importe
     * válido o tiene más de dos decimales.
     */
    public static BigDecimal parsearMonto(String texto) {
        if (texto == null) {
            return null;
        }
        String limpio = texto.trim().replace("$", "").replace(" ", "").replace("\u00A0", "");
        if (limpio.isEmpty() || !limpio.matches("[0-9.,]+")) {
            return null;
        }
        int ultimaComa = limpio.lastIndexOf(',');
        int ultimoPunto = limpio.lastIndexOf('.');
        String entera;
        String decimales = "";

        if (ultimaComa >= 0 && ultimoPunto >= 0) {
            // Los dos separadores: el que está más a la derecha es el decimal y el otro agrupa miles.
            int posDecimal = Math.max(ultimaComa, ultimoPunto);
            char miles = ultimaComa > ultimoPunto ? '.' : ',';
            String parteEntera = limpio.substring(0, posDecimal);
            if (!parteEntera.matches("\\d{1,3}(\\" + miles + "\\d{3})+")) {
                return null;
            }
            entera = parteEntera.replace(String.valueOf(miles), "");
            decimales = limpio.substring(posDecimal + 1);
        } else if (ultimaComa >= 0 || ultimoPunto >= 0) {
            char separador = ultimaComa >= 0 ? ',' : '.';
            int cantidad = limpio.length() - limpio.replace(String.valueOf(separador), "").length();
            int posicion = limpio.lastIndexOf(separador);
            if (cantidad > 1) {
                // Solo es válido si son separadores de miles bien agrupados.
                if (!limpio.matches("\\d{1,3}(\\" + separador + "\\d{3})+")) {
                    return null;
                }
                entera = limpio.replace(String.valueOf(separador), "");
            } else {
                int digitosAntes = posicion;
                int digitosDespues = limpio.length() - posicion - 1;
                boolean esMiles = digitosDespues == 3 && digitosAntes >= 1 && digitosAntes <= 3
                        && limpio.charAt(0) != '0';
                if (esMiles) {
                    entera = limpio.substring(0, posicion) + limpio.substring(posicion + 1);
                } else {
                    entera = limpio.substring(0, posicion);
                    decimales = limpio.substring(posicion + 1);
                }
            }
        } else {
            entera = limpio;
        }

        if (entera.isEmpty()) {
            entera = "0";
        }
        try {
            BigDecimal valor = new BigDecimal(entera + (decimales.isEmpty() ? "" : "." + decimales));
            if (valor.stripTrailingZeros().scale() > 2) {
                return null;
            }
            return valor.setScale(2, java.math.RoundingMode.UNNECESSARY);
        } catch (NumberFormatException | ArithmeticException e) {
            return null;
        }
    }
}
