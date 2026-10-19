package com.sgeg.vista;

import javafx.event.ActionEvent;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.TextArea;

/** Muestra un texto de ancho fijo (comprobante, resumen de caja) con la opción de guardarlo en un archivo. */
public final class TextoDialogo {

    private TextoDialogo() {
    }

    public static void mostrar(Node origen, String titulo, String texto, String nombreArchivo) {
        Dialog<Void> dialogo = new Dialog<>();
        if (origen != null && origen.getScene() != null) {
            dialogo.initOwner(origen.getScene().getWindow());
        }
        dialogo.setTitle("SGEG");
        dialogo.setHeaderText(titulo);
        Dialogos.aplicarEstilos(dialogo);

        TextArea contenido = new TextArea(texto);
        contenido.setEditable(false);
        contenido.getStyleClass().add("comprobante");
        contenido.setPrefColumnCount(48);
        contenido.setPrefRowCount(Math.min(Math.max(texto.split("\n").length + 1, 10), 28));
        dialogo.getDialogPane().setContent(contenido);

        ButtonType guardar = new ButtonType("Guardar como…", ButtonBar.ButtonData.LEFT);
        dialogo.getDialogPane().getButtonTypes().addAll(guardar, ButtonType.CLOSE);

        // «Guardar como…» no cierra la ventana: se puede guardar y seguir mirando el texto.
        Button botonGuardar = (Button) dialogo.getDialogPane().lookupButton(guardar);
        botonGuardar.addEventFilter(ActionEvent.ACTION, evento -> {
            evento.consume();
            if (Archivos.guardarTexto(contenido, nombreArchivo, texto)) {
                Dialogos.informar(contenido, "Guardado", "El archivo se guardó correctamente.");
            }
        });
        dialogo.showAndWait();
    }
}
