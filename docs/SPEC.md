# KRONO — Especificación del producto

> Fuente de verdad funcional para el equipo y los agentes de desarrollo.
> Cada funcionalidad tiene un ID (`F-XX`) y criterios de aceptación (`CA-X`)
> verificables. El desarrollador implementa contra estos criterios y QA aprueba
> o rechaza contra ellos.
>
> Las reglas de arquitectura y del sistema de diseño viven en `CLAUDE.md` y
> prevalecen sobre cualquier cosa de este documento. Las referencias visuales
> viven en `/design/` (una carpeta por pantalla con `code.html` + `screen.png`).
>
> Lo marcado como **(propuesta)** es una sugerencia inicial que el equipo debe
> confirmar en la sección 9 antes de implementarlo.

**Versión:** 0.7 · **Última actualización:** 2026-10-13

---

## 1. Visión

KRONO es una app Android de gestión de tiempo para **estudiantes universitarios
que también trabajan**. En lugar de pedir estimaciones manuales, mide con un
**cronómetro** la duración real de cada tarea, aprende de ese historial y genera
estimaciones personalizadas y sugerencias de priorización.

**Métricas de éxito:**
- Reducir la brecha entre tiempo estimado y tiempo real de las tareas.
- Reducir la tasa de entregas extemporáneas.

## 2. Usuarios

| Usuario | Qué necesita lograr |
|---|---|
| Estudiante que trabaja | Saber cuánto le toman realmente sus tareas, cumplir entregas y equilibrar estudio, trabajo y vida personal |

Categorías de tareas: **Estudio**, **Trabajo**, **Personal**.

## 3. Alcance

**Incluido (por fases, ver sección 8):**
- Gestión de tareas con subtareas, prioridad, categoría y fecha límite
- Cronómetro que registra la duración real y modo enfoque por bloques
- Estimación basada en historial y asistente de prioridades
- Dashboard diario, notificaciones y recordatorios
- Calendario día/semana/mes con detección de conflictos
- Perfil, ajustes y zona de seguridad
- Autenticación y onboarding

**Fuera de alcance por ahora:**
- Tema claro o colores dinámicos (Material You)
- Versión web, iOS o tablet optimizada
- Funciones sociales o colaborativas entre usuarios

## 4. Requisitos no funcionales

| Área | Requisito |
|---|---|
| Plataforma | Android, minSdk 26, compileSdk/targetSdk 35, JDK 17 |
| UI | 100% Jetpack Compose. Sin layouts XML de pantalla |
| Sistema de diseño | Lumina Glass System: siempre oscuro, tokens solo desde `core-ui` (`KronoTheme`, `KronoSpacing`, `KronoShapes`, `Modifier.glassSurface()`) |
| Idioma | Todo el copy de la UI en español, en `strings.xml` del módulo |
| Accesibilidad | Áreas táctiles mínimas de 48dp y `contentDescription` en íconos interactivos |
| Datos | Cloud Firestore es la fuente de datos y usa su caché offline para funcionar sin conexión; la autenticación usa Firebase Authentication (D-1) |
| Fechas | `kotlinx.datetime` en dominio; `kotlin.time.Duration` para duraciones |
| Errores | Las capas de dominio y datos devuelven `KronoResult` (de `core-common`), sin excepciones cruzando módulos |
| Calidad | Cada PR pasa `./gradlew lint testDebugUnitTest assembleDebug` (igual que el CI) |

## 5. Arquitectura

Resumen; el detalle obligatorio está en `CLAUDE.md`.

| Módulo | Responsabilidad |
|---|---|
| `app` | `MainActivity`, `NavHost` y barra inferior. Único módulo que depende de todos los `feature-*` |
| `core-common` | Utilidades sin Android (`KronoResult`) |
| `core-domain` | Modelos y casos de uso en Kotlin puro |
| `core-data` | Firebase (Authentication y Cloud Firestore), repositorios |
| `core-ui` | Tema Lumina Glass y componentes compartidos |
| `feature-auth` | Login, registro, recuperar contraseña |
| `feature-onboarding` | Flujo inicial y bienvenida |
| `feature-dashboard` | Inicio y centro de notificaciones |
| `feature-tasks` | Tareas, cronómetro, modo enfoque, estimación e IA de prioridades |
| `feature-calendar` | Vistas día/semana/mes y conflictos |
| `feature-profile` | Perfil, ajustes y zona de seguridad |

Reglas duras: ningún `feature-*` importa otro `feature-*`; los ViewModels no
importan `androidx.compose.*`; MVVM con estado de UI expuesto por `StateFlow`;
Hilt para inyección de dependencias.

## 6. Modelo de datos

Modelo aprobado en D-3. `Task` ya existe en `core-domain`.

Almacenamiento en Cloud Firestore bajo `users/{uid}`: las subtareas van embebidas en el documento de su tarea; las sesiones de tiempo y las demás entidades van en subcolecciones de `users/{uid}`. El diseño detallado de colecciones y documentos se define en F-02.

| Entidad | Campos | Notas |
|---|---|---|
| `Task` (existente) | `id`, `title`, `dueDate: LocalDateTime?`, `estimatedDuration: Duration?`, `actualDuration: Duration`, `isCompleted` | Extender con `category`, `priority`, `note: String?` |
| `TaskCategory` | `ESTUDIO`, `TRABAJO`, `PERSONAL` | Enum |
| `TaskPriority` | `BAJA`, `MEDIA`, `ALTA` | Enum |
| `Subtask` | `id`, `taskId`, `title`, `isDone`, `position` | Embebida en el documento de su tarea; se elimina junto con ella |
| `TimeSession` | `id`, `taskId`, `startedAt`, `endedAt`, `duration`, `type` (`CRONOMETRO` / `ENFOQUE`) | Fuente de verdad de la duración real; `Task.actualDuration` es la suma de sus sesiones |
| `FocusTemplate` | `id`, `name`, `focusMinutes`, `breakMinutes`, `isActive` | Plantillas de bloques de enfoque |
| `CalendarEvent` | `id`, `title`, `start`, `end`, `location?`, `priority`, `source` (`LOCAL` / externo) | Base para conflictos |
| `AppNotification` | `id`, `type`, `title`, `body`, `createdAt`, `isRead` | Centro de avisos |
| `UserProfile` | `name`, `lastName`, `email`, `role`, `birthDate?`, `phone?` | |
| `NotificationPrefs` | `deadlineReminderMinutes` (30 por defecto), `focusStartSound` | |

## 7. Pantallas y referencias de diseño

Barra inferior: **Inicio · Tareas · Calendario · Ajustes**.

| Pantalla | Módulo | Referencia en `/design/` | Funcionalidad |
|---|---|---|---|
| Iniciar sesión | auth | `iniciar_sesi_n_lumina_style_1`, `iniciar_sesi_n_error_de_validaci_n` | F-22 |
| Crear cuenta | auth | `crear_cuenta_lumina_style_1`, `crear_cuenta_error_contrase_as_no_coinciden`, `crear_cuenta_xito_lumina_style_1` | F-23 |
| Recuperar contraseña | auth | `recuperar_contrase_a_estilo_lumina_glass_1`, `recuperar_contrase_a_error_correo_no_encontrado`, `recuperar_contrase_a_xito_lumina_style_1` | F-24 |
| Cerrar sesión | auth | `cerrar_sesi_n_alerta_segura_krono` | F-25 |
| Completar perfil (onboarding) | onboarding | `configuraci_n_de_perfil_alta_resoluci_n_1`, `configuraci_n_de_perfil_error_campos_vac_os`, `configuraci_n_de_perfil_xito_lumina_style_1` | F-26 |
| Inicio | dashboard | `dashboard_inicio_accesible_1` | F-13 |
| Centro de avisos | dashboard | `notificaciones_centro_de_avisos_krono` | F-14 |
| Lista de tareas | tasks | `lista_de_tareas_estimaci_n_inteligente_krono`, `lista_de_tareas_error_de_red_network_error_1`, `lista_de_tareas_error_de_validaci_n_y_acci_n_1` | F-03 |
| Nueva tarea | tasks | `a_adir_tarea_1`, `krono_error_de_validaci_n_de_formulario_1` | F-04 |
| Editar tarea | tasks | `editar_tarea_krono` | F-05 |
| Detalle y subtareas | tasks | `detalle_de_tarea_y_subtareas` | F-06 |
| Eliminar tarea | tasks | `confirmar_eliminaci_n_de_tarea_alerta_krono`, `aviso_de_eliminaci_n_cr_tica_delete_393x852_1` | F-07 |
| Modo enfoque | tasks | `modo_enfoque_krono_1` | F-09 |
| Bloques de enfoque | tasks | `gesti_n_de_bloques_de_enfoque_krono_crud_1` | F-10 |
| Asistente de prioridades | tasks | `asistente_de_prioridades_ia_1` | F-12 |
| Calendario día/semana/mes | calendar | `calendardayscreen_vista_diaria_krono`, `calendarweekscreen_vista_semanal_krono`, `calendarweekscreen_vista_semanal_en_cuadr_cula`, `calendarmonthscreen_vista_mensual_y_agenda` | F-16 |
| Conflictos | calendar | `dayschedulescreen_jornada_diaria_y_detecci_n_de_conflictos`, `calendario_conflicto_cr_tico_de_horarios` | F-17 |
| Error de sincronización | calendar | `calendario_error_de_sincronizaci_n_krono` | F-18 |
| Perfil | profile | `perfil_de_usuario_krono_1`, `editar_informaci_n_perfil_krono_1` | F-19 |
| Ajustes | profile | `ajustes_hub_principal_1` | F-20 |
| Notificaciones y alarmas | profile | `notificaciones_y_alarmas_perfil_krono_1` | F-15 |
| Zona de seguridad | profile | `zona_de_seguridad_paso_1_393x852_1` | F-21 |

Si una referencia de Stitch contradice `CLAUDE.md` (por ejemplo un color fuera
de la paleta), gana `CLAUDE.md`. Los textos de ejemplo de los diseños
("Alexa", "Informe Trimestral") son datos de muestra, no copy fijo.

## 8. Funcionalidades

Estados: `Pendiente` · `En progreso` · `En revisión` · `Hecho` · `Bloqueada`

### Fase 0 — Infraestructura (prerrequisito de todo lo demás)

#### F-00 — Gradle Wrapper y dependencias de prueba
- **Estado:** Hecho · **Prioridad:** Alta · **Módulos:** raíz, todos · **Depende de:** ninguna · **PR:** #5

Sin el wrapper el CI falla en todos los PRs (`chmod +x gradlew`), y ningún
módulo de librería declara dependencias de prueba.

- **CA-1:** Dado el repo clonado, cuando se ejecuta `./gradlew assembleDebug`, entonces compila sin pasos manuales (wrapper Gradle 9.6.0 versionado, incluido `gradle-wrapper.jar`).
- **CA-2:** Dado un PR abierto, cuando corre el workflow de CI, entonces los pasos lint, pruebas unitarias y assemble terminan en verde.
- **CA-3:** Dado cualquier módulo `core-*` o `feature-*`, cuando se agrega una prueba unitaria, entonces `./gradlew testDebugUnitTest` la ejecuta (dependencias declaradas desde `libs.versions.toml`, incluida `kotlinx-coroutines-test`).

#### F-01 — Navegación base
- **Estado:** Hecho · **Prioridad:** Alta · **Módulos:** `app` · **Depende de:** F-00 · **PR:** #16

- **CA-1:** Dado que la app abre, cuando carga `MainActivity`, entonces muestra `KronoTheme` con un `NavHost` y la barra inferior Inicio, Tareas, Calendario y Ajustes.
- **CA-2:** Dado cualquier destino de la barra, cuando el usuario lo toca, entonces navega a la pantalla correspondiente conservando el estado de la pestaña anterior.
- **CA-3:** Dado el código de navegación, cuando se revisan las dependencias, entonces solo `app` conoce las rutas de varios `feature-*`; cada feature expone su grafo sin importar otros features.

Las etiquetas de la barra inferior limitan su escala de fuente a 1,3x (D-11); el resto de la app escala al 200 % (PR #18).

#### F-27 — Fuentes del sistema de diseño
- **Estado:** Hecho · **Prioridad:** Media · **Módulos:** `core-ui` · **Depende de:** F-00 · **PR:** #17

- **CA-1:** Dado `Type.kt`, cuando se renderiza texto, entonces usa Hanken Grotesk para texto y JetBrains Mono para datos y números (recursos en `res/font/`), sin `FontFamily.Default` como placeholder.

### Fase 1 — Núcleo: tareas y tiempo real

#### F-02 — Persistencia de tareas
- **Estado:** Pendiente · **Prioridad:** Alta · **Responsable:** Janer García (Janer-Garcia) · **Módulos:** `core-domain`, `core-data` · **Depende de:** F-00, F-22 a F-25 (Auth hecha) · **PR:** —

La persistencia usa Cloud Firestore (D-1) con el modelo de la sección 6 (D-3): subtareas embebidas en la tarea y demás entidades en subcolecciones de `users/{uid}`. El diseño fino de colecciones y documentos se define en esta funcionalidad.

- **CA-1:** Dado el modelo de la sección 6, cuando se crea, edita o elimina una tarea con subtareas, entonces el cambio persiste en Cloud Firestore y sobrevive a reiniciar la app, incluso sin conexión gracias a su caché offline.
- **CA-2:** Dado el repositorio de tareas, cuando la UI observa la lista, entonces recibe actualizaciones como `Flow` sin consultas manuales.
- **CA-3:** Dado un error de Firestore, cuando un caso de uso falla, entonces devuelve `KronoResult.Error` en lugar de lanzar la excepción.

#### F-03 — Lista de tareas
- **Estado:** Pendiente · **Prioridad:** Alta · **Responsable:** Janer García (Janer-Garcia) · **Módulos:** `feature-tasks` · **Depende de:** F-02 · **PR:** —

- **CA-1:** Dado que hay tareas, cuando se abre la lista, entonces se agrupan en "Para Hoy" y "Esta Semana" con su conteo, y cada tarjeta muestra prioridad, título, fecha/hora y estimación si existe.
- **CA-2:** Dados los filtros Todas, Estudio, Trabajo y Personal, cuando se selecciona uno, entonces solo se muestran las tareas de esa categoría.
- **CA-3:** Dada una tarea, cuando se marca como completada, entonces cambia su estado y el porcentaje de "Enfoque Diario" se recalcula.
- **CA-4:** Dado que no hay tareas, cuando se abre la lista, entonces se muestra un estado vacío con acceso a "Añadir Nueva Tarea".

#### F-04 — Crear tarea
- **Estado:** Pendiente · **Prioridad:** Alta · **Responsable:** Janer García (Janer-Garcia) · **Módulos:** `feature-tasks` · **Depende de:** F-02 · **PR:** —

- **CA-1:** Dado el formulario, cuando se completan nombre, categoría, fecha, hora, prioridad y tiempo estimado (25m, 45m, 1h u Otro), entonces "Guardar tarea" la crea y vuelve a la lista.
- **CA-2:** Dado que falta el nombre o la fecha, cuando se intenta guardar, entonces se muestra el error de validación en el campo y no se crea la tarea.
- **CA-3:** Dada una fecha y hora en el pasado, cuando se intenta guardar, entonces se informa el error y no se crea la tarea.
- **CA-4:** Dado que el tiempo estimado es opcional, cuando no se elige, entonces la tarea se guarda con `estimatedDuration = null`.

#### F-05 — Editar tarea
- **Estado:** Pendiente · **Prioridad:** Media · **Responsable:** Janer García (Janer-Garcia) · **Módulos:** `feature-tasks` · **Depende de:** F-04 · **PR:** —

- **CA-1:** Dada una tarea existente, cuando se abre la edición, entonces el formulario viene precargado con sus valores.
- **CA-2:** Dados cambios válidos, cuando se guarda, entonces se actualizan sin alterar `actualDuration` ni las sesiones registradas.
- **CA-3:** Las mismas validaciones de F-04 (CA-2 y CA-3) aplican al editar.

#### F-06 — Detalle de tarea y subtareas
- **Estado:** Pendiente · **Prioridad:** Media · **Responsable:** Janer García (Janer-Garcia) · **Módulos:** `feature-tasks` · **Depende de:** F-02 · **PR:** —

- **CA-1:** Dada una tarea, cuando se abre su detalle, entonces muestra categoría, prioridad, límite de entrega con tiempo restante ("En 3 horas"), estimación, subtareas con progreso (x/n) y nota rápida.
- **CA-2:** Dado el detalle, cuando se agrega, marca o desmarca una subtarea, entonces el progreso se actualiza y persiste.
- **CA-3:** Dado el detalle, cuando se toca "Iniciar Enfoque", entonces abre el modo enfoque (F-09) para esa tarea.

#### F-07 — Eliminar o posponer tarea
- **Estado:** Pendiente · **Prioridad:** Media · **Responsable:** Janer García (Janer-Garcia) · **Módulos:** `feature-tasks` · **Depende de:** F-02 · **PR:** —

- **CA-1:** Dada una tarea, cuando se pide eliminarla, entonces se muestra una confirmación que indica cuántas subtareas se descartarán.
- **CA-2:** Dada la confirmación, cuando se elige "Sí, eliminar tarea", entonces se eliminan la tarea y sus subtareas; con "Conservar tarea" no cambia nada.
- **CA-3:** Dada la confirmación, cuando se elige "posponer para mañana", entonces la fecha límite se mueve un día y la tarea no se elimina.
- **CA-4:** Dada una tarea eliminada, cuando se recalculan estadísticas, entonces sus sesiones de tiempo ya no cuentan para la estimación.

#### F-08 — Cronómetro de duración real
- **Estado:** En progreso · **Prioridad:** Alta (diferenciador) · **Responsable:** Kevin Vuskovit García González · **Módulos:** `feature-tasks`, `core-domain`, `core-data`, `app` · **Depende de:** F-02 · **PR:** —

Implementación: el cronómetro en curso es un único documento `users/{uid}/timer/active`
(`taskId`, `startedAtMillis`, `runningSinceMillis`, `accumulatedMillis`), así solo puede
haber uno (CA-4) y sobrevive a cerrar la app (CA-3). Al finalizar, una escritura atómica
crea la sesión en `users/{uid}/timeSessions/{id}`, suma su duración a
`tasks/{id}.actualDurationMillis` con `FieldValue.increment` y borra el cronómetro. Las
pausas no cuentan como tiempo trabajado. Se abre con el botón "Cronómetro" del detalle de
la tarea.

- **CA-1:** Dada una tarea, cuando se inicia el cronómetro, entonces cuenta el tiempo transcurrido y se puede pausar, reanudar y finalizar.
- **CA-2:** Dado un cronómetro finalizado, cuando se guarda, entonces se registra una `TimeSession` y `actualDuration` de la tarea aumenta en esa duración.
- **CA-3:** Dado un cronómetro en curso, cuando la app pasa a segundo plano o se cierra la pantalla, entonces el tiempo no se pierde al volver (se calcula desde la marca de inicio, no por ticks en memoria).
- **CA-4:** Solo puede haber un cronómetro activo a la vez; iniciar otro pide confirmar que se detiene el actual.

#### F-09 — Modo enfoque
- **Estado:** Pendiente · **Prioridad:** Alta · **Módulos:** `feature-tasks` · **Depende de:** F-08, F-10 · **PR:** —

- **CA-1:** Dada una tarea y una plantilla activa, cuando se inicia el modo enfoque, entonces muestra cuenta regresiva del bloque, nombre de la tarea y subtarea actual ("Paso 3 de 4").
- **CA-2:** Dado el modo enfoque, cuando se usa pausar, "+5m Descanso" o "Finalizar", entonces el temporizador responde y al finalizar se registra una `TimeSession` de tipo `ENFOQUE` con el tiempo realmente trabajado.
- **CA-3:** Dado que el bloque llega a cero, cuando termina, entonces se avisa al usuario y se ofrece el descanso de la plantilla.

#### F-10 — Plantillas de bloques de enfoque
- **Estado:** Pendiente · **Prioridad:** Media · **Módulos:** `feature-tasks` · **Depende de:** F-02 · **PR:** —

- **CA-1:** Dada una instalación nueva, cuando se abren las plantillas, entonces existen tres por defecto: Estudio Profundo (45m/10m, activa), Sprint Rápido (25m/5m) y Lectura Nocturna (60m/15m).
- **CA-2:** Dadas las plantillas, cuando se crea, edita o elimina una, entonces el cambio persiste; siempre hay exactamente una activa.
- **CA-3:** Dada una plantilla, cuando se muestra, entonces indica el porcentaje de foco y descanso del ciclo.

#### F-11 — Estimación basada en historial
- **Estado:** En progreso · **Prioridad:** Alta · **Responsable:** Kevin Vuskovit García González · **Módulos:** `core-domain`, `feature-tasks` · **Depende de:** F-08 · **PR:** —

Algoritmo **(propuesta, confirmar en D-4)**: promedio de `actualDuration` de las
últimas 5 tareas completadas de la misma categoría, redondeado a 5 minutos.

Implementación (`EstimateTaskDurationUseCase`): solo cuentan las tareas completadas con
tiempo real medido (`actualDuration > 0`), así que la sugerencia aparece cuando exista el
cronómetro (F-08). `Task` no guarda la fecha en que se completó, por lo que "últimas" se
aproxima con la fecha de entrega más reciente.

- **CA-1:** Dadas al menos 3 tareas completadas de una categoría, cuando se crea una tarea de esa categoría, entonces se muestra "Sugerencia IA: ~X min · Basado en historial" y se puede aplicar con un toque.
- **CA-2:** Dado que hay menos de 3 tareas completadas en la categoría, cuando se crea una tarea, entonces no se muestra sugerencia.
- **CA-3:** El cálculo vive en un caso de uso de `core-domain` con pruebas unitarias que cubren historial vacío, insuficiente y suficiente.

### Fase 2 — Organización diaria

#### F-13 — Dashboard de inicio
- **Estado:** Pendiente · **Prioridad:** Alta · **Módulos:** `feature-dashboard` · **Depende de:** F-02, F-08 · **PR:** —

- **CA-1:** Dado el inicio, cuando carga, entonces muestra saludo con el nombre del usuario, porcentaje del día completado, conteo de completadas y pendientes y tiempo de enfoque del día.
- **CA-2:** Dado el bloque "Sesión de Enfoque", cuando se toca "Iniciar", entonces arranca el modo enfoque con la plantilla activa.
- **CA-3:** Dado "Tareas prioritarias", cuando carga, entonces lista las pendientes de hoy ordenadas por prioridad y hora, con acceso a "Ver todas".
- **CA-4:** El dashboard obtiene los datos vía `core-domain`, sin importar `feature-tasks`.

#### F-14 — Centro de avisos
- **Estado:** Pendiente · **Prioridad:** Media · **Módulos:** `feature-dashboard` · **Depende de:** F-15 · **PR:** —

- **CA-1:** Dadas notificaciones generadas, cuando se abre el centro, entonces se agrupan en "Hoy" y "Anteriores" con filtros Todas, Críticas, IA & Ritmo y Tareas.
- **CA-2:** Dado "Leer todo", cuando se toca, entonces todas quedan marcadas como leídas.

#### F-15 — Recordatorios y preferencias de notificación
- **Estado:** Pendiente · **Prioridad:** Media · **Módulos:** `feature-profile`, `core-data` · **Depende de:** F-02 · **PR:** —

- **CA-1:** Dada una tarea con fecha límite, cuando faltan 30 minutos (valor configurable), entonces se emite una notificación del sistema con el nombre de la tarea y las subtareas pendientes.
- **CA-2:** Dada la pantalla "Notificaciones y Alarmas", cuando se cambian las preferencias y se guarda, entonces persisten; "Restablecer valores predeterminados" vuelve a 30 minutos.
- **CA-3:** En Android 13 o superior, la app solicita el permiso de notificaciones y funciona sin fallar si el usuario lo niega.

#### F-16 — Calendario día, semana y mes
- **Estado:** Pendiente · **Prioridad:** Media · **Módulos:** `feature-calendar` · **Depende de:** F-02 · **PR:** —

- **CA-1:** Dado el calendario, cuando se alterna entre Mes, Semana y Día, entonces cada vista muestra tareas y eventos del período con navegación anterior/siguiente.
- **CA-2:** Dada la vista mensual, cuando se toca un día, entonces la agenda inferior lista sus actividades con horario.
- **CA-3:** Dada la vista diaria, cuando hay una actividad en curso, entonces se marca como "En curso".

#### F-17 — Detección de conflictos de horario
- **Estado:** Pendiente · **Prioridad:** Media · **Módulos:** `core-domain`, `feature-calendar` · **Depende de:** F-16 · **PR:** —

- **CA-1:** Dados dos eventos donde `inicioA < finB` y `inicioB < finA`, cuando se cargan, entonces se marcan como conflicto con los minutos de superposición.
- **CA-2:** Dado un conflicto, cuando se muestra, entonces se propone reubicar el evento de menor prioridad en la primera ventana libre del mismo día con su misma duración **(propuesta)**, con opciones "Aceptar reajuste" y "Mantener".
- **CA-3:** La detección es un caso de uso puro en `core-domain` con pruebas de solapamiento parcial, total, contiguo (sin conflicto) y sin ventana libre.

#### F-19 — Perfil de usuario
- **Estado:** Pendiente · **Prioridad:** Media · **Módulos:** `feature-profile` · **Depende de:** F-02 · **PR:** —

- **CA-1:** Dado el perfil, cuando se abre, entonces muestra nombre, correo y rol, con accesos a editar información, notificaciones y cerrar sesión.
- **CA-2:** Dada la edición, cuando se guardan cambios válidos, entonces persisten; con campos obligatorios vacíos se muestra el error correspondiente.

#### F-20 — Ajustes
- **Estado:** Pendiente · **Prioridad:** Baja · **Módulos:** `feature-profile` · **Depende de:** F-10, F-15 · **PR:** —

- **CA-1:** Dado el hub de ajustes, cuando se abre, entonces da acceso a plantillas de concentración, alertas prioritarias, zona de seguridad y cerrar sesión.
- **CA-2:** Cada interruptor de ajustes persiste su valor entre reinicios.

#### F-21 — Zona de seguridad
- **Estado:** Pendiente · **Prioridad:** Baja · **Módulos:** `feature-profile` · **Depende de:** F-02 · **PR:** —

- **CA-1:** Dada la zona de seguridad, cuando se elige "Restablecer configuración y datos" y se confirma en un segundo paso, entonces se borran todos los datos del usuario en Cloud Firestore (tareas, sesiones, plantillas, avisos y demás), se restauran los valores por defecto y se conserva la cuenta.
- **CA-2:** Dado "Eliminar cuenta y perfil completo", cuando se confirma en un segundo paso, entonces se borran los datos del usuario en Cloud Firestore, se pide reautenticación (Firebase la exige si el inicio de sesión no es reciente) y se elimina la cuenta de Firebase Authentication. Se implementa junto con `feature-profile`.

### Fase 3 — Cuenta, sincronización e IA avanzada

#### F-22 a F-25 — Iniciar sesión, crear cuenta, recuperar contraseña, cerrar sesión
- **Estado:** Hecho · **Prioridad:** Alta · **Módulos:** `feature-auth`, `core-domain`, `core-data`, `app` · **Depende de:** F-00 · **PR:** #13

La autenticación usa Firebase Authentication con correo y contraseña (D-1); `AuthRepository` en `core-domain` aísla la implementación. El PR #7 (hito 1) entregó una versión local con Room, que el PR #13 reemplazó por Firebase junto con la eliminación de Room y de sus cuentas de prueba.

- **CA-1 (registro):** El formulario tiene los campos correo, contraseña y confirmación, sin campo de nombre (el nombre se pide en el onboarding, F-26). La contraseña exige mínimo 8 caracteres, al menos un número y un carácter especial, con indicadores que se marcan en vivo; si la confirmación no coincide se muestra "Las contraseñas no coinciden". La contraseña la gestiona Firebase (la app no la almacena ni le aplica hash propio).
- **CA-2 (login):** Con campos vacíos se muestra "Por favor, completa todos los campos obligatorios"; durante el envío el botón muestra progreso y no permite doble envío.
- **CA-3 (recuperar):** El envío es real por correo mediante Firebase (`sendPasswordResetEmail`) y se muestra la pantalla de éxito. La pantalla de éxito conserva el título "¡Enlace Enviado!" del diseño y muestra el texto neutro "Si el correo está registrado, te enviamos un enlace", para no exponer qué correos tienen cuenta (D-10; sigue activa la protección contra enumeración de correos de Firebase). La pantalla de error se usa para un correo con formato inválido o sin conexión, no para "correo no registrado".
- **CA-4 (cerrar sesión):** El caso de uso `LogoutUseCase` cierra la sesión de Firebase. Hasta que `feature-profile` (F-19/F-20) lo reemplace, la pestaña Ajustes provisional de `app` tiene un botón temporal "Cerrar sesión" que pide confirmación con un diálogo ("¿Cerrar sesión?", con las opciones Cancelar y Cerrar sesión); al confirmar invoca `LogoutUseCase` y vuelve al login limpiando el back stack, y si falla muestra un mensaje de error. El diálogo definitivo ("Permanecer en KRONO" y "Cerrar Sesión") vivirá en `feature-profile`. Decisión del 2026-10-08, implementada en el PR #16.
- **CA-5 (entrada al flujo principal):** Ya no existe la pantalla provisional "Próximo hito". Tras autenticarse (o al abrir la app con sesión activa) se entra al flujo principal con la barra inferior de F-01; "atrás" no vuelve al login. Decisión del 2026-10-08, implementada en el PR #16.
- **CA-6 (sesión persistente):** Dada una sesión activa de Firebase, cuando se reabre la app, entonces se entra directamente sin pasar por el login.
- **CA-7 (errores):** Dadas credenciales inválidas, un correo ya en uso o la falta de conexión, cuando se intenta iniciar sesión o crear la cuenta, entonces se muestra un mensaje en español sin que la app falle. Los errores cruzan los módulos como `KronoResult.Error`.

#### F-26 — Onboarding: completar perfil
- **Estado:** Pendiente · **Prioridad:** Media · **Módulos:** `feature-onboarding` · **Depende de:** F-23 (autenticación, D-1 resuelta) · **PR:** —
- **CA-1:** Tras crear cuenta, se muestra la bienvenida y luego "Completa tu Perfil" (donde se pide el nombre); con campos obligatorios vacíos no avanza.

#### F-12 — Asistente de prioridades
- **Estado:** En progreso · **Prioridad:** Media · **Responsable:** Kevin Vuskovit García González · **Módulos:** `feature-tasks`, `core-domain`, `core-data`, `app` · **PR:** —

Enfoque híbrido (D-5): `BuildPriorityPlanUseCase` (heurística local en `core-domain`) puntúa
las tareas pendientes por urgencia de la entrega, prioridad y margen de tiempo, sugiere la
primera y encadena las demás en bloques desde la hora actual. La confianza mostrada sale de
la diferencia de puntuación con la segunda tarea y de la calidad de los datos (estimación y
fecha). Gemini, vía Firebase AI Logic (`core-data`), solo redacta el motivo en español a
partir de esos datos; sin conexión o si falla, la UI arma el motivo con plantillas. El
balance por categoría reemplaza la "Telemetría Cognitiva" del diseño. Se accede desde el
botón "Asistente IA" de la lista de tareas.

- **CA-1:** Dadas tareas pendientes, cuando se abre el asistente, entonces sugiere la tarea a hacer ahora con su motivo y una secuencia ordenada para el resto del día.
- **CA-2:** Dado que el servicio de IA no responde, cuando se abre el asistente, entonces la recomendación y la secuencia se muestran igual y el motivo usa el texto de plantilla.
- **CA-3:** La heurística vive en casos de uso de `core-domain` con pruebas unitarias; el modelo de lenguaje nunca decide el orden.

#### F-18 — Sincronización con calendarios externos
- **Estado:** Bloqueada (D-6) · **Prioridad:** Baja · **Módulos:** `feature-calendar`, `core-data` · **PR:** —
- **CA-1:** Sin conexión, el calendario muestra "Modo sin conexión activo", conserva los cambios locales y ofrece "Reintentar sincronización".

## 9. Decisiones abiertas

Los agentes no resuelven estas decisiones: si una tarea depende de una, la
reportan al orquestador.

| # | Pregunta | Afecta a | Decisión |
|---|---|---|---|
| D-1 | ¿Qué backend de autenticación y nube se usa (Firebase, propio, ninguno)? | F-21 (CA-2), F-22 a F-26, sincronización | Resuelta (Juan Fernando Sánchez Otero, 2026-10-08): el backend es Firebase. Autenticación con Firebase Authentication (correo y contraseña) y Cloud Firestore como única fuente de datos, con su persistencia offline; Room se elimina. Las cuentas locales de prueba se descartan. `AuthRepository` y los repositorios de `core-domain` aíslan la implementación. Orden de trabajo: primero Auth (F-22 a F-25), luego Firestore (datos de tareas), cada uno en su PR. Queda sin efecto el hash con sal y la recuperación simulada: la contraseña la gestiona Firebase y la recuperación es real por correo. |
| D-2 | ¿Qué proveedores de login social ("o continúa con")? | F-22 | Ninguno: sin login social por ahora. |
| D-3 | ¿Se aprueba el modelo de datos propuesto de la sección 6? | F-02 en adelante | Resuelta (Juan Fernando Sánchez Otero, 2026-10-08): se aprueba el modelo de la sección 6, almacenado en Cloud Firestore bajo `users/{uid}`: subtareas embebidas en el documento de la tarea; sesiones de tiempo y demás entidades en subcolecciones de `users/{uid}`. El diseño fino queda para F-02. |
| D-4 | ¿Se aprueba el algoritmo de estimación propuesto en F-11? | F-11 | Pendiente de aprobación: la propuesta quedó implementada en `EstimateTaskDurationUseCase` (Kevin Vuskovit García González, 2026-10-22) |
| D-5 | ¿El asistente de prioridades es una heurística local o usa un servicio de IA externo? Los diseños mencionan "ritmo circadiano" y "confianza 94%": definir qué datos los respaldan | F-12 | Resuelta (Kevin Vuskovit García González, 2026-10-22): híbrido. La heurística local de `core-domain` decide el orden y la confianza; Gemini vía Firebase AI Logic solo redacta el motivo, con texto de plantilla si no responde. No se muestran datos "circadianos" porque la app no los mide; la confianza se calcula con datos reales de las tareas. Requiere habilitar la Gemini Developer API en la consola de Firebase. |
| D-6 | ¿Entra la sincronización con Google Calendar y Outlook en el alcance del proyecto académico? | F-18 | Pendiente |
| D-7 | `CONTRIBUTING.md` define la rama `develop`, pero solo existe `main` en el remoto. ¿Se crea `develop` como base de los PRs? | Flujo de PRs | Resuelta: `develop` existe y es la base de los PRs. |
| D-8 | ¿Qué integrante es responsable de cada módulo? Los agentes solo deben tocar los módulos asignados a quien los ejecuta | Todas | Actualizada (2026-10-13): Janer García (Janer-Garcia) es responsable de F-02 (Persistencia de tareas en Cloud Firestore — core-domain / core-data) y del módulo `:feature-tasks` completo: F-03 (Lista de tareas), F-04 (Crear tarea), F-05 (Editar tarea), F-06 (Detalle de tarea y subtareas) y F-07 (Eliminar o posponer tarea); Juan Esteban Sora Ariza es responsable exclusivo del módulo `:feature-profile` (F-15, F-19, F-20, F-21, F-25 CA-4); Juan Fernando Sánchez Otero mantiene `app` y `feature-auth`; `feature-onboarding`, `feature-dashboard` y `feature-calendar` quedan sin asignar para hitos posteriores. |
| D-9 | Ajustes muestra "Tema de pantalla: Cyber Dark", pero no hay tema claro. ¿Se omite la opción o queda informativa? | F-20 | Pendiente |
| D-10 | Firebase puede devolver éxito al recuperar la contraseña aunque el correo no exista (protección contra enumeración de correos, activa por defecto en proyectos nuevos). ¿Se desactiva esa protección en la consola de Firebase para conservar el mensaje del diseño "Este correo no está registrado en KRONO", o se cambia el copy a un mensaje neutro ("Si el correo está registrado, te enviamos un enlace")? | F-24 (CA-3) | Resuelta (Juan Fernando Sánchez Otero, 2026-10-08): se conserva el diseño visual de las tres pantallas, con texto neutro para no exponer qué correos tienen cuenta (la protección de Firebase sigue activa). Envío real con `sendPasswordResetEmail`; éxito: "Si el correo está registrado, te enviamos un enlace" (título "¡Enlace Enviado!"); error solo para correo inválido o sin conexión. |
| D-11 | ¿Las etiquetas de la barra inferior escalan con la fuente del sistema hasta el 200 %? | F-01 | Resuelta (Juan Fernando Sánchez Otero, 2026-10-09): las etiquetas limitan su escala de fuente a 1,3x (`ESCALA_MAXIMA_ETIQUETA`), excepción consciente a WCAG 1.4.4 para que la barra no se desborde; el ícono y la descripción de accesibilidad "Ir a <sección>" se mantienen. El resto de la app escala al 200 % (PR #18). |
| D-12 | ¿Se implementa el desenfoque real del fondo (backdrop blur) del efecto glass? `Modifier.blur` y `RenderEffect` no desenfocan lo que hay detrás de la superficie; requeriría una librería como Haze. Hoy `glassSurface()` no desenfoca (documentado en `Glass.kt`, PR #18) | Sistema de diseño (`core-ui`) | Pendiente |
| D-13 | ¿Cómo escalan los íconos pequeños con la fuente del sistema? `iconSmall` es fijo en dp y no crece al 200 % | Sistema de diseño (`core-ui`) | Pendiente |

### Responsables por módulo (D-8)

| Módulo / Capa | Responsable | Funcionalidades / Alcance |
|---|---|---|
| `:feature-profile` | **Juan Esteban Sora Ariza** (responsable exclusivo) | **F-19** (Perfil de Usuario y Edición), **F-20** (Hub de Ajustes, Ritmo Circadiano y Enfoque Adaptativo), **F-21** (Zona de Seguridad y Purga de Caché), **F-15** (Preferencias de Notificaciones) y **F-25 CA-4** (Flujo y Modal Oficial de Cierre de Sesión). |
| `:feature-tasks`, `:core` (persistencia de tareas) | **Janer García (Janer-Garcia)** | **F-02** (Persistencia de tareas en Cloud Firestore — `core-domain` / `core-data`) y módulo `:feature-tasks` completo: **F-03** (Lista de tareas), **F-04** (Crear tarea), **F-05** (Editar tarea), **F-06** (Detalle de tarea y subtareas) y **F-07** (Eliminar o posponer tarea). |
| `app`, `feature-auth` | **Juan Fernando Sánchez Otero** | Mantiene `app` y `feature-auth`. |
| `:feature-tasks` (asistente), `core-domain` / `core-data` (`assistant`) | **Kevin Vuskovit García González** | **F-11** (Estimación basada en historial) y **F-12** (Asistente de prioridades con IA). |
| `feature-onboarding` | Sin asignar | Hitos posteriores |
| `feature-dashboard` | Sin asignar | Hitos posteriores |
| `feature-calendar` | Sin asignar | Hitos posteriores |

## 10. Registro de cambios

| Fecha | Cambio |
|---|---|
| 2026-10-05 | Versión inicial a partir del scaffold, `CLAUDE.md`, `CONTRIBUTING.md` y las referencias de `/design/` |
| 2026-10-05 | v0.2: F-00 marcada como Hecha (PR #3) y wrapper corregido a Gradle 9.6.0 (AGP 9.4.1 exige Gradle 9.x); D-1 (autenticación temporal en memoria), D-2 (login social fuera de alcance) y D-7 (`develop` existe) resueltas |
| 2026-10-05 | v0.3: decisiones de Juan Fernando Sánchez Otero: F-00 con PR #5; D-1 (autenticación y datos locales con Room, hash con sal), D-2 (sin login social) y D-8 (responsable de `app`, `core-*` y `feature-auth`; demás módulos sin asignar); F-22 a F-26 pasan a Pendiente con criterios ajustados (registro sin nombre, recuperación simulada, pantalla provisional, sesión persistente, `LogoutUseCase`); aclaración: el proyecto usa AGP 8.7.0 con Gradle 9.6.0 (compila, con aviso de incompatibilidad futura con Gradle 10; subir AGP queda como tarea aparte), por lo que la nota de v0.2 sobre AGP 9.4.1 no aplica |
| 2026-10-06 | F-22 a F-25: CA-4 aclarado con un botón temporal "Cerrar sesión" (sin confirmación) en la pantalla provisional de `app` mientras no exista `feature-profile`; decisión de Juan Fernando Sánchez Otero |
| 2026-10-06 | v0.4: cierre del hito 1: F-22 a F-25 marcadas como Hechas (PR #7): autenticación local en Room, botón temporal "Cerrar sesión" en la pantalla provisional de `app`, ícono adaptativo y logo de KRONO |
| 2026-10-08 | v0.5: D-1 pasa a Firebase: Authentication (correo y contraseña) y Cloud Firestore como única fuente de datos, Room se elimina; F-02 se reformula sobre Firestore y depende de D-3; se descartan el hash con sal, la recuperación simulada y las cuentas locales de prueba; F-22 a F-25 vuelven a En progreso (migración, PR —) con CA-1, CA-3 y CA-6 ajustados y CA-7 (errores) nuevo; nueva decisión abierta D-10 (mensaje de correo no registrado); F-21 CA-2 sigue Bloqueada con razón actualizada; decisión de Juan Fernando Sánchez Otero |
| 2026-10-08 | v0.6: D-3 resuelta (modelo de datos aprobado en Firestore bajo `users/{uid}`, subtareas embebidas, demás entidades en subcolecciones; F-02 ya no depende de D-3); D-10 resuelta (texto neutro en recuperar contraseña, F-24 CA-3); F-21 CA-1 borra los datos del usuario en Firestore y CA-2 deja de estar Bloqueada (borrado de datos, reautenticación y eliminación de la cuenta); se quita de D-1 la frase obsoleta sobre librerías de red; decisión de Juan Fernando Sánchez Otero |
| 2026-10-09 | v0.7: cierre de la fase 0 (decisiones de Juan Fernando Sánchez Otero). Estados: F-22 a F-25 Hechas (PR #13, Firebase Authentication), F-01 Hecha (PR #16), F-27 Hecha (PR #17). F-22 a F-25: CA-4 (botón temporal "Cerrar sesión" en Ajustes provisional con diálogo de confirmación) y CA-5 (se elimina la pantalla "Próximo hito"; se entra al flujo principal con barra inferior) ajustados a lo implementado en el PR #16 (decisión del 2026-10-08). D-8: `feature-tasks` asignado a Juan Fernando Sánchez Otero; sigue Parcial para onboarding, dashboard, calendar y profile. D-11 resuelta (etiquetas de la barra inferior limitadas a 1,3x, PR #18). Nuevas decisiones abiertas D-12 (backdrop blur real) y D-13 (escala de íconos pequeños). Nueva sección 11 "Deuda técnica conocida". Se corrige la referencia obsoleta a Room en la nota de F-22 a F-25 |
| 2026-10-13 | D-8 actualizada: se asigna formalmente a Janer García (Janer-Garcia) la responsabilidad de F-02 (persistencia de tareas en Cloud Firestore — core-domain / core-data) y del módulo feature-tasks completo (F-03, F-04, F-05, F-06 y F-07) |
| 2026-10-22 | F-11 y F-12 pasan a En progreso con Kevin Vuskovit García González como responsable; D-5 resuelta (asistente híbrido: heurística local + Gemini vía Firebase AI Logic para redactar el motivo); D-4 con la propuesta implementada, pendiente de aprobación; F-12 suma CA-2 (respaldo sin IA) y CA-3 (heurística probada en `core-domain`) |
| 2026-10-23 | F-08 pasa a En progreso con Kevin Vuskovit García González como responsable: cronómetro persistido en `users/{uid}/timer/active` y sesiones en `users/{uid}/timeSessions` |

## 11. Deuda técnica conocida

Pendientes detectados al cerrar la fase 0 que no son funcionalidades (`F-XX`) ni
decisiones abiertas.

**Pruebas y CI**
- Las pruebas instrumentadas (`androidTest`) no corren en el CI; solo se ejecutan en local con emulador.
- `./gradlew connectedDebugAndroidTest` global falla en módulos sin pruebas instrumentadas; se ejecuta por módulo.

**Autenticación (F-22 a F-25)**
- Falta un mensaje propio para `ERROR_USER_DISABLED` (cuenta deshabilitada).
- Firebase puede rechazar una contraseña aunque los indicadores de fortaleza estén en verde.
- La sesión en caché persiste si la cuenta se borra desde la consola de Firebase.
- Firebase BoM queda en 34.12.0, limitado por Kotlin 2.1.0.

**Asistente de prioridades (F-11, F-12)**
- `firebase-ai` se toma del mismo Firebase BoM 34.12.0; si su versión no compila con Kotlin 2.1.0, hay que subir Kotlin o fijar una versión compatible de `firebase-ai`.
- Falta Firebase App Check para que solo la app oficial pueda llamar a Gemini, y mover el nombre del modelo (`gemini-3.5-flash`) a Remote Config.
- El prompt envía a Gemini el título, la categoría, la prioridad y la fecha de las tareas del plan; conviene mencionarlo en la política de privacidad.
- La sugerencia de F-11 usa el tiempo real que mide el cronómetro (F-08).

**Cronómetro (F-08)**
- La zona de seguridad (F-21) debe borrar también `timer` y `timeSessions` al restablecer los datos del usuario.
- Las reglas de seguridad de Firestore deben permitir `users/{uid}/timer` y `users/{uid}/timeSessions` al propio usuario.
- No hay notificación persistente mientras el cronómetro corre; el tiempo no se pierde (se calcula con marcas de tiempo), pero el usuario no lo ve fuera de la app.
