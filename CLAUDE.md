# DeepBlue Rescue

Proyecto de laboratorios en Spring Boot (persistencia y capa de servicio).

## Flujo de Git: Git Flow

### Ramas permanentes
- `main`: código estable, solo versiones liberadas. Cada merge a `main` lleva un tag de versión (`vX.Y.Z`).
- `develop`: rama de integración. Aquí confluyen todas las funcionalidades terminadas.

### Ramas temporales
| Tipo | Se crea desde | Se fusiona hacia | Nombre |
|---|---|---|---|
| `feature/*` | `develop` | `develop` | `feature/<descripcion-corta>` |
| `release/*` | `develop` | `main` y `develop` | `release/<X.Y.Z>` |
| `hotfix/*` | `main` | `main` y `develop` | `hotfix/<descripcion-corta>` |

### Reglas
1. Nunca hacer commit directo en `main` ni en `develop`; todo entra por Pull Request.
2. Una rama `feature/*` por tarea o funcionalidad; ramas cortas y de un solo propósito.
3. Antes de abrir un PR, actualizar la rama con `develop` y verificar que compile y pasen las pruebas (`mvn test`).
4. Los PR a `develop` se fusionan con merge commit (`--no-ff`) para conservar el historial de la funcionalidad.
5. `release/*` solo admite correcciones menores y ajuste de versión; no se agregan funcionalidades nuevas.
6. `hotfix/*` se usa solo para errores críticos en `main`; al cerrarlo se fusiona también en `develop`.
7. Eliminar la rama temporal una vez fusionada.

### Mensajes de commit
Formato: `<tipo>(<alcance>): <descripcion en imperativo>`

Tipos: `feat`, `fix`, `docs`, `refactor`, `test`, `chore`.

Ejemplos:
- `feat(persistencia): agregar repositorio de rescates`
- `fix(servicio): validar nulos al crear una misión`
- `test(servicio): cubrir caso de rescate duplicado`

Commits pequeños y atómicos: un cambio lógico por commit.

## Instrucciones para Claude Code
- Antes de cualquier cambio, verificar la rama actual con `git branch --show-current`. Si es `main` o `develop`, crear primero la rama `feature/*` correspondiente.
- No hacer `push` ni fusiones a `main` o `develop` sin que el usuario lo pida explícitamente.
- No reescribir historial compartido (`push --force`, `rebase` de ramas ya publicadas).
- Ejecutar `mvn test` antes de proponer un commit o PR.
- Para tareas de aprendizaje, guiar al usuario paso a paso en lugar de resolver todo directamente.
