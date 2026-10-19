package com.sgeg.vista;

import com.sgeg.modelo.Usuario;
import com.sgeg.servicio.SesionService;
import java.util.List;
import java.util.Map;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.Label;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.VBox;

/**
 * Pantalla principal. Muestra solo los módulos habilitados para el rol del
 * usuario autenticado (RF03). Por ahora cada módulo es un marcador: se irán
 * reemplazando por las pantallas reales a medida que se implementen.
 */
public class PrincipalController {

    private enum Destino { MESAS, SEGUIMIENTO, EN_DESARROLLO }

    private record Modulo(String nombre, String descripcion, Destino destino) {

        /** Módulo que todavía no tiene pantalla propia. */
        Modulo(String nombre, String descripcion) {
            this(nombre, descripcion, Destino.EN_DESARROLLO);
        }
    }

    private static final Map<String, List<Modulo>> MODULOS_POR_ROL = Map.of(
            "Administrador", List.of(
                    new Modulo("Usuarios y roles", "Altas, bajas y permisos del personal"),
                    new Modulo("Productos y menú", "Carta, precios y disponibilidad"),
                    new Modulo("Stock e insumos", "Existencias, ingresos y alertas de mínimo"),
                    new Modulo("Reportes", "Ventas y productos más vendidos"),
                    new Modulo("Cierre de caja", "Totales de la jornada por forma de pago")),
            "Mozo", List.of(
                    new Modulo("Mesas y pedidos", "Elegir una mesa y tomar el pedido", Destino.MESAS),
                    new Modulo("Pedidos en seguimiento", "Estado de los pedidos y entrega al cliente", Destino.SEGUIMIENTO),
                    new Modulo("Menú", "Consultar los productos disponibles")),
            "Cocinero", List.of(
                    new Modulo("Pedidos en cocina", "Pendientes ordenados por antigüedad", Destino.SEGUIMIENTO),
                    new Modulo("Stock", "Consultar los insumos disponibles")),
            "Cajero", List.of(
                    new Modulo("Cobro y comprobantes", "Facturar pedidos y registrar el pago"),
                    new Modulo("Cierre de caja", "Totales de la jornada por forma de pago")));

    @FXML private Label lblUsuario;
    @FXML private Label lblRol;
    @FXML private FlowPane panelModulos;

    private Navegador navegador;
    private SesionService sesionService;
    private Usuario usuario;

    /** Lo invoca el Navegador después de cargar el FXML. */
    public void inicializar(Navegador navegador, SesionService sesionService, Usuario usuario) {
        this.navegador = navegador;
        this.sesionService = sesionService;
        this.usuario = usuario;

        String rol = usuario.getRol().getDescripcion();
        lblUsuario.setText(usuario.getNombre());
        lblRol.setText(rol);

        List<Modulo> modulos = MODULOS_POR_ROL.getOrDefault(rol, List.of());
        if (modulos.isEmpty()) {
            panelModulos.getChildren().add(new Label("Tu rol no tiene módulos habilitados. Consultá con el administrador."));
        }
        for (Modulo modulo : modulos) {
            panelModulos.getChildren().add(crearBotonModulo(modulo));
        }
    }

    @FXML
    private void cerrarSesion() {
        sesionService.cerrarSesion();
        navegador.mostrarLogin();
    }

    private Button crearBotonModulo(Modulo modulo) {
        Label titulo = new Label(modulo.nombre());
        titulo.getStyleClass().add("modulo-titulo");

        Label descripcion = new Label(modulo.descripcion());
        descripcion.getStyleClass().add("modulo-descripcion");
        descripcion.setWrapText(true);
        descripcion.setMaxWidth(220);

        VBox contenido = new VBox(6, titulo, descripcion);
        contenido.setAlignment(Pos.TOP_LEFT);

        Button boton = new Button();
        boton.setGraphic(contenido);
        boton.setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
        boton.setAlignment(Pos.TOP_LEFT);
        boton.getStyleClass().add("boton-modulo");
        boton.setOnAction(evento -> abrir(modulo));
        return boton;
    }

    private void abrir(Modulo modulo) {
        switch (modulo.destino()) {
            case MESAS -> navegador.mostrarMesas(usuario);
            case SEGUIMIENTO -> navegador.mostrarSeguimiento(usuario);
            case EN_DESARROLLO -> mostrarEnDesarrollo(modulo);
        }
    }

    private void mostrarEnDesarrollo(Modulo modulo) {
        Alert aviso = new Alert(Alert.AlertType.INFORMATION);
        aviso.initOwner(panelModulos.getScene().getWindow());
        aviso.setTitle("Módulo en desarrollo");
        aviso.setHeaderText(modulo.nombre());
        aviso.setContentText("Este módulo se implementa en la próxima etapa del proyecto.");
        aviso.showAndWait();
    }
}
