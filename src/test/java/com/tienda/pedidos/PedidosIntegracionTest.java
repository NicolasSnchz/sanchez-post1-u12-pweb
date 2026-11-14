package com.tienda.pedidos;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tienda.pedidos.repository.PedidoRepository;
import com.tienda.pedidos.strategy.EstrategiaDescuento;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// La aplicacion completa con H2: el Map de estrategias que arma Spring y el recorrido POST -> GET
@SpringBootTest
@AutoConfigureMockMvc
class PedidosIntegracionTest {

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired PedidoRepository repo;
    @Autowired Map<String, EstrategiaDescuento> estrategias;

    @Test
    void contexto_registraLasTresEstrategiasConElNombreDelBeanComoLlave() {
        assertThat(estrategias).containsOnlyKeys("REGULAR", "VIP", "MAYORISTA");
    }

    @Test
    void postYGet_pedidoVip_guardaNotasInternasEnLaBaseYNoLasExpone() throws Exception {
        String cuerpo = mvc.perform(post("/api/pedidos").contentType(MediaType.APPLICATION_JSON)
                .content("{\"clienteEmail\":\"ana@correo.com\",\"tipoCliente\":\"VIP\",\"subtotal\":100000}"))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.notasInternas").doesNotExist())
            .andReturn().getResponse().getContentAsString();
        JsonNode creado = json.readTree(cuerpo);
        long id = creado.get("id").asLong();

        mvc.perform(get("/api/pedidos/" + id))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.descuento").value(15000.0))
            .andExpect(jsonPath("$.iva").value(16150.0))
            .andExpect(jsonPath("$.total").value(101150.0))
            .andExpect(jsonPath("$.notasInternas").doesNotExist());

        // notasInternas si quedo en la base: el DTO es el que no la deja salir
        assertThat(repo.findById(id).orElseThrow().getNotasInternas()).isNotBlank();
    }

    @Test
    void get_idInexistente_responde404YaNo200Vacio() throws Exception {
        mvc.perform(get("/api/pedidos/999999"))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.mensaje").value("Pedido con id 999999 no encontrado."));
    }
}
