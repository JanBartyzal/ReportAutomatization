# 02 — Clouply Design System — Návrh sjednocení

> Verze: 1.0 (draft pro Figma review)
> Datum: 2026-03-11
> Základ: Fluent UI v9 + Azure brand identity

---

## 1. Design Principles

### 1.1 Hodnoty

| Princip | Popis |
|---------|-------|
| **Data-first** | UI slouží datům. Každý pixel musí podporovat čitelnost a rychlé rozhodování. |
| **Consistent** | Jeden vzor = jedno řešení. Žádné ad-hoc styly. |
| **Accessible** | WCAG 2.1 AA minimum. Kontrast ≥ 4.5:1 pro text, ≥ 3:1 pro interaktivní prvky. |
| **Responsive** | Desktop-first s graceful degradation na tablet/mobile. |
| **Themed** | Vše přes Fluent tokeny. Light/dark mode musí fungovat automaticky. |
| **Glassmorphism** | Subtilní průhledné/frosted glass efekty pro vybrané prvky. Dodává premium look. |

### 1.2 Glassmorphism — pravidla použití

Glassmorphism (`backdrop-filter: blur()` + poloprůhledné pozadí) je schválený designový prvek. Pravidla:

| Kde ANO | Hodnoty |
|---------|---------|
| **TopNav** | `rgba(20, 20, 20, 0.8)` + `backdrop-filter: blur(20px)` |
| **Floating panels** (AI chat, Feedback widget) | `rgba(255, 255, 255, 0.85)` + `blur(16px)` (light) / `rgba(30, 30, 30, 0.85)` + `blur(16px)` (dark) |
| **Modal overlays** | `rgba(0, 0, 0, 0.4)` + `blur(4px)` na backdrop |
| **Locked feature overlay** | `rgba(255, 255, 255, 0.15)` + `blur(2px)` |

| Kde NE |
|--------|
| Karty v gridu (KPI, Widget) — ty mají solid bg |
| DataGrid tabulky — čitelnost dat je priorita |
| Formuláře — solid backgrounds pro kontrast |
| Sidebar navigace — solid bg pro čitelnost |

**Dark mode:** Glassmorphism funguje lépe v dark mode. V light mode snížit opacity a blur, aby nedošlo k "bělení" obsahu.

### 1.3 Anti-patterns (zakázané přístupy)

- ❌ Hardcoded hex barvy mimo token systém
- ❌ Tailwind utility classes (buď full adoption, nebo nula — volíme nulu, Fluent stačí)
- ❌ Raw CSS soubory pro komponent styling (jen globální reset/base)
- ❌ `!important` overrides
- ❌ Inline `style={}` pro layout/barvy (pouze pro dynamické runtime hodnoty: pozice, rozměry z dat)
- ❌ Přepisování Fluent komponent CSS selektory

---

## 2. Barvy

### 2.1 Brand Palette (Azure)

Primární brand barva: **Azure Blue `#0078D4`** (slot 90)

```
Brand 10:  #020305    ██  Darkest (text na light bg)
Brand 20:  #111723    ██
Brand 30:  #16263D    ██
Brand 40:  #193253    ██  Dark UI elements
Brand 50:  #1B3F6A    ██
Brand 60:  #1B4C82    ██
Brand 70:  #18599B    ██  Hover states
Brand 80:  #1267B4    ██  Active states
Brand 90:  #0078D4    ██  PRIMARY — buttons, links, focus rings
Brand 100: #2886E0    ██  Light mode foreground on dark bg
Brand 110: #4695EB    ██
Brand 120: #60A3F5    ██
Brand 130: #7AB2FF    ██  Dark mode foreground
Brand 140: #91C1FF    ██
Brand 150: #A7CFFF    ██
Brand 160: #BDDDFF    ██  Lightest tint
```

### 2.2 Sémantické barvy

| Sémantika | Light foreground | Light bg | Dark foreground | Dark bg | Použití |
|-----------|-----------------|---------|----------------|---------|---------|
| **Success** | `#107C10` | `#DFF6DD` | `#54B054` | `#052505` | Optimalizovaný zdroj, úspěch, savings |
| **Danger** | `#D13438` | `#FDE7E9` | `#F1707B` | `#3D1314` | Budget overrun, chyba, smazání |
| **Warning** | `#D83B01` | `#FDEBE2` | `#FCE100` | `#4A1504` | Chybějící data, pending, pozor |
| **Info** | `#0078D4` | `#DEECF9` | `#2886E0` | `#0A2E4A` | Informativní badge, tip |

### 2.3 Neutrální škála

| Token | Light | Dark | Účel |
|-------|-------|------|------|
| `Background1` | `#FFFFFF` | `#1B1A19` | Primární pozadí (karty, tabulky) |
| `Background2` | `#F8F8F8` | `#252423` | Sekundární pozadí (hlavičky, stripe rows) |
| `Background3` | `#F3F2F1` | `#323130` | Terciární (separátory, disabled oblasti) |
| `Stroke1` | `#E1DFDD` | `#484644` | Bordery, dividers |
| `Foreground1` | `#242424` | `#FFFFFF` | Primární text |
| `Foreground2` | `#616161` | `#D6D6D6` | Sekundární text |
| `Foreground3` | `#9E9E9E` | `#ADADAD` | Disabled / placeholder text |

### 2.4 Chart Palette

8 barev pro datovou vizualizaci, odvozeno z brand palety + doplňkové barvy:

```
Chart 1:  #0078D4  ██  Azure Blue (primary series)
Chart 2:  #107C10  ██  Green (success/savings)
Chart 3:  #D13438  ██  Red (danger/overrun)
Chart 4:  #D83B01  ██  Orange (warning)
Chart 5:  #8764B8  ██  Purple (category 5)
Chart 6:  #008272  ██  Teal (category 6)
Chart 7:  #E3008C  ██  Magenta (category 7)
Chart 8:  #986F0B  ██  Gold (category 8)
```

Pro gradienty v hero sekcích:
```
Hero gradient:    linear-gradient(135deg, Brand50 0%, Brand20 100%)
                  = linear-gradient(135deg, #1B3F6A 0%, #111723 100%)
```

### 2.5 Tier barvy

| Tier | Badge bg | Badge text | Účel |
|------|---------|-----------|------|
| Micro | `#E0F2FE` | `#0369A1` | Free tier |
| Starter | `#FEF3C7` | `#92400E` | Entry paid |
| Pro | `#EDE9FE` | `#6D28D9` | Professional |
| Enterprise | `#FCE7F3` | `#9D174D` | Enterprise |

---

## 3. Typografie

### 3.1 Font Stack

```
Primary:    'Inter', 'Roboto', 'Segoe UI', sans-serif
Monospace:  'Consolas', 'Courier New', monospace
```

**Inter** je primární font. Musí být loadovaný přes `@fontsource/inter` nebo Google Fonts. Fallback na Roboto → Segoe UI.

### 3.2 Typografická škála

Založena na Fluent UI size tokens s mapováním na CSS ekvivalenty:

| Název | Fluent size | CSS | Weight | Line-height | Použití |
|-------|------------|-----|--------|-------------|---------|
| **Display** | 800 | 2rem (32px) | 700 | 1.2 | Hero čísla (měsíční náklady) |
| **Title 1** | 600 | 1.5rem (24px) | 600 | 1.3 | Nadpisy stránek |
| **Title 2** | 500 | 1.25rem (20px) | 600 | 1.4 | Nadpisy sekcí, karty |
| **Title 3** | 400 | 1rem (16px) | 600 | 1.5 | Podnadpisy, widget headers |
| **Body 1** | 300 | 0.875rem (14px) | 400 | 1.5 | Hlavní text, popisy |
| **Body 2** | 200 | 0.75rem (12px) | 400 | 1.5 | Drobný text, badges, labels |
| **Caption** | 100 | 0.625rem (10px) | 400 | 1.4 | Miniaturní popisky (sidebar section headers) |

### 3.3 Typografická pravidla

- **Čísla:** Vždy `fontFamilyNumeric` (tabular figures) pro finanční data
- **KPI hodnoty:** Display (size 800), weight 700
- **Nadpisy karet:** Title 3 (size 400), weight 600
- **Tabulky:** Body 1 (size 300) pro buňky, Title 3 (size 400) weight 600 pro hlavičky
- **Labels/captions:** Body 2 (size 200), uppercase jen pro section headers v sidebaru
- **Letter-spacing:** `0.05em` pouze pro uppercase labels

---

## 4. Spacing & Layout

### 4.1 Spacing Scale (8px grid)

| Token | Hodnota | Alias | Použití |
|-------|---------|-------|---------|
| `XXS` | 2px | — | Minimální gap (icon + text inline) |
| `XS` | 4px | — | Tight gap (nav items, badges) |
| `S` | 8px | `spacingS` | Malý padding, gap v kompaktních layoutech |
| `M` | 16px | `spacingM` | Standardní padding, gap v grid |
| `L` | 24px | `spacingL` | Section padding, card padding |
| `XL` | 32px | `spacingXL` | Page section margins |
| `XXL` | 40px | `spacingXXL` | Hero sections, page top padding |
| `XXXL` | 48px | — | Max spacing (search bar margins) |

### 4.2 Layout Grid

```
Max width:        1400px (content area, centrovaný)
Grid:             12 sloupců, gap 24px (spacingL)
Sidebar expanded: 260px
Sidebar collapsed: 60px
TopNav height:    64px
Card min-height:  Neurčeno (závisí na obsahu)
```

### 4.3 Breakpoints

| Název | Hodnota | Chování |
|-------|---------|---------|
| `xs` | 0–639px | Mobil — single column, sidebar schovaný |
| `sm` | 640–767px | Malý mobil — sidebar overlay |
| `md` | 768–1023px | Tablet — 2 sloupce, sidebar collapsed |
| `lg` | 1024–1279px | Desktop — 3 sloupce, sidebar expanded |
| `xl` | 1280px+ | Široký desktop — full layout |

**Implementace:** Custom React hook `useBreakpoint()` (ne CSS media queries s different hodnotami na různých místech).

---

## 5. Elevace (Shadows)

### 5.1 Systém úrovní

| Úroveň | Fluent token | CSS hodnota | Použití |
|---------|-------------|-------------|---------|
| **Level 0** | — | none | Flat prvky (inline) |
| **Level 1** | `shadow2` | `0 1px 2px rgba(0,0,0,0.06)` | Karty v gridu, tabulky |
| **Level 2** | `shadow4` | `0 2px 8px rgba(0,0,0,0.08)` | Aktivní karty, hover state |
| **Level 3** | `shadow8` | `0 4px 16px rgba(0,0,0,0.12)` | Dropdowns, popovers, tooltips |
| **Level 4** | `shadow16` | `0 8px 32px rgba(0,0,0,0.16)` | Modaly, dialogy, floating panels |

### 5.2 Pravidla

- Hover na kartě: Level 1 → Level 2 (transition 200ms)
- Overlay (locked feature): `backdrop-filter: blur(2px)` + Level 3
- TopNav: Fixní Level 2 + `backdrop-filter: blur(20px)`
- **Zakázáno:** Colored shadows (kromě brand CTA: `0 4px 12px rgba(0,120,212,0.3)`)

---

## 6. Border Radius

| Token | Hodnota | Použití |
|-------|---------|---------|
| `radiusNone` | 0 | Tabulky (vnější border), full-width sekce |
| `radiusSm` | 4px | Input fields, malé badges |
| `radiusMd` | 8px | Karty, buttony, dropdown items |
| `radiusLg` | 12px | Hero karty, modal dialogy, overlay content |
| `radiusXl` | 16px | Dashboard hero, onboarding karty |
| `radiusPill` | 999px | Pill badges, tags, toggles |

---

## 7. Ikony

### 7.1 Pravidla

- **Výhradně Fluent Icons v2** (`@fluentui/react-icons`)
- Velikosti: `24Regular` pro navigaci a akce, `16Regular` pro inline indikátory
- `*Filled` varianta pouze pro aktivní/selected stav
- Barva ikon: dědí `color` od parent textu, nebo explicit přes Fluent token

### 7.2 Custom ikony

Pro cloud provider loga a specifické CIM ikony (pokud Fluent nemá):
- SVG v `shared-ui/src/icons/`
- Viewbox `0 0 24 24`, stroke-width 1.5
- Exportovat jako React komponenty se stejným API jako Fluent icons

---

## 8. Motion & Animace

### 8.1 Timing funkce

| Typ | Duration | Easing | Použití |
|-----|----------|--------|---------|
| **Micro** | 100ms | `ease-out` | Button press, toggle |
| **Short** | 200ms | `ease-in-out` | Hover effects, focus rings |
| **Medium** | 300ms | `ease-in-out` | Sidebar expand/collapse, panel transitions |
| **Long** | 500ms | `ease-in-out` | Page transitions, chart animations |

### 8.2 Pravidla

- `transform` a `opacity` pouze (GPU-accelerated properties)
- Skeleton loading: `@keyframes pulse` (2s, infinite)
- Chart enter: fade-in + scale (300ms)
- Sidebar: width transition 200ms ease-in-out
- **Zakázáno:** `transition: all`, animace na `height`/`width` elementů s text content
- `prefers-reduced-motion: reduce` — vypnout vše kromě opacity transitions

---

## 9. Formuláře

### 9.1 Struktura

```
<Field>              ← Fluent Field wrapper (label, validation, hint)
  <Input />          ← Fluent Input / Select / Textarea
</Field>
```

### 9.2 Stavy

| Stav | Border | Label | Ikona |
|------|--------|-------|-------|
| Default | `Stroke1` | `Foreground2` | — |
| Hover | `Brand80` | `Foreground2` | — |
| Focus | `Brand90` 2px | `Brand90` | — |
| Error | `Red` | `Red` | `ErrorCircle16Regular` |
| Disabled | `Stroke1` 50% opacity | `Foreground3` | — |

### 9.3 Layout

- Labels: vždy nad inputem (ne inline, ne floating)
- Required marker: `*` za label textem, barva `Red`
- Hint text: pod inputem, Body 2, `Foreground3`
- Error message: pod inputem, Body 2, `Red`, s ikonou
- Field spacing: `spacingM` (16px) vertikálně mezi poli

---

## 10. Data Display

### 10.1 Tabulky (DataGrid)

- Header: `Background2`, Title 3 weight 600, uppercase disabled
- Rows: alternating `Background1` / `Background2` (zebra stripe)
- Row height: 40px minimum
- Cell padding: `spacingS` horizontálně, `spacingXS` vertikálně
- Sort ikony: `ArrowSort24Regular`
- Pagination: Fluent `Pagination` pod tabulkou, right-aligned

### 10.2 KPI Karty

```
┌─────────────────────────────┐
│  Title 3        [trend ↑]   │  ← Header: label + trend icon
│                             │
│  Display                    │  ← KPI value (velké číslo)
│  Body 2 (secondary)        │  ← Subtitle / context
│                             │
│  [sparkline]                │  ← Volitelný mini chart
└─────────────────────────────┘
```

- Trend indikátor: ↑ zelený (pozitivní), ↓ červený (negativní), → šedý (stagnace)
- Padding: `spacingL` (24px)
- Shadow: Level 1
- Hover: Level 2

### 10.3 Widget Karty

```
┌─────────────────────────────┐
│  CardHeader                 │  ← Fluent CardHeader (title + action menu)
├─────────────────────────────┤
│                             │
│  Content area               │  ← Flexible content (chart, list, form)
│                             │
│                             │
└─────────────────────────────┘
```

- Radius: `radiusMd` (8px)
- Border: `Stroke1` 1px
- Padding: `spacingL` (24px) — content area

---

## 11. Dark Mode

### 11.1 Implementace

- **Fluent `FluentProvider`** s `theme` prop přepíná celou paletu
- **`useTheme()` hook** z `shared-state` — `isDarkMode`, `setThemePreference('light'|'dark'|'system')`
- **CSS soubory musí používat Fluent CSS variables** (automaticky se přepínají), ne hardcoded barvy
- `@media (prefers-color-scheme: dark)` jen pro elementy mimo FluentProvider scope (splash screen, HTML body)

### 11.2 Pravidla pro dark mode

- Žádné `background: white` nebo `color: #1e293b` v CSS — nahradit tokeny
- Shadows v dark mode: nižší opacity (0.3–0.5 → 0.2–0.3)
- Gradienty: upravit na dark varianty (Brand40 → Brand20)
- Charty: barvy z Chart Palette fungují v obou módech (ověřit kontrast)

---

## 12. Přístupnost (A11y)

### 12.1 Minimální požadavky

- Kontrast textu: ≥ 4.5:1 (AA)
- Kontrast interaktivních prvků: ≥ 3:1
- Focus ring: `2px solid Brand90`, offset `2px`
- Keyboard navigace: Tab order, Enter/Space pro akce, Escape pro zavření
- ARIA labels pro ikony bez textu
- `role="status"` pro live KPI updates

### 12.2 Specifika CIM

- Tabulky: `role="grid"`, header cells `scope="col"`
- Treemap: `role="img"`, `aria-label` se sumářem dat
- Locked features: `aria-disabled="true"`, popis blokace ve screen readeru
- Skeleton loading: `aria-busy="true"` na containeru

---

## 13. Migrační plán

### Fáze 1: Quick wins (1–2 dny)
- [ ] Smazat Tailwind z `OptimizationControls.tsx`, přepsat na `makeStyles`
- [ ] Sjednotit `font-family` — smazat override v `dashboard.css`
- [ ] Definovat CSS custom properties pro Chart Palette v `:root`

### Fáze 2: Token migration (3–5 dní)
- [ ] Nahradit všechny hardcoded barvy v `dashboard.css` za Fluent CSS variables
- [ ] Přidat dark mode support do `dashboard.css` (nebo migrovat na makeStyles)
- [ ] Sjednotit border-radius na definovaný systém
- [ ] Sjednotit shadow systém

### Fáze 3: Systémové změny (1–2 týdny)
- [ ] Implementovat `useBreakpoint()` hook
- [ ] Migrovat `dashboard.css` kompletně na `makeStyles` + tokens
- [ ] Vytvořit sdílené chart wrapper komponenty s theme-aware barvami
- [ ] Storybook: přidat stories pro všechny atomic komponenty
- [ ] Smazat `responsive.css` po migraci na hook-based systém

### Fáze 4: Polish (ongoing)
- [ ] A11y audit (axe-core)
- [ ] Performance audit (rendering, re-renders)
- [ ] Visual regression testing (Chromatic / Percy)
