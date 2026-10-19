package com.sgeg.vista;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import javafx.scene.Node;
import javafx.stage.FileChooser;

/** Guardado de textos (comprobantes y resúmenes) en un archivo elegido por el usuario. */
public final class Archivos {

    private Archivos() {
    }

    /** @return true si el usuario eligió un archivo y se guardó; false si canceló. */
    public static boolean guardarTexto(Node origen, String nombreSugerido, String contenido) {
        FileChooser selector = new FileChooser();
        selector.setTitle("Guardar como");
        selector.setInitialFileName(nombreSugerido);
        selector.getExtensionFilters().add(new FileChooser.ExtensionFilter("Texto (*.txt)", "*.txt"));
        File destino = selector.showSaveDialog(origen != null && origen.getScene() != null ? origen.getScene().getWindow() : null);
        if (destino == null) {
            return false;
        }
        try {
            Files.writeString(destino.toPath(), contenido, StandardCharsets.UTF_8);
            return true;
        } catch (IOException e) {
            Dialogos.error(origen, e);
            return false;
        }
    }
}
