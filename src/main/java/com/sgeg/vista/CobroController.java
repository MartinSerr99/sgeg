package com.sgeg.vista;

import com.sgeg.modelo.DetallePedido;
import com.sgeg.modelo.FormaPago;
import com.sgeg.modelo.Pago;
import com.sgeg.modelo.Pedido;
import com.sgeg.modelo.Usuario;
import com.sgeg.modelo.Venta;
import com.sgeg.servicio.VentaService;
import com.sgeg.util.Comprobantes;
import com.sgeg.util.Formatos;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.util.Duration;

/**
 * Cobro de pedidos entregados (CU13 y CU14). El cajero elige un pedido entregado, indica cómo
 * se paga y el sistema registra la venta, libera la mesa y muestra el comprobante.
 */
public class CobroController implements Detenible {

    private static final int SEGUNDOS_ENTRE_ACTUALIZACIONES = 15;
    private static final DateTimeFormatter HORA = DateTimeFormatter.ofPattern("HH:mm:ss");

    @FXML private Label lblActualizado;
    @FXML private Button btnActualizar;
    @FXML private TableView<Pedido> tblPedidos;
    @FXML private TableColumn<Pedido, String> colNumero;
    @FXML private TableColumn<Pedido, String> colDestino;
    @FXML private TableColumn<Pedido, String> colMozo;
    @FXML private TableColumn<Pedido, String> colHora;
    @FXML private TableColumn<Pedido, String> colTotal;
    @FXML private Label lblDetalleTitulo;
    @FXML private TableView<DetallePedido> tblDetalle;
    @FXML private TableColumn<DetallePedido, String> colDetalleProducto;
    @FXML private TableColumn<DetallePedido, String> colDetalleCantidad;
    @FXML private TableColumn<DetallePedido, String> colDetalleSubtotal;
    @FXML private Label lblTotal;
    @FXML private Button btnCobrar;

    private final ObservableList<Pedido> pedidos = FXCollections.observableArrayList();
    private final ObservableList<DetallePedido> detalle = FXCollections.observableArrayList();

    private Navegador navegador;
    private VentaService ventaService;
    private Usuario usuario;
    private Timeline temporizador;
    private int generacion;
    private boolean detenido;
    private boolean cobrando;

    /** Lo invoca el Navegador después de cargar el FXML. */
    public void inicializar(Navegador navegador, VentaService ventaService, Usuario usuario) {
        this.navegador = navegador;
        this.ventaService = ventaService;
        this.usuario = usuario;

        colNumero.setCellValueFactory(c -> new SimpleStringProperty("#" + c.getValue().getIdPedido()));
        colDestino.setCellValueFactory(c -> new SimpleStringProperty(destinoDe(c.getValue())));
        colMozo.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getUsuario().getNombre()));
        colHora.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getHoraCreacion()));
        colTotal.setCellValueFactory(c -> new SimpleStringProperty(Formatos.moneda(c.getValue().calcularTotal())));
        tblPedidos.setItems(pedidos);
        tblPedidos.setPlaceholder(new Label("No hay pedidos entregados para cobrar."));
        tblPedidos.getSelectionModel().selectedItemProperty().addListener((obs, anterior, actual) -> mostrarDetalle(actual));

        colDetalleProducto.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getProducto().getNombre()));
        colDetalleCantidad.setCellValueFactory(c -> new SimpleStringProperty(String.valueOf(c.getValue().getCantidad())));
        colDetalleSubtotal.setCellValueFactory(c -> new SimpleStringProperty(Formatos.moneda(c.getValue().getSubtotal())));
        tblDetalle.setItems(detalle);
        tblDetalle.setPlaceholder(new Label("Elegí un pedido de la lista."));

        mostrarDetalle(null);

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
                ventaService::listarPedidosACobrar,
                lista -> {
                    if (detenido || miGeneracion != generacion) {
                        return;
                    }
                    btnActualizar.setDisable(false);
                    mostrar(lista);
                },
                causa -> {
                    if (detenido || miGeneracion != generacion) {
                        return;
                    }
                    btnActualizar.setDisable(false);
                    lblActualizado.setText("No se pudo actualizar");
                    Dialogos.error(tblPedidos, causa);
                });
    }

    /** Reemplaza la lista conservando el pedido elegido, si todavía está. */
    private void mostrar(List<Pedido> lista) {
        Pedido elegido = tblPedidos.getSelectionModel().getSelectedItem();
        int idElegido = elegido == null ? -1 : elegido.getIdPedido();
        pedidos.setAll(lista);
        for (Pedido pedido : lista) {
            if (pedido.getIdPedido() == idElegido) {
                tblPedidos.getSelectionModel().select(pedido);
                break;
            }
        }
        if (tblPedidos.getSelectionModel().getSelectedItem() == null) {
            mostrarDetalle(null);
        }
        lblActualizado.setText("Actualizado " + LocalTime.now().format(HORA));
    }

    private void mostrarDetalle(Pedido pedido) {
        if (pedido == null) {
            lblDetalleTitulo.setText("Detalle del pedido");
            detalle.clear();
            lblTotal.setText(Formatos.moneda(java.math.BigDecimal.ZERO));
        } else {
            lblDetalleTitulo.setText("Pedido #" + pedido.getIdPedido() + " · " + destinoDe(pedido));
            detalle.setAll(pedido.getDetalles());
            lblTotal.setText(Formatos.moneda(pedido.calcularTotal()));
        }
        btnCobrar.setDisable(pedido == null || cobrando);
    }

    @FXML
    private void cobrar() {
        Pedido pedido = tblPedidos.getSelectionModel().getSelectedItem();
        if (pedido == null) {
            return;
        }
        cobrando = true;
        btnCobrar.setDisable(true);
        Tareas.ejecutar(
                ventaService::listarFormasDePago,
                formas -> pedirPago(pedido, formas),
                causa -> {
                    terminarCobro();
                    Dialogos.error(tblPedidos, causa);
                });
    }

    private void pedirPago(Pedido pedido, List<FormaPago> formas) {
        Optional<List<Pago>> pagos = CobroDialogo.pedir(tblPedidos, pedido.getIdPedido(), pedido.calcularTotal(), formas);
        if (pagos.isEmpty()) {
            terminarCobro();
            return;
        }
        Tareas.ejecutar(
                () -> ventaService.cobrar(usuario, pedido, pagos.get()),
                venta -> {
                    terminarCobro();
                    cargar();
                    mostrarComprobante(venta);
                },
                causa -> {
                    terminarCobro();
                    Dialogos.error(tblPedidos, causa);
                    cargar(); // otro cajero pudo haberlo cobrado: se muestra la lista real
                });
    }

    private void terminarCobro() {
        cobrando = false;
        mostrarDetalle(tblPedidos.getSelectionModel().getSelectedItem());
    }

    private void mostrarComprobante(Venta venta) {
        TextoDialogo.mostrar(tblPedidos, "Venta registrada · Comprobante N° " + String.format("%06d", venta.getIdVenta()),
                Comprobantes.deVenta(venta), "comprobante_" + String.format("%06d", venta.getIdVenta()) + ".txt");
    }

    private static String destinoDe(Pedido pedido) {
        return pedido.esParaLlevar() ? "Para llevar" : "Mesa " + pedido.getMesa().getNumero();
    }
}
