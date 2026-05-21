Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

Push-Location $PSScriptRoot\..
try {
    Write-Host "==> Maven tests"
    mvn clean test

    Write-Host "==> Checkstyle"
    mvn checkstyle:check

    Write-Host "==> Package"
    mvn -DskipTests package

    Write-Host "==> CLI smoke test"
    java -jar target/books-crawler-java-1.0.0.jar --delay-ms 0 --max-books 2 --output-dir output-validation

    Write-Host "==> Docker Compose config validation"
    docker compose config | Out-Null

    Write-Host "==> Dynamic bonus"
    npm install
    npm run dynamic:scrape

    if (Get-Command ollama -ErrorAction SilentlyContinue) {
        try {
            Write-Host "==> AI bonus"
            Start-Process -FilePath ollama -ArgumentList "serve" -WindowStyle Hidden | Out-Null
            Start-Sleep -Seconds 3
            $env:BOOKS_AI_ENABLED = "true"
            $env:BOOKS_AI_MODEL = "qwen2.5-coder:7b"
            $env:BOOKS_AI_BASE_URL = "http://127.0.0.1:11434/v1"
            java -jar target/books-crawler-java-1.0.0.jar --delay-ms 0 --max-books 1 --output-dir output-ai-validation
        } catch {
            Write-Warning "AI validation skipped: $($_.Exception.Message)"
        }
    } else {
        Write-Warning "Ollama not installed. AI validation skipped."
    }

    Write-Host "Validation completed successfully."
} finally {
    Pop-Location
}
