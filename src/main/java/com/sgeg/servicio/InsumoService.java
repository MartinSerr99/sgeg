package com.sgeg.servicio;

import com.sgeg.dao.IInsumoDAO;
import com.sgeg.excepcion.NegocioException;
import com.sgeg.modelo.Insumo;
import com.sgeg.modelo.Rol;
import com.sgeg.modelo.Usuario;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/** Stock e insumos: alta, ingreso de stock (CU10), consulta (CU11) y alerta de stock mínimo (CU12). */
public class InsumoService {

    private static final BigDecimal TOPE = new BigDecimal("99999999.99");

    private final IInsumoDAO insumoDAO;

    public InsumoService(IInsumoDAO insumoDAO) {
        this.insumoDAO = insumoDAO;
    }

    /** Administrador y cocinero consultan el stock; solo el administrador lo modifica. */
    public boolean puedeConsultar(Usuario usuario) {
        return usuario != null && (usuario.tieneRol(Rol.ADMINISTRADOR) || usuario.tieneRol(Rol.COCINERO));
    }

    public boolean puedeModificar(Usuario usuario) {
        return usuario != null && usuario.tieneRol(Rol.ADMINISTRADOR);
    }

    public List<Insumo> listar() {
        return insumoDAO.listarTodos();
    }

    /** CU12 / RF24: insumos en o por debajo de su mínimo, para mostrar la alerta. */
    public List<Insumo> listarBajoMinimo() {
        List<Insumo> alertas = new ArrayList<>();
        for (Insumo insumo : insumoDAO.listarTodos()) {
            if (insumo.estaBajoMinimo()) {
                alertas.add(insumo);
            }
        }
        return alertas;
    }

    public int contarBajoMinimo() {
        return insumoDAO.contarBajoMinimo();
    }

    /** Alta de un insumo con su stock inicial. */
    public void crear(Usuario usuario, Insumo insumo) {
        exigirAdministrador(usuario);
        validar(insumo, true);
        if (insumoDAO.existeNombre(insumo.getNombre(), 0)) {
            throw new NegocioException("Ya existe un insumo llamado «" + insumo.getNombre() + "».");
        }
        insumoDAO.guardar(insumo);
    }

    /** Cambia nombre, unidad y mínimo. El stock actual no se edita a mano: se ajusta con ingresos. */
    public void modificar(Usuario usuario, Insumo insumo) {
        exigirAdministrador(usuario);
        validar(insumo, false);
        if (insumoDAO.existeNombre(insumo.getNombre(), insumo.getIdInsumo())) {
            throw new NegocioException("Ya existe otro insumo llamado «" + insumo.getNombre() + "».");
        }
        insumoDAO.actualizar(insumo);
    }

    /** CU10 / RF22: suma al stock la cantidad que ingresó (compra, reposición). */
    public void registrarIngreso(Usuario usuario, Insumo insumo, BigDecimal cantidad) {
        exigirAdministrador(usuario);
        if (cantidad == null || cantidad.signum() <= 0) {
            throw new NegocioException("La cantidad que ingresa debe ser mayor a cero.");
        }
        if (cantidad.compareTo(TOPE) > 0 || insumo.getStockActual().add(cantidad).compareTo(TOPE) > 0) {
            throw new NegocioException("La cantidad es demasiado grande.");
        }
        insumoDAO.sumarStock(insumo.getIdInsumo(), cantidad);
    }

    private static void exigirAdministrador(Usuario usuario) {
        if (usuario == null || !usuario.tieneRol(Rol.ADMINISTRADOR)) {
            throw new NegocioException("Solo el administrador puede modificar el stock.");
        }
    }

    private static void validar(Insumo insumo, boolean conStockInicial) {
        if (insumo.getNombre() == null || insumo.getNombre().isBlank()) {
            throw new NegocioException("El nombre del insumo es obligatorio.");
        }
        insumo.setNombre(insumo.getNombre().trim());
        if (insumo.getNombre().length() > 100) {
            throw new NegocioException("El nombre del insumo admite hasta 100 caracteres.");
        }
        if (insumo.getUnidadMedida() == null || insumo.getUnidadMedida().isBlank()) {
            throw new NegocioException("La unidad de medida es obligatoria (kg, litro, unidad…).");
        }
        insumo.setUnidadMedida(insumo.getUnidadMedida().trim());
        if (insumo.getUnidadMedida().length() > 20) {
            throw new NegocioException("La unidad de medida admite hasta 20 caracteres.");
        }
        if (insumo.getStockMinimo() == null || insumo.getStockMinimo().signum() < 0
                || insumo.getStockMinimo().compareTo(TOPE) > 0) {
            throw new NegocioException("El stock mínimo no puede ser negativo.");
        }
        if (conStockInicial && (insumo.getStockActual() == null || insumo.getStockActual().signum() < 0
                || insumo.getStockActual().compareTo(TOPE) > 0)) {
            throw new NegocioException("El stock inicial no puede ser negativo.");
        }
    }
}
