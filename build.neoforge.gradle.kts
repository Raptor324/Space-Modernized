plugins {
	id("mod-platform")
	id("net.neoforged.moddev")
}

platform {
	loader = "neoforge"
	dependencies {
		required("minecraft") {
			forgeVersionRange = "[${prop("deps.minecraft")}]"
		}
		required("neoforge") {
			forgeVersionRange = "[1,)"
		}
		required("architectury") {
			slug("architectury-api")
			forgeVersionRange = "[${prop("deps.architectury")},)"
		}
		required("hbm_m") {
			slug("hbms-nuclear-tech-modernized")
			forgeVersionRange = "[${prop("deps.hbm_m")},)"
		}
	}
}

neoForge {
	version = property("deps.neoforge") as String
	accessTransformers.from(rootProject.file("src/main/resources/META-INF/accesstransformer.cfg"))
	validateAccessTransformers = true

	if (hasProperty("deps.parchment")) parchment {
		val (mc, ver) = (property("deps.parchment") as String).split(':')
		mappingsVersion = ver
		minecraftVersion = mc
	}

	runs {
		register("client") {
			client()
			gameDirectory = file("run/")
			ideName = "NeoForge Client (${stonecutter.active?.version})"
			programArgument("--username=Dev")
			jvmArguments.addAll("-Xmx4G", "-Xms2G", "-Dfile.encoding=UTF-8", "-Dconsole.encoding=UTF-8",
				"-Duser.language=en", "-Duser.country=US")
		}
		register("server") {
			server()
			gameDirectory = file("run/")
			ideName = "NeoForge Server (${stonecutter.active?.version})"
			jvmArguments.addAll("-Xmx4G", "-Xms2G", "-Dfile.encoding=UTF-8",
				"-Duser.language=en", "-Duser.country=US")
		}
		// Datagen is 1.20.1 Forge-only. NeoForge picks up the already generated
		// resources from src/generated/resources and remaps them via processResources (see below).
	}

	mods {
		register(property("mod.id") as String) {
			sourceSet(sourceSets["main"])
		}
	}
	sourceSets["main"].resources.srcDir(rootProject.file("src/generated/resources"))
}

repositories {
	mavenCentral()
	maven("https://maven.parchmentmc.org") { name = "ParchmentMC" }
	strictMaven("https://api.modrinth.com/maven", "maven.modrinth") { name = "Modrinth" }
	strictMaven("https://maven.architectury.dev/", "dev.architectury") { name = "Architectury" }
}

dependencies {
	// Architectury — compile+runtime, NOT jarJar (HBM-Modernized already bundles it)
	implementation("dev.architectury:architectury-neoforge:${prop("deps.architectury")}")

	// HBM-Modernized — required dependency.
	// If hbm.local.path is set in local.properties, a local jar is used;
	// otherwise it is pulled from Modrinth (for CI and other developers).
	// NeoForge MDG has no modImplementation — compileOnly+runtimeOnly is equivalent.
	val hbmDep = hbmDependency(stonecutter.current.project)
	"compileOnly"(hbmDep)
	"runtimeOnly"(hbmDep)
}

tasks.named("createMinecraftArtifacts") {
	dependsOn(tasks.named("stonecutterGenerate"))
}

// On 1.21+ data-pack directories were renamed from plural to singular
// (recipes → recipe, tags/blocks → tags/block, etc.), the ItemStack codec
// changed its key from "item" to "id" (brewing recipe result became an object
// instead of a string), and conventional Forge tags moved from the forge:
// namespace to c:. Datagen is 1.20.1-only and writes everything in the old
// format, so we normalize resources on processResources output instead of
// touching datagen.
tasks.named<ProcessResources>("processResources") {
	doLast {
		val dataDir = File(destinationDir, "data")
		if (!dataDir.isDirectory) return@doLast

		fun moveInto(source: File, target: File) {
			target.mkdirs()
			source.listFiles()!!.forEach { child ->
				val dest = File(target, child.name)
				if (dest.exists()) {
					if (child.isDirectory) moveInto(child, dest) else child.delete()
				} else {
					child.renameTo(dest)
				}
			}
			source.deleteRecursively()
		}

		// Directory renames (merge on collision).
		val dirRenames = mapOf(
			"recipes" to "recipe", "advancements" to "advancement",
			"loot_tables" to "loot_table", "structures" to "structure",
		)
		val tagRenames = mapOf(
			"blocks" to "block", "items" to "item", "entity_types" to "entity_type",
			"fluids" to "fluid", "game_events" to "game_event",
		)

		// On NeoForge 1.21+ biome modifiers are read from neoforge/biome_modifier
		// with type neoforge:add_features; datagen (1.20.1) writes
		// forge/biome_modifier + forge:add_features — without remapping, ores don't spawn.
		dataDir.listFiles()!!.filter { it.isDirectory }.forEach { nsDir ->
			val bm = File(nsDir, "forge/biome_modifier")
			if (bm.isDirectory) {
				val target = File(nsDir, "neoforge/biome_modifier")
				moveInto(bm, target)
				target.walkTopDown().filter { it.isFile && it.extension == "json" }.forEach { f ->
					f.writeText(f.readText()
						.replace("\"forge:add_features\"", "\"neoforge:add_features\"")
						.replace("\"forge:add_spawns\"", "\"neoforge:add_spawns\""))
				}
			}
		}

		dataDir.listFiles()!!.filter { it.isDirectory }.forEach { nsDir ->
			// Tags forge: → c: (conventional tags on NeoForge 1.21+ live in c:).
			if (nsDir.name == "forge") {
				moveInto(nsDir, File(dataDir, "c"))
				// After moving to c:, the tags themselves must also be singularized.
				val cTags = File(File(dataDir, "c"), "tags")
				if (cTags.isDirectory) {
					for ((old, new) in tagRenames) {
						val plural = File(cTags, old)
						if (plural.isDirectory) moveInto(plural, File(cTags, new))
					}
				}
			}
			for ((old, new) in dirRenames) {
				val plural = File(nsDir, old)
				if (plural.isDirectory) moveInto(plural, File(nsDir, new))
			}
			val tags = File(nsDir, "tags")
			if (tags.isDirectory) {
				for ((old, new) in tagRenames) {
					val plural = File(tags, old)
					if (plural.isDirectory) moveInto(plural, File(tags, new))
				}
			}
		}

		// Remap forge: → c: references inside JSON files.
		// The leading quote in the pattern keeps "neoforge:add_features" untouched.
		// glass is the only tag whose name also changed: c:glass_blocks.
		dataDir.walkTopDown().filter { it.isFile && it.extension == "json" }.forEach { file ->
			val text = file.readText()
			if (!text.contains("forge:")) return@forEach
			val out = text.replace("\"#forge:", "\"#c:").replace("\"forge:", "\"c:")
				.replace("\"c:glass\"", "\"c:glass_blocks\"").replace("\"#c:glass\"", "\"#c:glass_blocks\"")
			if (out != text) file.writeText(out)
		}

		// Remap silk-touch conditions in loot tables: datagen (1.20.1) writes
		// a match_tool predicate with the "enchantments" field inside ItemPredicate,
		// while on 1.21.1 this field was removed — enchantment checks moved to
		// ItemSubPredicates ("predicates"."minecraft:enchantments").
		// Without remapping, the silk-touch branch of ore tables never applies.
		val lootRoots = dataDir.listFiles()!!.mapNotNull { File(it, "loot_table").takeIf(File::isDirectory) }
		lootRoots.forEach { root ->
			root.walkTopDown().filter { it.isFile && it.extension == "json" }.forEach { file ->
				val text = file.readText()
				if (!text.contains("minecraft:match_tool")) return@forEach
				val tree = groovy.json.JsonSlurper().parseText(text)
				@Suppress("UNCHECKED_CAST")
				fun fixPredicate(predicate: MutableMap<String, Any>) {
					val ench = predicate.remove("enchantments") as? List<Any> ?: return
					val converted = ench.map { e ->
						val m = e as MutableMap<String, Any>
						val out = linkedMapOf<String, Any>()
						m["enchantment"]?.let { out["enchantments"] = it }
						m["levels"]?.let { out["levels"] = it }
						out
					}
					predicate["predicates"] = linkedMapOf("minecraft:enchantments" to converted)
				}
				@Suppress("UNCHECKED_CAST")
				fun walk(node: Any?) {
					when (node) {
						is MutableMap<*, *> -> {
							if (node["condition"] == "minecraft:match_tool") {
								(node["predicate"] as? MutableMap<String, Any>)?.let { fixPredicate(it) }
							}
							node.values.forEach { walk(it) }
						}
						is MutableList<*> -> node.forEach { walk(it) }
					}
				}
				walk(tree)
				file.writeText(groovy.json.JsonOutput.prettyPrint(groovy.json.JsonOutput.toJson(tree)))
			}
		}

		// Normalize vanilla recipes (minecraft:*) to the 1.21.x format:
		// "result": {"item": X} → {"id": X};  "result": "X" → {"id": X}.
		// Custom recipes (${prop("mod.id")}:*) are left alone — RecipeHooks normalizes them.
		val recipeRoots = dataDir.listFiles()!!.mapNotNull { File(it, "recipe").takeIf(File::isDirectory) }
		val slurper = groovy.json.JsonSlurper()
		recipeRoots.forEach { root ->
			root.walkTopDown().filter { it.isFile && it.extension == "json" }.forEach { file ->
				val tree = slurper.parse(file) as? Map<*, *> ?: return@forEach
				if ((tree["type"] as? String)?.startsWith("minecraft:") != true) return@forEach
				val result = tree["result"]
				val normalized: Any? = when (result) {
					is String -> mapOf("id" to result)
					is Map<*, *> ->
						if (result.containsKey("item") && !result.containsKey("id"))
							result.entries.associate { (k, v) -> if (k == "item") "id" to v else k to v }
						else null
					else -> null
				}
				if (normalized != null) {
					val copy = tree.toMutableMap()
					copy["result"] = normalized
					file.writeText(groovy.json.JsonOutput.prettyPrint(groovy.json.JsonOutput.toJson(copy)))
				}
			}
		}

		// minecraft:grass was renamed to short_grass in 1.20.3+ (datagen 1.20.1 writes the old name).
		dataDir.walkTopDown()
			.filter { it.isFile && it.extension == "json" }.forEach { file ->
				val text = file.readText()
				if (text.contains("\"minecraft:grass\"")) {
					file.writeText(text.replace("\"minecraft:grass\"", "\"minecraft:short_grass\""))
				}
			}
	}
}

// Datagen classes only exist in the 1.20.1 Forge dev environment — they are absent
// on NeoForge, so exclude them from compilation and from the final jar.
sourceSets {
	main {
		java {
			exclude("com/${prop("mod.id")}/datagen/**")
		}
	}
}

tasks.named<Jar>("jar") {
	exclude("com/${prop("mod.id")}/datagen/**")
}

tasks.withType<JavaCompile>().configureEach {
	options.encoding = "UTF-8"
}

stonecutter {
}
