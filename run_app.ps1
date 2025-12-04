Write-Host "Starting Scientific Calculator V1..." -ForegroundColor Cyan

# Get the current directory
$projectRoot = Get-Location

# 1. Start the Backend Server in a new PowerShell window
Write-Host "Launching Backend Server (Maven)..." -ForegroundColor Green
Start-Process powershell -ArgumentList "-NoExit", "-Command", "cd '$projectRoot'; mvn compile exec:java"

# 2. Start the Frontend in a new PowerShell window
Write-Host "Launching Frontend (React)..." -ForegroundColor Green
Start-Process powershell -ArgumentList "-NoExit", "-Command", "cd '$projectRoot\calculator-ui-react'; npm run dev"

Write-Host "------------------------------------------------" -ForegroundColor Cyan
Write-Host "Both processes have been started in new windows." -ForegroundColor Cyan
Write-Host "Backend will run on http://localhost:8080" -ForegroundColor Gray
Write-Host "Frontend will run on http://localhost:5173" -ForegroundColor Gray
Write-Host "------------------------------------------------" -ForegroundColor Cyan
