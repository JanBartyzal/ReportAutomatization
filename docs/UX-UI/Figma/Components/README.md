# Figma Component Assets - Clouply Design System
# Clouply — Complete Vector Component Library

This directory contains the 100% vector-based design system for **Clouply Suite** (CIM, Pulse, Guard, ArchDecide).

## Key Files

1.  **[tokens.json](tokens.json)**:
    - Standard design tokens for colors, typography, spacing, radius, and **elevation (shadows)**.
    - Updated to include Tier-based color semantic (Micro, Starter, Pro, Enterprise).

2.  **[atoms_complete.svg](atoms_complete.svg)**:
    - **Buttons**: 5 variants (Primary, Secondary, Outline, Subtle, Transparent) × 3 sizes × 5 states.
    - **Input Fields**: 5 types (Text, Password, Search, Number, Textarea) × 3 sizes + Error states.
    - **Avatars**: 4 sizes (S, M, L, XL) × states.
    - **Badges**: Pill and Rounded variants for Status and Tiers.

3.  **[molecules_complete.svg](molecules_complete.svg)**:
    - **KPI Cards**: Full trend matrix (Up, Down, Flat) + Compact variants.
    - **Navigation**: Sidebar item matrix with states (Hover, Locked, Collapsed).
    - **Loading**: Skeleton placeholders for cards, lists, and sidebars.
    - **Empty State**: Vector representation of no-data states.

4.  **[organisms_complete.svg](organisms_complete.svg)**:
    - **Navigation**: Responsive Sidebars (Expanded, Collapsed, Mobile) and TopNav.
    - **DataGrid**: Full table system with bulk toolbar and pagination.
    - **Treemap**: Flat D3-style cost visualization with tooltip.

5.  **[icons_grid.svg](icons_grid.svg)**:
    - Grid of 20+ specialized cloud infrastructure and navigation icons.

## How to Import into Figma
- **Direct Import**: Simply drag and drop the `.svg` files into your Figma canvas. They will appear as editable vector layers.
- **Tokens**: Use the **Tokens Studio for Figma** plugin to import `tokens.json`.

---
*Unified Branding: Clouply Suite*

## Steps for the Designer

1.  Create a new Figma file "Clouply Design System".
2.  Import `tokens.json` using Tokens Studio.
3.  Build the atomic components (Buttons, Inputs) following the `03-figma-components.md` spec and using the visual masters for guidance.
4.  Assemble molecular and organic components.
5.  Validate designs against the page briefs in `04-figma-pages.md`.

---
*Prepared by Antigravity AI - March 2026*
