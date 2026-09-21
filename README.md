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
| `/api/shipments/**` | GET: Admin, Operador, Bodega, Despachador, Cliente · POST: Admin, Operador, Despachador, Cliente · PATCH (estado): Admin, Operador, Bodega, Despachador · DELETE: Admin |
| `/api/catalog/**` | GET: Admin, Operador, Bodega, Despachador · resto: Admin |
| `/api/reports/**`, `/api/notifications/**` | Admin |
| `/api/audit/**` | Admin, Auditor |
| `/actuator/health` | público |

Los roles del token pueden ser los implementados por los servicios (`Admin`, `Operador`, `Bodega`) o los del caso (`Despachador`, `Cliente`, `Auditor`); ver `contratos/roles.md` en el repo central.

## Variables de entorno

| Variable | Descripción | Por defecto |
|---|---|---|
| `AZURE_TENANT_ID` | Tenant de Azure AD | `common` |
| `AZURE_API_AUDIENCE` | Audiencia esperada (`api://<API_CLIENT_ID>`) | `api://rutaexpress` |
| `ALLOWED_ORIGINS` | Orígenes CORS permitidos | `http://localhost:5173` |
| `SHIPMENTS_URL`, `CATALOG_URL`, `NOTIFY_URL`, `REPORT_URL`, `AUDIT_URL` | URLs de los microservicios | `localhost:8081`, `8082`, `8083`, `8084`, `8085` |
| `PORT` | Puerto del BFF | `8080` |

## Pruebas

`./mvnw test` ejecuta 14 pruebas: 401/403/rol válido/PATCH por rol/ruta denegada (`SecurityTests`) y validación de token (`TokenValidationTests`): firma RSA correcta y de otra clave, expirado, issuer y audience distintos.

## Ejecutar

```bash
./mvnw test
AZURE_TENANT_ID=<tenant> AZURE_API_AUDIENCE=api://<api-client-id> ./mvnw spring-boot:run
```

Coordinación entre repos, contratos y reglas de trabajo: repositorio `Cloud-Native-1`.
