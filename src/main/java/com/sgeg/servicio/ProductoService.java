package com.sgeg.servicio;

import com.sgeg.dao.ICategoriaDAO;
import com.sgeg.dao.IInsumoDAO;
import com.sgeg.dao.IProductoDAO;
import com.sgeg.excepcion.NegocioException;
import com.sgeg.modelo.Categoria;
import com.sgeg.modelo.IngredienteReceta;
import com.sgeg.modelo.Insumo;
import com.sgeg.modelo.Producto;
import com.sgeg.modelo.Rol;
import com.sgeg.modelo.Usuario;
import java.math.BigDecimal;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Productos, menú y categorías: alta y modificación (CU03), baja lógica (CU04) y consulta del menú (CU05). */
public class ProductoService {

    private static final BigDecimal TOPE = new BigDecimal("99999999.99");

    private final IProductoDAO productoDAO;
    private final ICategoriaDAO categoriaDAO;
    private final IInsumoDAO insumoDAO;

    public ProductoService(IProductoDAO productoDAO, ICategoriaDAO categoriaDAO, IInsumoDAO insumoDAO) {
        this.productoDAO = productoDAO;
        this.categoriaDAO = categoriaDAO;
        this.insumoDAO = insumoDAO;
    }

    public boolean puedeEditar(Usuario usuario) {
        return usuario != null && usuario.tieneRol(Rol.ADMINISTRADOR);
    }

    // ------------------------------------------------------------------ consultas

    /** CU05: el menú que ve el personal de salón (solo productos habilitados). */
    public List<Producto> listarMenu() {
        return productoDAO.listarMenu();
    }

    /** Todos los productos, también los dados de baja (administración). */
    public List<Producto> listarTodos() {
        return productoDAO.listarTodos();
    }

    public List<Categoria> listarCategorias() {
        return categoriaDAO.listarTodas();
    }

    public List<Insumo> listarInsumos() {
        return insumoDAO.listarTodos();
    }

    public List<IngredienteReceta> obtenerReceta(int idProducto) {
        return productoDAO.obtenerReceta(idProducto);
    }

    // ------------------------------------------------------------------ CU03: alta y modificación

    public void crear(Usuario usuario, Producto producto, List<IngredienteReceta> receta) {
        exigirAdministrador(usuario);
        validar(producto, receta);
        if (productoDAO.existeNombre(producto.getNombre(), 0)) {
            throw new NegocioException("Ya existe un producto llamado «" + producto.getNombre() + "».");
        }
        productoDAO.guardar(producto, receta);
    }

    public void modificar(Usuario usuario, Producto producto, List<IngredienteReceta> receta) {
        exigirAdministrador(usuario);
        validar(producto, receta);
        if (productoDAO.existeNombre(producto.getNombre(), producto.getIdProducto())) {
            throw new NegocioException("Ya existe otro producto llamado «" + producto.getNombre() + "».");
        }
        productoDAO.actualizar(producto, receta);
    }

    // ------------------------------------------------------------------ CU04: baja lógica

    /**
     * El producto deja de ofrecerse pero se conserva: las ventas ya hechas siguen mostrándolo
     * (el historial no se pierde). Se puede reactivar.
     */
    public void cambiarDisponibilidad(Usuario usuario, Producto producto, boolean disponible) {
        exigirAdministrador(usuario);
        productoDAO.cambiarDisponibilidad(producto.getIdProducto(), disponible);
    }

    // ------------------------------------------------------------------ categorías

    public void crearCategoria(Usuario usuario, String nombre) {
        exigirAdministrador(usuario);
        String limpio = validarNombreCategoria(nombre);
        if (categoriaDAO.existeNombre(limpio, 0)) {
            throw new NegocioException("Ya existe la categoría «" + limpio + "».");
        }
        categoriaDAO.guardar(new Categoria(0, limpio));
    }

    public void renombrarCategoria(Usuario usuario, Categoria categoria, String nombre) {
        exigirAdministrador(usuario);
        String limpio = validarNombreCategoria(nombre);
        if (categoriaDAO.existeNombre(limpio, categoria.getIdCategoria())) {
            throw new NegocioException("Ya existe la categoría «" + limpio + "».");
        }
        categoriaDAO.renombrar(categoria.getIdCategoria(), limpio);
    }

    /** Una categoría con productos no se puede eliminar: primero hay que pasar sus productos a otra. */
    public void eliminarCategoria(Usuario usuario, Categoria categoria) {
        exigirAdministrador(usuario);
        int productos = categoriaDAO.contarProductos(categoria.getIdCategoria());
        if (productos > 0) {
            throw new NegocioException("La categoría «" + categoria.getNombre() + "» tiene " + productos
                    + (productos == 1 ? " producto" : " productos")
                    + ". Cambiá su categoría antes de eliminarla.");
        }
        categoriaDAO.eliminar(categoria.getIdCategoria());
    }

    // ------------------------------------------------------------------ validaciones

    private static void exigirAdministrador(Usuario usuario) {
        if (usuario == null || !usuario.tieneRol(Rol.ADMINISTRADOR)) {
            throw new NegocioException("Solo el administrador puede modificar productos y categorías.");
        }
    }

    private static String validarNombreCategoria(String nombre) {
        if (nombre == null || nombre.isBlank()) {
            throw new NegocioException("El nombre de la categoría es obligatorio.");
        }
        String limpio = nombre.trim();
        if (limpio.length() > 50) {
            throw new NegocioException("El nombre de la categoría admite hasta 50 caracteres.");
        }
        return limpio;
    }

    private static void validar(Producto producto, List<IngredienteReceta> receta) {
        if (producto.getNombre() == null || producto.getNombre().isBlank()) {
            throw new NegocioException("El nombre del producto es obligatorio.");
        }
        producto.setNombre(producto.getNombre().trim());
        if (producto.getNombre().length() > 100) {
            throw new NegocioException("El nombre del producto admite hasta 100 caracteres.");
        }
        if (producto.getCategoria() == null) {
            throw new NegocioException("Elegí la categoría del producto.");
        }
        if (producto.getPrecio() == null || producto.getPrecio().signum() <= 0) {
            throw new NegocioException("El precio debe ser mayor a cero.");
        }
        if (producto.getPrecio().compareTo(TOPE) > 0) {
            throw new NegocioException("El precio es demasiado grande.");
        }
        Set<Integer> usados = new HashSet<>();
        for (IngredienteReceta ingrediente : receta) {
            if (ingrediente.getCantidadNecesaria() == null || ingrediente.getCantidadNecesaria().signum() <= 0) {
                throw new NegocioException("La cantidad de «" + ingrediente.getNombreInsumo() + "» debe ser mayor a cero.");
            }
            if (ingrediente.getCantidadNecesaria().compareTo(TOPE) > 0) {
                throw new NegocioException("La cantidad de «" + ingrediente.getNombreInsumo() + "» es demasiado grande.");
            }
            if (!usados.add(ingrediente.getIdInsumo())) {
                throw new NegocioException("El insumo «" + ingrediente.getNombreInsumo() + "» está repetido en la receta.");
            }
        }
    }
}
