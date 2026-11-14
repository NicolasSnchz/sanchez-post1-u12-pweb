package com.tienda.pedidos.controller;

import com.tienda.pedidos.dto.PedidoResponseDTO;
import com.tienda.pedidos.exception.RecursoNoEncontradoException;
import com.tienda.pedidos.service.PedidoService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// Capa web: el controller y GlobalExceptionHandler reales, el servicio como mock. Cada respuesta de
// error debe salir como ApiError con su codigo correcto
@WebMvcTest(PedidoController.class)
class PedidoControllerTest {

    private static final String VALIDO = """
        {"clienteEmail":"ana@correo.com","tipoCliente":"VIP","subtotal":100000}""";

    @Autowired MockMvc mvc;
    @MockBean PedidoService service;

    @Test
    void post_valido_retorna201ConElDtoYSinNotasInternas() throws Exception {
        when(service.crear(any())).thenReturn(respuesta());

        mvc.perform(post("/api/pedidos").contentType(MediaType.APPLICATION_JSON).content(VALIDO))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.total").value(101150.0))
            .andExpect(jsonPath("$.notasInternas").doesNotExist());
    }

    @Test
    void post_bodyVacio_retorna400ConLosTresErroresDeValidacion() throws Exception {
        mvc.perform(post("/api/pedidos").contentType(MediaType.APPLICATION_JSON).content("{}"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.status").value(400))
            .andExpect(jsonPath("$.error").value("Bad Request"))
            .andExpect(jsonPath("$.mensaje").value(
                "clienteEmail: El email del cliente es obligatorio; subtotal: El subtotal es obligatorio; "
                + "tipoCliente: El tipo de cliente es obligatorio"))
            .andExpect(jsonPath("$.path").value("/api/pedidos"));
        verifyNoInteractions(service);
    }

    @Test
    void post_subtotalNegativoYEmailInvalido_retorna400() throws Exception {
        mvc.perform(post("/api/pedidos").contentType(MediaType.APPLICATION_JSON)
                .content("{\"clienteEmail\":\"no-es-correo\",\"tipoCliente\":\"VIP\",\"subtotal\":-5}"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.mensaje").value(containsString("El subtotal debe ser mayor a cero")))
            .andExpect(jsonPath("$.mensaje").value(containsString("no tiene un formato valido")));
    }

    @Test
    void post_jsonMalFormado_retorna400() throws Exception {
        mvc.perform(post("/api/pedidos").contentType(MediaType.APPLICATION_JSON).content("{\"subtotal\":"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.mensaje").value("El cuerpo de la peticion no es un JSON valido"));
    }

    @Test
    void get_idInexistente_retorna404ConApiError() throws Exception {
        when(service.buscarPorId(999L)).thenThrow(new RecursoNoEncontradoException("Pedido", 999L));

        mvc.perform(get("/api/pedidos/999"))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.status").value(404))
            .andExpect(jsonPath("$.error").value("Not Found"))
            .andExpect(jsonPath("$.mensaje").value("Pedido con id 999 no encontrado."))
            .andExpect(jsonPath("$.path").value("/api/pedidos/999"))
            .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    void get_idNoNumerico_retorna400() throws Exception {
        mvc.perform(get("/api/pedidos/abc"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.mensaje").value("El parametro 'id' tiene un valor invalido: abc"));
    }

    @Test
    void listar_sinClienteEmail_retorna400() throws Exception {
        mvc.perform(get("/api/pedidos"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.mensaje").value("Falta el parametro obligatorio 'clienteEmail'"));
    }

    @Test
    void get_errorInesperado_retorna500SinFiltrarElDetalleInterno() throws Exception {
        when(service.buscarPorId(1L)).thenThrow(new IllegalStateException("detalle interno de la base"));

        mvc.perform(get("/api/pedidos/1"))
            .andExpect(status().isInternalServerError())
            .andExpect(jsonPath("$.mensaje").value("Error inesperado. Contactar soporte."));
    }

    @Test
    void metodoNoPermitido_conservaSu405EnVezDeUn500() throws Exception {
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete("/api/pedidos/1"))
            .andExpect(status().isMethodNotAllowed())
            .andExpect(jsonPath("$.status").value(405));
    }

    private PedidoResponseDTO respuesta() {
        PedidoResponseDTO r = new PedidoResponseDTO();
        r.setId(1L);
        r.setClienteEmail("ana@correo.com");
        r.setTipoCliente("VIP");
        r.setSubtotal(100000.0);
        r.setDescuento(15000.0);
        r.setIva(16150.0);
        r.setTotal(101150.0);
        r.setEstado("CREADO");
        return r;
    }
}
