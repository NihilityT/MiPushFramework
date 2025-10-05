function Write-Line($Object) {
	$width = $Host.UI.RawUI.WindowSize.Width
	Write-Host "$Object".PadRight($width) @args
}

function Write-Failed($Object) {
	Write-Host $Object -BackgroundColor Red -ForegroundColor White
}

function Write-Successful($Object) {
	Write-Line $Object -BackgroundColor Green -ForegroundColor Black
}

$arguments = $args

function build {

$gradleTasks = @()
	$gradleTasks += ,@(
# build apks
		,"build"

# run unit tests
		,"test"
	)
if ($arguments[0] -ne 'quick') {
	$gradleTasks += ,@(
# run android tests
		,":common:connectedAndroidTest"
		,":mipush_hook:connectedAndroidTest"
		#,":push:connectedVc105DebugAndroidTest",
		,":push:connectedNormalDebugAndroidTest"
	)
}

foreach ($task in $gradleTasks) {
	$task = @() + $task
	Write-Host
	Write-Host
	Write-Host "Run Tasks: $((@('') + $task) -join "`n    ")" -ForegroundColor Green
	./gradlew @task
	if ($LASTEXITCODE -ne 0) {
		Write-Failed "BUILD FAILED"
		return
	}
}

Write-Host "Ensure dex files remain uncompressed in prebuilt variant" -ForegroundColor Green
$compressed = & ./build_scripts/check_dex_compressed.ps1 prebuilt
if ($compressed) {
	Write-Failed "Found compressed dex files in prebuilt variant"
	return
}

Write-Successful "BUILD SUCCESSFUL"

}


$elapsed = Measure-Command { build | Out-Default }

Write-Host -ForegroundColor Cyan ("{0} days {1:d2}:{2:d2}:{3:d2}:{4:d3}" -f
		$elapsed.Days, $elapsed.Hours, $elapsed.Minutes, $elapsed.Seconds, $elapsed.Milliseconds)
