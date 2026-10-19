package com.sgeg.excepcion;

/**
 * Se lanza cuando se viola una regla del negocio (stock insuficiente, mesa ocupada, rol sin permiso...).
 * El mensaje está redactado para mostrarse tal cual al usuario.
 */
public class NegocioException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public NegocioException(String mensaje) {
        super(mensaje);
    }
}
