$ProgressPreference = 'SilentlyContinue'
[Net.ServicePointManager]::SecurityProtocol = 'Tls12'

$url = 'https://repo1.maven.org/maven2/gradle/wrapper/gradle-wrapper/gradle-wrapper.jar'
$dest = 'D:\neighborhood-helper\gradle\wrapper\gradle-wrapper.jar'

Write-Host "Downloading gradle-wrapper.jar..."
Write-Host "URL: $url"
Write-Host "Destination: $dest"

try {
    $client = New-Object Net.WebClient
    $client.DownloadFile($url, $dest)

    if (Test-Path $dest) {
        $size = (Get-Item $dest).Length
        Write-Host "Success! Downloaded $size bytes"
    } else {
        Write-Host "File not created"
    }
} catch {
    Write-Host "Error: $_"
}

