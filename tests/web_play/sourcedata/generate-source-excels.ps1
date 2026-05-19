param(
    [string]$OutputDir = $PSScriptRoot
)

Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'

Add-Type -AssemblyName System.IO.Compression
Add-Type -AssemblyName System.IO.Compression.FileSystem

function Escape-Xml([string]$Value) {
    return [System.Security.SecurityElement]::Escape($Value)
}

function Column-Name([int]$Index) {
    $name = ''
    while ($Index -gt 0) {
        $mod = ($Index - 1) % 26
        $name = [char](65 + $mod) + $name
        $Index = [math]::Floor(($Index - $mod) / 26)
    }
    return $name
}

function Worksheet-Xml([object[]]$Rows) {
    $xmlRows = New-Object System.Collections.Generic.List[string]
    for ($r = 0; $r -lt $Rows.Count; $r++) {
        $cells = New-Object System.Collections.Generic.List[string]
        for ($c = 0; $c -lt $Rows[$r].Count; $c++) {
            $ref = "$(Column-Name ($c + 1))$($r + 1)"
            $value = Escape-Xml ([string]$Rows[$r][$c])
            $cells.Add("<c r=`"$ref`" t=`"inlineStr`"><is><t>$value</t></is></c>")
        }
        $xmlRows.Add("<row r=`"$($r + 1)`">$($cells -join '')</row>")
    }
    return "<?xml version=`"1.0`" encoding=`"UTF-8`" standalone=`"yes`"?><worksheet xmlns=`"http://schemas.openxmlformats.org/spreadsheetml/2006/main`"><sheetData>$($xmlRows -join '')</sheetData></worksheet>"
}

function Add-ZipEntry([System.IO.Compression.ZipArchive]$Zip, [string]$Name, [string]$Content) {
    $entry = $Zip.CreateEntry($Name, [System.IO.Compression.CompressionLevel]::Optimal)
    $writer = New-Object System.IO.StreamWriter($entry.Open(), [System.Text.UTF8Encoding]::new($false))
    try {
        $writer.Write($Content)
    } finally {
        $writer.Dispose()
    }
}

function New-XlsxWorkbook([string]$Path, [hashtable]$Workbook) {
    if (Test-Path $Path) {
        Remove-Item -LiteralPath $Path -Force
    }

    $stream = [System.IO.File]::Open($Path, [System.IO.FileMode]::CreateNew)
    try {
        $zip = New-Object System.IO.Compression.ZipArchive($stream, [System.IO.Compression.ZipArchiveMode]::Create)
        try {
            Add-ZipEntry $zip '[Content_Types].xml' '<?xml version="1.0" encoding="UTF-8" standalone="yes"?><Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types"><Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/><Default Extension="xml" ContentType="application/xml"/><Override PartName="/xl/workbook.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml"/><Override PartName="/xl/worksheets/sheet1.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml"/><Override PartName="/xl/worksheets/sheet2.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml"/><Override PartName="/xl/worksheets/sheet3.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml"/><Override PartName="/docProps/core.xml" ContentType="application/vnd.openxmlformats-package.core-properties+xml"/><Override PartName="/docProps/app.xml" ContentType="application/vnd.openxmlformats-officedocument.extended-properties+xml"/></Types>'
            Add-ZipEntry $zip '_rels/.rels' '<?xml version="1.0" encoding="UTF-8" standalone="yes"?><Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships"><Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="xl/workbook.xml"/><Relationship Id="rId2" Type="http://schemas.openxmlformats.org/package/2006/relationships/metadata/core-properties" Target="docProps/core.xml"/><Relationship Id="rId3" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/extended-properties" Target="docProps/app.xml"/></Relationships>'
            Add-ZipEntry $zip 'xl/_rels/workbook.xml.rels' '<?xml version="1.0" encoding="UTF-8" standalone="yes"?><Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships"><Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet" Target="worksheets/sheet1.xml"/><Relationship Id="rId2" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet" Target="worksheets/sheet2.xml"/><Relationship Id="rId3" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet" Target="worksheets/sheet3.xml"/></Relationships>'
            Add-ZipEntry $zip 'xl/workbook.xml' '<?xml version="1.0" encoding="UTF-8" standalone="yes"?><workbook xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main" xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships"><sheets><sheet name="Metadata" sheetId="1" r:id="rId1"/><sheet name="OpexLines" sheetId="2" r:id="rId2"/><sheet name="PersistPlan" sheetId="3" r:id="rId3"/></sheets></workbook>'
            Add-ZipEntry $zip 'xl/worksheets/sheet1.xml' (Worksheet-Xml $Workbook.Metadata)
            Add-ZipEntry $zip 'xl/worksheets/sheet2.xml' (Worksheet-Xml $Workbook.OpexLines)
            Add-ZipEntry $zip 'xl/worksheets/sheet3.xml' (Worksheet-Xml $Workbook.PersistPlan)
            Add-ZipEntry $zip 'docProps/core.xml' '<?xml version="1.0" encoding="UTF-8" standalone="yes"?><cp:coreProperties xmlns:cp="http://schemas.openxmlformats.org/package/2006/metadata/core-properties" xmlns:dc="http://purl.org/dc/elements/1.1/"><dc:creator>ReportAutomatization E2E</dc:creator><dc:title>Batch import source workbook</dc:title></cp:coreProperties>'
            Add-ZipEntry $zip 'docProps/app.xml' '<?xml version="1.0" encoding="UTF-8" standalone="yes"?><Properties xmlns="http://schemas.openxmlformats.org/officeDocument/2006/extended-properties"><Application>ReportAutomatization Playwright</Application></Properties>'
        } finally {
            $zip.Dispose()
        }
    } finally {
        $stream.Dispose()
    }
}

New-Item -ItemType Directory -Force -Path $OutputDir | Out-Null

$files = @(
    @{
        Name = 'BATCH-A_holding_consolidated.xlsx'
        Metadata = @(
            @('field','value'),
            @('batch_id','BATCH-A-2026-04'),
            @('entity_level','HOLDING'),
            @('holding','Raiffeisen Holding CZ'),
            @('company','ALL'),
            @('division','GROUP'),
            @('cost_center','GROUP-OPEX'),
            @('period','2026-04'),
            @('currency','CZK')
        )
        OpexLines = @(
            @('batch_id','entity_path','account','amount','currency','sink_key','persistent_table'),
            @('BATCH-A-2026-04','Raiffeisen Holding CZ','611100 Travel','124000','CZK','A-HOLD-001','opex_actuals_persisted'),
            @('BATCH-A-2026-04','Raiffeisen Holding CZ','612000 Consulting','380000','CZK','A-HOLD-002','opex_actuals_persisted')
        )
    },
    @{
        Name = 'BATCH-A_company_cz_finance.xlsx'
        Metadata = @(
            @('field','value'),
            @('batch_id','BATCH-A-2026-04'),
            @('entity_level','COMPANY'),
            @('holding','Raiffeisen Holding CZ'),
            @('company','RA CZ Finance'),
            @('division','Finance'),
            @('cost_center','FIN-100'),
            @('period','2026-04'),
            @('currency','CZK')
        )
        OpexLines = @(
            @('batch_id','entity_path','account','amount','currency','sink_key','persistent_table'),
            @('BATCH-A-2026-04','Raiffeisen Holding CZ/RA CZ Finance','621100 Payroll','910000','CZK','A-CZ-001','opex_actuals_persisted'),
            @('BATCH-A-2026-04','Raiffeisen Holding CZ/RA CZ Finance','622500 Training','46000','CZK','A-CZ-002','opex_actuals_persisted')
        )
    },
    @{
        Name = 'BATCH-A_division_cz_it.xlsx'
        Metadata = @(
            @('field','value'),
            @('batch_id','BATCH-A-2026-04'),
            @('entity_level','DIVISION'),
            @('holding','Raiffeisen Holding CZ'),
            @('company','RA CZ Services'),
            @('division','IT Shared Services'),
            @('cost_center','IT-220'),
            @('period','2026-04'),
            @('currency','CZK')
        )
        OpexLines = @(
            @('batch_id','entity_path','account','amount','currency','sink_key','persistent_table'),
            @('BATCH-A-2026-04','Raiffeisen Holding CZ/RA CZ Services/IT Shared Services','631000 Cloud','275000','CZK','A-IT-001','opex_actuals_persisted'),
            @('BATCH-A-2026-04','Raiffeisen Holding CZ/RA CZ Services/IT Shared Services','632000 Licenses','158000','CZK','A-IT-002','opex_actuals_persisted')
        )
    },
    @{
        Name = 'BATCH-A_costcenter_de_ops.xlsx'
        Metadata = @(
            @('field','value'),
            @('batch_id','BATCH-A-2026-04'),
            @('entity_level','COST_CENTER'),
            @('holding','Raiffeisen Holding CZ'),
            @('company','RA DE Operations'),
            @('division','Operations'),
            @('cost_center','OPS-410'),
            @('period','2026-04'),
            @('currency','EUR')
        )
        OpexLines = @(
            @('batch_id','entity_path','account','amount','currency','sink_key','persistent_table'),
            @('BATCH-A-2026-04','Raiffeisen Holding CZ/RA DE Operations/Operations/OPS-410','641000 Facilities','82000','EUR','A-DE-001','opex_actuals_persisted'),
            @('BATCH-A-2026-04','Raiffeisen Holding CZ/RA DE Operations/Operations/OPS-410','642000 Energy','31000','EUR','A-DE-002','opex_actuals_persisted')
        )
    },
    @{
        Name = 'BATCH-B_holding_consolidated.xlsx'
        Metadata = @(
            @('field','value'),
            @('batch_id','BATCH-B-2026-05'),
            @('entity_level','HOLDING'),
            @('holding','Raiffeisen Holding CZ'),
            @('company','ALL'),
            @('division','GROUP'),
            @('cost_center','GROUP-OPEX'),
            @('period','2026-05'),
            @('currency','CZK')
        )
        OpexLines = @(
            @('batch_id','entity_path','account','amount','currency','sink_key','persistent_table'),
            @('BATCH-B-2026-05','Raiffeisen Holding CZ','611100 Travel','132000','CZK','B-HOLD-001','opex_actuals_persisted'),
            @('BATCH-B-2026-05','Raiffeisen Holding CZ','612000 Consulting','405000','CZK','B-HOLD-002','opex_actuals_persisted')
        )
    },
    @{
        Name = 'BATCH-B_company_cz_finance.xlsx'
        Metadata = @(
            @('field','value'),
            @('batch_id','BATCH-B-2026-05'),
            @('entity_level','COMPANY'),
            @('holding','Raiffeisen Holding CZ'),
            @('company','RA CZ Finance'),
            @('division','Finance'),
            @('cost_center','FIN-100'),
            @('period','2026-05'),
            @('currency','CZK')
        )
        OpexLines = @(
            @('batch_id','entity_path','account','amount','currency','sink_key','persistent_table'),
            @('BATCH-B-2026-05','Raiffeisen Holding CZ/RA CZ Finance','621100 Payroll','928000','CZK','B-CZ-001','opex_actuals_persisted'),
            @('BATCH-B-2026-05','Raiffeisen Holding CZ/RA CZ Finance','622500 Training','52000','CZK','B-CZ-002','opex_actuals_persisted')
        )
    },
    @{
        Name = 'BATCH-B_division_cz_it.xlsx'
        Metadata = @(
            @('field','value'),
            @('batch_id','BATCH-B-2026-05'),
            @('entity_level','DIVISION'),
            @('holding','Raiffeisen Holding CZ'),
            @('company','RA CZ Services'),
            @('division','IT Shared Services'),
            @('cost_center','IT-220'),
            @('period','2026-05'),
            @('currency','CZK')
        )
        OpexLines = @(
            @('batch_id','entity_path','account','amount','currency','sink_key','persistent_table'),
            @('BATCH-B-2026-05','Raiffeisen Holding CZ/RA CZ Services/IT Shared Services','631000 Cloud','291000','CZK','B-IT-001','opex_actuals_persisted'),
            @('BATCH-B-2026-05','Raiffeisen Holding CZ/RA CZ Services/IT Shared Services','632000 Licenses','164000','CZK','B-IT-002','opex_actuals_persisted')
        )
    }
)

foreach ($file in $files) {
    $persistPlan = @(
        @('sink_table','persistent_table','merge_key','batch_column','output_excel','output_pdf'),
        @('sink_opex_lines','opex_actuals_persisted','sink_key','batch_id','batch_summary.xlsx','batch_report.pdf')
    )
    New-XlsxWorkbook -Path (Join-Path $OutputDir $file.Name) -Workbook @{
        Metadata = $file.Metadata
        OpexLines = $file.OpexLines
        PersistPlan = $persistPlan
    }
}

Write-Host "Generated $($files.Count) XLSX source workbooks in $OutputDir"
