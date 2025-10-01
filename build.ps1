
function build {

$gradleTasks = @(
	,@(
# build apks
		,"build"

# run unit tests
		,"test"
	)

	,@(
# run android tests
		,":common:connectedAndroidTest"
		,":mipush_hook:connectedAndroidTest"
		#,":push:connectedVc105DebugAndroidTest",
		,":push:connectedNormalDebugAndroidTest"
	)
)

$width = $Host.UI.RawUI.WindowSize.Width
foreach ($task in $gradleTasks) {
	$task = @() + $task
	Write-Host
	Write-Host
	Write-Host "Run Tasks: $((@('') + $task) -join "`n    ")" -ForegroundColor Green
	./gradlew @task
	if ($LASTEXITCODE -ne 0) {
		Write-Host "BUILD FAILED".PadRight($width) -BackgroundColor Red -ForegroundColor White
		return
	}
}

Write-Host "BUILD SUCCESSFUL".PadRight($width) -BackgroundColor Green -ForegroundColor Black

}


$elapsed = Measure-Command { build | Out-Default }

Write-Host -ForegroundColor Cyan ("{0} days {1:d2}:{2:d2}:{3:d2}:{4:d3}" -f
		$elapsed.Days, $elapsed.Hours, $elapsed.Minutes, $elapsed.Seconds, $elapsed.Milliseconds)
