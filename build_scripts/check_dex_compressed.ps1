param($Variant, [switch]$Debug)
$type = if ($Debug) { 'debug' } else { 'release' }
$apkPath = gi .\push\build\outputs\apk\$Variant\$type\*.apk

$dexLines = unzip -v $apkPath | ? { $_ -like '*.dex' }
$compressedLines = $dexLines | ? { $_ -notmatch ' Stored ' }
return $compressedLines
