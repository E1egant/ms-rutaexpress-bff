package cl.rutaexpress.bff;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import cl.rutaexpress.bff.security.AudienceValidator;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(properties = "rutaexpress.services.shipments-url=http://localhost:1")
@AutoConfigureMockMvc
class SecurityTests {

    @Autowired
    MockMvc mvc;

    @Test
    void healthEsPublico() throws Exception {
        mvc.perform(get("/actuator/health")).andExpect(status().isOk());
    }

    @Test
    void sinTokenRetorna401ConJson() throws Exception {
        mvc.perform(get("/api/shipments"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));
    }

    @Test
    void rolIncorrectoRetorna403() throws Exception {
        mvc.perform(get("/api/reports/kpis").with(jwt().authorities(new SimpleGrantedAuthority("ROLE_CLIENTE"))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));
    }

    @Test
    void auditorNoPuedeVerEnvios() throws Exception {
        mvc.perform(get("/api/shipments").with(jwt().authorities(new SimpleGrantedAuthority("ROLE_AUDITOR"))))
                .andExpect(status().isForbidden());
    }

    @Test
    void rolCorrectoPasaLaAutorizacionYLlegaAlProxy() throws Exception {
        mvc.perform(get("/api/shipments").with(jwt().authorities(new SimpleGrantedAuthority("ROLE_CLIENTE"))))
                .andExpect(status().isBadGateway());
    }

    @Test
    void bodegaPuedeCambiarEstadoPeroClienteNo() throws Exception {
        mvc.perform(patch("/api/shipments/1/status").with(jwt().authorities(new SimpleGrantedAuthority("ROLE_BODEGA"))))
                .andExpect(status().isBadGateway());
        mvc.perform(patch("/api/shipments/1/status").with(jwt().authorities(new SimpleGrantedAuthority("ROLE_CLIENTE"))))
                .andExpect(status().isForbidden());
    }

    @Test
    void rutaNoDeclaradaSeDeniega() throws Exception {
        mvc.perform(get("/otra-ruta").with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))))
                .andExpect(status().isForbidden());
    }

    @Test
    void validadorDeAudienciaRechazaTokenDeOtraApi() {
        AudienceValidator validator = new AudienceValidator("api://rutaexpress");
        assertThat(validator.validate(token("api://otra")).hasErrors()).isTrue();
        assertThat(validator.validate(token("api://rutaexpress")).hasErrors()).isFalse();
    }

    private static Jwt token(String audience) {
        return Jwt.withTokenValue("t").header("alg", "none").audience(List.of(audience))
                .issuedAt(Instant.now()).expiresAt(Instant.now().plusSeconds(60)).build();
    }
}
