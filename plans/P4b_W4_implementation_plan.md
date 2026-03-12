# P4b-W4 Implementation Plan: Frontend Generator UI

**Phase:** P4b – PPTX Report Generation
**Complexity:** Frontend
**Total Effort:** ~8 MD

---

## Overview

This plan covers the frontend implementation for:
- **P4b-W4-001**: Template Management UI
- **P4b-W4-002**: Report Generation UI

---

## UX/UI Design System Requirements

Per project requirements:
- **Brand Color:** Crimson `#C4314B`
- **Styling:** Fluent UI `makeStyles` + tokens
- **Dark mode:** Via `FluentProvider` theme
- **A11y:** WCAG 2.1 AA minimum

### Specific Requirements for P4b-W4:
- **Template Management:** DataGrid per section 10.1, slide thumbnails in card layout per Widget section 10.3
- **Placeholder Mapping:** Dropdown selectors with form rules per section 9
- **Report Generation:** Progress indicator with brand color, status badges per color overrides section 5

---

## P4b-W4-001: Template Management UI

### Files to Create:

1. **API Layer:**
   - `apps/frontend/src/api/templates.ts` - Template CRUD operations
   - `apps/frontend/src/hooks/useTemplates.ts` - React Query hooks

2. **Pages:**
   - `apps/frontend/src/pages/TemplateListPage.tsx` - Template list with DataGrid
   - `apps/frontend/src/pages/TemplateDetailPage.tsx` - Template detail with preview
   - `apps/frontend/src/pages/TemplateMappingPage.tsx` - Placeholder mapping editor

3. **Components:**
   - `apps/frontend/src/components/Templates/TemplateCard.tsx` - Template card with thumbnails
   - `apps/frontend/src/components/Templates/TemplateUploadDialog.tsx` - Upload dialog
   - `apps/frontend/src/components/Templates/PlaceholderMapper.tsx` - Mapping editor
   - `apps/frontend/src/components/Templates/SlidePreview.tsx` - Slide thumbnail preview

### Endpoints to Call:
- `GET /api/templates/pptx` - List templates
- `POST /api/templates/pptx` - Upload template
- `GET /api/templates/pptx/{id}` - Get template detail
- `GET /api/templates/pptx/{id}/placeholders` - Get placeholders
- `POST /api/templates/pptx/{id}/mapping` - Save mapping
- `GET /api/templates/pptx/{id}/mapping` - Get mapping

---

## P4b-W4-002: Report Generation UI

### Files to Create/Modify:

1. **API Layer:**
   - `apps/frontend/src/api/generation.ts` - Generation API calls

2. **Pages/Components:**
   - Modify `ReportDetailPage.tsx` - Add Generate button
   - Create `BatchGenerationPage.tsx` - Batch generation UI
   - Create `GeneratedReportsPage.tsx` - List generated reports

3. **Components:**
   - `apps/frontend/src/components/Generation/GenerateButton.tsx` - Generate button with progress
   - `apps/frontend/src/components/Generation/GenerationProgress.tsx` - Progress indicator
   - `apps/frontend/src/components/Generation/StatusBadge.tsx` - Status badges

### Endpoints to Call:
- `POST /api/reports/{id}/generate` - Trigger generation (via MS-ORCH)
- `GET /api/reports/{id}/generation-status` - Check status
- `GET /api/reports/{id}/download` - Download generated PPTX
- `POST /api/dashboards/generate-pptx` - Dashboard export (from W2)

---

## Implementation Sequence

```
Phase 1: API & Hooks
├── Create templates.ts API
├── Create generation.ts API  
├── Create React Query hooks

Phase 2: Template Management
├── TemplateListPage with DataGrid
├── TemplateCard component
├── TemplateUploadDialog
├── TemplateDetailPage with SlidePreview
└── PlaceholderMapper with dropdowns

Phase 3: Report Generation
├── GenerateButton on ReportDetailPage
├── GenerationProgress indicator
├── StatusBadge component
├── BatchGenerationPage
└── GeneratedReportsPage

Phase 4: Routing
├── Add routes to App.tsx
└── Update navigation
```

---

## Dependencies

- MS-TMPL-PPTX (backend) - Template management
- MS-GEN-PPTX (backend) - PPTX generation
- MS-LIFECYCLE (backend) - Report status
- TanStack Query - Server state management
- Fluent UI - Component library
- React Router - Navigation

---

## Notes

- Follow existing page patterns in the project
- Use existing DataGrid component if available
- Reuse StatusBadge from existing components if available
- Implement polling or SSE for progress updates
- Handle error states gracefully
