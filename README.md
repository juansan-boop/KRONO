# KRONO

App móvil Android para gestión de tiempo dirigida a estudiantes universitarios que también trabajan. En vez de pedir estimaciones manuales (como Google Calendar, TickTick o Notion), KRONO usa un **cronómetro** para medir la duración real de cada tarea, aprende de ese historial y sugiere priorización y estimaciones vía IA.

Ver `CLAUDE.md` para el contexto completo del proyecto (equipo, sistema de diseño, arquitectura y reglas), y el documento de arquitectura enlazado abajo para una explicación más extendida.

## 📄 Recursos del equipo

- **[Arquitectura de KRONO](https://claude.ai/artifact/L99qFGLCZM7ULg2smLXi5M)** — recap completo de la estructura de módulos, qué va en cada carpeta y las reglas de arquitectura. Punto de partida recomendado antes de tocar código.
- `CLAUDE.md` — contexto que usa Claude Code al trabajar en el repo (arquitectura, sistema de diseño, convenciones).
- `CONTRIBUTING.md` — estrategia de ramas y Conventional Commits.
- `design/` — export de Google Stitch (referencia visual por pantalla).

## Estructura del proyecto

```
app/                    → punto de entrada, navegación entre features
core/
  core-common/          → utilidades compartidas, sin dependencias de Android
  core-domain/          → modelos y casos de uso, Kotlin puro
  core-data/            → Room, repositorios, fuentes de datos
  core-ui/              → tema Lumina Glass System (Color, Type, Shape, Theme, Glass)
feature-auth/           → login, registro, recuperar contraseña
feature-onboarding/     → flujo inicial
feature-dashboard/      → inicio, notificaciones
feature-tasks/          → CRUD de tareas, cronómetro, IA de priorización, modo enfoque
feature-calendar/       → vistas día/semana/mes, detección de conflictos
feature-profile/        → perfil, ajustes
design/                 → export de Google Stitch (referencia visual por pantalla)
```

## Sistema de diseño — Lumina Glass System

- Estética dark glassmorphism, siempre oscuro, sin tema claro.
- Paleta: fondo Deep Midnight (`#121221`), magenta primario (`#BD00FF` / `#ECB2FF`), cyan secundario (`#49D9E5`).
- Tipografía: Hanken Grotesk + JetBrains Mono, sobre grilla de 8dp.
- Vive completo en `core-ui`. **Cero colores o espaciados hardcodeados fuera de ahí.**
- Toda la UI se construye con Jetpack Compose — no se usa XML de layout (`res/layout/`) para diseño de pantallas. El único XML del proyecto es de configuración (`AndroidManifest.xml`, `res/values/themes.xml`, `res/mipmap-*`).

## Requisitos

- Android Studio (última versión estable)
- JDK 17 (obligatorio: con el JDK 25 que trae Android Studio el build falla por un JVM target inconsistente en `core-common:compileKotlin`; el proyecto usa Kotlin 2.1.0)
- SDK Android 35 (compileSdk), mínimo API 26

### Configurar JDK 17

- **Android Studio:** *Settings → Build, Execution, Deployment → Build Tools → Gradle → Gradle JDK* = 17.
- **Línea de comandos:** apunta `JAVA_HOME` a un JDK 17 antes de ejecutar `./gradlew`.

## Primer arranque — pasos al clonar

1. **Clona el repo** y ábrelo con Android Studio.
2. **Gradle Wrapper**: ya está versionado en el repo (Gradle 9.6.0), no hay que generarlo. Asegúrate de usar JDK 17 (ver arriba) y ejecuta `./gradlew assembleDebug`.
3. **Genera los íconos de la app** si tu copia local no los tiene: clic derecho en `app/res` → *New → Image Asset* → "Launcher Icons (Adaptive and Legacy)".
4. **Revisa `CLAUDE.md`** antes de implementar cualquier pantalla — ahí están las reglas de arquitectura y el sistema de diseño que Claude Code (y cualquiera del equipo) debe respetar.
5. Usa `design/` (export de Stitch: `code.html` + `screen.png` por pantalla) como referencia visual al implementar cada feature.

## Reglas de arquitectura (resumen — detalle completo en CLAUDE.md)

- MVVM + Clean Architecture, módulos con fronteras estrictas.
- Ningún `feature-*` importa a otro `feature-*` directamente.
- Los ViewModel nunca importan `androidx.compose.*`.
- Cero colores/espaciados hardcodeados — todo sale de `core-ui`.

## Verificación

Antes de abrir un PR ejecuta los mismos comandos que el CI:

```
./gradlew lint testDebugUnitTest assembleDebug
```
