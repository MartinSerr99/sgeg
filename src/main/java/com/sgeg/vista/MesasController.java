package com.sgeg.vista;

import com.sgeg.modelo.EstadoMesa;
import com.sgeg.modelo.Mesa;
import com.sgeg.modelo.Usuario;
import com.sgeg.servicio.PedidoService;
import java.util.List;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.Label;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.VBox;

/**
 * Plano de mesas (paso previo de CU06). Solo una mesa libre permite iniciar un
 * pedido; las ocupadas y reservadas se muestran para tener el panorama del salón.
 */
public class MesasController {

    @FXML private FlowPane panelMesas;
    @FXML private Label lblEstado;
    @FXML private Button btnActualizar;

    private Navegador navegador;
    private PedidoService pedidoService;
    private Usuario usuario;

    /** Lo invoca el Navegador después de cargar el FXML. */
    public void inicializar(Navegador navegador, PedidoService pedidoService, Usuario usuario) {
        this.navegador = navegador;
        this.pedidoService = pedidoService;
        this.usuario = usuario;
        actualizar();
    }

    @FXML
    private void actualizar() {
        btnActualizar.setDisable(true);
        lblEstado.setText("Cargando mesas…");
        Tareas.ejecutar(pedidoService::listarMesas, this::mostrarMesas, causa -> {
            btnActualizar.setDisable(false);
            lblEstado.setText("No se pudieron cargar las mesas.");
            Dialogos.error(panelMesas, causa);
        });
    }

    @FXML
    private void pedidoParaLlevar() {
        navegador.mostrarNuevoPedido(usuario, null);
    }

    @FXML
    private void volver() {
        navegador.mostrarPrincipal(usuario);
    }

    private void mostrarMesas(List<Mesa> mesas) {
        btnActualizar.setDisable(false);
        panelMesas.getChildren().clear();
        long libres = mesas.stream().filter(Mesa::estaLibre).count();
        lblEstado.setText(libres + " de " + mesas.size() + " mesas libres. Elegí una mesa libre para tomar el pedido.");
        for (Mesa mesa : mesas) {
            panelMesas.getChildren().add(crearBotonMesa(mesa));
        }
    }

    private Button crearBotonMesa(Mesa mesa) {
        Label numero = new Label("Mesa " + mesa.getNumero());
        numero.getStyleClass().add("mesa-numero");
        Label estado = new Label(mesa.getEstado().getDescripcion());
        estado.getStyleClass().add("mesa-estado");

        VBox contenido = new VBox(4, numero, estado);
        contenido.setAlignment(Pos.CENTER);

        Button boton = new Button();
        boton.setGraphic(contenido);
        boton.setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
        boton.getStyleClass().addAll("mesa", claseDeEstado(mesa.getEstado()));
        boton.setOnAction(evento -> elegir(mesa));
        return boton;
    }

    private void elegir(Mesa mesa) {
        if (mesa.estaLibre()) {
            navegador.mostrarNuevoPedido(usuario, mesa);
            return;
        }
        String motivo = mesa.getEstado() == EstadoMesa.RESERVADA
                ? "La mesa " + mesa.getNumero() + " está reservada."
                : "La mesa " + mesa.getNumero() + " ya tiene un pedido en curso. Podés seguirlo en «Pedidos en seguimiento».";
        Dialogos.informar(panelMesas, "Mesa no disponible", motivo);
    }

    private static String claseDeEstado(EstadoMesa estado) {
        return switch (estado) {
            case LIBRE -> "mesa-libre";
            case OCUPADA -> "mesa-ocupada";
            case RESERVADA -> "mesa-reservada";
        };
    }
}
