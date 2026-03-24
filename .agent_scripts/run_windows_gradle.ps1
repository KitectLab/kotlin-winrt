param(
    [Parameter(ValueFromRemainingArguments = $true)]
    [string[]]$GradleArgs
)

$scriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$repoRoot = Split-Path -Parent $scriptDir

Set-Location -LiteralPath $repoRoot

$quotedArgs = @()
foreach ($arg in $GradleArgs) {
    $quotedArgs += '"' + ($arg -replace '"', '\"') + '"'
}

$commandLine = "call gradlew.bat " + ($quotedArgs -join ' ')
$process = Start-Process `
    -FilePath "cmd.exe" `
    -ArgumentList "/d", "/s", "/c", $commandLine `
    -WorkingDirectory $repoRoot `
    -NoNewWindow `
    -Wait `
    -PassThru

$exitCode = $process.ExitCode

if ($null -eq $exitCode) {
    $exitCode = 0
}

exit $exitCode
