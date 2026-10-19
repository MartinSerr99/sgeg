package com.sgeg.dao;

import com.sgeg.modelo.Mesa;
import java.util.List;

public interface IMesaDAO {

    /** Todas las mesas con su estado actual, ordenadas por número (RF13). */
    List<Mesa> listarTodas();
}
