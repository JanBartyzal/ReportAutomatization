# 04 — Figma Brief: Stránky & Layouty

> Specifikace pro vytvoření Figma page-level designů.
> Každá sekce = 1–3 Figma frames (Desktop + Tablet + Mobile).
> Breakpointy: Desktop 1440px, Tablet 768px, Mobile 375px.

---

## Obecné principy layoutu

### App Shell

```
Desktop (1440px):
┌────────────────────────────────────────────────────────────────────────────┐
│                           TopNav (64px)                                    │
├──────────────┬─────────────────────────────────────────────────────────────┤
│              │                                                             │
│  Sidebar     │              Main Content Area                              │
│  (260px)     │              max-width: 1400px, centered                    │
│              │              padding: 24px                                  │
│              │                                                             │
│              │              Scroll: vertical only                          │
│              │                                                             │
└──────────────┴─────────────────────────────────────────────────────────────┘

Tablet (768px):
┌────────────────────────────────────────────┐
│              TopNav (64px)                 │
├──────┬─────────────────────────────────────┤
│  60  │        Main Content Area            │  Sidebar collapsed (60px)
│  px  │        padding: 16px                │
│      │                                     │
└──────┴─────────────────────────────────────┘

Mobile (375px):
┌─────────────────────┐
│    TopNav (64px)     │
├─────────────────────┤
│                     │  Sidebar: hidden (hamburger toggle)
│  Main Content       │  padding: 12px
│  full-width         │
│                     │
└─────────────────────┘
```

---

## Page 01: Dashboard (hlavní stránka)

### Desktop (1440px)

```
┌────────────────────────────────────────────────────────────────┐
│  [Shell: TopNav + Sidebar]                                    │
│                                                                │
│  ┌──────────────────────────────────────────────────────────┐  │
│  │                                                          │  │
│  │              HERO METRICS CARD (span 12)                 │  │
│  │     CELKOVÉ MĚSÍČNÍ NÁKLADY: Kč 1 234 567              │  │
│  │     Optimalizováno 87%  │  Zdroje 1247  │  Úspora 234K │  │
│  │                                                          │  │
│  └──────────────────────────────────────────────────────────┘  │
│                                                                │
│  ┌────────────────────────┐  ┌────────────────────────────┐   │
│  │                        │  │                            │   │
│  │  SAVINGS OPPORTUNITIES │  │  GEO ARBITRAGE             │   │
│  │  (span 6)              │  │  (span 6)                  │   │
│  │                        │  │                            │   │
│  │  • Opportunity 1       │  │  Current: West EU Kč 5200  │   │
│  │  • Opportunity 2       │  │  Cheapest: North EU Kč 3800│   │
│  │  • Opportunity 3       │  │  Savings: 27%              │   │
│  │                        │  │                            │   │
│  └────────────────────────┘  └────────────────────────────┘   │
│                                                                │
│  ┌──────────────┐  ┌──────────────┐  ┌───────────────────┐   │
│  │ ACCURACY     │  │ CARBON       │  │ ANNUAL PROJECTION │   │
│  │ BADGE        │  │ FOOTPRINT    │  │                   │   │
│  │ (span 4)     │  │ (span 4)     │  │ (span 4)          │   │
│  │              │  │              │  │                   │   │
│  │  92% ★★★★   │  │  Rating B    │  │  Kč 14.8M        │   │
│  │              │  │  1.2t CO₂    │  │  ──────────       │   │
│  │              │  │              │  │  IaaS  8.2M       │   │
│  │              │  │              │  │  PaaS  4.1M       │   │
│  │              │  │              │  │  SaaS  2.5M       │   │
│  └──────────────┘  └──────────────┘  └───────────────────┘   │
│                                                                │
│  ┌──────────────────────────────────────────────────────────┐  │
│  │                                                          │  │
│  │              COST TREEMAP (span 12)                      │  │
│  │              [ D3 treemap vizualizace ]                   │  │
│  │                                                          │  │
│  └──────────────────────────────────────────────────────────┘  │
│                                                                │
└────────────────────────────────────────────────────────────────┘
```

### Tablet (768px)

```
┌────────────────────────────────────────┐
│  Hero card (span 12 → stacked metrics) │
├────────────────────────────────────────┤
│  Savings (span 12)                     │
├────────────────────────────────────────┤
│  Geo Arbitrage (span 12)              │
├────────────┬───────────────────────────┤
│  Accuracy  │  Carbon (span 6 each)    │
│  (span 6)  │                          │
├────────────┴───────────────────────────┤
│  Annual Projection (span 12)          │
├────────────────────────────────────────┤
│  Treemap (span 12, height 350px)      │
└────────────────────────────────────────┘
```

### Mobile (375px)

```
┌───────────────────┐
│  Hero (stacked)   │  cost-value: 2.5rem
├───────────────────┤
│  Savings          │  Each card full width
├───────────────────┤
│  Geo Arbitrage    │
├───────────────────┤
│  Accuracy         │
├───────────────────┤
│  Carbon           │
├───────────────────┤
│  Projection       │
├───────────────────┤
│  Treemap (350px)  │
└───────────────────┘
```

---

## Page 02: Executive Overview

### Desktop

```
┌────────────────────────────────────────────────────────────────┐
│  Title 1: "Executive přehled"                                  │
│                                                                │
│  ┌──────────┐ ┌──────────┐ ┌──────────┐ ┌──────────┐         │
│  │ KPI Card │ │ KPI Card │ │ KPI Card │ │ KPI Card │         │
│  │ Náklady  │ │ Úspora   │ │ Zdroje   │ │ Compliance│        │
│  │ Kč 1.2M  │ │ ↑ 18%    │ │ 1 247    │ │ 94%      │         │
│  │ ↑ +12%   │ │ ↑ +5pp   │ │ ↓ -23    │ │ ↑ +2pp   │         │
│  └──────────┘ └──────────┘ └──────────┘ └──────────┘         │
│                                                                │
│  ┌──────────────────────────┐ ┌───────────────────────────┐   │
│  │                          │ │                           │   │
│  │  COST TREND (Recharts)   │ │  TOP 10 SERVICES          │   │
│  │  LineChart 6 měsíců      │ │  DataGrid (compact)       │   │
│  │  (span 7)                │ │  (span 5)                 │   │
│  │                          │ │                           │   │
│  └──────────────────────────┘ └───────────────────────────┘   │
│                                                                │
│  ┌──────────────────────────────────────────────────────────┐  │
│  │  RECOMMENDATIONS                                         │  │
│  │  3 × Opportunity Card (horizontal list)                  │  │
│  └──────────────────────────────────────────────────────────┘  │
│                                                                │
└────────────────────────────────────────────────────────────────┘
```

---

## Page 03: Resource List (DataGrid page)

### Desktop

```
┌────────────────────────────────────────────────────────────────┐
│  Title 1: "Zdroje"                                             │
│                                                                │
│  ┌─────────────────────────────────────────────────────────┐   │
│  │  Filters Bar:                                           │   │
│  │  [Provider ▾] [Region ▾] [Tag ▾] [Stav ▾]  🔍 Hledat  │   │
│  │                                                         │   │
│  │  Active filters: [Azure ✕] [West EU ✕]                 │   │
│  └─────────────────────────────────────────────────────────┘   │
│                                                                │
│  ┌─────────────────────────────────────────────────────────┐   │
│  │  Toolbar:                                               │   │
│  │  [+ Přidat] [📥 Import]        [📄 PDF] [📊 CSV]      │   │
│  │  Vybráno: 3 zdroje  [🗑 Smazat] [📋 Tag]              │   │
│  └─────────────────────────────────────────────────────────┘   │
│                                                                │
│  ┌──────────────────────────────────────────────────────────┐  │
│  │  DataGrid                                                │  │
│  │  ┌────────┬──────────┬────────┬──────────┬──────┬──────┐│  │
│  │  │ ☐ Name │ Provider │ Region │ Cost/mo  │ CPU% │ Stav ││  │
│  │  ├────────┼──────────┼────────┼──────────┼──────┼──────┤│  │
│  │  │ ☐ vm-1 │ Azure    │ WestEU │ Kč 3,450 │  12% │ ● OK ││  │
│  │  │ ☐ vm-2 │ AWS      │ eu-w-1 │ Kč 5,120 │  78% │ ● ⚠ ││  │
│  │  │ ☐ vm-3 │ Azure    │ NorthEU│ Kč 2,890 │   3% │ ● OK ││  │
│  │  │ ...    │          │        │          │      │      ││  │
│  │  ├────────┴──────────┴────────┴──────────┴──────┴──────┤│  │
│  │  │                           ◁ 1 2 3 ... 24 ▷  25/str ││  │
│  │  └─────────────────────────────────────────────────────┘│  │
│  └──────────────────────────────────────────────────────────┘  │
│                                                                │
│  [Detail Drawer] →  ContextualSidebar (400px) slides in       │
│                     from right on row click                    │
│                                                                │
└────────────────────────────────────────────────────────────────┘
```

### Resource Detail Drawer

```
┌──────────────────────────────────┐
│  vm-prod-web-01           [✕]   │  Sticky header
├──────────────────────────────────┤
│                                  │
│  Provider: Azure                 │  Key-value pairs
│  Region:   West Europe           │
│  Type:     Standard_D4s_v3       │
│  Status:   ● Running             │
│                                  │
│  ── Náklady ──────────────────   │  Section
│  Měsíční: Kč 3,450              │
│  Trend:   ↑ +8% vs. min. měsíc  │
│  [sparkline]                     │
│                                  │
│  ── Využití ──────────────────   │  Section
│  CPU:     12% avg                │
│  Memory:  45% avg                │
│  Disk:    23% used               │
│                                  │
│  ── Doporučení ───────────────   │  Section
│  ⚡ Right-size na B2s            │
│     Úspora: Kč 2,340/měsíc      │
│     [Aplikovat]                  │
│                                  │
│  ── Tagy ─────────────────────   │  Section
│  [env:prod] [team:web] [+]      │
│                                  │
└──────────────────────────────────┘

Width: 400px
Shadow: Level 3
Slide-in: 300ms ease-in-out from right
Overlay: none (pushes content)
```

---

## Page 04: Compliance Dashboard

### Desktop

```
┌────────────────────────────────────────────────────────────────┐
│  Title 1: "Compliance"                                         │
│                                                                │
│  ┌──────────┐ ┌──────────┐ ┌──────────┐                      │
│  │ KPI      │ │ KPI      │ │ KPI      │                      │
│  │ Score    │ │ Controls │ │ Findings │                      │
│  │ 94%      │ │ 127/135  │ │ 8 open   │                      │
│  └──────────┘ └──────────┘ └──────────┘                      │
│                                                                │
│  ┌────────────────────────────────────┐ ┌────────────────────┐ │
│  │                                    │ │                    │ │
│  │  COMPLIANCE BY FRAMEWORK          │ │  RECENT FINDINGS   │ │
│  │                                    │ │                    │ │
│  │  ISO 27001  ████████████░░ 92%    │ │  • Finding 1 [!]   │ │
│  │  SOC 2      █████████████░ 96%    │ │  • Finding 2 [⚠]   │ │
│  │  GDPR       ██████████░░░░ 78%    │ │  • Finding 3 [i]   │ │
│  │  CIS        ████████████░░ 91%    │ │  • Finding 4 [!]   │ │
│  │  Guard      ███████░░░░░░░ 54%    │ │                    │ │
│  │  (span 7)                          │ │  (span 5)          │ │
│  └────────────────────────────────────┘ └────────────────────┘ │
│                                                                │
│  ┌──────────────────────────────────────────────────────────┐  │
│  │  CONTROLS TABLE (DataGrid)                               │  │
│  │  [Framework ▾] [Status ▾]                                │  │
│  │                                                          │  │
│  │  Control ID │ Description │ Framework │ Status │ Updated │  │
│  │  ...        │             │           │        │         │  │
│  └──────────────────────────────────────────────────────────┘  │
│                                                                │
└────────────────────────────────────────────────────────────────┘
```

---

## Page 05: Infrastructure Designer

### Desktop

```
┌────────────────────────────────────────────────────────────────┐
│  Title 1: "Infrastructure Designer"                            │
│                                                                │
│  ┌─────────────────────────────────────────────────────────┐   │
│  │  Toolbar:                                               │   │
│  │  [+ VM] [+ Storage] [+ DB] [+ Network] │ [⤴ Undo]     │   │
│  │  [+ Container] [+ LB]                  │ [⤵ Redo]     │   │
│  │                                         │ [💾 Uložit]  │   │
│  └─────────────────────────────────────────────────────────┘   │
│                                                                │
│  ┌──────────────────────────────────────────────────────────┐  │
│  │                                                          │  │
│  │              XY Flow Canvas (full width)                 │  │
│  │                                                          │  │
│  │    ┌──────────┐         ┌──────────┐                    │  │
│  │    │ 🖥 VM    │─────────│ 📦 Storage│                    │  │
│  │    │ prod-web │         │ blob-01  │                    │  │
│  │    └──────────┘         └──────────┘                    │  │
│  │         │                                               │  │
│  │         │                                               │  │
│  │    ┌──────────┐         ┌──────────┐                    │  │
│  │    │ 🗄 DB    │─────────│ ⚡ Redis  │                    │  │
│  │    │ pg-main  │         │ cache-01 │                    │  │
│  │    └──────────┘         └──────────┘                    │  │
│  │                                                          │  │
│  │  Controls: Pan, Zoom, Minimap                           │  │
│  └──────────────────────────────────────────────────────────┘  │
│                                                                │
│  [Properties Panel] →  Right sidebar (320px) on node select   │
│                                                                │
└────────────────────────────────────────────────────────────────┘
```

**Designer Node Design:**

```
┌──────────────────┐
│  🖥  VM           │  32px icon, Title 3
│  prod-web-01      │  Body 2, Foreground3
│  D4s v3 · West EU │  Body 2, Foreground3
│  Kč 3,450/mo      │  Body 1, weight 600
│  ● Running        │  Status badge
├──────────────────┤
│  ○ Input   Output ○│  XY Flow Handles
└──────────────────┘

bg: Background1
border: 1px Stroke1
radius: 8px
shadow: Level 1
selected: border 2px Brand90 + Level 2
```

---

## Page 06: Admin Panel

### Desktop

```
┌────────────────────────────────────────────────────────────────┐
│  Title 1: "Administrace"                                       │
│                                                                │
│  ┌────────────────────────────────┐                            │
│  │  Tab Navigation:               │                            │
│  │  [Uživatelé] [Role] [API klíče] [Audit log] [Nastavení]   │
│  └────────────────────────────────┘                            │
│                                                                │
│  Tab: Uživatelé                                                │
│  ┌─────────────────────────────────────────────────────────┐   │
│  │  [+ Pozvat uživatele]                    🔍 Hledat      │   │
│  │                                                         │   │
│  │  ┌──────────────────────────────────────────────────┐   │   │
│  │  │ Avatar │ Jméno  │ Email  │ Role  │ Stav │ Akce  │   │   │
│  │  ├────────┼────────┼────────┼───────┼──────┼───────┤   │   │
│  │  │  JB    │ Jan B. │ jan@.. │ Admin │ ● On │ ⋮     │   │   │
│  │  │  PK    │ Petr K.│ petr@..│ User  │ ● Off│ ⋮     │   │   │
│  │  └──────────────────────────────────────────────────┘   │   │
│  └─────────────────────────────────────────────────────────┘   │
│                                                                │
└────────────────────────────────────────────────────────────────┘
```

---

## Page 07: Onboarding Flow

### Desktop — Stepper Layout

```
┌────────────────────────────────────────────────────────────────┐
│  Title 1: "Nastavení CIM"                                      │
│                                                                │
│  ○─────●─────○─────○─────○                                     │
│  Org   Cloud  Import Pravidla Hotovo                           │
│        ^active                                                  │
│                                                                │
│  ┌──────────────────────────────────────────────────────────┐  │
│  │                                                          │  │
│  │  Krok 2: Připojení cloud účtů                           │  │
│  │                                                          │  │
│  │  ┌────────────────┐  ┌────────────────┐                 │  │
│  │  │  ☁️ Azure       │  │  ☁️ AWS         │                 │  │
│  │  │                │  │                │                 │  │
│  │  │  Subscription  │  │  Account ID    │                 │  │
│  │  │  [___________] │  │  [___________] │                 │  │
│  │  │                │  │                │                 │  │
│  │  │  Tenant ID     │  │  Access Key    │                 │  │
│  │  │  [___________] │  │  [___________] │                 │  │
│  │  │                │  │                │                 │  │
│  │  │  [✓ Ověřeno]   │  │  [Ověřit]      │                 │  │
│  │  └────────────────┘  └────────────────┘                 │  │
│  │                                                          │  │
│  │  ┌────────────────┐                                     │  │
│  │  │  ☁️ GCP         │                                     │  │
│  │  │  [+ Přidat]    │                                     │  │
│  │  └────────────────┘                                     │  │
│  │                                                          │  │
│  │                       [◁ Zpět]  [Pokračovat ▷]          │  │
│  └──────────────────────────────────────────────────────────┘  │
│                                                                │
└────────────────────────────────────────────────────────────────┘
```

---

## Page 08: Reports & Export

### Desktop

```
┌────────────────────────────────────────────────────────────────┐
│  Title 1: "Reporty"                                            │
│                                                                │
│  ┌─────────────────────────────────────────────────────────┐   │
│  │  Report Type:  [Nákladový přehled ▾]                    │   │
│  │  Období:       [📅 1.2.2026] — [📅 28.2.2026]          │   │
│  │  Filtr:        [Všechny providery ▾] [Všechny regiony ▾]│   │
│  │                                                 [Vytvořit]│  │
│  └─────────────────────────────────────────────────────────┘   │
│                                                                │
│  ┌──────────────────────────────────────────────────────────┐  │
│  │  REPORT PREVIEW                                          │  │
│  │                                                          │  │
│  │  ┌────────────────────────────────────────────────────┐  │  │
│  │  │  Report header with logo + date range              │  │  │
│  │  │                                                    │  │  │
│  │  │  KPI Summary (4 × inline KPI cards)               │  │  │
│  │  │                                                    │  │  │
│  │  │  Chart: Cost Trend                                │  │  │
│  │  │  Table: Top 20 services by cost                   │  │  │
│  │  │                                                    │  │  │
│  │  │  Chart: Provider breakdown (pie)                  │  │  │
│  │  │  Table: Recommendations                           │  │  │
│  │  └────────────────────────────────────────────────────┘  │  │
│  │                                                          │  │
│  │  [📄 PDF]  [📊 CSV]  [🖨 Tisk]                          │  │
│  └──────────────────────────────────────────────────────────┘  │
│                                                                │
│  ┌──────────────────────────────────────────────────────────┐  │
│  │  SAVED REPORTS (DataGrid)                                │  │
│  │  Název │ Typ │ Vytvořeno │ Autor │ Velikost │ Akce      │  │
│  │  ...   │     │           │       │          │ [📥] [🗑] │  │
│  └──────────────────────────────────────────────────────────┘  │
│                                                                │
└────────────────────────────────────────────────────────────────┘
```

---

## Page 09: AI Assistant (fullscreen variant)

### Desktop

```
┌────────────────────────────────────────────────────────────────┐
│  Title 1: "AI Asistent"                                        │
│                                                                │
│  ┌────────────────────────────┐ ┌────────────────────────────┐ │
│  │                            │ │                            │ │
│  │  CHAT PANEL (span 7)      │ │  CONTEXT PANEL (span 5)    │ │
│  │                            │ │                            │ │
│  │  🤖 Analyzoval jsem vaši  │ │  Vybraný zdroj:            │ │
│  │  infrastrukturu. Nalezl   │ │  vm-prod-web-01            │ │
│  │  jsem 5 optimalizačních   │ │                            │ │
│  │  příležitostí:            │ │  ── Metriky ──             │ │
│  │                            │ │  CPU: 12% avg              │ │
│  │  1. Right-size vm-prod-01 │ │  Memory: 45%               │ │
│  │  2. Reserved Instance...  │ │  Cost: Kč 3,450            │ │
│  │  3. Idle resource...      │ │                            │ │
│  │                            │ │  ── Doporučení ──         │ │
│  │  👤 Ukaž detail #1       │ │  Right-size: B2s           │ │
│  │                            │ │  Savings: Kč 2,340        │ │
│  │  🤖 Right-sizing pro     │ │                            │ │
│  │  vm-prod-web-01:          │ │  [Aplikovat doporučení]   │ │
│  │  ...                      │ │                            │ │
│  │                            │ │                            │ │
│  ├────────────────────────────┤ │                            │ │
│  │  [📎] Zeptejte se...  [➤] │ │                            │ │
│  └────────────────────────────┘ └────────────────────────────┘ │
│                                                                │
└────────────────────────────────────────────────────────────────┘
```

---

## Page 10: Login / Register

### Desktop (centered card)

```
┌────────────────────────────────────────────────────────────────┐
│                                                                │
│  bg: gradient Brand50 → Brand20                                │
│                                                                │
│                 ┌──────────────────────────┐                   │
│                 │                          │                   │
│                 │  CloudInfraMap           │  Logo
│                 │                          │                   │
│                 │  Email                   │                   │
│                 │  [___________________]   │                   │
│                 │                          │                   │
│                 │  Heslo                   │                   │
│                 │  [___________________]   │                   │
│                 │                          │                   │
│                 │  [Přihlásit se       ]   │  Primary btn
│                 │                          │                   │
│                 │  Zapomenuté heslo?       │  Link
│                 │  Nemáte účet? Registrace │  Link
│                 │                          │                   │
│                 └──────────────────────────┘                   │
│                                                                │
│  Card: bg Background1, radius: 16px, shadow: Level 4          │
│  Max-width: 420px, padding: 40px                               │
│                                                                │
└────────────────────────────────────────────────────────────────┘
```

---

## Page 11: Pricing / Plans

### Desktop

```
┌────────────────────────────────────────────────────────────────┐
│  Title 1: "Cenové plány"                                       │
│  Body 1: "Vyberte plán, který odpovídá vašim potřebám"        │
│                                                                │
│  [Měsíčně] [Ročně (-20%)]   ← Toggle                          │
│                                                                │
│  ┌──────────┐ ┌──────────┐ ┌──────────┐ ┌──────────┐         │
│  │  MICRO   │ │  STARTER │ │  PRO ★   │ │ENTERPRISE│         │
│  │          │ │          │ │ Popular  │ │          │         │
│  │  Zdarma  │ │ Kč 990   │ │ Kč 4 990│ │ Na míru  │         │
│  │          │ │ /měsíc   │ │ /měsíc  │ │          │         │
│  │  ──────  │ │  ──────  │ │  ──────  │ │  ──────  │         │
│  │  ✓ 10 res│ │  ✓ 100   │ │  ✓ ∞    │ │  ✓ ∞     │         │
│  │  ✓ 1 user│ │  ✓ 5     │ │  ✓ 25   │ │  ✓ ∞     │         │
│  │  ✓ Basic │ │  ✓ Full  │ │  ✓ Full │ │  ✓ Custom │         │
│  │  ✗ AI   │ │  ✓ AI    │ │  ✓ AI+  │ │  ✓ AI++  │         │
│  │          │ │          │ │          │ │          │         │
│  │ [Začít]  │ │[Vyzkoušet│ │ [Koupit]│ │[Kontakt] │         │
│  │          │ │  14 dní] │ │          │ │          │         │
│  └──────────┘ └──────────┘ └──────────┘ └──────────┘         │
│                                                                │
│  Pro plan: border 2px Brand90, shadow Level 2                  │
│  Others: border 1px Stroke1                                    │
│                                                                │
└────────────────────────────────────────────────────────────────┘
```

---

## Page 12: Import Wizard

### Desktop — Multi-step wizard

```
┌────────────────────────────────────────────────────────────────┐
│  Title 1: "Import infrastruktury"                              │
│                                                                │
│  ○─────●─────○─────○                                           │
│  Zdroj  Mapování  Validace  Import                             │
│         ^active                                                 │
│                                                                │
│  ┌──────────────────────────────────────────────────────────┐  │
│  │                                                          │  │
│  │  Mapování sloupců                                       │  │
│  │                                                          │  │
│  │  ┌────────────────────┐    ┌────────────────────┐       │  │
│  │  │  CSV sloupec       │ →  │  CIM pole          │       │  │
│  │  ├────────────────────┤    ├────────────────────┤       │  │
│  │  │  "server_name"     │ →  │  [Název zdroje  ▾] │       │  │
│  │  │  "monthly_cost"    │ →  │  [Měsíční náklad▾] │       │  │
│  │  │  "region"          │ →  │  [Region        ▾] │       │  │
│  │  │  "cpu_cores"       │ →  │  [CPU jádra     ▾] │       │  │
│  │  │  "unused_col"      │ →  │  [Přeskočit     ▾] │       │  │
│  │  └────────────────────┘    └────────────────────┘       │  │
│  │                                                          │  │
│  │  Preview: 5 řádků z CSV                                 │  │
│  │  ┌───────────────────────────────────────────────────┐  │  │
│  │  │ server_name │ monthly_cost │ region   │ cpu_cores │  │  │
│  │  │ vm-prod-01  │ 3450         │ westeu   │ 4         │  │  │
│  │  │ ...         │              │          │           │  │  │
│  │  └───────────────────────────────────────────────────┘  │  │
│  │                                                          │  │
│  │                       [◁ Zpět]  [Pokračovat ▷]          │  │
│  └──────────────────────────────────────────────────────────┘  │
│                                                                │
└────────────────────────────────────────────────────────────────┘
```

---

## Figma Page Deliverables Checklist

- [ ] **App Shell** — Desktop + Tablet + Mobile (3 frames)
- [ ] **Dashboard** — Desktop + Tablet + Mobile (3 frames, light + dark = 6)
- [ ] **Executive Overview** — Desktop (1 frame, light + dark = 2)
- [ ] **Resource List** — Desktop + Detail Drawer open (2 frames)
- [ ] **Resource List Empty State** — Desktop (1 frame)
- [ ] **Compliance Dashboard** — Desktop (1 frame)
- [ ] **Infrastructure Designer** — Desktop + Properties panel (2 frames)
- [ ] **Admin Panel** — Desktop, tab: Uživatelé (1 frame)
- [ ] **Onboarding Flow** — Desktop, each step (5 frames)
- [ ] **Reports** — Desktop (1 frame)
- [ ] **AI Assistant** — Desktop, fullscreen + floating panel (2 frames)
- [ ] **Login / Register** — Desktop + Mobile (2 frames each = 4)
- [ ] **Pricing Plans** — Desktop (1 frame)
- [ ] **Import Wizard** — Desktop, each step (4 frames)
- [ ] **Locked Feature** — Overlay na Dashboard (1 frame, all 3 preview modes)
- [ ] **Error States** — 404, 500, Offline (3 frames)

**Celkem: ~35–40 Figma frames**

Všechny v light + dark mode variantě = **~70–80 frames** pro kompletní coverage.

---

## Prioritizace pro Figma

### P0 — Must have (review blocker)
1. Dashboard (Desktop, light + dark)
2. Resource List + Detail Drawer
3. App Shell (Desktop layout)
4. Design System foundations (barvy, typografie, spacing)

### P1 — High priority
5. Login / Register
6. Admin Panel
7. Compliance Dashboard
8. KPI Card + Widget Card components

### P2 — Medium priority
9. Infrastructure Designer
10. Import Wizard
11. Reports
12. Onboarding Flow

### P3 — Nice to have
13. AI Assistant fullscreen
14. Pricing Plans
15. Error states
16. Mobile layouts
