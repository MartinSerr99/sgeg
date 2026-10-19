package com.sgeg.vista;

/**
 * Lo implementan las pantallas que mantienen algo en marcha (por ejemplo, un
 * temporizador de actualización). El Navegador llama a {@link #detener()} antes
 * de reemplazar la pantalla para que no queden tareas huérfanas.
 */
public interface Detenible {

    void detener();
}
