package com.sgeg;

import com.sgeg.dao.ClienteDAOImpl;
import com.sgeg.dao.MesaDAOImpl;
import com.sgeg.dao.PedidoDAOImpl;
import com.sgeg.dao.ProductoDAOImpl;
import com.sgeg.dao.UsuarioDAOImpl;
import com.sgeg.servicio.PedidoService;
import com.sgeg.servicio.SesionService;
import com.sgeg.util.ConexionBD;
import com.sgeg.vista.Navegador;
import javafx.application.Application;
import javafx.stage.Stage;

/** Punto de entrada de la aplicación JavaFX. Arma las capas y muestra el login. */
public class Main extends Application {

    @Override
    public void start(Stage stage) {
        SesionService sesionService = new SesionService(new UsuarioDAOImpl());
        PedidoService pedidoService = new PedidoService(
                new PedidoDAOImpl(), new ProductoDAOImpl(), new MesaDAOImpl(), new ClienteDAOImpl());
        Navegador navegador = new Navegador(stage, sesionService, pedidoService);

        stage.setTitle("SGEG - Gestión gastronómica");
        navegador.mostrarLogin();
        stage.show();
    }

    @Override
    public void stop() {
        ConexionBD.cerrarConexion();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
