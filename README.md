# ms-rutaexpress-bff

BFF de RutaExpress (Spring Boot 4.1, Java 21). Recibe las llamadas desde AWS API Gateway, valida el JWT de Azure AD y reenvía la petición al microservicio de dominio.

Flujo: `JWT → API Gateway → ms-rutaexpress-bff → microservicio de dominio`

## Seguridad

- Valida firma (JWKS de Azure AD), expiración, `issuer` y `audience` del token.
- Los roles salen del claim `roles` (`Admin`, `Despachador`, `Cliente`, `Auditor`) y se mapean a `ROLE_*`.
- Respuestas `401` (token ausente/inválido) y `403` (rol sin permiso) en JSON.
- CORS limitado a los orígenes configurados. Cualquier ruta no declarada se deniega.

| Ruta | Métodos / roles |
|---|---|
| `/api/shipments/**` | GET, POST: Admin, Despachador, Cliente · PUT: Admin, Despachador · DELETE: Admin |
| `/api/catalog/**` | GET: Admin, Despachador · resto: Admin |
| `/api/report/**` | Admin |
| `/api/audit/**` | Admin, Auditor |
| `/actuator/health` | público |

## Variables de entorno

| Variable | Descripción | Por defecto |
|---|---|---|
| `AZURE_TENANT_ID` | Tenant de Azure AD | `common` |
| `AZURE_API_AUDIENCE` | Audiencia esperada (`api://<API_CLIENT_ID>`) | `api://rutaexpress` |
| `ALLOWED_ORIGINS` | Orígenes CORS permitidos | `http://localhost:5173` |
| `SHIPMENTS_URL`, `CATALOG_URL`, `AUDIT_URL`, `REPORT_URL` | URLs de los microservicios | `localhost:8081..8084` |
| `PORT` | Puerto del BFF | `8080` |

## Ejecutar

```bash
./mvnw test
AZURE_TENANT_ID=<tenant> AZURE_API_AUDIENCE=api://<api-client-id> ./mvnw spring-boot:run
```

Coordinación entre repos, contratos y reglas de trabajo: repositorio `Cloud-Native-1`.
