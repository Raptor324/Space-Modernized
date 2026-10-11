@file:Suppress("unused", "DuplicatedCode")

import dev.kikugie.stonecutter.build.StonecutterBuildExtension
import me.modmuss50.mpp.ModPublishExtension
import me.modmuss50.mpp.ReleaseType
import org.gradle.api.file.DuplicatesStrategy
import org.gradle.api.JavaVersion
import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.dsl.RepositoryHandler
import org.gradle.api.artifacts.repositories.MavenArtifactRepository
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Copy
import org.gradle.api.tasks.javadoc.Javadoc
import org.gradle.external.javadoc.StandardJavadocDocletOptions
import org.gradle.internal.extensions.stdlib.toDefaultLowerCase
import org.gradle.jvm.tasks.Jar
import org.gradle.kotlin.dsl.*
import org.gradle.language.jvm.tasks.ProcessResources
import org.gradle.plugins.ide.idea.model.IdeaModel
import java.io.File
import java.util.*
import javax.inject.Inject

fun Project.prop(name: String): String = (findProperty(name) ?: "") as String

fun Project.env(variable: String): String? = providers.environmentVariable(variable).orNull

fun Project.envTrue(variable: String): Boolean = env(variable)?.toDefaultLowerCase() == "true"

fun RepositoryHandler.strictMaven(
	url: String, vararg groups: String, configure: MavenArtifactRepository.() -> Unit = {}
) = exclusiveContent {
	forRepository { maven(url) { configure() } }
	filter { groups.forEach(::includeGroup) }
}

/**
 * Returns the root directory of a local HBM-Modernized clone if [hbm.local.path] is set
 * in [local.properties], or null otherwise.
 */
fun Project.hbmLocalPath(): File? {
	// settings.gradle.kts copies local.properties into gradle.extra - that is NOT visible to
	// Project.findProperty, so check -P/gradle.properties, then gradle.extra, then the file itself.
	val fromProject = findProperty("hbm.local.path") as? String
	val gradleExtra = gradle.extensions.extraProperties
	val fromGradle = if (gradleExtra.has("hbm.local.path")) gradleExtra.get("hbm.local.path") as? String else null
	val fromFile = rootProject.file("local.properties").takeIf(File::isFile)?.let { f ->
		Properties().also { p -> f.inputStream().use(p::load) }.getProperty("hbm.local.path")
	}
	return listOf(fromProject, fromGradle, fromFile).firstNotNullOfOrNull { it?.trim()?.ifBlank { null } }?.let { File(it) }
}

/**
 * Expected jar name of the local HBM-Modernized clone for [stonecutterProject], named the same
 * way as its ModPlatformPlugin does: `hbm_m-<mod.version><mod.channel_tag>+<deps.minecraft>-<loader>.jar`.
 * Values come from `<clone>/gradle.properties`, overridden by `<clone>/versions/<project>/gradle.properties`.
 * Returns null if mod.version cannot be read.
 */
fun hbmExpectedJarName(localRoot: File, stonecutterProject: String): String? {
	val props = Properties()
	listOf(File(localRoot, "gradle.properties"), File(localRoot, "versions/$stonecutterProject/gradle.properties"))
		.filter(File::isFile)
		.forEach { f -> f.inputStream().use { props.load(it) } }
	val modId = props.getProperty("mod.id")?.trim()?.ifBlank { null } ?: "hbm_m"
	val version = props.getProperty("mod.version")?.trim()?.ifBlank { null } ?: return null
	val channel = props.getProperty("mod.channel_tag")?.trim().orEmpty()
	val mc = props.getProperty("deps.minecraft")?.trim()?.ifBlank { null } ?: stonecutterProject.substringBefore('-')
	val loader = stonecutterProject.substringAfter('-')
	return "$modId-$version$channel+$mc-$loader.jar"
}

/**
 * Task in the HBM-Modernized clone that writes the consumable jar into build/libs:
 * legacy Forge (MDG) puts the plain `jar` (mojmap) into build/devlibs and the SRG jar
 * from `reobfJar` into build/libs; NeoForge's `jar` goes straight to build/libs.
 */
/** Pseudo group of the local HBM-Modernized jar (only served by the flat-dir repository). */
const val HBM_LOCAL_GROUP = "hbm_local"

fun hbmJarTask(stonecutterProject: String): String =
	if (stonecutterProject.endsWith("-forge")) "reobfJar" else "jar"

/**
 * Resolves the HBM-Modernized dependency:
 *  - exactly one local jar from `<hbm.local.path>/versions/<stonecutterProject>/build/libs/`
 *    (the one matching the clone's mod.version, else the newest) when [hbm.local.path] is set in [local.properties];
 *  - otherwise the Modrinth coordinates `maven.modrinth:hbms-nuclear-tech-modernized:<deps.hbm_m>`.
 *
 * @param stonecutterProject the value of `stonecutter.current.project` (e.g. "1.20.1-forge")
 */
fun Project.hbmDependency(stonecutterProject: String): Any {
	val localRoot = hbmLocalPath()
	if (localRoot == null) {
		return "maven.modrinth:hbms-nuclear-tech-modernized:${prop("deps.hbm_m")}"
	}
	if (!localRoot.isDirectory) {
		logger.warn(
			"[Space-Modernized] hbm.local.path points to a non-existent directory: " +
			"${localRoot.absolutePath}. Falling back to the Modrinth artifact " +
			"(deps.hbm_m=${prop("deps.hbm_m")})."
		)
		return "maven.modrinth:hbms-nuclear-tech-modernized:${prop("deps.hbm_m")}"
	}

	val libsDir = File(localRoot, "versions/$stonecutterProject/build/libs")
	// Auxiliary artifacts are not valid compile/runtime dependencies:
	// -dev uses mojmap/remapped names, -sources/-javadoc are not binaries.
	val isAuxJar = { f: File ->
		listOf("-sources", "-javadoc", "-dev", "-reobf").any { f.name.endsWith("$it.jar") }
	}
	val candidateJars = libsDir.listFiles(File::isFile)?.filter { it.extension == "jar" && !isAuxJar(it) }

	if (candidateJars != null && candidateJars.isNotEmpty()) {
		// build/libs keeps every jar ever built (0.1.3 ... 0.3.5) - putting all of them on the
		// classpath gives duplicate classes and a random hbm_m version at runtime.
		// Pick exactly one: the jar matching the clone's current mod.version, else the newest one.
		val expected = hbmExpectedJarName(localRoot, stonecutterProject)
		val exact = expected?.let { name -> candidateJars.firstOrNull { it.name == name } }
		val chosen = exact ?: candidateJars.maxByOrNull { it.lastModified() }!!
		if (exact == null) {
			logger.warn(
				"[Space-Modernized] No HBM-Modernized jar named '${expected ?: "?"}' in ${libsDir.absolutePath} " +
				"(mod.version of the local clone). Using the newest jar instead: ${chosen.name}. " +
				"Build the clone first (:$stonecutterProject:${hbmJarTask(stonecutterProject)})."
			)
		} else {
			logger.lifecycle("[Space-Modernized] Using local HBM-Modernized jar: ${chosen.absolutePath}")
		}
		if (candidateJars.size > 1) {
			logger.info("[Space-Modernized] Ignoring ${candidateJars.size - 1} other jar(s) in ${libsDir.absolutePath}")
		}
		// Served as a module from a flat-dir repository, not as files(): legacy Forge's
		// modImplementation (SRG -> mojmap remapping) only accepts module dependencies
		// ("Cannot convert the provided notation to an object of type DependencyConstraint: file collection").
		val localRepo = repositories.flatDir {
			name = "HbmModernizedLocal"
			dirs(libsDir)
		}
		repositories.exclusiveContent {
			forRepositories(localRepo)
			filter { includeGroup(HBM_LOCAL_GROUP) }
		}
		val base = chosen.nameWithoutExtension
		return if (base.startsWith("hbm_m-")) "$HBM_LOCAL_GROUP:hbm_m:${base.removePrefix("hbm_m-")}"
		else "$HBM_LOCAL_GROUP:$base"
	}
	logger.warn(
		"[Space-Modernized] hbm.local.path is set, but no usable jar was found in " +
		"${libsDir.absolutePath}. Run './gradlew :$stonecutterProject:${hbmJarTask(stonecutterProject)}' in the " +
		"HBM-Modernized clone, or clear hbm.local.path to use the Modrinth artifact. " +
		"Falling back to Modrinth (deps.hbm_m=${prop("deps.hbm_m")})."
	)
	return "maven.modrinth:hbms-nuclear-tech-modernized:${prop("deps.hbm_m")}"
}

abstract class ModPlatformPlugin @Inject constructor() : Plugin<Project> {
	override fun apply(project: Project) = with(project) {
		val inferredLoader = project.buildFile.name.substringAfter('.').replace(".gradle.kts", "")

		val extension = extensions.create("platform", ModPlatformExtension::class.java).apply {
			loader.convention(inferredLoader)
			jarTask.convention("jar")
			sourcesJarTask.convention("sourcesJar")
		}

		afterEvaluate {
			configureProject(extension)
		}
	}

	private fun Project.configureProject(extension: ModPlatformExtension) {
		val loader = extension.loader.get()
		val isNeoForge = loader == "neoforge"
		val isForge = loader == "forge"

		val modId = prop("mod.id")
		val modVersion = prop("mod.version")
		val channelTag = prop("mod.channel_tag")
		val mcVersion = prop("deps.minecraft")

		val stonecutter = extensions.getByType<StonecutterBuildExtension>()

		listOf(
			"java",
			"me.modmuss50.mod-publish-plugin",
			"idea",
		).forEach { apply(plugin = it) }

		version = "$modVersion$channelTag+$mcVersion-$loader"

		extension.requiredJava.set(
			when {
				stonecutter.eval(stonecutter.current.version, ">=1.20.6") -> JavaVersion.VERSION_21
				stonecutter.eval(stonecutter.current.version, ">=1.18") -> JavaVersion.VERSION_17
				else -> JavaVersion.VERSION_1_8
			}
		)

		configureJarTask(modId, loader)
		configureIdea()
		configureProcessResources(isNeoForge, isForge, modId, "$modVersion$channelTag", mcVersion, extension, extension.requiredJava.get())
		configureJava(stonecutter, extension.requiredJava.get())
		registerBuildAndCollectTask(extension, "$modVersion$channelTag")
		configurePublishing(extension, loader, stonecutter, "$modVersion$channelTag", channelTag, version.toString())
	}

	private fun Project.configureJarTask(modId: String, loader: String) {
		val isForge = loader == "forge"

		tasks.withType<Jar>().configureEach {
			archiveBaseName.set(modId)
			duplicatesStrategy = DuplicatesStrategy.EXCLUDE
			if (isForge) {
				manifest.attributes(
					"MixinConfigs" to prop("mod.mixin_config")
				)
			}
		}
	}

	private fun Project.configureProcessResources(
		isNeoForge: Boolean,
		isForge: Boolean,
		modId: String,
		modVersion: String,
		mcVersion: String,
		extension: ModPlatformExtension,
		requiredJava: JavaVersion
	) {
		tasks.named<ProcessResources>("processResources") {
			dependsOn(tasks.named("stonecutterGenerate"))
			duplicatesStrategy = DuplicatesStrategy.EXCLUDE

			from(rootProject.files("LICENSE", "NOTICE.md")) {
				into("META-INF")
			}

			// Template "JAVA_${java}" in mixins.<mod_id>.json (the old pattern "*.mixins.json" never matched it,
			// and the value carried a second "JAVA_" prefix -> Mixin rejected "JAVA_${JAVA}").
			filesMatching(listOf("*.mixins.json", prop("mod.mixin_config")).filter(String::isNotBlank)) {
				expand("java" to requiredJava.majorVersion)
			}

			val authors = prop("mod.authors")
			val contributors = prop("mod.contributors")
			var issuesUrl = prop("mod.issues_url")
			if (issuesUrl == "") issuesUrl = prop("mod.sources_url") + "/issues"

			val dependencies = buildDependenciesBlock(modId, extension.dependencies)

			// NeoForge 21.x ships FML 4.x - "[47,)" (Forge 1.20.1) makes it reject the mod file.
			val javafmlRange = prop("deps.javafml_range").ifBlank { if (isNeoForge) "[4,)" else "[47,)" }
			val forgeDepRange = prop("deps.forge_dependency_range").ifBlank { "[1,)" }
			// Old default "[$mcVersion,1.21)" is an invalid range on 1.21.1 ("Range defies version ordering"),
			// NeoForge then drops space_m from the mod list. Default to the exact version like hbm_m does.
			val minecraftDepRange = prop("deps.minecraft_dependency_range").ifBlank { "[$mcVersion]" }

			val props = mapOf(
				"version" to modVersion,
				"minecraft" to mcVersion,
				"id" to modId,
				"name" to prop("mod.name"),
				"group" to prop("mod.group"),
				"authors" to authors,
				"contributors" to contributors,
				"license" to prop("mod.license"),
				"description" to prop("mod.description"),
				"issues_url" to issuesUrl,
				"homepage_url" to prop("mod.homepage_url"),
				"sources_url" to prop("mod.sources_url"),
				"discord_url" to prop("mod.discord_url"),
				"dependencies" to dependencies,
				"mixin_config" to prop("mod.mixin_config"),
				"javafml_range" to javafmlRange,
				"loader_version_range" to javafmlRange,
				"mod_license" to prop("mod.license"),
				"mod_id" to modId,
				"mod_version" to modVersion,
				"mod_name" to prop("mod.name"),
				"mod_authors" to authors,
				"mod_description" to prop("mod.description"),
				"forge_version_range" to forgeDepRange,
				"minecraft_version_range" to minecraftDepRange,
			)

			when {
				isNeoForge -> {
					filesMatching("META-INF/neoforge.mods.toml") { expand(props) }
					exclude("META-INF/mods.toml", "aw/*.cfg", ".cache", "pack.mcmeta")
				}

				isForge -> {
					filesMatching("META-INF/mods.toml") { expand(props) }
					exclude("META-INF/neoforge.mods.toml", "aw/*.accesswidener", ".cache")
				}
			}
		}
	}

	private fun buildDependenciesBlock(
		modId: String, deps: DependenciesConfig
	): String = buildString {
		fun appendBlock(container: NamedDomainObjectContainer<Dependency>, type: String) {
			container.forEach {
				appendLine(
					"""

					[[dependencies.$modId]]
					modId = "${it.modid.get()}"
					side = "${it.environment.get().uppercase(Locale.getDefault())}"
                    versionRange = "${it.forgeVersionRange.get()}"
                    ordering = "${it.ordering.get()}"
					mandatory = ${if (type == "required") "true" else "false"}
                    type = "$type"
					""".replace("                  ", "").trimIndent()
				)
			}
		}

		appendBlock(deps.required, "required")
		appendBlock(deps.optional, "optional")
		appendBlock(deps.incompatible, "incompatible")
	}

	private fun Project.configureJava(stonecutter: StonecutterBuildExtension, requiredJava: JavaVersion) {
		extensions.configure<JavaPluginExtension>("java") {
			withSourcesJar()
			withJavadocJar()
			sourceCompatibility = requiredJava
			targetCompatibility = requiredJava
		}
		tasks.withType<Javadoc>().configureEach {
			isFailOnError = false
			(options as StandardJavadocDocletOptions).encoding = "UTF-8"
			(options as StandardJavadocDocletOptions).charSet = "UTF-8"
		}
	}

	private fun Project.configureIdea() {
		extensions.configure<IdeaModel>("idea") {
			module {
				isDownloadJavadoc = true
				isDownloadSources = true
			}
		}
	}

	private fun Project.registerBuildAndCollectTask(extension: ModPlatformExtension, modVersion: String) {
		tasks.register<Copy>("buildAndCollect") {
			group = "build"
			from(
				tasks.named(extension.jarTask.get()),
				tasks.named(extension.sourcesJarTask.get()),
				tasks.named("javadocJar").get()
			)
			into(rootProject.layout.buildDirectory.file("libs/$modVersion"))
			dependsOn("build")
		}
	}

	private fun Project.configurePublishing(
		ext: ModPlatformExtension,
		loader: String,
		stonecutter: StonecutterBuildExtension,
		modVersion: String,
		channelTag: String,
		fullVersion: String,
	) {
		val additionalVersions = (findProperty("publish.additionalVersions") as String?)?.split(',')?.map(String::trim)
			?.filter(String::isNotEmpty).orEmpty()

		val releaseType = ReleaseType.of(
			channelTag.substringAfter('-').substringBefore('.').ifEmpty { "stable" })

		extensions.configure<ModPublishExtension>("publishMods") {
			val modrinthAccessToken = env("MODRINTH_API_TOKEN")
			val curseforgeAccessToken = env("CURSEFORGE_API_TOKEN")
			if (!envTrue("ENABLE_PUBLISHING")) {
				dryRun = true
			}

			val isForge = loader == "forge"
			val targetName = if (isForge) "reobfJar" else ext.jarTask.get()

			val jarTask = tasks.named(targetName).map { it as Jar }
			val srcJarTask = tasks.named(ext.sourcesJarTask.get()).map { it as Jar }
			val currentVersion = stonecutter.current.version
			val deps = ext.dependencies

			file.set(jarTask.flatMap(Jar::getArchiveFile))
			additionalFiles.from(srcJarTask.flatMap(Jar::getArchiveFile))
			type = releaseType
			version = fullVersion
			val changelogFile = rootProject.file("CHANGELOG.md")
			changelog.set(if (changelogFile.exists()) changelogFile.readText() else "")
			modLoaders.add(loader)

			displayName = "${prop("mod.name")} $modVersion ${loader.replaceFirstChar(Char::titlecase)} $currentVersion"

			modrinth {
				projectId = project.prop("publish.modrinth")
				accessToken = modrinthAccessToken
				minecraftVersions.addAll(listOf(currentVersion) + additionalVersions)

				deps.required.forEach { dep -> if (!dep.modrinth.orNull.isNullOrBlank()) requires(dep.modrinth.get()) }
				deps.optional.forEach { dep -> if (!dep.modrinth.orNull.isNullOrBlank()) optional(dep.modrinth.get()) }
				deps.incompatible.forEach { dep -> if (!dep.modrinth.orNull.isNullOrBlank()) incompatible(dep.modrinth.get()) }
				deps.embeds.forEach { dep -> if (!dep.modrinth.orNull.isNullOrBlank()) embeds(dep.modrinth.get()) }
			}

			curseforge {
				projectId = project.prop("publish.curseforge")
				accessToken = curseforgeAccessToken
				minecraftVersions.addAll(listOf(currentVersion) + additionalVersions)

				deps.required.forEach { dep -> if (!dep.curseforge.orNull.isNullOrBlank()) requires(dep.curseforge.get()) }
				deps.optional.forEach { dep -> if (!dep.curseforge.orNull.isNullOrBlank()) optional(dep.curseforge.get()) }
				deps.incompatible.forEach { dep -> if (!dep.curseforge.orNull.isNullOrBlank()) incompatible(dep.curseforge.get()) }
				deps.embeds.forEach { dep -> if (!dep.curseforge.orNull.isNullOrBlank()) embeds(dep.curseforge.get()) }
			}
		}
	}
}
