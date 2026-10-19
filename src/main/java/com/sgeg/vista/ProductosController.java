package com.sgeg.vista;

import com.sgeg.modelo.Categoria;
import com.sgeg.modelo.IngredienteReceta;
import com.sgeg.modelo.Insumo;
import com.sgeg.modelo.Producto;
import com.sgeg.modelo.Usuario;
import com.sgeg.servicio.ProductoService;
import com.sgeg.util.Formatos;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.TreeSet;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.TextInputDialog;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

/**
 * Productos y menú. El administrador da de alta, modifica y da de baja productos y define su receta
 * (CU03, CU04); el resto del personal solo consulta el menú disponible (CU05).
 */
public class ProductosController {

    private static final String TODAS_LAS_CATEGORIAS = "Todas las categorías";

    /** Producto con su receta, tal como sale del formulario. */
    private record DatosProducto(Producto producto, List<IngredienteReceta> receta) {
    }

    /** Lo que necesita el formulario de producto, cargado de una vez. */
    private record DatosFormulario(List<Categoria> categorias, List<Insumo> insumos, List<IngredienteReceta> receta) {
    }

    @FXML private Label lblTitulo;
    @FXML private Label lblAyuda;
    @FXML private ComboBox<String> cmbCategoria;
    @FXML private CheckBox chkBajas;
    @FXML private HBox acciones;
    @FXML private Button btnEditar;
    @FXML private Button btnBaja;
    @FXML private TableView<Producto> tblProductos;
    @FXML private TableColumn<Producto, String> colNombre;
    @FXML private TableColumn<Producto, String> colCategoria;
    @FXML private TableColumn<Producto, String> colPrecio;
    @FXML private TableColumn<Producto, String> colEstado;
    @FXML private TableColumn<Producto, String> colDisponibles;

    private final ObservableList<Producto> productos = FXCollections.observableArrayList();
    private final FilteredList<Producto> productosFiltrados = new FilteredList<>(productos);

    private Navegador navegador;
    private ProductoService productoService;
    private Usuario usuario;
    private boolean puedeEditar;

    /** Lo invoca el Navegador después de cargar el FXML. */
    public void inicializar(Navegador navegador, ProductoService productoService, Usuario usuario) {
        this.navegador = navegador;
        this.productoService = productoService;
        this.usuario = usuario;
        this.puedeEditar = productoService.puedeEditar(usuario);

        lblTitulo.setText(puedeEditar ? "Productos y menú" : "Menú");
        lblAyuda.setText(puedeEditar
                ? "Un producto dado de baja deja de ofrecerse, pero se conserva en el historial de ventas."
                : "Productos disponibles y cuántas unidades alcanzan con el stock actual.");
        acciones.setVisible(puedeEditar);
        acciones.setManaged(puedeEditar);
        chkBajas.setVisible(puedeEditar);
        chkBajas.setManaged(puedeEditar);

        colNombre.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getNombre()));
        colCategoria.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getCategoria().getNombre()));
        colPrecio.setCellValueFactory(c -> new SimpleStringProperty(Formatos.moneda(c.getValue().getPrecio())));
        colEstado.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().isDisponible() ? "Disponible" : "Dado de baja"));
        colDisponibles.setCellValueFactory(c -> new SimpleStringProperty(textoUnidades(c.getValue())));
        tblProductos.setItems(productosFiltrados);
        tblProductos.setPlaceholder(new Label("No hay productos para mostrar."));
        tblProductos.getSelectionModel().selectedItemProperty().addListener((obs, anterior, actual) -> actualizarBotones());

        cmbCategoria.getItems().setAll(TODAS_LAS_CATEGORIAS);
        cmbCategoria.getSelectionModel().select(TODAS_LAS_CATEGORIAS);
        cmbCategoria.valueProperty().addListener((obs, anterior, nueva) -> aplicarFiltro());
        chkBajas.selectedProperty().addListener((obs, anterior, nueva) -> aplicarFiltro());

        actualizarBotones();
        cargar();
    }

    @FXML
    private void volver() {
        navegador.mostrarPrincipal(usuario);
    }

    @FXML
    private void actualizar() {
        cargar();
    }

    private static String textoUnidades(Producto producto) {
        if (producto.tieneStockIlimitado()) {
            return "—";
        }
        return producto.hayStock() ? String.valueOf(producto.getUnidadesDisponibles()) : "Sin stock";
    }

    // ------------------------------------------------------------------ carga y filtros

    private void cargar() {
        Producto elegido = tblProductos.getSelectionModel().getSelectedItem();
        int idElegido = elegido == null ? -1 : elegido.getIdProducto();
        Tareas.ejecutar(
                () -> puedeEditar ? productoService.listarTodos() : productoService.listarMenu(),
                lista -> {
                    productos.setAll(lista);
                    TreeSet<String> categorias = new TreeSet<>();
                    for (Producto producto : lista) {
                        categorias.add(producto.getCategoria().getNombre());
                    }
                    String actual = cmbCategoria.getValue();
                    cmbCategoria.getItems().setAll(TODAS_LAS_CATEGORIAS);
                    cmbCategoria.getItems().addAll(categorias);
                    cmbCategoria.getSelectionModel().select(
                            actual != null && cmbCategoria.getItems().contains(actual) ? actual : TODAS_LAS_CATEGORIAS);
                    aplicarFiltro();
                    for (Producto producto : productosFiltrados) {
                        if (producto.getIdProducto() == idElegido) {
                            tblProductos.getSelectionModel().select(producto);
                        }
                    }
                    actualizarBotones();
                },
                causa -> Dialogos.error(tblProductos, causa));
    }

    private void aplicarFiltro() {
        String categoria = cmbCategoria.getValue();
        boolean conBajas = chkBajas.isSelected();
        productosFiltrados.setPredicate(producto ->
                (conBajas || producto.isDisponible())
                        && (categoria == null || TODAS_LAS_CATEGORIAS.equals(categoria)
                                || producto.getCategoria().getNombre().equals(categoria)));
    }

    private void actualizarBotones() {
        Producto elegido = tblProductos.getSelectionModel().getSelectedItem();
        btnEditar.setDisable(elegido == null);
        btnBaja.setDisable(elegido == null);
        btnBaja.setText(elegido != null && !elegido.isDisponible() ? "Reactivar" : "Dar de baja");
    }

    // ------------------------------------------------------------------ CU03: alta y modificación

    @FXML
    private void nuevo() {
        Tareas.ejecutar(
                () -> new DatosFormulario(productoService.listarCategorias(), productoService.listarInsumos(), List.of()),
                datos -> {
                    if (datos.categorias().isEmpty()) {
                        Dialogos.informar(tblProductos, "Primero una categoría",
                                "Todavía no hay categorías. Creá al menos una con el botón «Categorías».");
                        return;
                    }
                    pedirDatos(null, datos).ifPresent(resultado ->
                            Tareas.ejecutar(
                                    () -> {
                                        productoService.crear(usuario, resultado.producto(), resultado.receta());
                                        return Boolean.TRUE;
                                    },
                                    ok -> cargar(),
                                    causa -> Dialogos.error(tblProductos, causa)));
                },
                causa -> Dialogos.error(tblProductos, causa));
    }

    @FXML
    private void editar() {
        Producto elegido = tblProductos.getSelectionModel().getSelectedItem();
        if (elegido == null) {
            return;
        }
        Tareas.ejecutar(
                () -> new DatosFormulario(productoService.listarCategorias(), productoService.listarInsumos(),
                        productoService.obtenerReceta(elegido.getIdProducto())),
                datos -> pedirDatos(elegido, datos).ifPresent(resultado ->
                        Tareas.ejecutar(
                                () -> {
                                    productoService.modificar(usuario, resultado.producto(), resultado.receta());
                                    return Boolean.TRUE;
                                },
                                ok -> cargar(),
                                causa -> Dialogos.error(tblProductos, causa))),
                causa -> Dialogos.error(tblProductos, causa));
    }

    /** Formulario de producto con su receta. {@code existente} es null en el alta. */
    private Optional<DatosProducto> pedirDatos(Producto existente, DatosFormulario datos) {
        boolean esAlta = existente == null;
        FormularioDialogo<DatosProducto> formulario = new FormularioDialogo<>(tblProductos,
                esAlta ? "Nuevo producto" : "Editar «" + existente.getNombre() + "»", "Guardar");

        TextField nombre = formulario.agregar("Nombre", new TextField(esAlta ? "" : existente.getNombre()));
        ComboBox<Categoria> categoria = formulario.agregar("Categoría", new ComboBox<>());
        categoria.getItems().setAll(datos.categorias());
        if (!esAlta) {
            for (Categoria candidata : datos.categorias()) {
                if (candidata.getIdCategoria() == existente.getCategoria().getIdCategoria()) {
                    categoria.setValue(candidata);
                }
            }
        }
        TextField precio = formulario.agregar("Precio ($)",
                new TextField(esAlta ? "" : Formatos.numero(existente.getPrecio()).replace(".", "")));
        CheckBox disponible = formulario.agregar("Disponible", new CheckBox("Se ofrece en el menú"));
        disponible.setSelected(esAlta || existente.isDisponible());

        // ---- receta
        ObservableList<IngredienteReceta> receta = FXCollections.observableArrayList(datos.receta());
        TableView<IngredienteReceta> tblReceta = new TableView<>(receta);
        TableColumn<IngredienteReceta, String> colInsumo = new TableColumn<>("Insumo");
        colInsumo.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getNombreInsumo()));
        colInsumo.setPrefWidth(200);
        colInsumo.setSortable(false);
        TableColumn<IngredienteReceta, String> colCantidad = new TableColumn<>("Cantidad por unidad");
        colCantidad.setCellValueFactory(c -> new SimpleStringProperty(
                Formatos.numero(c.getValue().getCantidadNecesaria()) + " " + c.getValue().getUnidadMedida()));
        colCantidad.setPrefWidth(160);
        colCantidad.setSortable(false);
        tblReceta.getColumns().add(colInsumo);
        tblReceta.getColumns().add(colCantidad);
        tblReceta.setPrefHeight(150);
        tblReceta.setPlaceholder(new Label("Sin receta: el producto no descuenta insumos."));

        ComboBox<Insumo> cmbInsumo = new ComboBox<>();
        cmbInsumo.getItems().setAll(datos.insumos());
        cmbInsumo.setPromptText("Insumo");
        cmbInsumo.setPrefWidth(180);
        TextField txtCantidad = new TextField();
        txtCantidad.setPromptText("Cantidad");
        txtCantidad.setPrefWidth(90);
        Button agregar = new Button("Agregar");
        agregar.getStyleClass().add("boton-claro");
        Button quitar = new Button("Quitar");
        quitar.getStyleClass().add("boton-claro");
        Label lblReceta = new Label();
        lblReceta.getStyleClass().add("seccion-ayuda");

        agregar.setOnAction(evento -> {
            Insumo insumo = cmbInsumo.getValue();
            BigDecimal cantidad = Formatos.parsearMonto(txtCantidad.getText());
            if (insumo == null || cantidad == null || cantidad.signum() <= 0) {
                lblReceta.setText("Elegí un insumo e ingresá una cantidad mayor a cero (coma para decimales).");
                return;
            }
            lblReceta.setText("");
            receta.removeIf(ingrediente -> ingrediente.getIdInsumo() == insumo.getIdInsumo());
            receta.add(new IngredienteReceta(insumo.getIdInsumo(), insumo.getNombre(), insumo.getUnidadMedida(),
                    cantidad, insumo.getStockActual()));
            txtCantidad.clear();
        });
        quitar.setOnAction(evento -> {
            IngredienteReceta elegido = tblReceta.getSelectionModel().getSelectedItem();
            if (elegido != null) {
                receta.remove(elegido);
            }
        });

        VBox bloqueReceta = new VBox(6, new Label("Receta (insumos que gasta UNA unidad)"), tblReceta,
                new HBox(8, cmbInsumo, txtCantidad, agregar, quitar), lblReceta);
        bloqueReceta.getChildren().get(0).getStyleClass().add("campo-etiqueta");
        bloqueReceta.setPadding(new Insets(6, 0, 0, 0));
        formulario.agregarAncho(bloqueReceta);

        formulario.validarCon(() -> {
            if (nombre.getText().isBlank()) {
                return "El nombre es obligatorio.";
            }
            if (categoria.getValue() == null) {
                return "Elegí la categoría.";
            }
            BigDecimal valor = Formatos.parsearMonto(precio.getText());
            if (valor == null || valor.signum() <= 0) {
                return "Ingresá un precio mayor a cero, con coma para los centavos.";
            }
            return null;
        });

        return formulario.mostrar(() -> {
            Producto producto = new Producto();
            if (!esAlta) {
                producto.setIdProducto(existente.getIdProducto());
            }
            producto.setNombre(nombre.getText());
            producto.setCategoria(categoria.getValue());
            producto.setPrecio(Formatos.parsearMonto(precio.getText()));
            producto.setDisponible(disponible.isSelected());
            return new DatosProducto(producto, new ArrayList<>(receta));
        });
    }

    // ------------------------------------------------------------------ CU04: baja lógica

    @FXML
    private void cambiarDisponibilidad() {
        Producto elegido = tblProductos.getSelectionModel().getSelectedItem();
        if (elegido == null) {
            return;
        }
        boolean nuevoEstado = !elegido.isDisponible();
        if (!nuevoEstado && !Dialogos.confirmar(tblProductos, "Dar de baja",
                "«" + elegido.getNombre() + "» dejará de ofrecerse en el menú. Se conserva en el historial de ventas "
                        + "y se puede reactivar cuando quieras.")) {
            return;
        }
        Tareas.ejecutar(
                () -> {
                    productoService.cambiarDisponibilidad(usuario, elegido, nuevoEstado);
                    return Boolean.TRUE;
                },
                ok -> cargar(),
                causa -> Dialogos.error(tblProductos, causa));
    }

    // ------------------------------------------------------------------ categorías

    @FXML
    private void administrarCategorias() {
        Tareas.ejecutar(productoService::listarCategorias, this::mostrarCategorias,
                causa -> Dialogos.error(tblProductos, causa));
    }

    private void mostrarCategorias(List<Categoria> categorias) {
        Dialog<Void> dialogo = new Dialog<>();
        dialogo.initOwner(tblProductos.getScene().getWindow());
        dialogo.setTitle("SGEG");
        dialogo.setHeaderText("Categorías del menú");
        Dialogos.aplicarEstilos(dialogo);

        ListView<Categoria> lista = new ListView<>(FXCollections.observableArrayList(categorias));
        lista.setPrefHeight(220);
        lista.setPrefWidth(300);
        Button agregar = new Button("Nueva");
        agregar.getStyleClass().add("boton-claro");
        Button renombrar = new Button("Renombrar");
        renombrar.getStyleClass().add("boton-claro");
        Button eliminar = new Button("Eliminar");
        eliminar.getStyleClass().add("boton-peligro");
        VBox botones = new VBox(8, agregar, renombrar, eliminar);
        botones.setAlignment(Pos.TOP_LEFT);
        HBox contenido = new HBox(14, lista, botones);
        dialogo.getDialogPane().setContent(contenido);
        dialogo.getDialogPane().getButtonTypes().add(new ButtonType("Cerrar", ButtonBar.ButtonData.CANCEL_CLOSE));

        Runnable recargar = () -> Tareas.ejecutar(productoService::listarCategorias,
                nuevas -> lista.getItems().setAll(nuevas),
                causa -> Dialogos.error(lista, causa));

        agregar.setOnAction(evento -> pedirNombre(lista, "Nueva categoría", "").ifPresent(nombre ->
                Tareas.ejecutar(
                        () -> {
                            productoService.crearCategoria(usuario, nombre);
                            return Boolean.TRUE;
                        },
                        ok -> recargar.run(),
                        causa -> Dialogos.error(lista, causa))));
        renombrar.setOnAction(evento -> {
            Categoria elegida = lista.getSelectionModel().getSelectedItem();
            if (elegida != null) {
                pedirNombre(lista, "Renombrar categoría", elegida.getNombre()).ifPresent(nombre ->
                        Tareas.ejecutar(
                                () -> {
                                    productoService.renombrarCategoria(usuario, elegida, nombre);
                                    return Boolean.TRUE;
                                },
                                ok -> {
                                    recargar.run();
                                    cargar();
                                },
                                causa -> Dialogos.error(lista, causa)));
            }
        });
        eliminar.setOnAction(evento -> {
            Categoria elegida = lista.getSelectionModel().getSelectedItem();
            if (elegida != null && Dialogos.confirmar(lista, "Eliminar categoría",
                    "Se eliminará la categoría «" + elegida.getNombre() + "».")) {
                Tareas.ejecutar(
                        () -> {
                            productoService.eliminarCategoria(usuario, elegida);
                            return Boolean.TRUE;
                        },
                        ok -> recargar.run(),
                        causa -> Dialogos.error(lista, causa));
            }
        });
        dialogo.showAndWait();
        cargar();
    }

    private Optional<String> pedirNombre(javafx.scene.Node origen, String titulo, String inicial) {
        TextInputDialog dialogo = new TextInputDialog(inicial);
        dialogo.initOwner(origen.getScene().getWindow());
        dialogo.setTitle("SGEG");
        dialogo.setHeaderText(titulo);
        dialogo.setContentText("Nombre:");
        return dialogo.showAndWait();
    }
}
