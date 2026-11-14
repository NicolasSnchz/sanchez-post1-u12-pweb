package com.tienda.pedidos.exception;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.stream.Collectors;

// Manejo centralizado: reemplaza el try/catch que en el codigo de partida devolvia null (200 vacio).
// Ningun controller captura excepciones; todas terminan aqui y salen como ApiError
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(RecursoNoEncontradoException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ApiError handleNotFound(RecursoNoEncontradoException ex, HttpServletRequest req) {
        return new ApiError(404, "Not Found", ex.getMessage(), req.getRequestURI());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiError handleValidation(MethodArgumentNotValidException ex, HttpServletRequest req) {
        // ordenados para que el mensaje sea el mismo en cada llamada
        String errores = ex.getBindingResult().getFieldErrors().stream()
            .map(fe -> fe.getField() + ": " + fe.getDefaultMessage())
            .sorted()
            .collect(Collectors.joining("; "));
        return new ApiError(400, "Bad Request", errores, req.getRequestURI());
    }

    // Peticiones mal formadas: tambien son error del cliente (400), no un error inesperado del servidor
    @ExceptionHandler({HttpMessageNotReadableException.class, MethodArgumentTypeMismatchException.class,
                       MissingServletRequestParameterException.class})
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiError handleSolicitudMalFormada(Exception ex, HttpServletRequest req) {
        String mensaje;
        if (ex instanceof MethodArgumentTypeMismatchException tm) {
            mensaje = "El parametro '" + tm.getName() + "' tiene un valor invalido: " + tm.getValue();
        } else if (ex instanceof MissingServletRequestParameterException mp) {
            mensaje = "Falta el parametro obligatorio '" + mp.getParameterName() + "'";
        } else {
            mensaje = "El cuerpo de la peticion no es un JSON valido";
        }
        return new ApiError(400, "Bad Request", mensaje, req.getRequestURI());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleGeneral(Exception ex, HttpServletRequest req) {
        // Las excepciones propias de Spring MVC (ruta inexistente, metodo no permitido...) ya traen su
        // codigo: se respeta, en vez de convertir un 404 o un 405 en un 500
        if (ex instanceof ErrorResponse respuesta) {
            HttpStatusCode codigo = respuesta.getStatusCode();
            HttpStatus estado = HttpStatus.resolve(codigo.value());
            String razon = estado != null ? estado.getReasonPhrase() : "Error";
            return ResponseEntity.status(codigo)
                .body(new ApiError(codigo.value(), razon, respuesta.getBody().getDetail(), req.getRequestURI()));
        }
        // Error real: se registra completo (con la traza) y al cliente solo le llega un mensaje generico
        log.error("Error inesperado en {} {}", req.getMethod(), req.getRequestURI(), ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(new ApiError(500, "Internal Server Error", "Error inesperado. Contactar soporte.", req.getRequestURI()));
    }
}
