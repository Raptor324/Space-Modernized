package com.space_m.machine.chem;

import org.jetbrains.annotations.Nullable;

import com.hbm_m.interfaces.ILookOverlay;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;

/**
 * 1:1 NTM Space {@code BlockDummyableBeam} ({@code dummy_beam}, "Integrated Lamp"): unsichtbare Lampenzelle der
 * Hydroponik, gibt alle Interaktionen an den Kern weiter (Rechtsklick, Overlay, Abbau zerstoert die Maschine) und
 * verschwindet, wenn der Kern fehlt. Original: Licht 1.0 (= 15), Lichtdurchlaessig; die BE tauscht im Original die
 * Phantomzelle {@code hydrobay} gegen diesen Block - hier ist die Zelle immer ein {@code dummy_beam}, das Licht schaltet
 * die Eigenschaft {@code lit} (technisch noetig: HBMs Phantomzellen koennen nicht leuchten).
 * Original-Klasse: BlockDummyableBeam
 */
public class BlockDummyableBeam extends Block implements ILookOverlay {

    public static final BooleanProperty LIT = BooleanProperty.create("lit");

    public BlockDummyableBeam(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(LIT, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(LIT);
    }

    /** Original {@code getRenderType() = -1} (der Hydroponik-TESR zeichnet die Lampen). */
    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.INVISIBLE;
    }

    /** Original {@code setLightOpacity(0)}. */
    @Override
    public boolean propagatesSkylightDown(BlockState state, BlockGetter level, BlockPos pos) {
        return true;
    }

    /** Kern: zwei Felder tiefer und ein Feld seitlich ({@code xCoord +- rot, yCoord + 2}). */
    @Nullable
    public static BlockPos findCore(BlockGetter level, BlockPos pos) {
        BlockPos base = pos.below(2);
        for (Direction d : Direction.Plane.HORIZONTAL) {
            BlockPos c = base.relative(d);
            BlockState s = level.getBlockState(c);
            if (s.getBlock() instanceof MachineHydroponic) {
                for (BlockPos lamp : MachineHydroponic.lampCells(c, s.getValue(MachineHydroponic.FACING))) {
                    if (lamp.equals(pos)) return c;
                }
            }
        }
        return null;
    }

    /** Original {@code breakBlock}: Abbau der Lampe zerstoert die Maschine. */
    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        if (!state.is(newState.getBlock()) && !level.isClientSide()) {
            BlockPos core = findCore(level, pos);
            if (core != null) level.destroyBlock(core, true);
        }
        super.onRemove(state, level, pos, newState, isMoving);
    }

    /** Original {@code onNeighborBlockChange}/{@code updateTick}: ohne Kern verschwinden. */
    @Override
    public void neighborChanged(BlockState state, Level level, BlockPos pos, Block block, BlockPos from, boolean moving) {
        if (level.isClientSide) return;
        if (findCore(level, pos) == null) level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
    }

    @Override
    public boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        return true;
    }

    //? if < 1.21.1 {
    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        BlockPos core = findCore(level, pos);
        if (core == null) return InteractionResult.PASS;
        return ChemMachines.standardOpen(level, core, player);
    }
    //?} else {
    /*@Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        BlockPos core = findCore(level, pos);
        if (core == null) return InteractionResult.PASS;
        return ChemMachines.standardOpen(level, core, player);
    }
    *///?}

    @Override
    public void printHook(GuiGraphics g, Level world, BlockPos pos) {
        BlockPos core = findCore(world, pos);
        if (core != null && world.getBlockState(core).getBlock() instanceof ILookOverlay overlay) {
            overlay.printHook(g, world, core);
        }
    }

    /** Wie die Phantomzellen: Gegenstand = Maschine. */
    //? if < 1.21.1 {
    @Override
    public ItemStack getCloneItemStack(BlockGetter level, BlockPos pos, BlockState state) {
        return new ItemStack(ChemMachines.HYDROBAY.get());
    }
    //?} else {
    /*@Override
    public ItemStack getCloneItemStack(LevelReader level, BlockPos pos, BlockState state) {
        return new ItemStack(ChemMachines.HYDROBAY.get());
    }
    *///?}
}
