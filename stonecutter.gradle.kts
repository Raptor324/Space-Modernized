plugins {
	alias(libs.plugins.stonecutter)
	alias(libs.plugins.neoforged.moddev).apply(false)
	alias(libs.plugins.legacyforge.moddev).apply(false)
	alias(libs.plugins.mod.publish.plugin).apply(false)
}

stonecutter active "1.20.1-forge"

stonecutter parameters {
	constants.match(node.metadata.project.substringAfterLast('-'), "neoforge", "forge")
	swaps["mod_version"] = "\"" + property("mod.version") + "\";"
	swaps["mod_id"] = "\"" + property("mod.id") + "\";"
	swaps["mod_name"] = "\"" + property("mod.name") + "\";"
	swaps["mod_group"] = "\"" + property("mod.group") + "\";"
	swaps["minecraft"] = "\"" + node.metadata.version + "\";"
}
