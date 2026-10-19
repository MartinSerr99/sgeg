package com.sgeg.vista;

import com.sgeg.modelo.Usuario;
import com.sgeg.servicio.SesionService;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.TextField;

/** Pantalla de inicio de sesión (CU01). */
public class LoginController {

    private static final Logger LOG = Logger.getLogger(LoginController.class.getName());

    @FXML private TextField txtUsuario;
    @FXML private PasswordField txtContrasenia;
    @FXML private Button btnIngresar;
    @FXML private Label lblMensaje;
    @FXML private ProgressIndicator indicador;

    private Navegador navegador;
    private SesionService sesionService;

    /** Lo invoca el Navegador después de cargar el FXML. */
    public void inicializar(Navegador navegador, SesionService sesionService) {
        this.navegador = navegador;
        this.sesionService = sesionService;
        Platform.runLater(() -> txtUsuario.requestFocus());
    }

    @FXML
    private void iniciarSesion() {
        String usuario = txtUsuario.getText().trim();
        String contrasenia = txtContrasenia.getText();

        if (usuario.isEmpty() || contrasenia.isEmpty()) {
            mostrarError("Completá el usuario y la contraseña.");
            return;
        }

        establecerOcupado(true);

        // La autenticación consulta la base y calcula un hash: se hace fuera del
        // hilo de la interfaz para que la ventana no se congele.
        Task<Usuario> tarea = new Task<>() {
            @Override
            protected Usuario call() {
                return sesionService.autenticar(usuario, contrasenia);
            }
        };

        tarea.setOnSucceeded(evento -> {
            establecerOcupado(false);
            Usuario autenticado = tarea.getValue();
            if (autenticado == null) {
                txtContrasenia.clear();
                txtContrasenia.requestFocus();
                mostrarError("Usuario o contraseña incorrectos. Revisá los datos e intentá de nuevo.");
            } else {
                navegador.mostrarPrincipal(autenticado);
            }
        });

        tarea.setOnFailed(evento -> {
            establecerOcupado(false);
            LOG.log(Level.SEVERE, "Falló la autenticación", tarea.getException());
            mostrarError("No se pudo consultar la base de datos. Verificá que MySQL esté en ejecución "
                    + "y que los datos de config.properties sean correctos.");
        });

        Thread hilo = new Thread(tarea, "autenticacion");
        hilo.setDaemon(true);
        hilo.start();
    }

    private void establecerOcupado(boolean ocupado) {
        txtUsuario.setDisable(ocupado);
        txtContrasenia.setDisable(ocupado);
        btnIngresar.setDisable(ocupado);
        indicador.setVisible(ocupado);
        indicador.setManaged(ocupado);
        if (ocupado) {
            lblMensaje.setVisible(false);
            lblMensaje.setManaged(false);
        }
    }

    private void mostrarError(String mensaje) {
        lblMensaje.setText(mensaje);
        lblMensaje.setVisible(true);
        lblMensaje.setManaged(true);
    }
}
