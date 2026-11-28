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
    $env:SENSOR_API_KEY = "node-secure-local-device-key"
    .\mvnw spring-boot:run
    ```

    En macOS o Linux define primero `export SENSOR_API_KEY="node-secure-local-device-key"` y ejecuta `./mvnw spring-boot:run`. La base `node_secure` se crea sola. La clave es solo de desarrollo; en producción utiliza un secreto diferente.

3. Abre Swagger: <http://localhost:8080/swagger-ui.html>
4. Inicia sesión en `POST /api/v1/authentication/sign-in` con un usuario de prueba, copia el `token` y pégalo en el botón **Authorize** de Swagger.

En una base local vacía se crea la organización "NodeSecure Demo" con su propietario `fperez` (contraseña `IAM_DEFAULT_PASSWORD`, por defecto `12345678`). Los trabajadores se crean desde `POST /api/v1/users` o desde "Equipos y accesos". Desactívalo con `IAM_SEED_DEMO_DATA=false`.

Pruebas (unitarias + integración contra un MySQL 8 aislado en Docker con Testcontainers; requiere Docker Desktop en ejecución):

```powershell
.\mvnw clean test
```

Las pruebas de integración nunca usan FreeDB: arrancan su propio contenedor `mysql:8.0` y abortan si la URL no apunta a ese contenedor.

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
| `SENSOR_API_KEY` | Sin valor por defecto | Clave secreta requerida por `POST /api/v1/physical-events` |
| `SENSOR_CONNECTION_TIMEOUT_SECONDS` | `120` | Umbral provisional sin eventos para marcar Offline; debe ajustarse al intervalo real de heartbeat del ESP32 |

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
- **Proteger un endpoint:** usa `@PreAuthorize` con un **permiso** (no con el nombre del rol). El catálogo está en `iam/domain/model/valueobjects/Permission.java` y los permisos por defecto de cada rol en `RolePermissions.java`.

    ```java
    @PreAuthorize("hasAuthority('inventory.entry.create')")
    @PostMapping
    public ResponseEntity<?> createStockEntry(...)
    ```

- **Organización y alcance:** inyecta `IamContextFacade` y usa `requireCompanyId()` para filtrar por organización, `warehouseScope()` (`null` = todos los almacenes del owner) o `canAccessWarehouse(id)` para el alcance del trabajador, y `currentUsername()` para `createdBy`, `performedBy` y `resolvedBy`. Nunca uses un `companyId`, `warehouseId` o usuario enviado por el cliente sin verificarlo.
- **Eventos de dominio:** registra eventos en tu agregado con `registerEvent(...)`. En tu `CommandServiceImpl` sácalos con `pullDomainEvents()` antes de guardar y publícalos con `ApplicationEventPublisher` después de guardar, como en `UserCommandServiceImpl`.
- **Rutas públicas ya configuradas:** `GET /api/v1/subscription-plans/**` (Liz) y `POST /api/v1/physical-events` (Kelber, protegido con su filtro de API key de dispositivo).

## Multiempresa, roles y permisos

- Cada **organización** (`organizations.id = company_id`) es un tenant aislado. El registro público (`sign-up`) siempre crea una organización nueva y a su propietario (`ROLE_OWNER`).
- El propietario crea directamente las cuentas de sus trabajadores (`ROLE_SUPERVISOR` o `ROLE_OPERATOR`), que quedan activas de inmediato, con uno o más almacenes asignados (`user_warehouses`).
- Los permisos efectivos son los del rol más las excepciones por usuario (`user_permissions`). Los permisos administrativos (almacenes, IoT, equipo, suscripción, configuración) son exclusivos del propietario y no se pueden conceder.
- El JWT solo contiene `userId`, `companyId` y `tokenVersion`. Rol, permisos, estado y almacenes se leen de la base en cada petición, de modo que suspender a alguien o quitarle un permiso o un almacén tiene efecto inmediato.
- Recurso de otra organización → `404`; operación sin permiso → `403`.

### Base de datos en FreeDB (phpMyAdmin)

El esquema de producción **no** lo modifica Hibernate (`ddl-auto=validate` en el perfil `prod`). Antes de desplegar esta versión:

1. Exporta un respaldo completo desde phpMyAdmin.
2. Selecciona la base de FreeDB e importa `database/multitenant_rbac.sql`. Es aditivo, se puede reimportar y no borra ni sobrescribe datos.
3. Revisa los resultados de las consultas de verificación del final del script.
4. Importa después `database/saas_traceability_reporting.sql` (T25–T31: empresas, planes, suscripciones, facturas, bitácora y reportes). También es aditivo y reimportable.

## Endpoints de IAM

| Método | Ruta | Acceso |
| --- | --- | --- |
| POST | `/api/v1/authentication/sign-in` | Público |
| POST | `/api/v1/authentication/sign-up` | Público: crea organización + propietario |
| GET | `/api/v1/authentication/me` | Usuario con sesión |
| GET | `/api/v1/users` · `/api/v1/users/{id}` | `users.view` (solo mi organización) |
| POST | `/api/v1/users` | `users.create` |
| PATCH | `/api/v1/users/{id}` | `users.update` (perfil, rol, almacenes) |
| PUT | `/api/v1/users/{id}/permissions` | `users.update` |
| POST | `/api/v1/users/{id}/suspend` · `/reactivate` | `users.suspend` |
| DELETE | `/api/v1/users/{id}` | `users.update` |
| GET | `/api/v1/permissions` | `users.view` |
| GET · PATCH | `/api/v1/organizations/current` | `settings.view` · `settings.manage` |

Los errores se devuelven como `ProblemDetail` (RFC 9457) con el mensaje en inglés o español según el header `Accept-Language`.

## Endpoints de Warehouse Management e IoT Integration

Todos los recursos de gestión requieren un JWT Bearer emitido por `POST /api/v1/authentication/sign-in`. Los registros se aíslan por `companyId` obtenido de la sesión autenticada; el cliente no puede elegir otra empresa enviando un identificador en el body. Los trabajadores solo leen los almacenes, zonas y sensores de los almacenes que tienen asignados; crear, editar o eliminar almacenes, zonas y dispositivos IoT es exclusivo del propietario. El control por zona individual no está implementado (el alcance es por almacén).

| Método | Ruta | Descripción | Acceso |
| --- | --- | --- | --- |
| GET | `/api/v1/warehouses` | Listar almacenes (owner: todos; trabajador: asignados) | `warehouses.view` |
| GET | `/api/v1/warehouses/{id}` | Consultar un almacén | `warehouses.view` |
| POST | `/api/v1/warehouses` | Registrar un almacén | `warehouses.create` (owner) |
| PUT | `/api/v1/warehouses/{id}` | Actualizar un almacén | `warehouses.update` (owner) |
| DELETE | `/api/v1/warehouses/{id}` | Eliminar un almacén vacío | `warehouses.delete` (owner) |
| GET | `/api/v1/warehouse-zones` | Listar zonas de los almacenes visibles | `warehouses.view` |
| GET | `/api/v1/warehouse-zones/{id}` | Consultar una zona | `warehouses.view` |
| POST | `/api/v1/warehouse-zones` | Crear zona y configurar turno/riesgo | `warehouses.update` (owner) |
| PUT | `/api/v1/warehouse-zones/{id}` | Actualizar zona y turno | `warehouses.update` (owner) |
| DELETE | `/api/v1/warehouse-zones/{id}` | Eliminar zona | `warehouses.update` (owner) |
| GET | `/api/v1/sensor-nodes` | Listar sensores y estado de conexión | `iot.devices.view` |
| GET | `/api/v1/sensor-nodes/{id}` | Consultar un sensor | `iot.devices.view` |
| POST | `/api/v1/sensor-nodes` | Registrar/vincular sensor por MAC y zona | `iot.devices.manage` (owner) |
| PUT | `/api/v1/sensor-nodes/{id}` | Actualizar zona/MAC/modelo/tipo | `iot.devices.manage` (owner) |
| DELETE | `/api/v1/sensor-nodes/{id}` | Eliminar sensor sin eventos físicos | `iot.devices.manage` (owner) |
| POST | `/api/v1/physical-events` | Recibir payload de ESP32 y registrar evento | API Key de dispositivo |

Las respuestas de almacenes, zonas y sensores mantienen los nombres de propiedades consumidos por el frontend Angular (`companyId`, `warehouseId`, `zoneId`, `macAddress`, `connectionStatus`, `lastPingAt`, etc.).

### Payload de recepción IoT (TS09)

`POST /api/v1/physical-events` requiere el header `X-Device-Api-Key`. El cuerpo acepta el contrato del frontend (`macAddress`, `eventType`, `occurredAt`) y los alias de payload (`mac`, `state`, `timestamp`). Ejemplo:

```json
{
  "mac": "A4:CF:12:9B:1E:44",
  "state": "DOOR_OPENED",
  "timestamp": "2026-11-11T22:30:00Z"
}
```

Los tipos aceptados son `DOOR_OPENED`, `DOOR_CLOSED` y `NODE_DISCONNECTED`. La MAC debe corresponder a un sensor registrado. La API deriva `sensorNodeId` y `zoneId` desde ese registro y almacena el evento con `reconciled=false`; el payload no puede asignar arbitrariamente una zona. Para conservar la identidad de la evidencia histórica, la MAC no puede modificarse una vez que el sensor tiene eventos registrados.

La clave de dispositivo se configura mediante `SENSOR_API_KEY`. La API falla de forma segura cuando la variable no está definida; configura una clave local antes de iniciar el backend y una clave secreta propia en Render. No uses una clave compartida en el frontend: es exclusiva del dispositivo IoT y debe mantenerse fuera del repositorio.

La comunicación entre los bounded contexts se realiza mediante fachadas ACL: `SensorContextFacade` permite a Warehouse Management comprobar que una zona no tenga sensores antes de eliminarla, mientras que `WarehouseContextFacade` permite a Sensor Integration validar la pertenencia de una zona a la empresa autenticada y consultar su configuración (riesgo y turno operativo).

El estado del sensor comienza como `OFFLINE`. Cada evento normal recibido actualiza `lastPingAt` y lo marca `ONLINE`; `NODE_DISCONNECTED` lo marca `OFFLINE`. Al consultar los sensores, un último ping que supere `SENSOR_CONNECTION_TIMEOUT_SECONDS` (por defecto, 120 segundos) también se marca `OFFLINE`.

## Endpoints de suscripción, facturación, trazabilidad y reportes (T25–T31)

| TS | Método | Ruta | Permiso |
| --- | --- | --- | --- |
| T25 | POST | `/api/v1/companies` | `settings.manage` (RUC de 11 dígitos con dígito verificador SUNAT, único) |
| T25 | GET | `/api/v1/companies`, `/api/v1/companies?taxId=`, `/api/v1/companies/{id}` | `settings.view` |
| T26 | GET | `/api/v1/subscription-plans`, `/api/v1/subscription-plans/{id}` | Público |
| T27 | POST | `/api/v1/subscriptions` (`planId`, `planCode` o `planName`) | `subscriptions.manage` |
| T27 | PATCH | `/api/v1/subscriptions/{id}/plan` | `subscriptions.manage` |
| T27 | POST | `/api/v1/subscriptions/{id}/cancellation` | `subscriptions.manage` |
| T27 | GET | `/api/v1/subscriptions`, `/api/v1/subscriptions/current`, `/api/v1/subscriptions/{id}` | `subscriptions.view` |
| T28 | GET | `/api/v1/invoices`, `/api/v1/invoices?subscriptionId=`, `/api/v1/invoices/{id}` | `subscriptions.view` |
| T29/T30 | GET | `/api/v1/traceability-records?warehouseId=&recordType=&eventType=&from=&to=&page=&size=&direction=`, `/api/v1/traceability-records/{id}` | `traceability.view` |
| T31 | POST | `/api/v1/reporting/incident-reports` (`warehouseId`, `year`, `month`) | `reports.create` |
| T31 | GET | `/api/v1/reporting/{id}`, `/api/v1/reporting/{id}/export` (CSV) | `reports.view` |

- Precio y límites siempre salen del catálogo: el cliente no puede fijarlos. Activar o cambiar de plan emite la factura del periodo en la misma transacción. No hay pasarela de pago: las facturas quedan en estado `ISSUED`.
- Transiciones: `ACTIVE → ACTIVE` (cambio de plan) y `ACTIVE → CANCELLED` (final). Una sola suscripción activa por organización. No se permite un plan cuyos límites sean menores que el uso actual (almacenes, nodos, usuarios).
- La bitácora se llena sola con los eventos de dominio de usuarios, sensores, inventario y alertas, en la misma transacción que la operación. No tiene endpoints de escritura, modificación ni borrado.
- Un reporte por organización, almacén y mes (clave única). Si el mes seguía abierto al generarlo, se actualiza en lugar de duplicarse; un mes sin incidencias indica "Cero Incidentes Registrados".

## Despliegue

El `Dockerfile` compila con Maven y corre con el perfil `prod`. En Render crea un Web Service desde este repositorio y define `SPRING_PROFILES_ACTIVE=prod`, `DATABASE_URL`, `DATABASE_USER`, `DATABASE_PASSWORD` y `JWT_SECRET`.

## Licencia

MIT
