# REP: informe de evolución y madurez del producto

*Backend `REP` (Spring Boot) + cliente `rep-cliente` (Qt 6 / QML). Fecha: 2026-11-13.*

Convenciones de este informe:

- **[Hecho]**: se puede verificar en el código, en las pruebas o en una fuente citada.
- **[Valoración]**: es una opinión técnica del autor.
- Las fuentes externas están al final, numeradas.

---

## 1. Resumen ejecutivo

REP es una plataforma escolar cliente-servidor. Tiene un backend REST, una consola web
de administración y un cliente nativo de escritorio para los tres roles. Ese cliente
**sí existe** y cubre a profesores y estudiantes.

Se revisó un informe externo de madurez contra el código real. Ese informe acierta en
el diagnóstico general, pero **varias afirmaciones estaban desactualizadas o eran
incorrectas** (sección 2). Además, sus prioridades finales (S3/nube e IA) no encajan
con un requisito del producto: **funcionar con y sin conexión a internet**.

Ese requisito es la restricción de diseño principal. Según el Laboratorio de Economía de
la Educación de la Universidad Javeriana, **el 69,4 % de las sedes educativas rurales de
Colombia no tenía acceso a internet** (datos del censo EDUC-2022) [7]. Por eso REP se
orienta a "primero local" (*offline-first*):

- un servidor en la red local del colegio;
- clientes que siguen funcionando si se cae la conexión;
- sincronización cuando la conexión vuelve.

Es el mismo modelo que usa Kolibri, de Learning Equality [4][5].

En esta fase se implementaron las mejoras del informe que sí aportan y son compatibles
con ese enfoque:

- migraciones versionadas con Flyway;
- periodos académicos ponderados;
- retardo en la asistencia;
- evaluación con temporizador y autoguardado local;
- pruebas automáticas del cliente;
- integración continua (CI) en los dos repositorios.

Lo implementado se resume en la sección 5.

**Madurez recalibrada: 3,3 / 5** [Valoración]. Es un producto profesional en
consolidación. Las brechas principales son estas:

- distribución: instaladores y actualizaciones;
- copias de seguridad automáticas del servidor;
- duración de la sesión cuando se trabaja sin conexión por muchos días;
- control del tiempo de examen del lado del servidor;
- decidir qué canal se usa para la administración (web o escritorio).

---

## 2. Verificación del informe externo recibido

| Afirmación del informe | Estado en el código | Evidencia |
|---|---|---|
| "No existe UI para profesores ni estudiantes" (versión anterior) | **Incorrecta** | `rep-cliente/qml/pages`: 22 pantallas que cubren los 3 roles |
| `ApiRequests.qml` es una capa HTTP heredada que hay que eliminar (OP-QT1) | **Incorrecta** | Es un envoltorio QML de unas 60 líneas sobre el `ApiCliente` C++ (propiedad `api`). La red ya está consolidada en C++ (`HttpClient::red()`), y el formato de error (`ApiError`) se interpreta en `HttpClient::errorMessage`, ahora con pruebas (`tst_httpclient`). Eliminarlo obligaría a crear un ViewModel por pantalla sin beneficio. **Decisión: se mantiene.** |
| Sin clonación de actividades | Desactualizada | `POST /api/actividades/{id}/clonar`. En la fase anterior se corrigió que la copia no quedaba asociada al profesor (`ActividadServiceImpl.clonarActividad`). |
| GradingPage sin vista dividida rápida | Desactualizada | `GradingPage.qml`: vista dividida con "Guardar y siguiente" |
| Materiales sin caché sin conexión | Desactualizada | `AlmacenLocal` + caché de archivos con `ETag`/304 (`ApiCliente`). Probado apagando el servidor. |
| Sin temporizador ni autoguardado | Cierta, **resuelta en esta fase** | §5.4 |
| Asistencia sin retardo ni excusa | Parcial: la excusa ya existía; **retardo añadido** | `Asistencia.EstadoAsistencia.RETARDO` |
| Sin periodos académicos | Cierta, **resuelta en esta fase** | §5.2 |
| El cliente no tiene pruebas | Cierta, **resuelta en esta fase** | `rep-cliente/tests`: 4 suites, 22 casos |
| Sin CI ni migraciones | Cierta, **resuelta en esta fase** | `.github/workflows/ci.yml` en ambos repos; Flyway |
| "9 suites de pruebas en el backend" | Desactualizada | 13 clases, **90 pruebas** (`mvn test`, todas pasan) |
| "15 entidades" | Desactualizada | 17 entidades (se añadió `PeriodoAcademico`) |
| Incidentes de punteros nulos en el login (`SOLUCION_LOGIN_NULLPTR.md`) | Histórico | Son notas de depuración antiguas; el flujo de login actual no reproduce el fallo |
| Duplicidad entre la consola web de admin y el admin en QML | **Cierta** | §6, decisión pendiente del producto |
| Falta la renovación silenciosa del token | **Cierta** | §6 |
| Falta paginación en la API | Parcial | La auditoría está limitada a 5000 registros; los listados de usuarios y actividades no se paginan (volumen escolar bajo) |
| Prioridad: almacenamiento S3/MinIO | **No recomendada a corto plazo** | Va contra el modelo LAN sin internet; el almacenamiento local con `STORAGE_DIR` basta para un colegio |
| Prioridad: generador de preguntas con IA | **No recomendada a corto plazo** | Requiere internet o hardware que los colegios rurales no tienen |

---

## 3. Contexto: por qué *offline-first*

- **[Hecho]** El 69,4 % de las sedes rurales no tenía internet según el censo EDUC-2022
  [7]. Hay mediciones de prensa con cifras parecidas, cercanas al 70 % [8].
- **[Hecho]** El Gobierno reporta grandes despliegues de conectividad desde entonces
  [9], pero la conexión rural sigue siendo intermitente.
- **[Hecho]** Kolibri funciona en un servidor de aula al que se conectan los dispositivos
  por red local. Sincroniza entre equipos y, cuando hay conexión, envía datos a un
  portal central [4][5].
- **[Valoración]** El modelo correcto para REP es igual de simple:
  - un **servidor en la red del colegio** (un PC basta: el backend no depende de
    servicios externos);
  - **clientes resilientes** a cortes de red y de luz.

  Para eso no hace falta nube. Un posible "portal central" quedaría para una etapa
  posterior.

---

## 4. Comparación con referentes (con fuentes)

| Capacidad | Referente y fuente | REP hoy |
|---|---|---|
| Escala nacional Superior / Alto / Básico / Bajo y sistema institucional de evaluación | Decreto 1290 de 2009, art. 5; compilado en el Decreto 1075 de 2015 [1][2] | `EscalaCalificacion.desempeno`, con nota mínima configurable |
| Notas por periodo y boletines | Q10 anuncia calificaciones por periodo y boletines [6] (fuentes comerciales, verificar con el proveedor) | **Nuevo**: periodos ponderados, boletín y planilla por periodo, PDF con desglose |
| Contenido y cuestionarios sin conexión | App de Moodle: descarga de contenido, sincroniza cada 10 min y al reconectar. **Los cuestionarios sin conexión no admiten límite de tiempo** [3] | Caché de consultas y archivos, cola persistente de entregas y, **nuevo**, **cuestionarios cronometrados con autoguardado local** que se envían o se encolan al terminar el tiempo |
| Examen en entorno controlado | Safe Exam Browser: modo kiosco que impide cambiar de aplicación y contraseña para salir [10] | **Nuevo**: pantalla completa opcional. No bloquea el sistema (brecha, §6). |
| Servidor local y sincronización | Kolibri [4][5] | Servidor LAN; los clientes reintentan la cola al reconectar. Sin sincronización entre servidores (no se necesita hoy). |

**[Valoración]** Hay una diferencia útil frente a la app de Moodle. Moodle excluye
del modo sin conexión los cuestionarios con tiempo [3]. REP los admite: el reloj se
ancla a la primera apertura, que queda guardada en disco, y la entrega se encola si no
hay red.

El costo de esa diferencia es que **el servidor no valida el tiempo**. Es la siguiente
mejora recomendada (§6).

---

## 5. Implementado en esta fase

Todo lo de esta sección está verificado. El backend pasa **90/90** pruebas y el cliente
**22/22** casos QtTest. Además se hizo un recorrido de pantallas contra el backend con
datos demo. MariaDB 10.11 se usó como sustituto de MySQL para probar las migraciones.

### 5.1 Migraciones versionadas (Flyway) y enumerados a VARCHAR

**Problema encontrado [Hecho]:**

- Hibernate 6 crea los `@Enumerated(STRING)` como columnas `ENUM` nativas de MySQL.
- `ddl-auto=update` nunca modifica columnas existentes.
- Por eso, añadir un valor nuevo a un enumerado (por ejemplo `RETARDO`) haría fallar
  los INSERT en los colegios que ya tienen la base creada.
- Se comprobó con una base creada por la versión anterior: tenía 9 columnas `ENUM`.

**Solución:**

- Flyway con `baseline-on-migrate`: las bases existentes se marcan como versión 1.
- La migración Java `V2__Enums_a_varchar` convierte cada `ENUM` a `VARCHAR` y conserva
  los datos, la nulabilidad y el valor por defecto.
- Es idempotente y solo actúa en MySQL/MariaDB.
- Las entidades declaran `@JdbcTypeCode(SqlTypes.VARCHAR)`.

**Verificado:**

- Base heredada con datos demo: 14 usuarios y 50 asistencias intactos, 0 columnas `ENUM`.
- Base nueva: 0 `ENUM`.
- La CI repite la prueba contra MySQL 8.

Guía: `docs/migraciones.md`.

### 5.2 Periodos académicos ponderados (Decreto 1290)

- Entidad `PeriodoAcademico` (año, número, nombre, fechas, porcentaje).
- API: `/api/periodos` (consulta, todos los roles) y `/api/admin/periodos` (gestión).
- Validaciones con código de error propio:
  - fechas invertidas: `PERIODO_FECHAS_INVALIDAS`;
  - cruce de fechas con otro periodo: `PERIODO_SOLAPADO`;
  - número repetido en el año: `PERIODO_DUPLICADO`;
  - suma de porcentajes del año mayor que 100 %: `PERIODO_PORCENTAJE_EXCEDIDO`.
- Notas: `periodoId` opcional en el boletín, la planilla y las exportaciones.
- Sin periodo, la **nota anual es el promedio de cada periodo ponderado por su
  porcentaje**. Los periodos sin notas no cuentan y el resto se reescala.
- **Sin periodos configurados, el cálculo no cambia** (no se altera la regla de negocio
  existente).
- Cliente: pantalla de periodos para el administrador; selector de periodo en "Mis
  notas" y en la planilla; columnas por periodo y nota final.
- Pruebas: `PeriodosAcademicosTest` (7 pruebas, incluida la verificación numérica de la
  ponderación).

### 5.3 Asistencia

- Nuevo estado `RETARDO`. Cuenta como asistencia y aparece en el boletín.
- Antes, un estado desconocido se guardaba **en silencio como falla**. Ahora responde
  400 con `CAMPOS_INVALIDOS`.
- Cliente:
  - botonera Presente / Retardo / Excusa / Ausente;
  - conteo en vivo y botón "Todos presentes";
  - nombre completo del estudiante (antes solo el nombre de pila).

### 5.4 Evaluación con temporizador y autoguardado

- El servidor envía `duracionMinutos` junto con las preguntas.
- El cliente guarda en disco un **borrador** (hora de inicio + respuestas) en cada
  cambio:
  - escritura atómica, para que un corte de luz no lo deje a medias;
  - en una carpeta propia, para que "borrar datos sin conexión" no lo elimine.
- Al reabrir la actividad se recuperan las respuestas, y **el reloj no se reinicia**.
  Verificado en el recorrido real.
- Al llegar a cero se envía sola; sin red, queda en la cola.
- Antes de enviar, avisa cuántas preguntas quedan sin responder.
- Tiene un modo "pantalla completa".

### 5.5 Calidad y entrega

- El cliente se reorganizó en la biblioteca `repcore` (lógica) más el ejecutable, para
  poder probarla.
- Pruebas QtTest con un servidor HTTP falso:
  - modelo de preguntas;
  - almacén local y borradores;
  - contrato de errores y respaldo sin conexión;
  - cola de sincronización (servidor que se apaga y vuelve; rechazo sin reintento;
    persistencia en disco).
- GitHub Actions:
  - backend: `mvn verify` y arranque real contra MySQL 8;
  - cliente: Qt 6.8.3 + QtKeychain compilado + `ctest`.

---

## 6. Hoja de ruta recomendada (offline-first)

Prioridad P0: hacer antes de desplegar en más colegios. P1: siguiente trimestre.
P2: después.

| # | Mejora | Por qué | Prioridad | Esfuerzo (est.) |
|---|---|---|---|---|
| 1 | **Copias de seguridad automáticas** del servidor (volcado diario de la BD + `STORAGE_DIR` a disco externo, con restauración documentada) | En un servidor de colegio, sin nube, perder el PC es perder las notas | P0 | 3 días |
| 2 | **Validar el tiempo en el servidor** con tolerancia sin conexión: registrar el inicio en el servidor cuando hay red y aceptar entregas encoladas cuya hora de inicio local sea coherente | Hoy el límite de tiempo solo se controla en el cliente | P0 | 1 semana |
| 3 | **Instaladores** (MSI/NSIS para Windows, AppImage para Linux) y servidor como servicio de Windows/Linux | Hoy la instalación es manual | P0 | 1–2 semanas |
| 4 | **Sesión para uso prolongado sin conexión**: renovación del token al reconectar y política de caducidad configurable | Un estudiante sin red varios días pierde la sesión al reabrir | P1 | 1 semana |
| 5 | **Cola sin conexión también para profesores** (asistencia y calificaciones) | Hoy solo las entregas de estudiantes se encolan | P1 | 1 semana |
| 6 | **Decisión de canal de administración**: el cliente de escritorio como canal principal y la web solo para tareas de servidor (apagar, auditoría, perfil) | Evita mantener dos interfaces de admin | P1 | decisión + 1 semana |
| 7 | Bloqueo tipo kiosco opcional en exámenes (Windows/Linux), inspirado en SEB [10] | La pantalla completa actual no impide cambiar de ventana | P2 | 2 semanas |
| 8 | Banco de preguntas reutilizable (ya existe `PreguntaPlantillaDTO` sin uso) | Productividad docente | P2 | 1 semana |
| 9 | Sincronización entre sedes o con un portal central cuando haya internet (modelo Kolibri [4]) | Colegios con varias sedes | P2 | 4+ semanas |
| — | S3/MinIO, IA generativa | No aportan en el contexto sin internet | Pospuesto | — |

---

## 7. Madurez por dimensión (escala 1–5) [Valoración]

| Dimensión | Puntuación | Justificación |
|---|---|---|
| Funcionalidad | 3,5 | Los 3 roles completos; periodos, asistencia y evaluación cronometrada. Falta el banco de preguntas. |
| Arquitectura | 3,4 | MVVM en el cliente con una sola capa de red; backend modular; migraciones |
| Código | 3,3 | Errores tipados (`ErrorCode`); quedan paquetes `service/impl` y `service/logica` por unificar |
| UX/UI | 3,3 | Sistema de diseño propio; pantallas nuevas consistentes. Faltan una revisión de accesibilidad y unificar los estilos de la web de admin. |
| Seguridad | 3,6 | JWT invalidado al cambiar la contraseña, límite de intentos de login, llavero del sistema. Falta la validación del tiempo en el servidor. |
| Rendimiento | 3,2 | gzip, `ETag`, consultas agrupadas en las notas; caché local en el cliente |
| Pruebas | 3,3 | 90 pruebas en el backend y 22 en el cliente, con CI. Sin pruebas de interfaz QML. |
| Documentación | 3,8 | README, catálogo de errores, CHANGELOG, migraciones y este informe |
| DevOps | 2,4 | CI añadida; faltan instaladores, copias de seguridad y versionado de releases |
| Escalabilidad | 2,6 | Suficiente para un colegio en LAN; varias sedes requeriría sincronización |

---

## 8. Riesgos

- **Migraciones en MySQL 8 de producción.** Se verificaron en MariaDB 10.11 y se
  repiten en la CI contra MySQL 8. Aun así: **haga una copia de la base antes de
  actualizar**.
- **Reloj del equipo del estudiante.** Mientras no exista el punto 2 de §6, un reloj
  manipulado puede alargar el tiempo de un examen sin conexión.
- **Mezcla de años lectivos.** La vista anual usa los periodos del año en curso. Las
  actividades fuera de esos periodos no entran en la nota anual (sí en la vista sin
  periodos). Así se evita mezclar años lectivos.

---

## Fuentes

1. Función Pública, *Decreto 1290 de 2009*: https://www1.funcionpublica.gov.co/eva/gestornormativo/norma.php?i=35954
2. Icfes, normograma, *Decreto 1290 de 2009* (PDF): https://normograma.icfes.gov.co/docs/pdf/decreto_1290_2009.pdf
3. Moodle Docs, *Moodle app offline features*: https://docs.moodle.org/en/Moodle_app_offline_features · *Quiz offline attempts*: https://docs.moodle.org/en/Quiz_offline_attempts
4. Learning Equality, *Kolibri*: https://learningequality.org/kolibri/
5. EdSurge, *How offline-first edtech addresses education disparities worldwide* (2024): https://www.edsurge.com/news/2024-01-12-how-offline-first-edtech-addresses-education-disparities-worldwide
6. Capterra, *Q10*: https://www.capterra.es/software/1050359/q10 · bit4learn, *Plataforma Q10*: https://bit4learn.com/plataforma-q10/ (fuentes comerciales o de reseñas)
7. Universidad Javeriana, Laboratorio de Economía de la Educación, *Informe 98, Educación rural* (2024): https://javeriana.edu.co/recursosdb/5581483/11594517/INFORME98-Educacio%CC%81n-rural+LEE2024.pdf
8. Portafolio, *El 70 % de colegios rurales no tienen acceso a internet*: https://www.portafolio.co/economia/el-70-de-colegios-rurales-no-tienen-acceso-a-internet-574218
9. MinTIC, *Centros Digitales en escuelas rurales*: https://mintic.gov.co/portal/inicio/Sala-de-prensa/Noticias/438761:Mas-de-6-500-Centros-Digitales-duplican-su-velocidad-de-navegacion-en-internet-para-las-escuelas-publicas-de-zonas-rurales-apartadas
10. Safe Exam Browser, *About / User manual*: https://safeexambrowser.org/about_overview_en.html · https://safeexambrowser.org/windows/win_usermanual_en.html
