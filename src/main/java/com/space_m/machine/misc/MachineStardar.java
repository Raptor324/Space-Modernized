package com.space_m.machine.misc;

import org.jetbrains.annotations.Nullable;

import com.hbm_m.block.ModBlocks;
import com.hbm_m.multiblock.DummyableStructureBuilder;
import com.hbm_m.multiblock.MultiblockStructureHelper;

import dev.architectury.registry.menu.MenuRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 1:1 NTM Space {@code MachineStardar} ({@code machine_stardar}, "StarDar"): Radioteleskop, {@code getDimensions
 * {0, 3, 2, 2, 2, 2}}, Offset 2, Hoehenversatz 3 (der Kern sitzt in der Schuessel-Ebene, drei Felder ueber dem Boden).
 *
 * <p>Struktur ({@code fillSpace}, Kern = Klickpunkt + {@code dir * 2}, drei Felder hoeher):
 * <ul>
 *   <li>Hauptkoerper {@code {0, 0, 2, 2, 2, 2}}: 5x5 in der Kernebene,</li>
 *   <li>acht Beine {@code {0, 3, 0, 0, 0, 0}} an {@code (x + ox, z + oz)} fuer {@code ox, oz} aus {-2, 0, 2} ausser der
 *       Mitte (Weltachsen; die Menge ist drehsymmetrisch, daher hier lokal identisch),</li>
 *   <li>"Rack": zwei Zellen bei {@code -rot + dir * 2} auf {@code y - 2} und {@code y - 3}.</li>
 * </ul>
 * Rechtsklick ({@code standardOpenBehavior}): nicht geschlichen -> GUI. Tooltip {@code addStandardInfo}.
 * Original-Klasse: MachineStardar
 */
public class MachineStardar extends R7MachineBlock {

    public MachineStardar(Properties properties) {
        super(properties);
    }

    @Override
    protected MultiblockStructureHelper defineStructure() {
        // Main body
        DummyableStructureBuilder b = DummyableStructureBuilder.create().box(0, 0, 2, 2, 2, 2);

        // Legs
        for (int ox = -2; ox <= 2; ox += 2) {
            for (int oz = -2; oz <= 2; oz += 2) {
                if (ox == 0 && oz == 0) continue;
                for (int dy = 0; dy >= -3; dy--) b.cell(oz, dy, ox);
            }
        }

        // Rack
        b.cell(2, -2, -1);
        b.cell(2, -3, -1);

        return b.placementOffset(2).build(() -> ModBlocks.UNIVERSAL_MACHINE_PART.get().defaultBlockState());
    }

    @Override
    public int getHeightOffset() {
        return 3;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new TileEntityMachineStardar(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return createTickerHelper(type, MiscBlockEntities.STARDAR.get(), TileEntityMachineStardar::tick);
    }

    /** Original {@code standardOpenBehavior(world, x, y, z, player, 0)}. */
    @Override
    protected InteractionResult onActivated(Level level, BlockPos pos, BlockState state, Player player, ItemStack held, InteractionHand hand) {
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        } else if (!player.isShiftKeyDown()) {
            if (!(level.getBlockEntity(pos) instanceof TileEntityMachineStardar stardar)) return InteractionResult.FAIL;
            MenuRegistry.openExtendedMenu((ServerPlayer) player, stardar, buf -> buf.writeBlockPos(pos));
            return InteractionResult.CONSUME;
        } else {
            return InteractionResult.CONSUME;
        }
    }

    /** Original {@code addInformation -> addStandardInfo}. */
    @Override
    protected boolean standardInfo() {
        return true;
    }

    //? if >1.20.1 {
    /*public static final com.mojang.serialization.MapCodec<MachineStardar> CODEC = simpleCodec(MachineStardar::new);
    @Override protected com.mojang.serialization.MapCodec<? extends net.minecraft.world.level.block.BaseEntityBlock> codec() { return CODEC; }
    *///?}
}
