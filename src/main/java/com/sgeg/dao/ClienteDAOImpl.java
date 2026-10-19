package com.sgeg.dao;

import com.sgeg.modelo.Cliente;
import com.sgeg.util.ConexionBD;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class ClienteDAOImpl implements IClienteDAO {

    private static final String SQL_LISTAR =
            "SELECT id_cliente, nombre, telefono FROM cliente ORDER BY nombre";

    @Override
    public List<Cliente> listarTodos() {
        try {
            Connection conexion = ConexionBD.getInstancia().getConexion();
            try (PreparedStatement consulta = conexion.prepareStatement(SQL_LISTAR);
                 ResultSet resultado = consulta.executeQuery()) {
                List<Cliente> clientes = new ArrayList<>();
                while (resultado.next()) {
                    clientes.add(new Cliente(
                            resultado.getInt("id_cliente"),
                            resultado.getString("nombre"),
                            resultado.getString("telefono")));
                }
                return clientes;
            }
        } catch (SQLException e) {
            throw new DAOException("No se pudieron leer los clientes", e);
        }
    }
}
