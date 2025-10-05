class BuildManager {

    [bool]$isBuilding = $false
    [bool]$pendingBuild = $false
    $arguments
    $debounceTimer

    BuildManager($arguments, $debounceTimer) {
        $this.arguments = $arguments
        $this.debounceTimer = $debounceTimer
    }

    [void]Run() {

        if ($this.isBuilding) {
            $this.pendingBuild = $true
            Write-Host "Building, new task will be pendding" -ForegroundColor Magenta
            return
        }

        $this.isBuilding = $true
        $this.pendingBuild = $false

        Write-Host "Start Build" -ForegroundColor Green

        $args = $this.arguments
        try {
            & ./build @args
        }
        catch {
            Write-Host "Build Error: $_" -ForegroundColor Red
        }
        finally {
            $this.isBuilding = $false

            if ($this.pendingBuild) {
                Write-Host "run pendding build task" -ForegroundColor Yellow
                $this.debounceTimer.Start()
            }
        }
    }
}

class Watcher {

    $Data
    $ExcludeFolderPatterns
    $Watchers
    $Callback
    $Action = {
        $self = $Event.MessageData
        $self.DoCallback($Event)
    }

    Watcher([string[]]$excludeFolderPatterns) {
        $this.ExcludeFolderPatterns = $excludeFolderPatterns
        $folders = $this.ParseExcludeFolderPatterns($excludeFolderPatterns)

        $this.Watchers = @()
        if ($folders.excludeSubdirectories.Length -eq 0) {
            $this.Watchers += $this.CreateWatcher('.', $true)
        } else {
            $this.Watchers += $this.CreateWatcher('.', $false)
            foreach ($folder in $folders.excludeSubdirectories) {
                $this.Watchers += $this.CreateWatcher($folder, $false)
            }
            foreach ($folder in $folders.includeSubdirectories) {
                $this.Watchers += $this.CreateWatcher($folder, $true)
            }
        }
    }

    [hashtable]ParseExcludeFolderPatterns([string[]]$excludeFolderPatterns) {
        $currentDir = Get-Item .
        $folderFilters = $excludeFolderPatterns | %{ $_; "*\$_" }
        $needToFilterFolders = ls $folderFilters -ErrorAction Ignore -Directory
        $includeSubdirectories = @()
        $excludeSubdirectories = @()
        foreach ($folder in $needToFilterFolders) {
            $f = $folder.Parent
            while ($f.FullName -ne $currentDir.FullName) {
                $excludeSubdirectories += $f
                $includeSubdirectories += ls $f -Exclude $folderFilters -Directory
                $f = $f.Parent
            }
        }
        $excludeSubdirectories = $excludeSubdirectories | %{ $_.FullName }
        $includeSubdirectories = $includeSubdirectories | %{ $_.FullName }
        $includeSubdirectories = $includeSubdirectories | ? { $excludeSubdirectories -notcontains $_ }

        return @{
            excludeSubdirectories = $excludeSubdirectories
            includeSubdirectories = $includeSubdirectories
        }
    }

    [hashtable]CreateWatcher($path, $IncludeSubdirectories = $true) {
        $watcher = New-Object System.IO.FileSystemWatcher
        $watcher.Path = $path
        $watcher.IncludeSubdirectories = $IncludeSubdirectories  # 包含子目录
        $watcher.EnableRaisingEvents = $true   # 启用事件触发
        $watcher.NotifyFilter = [IO.NotifyFilters]'LastWrite,FileName'

        $handlers = @()
        $events = @('Created', 'Changed', 'Deleted', 'Renamed')


        foreach ($eventName in $events) {
            $handler = Register-ObjectEvent -InputObject $watcher -EventName $eventName -Action $this.Action -MessageData $this
            $handlers += $handler
        }

        return @{
            watcher = $watcher
            handlers = $handlers
        }
    }

    DoCallback($Event) {
        $name = $Event.SourceEventArgs.Name
        $path = $Event.SourceEventArgs.FullPath
        $isDirectory = Test-Path -PathType Container $path
        if ($isDirectory) {
            return
        }
        $shouldIgnore = $this.ExcludeFolderPatterns | Where-Object { $name -like $_ }
        if ($shouldIgnore) {
            return
        }
        Invoke-Command $this.Callback -ArgumentList $Event
    }

    SetCallback($callback) {
        $this.Callback = $callback
    }

    Loop() {
        try {
            Wait-Event -SourceIdentifier $this.Watchers[0].handlers[0].Name
        }
        finally {
            $this.watchers | % {
                $_.watcher.Dispose()
                $_.handlers | Remove-Job -Force
            }
        }
    }
}

$debounceTimer = New-Object System.Timers.Timer
$debounceTimer.Interval = 1000
$debounceTimer.AutoReset = $false

$buildManager = [BuildManager]::new($args, $debounceTimer)

$data = @{
    buildManager = $buildManager
    debounceTimer = $debounceTimer
    triggerSource = $null
}

$timerHandle = Register-ObjectEvent -InputObject $debounceTimer -EventName Elapsed -Action {
    $data = $Event.MessageData
    if ($data.triggerSource -eq $null) {
        $data.triggerSource = "pendding build task"
    }

    function TriggerBy($tag) {
        Write-Host "$($tag*8) triggered by [" -NoNewline
        Write-Host $data.triggerSource -NoNewline -ForegroundColor Green
        Write-Host ']'
    }

    TriggerBy '<'
    $data.buildManager.run()
    TriggerBy '>'

    $data.triggerSource = $null
} -MessageData $data

$watcher = [Watcher]::new(@(".*", "build"))
$watcher.Data = $data
$watcher.SetCallback({ param([System.Management.Automation.PSEventArgs]$Event)
    $watcher = $Event.MessageData
    $data = $watcher.Data

    $data.triggerSource = Resolve-Path -Relative $Event.SourceEventArgs.FullPath

    $debounceTimer = $data.debounceTimer
    $debounceTimer.Stop()
    $debounceTimer.Start()
})

try {
    $watcher.Loop()
} finally {
    $debounceTimer.Stop()
    $debounceTimer.Dispose()
    $timerHandle | Remove-Job -Force
}
