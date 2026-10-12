package com.space_m.machine.chem;

import java.util.List;
import java.util.function.Supplier;

import com.space_m.block.SpaceBlocks;
import com.space_m.blockentity.SpaceBlockEntities;
import com.space_m.inventory.SpaceMenus;
import com.space_m.machine.chem.recipe.ChemRecipes;
import com.space_m.main.SpaceCreativeTabs;

import dev.architectury.registry.menu.MenuRegistry;
import dev.architectury.registry.registries.RegistrySupplier;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;

/**
 * R7-B Chemie (Original {@code ModBlocks}, alle {@code Material.iron}, machineTab, Haerte 5):
 * <ul>
 *   <li>{@code machine_alkylation} (Widerstand 10 -> 6, Textur {@code block_steel_machine}), BE {@code alkylation}.</li>
 *   <li>{@code machine_cryo_distill} (10 -> 6, {@code block_steel}), BE {@code cryogenic_distillator}.</li>
 *   <li>{@code machine_vacuum_circuit} (30 -> 18, {@code block_steel}), BE {@code vacuum_circuit}.</li>
 *   <li>{@code machine_milk_reformer} (10 -> 6, {@code block_steel}), BE {@code milk_reformer}.</li>
 *   <li>{@code hydrobay} (10 -> 6, {@code block_steel_machine}), BE {@code hydrobay}; {@code dummy_beam} (Haerte 10,
 *       Widerstand 20 -> 12, Licht 15, ohne Item-Tab).</li>
 * </ul>
 * Kein Trittgeraeusch gesetzt (Original-Standard Stein). Rezepttypen in {@link ChemRecipes}.
 */
public final class ChemMachines {
    private ChemMachines() {}

    private static BlockBehaviour.Properties machine(float resistance) {
        return BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(5.0F, resistance * 3F / 5F).sound(SoundType.STONE)
                .requiresCorrectToolForDrops().noOcclusion().isSuffocating((s, w, p) -> false);
    }

    private static <T extends Block> RegistrySupplier<T> machine(String id, Supplier<T> block) {
        return SpaceBlocks.registerWithItem(id, block, SpaceCreativeTabs.Tab.MACHINE, b -> new SpaceMachineBlockItem(b, new Item.Properties()));
    }

    // ── Bloecke ──────────────────────────────────────────────────────────────────────────────

    public static final RegistrySupplier<MachineAlkylation> MACHINE_ALKYLATION = SpaceBlocks.dropsPersistent(
            machine("machine_alkylation", () -> new MachineAlkylation(machine(10F))));
    public static final RegistrySupplier<MachineCryoDistill> MACHINE_CRYO_DISTILL = SpaceBlocks.dropsPersistent(
            machine("machine_cryo_distill", () -> new MachineCryoDistill(machine(10F))));
    public static final RegistrySupplier<MachineVacuumCircuit> MACHINE_VACUUM_CIRCUIT = SpaceBlocks.dropsSelf(
            machine("machine_vacuum_circuit", () -> new MachineVacuumCircuit(machine(30F))));
    public static final RegistrySupplier<MachineMilkReformer> MACHINE_MILK_REFORMER = SpaceBlocks.dropsSelf(
            machine("machine_milk_reformer", () -> new MachineMilkReformer(machine(10F))));
    public static final RegistrySupplier<MachineHydroponic> HYDROBAY = SpaceBlocks.dropsSelf(
            machine("hydrobay", () -> new MachineHydroponic(machine(10F))));

    /** Original {@code dummy_beam}: kein eigener Drop (Teil der Hydroponik). */
    public static final RegistrySupplier<BlockDummyableBeam> DUMMY_BEAM = SpaceBlocks.register("dummy_beam",
            () -> new BlockDummyableBeam(BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(10.0F, 12.0F)
                    .sound(SoundType.STONE).noOcclusion().noLootTable().isSuffocating((s, w, p) -> false)
                    .lightLevel(s -> s.getValue(BlockDummyableBeam.LIT) ? 15 : 0)));

    // ── Blockentities (Original TileMappings ohne "tileentity_") ─────────────────────────────

    public static final RegistrySupplier<BlockEntityType<TileEntityMachineAlkylation>> ALKYLATION_BE =
            SpaceBlockEntities.register("alkylation", TileEntityMachineAlkylation::new, MACHINE_ALKYLATION);
    public static final RegistrySupplier<BlockEntityType<TileEntityMachineCryoDistill>> CRYO_DISTILL_BE =
            SpaceBlockEntities.register("cryogenic_distillator", TileEntityMachineCryoDistill::new, MACHINE_CRYO_DISTILL);
    public static final RegistrySupplier<BlockEntityType<TileEntityMachineVacuumCircuit>> VACUUM_CIRCUIT_BE =
            SpaceBlockEntities.register("vacuum_circuit", TileEntityMachineVacuumCircuit::new, MACHINE_VACUUM_CIRCUIT);
    public static final RegistrySupplier<BlockEntityType<TileEntityMachineMilkReformer>> MILK_REFORMER_BE =
            SpaceBlockEntities.register("milk_reformer", TileEntityMachineMilkReformer::new, MACHINE_MILK_REFORMER);
    public static final RegistrySupplier<BlockEntityType<TileEntityHydroponic>> HYDROBAY_BE =
            SpaceBlockEntities.register("hydrobay", TileEntityHydroponic::new, HYDROBAY);

    // ── Menues ───────────────────────────────────────────────────────────────────────────────

    public static final RegistrySupplier<MenuType<ContainerMachineCryoDistill>> CRYO_DISTILL_MENU =
            SpaceMenus.extended("machine_cryo_distill", ContainerMachineCryoDistill::new);
    public static final RegistrySupplier<MenuType<ContainerVacuumCircuit>> VACUUM_CIRCUIT_MENU =
            SpaceMenus.extended("machine_vacuum_circuit", ContainerVacuumCircuit::new);
    public static final RegistrySupplier<MenuType<ContainerMachineMilkReformer>> MILK_REFORMER_MENU =
            SpaceMenus.extended("machine_milk_reformer", ContainerMachineMilkReformer::new);
    public static final RegistrySupplier<MenuType<ContainerHydroponic>> HYDROBAY_MENU =
            SpaceMenus.extended("hydrobay", ContainerHydroponic::new);

    /** Spitzhacken-Bloecke (Datagen {@code mineable/pickaxe}). */
    public static List<Supplier<? extends Block>> pickaxeBlocks() {
        return List.of(MACHINE_ALKYLATION, MACHINE_CRYO_DISTILL, MACHINE_VACUUM_CIRCUIT, MACHINE_MILK_REFORMER, HYDROBAY, DUMMY_BEAM);
    }

    /**
     * Original {@code BlockDummyable.standardOpenBehavior}: Client true; nicht geschlichen -> GUI des Kerns; geschlichen
     * true ohne GUI.
     */
    public static InteractionResult standardOpen(Level level, BlockPos pos, Player player) {
        if (level.isClientSide) return InteractionResult.SUCCESS;
        if (!player.isShiftKeyDown() && level.getBlockEntity(pos) instanceof MenuProvider mp && player instanceof ServerPlayer sp) {
            MenuRegistry.openExtendedMenu(sp, mp, buf -> buf.writeBlockPos(pos));
        }
        return InteractionResult.SUCCESS;
    }

    /** Aus {@code SpaceBlocks.init()} (Mod-Konstruktor): Klasse laden, Rezepttypen anmelden. */
    public static void init() {
        ChemRecipes.register();
    }

    /** Aus {@code SpaceRegistry.init()}. */
    public static void commonInit() { }
}
