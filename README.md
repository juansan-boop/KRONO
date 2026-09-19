# KRONO

App móvil Android para gestión de tiempo dirigida a estudiantes universitarios que también trabajan. En vez de pedir estimaciones manuales (como Google Calendar, TickTick o Notion), KRONO usa un **cronómetro** para medir la duración real de cada tarea, aprende de ese historial y sugiere priorización y estimaciones vía IA.

Ver `CLAUDE.md` para el contexto completo del proyecto (equipo, sistema de diseño, arquitectura y reglas).

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

## Requisitos

- Android Studio (última versión estable)
- JDK 17
- SDK Android 35 (compileSdk), mínimo API 26

## Primer arranque

Este scaffold **no incluye el Gradle Wrapper** (`gradlew`, `gradlew.bat`, `gradle-wrapper.jar`) porque no se puede descargar el binario desde este entorno. Al abrir el proyecto en Android Studio por primera vez:

1. Abre la carpeta raíz con Android Studio — te ofrecerá generar el wrapper automáticamente, o
2. En terminal, con Gradle instalado localmente: `gradle wrapper --gradle-version 8.11`

Después de eso, `./gradlew build` debería funcionar.

## Reglas de arquitectura (resumen — detalle completo en CLAUDE.md)

- MVVM + Clean Architecture, módulos con fronteras estrictas.
- Ningún `feature-*` importa a otro `feature-*` directamente.
- Los ViewModel nunca importan `androidx.compose.*`.
- Cero colores/espaciados hardcodeados — todo sale de `core-ui`.
