package com.space_m.bomb;

import java.util.List;
import java.util.function.Supplier;

import com.space_m.block.SpaceBlocks;
import com.space_m.blockentity.SpaceBlockEntities;
import com.space_m.entity.SpaceEntities;
import com.space_m.inventory.SpaceMenus;
import com.space_m.main.SpaceCreativeTabs;

import dev.architectury.registry.registries.RegistrySupplier;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;

/**
 * R9-B1: Space-Bomben und Einzelbloecke (Original {@code ModBlocks}/{@code TileMappings}):
 * <ul>
 *   <li>{@code nuke_antimatter} ({@code NukeAntimatter}, {@code Material.iron}, Haerte 5, Widerstand 6000 -> 3600, nukeTab),
 *       BE {@code tileentity_nuke_Antimatter}, Menue {@code ContainerNukeAntimatter}.</li>
 *   <li>{@code det_salt} ({@code ExplosiveCharge}, Haerte 0.1, Widerstand 0, nukeTab): MK5 {@code missileRadius} + Pilz.</li>
 *   <li>{@code oil_barrel} ({@code BrownBarrel}, Haerte 0.5, Widerstand 2.5 -> 1.5, nukeTab), BE {@code tileentity_brown_barrel}
 *       (im Original registriert, vom Block aber nie erzeugt).</li>
 *   <li>{@code salted_fallout} ({@code BlockFallout}, {@code Material.snow}, Haerte 0.1, Kies-Geraeusch, blockTab).</li>
 *   <li>{@code capacitor_complex} ({@code MachineCapacitor}, 100 000 000 000 000 000 HE, Haerte 5, Widerstand 10 -> 6, machineTab).</li>
 *   <li>{@code troll_disturber} ({@code MachineTrollDisturber(10000000F)}, Haerte 5, Widerstand 200 -> 120, kein Tab).</li>
 *   <li>{@code ff} ({@code BlockFF}, Haerte 0.5, Kies-Geraeusch, kein Tab; im Original nie per {@code GameRegistry} registriert
 *       -> hier nur Block ohne Item), BE {@code tileentity_forcefield}.</li>
 * </ul>
 * Widerstaende: 1.7.10-Wert * 3 / 5 (wie im restlichen Addon).
 */
public final class SpaceBombs {

    private SpaceBombs() {}

    private static BlockBehaviour.Properties iron(float hardness, float resistance) {
        return BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(hardness, resistance).sound(SoundType.STONE)
                .requiresCorrectToolForDrops();
    }

    // ── Bloecke ──────────────────────────────────────────────────────────────────────────────

    public static final RegistrySupplier<NukeAntimatter> NUKE_ANTIMATTER = SpaceBlocks.dropsSelf(SpaceBlocks.registerWithItem("nuke_antimatter",
            () -> new NukeAntimatter(iron(5.0F, 3600.0F).noOcclusion()), SpaceCreativeTabs.Tab.NUKE,
            b -> new BombBlockItem(b, new Item.Properties())));

    /** Original {@code ExplosiveCharge}-Zweig {@code det_salt}: {@code statFacSalted} reicht {@code salted} nie an den Regen
     *  weiter -> Verhalten exakt wie {@code det_nuke} (HBM {@code ExplosiveCharge(nuke = true)}). */
    public static final RegistrySupplier<com.hbm_m.block.bomb.ExplosiveCharge> DET_SALT = SpaceBlocks.dropsSelf(SpaceBlocks.registerWithItem("det_salt",
            () -> new com.hbm_m.block.bomb.ExplosiveCharge(iron(0.1F, 0.0F), true), SpaceCreativeTabs.Tab.NUKE));

    public static final RegistrySupplier<BrownBarrel> OIL_BARREL = SpaceBlocks.dropsSelf(SpaceBlocks.registerWithItem("oil_barrel",
            () -> new BrownBarrel(iron(0.5F, 1.5F).noOcclusion()), SpaceCreativeTabs.Tab.NUKE));

    public static final RegistrySupplier<BlockSaltedFallout> SALTED_FALLOUT = SpaceBlocks.registerWithItem("salted_fallout",
            () -> new BlockSaltedFallout(BlockBehaviour.Properties.of().mapColor(MapColor.SNOW).strength(0.1F).sound(SoundType.GRAVEL)
                    .noOcclusion().replaceable()), SpaceCreativeTabs.Tab.BLOCKS);

    public static final RegistrySupplier<MachineCapacitorComplex> CAPACITOR_COMPLEX = SpaceBlocks.dropsPersistent(SpaceBlocks.registerWithItem("capacitor_complex",
            () -> new MachineCapacitorComplex(iron(5.0F, 6.0F).noOcclusion(), 100_000_000_000_000_000L), SpaceCreativeTabs.Tab.MACHINE));

    public static final RegistrySupplier<MachineTrollDisturber> TROLL_DISTURBER = SpaceBlocks.dropsSelf(SpaceBlocks.registerWithItem("troll_disturber",
            () -> new MachineTrollDisturber(iron(5.0F, 120.0F).randomTicks(), 10000000F), null));

    public static final RegistrySupplier<BlockFF> FF = SpaceBlocks.register("ff",
            () -> new BlockFF(BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(0.5F, 1.5F).sound(SoundType.GRAVEL)
                    .requiresCorrectToolForDrops().noOcclusion().noLootTable()));

    // ── Blockentities (Original TileMappings, ID ohne "tileentity_", kleingeschrieben) ─────────

    public static final RegistrySupplier<BlockEntityType<TileEntityAntimatter>> ANTIMATTER_BE =
            SpaceBlockEntities.register("nuke_antimatter", TileEntityAntimatter::new, NUKE_ANTIMATTER);
    public static final RegistrySupplier<BlockEntityType<TileEntityBrownBarrel>> BROWN_BARREL_BE =
            SpaceBlockEntities.register("brown_barrel", TileEntityBrownBarrel::new, OIL_BARREL);
    public static final RegistrySupplier<BlockEntityType<TileEntityCapacitorComplex>> CAPACITOR_COMPLEX_BE =
            SpaceBlockEntities.register("capacitor_complex", TileEntityCapacitorComplex::new, CAPACITOR_COMPLEX);
    public static final RegistrySupplier<BlockEntityType<TileEntityFF>> FORCEFIELD_BE =
            SpaceBlockEntities.register("forcefield", TileEntityFF::new, FF);

    // ── Menues ───────────────────────────────────────────────────────────────────────────────

    public static final RegistrySupplier<MenuType<ContainerNukeAntimatter>> ANTIMATTER_MENU =
            SpaceMenus.extended("nuke_antimatter", ContainerNukeAntimatter::new);

    // ── Entities (Explosionen) ───────────────────────────────────────────────────────────────

    /** Original {@code EntityBalefire} mit {@code antimatter()} (Space-Zweig). */
    public static final RegistrySupplier<EntityType<EntityBalefireAntimatter>> BALEFIRE_ANTIMATTER = SpaceEntities.register("balefire_antimatter",
            () -> EntityType.Builder.<EntityBalefireAntimatter>of(EntityBalefireAntimatter::new, MobCategory.MISC)
                    .sized(1.0F, 1.0F).clientTrackingRange(16).updateInterval(20).fireImmune());

    /** Spitzhacken-Bloecke (Original {@code Material.iron}). */
    public static List<Supplier<? extends Block>> pickaxeBlocks() {
        return List.of(NUKE_ANTIMATTER, DET_SALT, OIL_BARREL, CAPACITOR_COMPLEX, TROLL_DISTURBER, FF);
    }

    /** Schaufel-Bloecke (Original {@code Material.snow}). */
    public static List<Supplier<? extends Block>> shovelBlocks() {
        return List.of(SALTED_FALLOUT);
    }

    /** Aus {@code SpaceRegistry.init()} vor {@code SpaceBlocks.init()} (Klasse laden, Eintraege vor den Registern). */
    public static void init() { }

    /** BlockItem fuer Bloecke mit Original-{@code ItemRenderBase} (Darstellung ueber {@code ItemRenderMachines}). */
    public static class BombBlockItem extends BlockItem {
        public BombBlockItem(Block block, Properties props) {
            super(block, props);
        }

        //? if forge {
        @Override
        public void initializeClient(java.util.function.Consumer<net.minecraftforge.client.extensions.common.IClientItemExtensions> consumer) {
            consumer.accept(new net.minecraftforge.client.extensions.common.IClientItemExtensions() {
                @Override
                public net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer getCustomRenderer() {
                    return com.space_m.machine.misc.client.ItemRenderMachines.instance();
                }
            });
        }
        //?} elif neoforge {
        /*@SuppressWarnings("removal")
        @Override
        public void initializeClient(java.util.function.Consumer<net.neoforged.neoforge.client.extensions.common.IClientItemExtensions> consumer) {
            consumer.accept(new net.neoforged.neoforge.client.extensions.common.IClientItemExtensions() {
                @Override
                public net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer getCustomRenderer() {
                    return com.space_m.machine.misc.client.ItemRenderMachines.instance();
                }
            });
        }
        *///?}
    }
}
