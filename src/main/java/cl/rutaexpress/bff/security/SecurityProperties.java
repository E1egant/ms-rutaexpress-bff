package cl.rutaexpress.bff.security;

import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "rutaexpress.security")
public record SecurityProperties(String tenantId, String audience, List<String> allowedOrigins) {

    public String issuer() {
        return "https://login.microsoftonline.com/" + tenantId + "/v2.0";
    }

    public String jwkSetUri() {
        return "https://login.microsoftonline.com/" + tenantId + "/discovery/v2.0/keys";
    }
}
