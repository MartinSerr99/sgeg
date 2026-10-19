package com.sgeg.util;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

/**
 * Patrón Singleton: una única instancia administra la conexión a MySQL y la
 * comparten todos los DAO. Los datos de conexión se leen de config.properties.
 */
public final class ConexionBD {

    private static final String ARCHIVO_CONFIG = "/config.properties";
    private static ConexionBD instancia;

    private final String url;
    private final String usuario;
    private final String clave;
    private Connection conexion;

    private ConexionBD() {
        Properties props = new Properties();
        try (InputStream in = ConexionBD.class.getResourceAsStream(ARCHIVO_CONFIG)) {
            if (in == null) {
                throw new IllegalStateException("No se encontró " + ARCHIVO_CONFIG
                        + " (debe estar en src/main/resources).");
            }
            try (Reader lector = new InputStreamReader(in, StandardCharsets.UTF_8)) {
                props.load(lector);
            }
        } catch (IOException e) {
            throw new IllegalStateException("No se pudo leer config.properties", e);
        }
        this.url = requerida(props, "db.url");
        this.usuario = requerida(props, "db.usuario");
        this.clave = props.getProperty("db.password", "");
    }

    public static synchronized ConexionBD getInstancia() {
        if (instancia == null) {
            instancia = new ConexionBD();
        }
        return instancia;
    }

    /**
     * Devuelve la conexión activa. Si todavía no existe, o MySQL la cerró por
     * inactividad, abre una nueva. Los DAO NO deben cerrarla: es compartida.
     */
    public synchronized Connection getConexion() throws SQLException {
        if (conexion == null || !conexion.isValid(2)) {
            cerrarSilenciosamente();
            conexion = DriverManager.getConnection(url, usuario, clave);
        }
        return conexion;
    }

    /**
     * Abre una conexión PROPIA, independiente de la compartida. Se usa en las operaciones
     * transaccionales (varias sentencias que deben confirmarse o deshacerse juntas): así
     * el setAutoCommit(false) no afecta a las consultas que otras pantallas hacen en paralelo.
     * Quien la pide es responsable de cerrarla.
     */
    public Connection abrirConexionNueva() throws SQLException {
        return DriverManager.getConnection(url, usuario, clave);
    }

    /** Se invoca al cerrar la aplicación. No hace nada si nunca se abrió una conexión. */
    public static synchronized void cerrarConexion() {
        if (instancia != null) {
            instancia.cerrarSilenciosamente();
        }
    }

    private void cerrarSilenciosamente() {
        if (conexion != null) {
            try {
                conexion.close();
            } catch (SQLException ignorada) {
                // Ya estaba caída; no hay nada más que hacer.
            }
            conexion = null;
        }
    }

    private static String requerida(Properties props, String nombre) {
        String valor = props.getProperty(nombre);
        if (valor == null || valor.isBlank()) {
            throw new IllegalStateException("Falta la propiedad '" + nombre + "' en config.properties");
        }
        return valor.trim();
    }
}
