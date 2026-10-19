package com.sgeg.vista;

import com.sgeg.modelo.Mesa;
import com.sgeg.modelo.Usuario;
import com.sgeg.servicio.PedidoService;
import com.sgeg.servicio.SesionService;
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
    private final SesionService sesionService;
    private final PedidoService pedidoService;

    /** Pantalla que está mostrándose, para detener sus tareas al cambiar. */
    private Object controladorActual;

    public Navegador(Stage stage, SesionService sesionService, PedidoService pedidoService) {
        this.stage = stage;
        this.sesionService = sesionService;
        this.pedidoService = pedidoService;
    }

    public void mostrarLogin() {
        FXMLLoader cargador = crearCargador("/vista/login.fxml");
        Parent raiz = cargar(cargador);
        LoginController controlador = cargador.getController();
        controlador.inicializar(this, sesionService);
        mostrar(raiz, controlador, 820, 500, 0, 0);
    }

    public void mostrarPrincipal(Usuario usuario) {
        FXMLLoader cargador = crearCargador("/vista/principal.fxml");
        Parent raiz = cargar(cargador);
        PrincipalController controlador = cargador.getController();
        controlador.inicializar(this, sesionService, usuario);
        mostrar(raiz, controlador, 980, 620, 760, 480);
    }

    /** Plano de mesas: punto de partida para tomar un pedido. */
    public void mostrarMesas(Usuario usuario) {
        FXMLLoader cargador = crearCargador("/vista/mesas.fxml");
        Parent raiz = cargar(cargador);
        MesasController controlador = cargador.getController();
        controlador.inicializar(this, pedidoService, usuario);
        mostrar(raiz, controlador, 980, 620, 760, 480);
    }

    /** @param mesa mesa elegida, o null para un pedido para llevar. */
    public void mostrarNuevoPedido(Usuario usuario, Mesa mesa) {
        FXMLLoader cargador = crearCargador("/vista/nuevo_pedido.fxml");
        Parent raiz = cargar(cargador);
        NuevoPedidoController controlador = cargador.getController();
        controlador.inicializar(this, pedidoService, usuario, mesa);
        mostrar(raiz, controlador, 1100, 680, 900, 560);
    }

    public void mostrarSeguimiento(Usuario usuario) {
        FXMLLoader cargador = crearCargador("/vista/seguimiento.fxml");
        Parent raiz = cargar(cargador);
        SeguimientoController controlador = cargador.getController();
        controlador.inicializar(this, pedidoService, usuario);
        mostrar(raiz, controlador, 1100, 680, 900, 560);
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
