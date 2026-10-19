package com.sgeg.servicio;

import com.sgeg.dao.IUsuarioDAO;
import com.sgeg.modelo.Usuario;
import com.sgeg.util.SeguridadUtil;
import java.util.logging.Logger;

/** Lógica de negocio de la sesión: autenticación (CU01) y cierre de sesión (RF06). */
public class SesionService {

    private static final Logger LOG = Logger.getLogger(SesionService.class.getName());

    private final IUsuarioDAO usuarioDAO;
    private volatile Usuario usuarioActual;

    public SesionService(IUsuarioDAO usuarioDAO) {
        this.usuarioDAO = usuarioDAO;
    }

    /**
     * Valida las credenciales (RF01, RF02).
     *
     * @return el usuario autenticado, o null si las credenciales no son válidas
     *         o la cuenta está dada de baja.
     */
    public Usuario autenticar(String nombreUsuario, String contrasenia) {
        Usuario usuario = usuarioDAO.buscarPorNombreUsuario(nombreUsuario);

        if (usuario == null) {
            // Se hace el mismo trabajo de hash que con un usuario real para que el
            // tiempo de respuesta no revele si el nombre de usuario existe o no.
            SeguridadUtil.generarHash(contrasenia);
            return null;
        }

        boolean contraseniaCorrecta = SeguridadUtil.verificar(contrasenia, usuario.getContraseniaHash());
        if (!contraseniaCorrecta || !usuario.isActivo()) {
            return null;
        }

        usuarioActual = usuario;
        LOG.info("Sesión iniciada: " + usuario);
        return usuario;
    }

    /** RF06: registra el cierre de sesión. */
    public void cerrarSesion() {
        Usuario usuario = usuarioActual;
        if (usuario != null) {
            LOG.info("Sesión cerrada: " + usuario);
            usuarioActual = null;
        }
    }

    public Usuario getUsuarioActual() {
        return usuarioActual;
    }

    public boolean haySesionActiva() {
        return usuarioActual != null;
    }
}
