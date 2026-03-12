# Sample OPEX Budget Excel Template

This directory contains sample Excel templates for form data import.

## Files

- `OPEX_Budget_2026_Q1_template.xlsx` - Sample Excel template (can be generated via MS-FORM API)

## Template Structure

The Excel template for OPEX Budget 2026 follows this structure:

### Sheet 1: Personnel
| Header | Field Key | Type |
|--------|-----------|------|
| Total Headcount | headcount | number |
| Total Salaries (CZK) | salaries_total | number |
| Other Personnel Costs (CZK) | personnel_other | number |

### Sheet 2: IT
| Header | Field Key | Type |
|--------|-----------|------|
| Hardware & Equipment (CZK) | it_hardware | number |
| Software Licenses (CZK) | it_software | number |
| Cloud Services (CZK) | it_cloud | number |
| IT Support Services (CZK) | it_support | number |

### Sheet 3: Office
| Header | Field Key | Type |
|--------|-----------|------|
| Office Rent (CZK) | office_rent | number |
| Utilities (CZK) | office_utilities | number |
| Office Supplies (CZK) | office_supplies | number |
| Business Insurance (CZK) | office_insurance | number |

### Sheet 4: Travel
| Header | Field Key | Type |
|--------|-----------|------|
| Domestic Travel (CZK) | travel_domestic | number |
| International Travel (CZK) | travel_international | number |
| Client Entertainment (CZK) | travel_entertainment | number |

### Sheet 5: Summary
| Header | Field Key | Type |
|--------|-----------|------|
| Total Budget (CZK) | budget_total | number (calculated) |
| Budget Category | budget_category | dropdown |
| Additional Notes | budget_notes | text |

### Hidden Sheet: __form_meta
Contains metadata for validation:
- `form_id`: a1b2c3d4-0001-0001-0001-000000000001
- `form_version_id`: b2c3d4e5-0001-0001-0001-000000000001

## Usage

1. Download template via MS-FORM API: `GET /api/forms/{form_id}/export/excel-template`
2. Fill in the data in Excel
3. Import back via: `POST /api/forms/{form_id}/import/excel`
