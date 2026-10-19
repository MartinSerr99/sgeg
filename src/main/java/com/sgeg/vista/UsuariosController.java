package com.sgeg.vista;

import com.sgeg.modelo.Rol;
import com.sgeg.modelo.Usuario;
import com.sgeg.servicio.UsuarioService;
import java.util.List;
import java.util.Optional;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;

/** Usuarios y roles (CU02). Solo para el administrador. */
public class UsuariosController {

    private record Datos(List<Usuario> usuarios, List<Rol> roles) {
    }

    @FXML private Button btnEditar;
    @FXML private Button btnClave;
    @FXML private Button btnEstado;
    @FXML private TableView<Usuario> tblUsuarios;
    @FXML private TableColumn<Usuario, String> colNombre;
    @FXML private TableColumn<Usuario, String> colUsuario;
    @FXML private TableColumn<Usuario, String> colRol;
    @FXML private TableColumn<Usuario, String> colEstado;

    private final ObservableList<Usuario> usuarios = FXCollections.observableArrayList();

    private Navegador navegador;
    private UsuarioService usuarioService;
    private Usuario usuario;
    private List<Rol> roles = List.of();

    /** Lo invoca el Navegador después de cargar el FXML. */
    public void inicializar(Navegador navegador, UsuarioService usuarioService, Usuario usuario) {
        this.navegador = navegador;
        this.usuarioService = usuarioService;
        this.usuario = usuario;

        colNombre.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getNombre()));
        colUsuario.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getNombreUsuario()));
        colRol.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getRol().getDescripcion()));
        colEstado.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().isActivo() ? "Activo" : "Dado de baja"));
        tblUsuarios.setItems(usuarios);
        tblUsuarios.setPlaceholder(new Label("No hay usuarios."));
        tblUsuarios.getSelectionModel().selectedItemProperty().addListener((obs, anterior, actual) -> actualizarBotones());

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
        Usuario elegido = tblUsuarios.getSelectionModel().getSelectedItem();
        int idElegido = elegido == null ? -1 : elegido.getIdUsuario();
        Tareas.ejecutar(
                () -> new Datos(usuarioService.listar(), usuarioService.listarRoles()),
                datos -> {
                    roles = datos.roles();
                    usuarios.setAll(datos.usuarios());
                    for (Usuario candidato : datos.usuarios()) {
                        if (candidato.getIdUsuario() == idElegido) {
                            tblUsuarios.getSelectionModel().select(candidato);
                        }
                    }
                    actualizarBotones();
                },
                causa -> Dialogos.error(tblUsuarios, causa));
    }

    private void actualizarBotones() {
        Usuario elegido = tblUsuarios.getSelectionModel().getSelectedItem();
        btnEditar.setDisable(elegido == null);
        btnClave.setDisable(elegido == null);
        btnEstado.setDisable(elegido == null);
        btnEstado.setText(elegido != null && !elegido.isActivo() ? "Reactivar" : "Dar de baja");
    }

    // ------------------------------------------------------------------ alta

    @FXML
    private void nuevo() {
        record Alta(Usuario usuario, String contrasenia) {
        }
        FormularioDialogo<Alta> formulario = new FormularioDialogo<>(tblUsuarios, "Nuevo usuario", "Crear");
        TextField nombre = formulario.agregar("Nombre y apellido", new TextField());
        TextField acceso = formulario.agregar("Usuario de acceso", new TextField());
        ComboBox<Rol> rol = formulario.agregar("Rol", new ComboBox<>());
        rol.getItems().setAll(roles);
        PasswordField clave = formulario.agregar("Contraseña", new PasswordField());
        PasswordField repetir = formulario.agregar("Repetir contraseña", new PasswordField());
        formulario.validarCon(() -> {
            if (nombre.getText().isBlank() || acceso.getText().isBlank()) {
                return "Completá el nombre y el usuario de acceso.";
            }
            if (rol.getValue() == null) {
                return "Elegí el rol.";
            }
            if (!clave.getText().equals(repetir.getText())) {
                return "Las contraseñas no coinciden.";
            }
            return null;
        });

        Optional<Alta> alta = formulario.mostrar(() -> {
            Usuario nuevo = new Usuario();
            nuevo.setNombre(nombre.getText());
            nuevo.setNombreUsuario(acceso.getText());
            nuevo.setRol(rol.getValue());
            return new Alta(nuevo, clave.getText());
        });
        alta.ifPresent(datos -> Tareas.ejecutar(
                () -> {
                    usuarioService.crear(usuario, datos.usuario(), datos.contrasenia());
                    return Boolean.TRUE;
                },
                ok -> cargar(),
                causa -> Dialogos.error(tblUsuarios, causa)));
    }

    // ------------------------------------------------------------------ edición

    @FXML
    private void editar() {
        Usuario elegido = tblUsuarios.getSelectionModel().getSelectedItem();
        if (elegido == null) {
            return;
        }
        FormularioDialogo<Usuario> formulario = new FormularioDialogo<>(tblUsuarios,
                "Editar a " + elegido.getNombreUsuario(), "Guardar");
        TextField nombre = formulario.agregar("Nombre y apellido", new TextField(elegido.getNombre()));
        ComboBox<Rol> rol = formulario.agregar("Rol", new ComboBox<>());
        rol.getItems().setAll(roles);
        for (Rol candidato : roles) {
            if (candidato.getIdRol() == elegido.getRol().getIdRol()) {
                rol.setValue(candidato);
            }
        }
        formulario.validarCon(() -> nombre.getText().isBlank() ? "El nombre es obligatorio."
                : rol.getValue() == null ? "Elegí el rol." : null);

        formulario.mostrar(() -> new Usuario(elegido.getIdUsuario(), nombre.getText(), elegido.getNombreUsuario(),
                elegido.getContraseniaHash(), rol.getValue(), elegido.isActivo()))
                .ifPresent(editado -> Tareas.ejecutar(
                        () -> {
                            usuarioService.modificar(usuario, editado);
                            return Boolean.TRUE;
                        },
                        ok -> cargar(),
                        causa -> Dialogos.error(tblUsuarios, causa)));
    }

    @FXML
    private void cambiarContrasenia() {
        Usuario elegido = tblUsuarios.getSelectionModel().getSelectedItem();
        if (elegido == null) {
            return;
        }
        FormularioDialogo<String> formulario = new FormularioDialogo<>(tblUsuarios,
                "Nueva contraseña para " + elegido.getNombreUsuario(), "Cambiar");
        PasswordField clave = formulario.agregar("Nueva contraseña", new PasswordField());
        PasswordField repetir = formulario.agregar("Repetir contraseña", new PasswordField());
        formulario.validarCon(() -> clave.getText().equals(repetir.getText()) ? null : "Las contraseñas no coinciden.");

        formulario.mostrar(clave::getText).ifPresent(nueva -> Tareas.ejecutar(
                () -> {
                    usuarioService.cambiarContrasenia(usuario, elegido, nueva);
                    return Boolean.TRUE;
                },
                ok -> Dialogos.informar(tblUsuarios, "Contraseña actualizada",
                        "La nueva contraseña de «" + elegido.getNombreUsuario() + "» ya está en uso."),
                causa -> Dialogos.error(tblUsuarios, causa)));
    }

    // ------------------------------------------------------------------ baja lógica

    @FXML
    private void cambiarEstado() {
        Usuario elegido = tblUsuarios.getSelectionModel().getSelectedItem();
        if (elegido == null) {
            return;
        }
        boolean activar = !elegido.isActivo();
        if (!activar && !Dialogos.confirmar(tblUsuarios, "Dar de baja",
                "«" + elegido.getNombre() + "» ya no podrá iniciar sesión. Se conserva su historial y se puede reactivar.")) {
            return;
        }
        Tareas.ejecutar(
                () -> {
                    usuarioService.cambiarActivo(usuario, elegido, activar);
                    return Boolean.TRUE;
                },
                ok -> cargar(),
                causa -> Dialogos.error(tblUsuarios, causa));
    }
}
