# 00 — Projektový vzorník barev (ReportAutomatization)

> **Verze:** 1.0
> **Datum:** 2026-03-12
> **Základ:** Design system převzatý z CIM projektu (`02-design-system.md`)
> **Důležité:** Tento soubor definuje barevné ODCHYLKY od CIM. Vše ostatní (layout, typografie, spacing, elevace, motion, formuláře, a11y, dark mode) platí beze změn z `02-design-system.md`.

---

## 1. Vztah k CIM design systému

Design system (`02-design-system.md`, `03-figma-components.md`, `04-figma-pages.md`) byl převzat z projektu CIM (Cloud Infrastructure Management). Pro ReportAutomatization platí:

| Oblast | Převzato z CIM beze změn? | Poznámka |
|--------|---------------------------|----------|
| Layout & Grid | ANO | 12-column, 1400px max, breakpoints |
| Typografie | ANO | Inter font, 7-level scale |
| Spacing (8px grid) | ANO | Celý spacing systém |
| Elevace / Shadows | ANO | 5 úrovní |
| Border Radius | ANO | 6 variant |
| Motion & Animace | ANO | Timing, easing, pravidla |
| Formuláře | ANO | Field structure, stavy, layout |
| Data Display (DataGrid, KPI) | ANO | Struktura komponent |
| Dark Mode systém | ANO | FluentProvider, CSS variables |
| A11y pravidla | ANO | WCAG 2.1 AA |
| Glassmorphism pravidla | ANO | Kde ano/ne, hodnoty |
| Anti-patterns | ANO | Zakázané přístupy |
| **Brand Palette** | **NE — viz sekce 2** | Jiná primární barva |
| **Sémantické barvy** | **NE — viz sekce 3** | Mírné úpravy |
| **Chart Palette** | **NE — viz sekce 4** | Odvozeno z nové brand |
| **Tier barvy** | **NE — viz sekce 5** | Jiný kontext (ne cloud tiers) |

---

## 2. Brand Palette

CIM používá Azure Blue `#0078D4`. ReportAutomatization má vlastní identitu — Crimson Red.

**Crimson Red** (reporty, důležitost, profesionalita):

Primární brand barva: **Crimson `#C4314B`** (slot 90)

```
Brand 10:  #0D0206    ██  Darkest
Brand 20:  #23080F    ██
Brand 30:  #3A0D19    ██
Brand 40:  #521324    ██  Dark UI elements
Brand 50:  #6B1830    ██
Brand 60:  #841E3C    ██
Brand 70:  #9D2444    ██  Hover states
Brand 80:  #B32A48    ██  Active states
Brand 90:  #C4314B    ██  PRIMARY — buttons, links, focus rings
Brand 100: #CF4D62    ██  Light mode foreground on dark bg
Brand 110: #D96A79    ██
Brand 120: #E38790    ██
Brand 130: #EDA4A9    ██  Dark mode foreground
Brand 140: #F5BFC2    ██
Brand 150: #F9D8DA    ██
Brand 160: #FDF0F1    ██  Lightest tint
```

---

## 3. Sémantické barvy

Sémantické barvy zůstávají funkčně stejné (Success = zelená, Danger = červená atd.), ale primární Info barva se mění na novou brand:

| Sémantika | Light foreground | Light bg | Dark foreground | Dark bg | Změna vs CIM |
|-----------|-----------------|---------|----------------|---------|--------------|
| **Success** | `#107C10` | `#DFF6DD` | `#54B054` | `#052505` | Beze změny |
| **Danger** | `#D13438` | `#FDE7E9` | `#F1707B` | `#3D1314` | Beze změny |
| **Warning** | `#D83B01` | `#FDEBE2` | `#FCE100` | `#4A1504` | Beze změny |
| **Info** | `#C4314B` | `#FDF0F1` | `#CF4D62` | `#0D0206` | **Změna** — nová brand |

---

## 4. Chart Palette

8 barev pro datovou vizualizaci, odvozeno z nové brand palety:

```
Chart 1:  #C4314B  ██  Crimson (primary series)
Chart 2:  #107C10  ██  Green (success/savings)
Chart 3:  #0078D4  ██  Azure Blue (category 3)
Chart 4:  #D83B01  ██  Orange (warning)
Chart 5:  #6D28D9  ██  Purple (category 5)
Chart 6:  #008272  ██  Teal (category 6)
Chart 7:  #E3008C  ██  Magenta (category 7)
Chart 8:  #986F0B  ██  Gold (category 8)
```

Hero gradient:
```
Hero gradient:    linear-gradient(135deg, Brand50 0%, Brand20 100%)
                  = linear-gradient(135deg, #6B1830 0%, #23080F 100%)
```

---

## 5. Kontextové barvy (namísto CIM Tier barev)

ReportAutomatization nepoužívá cloud tiers. Místo toho definujeme barvy pro reporting stavy:

| Stav | Badge bg | Badge text | Použití |
|------|---------|-----------|------|
| **DRAFT** | `#F3F2F1` | `#616161` | Koncept, rozpracovaný report |
| **SUBMITTED** | `#FEF3C7` | `#92400E` | Odesláno ke schválení |
| **IN_REVIEW** | `#E0F2FE` | `#0369A1` | Probíhá kontrola |
| **APPROVED** | `#DFF6DD` | `#107C10` | Schváleno |
| **REJECTED** | `#FDE7E9` | `#D13438` | Zamítnuto, vráceno k přepracování |
| **OVERDUE** | `#FDEBE2` | `#D83B01` | Po termínu |

---

## 6. Implementace v kódu

### 6.1 Fluent Theme Override

```typescript
// apps/frontend/src/theme/brandTokens.ts
import { BrandVariants, createLightTheme, createDarkTheme } from '@fluentui/react-components';

const reportBrand: BrandVariants = {
  10: '#0D0206',
  20: '#23080F',
  30: '#3A0D19',
  40: '#521324',
  50: '#6B1830',
  60: '#841E3C',
  70: '#9D2444',
  80: '#B32A48',
  90: '#C4314B',
  100: '#CF4D62',
  110: '#D96A79',
  120: '#E38790',
  130: '#EDA4A9',
  140: '#F5BFC2',
  150: '#F9D8DA',
  160: '#FDF0F1',
};

export const lightTheme = createLightTheme(reportBrand);
export const darkTheme = createDarkTheme(reportBrand);
```

### 6.2 Chart Palette CSS Custom Properties

```css
:root {
  --chart-1: #C4314B;
  --chart-2: #107C10;
  --chart-3: #0078D4;
  --chart-4: #D83B01;
  --chart-5: #6D28D9;
  --chart-6: #008272;
  --chart-7: #E3008C;
  --chart-8: #986F0B;
}
```

### 6.3 Status Badge Mapping

```typescript
// apps/frontend/src/theme/statusColors.ts
export const STATUS_COLORS = {
  DRAFT:      { bg: '#F3F2F1', text: '#616161' },
  SUBMITTED:  { bg: '#FEF3C7', text: '#92400E' },
  IN_REVIEW:  { bg: '#E0F2FE', text: '#0369A1' },
  APPROVED:   { bg: '#DFF6DD', text: '#107C10' },
  REJECTED:   { bg: '#FDE7E9', text: '#D13438' },
  OVERDUE:    { bg: '#FDEBE2', text: '#D83B01' },
} as const;
```

---

## 7. Kontrolní checklist pro implementaci

Při implementaci každé frontend úlohy ověřit:

- [ ] Žádné hardcoded barvy — vše přes Fluent tokeny nebo CSS custom properties z tohoto vzorníku
- [ ] Brand barva odpovídá Crimson `#C4314B`, ne Azure Blue
- [ ] Chart palette používá projektové barvy (sekce 4)
- [ ] Status badges používají kontextové barvy (sekce 5)
- [ ] Info sémantická barva = brand (ne Azure Blue)
- [ ] Dark mode funguje automaticky (Fluent theme toggle)
- [ ] Kontrast ověřen (WCAG 2.1 AA: 4.5:1 text, 3:1 interaktivní)
