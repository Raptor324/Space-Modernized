package com.space_m.machine.dyson;

import java.util.ArrayList;
import java.util.List;

import org.jetbrains.annotations.Nullable;

import com.hbm_m.block.ModBlocks;
import com.hbm_m.item.ModItems;
import com.hbm_m.multiblock.DummyableStructureBuilder;
import com.hbm_m.multiblock.MultiblockStructureHelper;
import com.hbm_m.sound.ModSounds;
import com.hbm_m.util.BobMathUtil;
import com.space_m.machine.misc.R7MachineBlock;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundSource;
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
 * 1:1 NTM Space {@code MachineDysonReceiver} ({@code dyson_receiver}): {@code getDimensions {2, 0, 4, 2, 2, 2}}, Offset 2,
 * dazu Hauptkoerper {@code {1,0,8,-4,1,1}} + {@code {4,0,4,0,1,1}}, Schuessel auf y+10 {@code {1,0,6,6,6,6}}, Turm
 * {@code {17,0,1,1,1,1}}. Rechtsklick: Satelliten-ID-Chip einsetzen/herausnehmen ({@code item.upgradePlug}). Overlay: ID,
 * Schwarmgroesse, Verbraucher und {@code getEnergyOutput(count) / consumers * 20} HE/s.
 * Original-Klasse: MachineDysonReceiver
 */
public class MachineDysonReceiver extends R7MachineBlock {

    public MachineDysonReceiver(Properties properties) {
        super(properties);
    }

    @Override
    protected MultiblockStructureHelper defineStructure() {
        DummyableStructureBuilder b = DummyableStructureBuilder.create().box(2, 0, 4, 2, 2, 2);
        // Main structure
        b.box(1, 0, 8, -4, 1, 1);
        b.box(4, 0, 4, 0, 1, 1);
        // Dish
        b.boxAt(0, 10, 0, 1, 0, 6, 6, 6, 6);
        // Tower
        b.box(17, 0, 1, 1, 1, 1);
        return b.placementOffset(2).build(() -> ModBlocks.UNIVERSAL_MACHINE_PART.get().defaultBlockState());
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new TileEntityDysonReceiver(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return createTickerHelper(type, DysonBlockEntities.RECEIVER.get(), TileEntityDysonReceiver::tick);
    }

    @Override
    protected InteractionResult onActivated(Level world, BlockPos pos, BlockState state, Player player, ItemStack heldStack, InteractionHand hand) {
        if (world.isClientSide) {
            return InteractionResult.SUCCESS;
        } else if (!player.isShiftKeyDown()) {
            if (!(world.getBlockEntity(pos) instanceof TileEntityDysonReceiver receiver)) return InteractionResult.FAIL;

            ItemStack chip = receiver.getInventory().getStackInSlot(0);

            if (!heldStack.isEmpty() && heldStack.getItem() == ModItems.SAT_CHIP.get()) {
                if (!chip.isEmpty()) return InteractionResult.FAIL;

                receiver.getInventory().setStackInSlot(0, heldStack.copy());
                receiver.setChanged();
                heldStack.setCount(0);
                world.playSound(null, pos, ModSounds.UPGRADE_PLUG.get(), SoundSource.PLAYERS, 1.0F, 1.0F);
            } else if (heldStack.isEmpty() && !chip.isEmpty()) {
                if (player.getInventory().add(chip.copy())) {
                    receiver.getInventory().setStackInSlot(0, ItemStack.EMPTY);
                    receiver.setChanged();
                    world.playSound(null, pos, ModSounds.UPGRADE_PLUG.get(), SoundSource.PLAYERS, 1.0F, 1.0F);
                }
            }
        }

        return InteractionResult.SUCCESS;
    }

    @Override
    public void printHook(GuiGraphics g, Level world, BlockPos pos) {
        if (!(world.getBlockEntity(pos) instanceof TileEntityDysonReceiver receiver)) return;

        long energyOutput = 0;
        if (receiver.isReceiving && receiver.swarmConsumers > 0) {
            energyOutput = TileEntityDysonReceiver.getEnergyOutput(receiver.swarmCount) / receiver.swarmConsumers * 20;
        }

        List<Component> text = new ArrayList<>();

        if (receiver.swarmId > 0) {
            text.add(Component.literal("ID: " + receiver.swarmId));
            text.add(Component.literal("Swarm: " + receiver.swarmCount + " members"));
            text.add(Component.literal("Consumers: " + receiver.swarmConsumers + " consumers"));
            text.add(Component.literal("Power: " + BobMathUtil.getShortNumber(energyOutput) + "HE/s"));
        } else {
            text.add(Component.literal("No Satellite ID-Chip installed!"));
        }

        print(g, text);
    }

    @Override
    protected boolean standardInfo() {
        return true;
    }

    //? if >1.20.1 {
    /*public static final com.mojang.serialization.MapCodec<MachineDysonReceiver> CODEC = simpleCodec(MachineDysonReceiver::new);
    @Override protected com.mojang.serialization.MapCodec<? extends net.minecraft.world.level.block.BaseEntityBlock> codec() { return CODEC; }
    *///?}
}
