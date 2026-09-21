package cl.rutaexpress.bff.proxy;

import java.net.URI;
import java.util.Map;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import jakarta.servlet.http.HttpServletRequest;

@RestController
@EnableConfigurationProperties(ServicesProperties.class)
public class ProxyController {

    private final Map<String, String> targets;
    private final RestClient client = RestClient.create();

    public ProxyController(ServicesProperties props) {
        this.targets = Map.of(
                "/api/shipments", props.shipmentsUrl(),
                "/api/catalog", props.catalogUrl(),
                "/api/audit", props.auditUrl(),
                "/api/reports", props.reportUrl(),
                "/api/notifications", props.notifyUrl());
    }

    @RequestMapping({"/api/shipments/**", "/api/shipments", "/api/catalog/**", "/api/catalog",
            "/api/audit/**", "/api/audit", "/api/reports/**", "/api/reports",
            "/api/notifications/**", "/api/notifications"})
    public ResponseEntity<byte[]> forward(HttpServletRequest request,
            @RequestBody(required = false) byte[] body) {
        String path = request.getRequestURI();
        String base = targets.entrySet().stream()
                .filter(e -> path.equals(e.getKey()) || path.startsWith(e.getKey() + "/"))
                .map(Map.Entry::getValue)
                .findFirst()
                .orElseThrow();
        String query = request.getQueryString();
        URI target = URI.create(base + path + (query == null ? "" : "?" + query));

        RestClient.RequestBodySpec spec = client.method(HttpMethod.valueOf(request.getMethod()))
                .uri(target)
                .header(HttpHeaders.AUTHORIZATION, request.getHeader(HttpHeaders.AUTHORIZATION));
        if (request.getContentType() != null) {
            spec.contentType(MediaType.parseMediaType(request.getContentType()));
        }
        if (body != null && body.length > 0) {
            spec.body(body);
        }

        ResponseEntity<byte[]> response = spec.retrieve()
                .onStatus(status -> true, (req, res) -> { })
                .toEntity(byte[].class);
        ResponseEntity.BodyBuilder out = ResponseEntity.status(response.getStatusCode());
        MediaType contentType = response.getHeaders().getContentType();
        if (contentType != null) {
            out.contentType(contentType);
        }
        return out.body(response.getBody());
    }

    @ExceptionHandler(ResourceAccessException.class)
    ResponseEntity<Map<String, Object>> serviceUnavailable(ResourceAccessException ex) {
        return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                .body(Map.of("status", 502, "error", "Bad Gateway",
                        "message", "El microservicio de destino no responde"));
    }
}
