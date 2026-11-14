package com.tienda.pedidos.controller;

import com.tienda.pedidos.dto.PedidoRequestDTO;
import com.tienda.pedidos.dto.PedidoResponseDTO;
import com.tienda.pedidos.exception.ApiError;
import com.tienda.pedidos.service.PedidoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

// Solo traduce HTTP <-> servicio: no valida a mano, no calcula, no persiste, no notifica
@RestController
@RequestMapping("/api/pedidos")
@Tag(name = "Pedidos", description = "Creacion y consulta de pedidos")
public class PedidoController {

    // Un ejemplo de ApiError por cada respuesta de error, para que Swagger no muestre un 404 como
    // ejemplo de un 400
    private static final String EJ_400_VALIDACION = "{\"status\":400,\"error\":\"Bad Request\","
        + "\"mensaje\":\"subtotal: El subtotal es obligatorio\",\"timestamp\":\"2026-11-13T20:00:00\","
        + "\"path\":\"/api/pedidos\"}";
    private static final String EJ_400_ID = "{\"status\":400,\"error\":\"Bad Request\","
        + "\"mensaje\":\"El parametro 'id' tiene un valor invalido: abc\",\"timestamp\":\"2026-11-13T20:00:00\","
        + "\"path\":\"/api/pedidos/abc\"}";
    private static final String EJ_404 = "{\"status\":404,\"error\":\"Not Found\","
        + "\"mensaje\":\"Pedido con id 999 no encontrado.\",\"timestamp\":\"2026-11-13T20:00:00\","
        + "\"path\":\"/api/pedidos/999\"}";
    private static final String EJ_400_PARAMETRO = "{\"status\":400,\"error\":\"Bad Request\","
        + "\"mensaje\":\"Falta el parametro obligatorio 'clienteEmail'\",\"timestamp\":\"2026-11-13T20:00:00\","
        + "\"path\":\"/api/pedidos\"}";

    private final PedidoService service;

    public PedidoController(PedidoService service) {
        this.service = service;
    }

    @Operation(summary = "Crear un nuevo pedido",
               description = "Calcula el descuento segun tipoCliente (REGULAR 0 %, VIP 15 %, MAYORISTA 25 %), "
                           + "el IVA del 19 % sobre la base con descuento y el total")
    @ApiResponse(responseCode = "201", description = "Pedido creado, con descuento aplicado segun tipoCliente")
    @ApiResponse(responseCode = "400", description = "Datos de entrada invalidos",
                 content = @Content(schema = @Schema(implementation = ApiError.class),
                                    examples = @ExampleObject(value = EJ_400_VALIDACION)))
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PedidoResponseDTO crear(@Valid @RequestBody PedidoRequestDTO dto) {
        return service.crear(dto);
    }

    @Operation(summary = "Obtener pedido por ID")
    @ApiResponse(responseCode = "200", description = "Pedido encontrado")
    @ApiResponse(responseCode = "400", description = "El id no es un numero",
                 content = @Content(schema = @Schema(implementation = ApiError.class),
                                    examples = @ExampleObject(value = EJ_400_ID)))
    @ApiResponse(responseCode = "404", description = "Pedido no encontrado",
                 content = @Content(schema = @Schema(implementation = ApiError.class),
                                    examples = @ExampleObject(value = EJ_404)))
    @GetMapping("/{id}")
    public PedidoResponseDTO obtener(
        @Parameter(description = "ID del pedido", example = "1")
        @PathVariable Long id) {
        return service.buscarPorId(id); // sin try/catch: la excepcion sube a GlobalExceptionHandler
    }

    @Operation(summary = "Listar los pedidos de un cliente",
               description = "Devuelve una lista vacia si el cliente no tiene pedidos")
    @ApiResponse(responseCode = "200", description = "Pedidos del cliente (puede ser una lista vacia)")
    @ApiResponse(responseCode = "400", description = "Falta el parametro clienteEmail",
                 content = @Content(schema = @Schema(implementation = ApiError.class),
                                    examples = @ExampleObject(value = EJ_400_PARAMETRO)))
    @GetMapping
    public List<PedidoResponseDTO> listarPorCliente(
        @Parameter(description = "Correo del cliente", example = "ana@correo.com")
        @RequestParam String clienteEmail) {
        return service.listarPorCliente(clienteEmail);
    }
}
