# UAT Errors - step10_Atomizer_Excel

Timestamp: 2026-03-26T18:32:04

[FAIL] Exactly 1 sheet in workbook (got 0)
## Assertion Failed
- Message: Exactly 1 sheet in workbook (got 0)

[FAIL] Headers match expected 13 columns (got 163: ['[', '"', 'I', 'd', '"', ',', ' ', '"', 'P', 'r', 'o', 'j', 'e', 'c', 't', ' ', 'N', 'a', 'm', 'e', '"', ',', ' ', '"', 'M', 'a', 'x', ' ', 'p', 'r', 'o', 'j', 'e', 'c', 't', ' ', 'c', 'o', 's', 't', '"', ',', ' ', '"', 'B', 'u', 'd', 'g', 'e', 't', '2', '4', '"', ',', ' ', '"', 'B', 'u', 'd', 'g', 'e', 't', ' ', '2', '5', '"', ',', ' ', '"', 'B', 'u', 'd', 'g', 'e', 't', ' ', '2', '6', '"', ',', ' ', '"', 'C', 'o', 's', 't', '2', '4', '"', ',', ' ', '"', 'C', 'o', 's', 't', '2', '5', '"', ',', ' ', '"', 'C', 'o', 's', 't', '2', '6', '"', ',', ' ', '"', 'T', 'o', 't', 'a', 'l', 'C', 'o', 's', 't', '"', ',', ' ', '"', 'T', 'o', 'a', 'l', 'B', 'u', 'd', 'g', 'e', 't', '"', ',', ' ', '"', 'D', 'i', 'f', 'f', 'C', 'o', 's', 't', '"', ',', ' ', '"', 'D', 'i', 'f', 'f', 'B', 'u', 'd', 'g', 'e', 't', '"', ']'])
## Assertion Failed
- Message: Headers match expected 13 columns (got 163: ['[', '"', 'I', 'd', '"', ',', ' ', '"', 'P', 'r', 'o', 'j', 'e', 'c', 't', ' ', 'N', 'a', 'm', 'e', '"', ',', ' ', '"', 'M', 'a', 'x', ' ', 'p', 'r', 'o', 'j', 'e', 'c', 't', ' ', 'c', 'o', 's', 't', '"', ',', ' ', '"', 'B', 'u', 'd', 'g', 'e', 't', '2', '4', '"', ',', ' ', '"', 'B', 'u', 'd', 'g', 'e', 't', ' ', '2', '5', '"', ',', ' ', '"', 'B', 'u', 'd', 'g', 'e', 't', ' ', '2', '6', '"', ',', ' ', '"', 'C', 'o', 's', 't', '2', '4', '"', ',', ' ', '"', 'C', 'o', 's', 't', '2', '5', '"', ',', ' ', '"', 'C', 'o', 's', 't', '2', '6', '"', ',', ' ', '"', 'T', 'o', 't', 'a', 'l', 'C', 'o', 's', 't', '"', ',', ' ', '"', 'T', 'o', 'a', 'l', 'B', 'u', 'd', 'g', 'e', 't', '"', ',', ' ', '"', 'D', 'i', 'f', 'f', 'C', 'o', 's', 't', '"', ',', ' ', '"', 'D', 'i', 'f', 'f', 'B', 'u', 'd', 'g', 'e', 't', '"', ']'])

[FAIL] Exactly 5 data rows (got 597)
## Assertion Failed
- Message: Exactly 5 data rows (got 597)

[FAIL] Row 0: Id == 1
## Assertion Failed
- Message: Row 0: Id == 1

[FAIL] Row 0: Project Name == 'Item1'
## Assertion Failed
- Message: Row 0: Project Name == 'Item1'

[FAIL] Row 0: Max project cost == 150000
## Assertion Failed
- Message: Row 0: Max project cost == 150000

[FAIL] Row 0: TotalCost == 155000
## Assertion Failed
- Message: Row 0: TotalCost == 155000

[FAIL] Row 3: Id == 4
## Assertion Failed
- Message: Row 3: Id == 4

[FAIL] Row 3: Project Name == 'Item4'
## Assertion Failed
- Message: Row 3: Project Name == 'Item4'

[FAIL] Row 3: Max project cost == 1265000
## Assertion Failed
- Message: Row 3: Max project cost == 1265000

[FAIL] Row 3: TotalCost == 1342500 (largest project)
## Assertion Failed
- Message: Row 3: TotalCost == 1342500 (largest project)

[FAIL] Row 4: Id == 5
## Assertion Failed
- Message: Row 4: Id == 5

[FAIL] Row 4: DiffCost == 520500 (biggest positive diff)
## Assertion Failed
- Message: Row 4: DiffCost == 520500 (biggest positive diff)

[FAIL] Sum of TotalCost == 1617700 (got 0)
## Assertion Failed
- Message: Sum of TotalCost == 1617700 (got 0)

## Missing Feature (skipped)
- Endpoint: `GET /api/query/files/8b8f0e42-8c36-49bd-99ef-316b57aa031e/sheets/9999`
- Description: Per-sheet endpoint does not exist in current API
