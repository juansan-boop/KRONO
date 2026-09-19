# Contribuir a KRONO

## Estrategia de ramas
- `main`: siempre desplegable/estable.
- `develop`: integración de features.
- `feature/<nombre>`: una rama por feature-*, ej. `feature/tasks-crud`.

## Conventional Commits
`tipo(scope): descripción corta`

Tipos: `feat`, `fix`, `refactor`, `docs`, `test`, `chore`, `style`.
Ejemplo: `feat(feature-calendar): agregar vista semanal en cuadrícula`

## Antes de abrir un PR
- [ ] El módulo que tocaste no importa otro `feature-*` directamente.
- [ ] Ningún color/espaciado está hardcodeado — todo sale de `core-ui`.
- [ ] Los ViewModel no importan `androidx.compose.*`.
- [ ] La pantalla corresponde a su referencia en `/design` (Stitch) o al frame equivalente en Figma.
