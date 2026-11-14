package com.tienda.pedidos.strategy;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

// Cada estrategia por separado, sin Spring: el porcentaje de cada tipo de cliente
class EstrategiaDescuentoTest {

    @Test
    void descuentoRegular_cualquierSubtotal_esCero() {
        assertThat(new DescuentoRegular().calcularDescuento(100000)).isEqualTo(0.0);
    }

    @Test
    void descuentoVip_subtotal100000_es15000() {
        assertThat(new DescuentoVip().calcularDescuento(100000)).isEqualTo(15000.0);
    }

    @Test
    void descuentoMayorista_subtotal100000_es25000() {
        assertThat(new DescuentoMayorista().calcularDescuento(100000)).isEqualTo(25000.0);
    }
}
