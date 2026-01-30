# Task: Implement Template Aggregation Dashboard (FEAT-02-FE)

## Overview
Create a dashboard in React to allow users to select multiple files, preview common table schemas, and view aggregated data across those files.

## Checklist

### 1. UI Components
- [ ] **File Selector**: Component to multi-select files for analysis.
- [ ] **Schema Preview List**: Displays detected common schemas (fingerprints) with row counts and source file lists.
- [ ] **Aggregation Table**: Advanced grid component to display merged data from a selected schema, including metadata columns like `_source_file`.

### 2. API Integration
- [ ] Integrate with `POST /api/analytics/aggregate/preview`.
- [ ] Integrate with `GET /api/analytics/aggregate/{schema_fingerprint}`.

### 3. UX Features
- [ ] Loading states for analysis and data fetching.
- [ ] Error handling (e.g., if no common schemas are found).
- [ ] Export to CSV/Excel functionality for the aggregated dataset.

## Implementation Details
- **Location**: `frontend/src/pages/AggregationDashboard.tsx`
- **Routing**: Add `/aggregation` route to `App.tsx`.
- **State Management**: Use React Query for fetching and caching aggregation data.
