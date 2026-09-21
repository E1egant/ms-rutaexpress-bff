package cl.rutaexpress.bff.proxy;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "rutaexpress.services")
public record ServicesProperties(String shipmentsUrl, String catalogUrl, String auditUrl, String reportUrl,
        String notifyUrl) {
}
