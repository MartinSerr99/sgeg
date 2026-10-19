package com.sgeg.dao;

import com.sgeg.modelo.EstadoMesa;
import com.sgeg.modelo.Mesa;
import com.sgeg.util.ConexionBD;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class MesaDAOImpl implements IMesaDAO {

    private static final String SQL_LISTAR =
            "SELECT m.id_mesa, m.numero, em.descripcion AS estado "
            + "FROM mesa m "
            + "JOIN estado_mesa em ON em.id_estado_mesa = m.id_estado_mesa "
            + "ORDER BY m.numero";

    @Override
    public List<Mesa> listarTodas() {
        try {
            Connection conexion = ConexionBD.getInstancia().getConexion();
            try (PreparedStatement consulta = conexion.prepareStatement(SQL_LISTAR);
                 ResultSet resultado = consulta.executeQuery()) {
                List<Mesa> mesas = new ArrayList<>();
                while (resultado.next()) {
                    mesas.add(new Mesa(
                            resultado.getInt("id_mesa"),
                            resultado.getInt("numero"),
                            EstadoMesa.desdeDescripcion(resultado.getString("estado"))));
                }
                return mesas;
            }
        } catch (SQLException e) {
            throw new DAOException("No se pudieron leer las mesas", e);
        }
    }
}
