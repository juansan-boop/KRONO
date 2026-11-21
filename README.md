# KRONO

**Gestión del tiempo para estudiantes universitarios que también trabajan.**

KRONO es una aplicación móvil Android diseñada específicamente para optimizar la productividad de estudiantes universitarios que combinan estudios y empleo. A diferencia de las herramientas convencionales que requieren estimaciones manuales basadas en conjeturas (como Google Calendar, TickTick o Notion), KRONO mide la **duración real** de ejecución mediante un cronómetro incorporado, aprende del historial medido y genera estimaciones personalizadas y priorizaciones asistidas por Inteligencia Artificial.

![Plataforma](https://img.shields.io/badge/plataforma-Android%208.0%2B-3DDC84?logo=android&logoColor=white)
![Kotlin](https://img.shields.io/badge/Kotlin-2.1.0-7F52FF?logo=kotlin&logoColor=white)
![Jetpack Compose](https://img.shields.io/badge/UI-Jetpack%20Compose-4285F4)
![Firebase](https://img.shields.io/badge/backend-Firebase-FFCA28?logo=firebase&logoColor=black)
![Room](https://img.shields.io/badge/local%20cache-Room%20DB-3DDC84)
![Rama](https://img.shields.io/badge/rama%20base-develop-BD00FF)

---

## 👥 Integrantes

| Integrante | Responsabilidad principal |
|---|---|
| **Juan Fernando Sánchez Otero** | `app`, arquitectura multi-módulo, `core-*`, autenticación, dashboard, calendario, onboarding, modo enfoque, plantillas y reglas de seguridad de Firestore |
| **Kevin Vuskovit García González** | Cronómetro de duración real, estimación híbrida basada en historial (recencia `completedAt` + `modelo_app.json`), asistente de prioridades con IA |
| **Janer García Avendaño García** | Persistencia de tareas en Firestore y Room, sincronización offline-first y pantallas de tareas (lista, crear, editar, detalle, eliminar/posponer) |
| **Juan Esteban Sora Ariza** | Perfil de usuario, preferencias locales con DataStore (`ProfileLocalStore`), ajustes, notificaciones y zona de seguridad |

---

## 🔗 Repositorio y Rama de Trabajo

- **Repositorio oficial:** https://github.com/juansan-boop/KRONO
- **Rama principal de desarrollo y entrega:** [`develop`](https://github.com/juansan-boop/KRONO/tree/develop)

```bash
git clone -b develop https://github.com/juansan-boop/KRONO.git
```

---

## 🧱 Arquitectura y Modelo de Datos

KRONO implementa **Clean Architecture** estructurada en módulos Gradle desacoplados, siguiendo el patrón **MVVM** reactivo con Jetpack Compose y Kotlin Coroutines/Flow.

### 1. Separación de Dominio y Persistencia
- **Modelo de Dominio (`:core:core-domain`):**
  - Representado por la entidad inmutable `Task.kt` en Kotlin puro, totalmente desacoplada del SDK de Android, Firestore o Room.
  - Modela atributos esenciales: `id`, `title`, `note`, `category`, `priority`, `dueDate`, `estimatedDuration`, `actualDuration`, `isCompleted`, `subtasks`, `isDeleted`, `syncStatus` y `completedAt` (marca de tiempo exacta para trazabilidad y cálculo de recencia).
- **Persistencia Remota (`:core:core-data`):**
  - `TaskDto.kt` mapea los documentos almacenados en **Cloud Firestore**. Soporta escrituras atómicas completas y actualizaciones parciales por campos (`TaskField`) para evitar condiciones de carrera entre la edición manual y el cronómetro activo.
- **Persistencia Local y Caché Offline (`:core:core-data`):**
  - **AndroidX Room:** compuesto por `TaskEntity.kt`, `TaskDao` y `KronoDatabase`. Permite consultas instantáneas sin latencia y soporte completo fuera de línea (offline-first).
- **Preferencias de Perfil y Ajustes Locales (`:core:core-data`):**
  - `ProfileLocalStore` implementado sobre **Jetpack Preferences DataStore**, almacenando preferencias de usuario y ajustes sin dependencias acopladas a la base de datos de tareas.

### 2. Flujo CRUD y Estados de Sincronización
El ciclo de vida de las tareas opera a través de los módulos de la aplicación:
- **`feature-tasks`:** Lista con filtrado por categoría, creación con validación temporal, edición sin pisar el tiempo cronometrado, detalle con gestión de subtareas y eliminación/posposición.
- **`feature-calendar`:** Visualización en agenda y malla mensual, creación contextual desde cualquier fecha seleccionada y reprogramación de entregas directamente desde el calendario.
- **`feature-dashboard`:** Resumen diario, cálculo de carga y accesos rápidos a tareas prioritarias.
- **Estados de Sincronización Remota (`SyncStatus`):**
  - `CONFIRMED`: Registro respaldado y validado en el servidor Cloud Firestore.
  - `PENDING_SYNC`: Registro modificado en local (Room) encolado para sincronización cuando la red esté disponible.
  - `REJECTED`: Registro rechazado por validaciones del servidor o reglas de seguridad.

### 3. Motor IA H1 y Planificación Inteligente
- **Estimación Híbrida de Duración (`EstimateTaskDurationUseCase`):**
  - Infiere duraciones a partir del histórico de tareas completadas del usuario ponderando la **recencia** mediante la marca de tiempo `completedAt`.
  - Integra el modelo serializado `modelo_app.json` (árbol de decisión/regresión entrenado en `KRONO-IA/`), complementado con fallback heurístico por categoría y prioridad cuando el historial es reducido.
- **Agente de Planificación (`PlanningAgentUseCases`):**
  - Procesa requerimientos en lenguaje natural utilizando **Gemini** (a través de Firebase AI Logic) para sugerir bloques y tareas estructuradas, con capacidad de guardado por lotes (*batch insert*) en una sola transacción hacia el repositorio.

---

## 📐 Estructura de Módulos

```
KRONO/
├── app/                    → Punto de entrada, navegación global, shell adaptable (BottomBar / NavigationRail)
├── core/
│   ├── core-common/        → Resultados funcionales (KronoResult), extensiones y utilidades compartidas
│   ├── core-domain/        → Modelos de dominio puros (Task), casos de uso y contratos de repositorio
│   ├── core-data/          → Implementación de repositorios, Room (KronoDatabase), Firestore, DataStore y AI Logic
│   └── core-ui/            → Sistema de diseño Lumina Glass (Theme, Color, Type, Shape, Glass, Dimens)
├── feature-auth/           → Autenticación, registro, recuperación y validación de contraseñas
├── feature-onboarding/     → Bienvenida e introducción al flujo de tiempo real
├── feature-dashboard/      → Panel diario de enfoque, métricas y accesos directos
├── feature-tasks/          → CRUD de tareas, cronómetro activo, modo enfoque y asistente de prioridades
├── feature-calendar/       → Calendario interactivo (día, semana, mes), agenda y reprogramación
├── feature-profile/        → Perfil, zona de seguridad, preferencias locales y notificaciones
├── KRONO-IA/               → Datos, entrenamiento y exportación del modelo ligero modelo_app.json
├── design/                 → Especificaciones visuales exportadas desde Google Stitch
└── docs/                   → Especificación funcional (SPEC.md) y documentación técnica
```

---

## 🛠️ Compilar y Ejecutar

### Requisitos Previos
- **Android Studio** Ladybug (o versión estable más reciente).
- **JDK 17** (obligatorio para compatibilidad de bytecode Kotlin/JVM).
- **Android SDK:** `compileSdk = 35`, `minSdk = 26`.

### Pasos de Configuración y Despliegue

1. **Clonar el repositorio:**
   ```bash
   git clone -b develop https://github.com/juansan-boop/KRONO.git
   cd KRONO
   ```

2. **Configurar el entorno Java:**
   - Asegúrate de que `JAVA_HOME` apunte a una instalación de **JDK 17**.
   - En Android Studio: *Settings → Build, Execution, Deployment → Build Tools → Gradle → Gradle JDK* = 17.

3. **Ubicar el archivo de configuración de Firebase:**
   - Ubica tu archivo `google-services.json` provisto para el entorno dentro del directorio `app/`:
     ```
     KRONO/app/google-services.json
     ```
   *(Nota: Este archivo está protegido en `.gitignore` y no debe subirse al control de versiones).*

4. **Compilar y generar el APK de depuración:**
   ```bash
   # En Windows PowerShell / CMD:
   .\gradlew.bat assembleDebug

   # En Linux / macOS:
   ./gradlew assembleDebug
   ```
   El artefacto se genera en: `app/build/outputs/apk/debug/app-debug.apk`.

5. **Instalar en emulador o dispositivo físico:**
   ```bash
   .\gradlew.bat installDebug
   ```

---

## 🧪 Verificación y CI

Para validar la integridad del código, la ausencia de regresiones y el cumplimiento estricto de las reglas arquitectónicas, ejecuta los mismos comandos del pipeline de integración continua:

```bash
# Pruebas unitarias de todos los módulos
.\gradlew.bat testDebugUnitTest

# Auditoría estática y verificación de lint
.\gradlew.bat lint

# Compilación final del aplicativo
.\gradlew.bat :app:assembleDebug
```

---

## 📄 Documentación Adicional

- [`docs/SPEC.md`](docs/SPEC.md) — Requerimientos funcionales (`F-XX`) y criterios de aceptación (`CA-X`).
- [`CONTRIBUTING.md`](CONTRIBUTING.md) — Convenciones de Git y formato de commits (Conventional Commits).
- [`CLAUDE.md`](CLAUDE.md) — Guía de arquitectura, modularización y tokens del sistema Lumina Glass.
