package com.sgeg.vista;

import com.sgeg.modelo.ResumenPeriodo;
import com.sgeg.modelo.ResumenVenta;
import com.sgeg.modelo.TotalFormaPago;
import com.sgeg.modelo.Usuario;
import com.sgeg.modelo.Venta;
import com.sgeg.servicio.ReporteService;
import com.sgeg.servicio.VentaService;
import com.sgeg.util.Comprobantes;
import com.sgeg.util.Formatos;
import java.time.LocalDate;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.HBox;

/**
 * Cierre de caja (CU15): lo vendido en el día y lo cobrado con cada forma de pago.
 * Es una consulta: no modifica datos, así que se puede repetir las veces que haga falta.
 */
public class CierreCajaController {

    private record Datos(ResumenPeriodo resumen, int pedidosSinCobrar) {
    }

    @FXML private DatePicker dpDia;
    @FXML private Button btnConsultar;
    @FXML private Button btnGuardar;
    @FXML private HBox avisoSinCobrar;
    @FXML private Label lblAvisoSinCobrar;
    @FXML private Label lblCantidadVentas;
    @FXML private Label lblTotalVendido;
    @FXML private TableView<TotalFormaPago> tblFormas;
    @FXML private TableColumn<TotalFormaPago, String> colForma;
    @FXML private TableColumn<TotalFormaPago, String> colPagos;
    @FXML private TableColumn<TotalFormaPago, String> colTotalForma;
    @FXML private TableView<ResumenVenta> tblVentas;
    @FXML private TableColumn<ResumenVenta, String> colComprobante;
    @FXML private TableColumn<ResumenVenta, String> colFecha;
    @FXML private TableColumn<ResumenVenta, String> colDestino;
    @FXML private TableColumn<ResumenVenta, String> colMozo;
    @FXML private TableColumn<ResumenVenta, String> colTotalVenta;

    private final ObservableList<TotalFormaPago> formas = FXCollections.observableArrayList();
    private final ObservableList<ResumenVenta> ventas = FXCollections.observableArrayList();

    private Navegador navegador;
    private ReporteService reporteService;
    private VentaService ventaService;
    private Usuario usuario;
    private ResumenPeriodo ultimoResumen;
    private LocalDate ultimoDia;

    /** Lo invoca el Navegador después de cargar el FXML. */
    public void inicializar(Navegador navegador, ReporteService reporteService, VentaService ventaService, Usuario usuario) {
        this.navegador = navegador;
        this.reporteService = reporteService;
        this.ventaService = ventaService;
        this.usuario = usuario;

        dpDia.setValue(LocalDate.now());
        dpDia.setEditable(false);

        colForma.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().formaPago()));
        colPagos.setCellValueFactory(c -> new SimpleStringProperty(String.valueOf(c.getValue().cantidadPagos())));
        colTotalForma.setCellValueFactory(c -> new SimpleStringProperty(Formatos.moneda(c.getValue().total())));
        tblFormas.setItems(formas);
        tblFormas.setPlaceholder(new Label("Sin cobros en el día."));

        colComprobante.setCellValueFactory(c -> new SimpleStringProperty(String.format("%06d", c.getValue().idVenta())));
        colFecha.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().fechaHora()));
        colDestino.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().destino()));
        colMozo.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().mozo()));
        colTotalVenta.setCellValueFactory(c -> new SimpleStringProperty(Formatos.moneda(c.getValue().total())));
        tblVentas.setItems(ventas);
        tblVentas.setPlaceholder(new Label("Sin ventas en el día."));
        tblVentas.setOnMouseClicked(evento -> {
            if (evento.getButton() == MouseButton.PRIMARY && evento.getClickCount() == 2) {
                verComprobante();
            }
        });

        btnGuardar.setDisable(true);
        consultar();
    }

    @FXML
    private void consultar() {
        LocalDate dia = dpDia.getValue();
        btnConsultar.setDisable(true);
        Tareas.ejecutar(
                () -> new Datos(reporteService.cierreDeCaja(dia), reporteService.contarPedidosSinCobrar()),
                datos -> {
                    btnConsultar.setDisable(false);
                    ultimoDia = dia;
                    ultimoResumen = datos.resumen();
                    mostrar(datos);
                },
                causa -> {
                    btnConsultar.setDisable(false);
                    Dialogos.error(tblVentas, causa);
                });
    }

    private void mostrar(Datos datos) {
        ResumenPeriodo resumen = datos.resumen();
        lblCantidadVentas.setText(String.valueOf(resumen.cantidadVentas()));
        lblTotalVendido.setText(Formatos.moneda(resumen.totalVendido()));
        formas.setAll(resumen.totalesPorForma());
        ventas.setAll(resumen.ventas());
        btnGuardar.setDisable(false);

        boolean hayPendientes = datos.pedidosSinCobrar() > 0;
        avisoSinCobrar.setVisible(hayPendientes);
        avisoSinCobrar.setManaged(hayPendientes);
        lblAvisoSinCobrar.setText(datos.pedidosSinCobrar() == 1
                ? "Hay 1 pedido entregado que todavía no se cobró: no figura en este cierre."
                : "Hay " + datos.pedidosSinCobrar() + " pedidos entregados que todavía no se cobraron: no figuran en este cierre.");
    }

    @FXML
    private void guardar() {
        if (ultimoResumen == null) {
            return;
        }
        String texto = Comprobantes.resumenDeCaja("Cierre de caja", ultimoDia, ultimoDia, ultimoResumen);
        TextoDialogo.mostrar(tblVentas, "Cierre de caja del " + Formatos.fecha(ultimoDia), texto,
                "cierre_caja_" + ultimoDia + ".txt");
    }

    private void verComprobante() {
        ResumenVenta elegida = tblVentas.getSelectionModel().getSelectedItem();
        if (elegida == null) {
            return;
        }
        Tareas.ejecutar(
                () -> ventaService.obtenerVenta(elegida.idVenta()),
                this::mostrarComprobante,
                causa -> Dialogos.error(tblVentas, causa));
    }

    private void mostrarComprobante(Venta venta) {
        String numero = String.format("%06d", venta.getIdVenta());
        TextoDialogo.mostrar(tblVentas, "Comprobante N° " + numero, Comprobantes.deVenta(venta),
                "comprobante_" + numero + ".txt");
    }

    @FXML
    private void volver() {
        navegador.mostrarPrincipal(usuario);
    }
}
