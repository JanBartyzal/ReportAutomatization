# MS-ATM-XLS – Excel Atomizer

Python gRPC microservice that extracts structure, content, and data types from Excel (.xlsx) files using openpyxl.

## gRPC API

| RPC | Request | Response | Description |
|-----|---------|----------|-------------|
| `ExtractStructure` | `ExtractRequest` | `ExcelStructureResponse` | List sheets with metadata (row/col counts, merged cells) |
| `ExtractSheetContent` | `SheetRequest` | `SheetContentResponse` | Extract headers, rows, and detected column data types for one sheet |
| `ExtractAll` | `ExtractRequest` | `ExcelFullExtractionResponse` | Batch extraction of all sheets with partial-success handling |

## Sequence Diagram

```mermaid
sequenceDiagram
    participant Orchestrator
    participant MS-ATM-XLS
    participant BlobStorage

    Orchestrator->>MS-ATM-XLS: ExtractAll(file_id, blob_url)
    MS-ATM-XLS->>BlobStorage: Download Excel file
    BlobStorage-->>MS-ATM-XLS: Excel bytes

    loop For each sheet
        MS-ATM-XLS->>MS-ATM-XLS: Parse sheet content
        MS-ATM-XLS->>MS-ATM-XLS: Detect column data types
        alt Success
            MS-ATM-XLS->>MS-ATM-XLS: Add to successful_sheets
        else Failure
            MS-ATM-XLS->>MS-ATM-XLS: Add to failed_sheets
        end
    end

    MS-ATM-XLS-->>Orchestrator: ExcelFullExtractionResponse (COMPLETED | PARTIAL | FAILED)
```

## Features

- **Merged cells**: Unmerged and filled with top-left value
- **Hidden sheets**: Included in extraction with visibility tracked in metadata
- **Formula cells**: `data_only=True` returns computed values
- **Data type detection**: STRING, NUMBER, DATE, CURRENCY, PERCENTAGE (with Czech locale support)
- **Empty row filtering**: Configurable threshold via `EMPTY_ROW_THRESHOLD`

## Configuration (Environment Variables)

| Variable | Default | Description |
|----------|---------|-------------|
| `SERVICE_NAME` | `ms-atm-xls` | Service identifier |
| `GRPC_PORT` | `50051` | gRPC server port |
| `BLOB_STORAGE_URL` | `http://127.0.0.1:10000/devstoreaccount1` | Blob storage endpoint |
| `AZURE_STORAGE_CONNECTION_STRING` | - | Azure Storage connection string |
| `BLOB_CONTAINER` | `files` | Default blob container |
| `EMPTY_ROW_THRESHOLD` | `0` | Skip rows with fewer non-empty cells than this |

## Development

```bash
# Install dependencies
pip install -e ".[dev]"

# Run tests
pytest

# Run service
python -m src.main
```

## Docker

```bash
docker build -t ms-atm-xls .
docker run -p 50051:50051 ms-atm-xls
```
