package com.tienda.pedidos;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.env.Environment;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// Prueba de contexto del pipeline: Spring construye la aplicacion completa con el perfil dev (H2 en
// memoria, sin base externa) y Actuator responde. Si un bean o una propiedad fallan, el job
// build-and-test se detiene antes de construir la imagen Docker
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("dev")
class ApplicationContextTest {

    @Autowired Environment entorno;
    @Autowired MockMvc mvc;

    @Test
    void contextLoads_conPerfilDev() {
        assertThat(entorno.getActiveProfiles()).containsExactly("dev");
    }

    @Test
    void actuatorHealth_perfilDev_respondeUpConElDetalleDeLaBase() throws Exception {
        mvc.perform(get("/actuator/health"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("UP"))
            .andExpect(jsonPath("$.components.db.status").value("UP"));
    }

    @Test
    void actuatorInfo_muestraNombreYVersionDelArtefacto() throws Exception {
        mvc.perform(get("/actuator/info"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.app.nombre").value("pedidos"))
            .andExpect(jsonPath("$.app.version").value("0.0.1-SNAPSHOT"));
    }
}
