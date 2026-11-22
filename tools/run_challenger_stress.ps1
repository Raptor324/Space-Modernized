# Tools: run_challenger_stress.ps1
# Runs the Challenger 1 Adversarial Stress Suite for Milestone 1

$ErrorActionPreference = "Stop"
$env:JAVA_HOME = "C:\Program Files\Eclipse Adoptium\jdk-17.0.17.10-hotspot"
$java = "$env:JAVA_HOME\bin\java.exe"
$javac = "$env:JAVA_HOME\bin\javac.exe"

$outDir = "versions\1.20.1-forge\build\test_classes"
New-Item -ItemType Directory -Force -Path $outDir | Out-Null

if (-not (Test-Path "versions\1.20.1-forge\build\compile_cp.txt")) {
    Write-Host "Extracting compile classpath from Gradle..."
    @'
allprojects {
    afterEvaluate { p ->
        if (p.name == "1.20.1-forge") {
            def cp = p.configurations.compileClasspath.files.collect { it.absolutePath }
            new File(p.buildDir, "compile_cp.txt").text = cp.join(File.pathSeparator)
        }
    }
}
'@ | Set-Content -Path "dump_cp.gradle"
    .\gradlew.bat -q :1.20.1-forge:help -I dump_cp.gradle
    Remove-Item "dump_cp.gradle"
}

$rawCp = Get-Content "versions\1.20.1-forge\build\compile_cp.txt"
$cp = "$outDir;" + $rawCp

Write-Host "Compiling Challenger stress suite..."
& $javac -cp $cp -d $outDir `
    src/test/java/com/space_m/test/ChallengerAdversarialStressSuite.java

if ($LASTEXITCODE -ne 0) {
    Write-Error "Test compilation failed with exit code $LASTEXITCODE"
    exit $LASTEXITCODE
}

Write-Host "Executing ChallengerAdversarialStressSuite..."
$slf4j2 = "C:\Users\user\.gradle\caches\modules-2\files-2.1\org.slf4j\slf4j-api\2.0.1\f48d81adce2abf5ad3cfe463df517952749e03bc\slf4j-api-2.0.1.jar"
$runCp = "$outDir;$slf4j2;$rawCp"

& $java -cp $runCp com.space_m.test.ChallengerAdversarialStressSuite
exit $LASTEXITCODE
