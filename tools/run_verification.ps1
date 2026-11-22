# Tools: run_verification.ps1
# Runs the Space-Modernized Milestone 5 Verification Suite

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
$mainClasses = "versions\1.20.1-forge\build\classes\java\main"
$cp = "$outDir;$mainClasses;" + $rawCp

Write-Host "Compiling test sources..."
& $javac -cp $cp -d $outDir `
    src/main/java/com/space_m/SpaceMod.java `
    src/main/java/com/space_m/rocket/client/RocketModels.java `
    src/main/java/com/space_m/rocket/client/RocketPartRenderer.java `
    src/main/java/com/space_m/celestial/CelestialBody.java `
    src/main/java/com/space_m/celestial/SolarSystem.java `
    src/main/java/com/space_m/rocket/RocketPart.java `
    src/main/java/com/space_m/rocket/RocketStruct.java `
    src/main/java/com/space_m/rocket/entity/EntityRideableRocket.java `
    src/main/java/com/space_m/rocket/transport/EntityCombatDropPod.java `
    src/main/java/com/space_m/rocket/transport/client/CombatDropPodRenderer.java `
    src/main/java/com/space_m/rocket/client/RenderDropPod.java `
    src/main/java/com/space_m/rocket/client/RocketPronter.java `
    src/main/java/com/space_m/entity/mob/client/SpaceMobVbos.java `
    src/main/java/com/space_m/entity/mob/client/ObjMobModel.java `
    src/main/java/com/space_m/entity/mob/client/SpaceMobModels.java `
    src/main/java/com/space_m/fork/trinket/client/ForkTrinketRenderer.java `
    src/test/java/com/space_m/test/ModItemStackHandlerDummyTest.java `
    src/test/java/com/space_m/test/MachineRenderRegistryTest.java `
    src/test/java/com/space_m/test/ChallengerAdversarialStressSuite.java `
    src/test/java/com/space_m/test/RocketVboMigrationTest.java `
    src/test/java/com/space_m/test/MobTrinketVboMigrationTest.java `
    src/test/java/com/space_m/test/SpaceM5VerificationSuite.java

if ($LASTEXITCODE -ne 0) {
    Write-Error "Test compilation failed with exit code $LASTEXITCODE"
    exit $LASTEXITCODE
}

Write-Host "Executing SpaceM5VerificationSuite..."
$slf4j2 = "C:\Users\user\.gradle\caches\modules-2\files-2.1\org.slf4j\slf4j-api\2.0.1\f48d81adce2abf5ad3cfe463df517952749e03bc\slf4j-api-2.0.1.jar"
$runCp = "$outDir;$mainClasses;$slf4j2;$rawCp"

& $java -cp $runCp com.space_m.test.SpaceM5VerificationSuite
exit $LASTEXITCODE
