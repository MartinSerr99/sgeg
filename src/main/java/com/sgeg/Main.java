package com.sgeg;

import com.sgeg.dao.CategoriaDAOImpl;
import com.sgeg.dao.ClienteDAOImpl;
import com.sgeg.dao.InsumoDAOImpl;
import com.sgeg.dao.MesaDAOImpl;
import com.sgeg.dao.PedidoDAOImpl;
import com.sgeg.dao.ProductoDAOImpl;
import com.sgeg.dao.ReporteDAOImpl;
import com.sgeg.dao.UsuarioDAOImpl;
import com.sgeg.dao.VentaDAOImpl;
import com.sgeg.servicio.InsumoService;
import com.sgeg.servicio.PedidoService;
import com.sgeg.servicio.ProductoService;
import com.sgeg.servicio.ReporteService;
import com.sgeg.servicio.Servicios;
import com.sgeg.servicio.SesionService;
import com.sgeg.servicio.UsuarioService;
import com.sgeg.servicio.VentaService;
import com.sgeg.util.ConexionBD;
import com.sgeg.vista.Navegador;
import javafx.application.Application;
import javafx.stage.Stage;

/** Punto de entrada de la aplicación JavaFX. Arma las capas y muestra el login. */
public class Main extends Application {

    @Override
    public void start(Stage stage) {
        // Se arman todas las capas acá: DAO -> servicios -> pantallas.
        UsuarioDAOImpl usuarioDAO = new UsuarioDAOImpl();
        PedidoDAOImpl pedidoDAO = new PedidoDAOImpl();
        ProductoDAOImpl productoDAO = new ProductoDAOImpl();
        InsumoDAOImpl insumoDAO = new InsumoDAOImpl();
        VentaDAOImpl ventaDAO = new VentaDAOImpl();

        Servicios servicios = new Servicios(
                new SesionService(usuarioDAO),
                new PedidoService(pedidoDAO, productoDAO, new MesaDAOImpl(), new ClienteDAOImpl()),
                new VentaService(ventaDAO, pedidoDAO),
                new ReporteService(new ReporteDAOImpl(), pedidoDAO),
                new ProductoService(productoDAO, new CategoriaDAOImpl(), insumoDAO),
                new InsumoService(insumoDAO),
                new UsuarioService(usuarioDAO));
        Navegador navegador = new Navegador(stage, servicios);

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
