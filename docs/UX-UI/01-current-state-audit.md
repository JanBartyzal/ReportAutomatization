# 01 — Audit aktuálního vizuálního stavu CIM

> Datum auditu: 2026-03-11
> Verze: 1.0
> Scope: `apps/cim/frontend/` — shell + 16 micro-frontend apps + shared-ui lib

---

## 1. Shrnutí

CIM frontend vznikal iterativně ("živelný růst"). Základ je kvalitní — Fluent UI v9 theme system s custom tokeny, 8px grid, light/dark mode. Problém je v **nekonzistentním používání** těchto pravidel napříč komponentami a v **mixu stylizačních přístupů**.

### Celkové hodnocení

| Oblast | Stav | Závažnost |
|--------|------|-----------|
| Theme systém (tokeny) | ✅ Dobrý základ | — |
| Barvy | ⚠️ Mix tokenů a hardcoded | Střední |
| Typografie | ⚠️ Nekonzistentní font-size systém | Vysoká |
| Spacing | ⚠️ Tokeny existují, ale nepoužívají se všude | Střední |
| Komponenty | ⚠️ Mix Fluent + raw CSS + Tailwind | Vysoká |
| Responzivita | ⚠️ Fragmentovaná, bez systému | Střední |
| Dark mode | ✅ Fluent theme, ale CSS soubory ho ignorují | Střední |
| Elevace (shadows) | ⚠️ Mix Fluent tokenů a hardcoded | Nízká |
| Motion/animace | ⚠️ Bez systému, ad-hoc transitions | Nízká |

---

## 2. Co funguje dobře

### 2.1 Fluent Theme Systém
- `tokens.ts` definuje sémantické barvy (red/green/orange) a Azure brand paletu (16 odstínů)
- `lightTheme.ts` / `darkTheme.ts` korektně vytváří Fluent themes s custom overrides
- Font stack `Inter → Roboto → Segoe UI` je dobrá volba pro data-dense UI
- 8px grid spacing tokens jsou definovány

### 2.2 Komponentová knihovna
- `shared-ui` exportuje 60+ komponent z jednoho barrel souboru
- Layout komponenty (Sidebar, TopNav, Layout) konzistentně používají `makeStyles` + `tokens`
- Skeleton loading states existují pro hlavní entity
- Tier/paywall systém je promyšlený (blur, skeleton, screenshot varianty)

### 2.3 Ikony
- Výhradně Fluent Icons v2 — žádný mix knihoven
- Konzistentní naming pattern (`*24Regular`, `*16Regular`)

---

## 3. Identifikované problémy

### 3.1 🔴 Mix stylizačních přístupů (Vysoká priorita)

Existují **4 různé způsoby** stylizace komponent:

| Přístup | Kde se používá | Příklad |
|---------|---------------|---------|
| **Fluent `makeStyles` + `tokens`** | Layout, navigace, formuláře | `Sidebar.tsx`, `TopNav.tsx` |
| **Raw CSS soubory** | Dashboard, locked features | `dashboard.css`, `locked-feature.css` |
| **Tailwind utility classes** | OptimizationControls | `OptimizationControls.tsx` |
| **Inline styles** | Charty, ad-hoc prvky | D3 treemap, SVG animace |

**Problém:** Tailwind je nainstalovaný (v3.4.19) ale **nemá config soubor**. Používá se jen v 1 komponentě s dark theme hardcoded (`bg-gray-800`). To je architektonický dluh.

### 3.2 🔴 Nekonzistentní typografie (Vysoká priorita)

Font-size se používá ve **3 různých systémech** současně:

```
Fluent tokens:    size={200} | size={300} | size={400} | size={800}
CSS rem:          0.75rem | 0.875rem | 1.125rem | 1.25rem | 1.5rem | 2rem | 3.5rem
CSS px:           10px | 11px | 12px | 14px | 18px
```

**dashboard.css** definuje vlastní typografickou škálu (rem), která neodpovídá Fluent tokenům. Navíc přepisuje `h3` globálně.

### 3.3 🟡 Hardcoded barvy mimo token systém (Střední priorita)

**dashboard.css** používá barvy mimo definovaný systém:

| Hardcoded | Kontext | Měl by být token |
|-----------|---------|-----------------|
| `#1e3a5f`, `#0f172a` | Hero gradient | `colorBrandBackground` variant |
| `#e2e8f0`, `#f1f5f9` | Bordery, separátory | `colorNeutralStroke1` |
| `#f8fafc` | Card backgrounds | `colorNeutralBackground2` |
| `#334155`, `#1e293b` | Text barvy | `colorNeutralForeground1/2` |
| `#64748b`, `#94a3b8` | Secondary text | `colorNeutralForeground3/4` |
| `#3b82f6`, `#2563eb` | Action button | `colorBrandBackground` |
| `#dcfce7`, `#166534` | Savings badge | `colorPaletteGreenBackground1` |
| `#22c55e`, `#ef4444` | Better/worse | `semanticColors.green/red` |

Tyto barvy pochází z Tailwind CSS default palety, ne z Azure/Fluent systému.

### 3.4 🟡 Font-family konflikt (Střední priorita)

```css
/* dashboard.css */
font-family: 'Segoe UI', system-ui, sans-serif;

/* lightTheme.ts */
fontFamilyBase: "'Inter', 'Roboto', 'Segoe UI', sans-serif";
```

Dashboard CSS přepisuje font stack a staví Segoe UI na první místo, zatímco theme systém preferuje Inter.

### 3.5 🟡 Dark mode nekompatibilita CSS souborů (Střední priorita)

- `dashboard.css` — **žádná dark mode podpora** (hardcoded `background: white`, `color: #1e293b`)
- `locked-feature.css` — má `@media (prefers-color-scheme: dark)`, ale to nerespektuje Fluent theme toggle (který je JS-based, ne media query)
- `responsive.css` — neutrální, ale `!important` overrides mohou kolidovat

### 3.6 🟡 Responzivní systém bez jednotné strategie (Střední priorita)

Breakpointy jsou definovány na 3 místech:

```
responsive.css:    --breakpoint-sm: 640px, --breakpoint-md: 768px, etc.
dashboard.css:     @media (max-width: 1024px), @media (max-width: 768px)
Sidebar.tsx:       window.innerWidth < 768 (JS breakpoint)
```

Neexistuje sdílený breakpoint systém. CSS variables v `responsive.css` se nepoužívají v jiných souborech.

### 3.7 🟢 Border-radius nekonzistence (Nízká priorita)

```
16px — hero cards, accuracy badge
12px — panels, overlay content, comparison
8px — opportunity cards, skeletons, locked feature container
6px — buttons (CTA, action)
999px — pills/badges
```

Chybí definovaný systém zaoblení (např. `--radius-sm: 4px`, `--radius-md: 8px`, `--radius-lg: 16px`).

### 3.8 🟢 Shadow/elevace nekonzistence (Nízká priorita)

| Shadow | Kontext |
|--------|---------|
| `tokens.shadow4` | Fluent cards |
| `0 2px 8px rgba(0,0,0,0.05)` | Dashboard panels |
| `0 4px 20px rgba(0,0,0,0.15)` | Hero card |
| `0 4px 6px -1px rgba(0,0,0,0.1)` | Card hover |
| `0 4px 12px rgba(0,120,212,0.4)` | CTA hover (brand shadow) |
| `0 4px 24px rgba(0,0,0,0.12)` | Upgrade overlay |

Mix Fluent shadow tokenů a vlastních box-shadow hodnot.

---

## 4. Charting & Data Visualization

### Používané knihovny

| Knihovna | Verze | Účel |
|---------|-------|------|
| Recharts | 2.15.0 | Line charts, Sankey diagramy |
| D3.js | — | Treemap |
| Mermaid | 11.4.1 | Architecture diagramy |
| XY Flow | — | Infrastructure designer (graph) |

### Problémy
- **Žádný sdílený barevný systém** pro charty — barvy jsou inline v komponentách
- D3 treemap má vlastní barevnou paletu odpojenou od theme tokenů
- Recharts používá default SVG styling
- Framer Motion je nainstalovaný ale nepoužívaný

---

## 5. Micro-frontend apps — stav stylizace

| App | Má vlastní CSS? | Používá shared-ui? | Poznámka |
|-----|-----------------|---------------------|----------|
| shell | Minimální | ✅ Layout, TopNav, Sidebar | Root host |
| dashboard | ✅ dashboard.css | ✅ KpiCard, Widgets | Nejvíce raw CSS |
| ai | Ne | ✅ AIChat | — |
| compliance | Ne | ✅ Widgets | — |
| config | Ne | ✅ Forms | — |
| designer | Ne | ✅ + XY Flow | Vlastní node styling |
| admin | Minimální | ✅ DataGrid, Forms | — |
| reports | Ne | ✅ ExportButtons | — |
| onboarding | Ne | ✅ Forms, Stepper | — |
| executive | Ne | ✅ KpiCard | — |
| itbm | Ne | ✅ DataGrid | — |
| marketplace | Ne | ✅ Cards | — |
| plans | Ne | ✅ Forms | — |
| pricing | Ne | ✅ Widgets | — |
| converter | Ne | ✅ Forms | — |
| import-wizard | Ne | ✅ Stepper, Forms | — |

**Závěr:** Dashboard app je hlavní zdroj vizuálních nekonzistencí kvůli rozsáhlému raw CSS mimo Fluent systém.

---

## 6. Doporučení pro revizi

### Okamžitá opatření (Quick wins)
1. **Odstranit nebo nakonfigurovat Tailwind** — buď plně integrovat s Fluent tokeny, nebo odebrat a přepsat 1 komponentu
2. **Nahradit hardcoded barvy v `dashboard.css`** za Fluent CSS variables
3. **Sjednotit font-family** — odstranit override v dashboard.css

### Střednědobé kroky (Design System)
4. **Definovat typografickou škálu** — mapovat rem/px na Fluent size tokens
5. **Definovat elevation systém** — 4 úrovně (flat, raised, floating, overlay)
6. **Definovat border-radius systém** — 4 úrovně (sm, md, lg, pill)
7. **Chart color palette** — 8-12 barev odvozených z brand palety

### Dlouhodobé cíle
8. **Migrovat `dashboard.css`** na `makeStyles` + tokens
9. **Sjednotit responsive breakpoint systém** (CSS → JS hook nebo Fluent breakpoints)
10. **Storybook audit** — ověřit, že všechny shared-ui komponenty mají stories

→ Detailní specifikace viz [02-design-system.md](02-design-system.md)
