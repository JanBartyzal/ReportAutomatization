# Sample PPTX Templates

This directory contains sample PPTX templates for testing the report generation system.

## Structure

```
templates/
├── README.md              # This file
├── mapping-config.json    # Placeholder mapping configuration
├── sample_data.json       # Sample data for testing
└── sample_report.pptx     # Sample PPTX template (copy from demo)
```

## Placeholders

The sample template demonstrates the following placeholder types:

### Text Placeholders
- `{{company_name}}` - Organization name
- `{{period}}` - Reporting period (e.g., Q1 2026)
- `{{total_opex}}` - Total OPEX amount

### Table Placeholders
- `{{TABLE:opex_summary}}` - OPEX summary table with quarterly breakdown

### Chart Placeholders
- `{{CHART:monthly_trend}}` - Monthly costs trend line chart

## Usage

1. **Upload to MS-TMPL-PPTX**: Use the `/api/templates/pptx` endpoint to upload the template
2. **Configure Mapping**: Use the mapping configuration to link placeholders to data sources
3. **Generate Report**: Use MS-GEN-PPTX to generate a report with the template

## Sample Data

The `sample_data.json` contains example data that can be used for testing:

```json
{
  "company_name": "Acme Corporation",
  "period": "Q1 2026",
  "total_opex": "1,234,567 CZK",
  ...
}
```

## Testing

To test the template:
1. Start the docker-compose environment
2. Upload the sample PPTX template
3. Configure the placeholder mappings using the config
4. Generate a report using the sample data

## Notes

- The PPTX template must contain the exact placeholder names
- Table placeholders use the format `{{TABLE:table_name}}`
- Chart placeholders use the format `{{CHART:chart_name}}`
- Placeholders are case-sensitive
