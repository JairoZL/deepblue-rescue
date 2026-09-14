# DeepBlue Rescue — Persistencia con Spring Boot 4


## Integrantes
- Jairo Zapateiro
- Josué Ortiz

## Descripción
Capa de persistencia para un sistema de gestión de rescates de fauna marina.
Modela centros de rescate, casos, animales, expedientes médicos, especialistas,
áreas de experiencia y tratamientos, usando Spring Data JPA, Hibernate, Flyway
y PostgreSQL. Laboratorio académico enfocado exclusivamente en persistencia
(sin capa web/REST/Service).

## Estado actual:
- Esquema completo en Flyway (`V1__create_schema.sql`, `V2__insert_expertise_catalog.sql`)
  con las 7 tablas, constraints (PK, FK, UNIQUE, CHECK) e índices.
- Enums: `RescueStatus`, `AnimalSex`, `TreatmentType`.
- Entidades JPA completas: `RescueCenter`, `RescueCase`, `Animal`, `MedicalRecord`.

Pendiente: `Specialist`, `Expertise`, `Treatment`; repositories; Query Methods;
consultas JPQL; test de integración con Testcontainers; README completo según
checklist final. (Por indicación del docente, el Paso 47 — test de
`flyway_schema_history` — queda excluido del alcance.)

## Modelo de datos y relaciones implementadas
- `RescueCenter` 1:N `RescueCase` (dueño: `RescueCase.rescueCenter`)
- `RescueCase` 1:1 `Animal` (dueño: `Animal.rescueCase`)
- `Animal` 1:1 `MedicalRecord` (dueño: `MedicalRecord.animal`)

## Flyway
Las migraciones en `src/main/resources/db/migration` crean el esquema
(`V1`) e insertan el catálogo de expertise (`V2`). `ddl-auto` está en
`validate`: Hibernate no crea ni modifica el esquema, solo verifica que las
entidades coincidan con lo que Flyway ya aplicó.

## Cómo ejecutar
Requiere PostgreSQL corriendo localmente (o variables de entorno `DB_URL`,
`DB_USER`, `DB_PASSWORD` apuntando a otra instancia):

```bash
mvn spring-boot:run
```
