package com.sgeg.vista;

import com.sgeg.modelo.ProductoVendido;
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
import java.util.List;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.input.MouseButton;

/**
 * Reportes del administrador: ventas de un período (CU16) y productos más vendidos (CU17).
 * Solo se cuentan las ventas ya cobradas.
 */
public class ReportesController {

    private record Datos(ResumenPeriodo ventas, List<ProductoVendido> productos) {
    }

    @FXML private DatePicker dpDesde;
    @FXML private DatePicker dpHasta;
    @FXML private Spinner<Integer> spnTop;
    @FXML private Button btnConsultar;
    @FXML private Button btnResumen;

    @FXML private Label lblCantidadVentas;
    @FXML private Label lblTotalVendido;
    @FXML private Label lblTicketPromedio;
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

    @FXML private TableView<ProductoVendido> tblProductos;
    @FXML private TableColumn<ProductoVendido, String> colPuesto;
    @FXML private TableColumn<ProductoVendido, String> colProducto;
    @FXML private TableColumn<ProductoVendido, String> colUnidades;
    @FXML private TableColumn<ProductoVendido, String> colMonto;

    private final ObservableList<TotalFormaPago> formas = FXCollections.observableArrayList();
    private final ObservableList<ResumenVenta> ventas = FXCollections.observableArrayList();
    private final ObservableList<ProductoVendido> productos = FXCollections.observableArrayList();

    private Navegador navegador;
    private ReporteService reporteService;
    private VentaService ventaService;
    private Usuario usuario;
    private ResumenPeriodo ultimoResumen;
    private LocalDate ultimoDesde;
    private LocalDate ultimoHasta;

    /** Lo invoca el Navegador después de cargar el FXML. */
    public void inicializar(Navegador navegador, ReporteService reporteService, VentaService ventaService, Usuario usuario) {
        this.navegador = navegador;
        this.reporteService = reporteService;
        this.ventaService = ventaService;
        this.usuario = usuario;

        LocalDate hoy = LocalDate.now();
        dpDesde.setValue(hoy.withDayOfMonth(1));
        dpHasta.setValue(hoy);
        dpDesde.setEditable(false);
        dpHasta.setEditable(false);
        spnTop.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 100, 10));

        colForma.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().formaPago()));
        colPagos.setCellValueFactory(c -> new SimpleStringProperty(String.valueOf(c.getValue().cantidadPagos())));
        colTotalForma.setCellValueFactory(c -> new SimpleStringProperty(Formatos.moneda(c.getValue().total())));
        tblFormas.setItems(formas);
        tblFormas.setPlaceholder(new Label("Sin cobros en el período."));

        colComprobante.setCellValueFactory(c -> new SimpleStringProperty(String.format("%06d", c.getValue().idVenta())));
        colFecha.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().fechaHora()));
        colDestino.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().destino()));
        colMozo.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().mozo()));
        colTotalVenta.setCellValueFactory(c -> new SimpleStringProperty(Formatos.moneda(c.getValue().total())));
        tblVentas.setItems(ventas);
        tblVentas.setPlaceholder(new Label("Sin ventas en el período."));
        tblVentas.setOnMouseClicked(evento -> {
            if (evento.getButton() == MouseButton.PRIMARY && evento.getClickCount() == 2) {
                verComprobante();
            }
        });

        colPuesto.setCellValueFactory(c -> new SimpleStringProperty(String.valueOf(productos.indexOf(c.getValue()) + 1)));
        colProducto.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().producto()));
        colUnidades.setCellValueFactory(c -> new SimpleStringProperty(String.valueOf(c.getValue().unidades())));
        colMonto.setCellValueFactory(c -> new SimpleStringProperty(Formatos.moneda(c.getValue().monto())));
        tblProductos.setItems(productos);
        tblProductos.setPlaceholder(new Label("Sin ventas en el período."));

        btnResumen.setDisable(true);
        consultar();
    }

    @FXML
    private void hoy() {
        establecerPeriodo(LocalDate.now(), LocalDate.now());
    }

    @FXML
    private void ultimosSieteDias() {
        establecerPeriodo(LocalDate.now().minusDays(6), LocalDate.now());
    }

    @FXML
    private void esteMes() {
        LocalDate hoy = LocalDate.now();
        establecerPeriodo(hoy.withDayOfMonth(1), hoy);
    }

    private void establecerPeriodo(LocalDate desde, LocalDate hasta) {
        dpDesde.setValue(desde);
        dpHasta.setValue(hasta);
        consultar();
    }

    @FXML
    private void consultar() {
        LocalDate desde = dpDesde.getValue();
        LocalDate hasta = dpHasta.getValue();
        int limite = spnTop.getValue();
        btnConsultar.setDisable(true);
        Tareas.ejecutar(
                () -> new Datos(reporteService.ventasDelPeriodo(desde, hasta),
                        reporteService.productosMasVendidos(desde, hasta, limite)),
                datos -> {
                    btnConsultar.setDisable(false);
                    ultimoDesde = desde;
                    ultimoHasta = hasta;
                    ultimoResumen = datos.ventas();
                    mostrar(datos);
                },
                causa -> {
                    btnConsultar.setDisable(false);
                    Dialogos.error(tblVentas, causa);
                });
    }

    private void mostrar(Datos datos) {
        ResumenPeriodo resumen = datos.ventas();
        lblCantidadVentas.setText(String.valueOf(resumen.cantidadVentas()));
        lblTotalVendido.setText(Formatos.moneda(resumen.totalVendido()));
        lblTicketPromedio.setText(resumen.cantidadVentas() == 0 ? "—"
                : Formatos.moneda(resumen.totalVendido().divide(
                        java.math.BigDecimal.valueOf(resumen.cantidadVentas()), 2, java.math.RoundingMode.HALF_UP)));
        formas.setAll(resumen.totalesPorForma());
        ventas.setAll(resumen.ventas());
        productos.setAll(datos.productos());
        tblProductos.refresh();
        btnResumen.setDisable(false);
    }

    @FXML
    private void verResumen() {
        if (ultimoResumen == null) {
            return;
        }
        String texto = Comprobantes.resumenDeCaja("Reporte de ventas", ultimoDesde, ultimoHasta, ultimoResumen)
                + System.lineSeparator() + "Productos más vendidos:" + System.lineSeparator()
                + Comprobantes.productosVendidos(productos);
        TextoDialogo.mostrar(tblVentas, "Resumen del período", texto, "reporte_" + ultimoDesde + "_" + ultimoHasta + ".txt");
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
