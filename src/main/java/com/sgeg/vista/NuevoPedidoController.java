package com.sgeg.vista;

import com.sgeg.excepcion.StockInsuficienteException;
import com.sgeg.modelo.Cliente;
import com.sgeg.modelo.DetallePedido;
import com.sgeg.modelo.Mesa;
import com.sgeg.modelo.Pedido;
import com.sgeg.modelo.Producto;
import com.sgeg.modelo.Usuario;
import com.sgeg.servicio.PedidoService;
import com.sgeg.util.Formatos;
import java.util.List;
import java.util.TreeSet;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.input.MouseButton;

/**
 * Toma de un pedido (CU06). El mozo arma el pedido con lo que dice el cliente:
 * el stock se controla al agregar cada producto (RF19) y se descuenta al confirmar (RF23).
 * La misma pantalla sirve para modificar un pedido que sigue pendiente (CU07).
 */
public class NuevoPedidoController {

    private static final String TODAS_LAS_CATEGORIAS = "Todas las categorías";
    private static final Cliente SIN_CLIENTE = new Cliente(0, "Sin cliente registrado", null);

    /** Lo que se necesita para armar la pantalla, cargado en un solo paso. */
    private record DatosIniciales(List<Producto> menu, List<Cliente> clientes) {
    }

    @FXML private Label lblTitulo;
    @FXML private ComboBox<String> cmbCategoria;
    @FXML private TableView<Producto> tblMenu;
    @FXML private TableColumn<Producto, String> colProducto;
    @FXML private TableColumn<Producto, String> colCategoria;
    @FXML private TableColumn<Producto, String> colPrecio;
    @FXML private TableColumn<Producto, String> colDisponibles;
    @FXML private Spinner<Integer> spnCantidad;
    @FXML private Button btnAgregar;

    @FXML private TableView<DetallePedido> tblPedido;
    @FXML private TableColumn<DetallePedido, String> colPedidoProducto;
    @FXML private TableColumn<DetallePedido, String> colPedidoCantidad;
    @FXML private TableColumn<DetallePedido, String> colPedidoPrecio;
    @FXML private TableColumn<DetallePedido, String> colPedidoSubtotal;
    @FXML private Button btnQuitarUnidad;
    @FXML private Button btnQuitarLinea;
    @FXML private ComboBox<Cliente> cmbCliente;
    @FXML private Label lblTotal;
    @FXML private Button btnConfirmar;
    @FXML private Button btnCancelar;

    private final ObservableList<Producto> menu = FXCollections.observableArrayList();
    private final FilteredList<Producto> menuFiltrado = new FilteredList<>(menu);
    private final ObservableList<DetallePedido> lineas = FXCollections.observableArrayList();

    private Navegador navegador;
    private PedidoService pedidoService;
    private Usuario usuario;
    private Pedido pedido;
    private boolean ocupado;
    private boolean edicion;

    /** Lo invoca el Navegador después de cargar el FXML (pedido nuevo). */
    public void inicializar(Navegador navegador, PedidoService pedidoService, Usuario usuario, Mesa mesa) {
        this.pedido = pedidoService.iniciarPedido(mesa);
        this.edicion = false;
        lblTitulo.setText(mesa == null ? "Nuevo pedido · Para llevar" : "Nuevo pedido · Mesa " + mesa.getNumero());
        configurar(navegador, pedidoService, usuario);
    }

    /** Lo invoca el Navegador para modificar un pedido pendiente (CU07). */
    public void inicializarEdicion(Navegador navegador, PedidoService pedidoService, Usuario usuario, Pedido existente) {
        this.pedido = existente;
        this.edicion = true;
        String destino = existente.esParaLlevar() ? "Para llevar" : "Mesa " + existente.getMesa().getNumero();
        lblTitulo.setText("Modificar pedido #" + existente.getIdPedido() + " · " + destino);
        btnConfirmar.setText("Guardar cambios");
        configurar(navegador, pedidoService, usuario);
    }

    private void configurar(Navegador navegador, PedidoService pedidoService, Usuario usuario) {
        this.navegador = navegador;
        this.pedidoService = pedidoService;
        this.usuario = usuario;

        configurarMenu();
        configurarPedido();
        spnCantidad.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 99, 1));
        cmbCliente.getItems().setAll(SIN_CLIENTE);
        cmbCliente.getSelectionModel().select(SIN_CLIENTE);

        refrescarPedido();
        cargarDatos();
    }

    // ------------------------------------------------------------------ configuración de tablas

    private void configurarMenu() {
        colProducto.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getNombre()));
        colCategoria.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getCategoria().getNombre()));
        colPrecio.setCellValueFactory(c -> new SimpleStringProperty(Formatos.moneda(c.getValue().getPrecio())));
        colDisponibles.setCellValueFactory(c -> new SimpleStringProperty(textoDisponibles(c.getValue())));
        tblMenu.setItems(menuFiltrado);
        tblMenu.setPlaceholder(new Label("No hay productos para mostrar."));

        cmbCategoria.getItems().setAll(TODAS_LAS_CATEGORIAS);
        cmbCategoria.getSelectionModel().select(TODAS_LAS_CATEGORIAS);
        cmbCategoria.valueProperty().addListener((obs, anterior, elegida) -> filtrarPorCategoria(elegida));

        // Doble clic sobre un producto = agregarlo con la cantidad elegida.
        tblMenu.setOnMouseClicked(evento -> {
            if (evento.getButton() == MouseButton.PRIMARY && evento.getClickCount() == 2) {
                agregar();
            }
        });
    }

    private void configurarPedido() {
        colPedidoProducto.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getProducto().getNombre()));
        colPedidoCantidad.setCellValueFactory(c -> new SimpleStringProperty(String.valueOf(c.getValue().getCantidad())));
        colPedidoPrecio.setCellValueFactory(c -> new SimpleStringProperty(Formatos.moneda(c.getValue().getPrecioUnitario())));
        colPedidoSubtotal.setCellValueFactory(c -> new SimpleStringProperty(Formatos.moneda(c.getValue().getSubtotal())));
        tblPedido.setItems(lineas);
        tblPedido.setPlaceholder(new Label("Todavía no agregaste productos."));
        tblPedido.getSelectionModel().selectedItemProperty().addListener((obs, anterior, actual) -> actualizarBotones());
    }

    private static String textoDisponibles(Producto producto) {
        if (producto.tieneStockIlimitado()) {
            return "—";
        }
        return producto.hayStock() ? String.valueOf(producto.getUnidadesDisponibles()) : "Sin stock";
    }

    // ------------------------------------------------------------------ carga de datos

    private void cargarDatos() {
        establecerOcupado(true);
        Tareas.ejecutar(
                () -> new DatosIniciales(pedidoService.listarMenu(), pedidoService.listarClientes()),
                datos -> {
                    mostrarMenu(datos.menu());
                    cmbCliente.getItems().setAll(SIN_CLIENTE);
                    cmbCliente.getItems().addAll(datos.clientes());
                    cmbCliente.getSelectionModel().select(clienteDelPedido(datos.clientes()));
                    establecerOcupado(false);
                },
                causa -> {
                    establecerOcupado(false);
                    Dialogos.error(tblMenu, causa);
                });
    }

    /** Al modificar un pedido, el combo arranca con el cliente que ya tenía. */
    private Cliente clienteDelPedido(List<Cliente> clientes) {
        if (pedido.getCliente() != null) {
            for (Cliente cliente : clientes) {
                if (cliente.getIdCliente() == pedido.getCliente().getIdCliente()) {
                    return cliente;
                }
            }
        }
        return SIN_CLIENTE;
    }

    /** Vuelve a leer el menú (por ejemplo, después de un aviso de stock) conservando el filtro. */
    private void recargarMenu() {
        Tareas.ejecutar(pedidoService::listarMenu, this::mostrarMenu, causa -> Dialogos.error(tblMenu, causa));
    }

    private void mostrarMenu(List<Producto> productos) {
        menu.setAll(productos);

        String elegida = cmbCategoria.getValue();
        TreeSet<String> categorias = new TreeSet<>();
        for (Producto producto : productos) {
            categorias.add(producto.getCategoria().getNombre());
        }
        cmbCategoria.getItems().setAll(TODAS_LAS_CATEGORIAS);
        cmbCategoria.getItems().addAll(categorias);
        cmbCategoria.getSelectionModel().select(
                elegida != null && cmbCategoria.getItems().contains(elegida) ? elegida : TODAS_LAS_CATEGORIAS);
        filtrarPorCategoria(cmbCategoria.getValue());
    }

    private void filtrarPorCategoria(String categoria) {
        if (categoria == null || TODAS_LAS_CATEGORIAS.equals(categoria)) {
            menuFiltrado.setPredicate(null);
        } else {
            menuFiltrado.setPredicate(p -> p.getCategoria().getNombre().equals(categoria));
        }
    }

    // ------------------------------------------------------------------ acciones

    @FXML
    private void agregar() {
        Producto elegido = tblMenu.getSelectionModel().getSelectedItem();
        if (elegido == null) {
            Dialogos.informar(tblMenu, "Elegí un producto", "Seleccioná un producto del menú y luego tocá «Agregar».");
            return;
        }
        int cantidad = spnCantidad.getValue();

        establecerOcupado(true);
        Tareas.ejecutar(
                () -> {
                    pedidoService.agregarProducto(pedido, elegido, cantidad);
                    return Boolean.TRUE;
                },
                resultado -> {
                    spnCantidad.getValueFactory().setValue(1);
                    refrescarPedido();
                    establecerOcupado(false);
                },
                causa -> {
                    establecerOcupado(false);
                    Dialogos.error(tblMenu, causa);
                    if (causa instanceof StockInsuficienteException) {
                        recargarMenu();
                    }
                });
    }

    @FXML
    private void quitarUnidad() {
        DetallePedido seleccionada = tblPedido.getSelectionModel().getSelectedItem();
        if (seleccionada != null) {
            pedido.quitarUnidad(seleccionada.getProducto().getIdProducto());
            refrescarPedido();
        }
    }

    @FXML
    private void quitarLinea() {
        DetallePedido seleccionada = tblPedido.getSelectionModel().getSelectedItem();
        if (seleccionada != null) {
            pedido.quitarProducto(seleccionada.getProducto().getIdProducto());
            refrescarPedido();
        }
    }

    @FXML
    private void confirmar() {
        if (pedido.estaVacio()) {
            return;
        }
        Cliente cliente = cmbCliente.getValue();
        pedido.setCliente(cliente == null || cliente == SIN_CLIENTE ? null : cliente);

        String destino = pedido.esParaLlevar() ? "para llevar" : "para la mesa " + pedido.getMesa().getNumero();
        String pregunta = edicion
                ? "Se guardarán los cambios del pedido #" + pedido.getIdPedido() + " (" + destino + ") por "
                        + Formatos.moneda(pedido.calcularTotal()) + ".\nSe ajusta el stock de insumos por la diferencia."
                : "Se enviará a cocina el pedido " + destino + " por " + Formatos.moneda(pedido.calcularTotal())
                        + ".\nAl confirmar se descuentan los insumos del stock.";
        if (!Dialogos.confirmar(tblPedido, edicion ? "Guardar cambios" : "Confirmar pedido", pregunta)) {
            return;
        }

        establecerOcupado(true);
        Tareas.ejecutar(
                () -> {
                    if (edicion) {
                        pedidoService.modificarPedido(pedido, usuario);
                    } else {
                        pedidoService.confirmarPedido(pedido, usuario);
                    }
                    return pedido.getIdPedido();
                },
                numero -> {
                    if (edicion) {
                        Dialogos.informar(tblPedido, "Pedido modificado", "El pedido #" + numero + " quedó actualizado.");
                        navegador.mostrarSeguimiento(usuario);
                    } else {
                        Dialogos.informar(tblPedido, "Pedido registrado",
                                "El pedido #" + numero + " quedó pendiente y ya lo ve la cocina.");
                        navegador.mostrarMesas(usuario);
                    }
                },
                causa -> {
                    establecerOcupado(false);
                    Dialogos.error(tblPedido, causa);
                    if (causa instanceof StockInsuficienteException) {
                        recargarMenu();
                    }
                });
    }

    @FXML
    private void cancelar() {
        if (edicion) {
            if (Dialogos.confirmar(tblPedido, "Salir sin guardar", "Se descartarán los cambios hechos a este pedido. ¿Querés salir?")) {
                navegador.mostrarSeguimiento(usuario);
            }
            return;
        }
        if (!pedido.estaVacio()
                && !Dialogos.confirmar(tblPedido, "Cancelar pedido", "Se descartarán los productos cargados. ¿Querés salir?")) {
            return;
        }
        navegador.mostrarMesas(usuario);
    }

    // ------------------------------------------------------------------ estado de la pantalla

    private void refrescarPedido() {
        lineas.setAll(pedido.getDetalles());
        tblPedido.refresh();
        lblTotal.setText(Formatos.moneda(pedido.calcularTotal()));
        actualizarBotones();
    }

    private void establecerOcupado(boolean valor) {
        ocupado = valor;
        actualizarBotones();
    }

    private void actualizarBotones() {
        boolean haySeleccion = tblPedido.getSelectionModel().getSelectedItem() != null;
        btnAgregar.setDisable(ocupado);
        btnQuitarUnidad.setDisable(ocupado || !haySeleccion);
        btnQuitarLinea.setDisable(ocupado || !haySeleccion);
        btnConfirmar.setDisable(ocupado || pedido.estaVacio());
        btnCancelar.setDisable(ocupado);
    }
}
