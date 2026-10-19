package com.sgeg.vista;

import java.util.Optional;
import java.util.function.Supplier;
import javafx.event.ActionEvent;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

/**
 * Armado simple de formularios en un diálogo: filas «etiqueta + campo», un texto de error y un
 * validador. Si el validador devuelve un mensaje, el diálogo NO se cierra y se muestra el error.
 */
public final class FormularioDialogo<T> {

    private final Dialog<T> dialogo = new Dialog<>();
    private final GridPane rejilla = new GridPane();
    private final Label lblError = new Label();
    private final ButtonType aceptar;
    private int fila;
    private Supplier<String> validador = () -> null;
    private Supplier<T> resultado = () -> null;

    public FormularioDialogo(Node origen, String encabezado, String textoAceptar) {
        if (origen != null && origen.getScene() != null) {
            dialogo.initOwner(origen.getScene().getWindow());
        }
        dialogo.setTitle("SGEG");
        dialogo.setHeaderText(encabezado);
        Dialogos.aplicarEstilos(dialogo);

        rejilla.setHgap(12);
        rejilla.setVgap(10);

        lblError.getStyleClass().add("mensaje-error");
        lblError.setWrapText(true);
        lblError.setVisible(false);
        lblError.setManaged(false);

        VBox contenido = new VBox(8, rejilla, lblError);
        contenido.setPadding(new Insets(8, 0, 0, 0));
        dialogo.getDialogPane().setContent(contenido);

        aceptar = new ButtonType(textoAceptar, ButtonBar.ButtonData.OK_DONE);
        dialogo.getDialogPane().getButtonTypes().addAll(aceptar, ButtonType.CANCEL);
    }

    /** Agrega una fila con su etiqueta y devuelve el control para poder leerlo después. */
    public <N extends Node> N agregar(String etiqueta, N control) {
        Label rotulo = new Label(etiqueta);
        rotulo.getStyleClass().add("campo-etiqueta");
        rejilla.add(rotulo, 0, fila);
        rejilla.add(control, 1, fila);
        GridPane.setHgrow(control, Priority.ALWAYS);
        fila++;
        return control;
    }

    /** Agrega un bloque que ocupa todo el ancho (por ejemplo, una tabla). */
    public <N extends Node> N agregarAncho(N bloque) {
        rejilla.add(bloque, 0, fila, 2, 1);
        fila++;
        return bloque;
    }

    /** Devuelve un mensaje de error si lo ingresado no es válido, o null si está bien. */
    public void validarCon(Supplier<String> validador) {
        this.validador = validador;
    }

    /**
     * Muestra el diálogo. {@code construir} se invoca solo si se aceptó y el validador no encontró errores.
     * @return el resultado, o vacío si se canceló.
     */
    public Optional<T> mostrar(Supplier<T> construir) {
        this.resultado = construir;
        Button botonAceptar = (Button) dialogo.getDialogPane().lookupButton(aceptar);
        botonAceptar.addEventFilter(ActionEvent.ACTION, evento -> {
            String error = validador.get();
            if (error != null) {
                lblError.setText(error);
                lblError.setVisible(true);
                lblError.setManaged(true);
                evento.consume();
            }
        });
        dialogo.setResultConverter(boton -> boton == aceptar ? resultado.get() : null);
        return dialogo.showAndWait();
    }
}
