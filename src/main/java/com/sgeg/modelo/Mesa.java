package com.sgeg.modelo;

/** Mesa del salón. Tabla: mesa. */
public class Mesa {

    private int idMesa;
    private int numero;
    private EstadoMesa estado;

    public Mesa() {
    }

    public Mesa(int idMesa, int numero, EstadoMesa estado) {
        this.idMesa = idMesa;
        this.numero = numero;
        this.estado = estado;
    }

    public int getIdMesa() {
        return idMesa;
    }

    public void setIdMesa(int idMesa) {
        this.idMesa = idMesa;
    }

    public int getNumero() {
        return numero;
    }

    public void setNumero(int numero) {
        this.numero = numero;
    }

    public EstadoMesa getEstado() {
        return estado;
    }

    public void setEstado(EstadoMesa estado) {
        this.estado = estado;
    }

    public boolean estaLibre() {
        return estado == EstadoMesa.LIBRE;
    }

    @Override
    public String toString() {
        return "Mesa " + numero;
    }
}
