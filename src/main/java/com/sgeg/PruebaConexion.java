package com.sgeg;

import com.sgeg.dao.UsuarioDAOImpl;
import com.sgeg.modelo.Usuario;
import com.sgeg.util.ConexionBD;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

/**
 * Herramienta de diagnóstico (sin interfaz gráfica): comprueba la conexión,
 * que existan las tablas y que el usuario 'admin' esté cargado.
 * Ejecutarla ANTES de probar la aplicación completa.
 */
public class PruebaConexion {

    public static void main(String[] args) {
        System.out.println("== Prueba de conexión a la base de datos ==");
        try {
            Connection conexion = ConexionBD.getInstancia().getConexion();
            System.out.println("[OK] Conexión establecida: " + conexion.getMetaData().getURL());

            try (Statement sentencia = conexion.createStatement();
                 ResultSet resultado = sentencia.executeQuery("SELECT COUNT(*) FROM usuario")) {
                resultado.next();
                System.out.println("[OK] Tabla usuario accesible. Usuarios cargados: " + resultado.getInt(1));
            }

            Usuario admin = new UsuarioDAOImpl().buscarPorNombreUsuario("admin");
            if (admin == null) {
                System.out.println("[AVISO] No existe el usuario 'admin'. ¿Ejecutaste database/02_datos_iniciales.sql?");
            } else {
                System.out.println("[OK] Usuario 'admin' encontrado. Rol: " + admin.getRol().getDescripcion());
            }
        } catch (Exception e) {
            System.out.println("[ERROR] " + e.getMessage());
            e.printStackTrace();
        } finally {
            ConexionBD.cerrarConexion();
        }
    }
}
