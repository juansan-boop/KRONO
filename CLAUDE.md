# KRONO — Contexto del proyecto

## Qué es KRONO

App móvil Android para gestión de tiempo, dirigida a **estudiantes universitarios que también trabajan**. Proyecto académico grupal (4 personas, trabajando por módulos).

Diferenciador clave: en lugar de pedir estimaciones manuales (como Google Calendar, TickTick o Notion), KRONO usa un **cronómetro** para medir la duración real de cada tarea, aprende de ese historial y genera estimaciones personalizadas + sugerencias de priorización por IA.

Todo el copy de la UI está en **español**.

## Equipo

Juan Esteban Sora Ariza, Janer García Avendaño García, Juan Fernando Sánchez Otero, Kevin Vuskovit García González.

## Sistema de diseño — Lumina Glass System (cerrado, no renegociable)

- Glassmorphism oscuro. **Siempre dark, no hay tema claro.**
- Paleta base: fondo Deep Midnight `#121221`, primario magenta `#BD00FF` / `#ECB2FF`, secundario cyan `#49D9E5`.
- Tipografía: Hanken Grotesk (texto) + JetBrains Mono (datos/números), grid de 8dp.
- Iconografía: Material Symbols Outlined.
- Ya implementado en `core/core-ui/src/main/java/com/krono/core/ui/theme/`: `Color.kt`, `Type.kt`, `Shape.kt`, `Theme.kt`, `Glass.kt`.
- **Regla dura: cero colores o espaciados hardcodeados.** Todo sale de `core-ui`. Usa `Modifier.glassSurface()` (en `Glass.kt`) para cualquier superficie con efecto vidrio, en vez de recrearlo a mano.
- Las fuentes reales (Hanken Grotesk, JetBrains Mono) todavía no están agregadas como recursos — `Type.kt` usa placeholders (`FontFamily.Default` / `FontFamily.Monospace`). Al agregar los `.ttf`/`.otf` a `res/font/`, actualizar `HankenGrotesk` y `JetBrainsMono` en `Type.kt`.

## Arquitectura (cerrada, no renegociable)

MVVM + Clean Architecture, límites modulares estrictos.

Estructura Gradle multi-módulo (ya generada en este repo):
- `app`
- `core/core-ui`, `core/core-data`, `core/core-domain`, `core/core-common`
- `feature-auth`, `feature-onboarding`, `feature-dashboard`, `feature-tasks`, `feature-calendar`, `feature-profile`

Reglas duras:
- Los ViewModels **nunca** importan `androidx.compose.*`.
- Los módulos feature **nunca** se importan entre sí directamente — solo `app` puede depender de todos. Comunicación entre features vía `core-domain` o navegación.
- Sin colores/espaciados hardcodeados — todo desde `core-ui`.
- Inyección de dependencias con Hilt (ya configurado en `app` y en cada `feature-*`).
- Persistencia con Room (dependencia ya declarada en `core-data`).

## Diseño de referencia

- Diseñado en Google Stitch, exportado como ZIP (una carpeta por pantalla, cada una con `code.html` + `screen.png`).
- El export vive en `/design/` en este repo, en la raíz. Cada carpeta `feature-*` tiene un `README.md` interno listando qué carpetas de `/design/` le corresponden.
- Al implementar una pantalla, revisar primero `feature-*/src/main/java/com/krono/feature/<nombre>/README.md` para saber qué referencia visual usar.
- Si el MCP de Figma está conectado (lectura: `get_design_context`, `get_screenshot`, `get_variable_defs`), preferir el frame de Figma sobre el `code.html` estático cuando ambos existan — el de Figma puede reflejar ajustes posteriores al export original.

## Estado actual

Scaffold recién generado: estructura de módulos, Gradle (version catalog centralizado), tema `core-ui`, CI básico y `/design` poblado. El Gradle Wrapper ya está versionado (Gradle 9.6.0); compilar requiere JDK 17 (ver README.md). Ningún feature-* tiene pantallas implementadas todavía; el objetivo inmediato es empezar a construir screen por screen.

## Cómo trabajar en este repo

- Cada feature se implementa dentro de su módulo `feature-*`, siguiendo el layout de su carpeta correspondiente en `/design/` (ver el README interno de cada módulo).
- Antes de un PR, revisar el checklist en `.github/PULL_REQUEST_TEMPLATE.md` (fronteras de módulo, no hardcodear estilos, ViewModels sin Compose).
- Usar Conventional Commits (ver `CONTRIBUTING.md`).
- Métricas de éxito del producto (para tener en mente al priorizar features): reducción de la brecha entre tiempo estimado y real, y reducción de la tasa de entregas extemporáneas.

## Calidad y documentación del código

Estándar obligatorio para todo código Kotlin del repo:

- **KDoc en español** para toda clase, interfaz, objeto, caso de uso y
  función o propiedad pública. Usa `@param`, `@return` y `@throws` solo cuando
  aporten información que el nombre y la firma no dan.
- **Comentarios inline solo para explicar el "por qué"** (una decisión, una
  restricción, un caso borde), nunca el "qué" hace la línea.
- **Funciones cortas con una sola responsabilidad.** Si necesitas "y" para
  describirla, divídela.
- **Nombres descriptivos** para clases, funciones, variables y pruebas.
- **Sin código muerto ni comentado.** Lo que no se usa se borra (git conserva
  el historial).
- **Sin números mágicos:** todo literal con significado va en una constante
  con nombre (los espaciados y tamaños siguen saliendo de `core-ui`).
- **Clases y archivos con un propósito claro:** un archivo no mezcla
  responsabilidades ajenas entre sí.
- **Alcance en los PR:** el código que se toque en un PR debe quedar
  documentado según este estándar. No se hacen refactors masivos de lo que el
  PR no toca.

## Uso de la skill mobile-android-design

Esta skill (Material Design 3 + Jetpack Compose, de wshobson/agents) se usa
ÚNICAMENTE para:
- Patrones estructurales de Compose: layouts (Column/Row, LazyColumn/LazyVerticalGrid),
  layouts adaptativos, gestos e interacciones
- Navigation Compose (bottom navigation, drawers, manejo de estado de navegación)
- Buenas prácticas de state hoisting, recomposición y performance
- Accesibilidad: content descriptions, touch targets

NO se usa para:
- Theming, esquemas de color ni tipografía — eso viene EXCLUSIVAMENTE de
  `core-ui/theme` (Color.kt, Type.kt, Shape.kt, Theme.kt, Glass.kt)
- Dynamic color / Material You — KRONO no lo implementa, el sistema de diseño
  es Lumina Glass System: paleta fija (Deep Midnight #121221, magenta
  #BD00FF/#ECB2FF, cyan #49D9E5), siempre oscuro, sin tema claro
- Componentes M3 "de catálogo" tal cual (Card, Button, TextField por defecto)
  sin pasar por los wrappers/tokens de `core-ui`

Regla dura: si un ejemplo de la skill sugiere `MaterialTheme.colorScheme.*` o
`dynamicColorScheme()`, se descarta esa parte y se reemplaza por los tokens de
`core-ui`. Cero colores/espaciados hardcodeados sigue aplicando siempre.

## Especificación

Las funcionalidades, criterios de aceptación y decisiones abiertas están en `docs/SPEC.md`.
