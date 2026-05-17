param(
    [string]$Ec2Host = "34.240.100.223",
    [string]$Ec2User = "ubuntu",
    [string]$KeyPath = "D:\Project\Parking\docker\parking-ec2-key.pem",
    [string]$RdsHost = "park-db.czcq4k0w4b57.eu-west-1.rds.amazonaws.com",
    [int]$LocalPort = 15432,
    [int]$RdsPort = 5432
)

if (-not (Test-Path -LiteralPath $KeyPath)) {
    throw "SSH key not found: $KeyPath"
}

$forward = "127.0.0.1:{0}:{1}:{2}" -f $LocalPort, $RdsHost, $RdsPort
$target = "{0}@{1}" -f $Ec2User, $Ec2Host

Write-Host ""
Write-Host "Starting DB SSH tunnel"
Write-Host "EC2 Host   : $Ec2Host"
Write-Host "EC2 User   : $Ec2User"
Write-Host "RDS Host   : $RdsHost"
Write-Host "Local Port : $LocalPort"
Write-Host "Forward    : 127.0.0.1:$LocalPort -> $RdsHost`:$RdsPort"
Write-Host ""
Write-Host "Keep this PowerShell window open while tests are running."
Write-Host "Press Ctrl+C to stop the tunnel."
Write-Host ""

& ssh `
    -i $KeyPath `
    -o StrictHostKeyChecking=accept-new `
    -o ExitOnForwardFailure=yes `
    -o ServerAliveInterval=30 `
    -o ServerAliveCountMax=3 `
    -N `
    -L $forward `
    $target
