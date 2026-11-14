package com.tienda.pedidos.service;

import com.tienda.pedidos.dto.PedidoRequestDTO;
import com.tienda.pedidos.dto.PedidoResponseDTO;
import com.tienda.pedidos.entity.Pedido;
import com.tienda.pedidos.exception.RecursoNoEncontradoException;
import com.tienda.pedidos.repository.PedidoRepository;
import com.tienda.pedidos.strategy.DescuentoMayorista;
import com.tienda.pedidos.strategy.DescuentoRegular;
import com.tienda.pedidos.strategy.DescuentoVip;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

// Prueba unitaria del servicio: repositorio y notificador son mocks (posible gracias a DIP) y las
// estrategias son las reales, en el mismo Map que arma Spring
@ExtendWith(MockitoExtension.class)
class PedidoServiceImplTest {

    @Mock PedidoRepository repo;
    @Mock NotificacionService notificador;
    PedidoServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new PedidoServiceImpl(repo, notificador, Map.of(
            "REGULAR", new DescuentoRegular(), "VIP", new DescuentoVip(), "MAYORISTA", new DescuentoMayorista()));
    }

    @ParameterizedTest(name = "{0}: descuento={1}, iva={2}, total={3}")
    @CsvSource({
        "VIP,       15000.0, 16150.0, 101150.0",
        "MAYORISTA, 25000.0, 14250.0,  89250.0",
        "REGULAR,       0.0, 19000.0, 119000.0",
        "vip,       15000.0, 16150.0, 101150.0"
    })
    void crear_subtotal100000_aplicaElDescuentoDelTipoDeCliente(String tipo, double descuento, double iva, double total) {
        guardarDevolviendoConId(1L);

        PedidoResponseDTO r = service.crear(pedido("ana@correo.com", tipo, 100000.0));

        assertThat(r.getDescuento()).isEqualTo(descuento);
        assertThat(r.getIva()).isEqualTo(iva);
        assertThat(r.getTotal()).isEqualTo(total);
        assertThat(r.getTipoCliente()).isEqualTo(tipo.toUpperCase());
        assertThat(r.getEstado()).isEqualTo("CREADO");
        verify(notificador).enviarConfirmacion("ana@correo.com", total);
    }

    @Test
    void crear_tipoSinEstrategia_aplicaRegular() {
        guardarDevolviendoConId(2L);

        PedidoResponseDTO r = service.crear(pedido("ana@correo.com", "PLATINO", 100000.0));

        assertThat(r.getDescuento()).isEqualTo(0.0);
        assertThat(r.getTotal()).isEqualTo(119000.0);
    }

    @Test
    void crear_subtotalConCentavos_redondeaLosMontosADosDecimales() {
        guardarDevolviendoConId(3L);

        PedidoResponseDTO r = service.crear(pedido("ana@correo.com", "VIP", 123456.78));

        // sin redondeo serian 18518.517, 19938.2694 y 124876.53297
        assertThat(r.getDescuento()).isEqualTo(18518.52);
        assertThat(r.getIva()).isEqualTo(19938.27);
        assertThat(r.getTotal()).isEqualTo(124876.53);
    }

    @Test
    void crear_guardaNotasInternasEnLaEntidadPeroElDtoNoLasTiene() {
        guardarDevolviendoConId(4L);

        service.crear(pedido("ana@correo.com", "VIP", 100000.0));

        ArgumentCaptor<Pedido> guardado = ArgumentCaptor.forClass(Pedido.class);
        verify(repo).save(guardado.capture());
        assertThat(guardado.getValue().getNotasInternas()).isNotBlank();
        assertThat(Arrays.stream(PedidoResponseDTO.class.getDeclaredFields()).map(f -> f.getName()))
            .doesNotContain("notasInternas");
    }

    @Test
    void crear_siFallaLaNotificacion_igualDevuelveElPedidoGuardado() {
        guardarDevolviendoConId(5L);
        doThrow(new IllegalStateException("servidor de correo caido"))
            .when(notificador).enviarConfirmacion(anyString(), anyDouble());

        PedidoResponseDTO r = service.crear(pedido("ana@correo.com", "VIP", 100000.0));

        assertThat(r.getId()).isEqualTo(5L);
    }

    @Test
    void buscarPorId_noExiste_lanzaRecursoNoEncontradoException() {
        when(repo.findById(999L)).thenReturn(Optional.empty());

        RecursoNoEncontradoException ex =
            assertThrows(RecursoNoEncontradoException.class, () -> service.buscarPorId(999L));
        assertThat(ex.getMessage()).isEqualTo("Pedido con id 999 no encontrado.");
    }

    @Test
    void listarPorCliente_conPedidos_losDevuelveComoDto() {
        Pedido p = new Pedido();
        p.setId(7L);
        p.setClienteEmail("ana@correo.com");
        p.setNotasInternas("no debe salir");
        when(repo.findByClienteEmail("ana@correo.com")).thenReturn(List.of(p));

        assertThat(service.listarPorCliente("ana@correo.com"))
            .extracting(PedidoResponseDTO::getId).containsExactly(7L);
    }

    private void guardarDevolviendoConId(Long id) {
        when(repo.save(any())).thenAnswer(inv -> {
            Pedido p = inv.getArgument(0);
            p.setId(id);
            return p;
        });
    }

    private PedidoRequestDTO pedido(String email, String tipo, Double subtotal) {
        PedidoRequestDTO dto = new PedidoRequestDTO();
        dto.setClienteEmail(email);
        dto.setTipoCliente(tipo);
        dto.setSubtotal(subtotal);
        return dto;
    }
}
