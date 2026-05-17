param(
    [int]$LocalPort = 15432
)

$result = Test-NetConnection 127.0.0.1 -Port $LocalPort

Write-Host ""
Write-Host "DB tunnel check"
Write-Host "Host : 127.0.0.1"
Write-Host "Port : $LocalPort"
Write-Host "Open : $($result.TcpTestSucceeded)"
Write-Host ""

if (-not $result.TcpTestSucceeded) {
    throw "DB tunnel is not open on 127.0.0.1:$LocalPort. Start scripts\start-db-tunnel.ps1 in a separate PowerShell window first."
}
