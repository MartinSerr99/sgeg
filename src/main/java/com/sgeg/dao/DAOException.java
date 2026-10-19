package com.sgeg.dao;

/** Error de acceso a datos. Envuelve la SQLException para que las capas superiores no dependan de JDBC. */
public class DAOException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public DAOException(String mensaje, Throwable causa) {
        super(mensaje, causa);
    }
}
