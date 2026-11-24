$ErrorActionPreference = "Stop"
if (-not $env:CMS_CLIENT_SECRET) { throw "Set CMS_CLIENT_SECRET before running." }
java -jar "$PSScriptRoot/target/cms-cli-consumer-1.0.0.jar"
