# 폰이 USB 없이(와이파이·Tailscale) PC 서버 8080 포트에 접속할 수 있도록 Windows 방화벽을 연다.
# 사용법: PowerShell 을 "관리자 권한으로 실행" 한 뒤
#   cd <프로젝트>\server
#   powershell -ExecutionPolicy Bypass -File .\scripts\allow-phone-access.ps1
# 같은 공유기(로컬 서브넷)와 Tailscale(100.64.0.0/10) 에서 오는 접속만 허용한다. 인터넷 전체에 열지 않음.

param([int]$Port = 8080)

$name = "Health RPG server ($Port)"
$principal = New-Object Security.Principal.WindowsPrincipal([Security.Principal.WindowsIdentity]::GetCurrent())
if (-not $principal.IsInRole([Security.Principal.WindowsBuiltInRole]::Administrator)) {
    Write-Host "관리자 권한이 필요합니다. PowerShell 을 '관리자 권한으로 실행' 해 주세요." -ForegroundColor Red
    exit 1
}

Get-NetFirewallRule -DisplayName $name -ErrorAction SilentlyContinue | Remove-NetFirewallRule
New-NetFirewallRule -DisplayName $name -Direction Inbound -Protocol TCP -LocalPort $Port `
    -RemoteAddress LocalSubnet, 100.64.0.0/10 -Profile Any -Action Allow | Out-Null
Write-Host "방화벽 규칙 추가: $name (같은 와이파이 + Tailscale 만 허용)" -ForegroundColor Green

Write-Host ""
Write-Host "폰 앱 > 설정 > 서버 주소에 입력할 주소:"
Get-NetIPAddress -AddressFamily IPv4 |
    Where-Object { $_.IPAddress -notlike "127.*" -and $_.IPAddress -notlike "169.254.*" -and $_.InterfaceAlias -notlike "vEthernet*" } |
    ForEach-Object {
        $kind = if ($_.IPAddress -like "100.*") { "밖에서 (Tailscale)" } else { "같은 와이파이" }
        Write-Host ("  {0,-18} http://{1}:{2}   [{3}]" -f $kind, $_.IPAddress, $Port, $_.InterfaceAlias)
    }
