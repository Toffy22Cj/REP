# Migraciones del esquema (Flyway)

REP usa **Flyway** para los cambios de esquema que Hibernate no puede hacer solo
(cambios de tipo, renombres, datos) y mantiene `ddl-auto=update` para crear las tablas y
columnas nuevas.

## Cómo se aplica

- Al arrancar, Flyway ejecuta las migraciones pendientes **antes** de Hibernate.
- Una base existente sin historial (`flyway_schema_history`) se marca como **versión 1**
  (`baseline-on-migrate`) y solo recibe las migraciones posteriores.
- En una base nueva y vacía, las migraciones no encuentran tablas y no hacen nada;
  Hibernate crea el esquema.

| Versión | Tipo | Qué hace |
|---|---|---|
| 1 | baseline | Esquema existente creado por `ddl-auto=update` |
| 2 | Java (`db.migration.V2__Enums_a_varchar`) | Convierte las columnas `ENUM` nativas de MySQL/MariaDB a `VARCHAR(50)` conservando datos, nulabilidad y valor por defecto. Sin esto, añadir un valor a un enumerado (p. ej. `RETARDO`) rompía los INSERT. |

## Reglas para escribir migraciones nuevas

1. **Haga copia de la base antes de actualizar en producción** (`mysqldump colegio > copia.sql`).
2. Nombre: `V<n>__Descripcion` (SQL en `src/main/resources/db/migration`, Java en
   `src/main/java/db/migration`). Nunca modifique una migración ya publicada.
3. Deben ser **idempotentes y defensivas**: en una instalación nueva se ejecutan antes
   de que Hibernate cree las tablas. Compruebe en `information_schema` que la tabla o
   columna exista (vea la migración V2 como ejemplo).
4. No destructivas: nada de `DROP` de columnas con datos sin una migración de datos
   previa y aprobación explícita.
5. Los enumerados nuevos se declaran con `@Enumerated(EnumType.STRING)` +
   `@JdbcTypeCode(SqlTypes.VARCHAR)`.

## Variables

| Variable | Por defecto | Uso |
|---|---|---|
| `FLYWAY_ENABLED` | `true` | Desactivar migraciones (solo para diagnóstico) |
| `DDL_AUTO` | `update` | Estrategia de Hibernate (`validate` cuando todo el esquema esté versionado) |

## Verificación realizada

Con MariaDB 10.11: una base creada por la versión anterior (9 columnas `ENUM`, 14
usuarios, 50 asistencias) quedó en versión 2 con 0 `ENUM` y los datos intactos; una base
nueva se creó sin `ENUM`. El workflow `.github/workflows/ci.yml` (trabajo `mysql`) repite el
arranque contra MySQL 8 cuando GitHub Actions está disponible en la cuenta.
