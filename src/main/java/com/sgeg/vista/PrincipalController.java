package com.sgeg.vista;

import com.sgeg.modelo.Usuario;
import com.sgeg.servicio.InsumoService;
import com.sgeg.servicio.SesionService;
import java.util.List;
import java.util.Map;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.Label;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

/**
 * Pantalla principal. Muestra solo los módulos habilitados para el rol del
 * usuario autenticado (RF03). Para el administrador y el cocinero avisa, al entrar,
 * si hay insumos en o por debajo de su stock mínimo (CU12).
 */
public class PrincipalController {

    private enum Destino { MESAS, SEGUIMIENTO, COBRO, CIERRE_CAJA, REPORTES, PRODUCTOS, INSUMOS, USUARIOS }

    private record Modulo(String nombre, String descripcion, Destino destino) {
    }

    private static final Map<String, List<Modulo>> MODULOS_POR_ROL = Map.of(
            "Administrador", List.of(
                    new Modulo("Usuarios y roles", "Altas, bajas y permisos del personal", Destino.USUARIOS),
                    new Modulo("Productos y menú", "Carta, precios, recetas y disponibilidad", Destino.PRODUCTOS),
                    new Modulo("Stock e insumos", "Existencias, ingresos y alertas de mínimo", Destino.INSUMOS),
                    new Modulo("Reportes", "Ventas y productos más vendidos", Destino.REPORTES),
                    new Modulo("Cierre de caja", "Totales de la jornada por forma de pago", Destino.CIERRE_CAJA)),
            "Mozo", List.of(
                    new Modulo("Mesas y pedidos", "Elegir una mesa y tomar el pedido", Destino.MESAS),
                    new Modulo("Pedidos en seguimiento", "Estado, modificación y entrega al cliente", Destino.SEGUIMIENTO),
                    new Modulo("Menú", "Consultar los productos disponibles", Destino.PRODUCTOS)),
            "Cocinero", List.of(
                    new Modulo("Pedidos en cocina", "Pendientes ordenados por antigüedad", Destino.SEGUIMIENTO),
                    new Modulo("Stock", "Consultar los insumos disponibles", Destino.INSUMOS)),
            "Cajero", List.of(
                    new Modulo("Cobro y comprobantes", "Facturar pedidos y registrar el pago", Destino.COBRO),
                    new Modulo("Cierre de caja", "Totales de la jornada por forma de pago", Destino.CIERRE_CAJA)));

    @FXML private Label lblUsuario;
    @FXML private Label lblRol;
    @FXML private FlowPane panelModulos;
    @FXML private HBox avisoStock;
    @FXML private Label lblAvisoStock;

    private Navegador navegador;
    private SesionService sesionService;
    private InsumoService insumoService;
    private Usuario usuario;

    /** Lo invoca el Navegador después de cargar el FXML. */
    public void inicializar(Navegador navegador, SesionService sesionService, InsumoService insumoService, Usuario usuario) {
        this.navegador = navegador;
        this.sesionService = sesionService;
        this.insumoService = insumoService;
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
        if (insumoService.puedeConsultar(usuario)) {
            consultarAlertaDeStock();
        }
    }

    /** CU12: si hay insumos en o por debajo del mínimo, lo avisa al entrar. Si la consulta falla, simplemente no avisa. */
    private void consultarAlertaDeStock() {
        Tareas.ejecutar(
                insumoService::contarBajoMinimo,
                cantidad -> {
                    if (cantidad > 0) {
                        lblAvisoStock.setText(cantidad == 1
                                ? "Alerta de stock: 1 insumo está en o por debajo de su mínimo."
                                : "Alerta de stock: " + cantidad + " insumos están en o por debajo de su mínimo.");
                        avisoStock.setVisible(true);
                        avisoStock.setManaged(true);
                    }
                },
                causa -> { });
    }

    @FXML
    private void verStock() {
        navegador.mostrarInsumos(usuario);
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
            case COBRO -> navegador.mostrarCobro(usuario);
            case CIERRE_CAJA -> navegador.mostrarCierreCaja(usuario);
            case REPORTES -> navegador.mostrarReportes(usuario);
            case PRODUCTOS -> navegador.mostrarProductos(usuario);
            case INSUMOS -> navegador.mostrarInsumos(usuario);
            case USUARIOS -> navegador.mostrarUsuarios(usuario);
        }
    }
}
