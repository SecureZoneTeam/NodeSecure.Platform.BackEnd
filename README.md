# NodeSecure API

API REST de **NodeSecure** (startup **SafeZone**): seguridad física con sensores IoT y control de inventario para almacenes de PYMES. La consume el frontend [SafeZone-website](https://github.com/SecureZoneTeam/SafeZone-website).

## Tecnologías

| Tecnología | Versión |
| --- | --- |
| Java | 21 |
| Spring Boot (Web, Data JPA, Validation, Security) | 3.5.7 |
| MySQL | 8 |
| springdoc-openapi (Swagger UI) | 2.8.13 |
| JWT (jjwt) | 0.12.6 |
| Lombok | 1.18.48 |
| Maven (wrapper incluido) | 3.9.11 |

## Cómo ejecutarlo

1. Instala JDK 21 y MySQL 8, y deja MySQL corriendo con usuario `root` y contraseña `root` (o cambia las variables de abajo).
2. Desde la terminal de VS Code, en la carpeta del proyecto:

    ```powershell
    .\mvnw spring-boot:run
    ```

    En macOS o Linux usa `./mvnw spring-boot:run`. La base `node_secure` se crea sola.

3. Abre Swagger: <http://localhost:8080/swagger-ui.html>
4. Inicia sesión en `POST /api/v1/authentication/sign-in` con un usuario de prueba, copia el `token` y pégalo en el botón **Authorize** de Swagger.

Usuarios de prueba (contraseña `12345678`):

| Usuario | Rol |
| --- | --- |
| `fperez` | ROLE_ADMINISTRATOR |
| `lanahua` | ROLE_SECURITY_MANAGER |
| `ksandoval` | ROLE_WAREHOUSE_KEEPER |
| `lravello` | ROLE_AUDITOR |

Pruebas unitarias:

```powershell
.\mvnw clean test
```

## Variables de entorno

| Variable | Valor por defecto | Uso |
| --- | --- | --- |
| `SPRING_PROFILES_ACTIVE` | `dev` | `prod` en Render |
| `DATABASE_URL` | `jdbc:mysql://localhost:3306/node_secure?...` | URL JDBC de MySQL |
| `DATABASE_USER` | `root` | Usuario de MySQL |
| `DATABASE_PASSWORD` | `root` | Contraseña de MySQL |
| `JWT_SECRET` | clave de desarrollo | Clave para firmar tokens (mínimo 32 caracteres) |
| `IAM_DEFAULT_PASSWORD` | `12345678` | Contraseña de usuarios de prueba y miembros creados sin contraseña |
| `CORS_ALLOWED_ORIGINS` | `http://localhost:4200,https://securezoneteam.github.io` | Orígenes del frontend |
| `PORT` | `8080` | Puerto del servidor |

## Estructura y responsables

Cada integrante solo hace commits en sus carpetas.

```
src/main/java/com/safezone/nodesecure/platform/
├── NodeSecurePlatformApplication.java   Sebas
├── shared/                              Sebas
├── iam/                                 Sebas
├── warehouses/                          Kelber
├── sensors/                             Kelber
├── profiles/                            Liz
├── subscriptions/                       Liz
├── reporting/                           Liz
├── inventory/                           Luciana
└── alerts/                              Luciana
```

Cada contexto sigue las mismas capas que `iam/`:

```
<contexto>/
├── domain/          agregados, value objects, eventos, puertos de repositorio (Java puro)
├── application/     commands, queries, servicios y su implementación en internal/
├── infrastructure/  entidades JPA, repositorios Spring Data, mappers, adapters, seeders
└── interfaces/      rest/ (controllers, resources, transform) y acl/ (fachada para otros contextos)
```

## Reglas del equipo

- **Ramas:** `main` y `develop` protegidas. Cada uno trabaja en `feature/<contexto>` creada desde `develop` y entra por Pull Request con una revisión.
- **Commits:** Conventional Commits en inglés, por ejemplo `feat(warehouses): add Warehouse aggregate`.
- **Archivos compartidos:** `pom.xml`, `application*.properties`, `shared/` y `SecurityConfig` los cambia Sebas; pídelo en el grupo o en tu PR.
- **Mensajes de error:** agrega tus claves en `messages.properties` y `messages_es.properties` con el prefijo de tu contexto (`warehouse.error...`, `inventory.error...`).
- **Proteger un endpoint por rol:** usa `@PreAuthorize` en tu controller; no hace falta tocar `SecurityConfig`.

    ```java
    @PreAuthorize("hasAnyRole('ADMINISTRATOR', 'WAREHOUSE_KEEPER')")
    @PostMapping
    public ResponseEntity<?> createStockEntry(...)
    ```

- **Usuario actual:** inyecta `IamContextFacade` y usa `currentUsername()`, `currentUserId()`, `currentCompanyId()` o `currentWarehouseId()` para `createdBy`, `performedBy` y filtros por empresa.
- **Eventos de dominio:** registra eventos en tu agregado con `registerEvent(...)`. En tu `CommandServiceImpl` sácalos con `pullDomainEvents()` antes de guardar y publícalos con `ApplicationEventPublisher` después de guardar, como en `UserCommandServiceImpl`.
- **Rutas públicas ya configuradas:** `GET /api/v1/subscription-plans/**` (Liz) y `POST /api/v1/physical-events` (Kelber, protegido con su filtro de API key de dispositivo).

## Endpoints de IAM

| Método | Ruta | Acceso |
| --- | --- | --- |
| POST | `/api/v1/authentication/sign-in` | Público |
| POST | `/api/v1/authentication/sign-up` | Público |
| GET | `/api/v1/users` | Usuario con sesión |
| GET | `/api/v1/users/{id}` | Usuario con sesión |
| POST | `/api/v1/users` | ROLE_ADMINISTRATOR |
| PATCH | `/api/v1/users/{id}` | ROLE_ADMINISTRATOR |
| DELETE | `/api/v1/users/{id}` | ROLE_ADMINISTRATOR |

Los errores se devuelven como `ProblemDetail` (RFC 9457) con el mensaje en inglés o español según el header `Accept-Language`.

## Despliegue

El `Dockerfile` compila con Maven y corre con el perfil `prod`. En Render crea un Web Service desde este repositorio y define `SPRING_PROFILES_ACTIVE=prod`, `DATABASE_URL`, `DATABASE_USER`, `DATABASE_PASSWORD` y `JWT_SECRET`.

## Licencia

MIT
