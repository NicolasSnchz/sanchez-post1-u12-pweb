package com.tienda.pedidos;

import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

import java.io.InputStream;
import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;

// La URL JDBC del perfil prod tal como esta escrita en application-prod.properties, resuelta con las
// variables que entregaria cada plataforma (sin levantar Spring ni conectarse a ninguna base)
class UrlBaseDatosProdTest {

    private String urlDelArchivo() throws Exception {
        Properties prod = new Properties();
        try (InputStream in = getClass().getResourceAsStream("/application-prod.properties")) {
            prod.load(in);
        }
        return prod.getProperty("spring.datasource.url");
    }

    @Test
    void url_conHostPuertoYBaseDelBlueprintDeRender_armaLaUrlJdbc() throws Exception {
        MockEnvironment entorno = new MockEnvironment()
            .withProperty("DATASOURCE_HOST", "dpg-pedidos-a")
            .withProperty("DATASOURCE_PORT", "5432")
            .withProperty("DATASOURCE_DB", "appdb");

        assertThat(entorno.resolvePlaceholders(urlDelArchivo()))
            .isEqualTo("jdbc:postgresql://dpg-pedidos-a:5432/appdb");
    }

    @Test
    void url_conDatasourceUrlCompleta_laUsaTalCual() throws Exception {
        MockEnvironment entorno = new MockEnvironment()
            .withProperty("DATASOURCE_URL", "jdbc:postgresql://otro-host:6543/otra")
            .withProperty("DATASOURCE_HOST", "ignorado");

        assertThat(entorno.resolvePlaceholders(urlDelArchivo()))
            .isEqualTo("jdbc:postgresql://otro-host:6543/otra");
    }

    @Test
    void url_sinVariables_usaElValorPorDefectoDeDesarrollo() throws Exception {
        assertThat(new MockEnvironment().resolvePlaceholders(urlDelArchivo()))
            .isEqualTo("jdbc:postgresql://localhost:5432/appdb");
    }
}
