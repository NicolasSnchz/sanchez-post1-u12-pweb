package com.tienda.pedidos.service;

import com.tienda.pedidos.dto.PedidoRequestDTO;
import com.tienda.pedidos.dto.PedidoResponseDTO;
import com.tienda.pedidos.entity.Pedido;
import com.tienda.pedidos.exception.RecursoNoEncontradoException;
import com.tienda.pedidos.repository.PedidoRepository;
import com.tienda.pedidos.strategy.EstrategiaDescuento;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Service
public class PedidoServiceImpl implements PedidoService {

    // Los mensajes llevan identificadores (id, clienteEmail, tipoCliente) y montos; notasInternas nunca
    private static final Logger log = LoggerFactory.getLogger(PedidoServiceImpl.class);

    private static final double TASA_IVA = 0.19;

    private final PedidoRepository repo;
    private final NotificacionService notificador;
    private final Map<String, EstrategiaDescuento> estrategias;

    // Todas las dependencias llegan por constructor (DIP): ningun new de un colaborador. Spring arma el
    // mapa con cada EstrategiaDescuento usando el nombre del bean como llave (REGULAR, VIP, MAYORISTA)
    public PedidoServiceImpl(PedidoRepository repo,
                             NotificacionService notificador,
                             Map<String, EstrategiaDescuento> estrategias) {
        this.repo = repo;
        this.notificador = notificador;
        this.estrategias = estrategias;
    }

    @Override
    public PedidoResponseDTO crear(PedidoRequestDTO dto) {
        log.info("Creando pedido: clienteEmail={}, tipoCliente={}",
                 dto.getClienteEmail(), dto.getTipoCliente());
        String tipo = dto.getTipoCliente().toUpperCase();
        if (!estrategias.containsKey(tipo)) {
            log.warn("tipoCliente={} no tiene estrategia de descuento; se aplica REGULAR (clienteEmail={})",
                     dto.getTipoCliente(), dto.getClienteEmail());
        }
        EstrategiaDescuento estrategia = estrategias.getOrDefault(tipo, estrategias.get("REGULAR"));
        double descuento = estrategia.calcularDescuento(dto.getSubtotal());
        // Un solo punto de auditoria del descuento, aqui y no dentro de cada Strategy
        log.info("Descuento aplicado: pedido para clienteEmail={}, tipoCliente={}, descuento={}",
                 dto.getClienteEmail(), dto.getTipoCliente(), descuento);
        return guardar(dto, descuento);
    }

    @Override
    public PedidoResponseDTO buscarPorId(Long id) {
        log.debug("Buscando pedido con id={}", id);
        Pedido p = repo.findById(id).orElseThrow(() -> {
            log.warn("Pedido con id={} no encontrado", id);
            return new RecursoNoEncontradoException("Pedido", id);
        });
        return toResponseDTO(p);
    }

    @Override
    public List<PedidoResponseDTO> listarPorCliente(String clienteEmail) {
        List<PedidoResponseDTO> pedidos = repo.findByClienteEmail(clienteEmail).stream()
            .map(this::toResponseDTO).toList();
        log.debug("Listando pedidos de clienteEmail={}: {} encontrados", clienteEmail, pedidos.size());
        return pedidos;
    }

    private PedidoResponseDTO guardar(PedidoRequestDTO dto, double descuentoCalculado) {
        double descuento = redondear(descuentoCalculado);
        double baseConDescuento = dto.getSubtotal() - descuento;
        double iva = redondear(baseConDescuento * TASA_IVA);
        double total = redondear(baseConDescuento + iva);

        Pedido pedido = new Pedido();
        pedido.setClienteEmail(dto.getClienteEmail());
        pedido.setTipoCliente(dto.getTipoCliente().toUpperCase());
        pedido.setSubtotal(dto.getSubtotal());
        pedido.setDescuento(descuento);
        pedido.setIva(iva);
        pedido.setTotal(total);
        pedido.setEstado("CREADO");
        pedido.setNotasInternas("Pedido creado automaticamente, revisar stock antes de despachar");
        pedido.setFechaCreacion(LocalDateTime.now());

        Pedido guardado = repo.save(pedido);
        log.info("Pedido creado exitosamente con id={}, total={}", guardado.getId(), guardado.getTotal());
        try {
            notificador.enviarConfirmacion(guardado.getClienteEmail(), guardado.getTotal());
        } catch (RuntimeException e) {
            // El pedido ya quedo guardado: una falla del correo se registra, pero no anula la compra
            log.error("No se pudo enviar la confirmacion del pedido id={} a clienteEmail={}",
                      guardado.getId(), guardado.getClienteEmail(), e);
        }
        return toResponseDTO(guardado);
    }

    // Montos de dinero con dos decimales: con double, un subtotal como 123456.78 daria un total
    // de 124876.53297
    private static double redondear(double valor) {
        return BigDecimal.valueOf(valor).setScale(2, RoundingMode.HALF_UP).doubleValue();
    }

    // Mapeo explicito entidad -> DTO: notasInternas y fechaCreacion no se copian
    private PedidoResponseDTO toResponseDTO(Pedido p) {
        PedidoResponseDTO dto = new PedidoResponseDTO();
        dto.setId(p.getId());
        dto.setClienteEmail(p.getClienteEmail());
        dto.setTipoCliente(p.getTipoCliente());
        dto.setSubtotal(p.getSubtotal());
        dto.setDescuento(p.getDescuento());
        dto.setIva(p.getIva());
        dto.setTotal(p.getTotal());
        dto.setEstado(p.getEstado());
        return dto;
    }
}
