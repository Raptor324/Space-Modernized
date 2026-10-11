package com.space_m.dim.minmus;

import java.util.List;

import com.space_m.block.SpaceBlocks;
import com.space_m.dim.PlanetOres;
import com.space_m.main.SpaceCreativeTabs;

import dev.architectury.registry.registries.RegistrySupplier;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.block.Block;

/**
 * Bloecke von Minmus (Original {@code ModBlocks}): Gestein {@code minmus_stone}, Regolith {@code minmus_regolith},
 * "Minzeis" {@code minmus_smooth} (Textur {@code minflat_regolith}, {@code ItemBlockLore} mit
 * {@code tile.minmus_smooth.desc}) und die Minmus-Varianten (Metadatum {@code Body.MINMUS}) aller Erze, die
 * {@code WorldGeneratorCelestial}, {@code WorldGeneratorMinmus} und {@code ChunkProviderMinmus} hier erzeugen.
 *
 * <p>Dazu {@code stone_resource} Metadatum {@code CONGLOMERATE} ({@code BlockResourceStone}, neu in NTM Space,
 * Material.rock 5.0/10.0, Textur {@code stone_resource.conglomerate}, droppt sich selbst): die
 * {@code OreLayer3D}-Lage von Mun, Ike und Minmus. HBM-Modernized hat nur die 1.7.10-Typen, daher unter dem
 * Namensschema {@code stone_resource_<typ>} (wie {@code stone_resource_calcium} von Duna).
 *
 * <p>Werte (Haerte/Widerstand) 1:1 aus {@code ModBlocks}; Texturen/Modelle/Beute unter {@code assets|data/space_m}
 * (Skript {@code scratchpad/sR3a_resources.py}).
 */
public final class MinmusBlocks {

    private MinmusBlocks() {}

    public static final String BODY = "minmus";

    /** {@code minmus_stone = new BlockGeneric(Material.rock).setHardness(1.5F).setResistance(10.0F)}, Stein-Klang. */
    public static final RegistrySupplier<Block> MINMUS_STONE = PlanetOres.pickaxe(SpaceBlocks.registerWithItem("minmus_stone",
            () -> new Block(PlanetOres.rock(1.5F, 10.0F)), SpaceCreativeTabs.Tab.BLOCKS));
    /** {@code minmus_regolith}: BlockGeneric 1.5/10.0. */
    public static final RegistrySupplier<Block> MINMUS_REGOLITH = PlanetOres.pickaxe(SpaceBlocks.registerWithItem("minmus_regolith",
            () -> new Block(PlanetOres.rock(1.5F, 10.0F)), SpaceCreativeTabs.Tab.BLOCKS));
    /** {@code minmus_smooth}: BlockGeneric 1.5/10.0, registriert mit {@code ItemBlockLore} (Zusatzzeile .desc). */
    public static final RegistrySupplier<Block> MINMUS_SMOOTH = PlanetOres.pickaxe(SpaceBlocks.registerWithItem("minmus_smooth",
            () -> new Block(PlanetOres.rock(1.5F, 10.0F)), SpaceCreativeTabs.Tab.BLOCKS, b -> new LoreBlockItem(b, new Item.Properties())));

    /** {@code stone_resource} Metadatum {@code CONGLOMERATE}: BlockResourceStone 5.0/10.0. */
    public static final RegistrySupplier<Block> STONE_RESOURCE_CONGLOMERATE = PlanetOres.pickaxe(SpaceBlocks.registerWithItem("stone_resource_conglomerate",
            () -> new Block(PlanetOres.rock(5.0F, 10.0F)), SpaceCreativeTabs.Tab.BLOCKS));

    // ── WorldGeneratorCelestial.generateVanillaOres (3.0/5.0) ──
    public static final RegistrySupplier<Block> ORE_IRON = PlanetOres.ore("ore_iron", BODY, 3.0F, 5.0F);
    public static final RegistrySupplier<Block> ORE_GOLD = PlanetOres.ore("ore_gold", BODY, 3.0F, 5.0F);
    public static final RegistrySupplier<Block> ORE_REDSTONE = PlanetOres.ore("ore_redstone", BODY, 3.0F, 5.0F);
    public static final RegistrySupplier<Block> ORE_DIAMOND = PlanetOres.ore("ore_diamond", BODY, 3.0F, 5.0F);

    // ── WorldGeneratorCelestial.generateNTMOres (+ WorldGeneratorMinmus: Nickel, Titan, Kupfer) ──
    /** {@code BlockOreOutgas(rock, true, 5, true)}: gibt Radon ab (HBM {@code OutgasBlock}). */
    public static final RegistrySupplier<Block> ORE_URANIUM = PlanetOres.ore("ore_uranium", BODY,
            p -> new com.hbm_m.block.gas.OutgasBlock(p, () -> com.hbm_m.block.ModBlocks.GAS_RADON.get(), true, true), 5.0F, 10.0F);
    public static final RegistrySupplier<Block> ORE_THORIUM = PlanetOres.ore("ore_thorium", BODY, 5.0F, 10.0F);
    public static final RegistrySupplier<Block> ORE_TITANIUM = PlanetOres.ore("ore_titanium", BODY, 5.0F, 10.0F);
    public static final RegistrySupplier<Block> ORE_SULFUR = PlanetOres.ore("ore_sulfur", BODY, 5.0F, 10.0F);
    public static final RegistrySupplier<Block> ORE_ALUMINIUM = PlanetOres.ore("ore_aluminium", BODY, 5.0F, 10.0F);
    public static final RegistrySupplier<Block> ORE_COPPER = PlanetOres.ore("ore_copper", BODY, 5.0F, 10.0F);
    public static final RegistrySupplier<Block> ORE_NICKEL = PlanetOres.ore("ore_nickel", BODY, 5.0F, 10.0F);
    public static final RegistrySupplier<Block> ORE_ZINC = PlanetOres.ore("ore_zinc", BODY, 5.0F, 10.0F);
    public static final RegistrySupplier<Block> ORE_FLUORITE = PlanetOres.ore("ore_fluorite", BODY, 5.0F, 10.0F);
    public static final RegistrySupplier<Block> ORE_NITER = PlanetOres.ore("ore_niter", BODY, 5.0F, 10.0F);
    public static final RegistrySupplier<Block> ORE_TUNGSTEN = PlanetOres.ore("ore_tungsten", BODY, 5.0F, 10.0F);
    public static final RegistrySupplier<Block> ORE_LEAD = PlanetOres.ore("ore_lead", BODY, 5.0F, 10.0F);
    public static final RegistrySupplier<Block> ORE_BERYLLIUM = PlanetOres.ore("ore_beryllium", BODY, 5.0F, 15.0F);
    public static final RegistrySupplier<Block> ORE_RARE = PlanetOres.ore("ore_rare", BODY, 5.0F, 10.0F);
    public static final RegistrySupplier<Block> ORE_CINNEBAR = PlanetOres.ore("ore_cinnebar", BODY, 5.0F, 10.0F);
    public static final RegistrySupplier<Block> ORE_COBALT = PlanetOres.ore("ore_cobalt", BODY, 5.0F, 10.0F);
    /** {@code BlockCluster(rock)} 5.0/15.0. */
    public static final RegistrySupplier<Block> CLUSTER_IRON = PlanetOres.ore("cluster_iron", BODY, 5.0F, 15.0F);
    public static final RegistrySupplier<Block> CLUSTER_TITANIUM = PlanetOres.ore("cluster_titanium", BODY, 5.0F, 15.0F);
    public static final RegistrySupplier<Block> CLUSTER_ALUMINIUM = PlanetOres.ore("cluster_aluminium", BODY, 5.0F, 15.0F);
    public static final RegistrySupplier<Block> CLUSTER_COPPER = PlanetOres.ore("cluster_copper", BODY, 5.0F, 15.0F);

    // ── ChunkProviderMinmus: Sole-Blasen (BlockOreFluid BRINE, leer -> ore_brine_empty) ──
    public static final RegistrySupplier<Block> ORE_BRINE = PlanetOres.ore("ore_brine", BODY, 5.0F, 10.0F);
    public static final RegistrySupplier<Block> ORE_BRINE_EMPTY = PlanetOres.ore("ore_brine_empty", BODY, 5.0F, 10.0F);

    /** Erzwingt das Laden der Klasse (Registrierung vor {@code SpaceBlocks.init()}). */
    public static void init() {
    }

    /**
     * {@code ItemBlockLore}: haengt die Zeile {@code tile.<name>.desc} an (Port {@code block.space_m.<name>.desc}),
     * grau wie im Original ({@code EnumChatFormatting.GRAY} fuer ItemBlockLore-Zeilen ohne Farbcode).
     */
    public static class LoreBlockItem extends BlockItem {

        public LoreBlockItem(Block block, Properties properties) {
            super(block, properties);
        }

        //? if < 1.21.1 {
        @Override
        public void appendHoverText(ItemStack stack, @org.jetbrains.annotations.Nullable net.minecraft.world.level.Level level, List<Component> list, TooltipFlag flag) {
            super.appendHoverText(stack, level, list, flag);
        //?} else {
        /*@Override
        public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> list, TooltipFlag flag) {
            super.appendHoverText(stack, context, list, flag);
        *///?}
            list.add(Component.translatable(getDescriptionId() + ".desc").withStyle(ChatFormatting.GRAY));
        }
    }
}
