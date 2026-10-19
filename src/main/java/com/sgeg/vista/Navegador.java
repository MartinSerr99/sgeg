package com.sgeg.vista;

import com.sgeg.modelo.Mesa;
import com.sgeg.modelo.Pedido;
import com.sgeg.modelo.Usuario;
import com.sgeg.servicio.Servicios;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URL;
import java.util.Objects;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

/**
 * Se encarga de cambiar de pantalla dentro de la única ventana de la aplicación.
 * Cada controlador recibe por parámetro lo que necesita (no accede a variables globales).
 */
public class Navegador {

    private static final String HOJA_DE_ESTILOS = "/css/estilos.css";

    private final Stage stage;
    private final Servicios servicios;

    /** Pantalla que está mostrándose, para detener sus tareas al cambiar. */
    private Object controladorActual;

    public Navegador(Stage stage, Servicios servicios) {
        this.stage = stage;
        this.servicios = servicios;
    }

    public void mostrarLogin() {
        FXMLLoader cargador = crearCargador("/vista/login.fxml");
        Parent raiz = cargar(cargador);
        LoginController controlador = cargador.getController();
        controlador.inicializar(this, servicios.sesion());
        mostrar(raiz, controlador, 820, 500, 0, 0);
    }

    public void mostrarPrincipal(Usuario usuario) {
        FXMLLoader cargador = crearCargador("/vista/principal.fxml");
        Parent raiz = cargar(cargador);
        PrincipalController controlador = cargador.getController();
        controlador.inicializar(this, servicios.sesion(), servicios.insumos(), usuario);
        mostrar(raiz, controlador, 980, 620, 760, 480);
    }

    /** Plano de mesas: punto de partida para tomar un pedido. */
    public void mostrarMesas(Usuario usuario) {
        FXMLLoader cargador = crearCargador("/vista/mesas.fxml");
        Parent raiz = cargar(cargador);
        MesasController controlador = cargador.getController();
        controlador.inicializar(this, servicios.pedidos(), usuario);
        mostrar(raiz, controlador, 980, 620, 760, 480);
    }

    /** @param mesa mesa elegida, o null para un pedido para llevar. */
    public void mostrarNuevoPedido(Usuario usuario, Mesa mesa) {
        FXMLLoader cargador = crearCargador("/vista/nuevo_pedido.fxml");
        Parent raiz = cargar(cargador);
        NuevoPedidoController controlador = cargador.getController();
        controlador.inicializar(this, servicios.pedidos(), usuario, mesa);
        mostrar(raiz, controlador, 1100, 680, 900, 560);
    }

    /** CU07: el mismo armado de pedido, pero sobre un pedido ya registrado que sigue pendiente. */
    public void mostrarModificarPedido(Usuario usuario, Pedido pedido) {
        FXMLLoader cargador = crearCargador("/vista/nuevo_pedido.fxml");
        Parent raiz = cargar(cargador);
        NuevoPedidoController controlador = cargador.getController();
        controlador.inicializarEdicion(this, servicios.pedidos(), usuario, pedido);
        mostrar(raiz, controlador, 1100, 680, 900, 560);
    }

    public void mostrarSeguimiento(Usuario usuario) {
        FXMLLoader cargador = crearCargador("/vista/seguimiento.fxml");
        Parent raiz = cargar(cargador);
        SeguimientoController controlador = cargador.getController();
        controlador.inicializar(this, servicios.pedidos(), usuario);
        mostrar(raiz, controlador, 1100, 680, 900, 560);
    }

    /** CU13/CU14: cobro de los pedidos entregados (cajero). */
    public void mostrarCobro(Usuario usuario) {
        FXMLLoader cargador = crearCargador("/vista/cobro.fxml");
        Parent raiz = cargar(cargador);
        CobroController controlador = cargador.getController();
        controlador.inicializar(this, servicios.ventas(), usuario);
        mostrar(raiz, controlador, 1100, 680, 900, 560);
    }

    /** CU15: cierre de caja (cajero y administrador). */
    public void mostrarCierreCaja(Usuario usuario) {
        FXMLLoader cargador = crearCargador("/vista/cierre_caja.fxml");
        Parent raiz = cargar(cargador);
        CierreCajaController controlador = cargador.getController();
        controlador.inicializar(this, servicios.reportes(), servicios.ventas(), usuario);
        mostrar(raiz, controlador, 1100, 680, 900, 560);
    }

    /** CU16/CU17: reportes de ventas y de productos más vendidos (administrador). */
    public void mostrarReportes(Usuario usuario) {
        FXMLLoader cargador = crearCargador("/vista/reportes.fxml");
        Parent raiz = cargar(cargador);
        ReportesController controlador = cargador.getController();
        controlador.inicializar(this, servicios.reportes(), servicios.ventas(), usuario);
        mostrar(raiz, controlador, 1100, 680, 900, 560);
    }

    /** CU03/CU04 (administrador) y CU05 (consulta del menú para el resto). */
    public void mostrarProductos(Usuario usuario) {
        FXMLLoader cargador = crearCargador("/vista/productos.fxml");
        Parent raiz = cargar(cargador);
        ProductosController controlador = cargador.getController();
        controlador.inicializar(this, servicios.productos(), usuario);
        mostrar(raiz, controlador, 1100, 680, 900, 560);
    }

    /** CU10/CU11/CU12: stock e insumos (administrador modifica, cocinero consulta). */
    public void mostrarInsumos(Usuario usuario) {
        FXMLLoader cargador = crearCargador("/vista/insumos.fxml");
        Parent raiz = cargar(cargador);
        InsumosController controlador = cargador.getController();
        controlador.inicializar(this, servicios.insumos(), usuario);
        mostrar(raiz, controlador, 1100, 680, 900, 560);
    }

    /** CU02: usuarios y roles (administrador). */
    public void mostrarUsuarios(Usuario usuario) {
        FXMLLoader cargador = crearCargador("/vista/usuarios.fxml");
        Parent raiz = cargar(cargador);
        UsuariosController controlador = cargador.getController();
        controlador.inicializar(this, servicios.usuarios(), usuario);
        mostrar(raiz, controlador, 1000, 640, 800, 520);
    }

    private void mostrar(Parent raiz, Object controlador, double ancho, double alto, double minAncho, double minAlto) {
        detenerPantallaActual();
        controladorActual = controlador;

        Scene escena = new Scene(raiz, ancho, alto);
        URL estilos = Objects.requireNonNull(Navegador.class.getResource(HOJA_DE_ESTILOS),
                "No se encontró " + HOJA_DE_ESTILOS);
        escena.getStylesheets().add(estilos.toExternalForm());

        stage.setMinWidth(minAncho);
        stage.setMinHeight(minAlto);
        stage.setScene(escena);
        stage.sizeToScene();
        stage.centerOnScreen();
    }

    private void detenerPantallaActual() {
        if (controladorActual instanceof Detenible detenible) {
            detenible.detener();
        }
        controladorActual = null;
    }

    private FXMLLoader crearCargador(String ruta) {
        URL recurso = Navegador.class.getResource(ruta);
        if (recurso == null) {
            throw new IllegalStateException("No se encontró la pantalla " + ruta);
        }
        return new FXMLLoader(recurso);
    }

    private Parent cargar(FXMLLoader cargador) {
        try {
            return cargador.load();
        } catch (IOException e) {
            throw new UncheckedIOException("No se pudo cargar la pantalla", e);
        }
    }
}
