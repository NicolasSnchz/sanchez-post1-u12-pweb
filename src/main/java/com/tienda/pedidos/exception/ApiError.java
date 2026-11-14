package com.tienda.pedidos.exception;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

// Cuerpo unico de error de la API: el cliente siempre recibe la misma forma, sea 400, 404 o 500
@Schema(description = "Cuerpo de error de la API")
public class ApiError {

    @Schema(description = "Codigo HTTP", example = "404")
    private final int status;
    @Schema(description = "Nombre del codigo HTTP", example = "Not Found")
    private final String error;
    @Schema(description = "Detalle del error", example = "Pedido con id 999 no encontrado.")
    private final String mensaje;
    @Schema(description = "Momento del error", example = "2026-11-13T20:00:00")
    private final String timestamp;
    @Schema(description = "Ruta que se pidio", example = "/api/pedidos/999")
    private final String path;

    public ApiError(int status, String error, String mensaje, String path) {
        this.status = status;
        this.error = error;
        this.mensaje = mensaje;
        this.timestamp = LocalDateTime.now().withNano(0).toString();
        this.path = path;
    }

    public int getStatus() { return status; }
    public String getError() { return error; }
    public String getMensaje() { return mensaje; }
    public String getTimestamp() { return timestamp; }
    public String getPath() { return path; }
}
