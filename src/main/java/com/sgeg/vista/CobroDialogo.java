package com.sgeg.vista;

import com.sgeg.modelo.FormaPago;
import com.sgeg.modelo.Pago;
import com.sgeg.util.Formatos;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

/**
 * Pide cómo se paga un pedido (CU14). Cada línea es una forma de pago con su importe; se pueden
 * agregar varias (por ejemplo, parte en efectivo y parte con tarjeta). Solo deja cobrar cuando
 * los importes suman exactamente el total.
 */
public final class CobroDialogo {

    private CobroDialogo() {
    }

    /** Una línea del diálogo: forma de pago + importe + botón para quitarla. */
    private static final class Linea {
        final ComboBox<FormaPago> forma = new ComboBox<>();
        final TextField monto = new TextField();
        final Button quitar = new Button("Quitar");
        final HBox caja = new HBox(10, forma, monto, quitar);

        Linea(List<FormaPago> formas) {
            forma.getItems().setAll(formas);
            forma.setPrefWidth(190);
            monto.setPrefWidth(140);
            monto.setAlignment(Pos.CENTER_RIGHT);
            quitar.getStyleClass().add("boton-claro");
            caja.setAlignment(Pos.CENTER_LEFT);
        }

        BigDecimal montoLeido() {
            return Formatos.parsearMonto(monto.getText());
        }
    }

    /** @return los pagos ingresados, o vacío si se canceló. */
    public static Optional<List<Pago>> pedir(Node origen, int numeroPedido, BigDecimal total, List<FormaPago> formas) {
        Dialog<List<Pago>> dialogo = new Dialog<>();
        if (origen != null && origen.getScene() != null) {
            dialogo.initOwner(origen.getScene().getWindow());
        }
        dialogo.setTitle("SGEG");
        dialogo.setHeaderText("Cobrar el pedido #" + numeroPedido + " · Total " + Formatos.moneda(total));
        Dialogos.aplicarEstilos(dialogo);

        ButtonType cobrar = new ButtonType("Cobrar", ButtonBar.ButtonData.OK_DONE);
        dialogo.getDialogPane().getButtonTypes().addAll(cobrar, ButtonType.CANCEL);
        Button botonCobrar = (Button) dialogo.getDialogPane().lookupButton(cobrar);

        List<Linea> lineas = new ArrayList<>();
        VBox contenedorLineas = new VBox(8);
        Label estado = new Label();
        Button agregar = new Button("+ Agregar otra forma de pago");
        agregar.getStyleClass().add("boton-claro");

        Runnable revisar = () -> {
            BigDecimal suma = BigDecimal.ZERO;
            boolean lineasValidas = true;
            Set<Integer> usadas = new HashSet<>();
            for (Linea linea : lineas) {
                BigDecimal monto = linea.montoLeido();
                if (monto == null || monto.signum() <= 0 || linea.forma.getValue() == null
                        || !usadas.add(linea.forma.getValue().getIdFormaPago())) {
                    lineasValidas = false;
                } else {
                    suma = suma.add(monto);
                }
            }
            BigDecimal diferencia = total.subtract(suma);
            if (!lineasValidas) {
                estado.setText("Elegí una forma de pago distinta en cada línea e ingresá importes mayores a cero.");
            } else if (diferencia.signum() > 0) {
                estado.setText("Falta cobrar " + Formatos.moneda(diferencia));
            } else if (diferencia.signum() < 0) {
                estado.setText("Los importes superan el total en " + Formatos.moneda(diferencia.negate()));
            } else {
                estado.setText("Importes completos.");
            }
            botonCobrar.setDisable(!(lineasValidas && diferencia.signum() == 0));
            agregar.setDisable(lineas.size() >= formas.size());
            for (Linea linea : lineas) {
                linea.quitar.setDisable(lineas.size() == 1);
            }
        };

        Runnable[] nuevaLinea = new Runnable[1];
        nuevaLinea[0] = () -> {
            Linea linea = new Linea(formas);
            BigDecimal restante = total;
            for (Linea otra : lineas) {
                BigDecimal monto = otra.montoLeido();
                if (monto != null) {
                    restante = restante.subtract(monto);
                }
            }
            linea.monto.setText(restante.signum() > 0 ? Formatos.numero(restante).replace(".", "") : "");
            // Primera forma sin usar, para que no haya que elegirla siempre a mano.
            for (FormaPago candidata : formas) {
                boolean libre = lineas.stream().noneMatch(otra -> otra.forma.getValue() != null
                        && otra.forma.getValue().getIdFormaPago() == candidata.getIdFormaPago());
                if (libre) {
                    linea.forma.setValue(candidata);
                    break;
                }
            }
            linea.forma.valueProperty().addListener((obs, anterior, nueva) -> revisar.run());
            linea.monto.textProperty().addListener((obs, anterior, nuevo) -> revisar.run());
            linea.quitar.setOnAction(evento -> {
                lineas.remove(linea);
                contenedorLineas.getChildren().remove(linea.caja);
                revisar.run();
            });
            lineas.add(linea);
            contenedorLineas.getChildren().add(linea.caja);
            revisar.run();
        };
        agregar.setOnAction(evento -> nuevaLinea[0].run());
        nuevaLinea[0].run();
        // La primera línea arranca con el total completo.
        lineas.get(0).monto.setText(Formatos.numero(total).replace(".", ""));

        Label ayuda = new Label("Importes con coma para los centavos (ejemplo: 6700,50).");
        ayuda.getStyleClass().add("seccion-ayuda");
        VBox contenido = new VBox(12, contenedorLineas, agregar, estado, ayuda);
        contenido.setPadding(new Insets(8, 0, 0, 0));
        dialogo.getDialogPane().setContent(contenido);

        dialogo.setResultConverter(boton -> {
            if (boton != cobrar) {
                return null;
            }
            List<Pago> pagos = new ArrayList<>();
            for (Linea linea : lineas) {
                pagos.add(new Pago(linea.forma.getValue(), linea.montoLeido()));
            }
            return pagos;
        });
        return dialogo.showAndWait();
    }
}
