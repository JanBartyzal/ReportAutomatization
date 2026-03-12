# CIM UX/UI — Vizuální revize & Design System

Dokumentace pro sjednocení vizuálního stylu CIM produktu. Vznikla na základě auditu aktuálního stavu frontendu (březen 2026).

## Obsah

| Dokument | Účel |
|----------|------|
| [01-current-state-audit.md](01-current-state-audit.md) | Audit aktuálního stavu — co existuje, kde jsou nekonzistence |
| [02-design-system.md](02-design-system.md) | Návrh jednotného Design Systému — barvy, typografie, spacing, elevace, motion |
| [03-figma-components.md](03-figma-components.md) | Figma brief pro atomické a molekulární komponenty |
| [04-figma-pages.md](04-figma-pages.md) | Figma brief pro stránky a layouty (wireframes + specifikace) |

## Workflow

1. **Audit** → přečíst `01-current-state-audit.md` pro pochopení výchozího stavu
2. **Review Design System** → `02-design-system.md` definuje pravidla, která sjednotí styl
3. **Figma práce** → podle `03` a `04` vytvořit Figma screens pro review
4. **Implementace** → po schválení Figma návrhů aplikovat do kódu

## Klíčové soubory v kódu

| Cesta | Popis |
|-------|-------|
| `apps/cim/frontend/libs/shared-ui/src/themes/tokens.ts` | Barevné a spacing tokeny |
| `apps/cim/frontend/libs/shared-ui/src/themes/lightTheme.ts` | Fluent light theme |
| `apps/cim/frontend/libs/shared-ui/src/themes/darkTheme.ts` | Fluent dark theme |
| `apps/cim/frontend/libs/shared-ui/src/styles/dashboard.css` | Dashboard CSS (raw) |
| `apps/cim/frontend/libs/shared-ui/src/styles/locked-feature.css` | Tier overlay CSS |
| `apps/cim/frontend/libs/shared-ui/src/styles/responsive.css` | Responsive utilities |
| `apps/cim/frontend/libs/shared-ui/src/index.ts` | Component barrel export |
