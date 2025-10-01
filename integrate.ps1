param([string]$Message, [string]$Commit, [switch]$Reset)

if ($message -eq "") {
	Write-Host "Need -message argument for integration message" -ForegroundColor Red
	return;
}

Write-Host "Checking for uncommitted changes" -ForegroundColor Green
[string]$res = git status --porcelain
if ($res) {
	Write-Host "Commit changes before integrating" -ForegroundColor Red
	return;
}

$integrationBranch = "master"
$currentBranch = git rev-parse --abbrev-ref HEAD

if ($commit -eq "") {
	$commit = $currentBranch
}

$tempBranch = "temp_integrate_branch_$commit"

try {
	git checkout -B $tempBranch $commit
	Write-Host "Validating build" -ForegroundColor Green
	./build
	if ($LASTEXITCODE -ne 0) {
		return
	}

	Write-Host "Integrating $currentBranch into $integrationBranch" -ForegroundColor Green

	git checkout $integrationBranch
	if ($Reset) {
		git reset --hard origin/$integrationBranch
	}
	git merge $commit --no-ff --log=9999 --message="INTEGRATE: $message"
	git rebase $integrationBranch $currentBranch
}
finally {
	git checkout -f $currentBranch
	git branch -D $tempBranch
}
