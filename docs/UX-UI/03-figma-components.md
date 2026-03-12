# 03 — Figma Brief: Komponenty

> Specifikace pro vytvoření Figma component library.
> Každá sekce = 1 Figma frame/page s variantami a stavy.

---

## Organizace v Figma

```
📁 Clouply Design System
  📄 00 — Foundations (barvy, typografie, spacing, ikony)
  📄 01 — Atoms (tlačítka, inputy, badges, ikony)
  📄 02 — Molecules (karty, form fields, nav items)
  📄 03 — Organisms (tabulky, widgety, navigace)
  📄 04 — Templates (page layouts, dashboardy)
  📄 05 — Pages (konkrétní screens)
```

---

## 00 — Foundations

### 00.1 Color Palette

**Frame:** Všechny barvy z `02-design-system.md` sekce 2, vyložené jako swatche.

```
┌──────────────────────────────────────────────────────────┐
│  BRAND PALETTE                                           │
│  ┌────┐ ┌────┐ ┌────┐ ... ┌────┐                       │
│  │ 10 │ │ 20 │ │ 30 │     │160 │                       │
│  │####│ │####│ │####│     │####│                       │
│  └────┘ └────┘ └────┘     └────┘                       │
│                                                          │
│  SEMANTIC                                                │
│  ┌────┐ ┌────┐ ┌────┐ ┌────┐                           │
│  │Succ│ │Dang│ │Warn│ │Info│                           │
│  │FG  │ │FG  │ │FG  │ │FG  │   (foreground + bg var.) │
│  └────┘ └────┘ └────┘ └────┘                           │
│                                                          │
│  NEUTRAL (Light + Dark side by side)                     │
│  BG1 │ BG2 │ BG3 │ Stroke1 │ FG1 │ FG2 │ FG3          │
│                                                          │
│  CHART PALETTE (8 barev)                                 │
│  TIER BADGES (4 × bg + fg)                              │
└──────────────────────────────────────────────────────────┘
```

### 00.2 Typography Scale

**Frame:** Všech 7 úrovní typografie, light + dark mode, s ukázkovým textem.

```
Display    32px / 700 / 1.2    "Kč 1 234 567"
Title 1    24px / 600 / 1.3    "Přehled infrastruktury"
Title 2    20px / 600 / 1.4    "Nákladová optimalizace"
Title 3    16px / 600 / 1.5    "Úspory tento měsíc"
Body 1     14px / 400 / 1.5    "Celkem 47 zdrojů bylo analyzováno..."
Body 2     12px / 400 / 1.5    "Poslední aktualizace: 11.3.2026"
Caption    10px / 400 / 1.4    "PLATFORMA"
```

### 00.3 Spacing & Grid

**Frame:** Vizualizace 8px grid systému, ukázka 12-sloupcového gridu.

```
Spacing:  ■ 2px  ■■ 4px  ████ 8px  ████████ 16px  ████████████ 24px  ...

Grid (1400px):
┌─┬─┬─┬─┬─┬─┬─┬─┬─┬─┬─┬─┐
│1│2│3│4│5│6│7│8│9│0│1│2│  gap: 24px
└─┴─┴─┴─┴─┴─┴─┴─┴─┴─┴─┴─┘
```

### 00.4 Elevation

**Frame:** 5 karet nad sebou, každá s příslušným stínem.

```
Level 0: [Flat]         — žádný stín
Level 1: [Raised]       — shadow2
Level 2: [Active]       — shadow4
Level 3: [Floating]     — shadow8
Level 4: [Overlay]      — shadow16
```

### 00.5 Border Radius

**Frame:** 6 obdélníků s různým zaoblením, labeled.

```
None: 0px    Sm: 4px    Md: 8px    Lg: 12px    Xl: 16px    Pill: 999px
┌────────┐  ╭────────╮  ╭────────╮  ╭────────╮  ╭────────╮  ╭────────╮
│        │  │        │  │        │  │        │  │        │  │        │
└────────┘  ╰────────╯  ╰────────╯  ╰────────╯  ╰────────╯  ╰────────╯
```

---

## 01 — Atoms

### 01.1 Buttons

**Varianty (Figma component properties):**

| Property | Values |
|----------|--------|
| Variant | Primary, Secondary, Outline, Subtle, Transparent |
| Size | Small (28px), Medium (32px), Large (40px) |
| State | Default, Hover, Pressed, Focus, Disabled |
| Icon | None, Left, Right, Icon-only |

**Specifikace Primary:**

```
┌─────────────────────────┐
│  ▶ Přidat zdroj         │   bg: Brand90, color: white
│                         │   radius: 8px, padding: 0 16px
│                         │   height: 32px (medium)
└─────────────────────────┘

Hover:   bg: Brand80, shadow: Level 2
Pressed: bg: Brand70, transform: none
Focus:   outline: 2px solid Brand90, offset 2px
Disabled: bg: Background3, color: Foreground3
```

### 01.2 Input Fields

**Varianty:**

| Property | Values |
|----------|--------|
| Type | Text, Password, Search, Number, Textarea |
| Size | Small (28px), Medium (32px), Large (40px) |
| State | Default, Hover, Focus, Error, Disabled |
| Addons | None, Prefix icon, Suffix icon, Prefix text, Suffix text |

```
Label *
┌──────────────────────────────┐
│ 🔍 Placeholder text...      │   border: 1px Stroke1
│                              │   radius: 4px
└──────────────────────────────┘
Hint text                          Body 2, Foreground3

Error state:
Label *
┌──────────────────────────────┐
│ Invalid input                │   border: 2px Red
│                              │
└──────────────────────────────┘
⚠ Chybová zpráva                  Body 2, Red
```

### 01.3 Status Badges

**Varianty:**

| Property | Values |
|----------|--------|
| Sentiment | Success, Danger, Warning, Info, Neutral |
| Size | Small (20px), Medium (24px) |
| Shape | Rounded (4px), Pill (999px) |

```
 ● Validated       bg: GreenBg, color: Green, radius: pill
 ● Over Budget     bg: RedBg, color: Red
 ● Waiting         bg: OrangeBg, color: Orange
 ● Informace       bg: BlueBg, color: Blue
```

### 01.4 Tier Badges

```
 MICRO       bg: #E0F2FE, color: #0369A1, 10px, bold, uppercase
 STARTER     bg: #FEF3C7, color: #92400E
 PRO         bg: #EDE9FE, color: #6D28D9
 ENTERPRISE  bg: #FCE7F3, color: #9D174D
```

### 01.5 Avatars

**Varianty:**
- Sizes: 24px, 32px, 40px, 64px
- States: Image, Initials, Placeholder icon
- Badge: None, Online (green dot), Offline (gray dot)

### 01.6 Icons

**Frame:** Grid 24px ikon, seskupených podle kategorie:
- Navigation (Home, Settings, Search, Menu, ChevronDown/Right)
- Actions (Add, Delete, Save, Edit, Export, Import)
- Status (Checkmark, Error, Warning, Info)
- Infrastructure (Server, Storage, Database, Cloud, Network)
- Communication (Send, Bot, Person, SignOut)

---

## 02 — Molecules

### 02.1 Form Field (Label + Input + Validation)

```
┌─────────────────────────────────────┐
│  Label *                     Body 1 │
│  ┌─────────────────────────────┐    │
│  │  Value or placeholder       │    │
│  └─────────────────────────────┘    │
│  Hint text (optional)        Body 2 │
│  ⚠ Error message (optional) Body 2 │
│                                     │
│  Spacing: 4px label→input           │
│           4px input→hint/error      │
│           16px field→field          │
└─────────────────────────────────────┘
```

### 02.2 Nav Item

**Varianty:**
- State: Default, Hover, Active, Disabled
- Icon: Yes, No
- Badge: None, Count, Tier, Lock
- Sidebar mode: Expanded (icon + text), Collapsed (icon only + tooltip)

```
Expanded:
┌─────────────────────────────┐
│  🏠  Dashboard        [3]  │   height: 36px, padding: 8px 12px
└─────────────────────────────┘

Collapsed:
┌─────┐
│ 🏠  │   width: 60px, centered, tooltip on hover
└─────┘

Active:
┌─────────────────────────────┐
│  🏠  Dashboard              │   bg: Background1Selected, color: Brand90
└─────────────────────────────┘

Locked:
┌─────────────────────────────┐
│  🏠  Dashboard  🔒 PRO     │   lock icon 16px + tier badge
└─────────────────────────────┘
```

### 02.3 KPI Card

**Varianty:**
- Trend: Up (green), Down (red), Flat (gray)
- Size: Standard, Compact
- Sparkline: Yes, No

```
Standard (min-width: 240px):
┌───────────────────────────────┐
│  Měsíční náklady    ↑ +12%   │  Title 3 + Badge
│                               │
│  Kč 1 234 567                │  Display, weight 700
│  vs. minulý měsíc            │  Body 2, Foreground3
│                               │
│  ╱╲_╱╲╱‾╲                     │  Sparkline (optional)
│                               │
│  Padding: 24px                │
│  Shadow: Level 1              │
│  Radius: 8px                  │
└───────────────────────────────┘
```

### 02.4 Widget Card (generic container)

```
┌───────────────────────────────┐
│  Title            ⋮ [menu]   │  CardHeader — Title 3 + action menu
├───────────────────────────────┤
│                               │
│  [ Content slot ]             │  Flexible: chart, list, form, text
│                               │
│  Padding: 24px                │
│  Shadow: Level 1              │
│  Radius: 8px                  │
│  Border: 1px Stroke1          │
└───────────────────────────────┘
```

### 02.5 Opportunity Card (savings list item)

```
┌───────────────────────────────────────────────────────┐
│  ┌────┐  Right-size vm-prod-web-01        ┌────────┐ │
│  │ 💰 │  Aktuálně D4s → doporučení B2s   │ Provést│ │
│  └────┘  Kč 2 340/měs               │ │
│          ╭─────────────────╮              └────────┘ │
│          │ Úspora 68%      │                         │
│          ╰─────────────────╯                         │
│  bg: Background2, radius: 8px, padding: 16px         │
│  hover: translateY(-2px) + Level 2 shadow             │
└───────────────────────────────────────────────────────┘
```

### 02.6 Skeleton Loading States

**Varianty pro každou molekulu:**
- Widget Skeleton (card shell + 3 pulse bars)
- KPI Skeleton (card shell + circle + 2 bars)
- DataGrid Skeleton (header row + 5 body rows)
- Form Skeleton (3 label + input pairs)

```
┌───────────────────────────────┐
│  ████████████  ████           │  pulse animation
│                               │
│  ████████████████████         │  2s infinite
│  ████████████████             │  cubic-bezier(0.4, 0, 0.6, 1)
│  ██████████                   │
│                               │
│  bg: #E2E8F0 (or Background3) │
│  radius: 4px per bar          │
└───────────────────────────────┘
```

---

## 03 — Organisms

### 03.1 Sidebar Navigation

**States:** Expanded (260px) | Collapsed (60px) | Mobile overlay

```
Expanded (260px):
┌──────────────────────────┐
│  CloudInfraMap    [◁]    │  Logo + collapse toggle
├──────────────────────────┤
│                          │
│  PLATFORMA               │  Section header (Caption, uppercase)
│  🏠 Dashboard            │
│  📊 Executive            │
│  📈 Reports              │
│                          │
│  INFRASTRUKTURA          │
│  🗺️ Designer       🔒PRO │
│  📦 Import               │
│  ⚙️ Konfigurace          │
│                          │
│  SPRÁVA                  │
│  👤 Admin                │
│  🛡️ Compliance           │
│  🤖 AI Asistent          │
│                          │
├──────────────────────────┤
│  [?] Nápověda            │  Footer items
│  [⚙] Nastavení          │
└──────────────────────────┘

bg: Background1
border-right: 1px Stroke1
transition: width 200ms ease-in-out
```

### 03.2 TopNav Bar

```
┌────────────────────────────────────────────────────────────────────────┐
│  [☰]  CloudInfraMap  │  🔍 Hledat zdroje, služby...   │ 🔔 🌙 👤 ▾ │
│                      │                                 │  Org ▾      │
│  64px height         │  max-width: 600px               │  Quick links│
│  bg: rgba(20,20,20,0.8) + backdrop-filter: blur(20px)                │
│  position: sticky, z-index: 100                                      │
└────────────────────────────────────────────────────────────────────────┘
```

**Varianty:**
- Desktop: plný layout
- Mobile: hamburger menu, search schovaný, avatar menu

### 03.3 DataGrid (tabulka)

```
┌──────────────────────────────────────────────────────────┐
│  Název ▲      │ Provider  │ Region    │ Náklady  │ Stav │
├───────────────┼───────────┼───────────┼──────────┼──────┤
│  vm-prod-01   │ Azure     │ West EU   │ Kč 3 450 │ ● OK │  Zebra: BG1
│  vm-prod-02   │ AWS       │ eu-west-1 │ Kč 5 120 │ ● ⚠ │  Zebra: BG2
│  vm-prod-03   │ Azure     │ North EU  │ Kč 2 890 │ ● OK │  Zebra: BG1
│  ...          │           │           │          │      │
├───────────────┴───────────┴───────────┴──────────┴──────┤
│                              ◁ 1 2 3 4 5 ▷   10/strana │  Pagination
└──────────────────────────────────────────────────────────┘

Header: bg Background2, Title 3 weight 600
Rows: 40px min-height, Body 1
Hover row: Background2 (subtle highlight)
Sort icon: inline after header text
Pagination: right-aligned, Fluent components
```

### 03.4 Locked Feature Overlay

**3 preview varianty:**

```
BLUR (default):
┌───────────────────────────────┐
│  [Blurred content]            │  filter: blur(6px), opacity: 0.7
│      ┌────────────────┐       │
│      │  🔒             │       │
│      │  Funkce PRO     │       │  Overlay card: bg white 95%
│      │  Upgrade pro    │       │  shadow: Level 3
│      │  přístup        │       │  radius: 12px
│      │  [Upgradovat]   │       │  max-width: 360px
│      │  Náhled ❯       │       │
│      └────────────────┘       │
└───────────────────────────────┘

SKELETON:
┌───────────────────────────────┐
│  ████ ██████ ████████         │  opacity: 0.4
│  ████████████████████         │  + overlay card
│  ██████████                   │
└───────────────────────────────┘

SCREENSHOT:
┌───────────────────────────────┐
│  [Grayscale screenshot]       │  opacity: 0.6, grayscale: 30%
│      + overlay card           │  + overlay card
└───────────────────────────────┘
```

### 03.5 Hero Metrics Card (Dashboard)

```
┌──────────────────────────────────────────────────────────────────────┐
│                                                                      │
│  CELKOVÉ MĚSÍČNÍ NÁKLADY                                            │
│                                                                      │
│              Kč 1 234 567                                            │
│                                                                      │
│  ─────────────────────────────────────────────────────                │
│                                                                      │
│   Optimalizováno     Zdroje celkem     Úspora potenciál              │
│   ↑ 87%              1 247             Kč 234 567                    │
│                                                                      │
│  gradient: Brand50 → Brand20 (135deg)                                │
│  radius: 16px, padding: 32px                                         │
│  shadow: Level 2                                                     │
│  text: white                                                         │
│  grid: span 12 (full width)                                         │
└──────────────────────────────────────────────────────────────────────┘
```

### 03.6 AI Chat Panel

```
┌───────────────────────────────┐
│  🤖 AI Asistent         [✕]  │  Header
├───────────────────────────────┤
│                               │
│  ┌─────────────────────────┐  │  AI message (left-aligned)
│  │ Na základě analýzy...   │  │  bg: Background2, radius: 8px
│  └─────────────────────────┘  │
│                               │
│       ┌────────────────────┐  │  User message (right-aligned)
│       │ Jaké jsou úspory?  │  │  bg: Brand90, color: white
│       └────────────────────┘  │
│                               │
│  ┌─────────────────────────┐  │  AI typing indicator
│  │ ● ● ●                  │  │  3 pulsing dots
│  └─────────────────────────┘  │
│                               │
├───────────────────────────────┤
│  [📎] Zadejte zprávu... [➤]  │  Input area
└───────────────────────────────┘
```

### 03.7 Treemap Visualization

```
┌──────────────────────────────────────────────────────────┐
│  Widget Card Header: "Rozložení nákladů"     [⋮]        │
├──────────────────────────────────────────────────────────┤
│  ┌──────────────┬────────┬───────────────────────────┐   │
│  │              │        │                           │   │
│  │  Azure VMs   │ Storage│   AWS EC2                 │   │
│  │  Kč 450K     │ Kč 120K│   Kč 380K                │   │
│  │              │        │                           │   │
│  │              ├────────┤                           │   │
│  │              │ Network│                           │   │
│  │              │ Kč 80K │                           │   │
│  ├──────────────┴────────┴───┬───────────────────────┤   │
│  │  GCP Compute              │   Ostatní             │   │
│  │  Kč 190K                  │   Kč 45K              │   │
│  └───────────────────────────┴───────────────────────┘   │
│                                                          │
│  Barvy: Chart Palette (1–8)                              │
│  Hover: opacity 0.8, tooltip s detaily                   │
│  Responsive: min-height 350px na mobilu                   │
└──────────────────────────────────────────────────────────┘
```

---

## 04 — Speciální komponenty

### 04.1 Organization Switcher

```
┌───────────────────────────────┐
│  🏢 Acme Corp           ▾    │  Trigger (v TopNav)
├───────────────────────────────┤
│  🔍 Hledat organizaci...     │  Search
├───────────────────────────────┤
│  🏢 Acme Corp          ✓     │  Active (checkmark)
│  🏢 Beta Industries          │  Other orgs
│  🏢 Gamma Solutions          │
├───────────────────────────────┤
│  + Nová organizace            │  Action
└───────────────────────────────┘
```

### 04.2 Export Buttons

```
┌────────────────────────────────┐
│  [📄 PDF]  [📊 CSV]  [🖨 Tisk] │  Button group, outline variant
└────────────────────────────────┘
```

### 04.3 Feedback Widget (floating)

```
                                ┌──────────────────┐
                                │  💬 Zpětná vazba  │  Bottom-right
                                └──────────────────┘
                                        ↓
                          ┌──────────────────────────┐
                          │  Jak se vám líbí CIM?    │
                          │                          │
                          │  ★ ★ ★ ★ ☆              │
                          │                          │
                          │  ┌──────────────────────┐│
                          │  │ Komentář...          ││
                          │  └──────────────────────┘│
                          │           [Odeslat]      │
                          └──────────────────────────┘
```

### 04.4 Empty State

```
┌───────────────────────────────────────┐
│                                       │
│            ┌─────────┐               │
│            │  📦💨    │               │  Ilustrace (Fluent illustration nebo custom SVG)
│            └─────────┘               │
│                                       │
│     Zatím žádné zdroje                │  Title 2
│     Importujte svou infrastrukturu    │  Body 1, Foreground3
│     pro začátek.                      │
│                                       │
│         [+ Importovat]                │  Primary button
│                                       │
└───────────────────────────────────────┘
```

---

## Figma Deliverables Checklist

Pro Figma designer — vytvořit následující:

- [ ] **Color Styles** — všechny barvy z palety jako Figma Color Styles
- [ ] **Text Styles** — 7 úrovní typografie (Display → Caption) × 2 weights
- [ ] **Effect Styles** — 5 úrovní elevation jako Figma Effect Styles
- [ ] **Component Set: Buttons** — 5 variant × 3 sizes × 5 states × 4 icon options
- [ ] **Component Set: Inputs** — 5 types × 3 sizes × 5 states × 4 addons
- [ ] **Component Set: Badges** — Status (5 sentiments) + Tier (4 tiers)
- [ ] **Component Set: Nav Item** — 4 states × 2 modes × 4 badge options
- [ ] **Component Set: Cards** — KPI, Widget, Opportunity, Hero
- [ ] **Component Set: DataGrid** — Header + rows + pagination
- [ ] **Component Set: Skeleton** — Widget, KPI, Grid, Form varianty
- [ ] **Component: Sidebar** — Expanded + Collapsed + Mobile
- [ ] **Component: TopNav** — Desktop + Mobile
- [ ] **Component: Locked Feature** — Blur + Skeleton + Screenshot × Light/Dark
- [ ] **Component: AI Chat** — Messages + typing + input
- [ ] **Component: Treemap** — Placeholder s barevnými bloky
- [ ] **Component: Empty State** — S ilustrací + CTA

**Všechny komponenty musí mít light + dark mode variantu.**
