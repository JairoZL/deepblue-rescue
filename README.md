# DeepBlue Rescue

Backend para gestionar el rescate y la rehabilitación de fauna marina, construido por capas
con Spring Boot 4. Proyecto académico de la asignatura Programación para Web.

## Integrantes

- Jairo Zapateiro
- Josué Ortiz

## Estado

| Laboratorio | Contenido | Versión |
|---|---|---|
| Persistencia | Esquema Flyway, entidades JPA, repositories, Query Methods, JPQL y tests de integración | incluida en `v0.2.0` |
| Capa de servicio | DTOs, MapStruct, excepciones, servicios con reglas de negocio y tests unitarios | `v0.2.0` |

Pendiente en laboratorios posteriores: controllers REST, `GlobalExceptionHandler` y seguridad.

## Stack

Java 21 · Spring Boot 4.1 · Spring Data JPA / Hibernate · Flyway · PostgreSQL 18 ·
MapStruct 1.6 · Bean Validation · JUnit 5 · Mockito · AssertJ · Testcontainers

## Arquitectura

```text
service      interfaces (contrato + validación) e impl (@Service, @Transactional)
   │  usa
   ├── repository   Spring Data JPA (Query Methods y JPQL)
   └── mapper       MapStruct: Entity → DTO
dto          records de request (con Bean Validation) y response
domain       entidades JPA y enums
exception    ResourceNotFoundException · BusinessRuleException
```

- Los servicios exponen DTOs (`record`), nunca entidades.
- Las clases de servicio son `@Transactional(readOnly = true)`; los métodos de escritura
  (`changeStatus`, `register`) usan `@Transactional`.
- La inyección de dependencias es por constructor.

## Modelo de datos

- `RescueCenter` 1:N `RescueCase`
- `RescueCase` 1:1 `Animal` (dueño: `Animal.rescueCase`)
- `Animal` 1:1 `MedicalRecord` (dueño: `MedicalRecord.animal`)
- `Specialist` N:M `Expertise` (tabla `specialist_expertise`)
- `Treatment` N:1 `Animal` y N:1 `Specialist`

El esquema lo crea Flyway (`src/main/resources/db/migration`):

| Migración | Contenido |
|---|---|
| `V1__create_schema.sql` | 8 tablas, PK, FK, UNIQUE, CHECK e índices |
| `V2__insert_expertise_catalog.sql` | Catálogo de áreas de experiencia |
| `V3__add_tracking_device_to_animal.sql` | Columna `tracking_device_code` (única) en `animals` |

Hibernate está en `ddl-auto: validate`: solo verifica que las entidades coincidan con el esquema.

## Reglas de negocio

**Flujo de un caso de rescate** (`RescueCaseService.changeStatus`), solo hacia adelante y un paso a la vez:

```text
ADMITTED → UNDER_EVALUATION → IN_REHABILITATION → READY_FOR_RELEASE → RELEASED
```

`RELEASED` y `CLOSED` no admiten transiciones.

**Registro de un tratamiento** (`TreatmentService.register`):

1. El animal debe existir (`ResourceNotFoundException`).
2. El especialista debe existir (`ResourceNotFoundException`).
3. El especialista debe estar activo (`BusinessRuleException`).
4. El caso debe estar en `UNDER_EVALUATION` o `IN_REHABILITATION` (`BusinessRuleException`).
   Esta regla está en `RescueStatus.allowsTreatments()` y la comparte `AnimalService.canReceiveTreatment`.
5. La fecha del tratamiento no puede ser anterior a la fecha de rescate (`BusinessRuleException`).

## Cómo ejecutar

La aplicación necesita PostgreSQL. Por defecto se conecta a `jdbc:postgresql://localhost:5432/deepblue`
con usuario y contraseña `postgres`; se puede cambiar con las variables `DB_URL`, `DB_USER` y `DB_PASSWORD`.

```bash
./mvnw spring-boot:run
```

Para levantarla sin instalar PostgreSQL (requiere Docker), ejecutar `TestDeepblueRescueApplication`
desde el IDE: usa el mismo contenedor de Testcontainers que los tests.

## Tests

```bash
./mvnw test     # unitarios: servicios (Mockito) y mappers. No necesita Docker
./mvnw verify   # unitarios + integración (*IT, con Testcontainers). Necesita Docker
```

| Tipo | Clases | Qué verifica |
|---|---|---|
| Unitarios (Surefire) | `*ServiceImplTest`, `MapperTest` | Reglas de negocio con repositories simulados; mappers generados por MapStruct |
| Integración (Failsafe) | `PersistenceIT`, `ServiceValidationIT`, `DeepblueRescueApplicationIT` | Migraciones, relaciones y consultas sobre PostgreSQL real; validación de los servicios |

Todos los tests de integración comparten un contenedor `postgres:18-alpine` definido en
`TestcontainersConfiguration`.

## Flujo de trabajo

El repositorio usa **Git Flow**:

| Rama | Uso |
|---|---|
| `main` | Versiones entregadas, con tag `vX.Y.Z` |
| `develop` | Integración |
| `feature/*` | Un cambio, sale de `develop` y vuelve por PR |
| `release/X.Y.Z` | Cierre de una entrega, de `develop` a `main` |
| `hotfix/X.Y.Z` | Corrección sobre `main` |

- Commits con [Conventional Commits](https://www.conventionalcommits.org/) (`feat:`, `fix:`, `test:`, `build:`, `ci:`, `docs:`).
- `main` y `develop` están protegidas: solo reciben PRs con merge commit y con el check **Build and test** en verde.
- **CI** (`.github/workflows/ci.yml`): `./mvnw clean verify` en cada PR y push a `develop`/`main`.
- **Releases** (`.github/workflows/release-tag.yml`): al fusionar `release/X.Y.Z` o `hotfix/X.Y.Z`
  en `main` se crean el tag `vX.Y.Z` y el GitHub Release. Después se hace el back-merge `main → develop` por PR.
