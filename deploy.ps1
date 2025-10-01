param([switch]$Debug, [switch]$Vc105)

Write-Host "Checking for uncommitted changes" -ForegroundColor Green
[string]$res = git status --porcelain
if ($res) {
	Write-Host "Commit changes before deploying" -ForegroundColor Red
	return;
}

Write-Host "Validating build" -ForegroundColor Green
./build
if ($LASTEXITCODE -ne 0) {
	return
}

$ver = if ($Vc105) { 'vc105' } else { 'normal' }
$type = if ($Debug) { 'debug' } else { 'release' }

$file = ls .\push\build\outputs\apk\$ver\$type\*.apk
$path = $file.FullName
Write-Host "Deploying $path" -ForegroundColor Green
$devices = adb devices | % { if ($null = $_ -match '^(\S+)\s+device$') { $Matches[1] } }
foreach ($device in $devices) {
	Write-Host "Deploying for $device" -ForegroundColor Green
	adb -s $device install $path
}
