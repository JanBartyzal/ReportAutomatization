#Requires -Version 7.0
<#
.SYNOPSIS
    Parallel Execution Module for Batch Repair Workflow
    
.DESCRIPTION
    Provides parallel job management and execution for the batch repair workflow.
    Handles concurrent builds with proper synchronization and error handling.
#>

# ============================================================================
# PARALLEL JOB MANAGEMENT
# ============================================================================

class ParallelJobManager {
    [int]$MaxConcurrent
    [hashtable[]]$Jobs
    [hashtable[]]$Results
    [scriptblock]$WorkerScript
    
    ParallelJobManager([int]$maxConcurrent, [scriptblock]$workerScript) {
        $this.MaxConcurrent = $maxConcurrent
        $this.Jobs = @()
        $this.Results = @()
        $this.WorkerScript = $workerScript
    }
    
    [void] AddJob([hashtable]$unit) {
        $this.Jobs += $unit
    }
    
    [hashtable[]] ExecuteAll() {
        $totalJobs = $this.Jobs.Count
        $completedJobs = 0
        $activeJobs = @()
        $jobIndex = 0
        
        Write-Host "Starting parallel execution of $totalJobs jobs (max $($this.MaxConcurrent) concurrent)" -ForegroundColor Cyan
        
        # Initial batch of jobs
        while ($activeJobs.Count -lt $this.MaxConcurrent -and $jobIndex -lt $totalJobs) {
            $job = $this.StartJob($this.Jobs[$jobIndex])
            $activeJobs += $job
            $jobIndex++
        }
        
        # Process remaining jobs
        while ($activeJobs.Count -gt 0) {
            # Wait for any job to complete
            $completed = Wait-Job -Job $activeJobs -Any
            
            # Collect result
            $result = Receive-Job -Job $completed
            $this.Results += $result
            $completedJobs++
            
            # Remove completed job
            $activeJobs = $activeJobs | Where-Object { $_.Id -ne $completed.Id }
            Remove-Job -Job $completed
            
            # Start new job if available
            if ($jobIndex -lt $totalJobs) {
                $job = $this.StartJob($this.Jobs[$jobIndex])
                $activeJobs += $job
                $jobIndex++
            }
            
            $progress = [math]::Round(($completedJobs / $totalJobs) * 100, 0)
            Write-Host "Progress: $completedJobs/$totalJobs ($progress%) - Active jobs: $($activeJobs.Count)" -ForegroundColor Yellow
        }
        
        return $this.Results
    }
    
    [System.Management.Automation.Job] StartJob([hashtable]$unit) {
        $job = Start-Job -ScriptBlock $this.WorkerScript -ArgumentList $unit
        Write-Host "Started job for: $($unit.Name) (Job ID: $($job.Id))" -ForegroundColor Gray
        return $job
    }
}

# ============================================================================
# THROTTLED EXECUTION
# ============================================================================

function Invoke-ThrottledExecution {
    param(
        [array]$Items,
        [scriptblock]$ScriptBlock,
        [int]$MaxConcurrent = 4,
        [string]$ItemName = "Item"
    )
    
    $results = @()
    $jobs = @()
    $itemIndex = 0
    
    Write-Host "Starting throttled execution: $($Items.Count) items, max $MaxConcurrent concurrent" -ForegroundColor Cyan
    
    # Start initial batch
    while ($jobs.Count -lt $MaxConcurrent -and $itemIndex -lt $Items.Count) {
        $item = $Items[$itemIndex]
        $job = Start-Job -ScriptBlock $ScriptBlock -ArgumentList $item
        $jobs += @{ Job = $job; Item = $item }
        $itemIndex++
        Write-Host "Started: $ItemName $($item.Name)" -ForegroundColor Gray
    }
    
    # Process remaining items
    while ($jobs.Count -gt 0) {
        $completedJob = Wait-Job -Job ($jobs.Job) -Any
        
        # Find and remove the completed job
        $completedEntry = $jobs | Where-Object { $_.Job.Id -eq $completedJob.Id } | Select-Object -First 1
        $result = Receive-Job -Job $completedJob
        $results += $result
        
        $jobs = $jobs | Where-Object { $_.Job.Id -ne $completedJob.Id }
        Remove-Job -Job $completedJob
        
        # Start new job if available
        if ($itemIndex -lt $Items.Count) {
            $item = $Items[$itemIndex]
            $job = Start-Job -ScriptBlock $ScriptBlock -ArgumentList $item
            $jobs += @{ Job = $job; Item = $item }
            $itemIndex++
            Write-Host "Started: $ItemName $($item.Name)" -ForegroundColor Gray
        }
        
        $completed = $results.Count
        $progress = [math]::Round(($completed / $Items.Count) * 100, 0)
        Write-Host "Progress: $completed/$($Items.Count) ($progress%) - Active: $($jobs.Count)" -ForegroundColor Yellow
    }
    
    return $results
}

# ============================================================================
# BATCH PROCESSING WITH RETRY
# ============================================================================

function Invoke-BatchProcessingWithRetry {
    param(
        [array]$Items,
        [scriptblock]$ProcessScript,
        [int]$MaxRetries = 3,
        [int]$MaxConcurrent = 4
    )
    
    $results = @()
    $failedItems = @()
    $retryCount = 0
    $itemsToProcess = $Items
    
    while ($itemsToProcess.Count -gt 0 -and $retryCount -le $MaxRetries) {
        Write-Host "Processing batch (Attempt $($retryCount + 1)/$($MaxRetries + 1)): $($itemsToProcess.Count) items" -ForegroundColor Cyan
        
        $batchResults = Invoke-ThrottledExecution -Items $itemsToProcess -ScriptBlock $ProcessScript -MaxConcurrent $MaxConcurrent -ItemName "Unit"
        
        # Separate successful and failed items
        $successfulResults = @()
        $itemsToProcess = @()
        
        foreach ($result in $batchResults) {
            if ($result.Success -or $result.Status -eq "SUCCESS") {
                $successfulResults += $result
            }
            else {
                # Find original item for retry
                $originalItem = $Items | Where-Object { $_.Name -eq $result.Name } | Select-Object -First 1
                if ($originalItem) {
                    $itemsToProcess += $originalItem
                }
            }
        }
        
        $results += $successfulResults
        $retryCount++
        
        if ($itemsToProcess.Count -gt 0 -and $retryCount -le $MaxRetries) {
            Write-Host "Retrying $($itemsToProcess.Count) failed items..." -ForegroundColor Yellow
            Start-Sleep -Seconds 2
        }
    }
    
    # Remaining failed items
    if ($itemsToProcess.Count -gt 0) {
        Write-Host "Failed to process $($itemsToProcess.Count) items after $MaxRetries retries" -ForegroundColor Red
        $failedItems = $itemsToProcess
    }
    
    return @{
        Successful     = $results
        Failed         = $failedItems
        TotalProcessed = $results.Count
        TotalFailed    = $failedItems.Count
    }
}

# ============================================================================
# PROGRESS TRACKING
# ============================================================================

class ProgressTracker {
    [int]$Total
    [int]$Completed
    [int]$Failed
    [datetime]$StartTime
    [hashtable]$ItemStatus
    
    ProgressTracker([int]$total) {
        $this.Total = $total
        $this.Completed = 0
        $this.Failed = 0
        $this.StartTime = Get-Date
        $this.ItemStatus = @{}
    }
    
    [void] MarkComplete([string]$itemName, [bool]$success) {
        if ($success) {
            $this.Completed++
            $this.ItemStatus[$itemName] = "SUCCESS"
        }
        else {
            $this.Failed++
            $this.ItemStatus[$itemName] = "FAILED"
        }
    }
    
    [void] PrintProgress() {
        $elapsed = New-TimeSpan -Start $this.StartTime -End (Get-Date)
        $processed = $this.Completed + $this.Failed
        $percentage = [math]::Round(($processed / $this.Total) * 100, 0)
        
        $estimatedTotal = if ($processed -gt 0) {
            [timespan]::FromSeconds(($elapsed.TotalSeconds / $processed) * $this.Total)
        }
        else {
            [timespan]::Zero
        }
        
        $remaining = $estimatedTotal - $elapsed
        
        Write-Host "Progress: $processed/$($this.Total) ($percentage%) | Success: $($this.Completed) | Failed: $($this.Failed) | Elapsed: $([math]::Round($elapsed.TotalMinutes, 1))m | ETA: $([math]::Round($remaining.TotalMinutes, 1))m" -ForegroundColor Cyan
    }
    
    [hashtable] GetSummary() {
        $elapsed = New-TimeSpan -Start $this.StartTime -End (Get-Date)
        return @{
            Total       = $this.Total
            Completed   = $this.Completed
            Failed      = $this.Failed
            SuccessRate = [math]::Round(($this.Completed / $this.Total) * 100, 2)
            ElapsedTime = $elapsed
            ItemStatus  = $this.ItemStatus
        }
    }
}

# ============================================================================
# RESOURCE MONITORING
# ============================================================================

function Get-SystemResourceStatus {
    $cpuUsage = (Get-WmiObject win32_processor | Measure-Object -Property LoadPercentage -Average).Average
    $memUsage = (Get-WmiObject win32_operatingsystem | ForEach-Object { [math]::Round(($_.TotalVisibleMemorySize - $_.FreePhysicalMemory) / $_.TotalVisibleMemorySize * 100, 2) })
    
    return @{
        CPUUsage    = $cpuUsage
        MemoryUsage = $memUsage
        Timestamp   = Get-Date
    }
}

function Monitor-ResourcesAndAdjustConcurrency {
    param(
        [int]$CurrentConcurrency,
        [int]$MaxConcurrency,
        [int]$MinConcurrency = 1
    )
    
    $resources = Get-SystemResourceStatus
    
    if ($resources.CPUUsage -gt 85 -or $resources.MemoryUsage -gt 85) {
        $newConcurrency = [math]::Max($MinConcurrency, $CurrentConcurrency - 1)
        Write-Host "High resource usage detected (CPU: $($resources.CPUUsage)%, Mem: $($resources.MemoryUsage)%). Reducing concurrency from $CurrentConcurrency to $newConcurrency" -ForegroundColor Yellow
        return $newConcurrency
    }
    elseif ($resources.CPUUsage -lt 50 -and $resources.MemoryUsage -lt 50 -and $CurrentConcurrency -lt $MaxConcurrency) {
        $newConcurrency = [math]::Min($MaxConcurrency, $CurrentConcurrency + 1)
        Write-Host "Low resource usage detected (CPU: $($resources.CPUUsage)%, Mem: $($resources.MemoryUsage)%). Increasing concurrency from $CurrentConcurrency to $newConcurrency" -ForegroundColor Green
        return $newConcurrency
    }
    
    return $CurrentConcurrency
}

Export-ModuleMember -Function @(
    'Invoke-ThrottledExecution',
    'Invoke-BatchProcessingWithRetry',
    'Get-SystemResourceStatus',
    'Monitor-ResourcesAndAdjustConcurrency'
) -Class @(
    'ParallelJobManager',
    'ProgressTracker'
)
