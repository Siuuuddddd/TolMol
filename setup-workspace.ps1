[CmdletBinding()]
param(
    [string]$BackendJavaHome = 'C:/Program Files/Java/jdk-21',
    [string]$AndroidJavaHome = 'C:/Program Files/Android/Android Studio/jbr'
)

$ErrorActionPreference = 'Stop'
foreach ($javaHome in @($BackendJavaHome, $AndroidJavaHome)) {
    if (!(Test-Path -LiteralPath (Join-Path $javaHome 'bin/java.exe'))) {
        throw "Java was not found at $javaHome. Pass the installed JDK path to this script."
    }
}

function Set-GradleProject($folder, $buildPath, $javaHome) {
    $directory = Join-Path $PSScriptRoot $folder
    $path = Join-Path $directory '.idea/gradle.xml'
    if (Test-Path -LiteralPath $path) {
        [xml]$document = Get-Content -Raw -LiteralPath $path
    } else {
        [xml]$document = '<project version="4"/>'
    }
    $component = $document.SelectSingleNode('/project/component[@name="GradleSettings"]')
    if (!$component) {
        $component = $document.CreateElement('component')
        $component.SetAttribute('name', 'GradleSettings')
        $null = $document.project.AppendChild($component)
    }
    $links = $component.SelectSingleNode('option[@name="linkedExternalProjectsSettings"]')
    if (!$links) {
        $links = $document.CreateElement('option')
        $links.SetAttribute('name', 'linkedExternalProjectsSettings')
        $null = $component.AppendChild($links)
    }
    $settings = $links.SelectSingleNode('GradleProjectSettings')
    if (!$settings) {
        $settings = $document.CreateElement('GradleProjectSettings')
        $null = $links.AppendChild($settings)
    }
    foreach ($entry in @{externalProjectPath=$buildPath; gradleJvm='#GRADLE_LOCAL_JAVA_HOME'}.GetEnumerator()) {
        $option = $settings.SelectSingleNode("option[@name='$($entry.Key)']")
        if (!$option) {
            $option = $document.CreateElement('option')
            $option.SetAttribute('name', $entry.Key)
            $null = $settings.AppendChild($option)
        }
        $option.SetAttribute('value', $entry.Value)
    }
    foreach ($module in $settings.SelectNodes('option[@name="modules"]/set/option')) {
        if ($module.GetAttribute('value') -eq '$PROJECT_DIR$') {
            $module.SetAttribute('value', $buildPath)
        }
    }
    New-Item -ItemType Directory -Force -Path (Split-Path -Parent $path) | Out-Null
    $document.Save($path)
    $propertiesFolder = Join-Path $directory '.gradle'
    New-Item -ItemType Directory -Force -Path $propertiesFolder | Out-Null
    [IO.File]::WriteAllText((Join-Path $propertiesFolder 'config.properties'),
        'java.home=' + $javaHome.Replace('\','/') + "`n")
}

Set-GradleProject '.' '$PROJECT_DIR$/backend' $BackendJavaHome
Set-GradleProject 'backend' '$PROJECT_DIR$' $BackendJavaHome
Set-GradleProject 'android' '$PROJECT_DIR$' $AndroidJavaHome
Write-Output 'Gradle workspace links and local Java homes are configured. Reload the IDE projects.'
