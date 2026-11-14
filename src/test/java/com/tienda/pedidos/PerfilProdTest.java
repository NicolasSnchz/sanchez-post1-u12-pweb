package com.tienda.pedidos;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// El perfil prod real (application-prod.properties, script de esquema y ddl-auto=validate), con la
// base cambiada por variable de entorno, igual que en Render: aqui H2 en modo PostgreSQL para no
// depender de un servidor. Comprueba que Actuator solo expone health e info
@SpringBootTest(properties = {
    "DATASOURCE_URL=jdbc:h2:mem:prodtest;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
    "DATASOURCE_USER=sa"
})
@AutoConfigureMockMvc
@ActiveProfiles("prod")
class PerfilProdTest {

    @Autowired MockMvc mvc;

    @Test
    void actuatorHealth_perfilProd_respondeUpSinDetalles() throws Exception {
        mvc.perform(get("/actuator/health"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("UP"))
            .andExpect(jsonPath("$.components").doesNotExist());
    }

    @Test
    void actuatorBeansYEnv_perfilProd_noEstanExpuestos() throws Exception {
        mvc.perform(get("/actuator/beans")).andExpect(status().isNotFound());
        mvc.perform(get("/actuator/env")).andExpect(status().isNotFound());
    }

    @Test
    void crearPedido_perfilProd_usaLaTablaDelScriptValidadaPorHibernate() throws Exception {
        mvc.perform(post("/api/pedidos").contentType("application/json")
                .content("{\"clienteEmail\":\"ana@correo.com\",\"tipoCliente\":\"VIP\",\"subtotal\":100000}"))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.total").value(101150.0));
    }
}
