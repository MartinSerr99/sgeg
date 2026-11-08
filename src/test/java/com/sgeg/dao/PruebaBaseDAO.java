package com.sgeg.dao;

import com.sgeg.modelo.*;
import java.lang.reflect.*;
import java.math.BigDecimal;
import java.sql.*;
import java.util.*;
import java.util.logging.*;

/**
 * Prueba unitaria de BaseDAO y de los DAO que heredan de ella, sin necesidad de MySQL: se registra un driver
 * JDBC simulado (url jdbc:fake:) que permite provocar fallas y ver si el DAO hace commit, rollback y cierre.
 * Usa src/test/resources/config.properties (url falsa). Ejecutar el método main; imprime [OK] o [FALLA].
 */
public class PruebaBaseDAO {
    static int ok = 0, mal = 0;
    static List<String> eventos = new ArrayList<>();
    static boolean fallarPrepare = false, fallarExecuteUpdate = false;

    static void check(String nombre, boolean cond) {
        if (cond) { ok++; System.out.println("[OK]   " + nombre); }
        else { mal++; System.out.println("[FALLA] " + nombre); }
    }

    static Object def(Class<?> t) {
        if (t == boolean.class) return false;
        if (t == int.class) return 0;
        if (t == long.class) return 0L;
        return null;
    }

    static ResultSet rs(int filas) {
        int[] pos = {0};
        return (ResultSet) Proxy.newProxyInstance(PruebaBaseDAO.class.getClassLoader(), new Class<?>[]{ResultSet.class}, (p, m, a) -> {
            switch (m.getName()) {
                case "next": return pos[0]++ < filas;
                case "getInt": return 7;
                case "getBoolean": return true;
                case "getString": return "dato";
                case "close": return null;
                default: return def(m.getReturnType());
            }
        });
    }

    static PreparedStatement ps(int filas) {
        return (PreparedStatement) Proxy.newProxyInstance(PruebaBaseDAO.class.getClassLoader(), new Class<?>[]{PreparedStatement.class}, (p, m, a) -> {
            switch (m.getName()) {
                case "executeQuery": case "getGeneratedKeys": return rs(filas);
                case "executeUpdate":
                    if (fallarExecuteUpdate) throw new SQLException("fallo simulado de MySQL");
                    return 1;
                case "close": return null;
                default: return def(m.getReturnType());
            }
        });
    }

    static Connection conexion() {
        return (Connection) Proxy.newProxyInstance(PruebaBaseDAO.class.getClassLoader(), new Class<?>[]{Connection.class}, (p, m, a) -> {
            switch (m.getName()) {
                case "prepareStatement":
                    if (fallarPrepare) throw new SQLException("MySQL no responde");
                    return ps(1);
                case "isValid": return true;
                case "setAutoCommit": eventos.add("autocommit=" + a[0]); return null;
                case "commit": eventos.add("commit"); return null;
                case "rollback": eventos.add("rollback"); return null;
                case "close": eventos.add("close"); return null;
                default: return def(m.getReturnType());
            }
        });
    }

    public static void main(String[] args) throws Exception {
        Logger.getLogger("").setLevel(Level.OFF);
        DriverManager.registerDriver(new Driver() {
            public Connection connect(String url, Properties i) { return url.startsWith("jdbc:fake:") ? conexion() : null; }
            public boolean acceptsURL(String url) { return url.startsWith("jdbc:fake:"); }
            public DriverPropertyInfo[] getPropertyInfo(String u, Properties i) { return new DriverPropertyInfo[0]; }
            public int getMajorVersion() { return 1; }
            public int getMinorVersion() { return 0; }
            public boolean jdbcCompliant() { return false; }
            public java.util.logging.Logger getParentLogger() { return null; }
        });

        // 1) consulta simple por la conexión compartida
        IUsuarioDAO usuarios = new UsuarioDAOImpl();
        Usuario u = usuarios.buscarPorNombreUsuario("admin");
        check("buscarPorNombreUsuario devuelve el usuario mapeado", u != null && "dato".equals(u.getNombreUsuario()) && u.getIdUsuario() == 7);
        check("la lista de roles se arma como ArrayList", usuarios.listarRoles() instanceof ArrayList);

        // 2) error en consulta -> DAOException con el nombre del DAO y la causa SQLException
        fallarPrepare = true;
        try {
            usuarios.buscarPorNombreUsuario("admin");
            check("consulta fallida lanza DAOException", false);
        } catch (DAOException e) {
            check("consulta fallida lanza DAOException", true);
            check("mensaje con prefijo del DAO", e.getMessage().equals("usuario: No se pudo buscar el usuario 'admin'"));
            check("la causa es la SQLException original", e.getCause() instanceof SQLException && e.getCause().getMessage().equals("MySQL no responde"));
        }
        fallarPrepare = false;

        // 3) transacción exitosa: autocommit off, commit y cierre de la conexión propia
        IProductoDAO productos = new ProductoDAOImpl();
        Producto p = new Producto();
        p.setNombre("Agua 500ml"); p.setPrecio(new BigDecimal("1000")); p.setDisponible(true);
        p.setCategoria(new Categoria(3, "Bebidas"));
        eventos.clear();
        productos.guardar(p, new ArrayList<>());
        check("transacción OK: autocommit=false, commit, close (en ese orden)", eventos.equals(List.of("autocommit=false", "commit", "close")));
        check("el id generado queda asignado", p.getIdProducto() == 7);

        // 4) transacción fallida: rollback una vez, cierre una vez, sin commit, DAOException
        fallarExecuteUpdate = true;
        eventos.clear();
        try {
            productos.guardar(p, new ArrayList<>());
            check("transacción fallida lanza DAOException", false);
        } catch (DAOException e) {
            check("transacción fallida lanza DAOException", true);
            check("rollback y close, sin commit", eventos.equals(List.of("autocommit=false", "rollback", "close")));
            check("mensaje del DAO de producto", e.getMessage().equals("producto: No se pudo guardar el producto 'Agua 500ml'"));
        }
        fallarExecuteUpdate = false;

        // 5) BaseDAO es abstracta y exige entidad()
        check("BaseDAO es abstracta", Modifier.isAbstract(BaseDAO.class.getModifiers()));
        boolean todos = true;
        for (Class<?> c : List.of(UsuarioDAOImpl.class, PedidoDAOImpl.class, ProductoDAOImpl.class, InsumoDAOImpl.class,
                CategoriaDAOImpl.class, ClienteDAOImpl.class, MesaDAOImpl.class, VentaDAOImpl.class, ReporteDAOImpl.class)) {
            todos &= BaseDAO.class.isAssignableFrom(c);
        }
        check("los 9 DAO concretos heredan de BaseDAO", todos);
        Method m = BaseDAO.class.getDeclaredMethod("entidad");
        check("entidad() es el método abstracto", Modifier.isAbstract(m.getModifiers()));

        System.out.println("\nResultado: " + ok + " OK, " + mal + " con falla");
        System.exit(mal == 0 ? 0 : 1);
    }
}
