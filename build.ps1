$gradleTasks = @(
# build apks
	"build",

# run unit tests
	"test",

# run android tests
	":common:connectedAndroidTest",
	":mipush_hook:connectedAndroidTest",
	#":push:connectedVc105DebugAndroidTest",
	":push:connectedNormalDebugAndroidTest"
)

$width = $Host.UI.RawUI.WindowSize.Width
foreach ($task in $gradleTasks) {
	./gradlew ($task -split ' +')
	if ($LASTEXITCODE -ne 0) {
		Write-Host "task failed: $task".PadRight($width) -BackgroundColor Red -ForegroundColor Black
		return
	}
}

Write-Host "all tasks success!".PadRight($width) -BackgroundColor Green -ForegroundColor Black
