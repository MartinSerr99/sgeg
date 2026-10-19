package com.sgeg.servicio;

import com.sgeg.dao.IUsuarioDAO;
import com.sgeg.excepcion.NegocioException;
import com.sgeg.modelo.Rol;
import com.sgeg.modelo.Usuario;
import com.sgeg.util.SeguridadUtil;
import java.util.List;
import java.util.regex.Pattern;

/** Gestión de usuarios y roles (CU02): alta, edición, cambio de contraseña y baja lógica. */
public class UsuarioService {

    static final int LARGO_MINIMO_CONTRASENIA = 6;
    private static final Pattern NOMBRE_USUARIO = Pattern.compile("[A-Za-z0-9._-]{3,50}");

    private final IUsuarioDAO usuarioDAO;

    public UsuarioService(IUsuarioDAO usuarioDAO) {
        this.usuarioDAO = usuarioDAO;
    }

    public List<Usuario> listar() {
        return usuarioDAO.listarTodos();
    }

    public List<Rol> listarRoles() {
        return usuarioDAO.listarRoles();
    }

    /** RF04: alta de un usuario con su rol. La contraseña se guarda siempre con hash, nunca en texto plano. */
    public void crear(Usuario administrador, Usuario nuevo, String contrasenia) {
        exigirAdministrador(administrador);
        validarNombre(nuevo.getNombre());
        nuevo.setNombre(nuevo.getNombre().trim());
        String nombreUsuario = nuevo.getNombreUsuario() == null ? "" : nuevo.getNombreUsuario().trim();
        if (!NOMBRE_USUARIO.matcher(nombreUsuario).matches()) {
            throw new NegocioException("El usuario debe tener entre 3 y 50 caracteres: letras, números, punto, guion o guion bajo.");
        }
        nuevo.setNombreUsuario(nombreUsuario);
        if (nuevo.getRol() == null) {
            throw new NegocioException("Elegí el rol del usuario.");
        }
        validarContrasenia(contrasenia);
        if (usuarioDAO.buscarPorNombreUsuario(nombreUsuario) != null) {
            throw new NegocioException("Ya existe un usuario «" + nombreUsuario + "». Elegí otro nombre de acceso.");
        }
        nuevo.setContraseniaHash(SeguridadUtil.generarHash(contrasenia));
        nuevo.setActivo(true);
        usuarioDAO.guardar(nuevo);
    }

    /** Cambia nombre y rol. No se puede cambiar el propio rol ni dejar el sistema sin administrador. */
    public void modificar(Usuario administrador, Usuario usuario) {
        exigirAdministrador(administrador);
        validarNombre(usuario.getNombre());
        usuario.setNombre(usuario.getNombre().trim());
        if (usuario.getRol() == null) {
            throw new NegocioException("Elegí el rol del usuario.");
        }
        Usuario actual = buscarPorId(usuario.getIdUsuario());
        boolean cambiaRol = actual.getRol().getIdRol() != usuario.getRol().getIdRol();
        if (cambiaRol && administrador.getIdUsuario() == usuario.getIdUsuario()) {
            throw new NegocioException("No podés cambiar tu propio rol. Pedile a otro administrador que lo haga.");
        }
        if (cambiaRol && actual.tieneRol(Rol.ADMINISTRADOR) && actual.isActivo()) {
            exigirOtroAdministrador("cambiar el rol de");
        }
        usuarioDAO.actualizar(usuario);
    }

    /** RF05: baja lógica (o reactivación). Un usuario dado de baja no puede iniciar sesión pero se conserva su historial. */
    public void cambiarActivo(Usuario administrador, Usuario usuario, boolean activo) {
        exigirAdministrador(administrador);
        if (!activo) {
            if (administrador.getIdUsuario() == usuario.getIdUsuario()) {
                throw new NegocioException("No podés darte de baja a vos mismo.");
            }
            if (usuario.tieneRol(Rol.ADMINISTRADOR) && usuario.isActivo()) {
                exigirOtroAdministrador("dar de baja a");
            }
        }
        usuarioDAO.cambiarActivo(usuario.getIdUsuario(), activo);
    }

    public void cambiarContrasenia(Usuario administrador, Usuario usuario, String nuevaContrasenia) {
        exigirAdministrador(administrador);
        validarContrasenia(nuevaContrasenia);
        usuarioDAO.cambiarContrasenia(usuario.getIdUsuario(), SeguridadUtil.generarHash(nuevaContrasenia));
    }

    // ------------------------------------------------------------------ auxiliares

    private Usuario buscarPorId(int idUsuario) {
        for (Usuario usuario : usuarioDAO.listarTodos()) {
            if (usuario.getIdUsuario() == idUsuario) {
                return usuario;
            }
        }
        throw new NegocioException("El usuario ya no existe. Actualizá la lista.");
    }

    private void exigirOtroAdministrador(String accion) {
        if (usuarioDAO.contarAdministradoresActivos() <= 1) {
            throw new NegocioException("No se puede " + accion + " el único administrador activo: el sistema quedaría sin administración.");
        }
    }

    private static void exigirAdministrador(Usuario usuario) {
        if (usuario == null || !usuario.tieneRol(Rol.ADMINISTRADOR)) {
            throw new NegocioException("Solo el administrador puede gestionar usuarios.");
        }
    }

    private static void validarNombre(String nombre) {
        if (nombre == null || nombre.isBlank()) {
            throw new NegocioException("El nombre y apellido es obligatorio.");
        }
        if (nombre.trim().length() > 100) {
            throw new NegocioException("El nombre admite hasta 100 caracteres.");
        }
    }

    private static void validarContrasenia(String contrasenia) {
        if (contrasenia == null || contrasenia.length() < LARGO_MINIMO_CONTRASENIA) {
            throw new NegocioException("La contraseña debe tener al menos " + LARGO_MINIMO_CONTRASENIA + " caracteres.");
        }
    }
}
