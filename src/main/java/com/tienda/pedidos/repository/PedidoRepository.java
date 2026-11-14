package com.tienda.pedidos.repository;

import com.tienda.pedidos.entity.Pedido;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

// DAO con Spring Data JPA: reemplaza el JdbcTemplate con SQL concatenado. save, findById y las
// consultas derivadas del nombre del metodo se generan solas y siempre con parametros
public interface PedidoRepository extends JpaRepository<Pedido, Long> {

    List<Pedido> findByClienteEmail(String clienteEmail);
}
