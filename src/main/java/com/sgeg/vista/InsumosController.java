package com.sgeg.vista;

import com.sgeg.modelo.Insumo;
import com.sgeg.modelo.Usuario;
import com.sgeg.servicio.InsumoService;
import com.sgeg.util.Formatos;
import java.math.BigDecimal;
import java.util.List;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;

/**
 * Stock e insumos. El administrador da de alta insumos, registra ingresos de stock (CU10) y ve la
 * alerta de mínimos (CU12); el cocinero solo consulta (CU11).
 */
public class InsumosController {

    private static final String[] UNIDADES = {"kg", "g", "litro", "ml", "unidad"};

    @FXML private Label lblTitulo;
    @FXML private HBox avisoStock;
    @FXML private Label lblAvisoStock;
    @FXML private HBox acciones;
    @FXML private Button btnIngreso;
    @FXML private Button btnEditar;
    @FXML private TableView<Insumo> tblInsumos;
    @FXML private TableColumn<Insumo, String> colNombre;
    @FXML private TableColumn<Insumo, String> colStock;
    @FXML private TableColumn<Insumo, String> colMinimo;
    @FXML private TableColumn<Insumo, String> colUnidad;
    @FXML private TableColumn<Insumo, String> colEstado;

    private final ObservableList<Insumo> insumos = FXCollections.observableArrayList();

    private Navegador navegador;
    private InsumoService insumoService;
    private Usuario usuario;

    /** Lo invoca el Navegador después de cargar el FXML. */
    public void inicializar(Navegador navegador, InsumoService insumoService, Usuario usuario) {
        this.navegador = navegador;
        this.insumoService = insumoService;
        this.usuario = usuario;

        boolean puedeModificar = insumoService.puedeModificar(usuario);
        acciones.setVisible(puedeModificar);
        acciones.setManaged(puedeModificar);
        lblTitulo.setText(puedeModificar ? "Stock e insumos" : "Stock de insumos");

        colNombre.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getNombre()));
        colStock.setCellValueFactory(c -> new SimpleStringProperty(Formatos.numero(c.getValue().getStockActual())));
        colMinimo.setCellValueFactory(c -> new SimpleStringProperty(Formatos.numero(c.getValue().getStockMinimo())));
        colUnidad.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getUnidadMedida()));
        colEstado.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getEstado()));
        colEstado.setCellFactory(columna -> new TableCell<Insumo, String>() {
            @Override
            protected void updateItem(String estado, boolean vacia) {
                super.updateItem(estado, vacia);
                getStyleClass().removeAll("stock-ok", "stock-bajo", "stock-agotado");
                if (vacia || estado == null) {
                    setText(null);
                    return;
                }
                setText(estado);
                getStyleClass().add("Agotado".equals(estado) ? "stock-agotado"
                        : "Normal".equals(estado) ? "stock-ok" : "stock-bajo");
            }
        });
        tblInsumos.setItems(insumos);
        tblInsumos.setPlaceholder(new Label("Todavía no hay insumos cargados."));
        tblInsumos.getSelectionModel().selectedItemProperty().addListener((obs, anterior, actual) -> actualizarBotones());

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

    private void cargar() {
        Insumo elegido = tblInsumos.getSelectionModel().getSelectedItem();
        int idElegido = elegido == null ? -1 : elegido.getIdInsumo();
        Tareas.ejecutar(
                insumoService::listar,
                lista -> {
                    insumos.setAll(lista);
                    for (Insumo insumo : lista) {
                        if (insumo.getIdInsumo() == idElegido) {
                            tblInsumos.getSelectionModel().select(insumo);
                        }
                    }
                    mostrarAviso(lista);
                    actualizarBotones();
                },
                causa -> Dialogos.error(tblInsumos, causa));
    }

    /** CU12: avisa qué insumos llegaron a su mínimo. */
    private void mostrarAviso(List<Insumo> lista) {
        StringBuilder nombres = new StringBuilder();
        int cantidad = 0;
        for (Insumo insumo : lista) {
            if (insumo.estaBajoMinimo()) {
                if (cantidad > 0) {
                    nombres.append(", ");
                }
                nombres.append(insumo.getNombre());
                cantidad++;
            }
        }
        avisoStock.getStyleClass().removeAll("aviso-alerta", "aviso-ok");
        if (cantidad == 0) {
            avisoStock.getStyleClass().add("aviso-ok");
            lblAvisoStock.setText("Todos los insumos están por encima de su stock mínimo.");
        } else {
            avisoStock.getStyleClass().add("aviso-alerta");
            lblAvisoStock.setText((cantidad == 1 ? "1 insumo está en o por debajo de su stock mínimo: "
                    : cantidad + " insumos están en o por debajo de su stock mínimo: ") + nombres + ".");
        }
    }

    private void actualizarBotones() {
        boolean haySeleccion = tblInsumos.getSelectionModel().getSelectedItem() != null;
        btnIngreso.setDisable(!haySeleccion);
        btnEditar.setDisable(!haySeleccion);
    }

    // ------------------------------------------------------------------ CU10: ingreso de stock

    @FXML
    private void registrarIngreso() {
        Insumo elegido = tblInsumos.getSelectionModel().getSelectedItem();
        if (elegido == null) {
            return;
        }
        FormularioDialogo<BigDecimal> formulario = new FormularioDialogo<>(tblInsumos,
                "Registrar ingreso de «" + elegido.getNombre() + "»", "Registrar");
        formulario.agregar("Stock actual",
                new Label(Formatos.numero(elegido.getStockActual()) + " " + elegido.getUnidadMedida()));
        TextField cantidad = formulario.agregar("Cantidad que ingresa (" + elegido.getUnidadMedida() + ")", new TextField());
        cantidad.setPromptText("Ej.: 12,5");
        formulario.validarCon(() -> {
            BigDecimal valor = Formatos.parsearMonto(cantidad.getText());
            return valor == null || valor.signum() <= 0
                    ? "Ingresá una cantidad mayor a cero, con coma para los decimales (máximo dos)." : null;
        });

        formulario.mostrar(() -> Formatos.parsearMonto(cantidad.getText())).ifPresent(valor ->
                Tareas.ejecutar(
                        () -> {
                            insumoService.registrarIngreso(usuario, elegido, valor);
                            return Boolean.TRUE;
                        },
                        ok -> cargar(),
                        causa -> Dialogos.error(tblInsumos, causa)));
    }

    // ------------------------------------------------------------------ alta y modificación

    @FXML
    private void nuevo() {
        pedirDatos(null).ifPresent(insumo ->
                Tareas.ejecutar(
                        () -> {
                            insumoService.crear(usuario, insumo);
                            return Boolean.TRUE;
                        },
                        ok -> cargar(),
                        causa -> Dialogos.error(tblInsumos, causa)));
    }

    @FXML
    private void editar() {
        Insumo elegido = tblInsumos.getSelectionModel().getSelectedItem();
        if (elegido == null) {
            return;
        }
        pedirDatos(elegido).ifPresent(insumo ->
                Tareas.ejecutar(
                        () -> {
                            insumoService.modificar(usuario, insumo);
                            return Boolean.TRUE;
                        },
                        ok -> cargar(),
                        causa -> Dialogos.error(tblInsumos, causa)));
    }

    /** Formulario de alta (existente == null) o de edición. En la edición no se toca el stock actual. */
    private java.util.Optional<Insumo> pedirDatos(Insumo existente) {
        boolean esAlta = existente == null;
        FormularioDialogo<Insumo> formulario = new FormularioDialogo<>(tblInsumos,
                esAlta ? "Nuevo insumo" : "Editar «" + existente.getNombre() + "»", "Guardar");
        TextField nombre = formulario.agregar("Nombre", new TextField(esAlta ? "" : existente.getNombre()));
        ComboBox<String> unidad = formulario.agregar("Unidad de medida", new ComboBox<>());
        unidad.getItems().setAll(UNIDADES);
        unidad.setEditable(true);
        unidad.setValue(esAlta ? "kg" : existente.getUnidadMedida());
        TextField stockInicial = null;
        if (esAlta) {
            stockInicial = formulario.agregar("Stock inicial", new TextField("0"));
        }
        TextField minimo = formulario.agregar("Stock mínimo (alerta)",
                new TextField(esAlta ? "0" : Formatos.numero(existente.getStockMinimo()).replace(".", "")));
        final TextField campoStockInicial = stockInicial;

        formulario.validarCon(() -> {
            if (nombre.getText().isBlank()) {
                return "El nombre es obligatorio.";
            }
            if (unidad.getValue() == null || unidad.getValue().isBlank()) {
                return "Indicá la unidad de medida.";
            }
            if (campoStockInicial != null && Formatos.parsearMonto(campoStockInicial.getText()) == null) {
                return "El stock inicial no es un número válido (máximo dos decimales).";
            }
            if (Formatos.parsearMonto(minimo.getText()) == null) {
                return "El stock mínimo no es un número válido (máximo dos decimales).";
            }
            return null;
        });

        return formulario.mostrar(() -> {
            Insumo insumo = esAlta ? new Insumo() : new Insumo(existente.getIdInsumo(), null,
                    existente.getStockActual(), null, null);
            insumo.setNombre(nombre.getText());
            insumo.setUnidadMedida(unidad.getValue());
            insumo.setStockMinimo(Formatos.parsearMonto(minimo.getText()));
            if (campoStockInicial != null) {
                insumo.setStockActual(Formatos.parsearMonto(campoStockInicial.getText()));
            }
            return insumo;
        });
    }
}
