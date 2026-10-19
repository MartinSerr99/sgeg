package com.sgeg.vista;

import java.util.function.Consumer;
import java.util.function.Supplier;
import javafx.concurrent.Task;

/**
 * Ejecuta trabajo de base de datos fuera del hilo de la interfaz para que la
 * ventana no se congele. El resultado (o el error) vuelve al hilo de JavaFX.
 */
public final class Tareas {

    private Tareas() {
    }

    public static <T> void ejecutar(Supplier<T> trabajo, Consumer<T> alTerminar, Consumer<Throwable> alFallar) {
        Task<T> tarea = new Task<>() {
            @Override
            protected T call() {
                return trabajo.get();
            }
        };
        tarea.setOnSucceeded(evento -> alTerminar.accept(tarea.getValue()));
        tarea.setOnFailed(evento -> alFallar.accept(tarea.getException()));

        Thread hilo = new Thread(tarea, "sgeg-tarea");
        hilo.setDaemon(true);
        hilo.start();
    }
}
