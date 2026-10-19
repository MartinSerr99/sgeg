package com.sgeg.vista;

import com.sgeg.modelo.DetallePedido;
import com.sgeg.modelo.EstadoPedido;
import com.sgeg.modelo.Pedido;
import com.sgeg.modelo.Rol;
import com.sgeg.modelo.Usuario;
import com.sgeg.servicio.PedidoService;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

/**
 * Seguimiento de pedidos en curso (CU08 y CU09). La cocina ve los pedidos del más
 * antiguo al más nuevo (RF18) y los hace avanzar hasta "Listo"; el mozo ve lo mismo
 * y marca "Entregado" cuando lleva el pedido al cliente. Se actualiza sola.
 */
public class SeguimientoController implements Detenible {

    private static final int SEGUNDOS_ENTRE_ACTUALIZACIONES = 10;
    private static final DateTimeFormatter HORA = DateTimeFormatter.ofPattern("HH:mm:ss");

    @FXML private Label lblTitulo;
    @FXML private Label lblActualizado;
    @FXML private Label lblVacio;
    @FXML private Button btnActualizar;
    @FXML private FlowPane panelPedidos;

    private Navegador navegador;
    private PedidoService pedidoService;
    private Usuario usuario;
    private Timeline temporizador;

    /** Se incrementa en cada lectura: una respuesta vieja que llega tarde se descarta. */
    private int generacion;
    private boolean detenido;

    /** Lo invoca el Navegador después de cargar el FXML. */
    public void inicializar(Navegador navegador, PedidoService pedidoService, Usuario usuario) {
        this.navegador = navegador;
        this.pedidoService = pedidoService;
        this.usuario = usuario;

        lblTitulo.setText(usuario.tieneRol(Rol.COCINERO) ? "Pedidos en cocina" : "Seguimiento de pedidos");

        temporizador = new Timeline(new KeyFrame(Duration.seconds(SEGUNDOS_ENTRE_ACTUALIZACIONES), evento -> cargar()));
        temporizador.setCycleCount(Animation.INDEFINITE);
        temporizador.play();

        cargar();
    }

    @Override
    public void detener() {
        detenido = true;
        if (temporizador != null) {
            temporizador.stop();
        }
    }

    @FXML
    private void actualizar() {
        cargar();
    }

    @FXML
    private void volver() {
        navegador.mostrarPrincipal(usuario);
    }

    private void cargar() {
        int miGeneracion = ++generacion;
        btnActualizar.setDisable(true);
        Tareas.ejecutar(
                pedidoService::listarPedidosActivos,
                pedidos -> {
                    if (detenido || miGeneracion != generacion) {
                        return;
                    }
                    btnActualizar.setDisable(false);
                    mostrar(pedidos);
                },
                causa -> {
                    if (detenido || miGeneracion != generacion) {
                        return;
                    }
                    btnActualizar.setDisable(false);
                    lblActualizado.setText("No se pudo actualizar");
                    Dialogos.error(panelPedidos, causa);
                });
    }

    private void mostrar(List<Pedido> pedidos) {
        panelPedidos.getChildren().clear();
        for (Pedido pedido : pedidos) {
            panelPedidos.getChildren().add(crearTarjeta(pedido));
        }
        boolean vacio = pedidos.isEmpty();
        lblVacio.setVisible(vacio);
        lblVacio.setManaged(vacio);
        lblActualizado.setText("Actualizado " + LocalTime.now().format(HORA));
    }

    private VBox crearTarjeta(Pedido pedido) {
        Label numero = new Label("Pedido #" + pedido.getIdPedido());
        numero.getStyleClass().add("comanda-numero");
        Label estado = new Label(pedido.getEstado().getDescripcion());
        estado.getStyleClass().addAll("estado-insignia", claseDeEstado(pedido.getEstado()));
        Region separador = new Region();
        HBox.setHgrow(separador, Priority.ALWAYS);
        HBox encabezado = new HBox(8, numero, separador, estado);

        Label destino = new Label(pedido.esParaLlevar() ? "Para llevar" : "Mesa " + pedido.getMesa().getNumero());
        destino.getStyleClass().add("comanda-destino");

        Label datos = new Label("Ingresó " + pedido.getHoraCreacion() + " (" + textoEspera(pedido.getMinutosEspera())
                + ") · Mozo: " + pedido.getUsuario().getNombre());
        datos.getStyleClass().add("comanda-datos");
        datos.setWrapText(true);

        VBox productos = new VBox(3);
        for (DetallePedido detalle : pedido.getDetalles()) {
            Label linea = new Label(detalle.getCantidad() + " × " + detalle.getProducto().getNombre());
            linea.getStyleClass().add("comanda-linea");
            linea.setWrapText(true);
            productos.getChildren().add(linea);
        }

        VBox tarjeta = new VBox(8, encabezado, destino, datos, productos);
        tarjeta.getStyleClass().addAll("comanda", claseDeEstado(pedido.getEstado()) + "-borde");

        if (pedidoService.puedeAvanzar(usuario, pedido.getEstado())) {
            Button accion = new Button(textoDeAccion(pedido.getEstado()));
            accion.getStyleClass().add("boton-primario");
            accion.setMaxWidth(Double.MAX_VALUE);
            accion.setOnAction(evento -> avanzar(pedido, accion));
            tarjeta.getChildren().add(accion);
        }
        if (pedidoService.puedeModificar(usuario, pedido.getEstado())) {
            Button modificar = new Button("Modificar");
            modificar.getStyleClass().add("boton-claro");
            modificar.setOnAction(evento -> modificar(pedido, modificar));
            Button cancelar = new Button("Cancelar pedido");
            cancelar.getStyleClass().add("boton-peligro");
            cancelar.setOnAction(evento -> cancelar(pedido, cancelar));
            HBox acciones = new HBox(8, modificar, cancelar);
            tarjeta.getChildren().add(acciones);
        }
        return tarjeta;
    }

    private void modificar(Pedido pedido, Button boton) {
        boton.setDisable(true);
        Tareas.ejecutar(
                () -> pedidoService.cargarParaModificar(usuario, pedido.getIdPedido()),
                actualizado -> navegador.mostrarModificarPedido(usuario, actualizado),
                causa -> {
                    Dialogos.error(panelPedidos, causa);
                    cargar();
                });
    }

    private void cancelar(Pedido pedido, Button boton) {
        String destino = pedido.esParaLlevar() ? "para llevar" : "de la mesa " + pedido.getMesa().getNumero();
        if (!Dialogos.confirmar(panelPedidos, "Cancelar pedido",
                "Se cancelará el pedido #" + pedido.getIdPedido() + " " + destino
                        + ".\nSe devuelven los insumos al stock y la mesa queda libre. Esta acción no se puede deshacer.")) {
            return;
        }
        boton.setDisable(true);
        Tareas.ejecutar(
                () -> {
                    pedidoService.cancelarPedido(usuario, pedido);
                    return Boolean.TRUE;
                },
                resultado -> cargar(),
                causa -> {
                    Dialogos.error(panelPedidos, causa);
                    cargar();
                });
    }

    private void avanzar(Pedido pedido, Button boton) {
        boton.setDisable(true);
        Tareas.ejecutar(
                () -> pedidoService.avanzarEstado(usuario, pedido),
                nuevoEstado -> cargar(),
                causa -> {
                    Dialogos.error(panelPedidos, causa);
                    cargar(); // el pedido pudo haber cambiado: se muestra el estado real
                });
    }

    private static String textoDeAccion(EstadoPedido estado) {
        return switch (estado) {
            case PENDIENTE -> "Iniciar preparación";
            case EN_PREPARACION -> "Marcar como listo";
            case LISTO -> "Marcar como entregado";
            default -> "";
        };
    }

    private static String textoEspera(long minutos) {
        if (minutos < 1) {
            return "recién ingresado";
        }
        return "hace " + minutos + " min";
    }

    private static String claseDeEstado(EstadoPedido estado) {
        return switch (estado) {
            case PENDIENTE -> "estado-pendiente";
            case EN_PREPARACION -> "estado-en-preparacion";
            case LISTO -> "estado-listo";
            default -> "estado-otro";
        };
    }
}
