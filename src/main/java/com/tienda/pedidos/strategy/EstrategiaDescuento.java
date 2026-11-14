package com.tienda.pedidos.strategy;

// Strategy: cada tipo de cliente es una implementacion registrada como bean con el nombre del tipo.
// Un tipo nuevo (por ejemplo CORPORATIVO) es una clase nueva con @Component("CORPORATIVO"); no se
// modifica PedidoServiceImpl (OCP)
public interface EstrategiaDescuento {

    double calcularDescuento(double subtotal);
}
