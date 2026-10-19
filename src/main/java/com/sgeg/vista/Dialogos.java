package com.sgeg.vista;

import com.sgeg.excepcion.NegocioException;
import java.net.URL;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;

/** Mensajes al usuario con un formato uniforme. */
public final class Dialogos {

    private static final Logger LOG = Logger.getLogger(Dialogos.class.getName());

    private Dialogos() {
    }

    public static void informar(Node origen, String titulo, String mensaje) {
        mostrar(Alert.AlertType.INFORMATION, origen, titulo, mensaje);
    }

    /**
     * Muestra un error. Si es una regla de negocio, se muestra su mensaje tal cual
     * (ya está redactado para el usuario); cualquier otra falla se registra en el
     * log y se muestra un texto genérico, sin detalles técnicos.
     */
    public static void error(Node origen, Throwable causa) {
        if (causa instanceof NegocioException) {
            mostrar(Alert.AlertType.WARNING, origen, "No se pudo completar la operación", causa.getMessage());
        } else {
            LOG.log(Level.SEVERE, "Error inesperado", causa);
            mostrar(Alert.AlertType.ERROR, origen, "Error",
                    "Ocurrió un problema al consultar o guardar los datos. Verificá que MySQL esté en "
                            + "ejecución e intentá de nuevo.");
        }
    }

    public static boolean confirmar(Node origen, String titulo, String mensaje) {
        Alert alerta = crear(Alert.AlertType.CONFIRMATION, origen, titulo, mensaje);
        Optional<ButtonType> respuesta = alerta.showAndWait();
        return respuesta.isPresent() && respuesta.get() == ButtonType.OK;
    }

    /** Aplica la hoja de estilos de la aplicación a un diálogo propio (los de Alert usan el estilo por defecto). */
    public static void aplicarEstilos(Dialog<?> dialogo) {
        URL estilos = Dialogos.class.getResource("/css/estilos.css");
        if (estilos != null) {
            dialogo.getDialogPane().getStylesheets().add(estilos.toExternalForm());
        }
    }

    private static void mostrar(Alert.AlertType tipo, Node origen, String titulo, String mensaje) {
        crear(tipo, origen, titulo, mensaje).showAndWait();
    }

    private static Alert crear(Alert.AlertType tipo, Node origen, String titulo, String mensaje) {
        Alert alerta = new Alert(tipo);
        if (origen != null && origen.getScene() != null) {
            alerta.initOwner(origen.getScene().getWindow());
        }
        alerta.setTitle("SGEG");
        alerta.setHeaderText(titulo);
        alerta.setContentText(mensaje);
        return alerta;
    }
}
