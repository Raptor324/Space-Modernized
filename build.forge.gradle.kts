plugins {
	id("mod-platform")
	id("net.neoforged.moddev.legacyforge")
}

platform {
	loader = "forge"
	dependencies {
		required("minecraft") {
			forgeVersionRange = "[${prop("deps.minecraft")}]"
		}
		required("forge") {
			forgeVersionRange = "[47.3.31,)"
		}
		required("architectury") {
			slug("architectury-api")
			forgeVersionRange = "[${prop("deps.architectury")},)"
		}
		required("hbm_m") {
			slug("hbms-nuclear-tech-modernized")
			// mods.toml compares against hbm_m's own version ("0.2.2-alpha"), not the artifact
			// version "0.2.2-alpha+1.20.1-forge" - that one sorts higher and would reject the match.
			forgeVersionRange = "[${prop("deps.hbm_m").substringBefore('+')},)"
			// Space content builds on hbm_m registries/events -> always load after it.
			ordering = "AFTER"
		}
	}
}

legacyForge {
	version = "${property("deps.minecraft")}-${property("deps.forge")}"
	if (hasProperty("deps.parchment")) parchment {
		val parchmentProp = property("deps.parchment") as String
		val parts = parchmentProp.split(":")
		mappingsVersion = parts[1]
		minecraftVersion = parts[0]
	}
	validateAccessTransformers = true
	accessTransformers.from(rootProject.file("src/main/resources/META-INF/accesstransformer.cfg"))

	runs {
		register("client") {
			client()
			gameDirectory = file("run/")
			ideName = "Forge Client (${stonecutter.active?.version})"
			programArgument("--username=Dev")
			jvmArguments.addAll("-Xmx4G", "-Xms2G", "-Dfile.encoding=UTF-8", "-Dconsole.encoding=UTF-8",
				"-Duser.language=en", "-Duser.country=US")
		}
		register("server") {
			server()
			gameDirectory = file("run/")
			ideName = "Forge Server (${stonecutter.active?.version})"
			programArgument("--nogui")
			jvmArguments.addAll("-Xmx4G", "-Xms2G", "-Dfile.encoding=UTF-8",
				"-Duser.language=en", "-Duser.country=US")
		}
		// Headless server start without eula.txt (GameTestServer bypasses Main): loads hbm_m + space_m,
		// runs the space_m game tests (none yet) and exits. ./gradlew :1.20.1-forge:runGameTestServer
		register("gameTestServer") {
			type = "gameTestServer"
			gameDirectory = file("run/")
			ideName = "Forge GameTest (${stonecutter.active?.version})"
			systemProperty("forge.enabledGameTestNamespaces", prop("mod.id"))
			jvmArguments.addAll("-Xmx4G", "-Xms2G", "-Dfile.encoding=UTF-8")
		}
		register("data") {
			data()
			gameDirectory = file("run/")
			ideName = "Forge Data (${stonecutter.active?.version})"
			jvmArguments.addAll("-Xmx4G", "-Xms2G", "-Dfile.encoding=UTF-8", "-Dconsole.encoding=UTF-8")
			val datagenOutput = rootProject.file("src/generated/resources")
			val existingResources = rootProject.file("src/main/resources")
			programArgument("--mod")
			programArgument(prop("mod.id"))
			programArgument("--all")
			programArgument("--output")
			programArgument(datagenOutput.absolutePath)
			programArgument("--existing")
			programArgument(existingResources.absolutePath)
			// Modelle/Texturen von HBM-Modernized (Eltern-Modelle, gemeinsame Texturen) fuer den ExistingFileHelper.
			programArgument("--existing-mod")
			programArgument("hbm_m")
		}
	}

	mods {
		register(prop("mod.id")) {
			sourceSet(sourceSets["main"])
		}
	}
}

mixin {
	add(sourceSets.main.get(), prop("mod.mixin_refmap"))
	config(prop("mod.mixin_config"))
}

repositories {
	mavenLocal()
	mavenCentral()
	maven("https://maven.minecraftforge.net/") { name = "MinecraftForge" }
	maven("https://maven.parchmentmc.org") { name = "ParchmentMC" }
	strictMaven("https://api.modrinth.com/maven", "maven.modrinth") { name = "Modrinth" }
	strictMaven("https://cursemaven.com", "curse.maven") { name = "CurseForge" }
	strictMaven("https://maven.architectury.dev/", "dev.architectury") { name = "Architectury" }
}

dependencies {
	annotationProcessor("org.spongepowered:mixin:${libs.versions.mixin.get()}:processor")

	// Architectury — compile+runtime, NOT jarJar (HBM-Modernized already bundles it)
	"modImplementation"("dev.architectury:architectury-forge:${prop("deps.architectury")}")

	// HBM-Modernized — required dependency.
	// If hbm.local.path is set in local.properties, a local jar is used;
	// otherwise it is pulled from Modrinth (for CI and other developers).
	"modImplementation"(hbmDependency(stonecutter.current.project))

	// JEI (nur Compile, R2-C Atmo-Maschinen: Space-Kategorien auf Basis der HBM-JEI-Kategorien). Version wie HBM deps.jei.
	"modCompileOnly"("curse.maven:jei-238222:7270446")
	// Dev-Laufzeit (nur runClient/runServer, nicht im Jar): JEI + WorldEdit zum Testen
	"modRuntimeOnly"("curse.maven:jei-238222:7270446")
	"modRuntimeOnly"("maven.modrinth:worldedit:Wrlqaul6") // WorldEdit 7.2.15 Forge 1.20.1
}

sourceSets {
	main {
		resources.srcDir(rootProject.file("src/generated/resources"))
	}
}

tasks.named("createMinecraftArtifacts") {
	dependsOn(tasks.named("stonecutterGenerate"))
}

tasks.named<Jar>("jar") {
	exclude("com/${prop("mod.id")}/datagen/**")
}

tasks.withType<JavaCompile>().configureEach { options.encoding = "UTF-8" }

stonecutter {
}
