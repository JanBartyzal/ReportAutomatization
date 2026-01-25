# OPEX API

Endpoints for processing and retrieving data from OPEX PowerPoint reports.

## Get OPEX Processing Data

`GET /api/opex/data`

Check or retrieve processing status for a specific OPEX report.

### Query Parameters
- `opex_id` (string): The unique ID of the OPEX report.

### Responses
- **200 OK**: Status message and user ID.

### Authentication
Required.

---

## Run OPEX Processing

`GET /api/opex/run_opex`

Trigger the extraction and processing logic for an uploaded PPTX file.

### Query Parameters
- `file_id` (string): The ID of the uploaded file to process.

### Responses
- **200 OK**: Message confirming processing has started.

### Authentication
Required.

---

## Get Presentation Header

`GET /api/opex/get_file_header`

Retrieve high-level summary information about slides in a processed presentation.

### Query Parameters
- `file_id` (string): The ID of the processed file.

### Responses
- **200 OK**: List of slide summaries (captions, slide levels).

### Authentication
Required.

---

## Get Slide Data

`GET /api/opex/get_slide_data`

Retrieve detailed table data and content for a specific slide.

### Query Parameters
- `file_id` (string): The ID of the processed file.
- `slide_id` (int): The index of the slide.

### Responses
- **200 OK**: Detailed data for the requested slide.

### Authentication
Required.
