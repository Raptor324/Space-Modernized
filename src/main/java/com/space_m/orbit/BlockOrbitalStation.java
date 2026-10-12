package com.space_m.orbit;


import org.jetbrains.annotations.Nullable;

import com.hbm_m.block.ModBlocks;
import com.hbm_m.block.machines.DummyableMachineBlock;
import com.hbm_m.interfaces.ILookOverlay;
import com.hbm_m.multiblock.DummyableStructureBuilder;
import com.hbm_m.multiblock.MultiblockStructureHelper;
import com.space_m.celestial.CelestialBody;
import com.space_m.rocket.ItemCustomRocket;
import com.space_m.rocket.RocketStruct;
import com.space_m.rocket.SpaceRocketItems;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/**
 * 1:1 NTM Space {@code BlockOrbitalStation} ({@code orbital_station} Hauptport/Kern und {@code orbital_station_port}):
 * 5 x 2 x 5 ({@code getDimensions {1, 0, 2, 2, 2, 2}}, Offset 2) mit 12 Anschlusszellen ({@code makeExtra}) auf Hoehe +1
 * an allen vier Seiten (Hydrazin-Zufuhr). Abgedichtet ({@code IBlockSealable.isSealed = true}, {@link OrbitSealing}).
 *
 * <p>Rechtsklick (nur im Orbit, nur auf den inneren 3 x 3 - die Randzellen bleiben zum Bauen frei): gelagerte Raketen
 * ausgeben; angedockte Rakete betreten bzw. (Schleichen, leere Hand) einlagern; sonst eine betankte {@code rocket_custom}
 * oder eine Kapsel ({@code rp_c_20}/{@code rp_pod_20}) aus der Hand andocken. Blick-Overlay wie im Original.
 *
 * <p>Der Kern ({@code orbital_station}) laesst sich weder setzen ({@code canPlaceBlockAt}) noch von Spielern abbauen
 * ({@code removedByPlayer}); Explosionen prallen ab ({@code setResistance(Float.POSITIVE_INFINITY)} - die HBM-Phantomteile
 * leiten Explosionen an den Kern weiter). Startport (R6-B) erbt von dieser Klasse wie im Original.
 * Original-Klasse: BlockOrbitalStation
 */
public class BlockOrbitalStation extends DummyableMachineBlock implements ILookOverlay {

    public BlockOrbitalStation(Properties properties) {
        super(properties);
    }

    /** Original {@code getDimensions {1, 0, 2, 2, 2, 2}}, {@code getOffset 2}, {@code fillSpace}: 12 {@code makeExtra}. */
    @Override
    protected MultiblockStructureHelper defineStructure() {
        DummyableStructureBuilder b = DummyableStructureBuilder.create().box(1, 0, 2, 2, 2, 2);

        // makeExtra(x +- 2, y + 1, z - 1..1) und (x - 1..1, y + 1, z +- 2); fuer dir = NORTH: forward = -z, side = rot = EAST = +x
        for (int o = -1; o <= 1; o++) {
            b.extra(-o, 1, 2);
            b.extra(-o, 1, -2);
            b.extra(-2, 1, o);
            b.extra(2, 1, o);
        }

        return b.placementOffset(2).build(() -> ModBlocks.UNIVERSAL_MACHINE_PART.get().defaultBlockState());
    }

    protected boolean isMainPort() {
        return this == OrbitBlocks.ORBITAL_STATION.get();
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new TileEntityOrbitalStation(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return createTickerHelper(type, OrbitBlocks.ORBITAL_STATION_BE.get(), TileEntityOrbitalStation::tick);
    }

    /** Original {@code getRenderType() = -1} (TESR). */
    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.ENTITYBLOCK_ANIMATED;
    }

    /** Original {@code canPlaceBlockAt}: block placing of extra main ports (use the dedicated sub-ports!). */
    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        if (isMainPort()) return null;
        return super.getStateForPlacement(context);
    }

    // ── Unzerstoerbarer Hauptport ──────────────────────────────────────────────────────────────

    /** Phantomteile leiten Explosionen an den Kern weiter, damit der Hauptport ganz bleibt. */
    @Override
    public boolean forwardPartExplosions() {
        return isMainPort();
    }

    @Override
    public void onBlockExploded(BlockState state, Level level, BlockPos pos, Explosion explosion) {
        if (isMainPort()) return; // setResistance(Float.POSITIVE_INFINITY)
        super.onBlockExploded(state, level, pos, explosion);
    }

    /** Original {@code TileEntityOrbitalStation.invalidate}: Port abmelden, angedockte Rakete fallen lassen. */
    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        if (!state.is(newState.getBlock()) && !level.isClientSide && level.getBlockEntity(pos) instanceof TileEntityOrbitalStation station) {
            station.onBroken();
        }
        super.onRemove(state, level, pos, newState, isMoving);
    }

    // ── Rechtsklick ────────────────────────────────────────────────────────────────────────────

    //? if < 1.21.1 {
    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        return onActivated(level, pos, player, hit);
    }
    //?} else {
    /*@Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        return onActivated(level, pos, player, hit);
    }
    *///?}

    /**
     * Angeklickte Zelle: HBM-Phantomteile reichen den Klick mit der Kernposition weiter
     * ({@code hit.withPosition(core)}), der Trefferpunkt bleibt aber auf der angeklickten Zelle.
     */
    public static BlockPos clickedCell(BlockHitResult hit) {
        Vec3 normal = Vec3.atLowerCornerOf(hit.getDirection().getNormal());
        return BlockPos.containing(hit.getLocation().subtract(normal.scale(0.001D)));
    }

    /** Original {@code Math.abs(pos[0] - x) >= 2 || Math.abs(pos[2] - z) >= 2}: Randzellen. */
    public static boolean isSideCell(BlockPos core, BlockPos cell) {
        return Math.abs(core.getX() - cell.getX()) >= 2 || Math.abs(core.getZ() - cell.getZ()) >= 2;
    }

    protected InteractionResult onActivated(Level world, BlockPos pos, Player player, BlockHitResult hit) {
        if (!CelestialBody.inOrbit(world)) return InteractionResult.PASS;

        // If activating the side blocks, ignore, to allow placing
        if (isSideCell(pos, clickedCell(hit))) return InteractionResult.PASS;

        if (world.isClientSide) {
            return InteractionResult.SUCCESS;
        }

        if (!(world.getBlockEntity(pos) instanceof TileEntityOrbitalStation station)) return InteractionResult.PASS;

        if (station.hasStoredItems()) {
            station.giveStoredItems(player);
        } else if (station.hasDocked) {
            if (!station.hasRider) {
                if (player.isShiftKeyDown()) {
                    if (player.getMainHandItem().isEmpty()) {
                        station.despawnRocket();
                        station.giveStoredItems(player);
                    }
                } else {
                    station.enterCapsule(player);
                }
            }
        } else {
            ItemStack held = player.getMainHandItem();
            if (!held.isEmpty()) {
                if (held.getItem() == SpaceRocketItems.ROCKET_CUSTOM.get() && ItemCustomRocket.hasFuel(held)) {
                    station.spawnRocket(held);
                    held.shrink(1);
                }
                if (held.getItem() == SpaceRocketItems.RP_CAPSULE_20.get() || held.getItem() == SpaceRocketItems.RP_POD_20.get()) {
                    station.spawnRocket(ItemCustomRocket.build(new RocketStruct(held)));
                    held.shrink(1);
                }
            }
        }

        return InteractionResult.SUCCESS;
    }

    // ── Blick-Overlay ──────────────────────────────────────────────────────────────────────────

    /** Blick-Overlay (Original {@code printHook}), Client-Teil in {@code StationOverlay}. */
    @Override
    public void printHook(GuiGraphics g, Level world, BlockPos pos) {
        com.space_m.orbit.client.StationOverlay.print(this, g, world, pos);
    }

    //? if >1.20.1 {
    /*public static final com.mojang.serialization.MapCodec<BlockOrbitalStation> CODEC = simpleCodec(BlockOrbitalStation::new);

    @Override
    protected com.mojang.serialization.MapCodec<? extends net.minecraft.world.level.block.BaseEntityBlock> codec() {
        return CODEC;
    }
    *///?}
}
