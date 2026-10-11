//? if forge {
package com.space_m.datagen;

import java.util.concurrent.CompletableFuture;

import com.space_m.SpaceMod;
import com.space_m.lib.SpaceTags;

import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.data.tags.EntityTypeTagsProvider;
import net.minecraft.data.tags.ItemTagsProvider;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.common.data.BlockTagsProvider;
import net.minecraftforge.common.data.ExistingFileHelper;

/**
 * Tags des Addons. Legt die Ersatz-Tags fuer Original-Schnittstellen an ({@link SpaceTags}); Inhalte kommen in
 * den jeweiligen Runden dazu (Abdichtung R2, Atemschutz R2, Mobs R8). Werkzeug-/Abbau-Tags
 * ({@code mineable/pickaxe}, {@code needs_iron_tool}) fuer neue Bloecke ebenfalls hier.
 */
public final class SpaceTagProviders {
    private SpaceTagProviders() {}

    public static final class Blocks extends BlockTagsProvider {
        public Blocks(PackOutput out, CompletableFuture<HolderLookup.Provider> lookup, ExistingFileHelper efh) {
            super(out, lookup, SpaceMod.MOD_ID, efh);
        }

        @Override
        protected void addTags(HolderLookup.Provider provider) {
            tag(SpaceTags.SEALABLE);
            // R2-A: Atmosphaere (nie dichte Vollbloecke, Zusatz-Pflanzen)
            for (String id : com.space_m.atmosphere.AtmosphereTagData.UNSEALABLE) tag(SpaceTags.UNSEALABLE).add(net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.BLOCK, new net.minecraft.resources.ResourceLocation(id)));
            tag(SpaceTags.BREATHING_PLANTS);
            // R1-B: Planetengestein + Koerper-Erze (Original Material.rock -> Spitzhacke)
            for (var b : com.space_m.dim.PlanetOres.pickaxeBlocks()) tag(net.minecraft.tags.BlockTags.MINEABLE_WITH_PICKAXE).add(b.get());
            // R3-4: Laythe/Tekto (Schaufel/Axt/Hacke, Blaetter, Staemme, Bretter, Setzlinge, Ranken)
            com.space_m.dim.laythe.LaytheBlocks.addTags((k, b) -> tag(k).add(b));
            com.space_m.dim.tekto.TektoBlocks.addTags((k, b) -> tag(k).add(b));
            for (var b : com.space_m.dim.eve.EveBlocks.shovelBlocks()) tag(net.minecraft.tags.BlockTags.MINEABLE_WITH_SHOVEL).add(b.get()); // R3-3 eve_silt
            // R3-1: OreDictManager KEY_STONE / KEY_COBBLESTONE (minmus_stone, minmus_smooth, ike_stone / minmus_regolith, ike_regolith)
            tag(net.minecraftforge.common.Tags.Blocks.STONE).add(com.space_m.dim.minmus.MinmusBlocks.MINMUS_STONE.get(), com.space_m.dim.minmus.MinmusBlocks.MINMUS_SMOOTH.get(), com.space_m.dim.ike.IkeBlocks.IKE_STONE.get());
            tag(net.minecraftforge.common.Tags.Blocks.COBBLESTONE).add(com.space_m.dim.minmus.MinmusBlocks.MINMUS_REGOLITH.get(), com.space_m.dim.ike.IkeBlocks.IKE_REGOLITH.get());
            // R3-2: Duna-Sand/Eisenton (Material sand/clay -> Schaufel); OreDict KEY_SAND duna_sands, KEY_STONE duna_rock, KEY_COBBLESTONE duna_cobble + dres_rock
            for (var b : com.space_m.dim.duna.DunaBlocks.shovelBlocks()) tag(net.minecraft.tags.BlockTags.MINEABLE_WITH_SHOVEL).add(b.get());
            tag(net.minecraftforge.common.Tags.Blocks.SAND).add(com.space_m.dim.duna.DunaBlocks.DUNA_SANDS.get());
            tag(net.minecraftforge.common.Tags.Blocks.STONE).add(com.space_m.dim.duna.DunaBlocks.DUNA_ROCK.get());
            tag(net.minecraftforge.common.Tags.Blocks.COBBLESTONE).add(com.space_m.dim.duna.DunaBlocks.DUNA_COBBLE.get(), com.space_m.dim.dres.DresBlocks.DRES_ROCK.get());
            // R2-C: Atmo-Maschinen (Original Material.iron -> Spitzhacke)
            for (var b : com.space_m.machine.atmo.AtmoMachines.pickaxeBlocks()) tag(net.minecraft.tags.BlockTags.MINEABLE_WITH_PICKAXE).add(b.get());
            // R4-B: Gummi-Ackerboden + Feldfruechte
            com.space_m.item.SpaceItemTags.addBlockTags((k, b) -> tag(k).add(b));
        }
    }

    public static final class Items extends ItemTagsProvider {
        public Items(PackOutput out, CompletableFuture<HolderLookup.Provider> lookup,
                     CompletableFuture<TagLookup<Block>> blockTags, ExistingFileHelper efh) {
            super(out, lookup, blockTags, SpaceMod.MOD_ID, efh);
        }

        @Override
        protected void addTags(HolderLookup.Provider provider) {
            tag(SpaceTags.SEALED_ARMOR);
            // R2-A: dichte Anzuege (Original ArmorFSB.canSeal), Korrosionsschutz-Einsatz
            for (String id : com.space_m.atmosphere.AtmosphereTagData.SEALED_ARMOR) tag(SpaceTags.SEALED_ARMOR).add(net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.ITEM, new net.minecraft.resources.ResourceLocation(id)));
            for (String id : com.space_m.atmosphere.AtmosphereTagData.CORROSION_PROTECTION_OPTIONAL) tag(SpaceTags.CORROSION_PROTECTION).addOptional(new net.minecraft.resources.ResourceLocation(id));
            tag(SpaceTags.OXYGEN_SUPPLY);
            // R3-3: OreDictManager (KEY_SAND eve_silt, KEY_COBBLESTONE eve_rock/moho_regolith, KEY_STONE moho_stone)
            tag(net.minecraft.tags.ItemTags.create(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("forge", "sand"))).add(com.space_m.dim.eve.EveBlocks.EVE_SILT.get().asItem());
            tag(net.minecraft.tags.ItemTags.create(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("forge", "cobblestone"))).add(com.space_m.dim.eve.EveBlocks.EVE_ROCK.get().asItem(), com.space_m.dim.moho.MohoBlocks.MOHO_REGOLITH.get().asItem());
            tag(net.minecraft.tags.ItemTags.create(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("forge", "stone"))).add(com.space_m.dim.moho.MohoBlocks.MOHO_STONE.get().asItem());
            // R3-1: Item-Gegenstuecke von forge:stone / forge:cobblestone
            copy(net.minecraftforge.common.Tags.Blocks.STONE, net.minecraftforge.common.Tags.Items.STONE);
            copy(net.minecraftforge.common.Tags.Blocks.COBBLESTONE, net.minecraftforge.common.Tags.Items.COBBLESTONE);
            copy(net.minecraftforge.common.Tags.Blocks.SAND, net.minecraftforge.common.Tags.Items.SAND); // R3-2 duna_sands
            // R4-B: OreDictManager (Space) -> hbm_m:oredict/* + forge:-Formen
            com.space_m.item.SpaceItemTags.addItemTags((k, i) -> tag(k).add(i));
        }
    }

    public static final class Entities extends EntityTypeTagsProvider {
        public Entities(PackOutput out, CompletableFuture<HolderLookup.Provider> lookup, ExistingFileHelper efh) {
            super(out, lookup, SpaceMod.MOD_ID, efh);
        }

        @Override
        protected void addTags(HolderLookup.Provider provider) {
            tag(SpaceTags.SUFFOCATION_IMMUNE);
            // R2-A: HBM-Mobs mit ISuffocationImmune
            for (String id : com.space_m.atmosphere.AtmosphereTagData.SUFFOCATION_IMMUNE) tag(SpaceTags.SUFFOCATION_IMMUNE).add(net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.ENTITY_TYPE, new net.minecraft.resources.ResourceLocation(id)));
        }
    }
}
//?}
