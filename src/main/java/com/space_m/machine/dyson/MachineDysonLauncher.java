package com.space_m.machine.dyson;

import java.util.ArrayList;
import java.util.List;

import org.jetbrains.annotations.Nullable;

import com.hbm_m.block.ModBlocks;
import com.hbm_m.block.decorations.TrinketTypes;
import com.hbm_m.item.ModItems;
import com.hbm_m.item.TrinketBlockItem;
import com.hbm_m.multiblock.DummyableStructureBuilder;
import com.hbm_m.multiblock.MultiblockStructureHelper;
import com.hbm_m.sound.ModSounds;
import com.hbm_m.util.BobMathUtil;
import com.space_m.machine.misc.R7MachineBlock;

import net.minecraft.ChatFormatting;
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
 * 1:1 NTM Space {@code MachineDysonLauncher} ("swarm shitter", {@code dyson_launcher}): Schleuderstartanlage fuer
 * Dyson-Schwarmmitglieder. Struktur = Original {@code fillSpace} (Beine, Rueckschuessel, Schuessel, Sockel; ohne den
 * {@code getDimensions}-Quader, der nur fuer Wand-/Deckenplatzierung dient), Offset 0, Anschlusszellen: fuenf Stromzellen
 * an der Seite ({@code -dir*0..4 - rot*2}) und die Ladezelle hinten ({@code -dir*8}).
 *
 * <p>Rechtsklick (nicht geschlichen): Satelliten-ID-Chip einsetzen (Slot 1, Geraeusch {@code item.upgradePlug}), mit leerer
 * Hand Chip herausnehmen; mit dem Pluesch "FAT TETO" ({@code PlushieType.FATO}) Sunset Overdrive einschalten. Overlay:
 * ID, Schwarmgroesse, Energie (rot unter Maximum) bzw. "No Satellite ID-Chip installed!".
 *
 * <p>Abweichung: HBM-Modernized kennt den Pluesch {@code FATO} nicht (nur Master-Typen) - der Overdrive ist damit derzeit
 * nicht ausloesbar (Pruefung ueber den Typnamen, wirkt sofort, sobald HBM den Typ fuehrt); NBT {@code overdrive} bleibt erhalten.
 * Original-Klasse: MachineDysonLauncher
 */
public class MachineDysonLauncher extends R7MachineBlock {

    public MachineDysonLauncher(Properties properties) {
        super(properties);
    }

    @Override
    protected MultiblockStructureHelper defineStructure() {
        DummyableStructureBuilder b = DummyableStructureBuilder.create();

        // LEG FUCKER
        b.boxAt(6, 14, 0, 0, 14, 1, 0, 6, -5);
        b.boxAt(6, 14, 0, 0, 14, 1, 0, -5, 6);

        // BACKDISH CRAPPER
        b.boxAt(2, 7, 0, 2, 0, 0, 3, 2, 2);
        b.boxAt(1, 6, 0, 2, 0, 0, 3, 3, 3);
        b.boxAt(0, 5, 0, 2, 0, 0, 3, 3, 3);
        b.boxAt(-1, 4, 0, 2, 0, 0, 3, 3, 3);
        b.boxAt(-2, 3, 0, 2, 0, 0, 3, 2, 2);

        // DISH SHITTER
        b.boxAt(6, 14, 0, 2, 0, 0, 0, 3, 3);
        b.boxAt(6, 14, 0, 0, 0, 0, 2, 3, 3);
        b.boxAt(5, 13, 0, 2, 0, 0, 2, 10, 5);
        b.boxAt(4, 12, 0, 2, 0, 0, 2, 10, 7);
        b.boxAt(3, 11, 0, 2, 0, 0, 2, 10, 8);
        b.boxAt(2, 10, 0, 2, 0, 0, 2, 10, 9);
        b.boxAt(1, 9, 0, 2, 0, 0, 2, 10, 10);
        b.boxAt(0, 8, 0, 2, 0, 0, 2, 10, 10);
        b.boxAt(-1, 7, 0, 2, 0, 0, 2, 10, 10);
        b.boxAt(-2, 6, 0, 2, 0, 0, 2, 10, 10);
        b.boxAt(-3, 5, 0, 2, 0, 0, 2, 10, 10);
        b.boxAt(-4, 4, 0, 2, 0, 0, 2, 9, 9);

        b.boxAt(-3, 3, 0, 2, 0, 2, 0, 8, 8);
        b.boxAt(-4, 2, 0, 2, 0, 2, 0, 7, 7);
        b.boxAt(-7, 2, 0, 1, 0, 0, 0, 6, 6);

        b.boxAt(0, 2, 0, 0, 0, 8, -8, 3, 3);
        b.boxAt(0, 1, 0, 1, 1, 8, -8, 1, 1);

        b.boxAt(-7, 0, 0, 1, 0, 0, 0, 4, 4);
        b.boxAt(-6, 0, 0, 1, 0, 0, 0, 5, 5);
        b.boxAt(-5, 0, 0, 1, 0, 0, 0, 6, 6);

        // BASE PIECE (OF ASS)
        b.box(3, 0, 7, 0, 2, 2);

        // and finally, extra metas
        for (int i = 0; i <= 4; i++) b.extra(-i, 0, -2);
        b.extra(-8, 0, 0);

        return b.placementOffset(0).build(() -> ModBlocks.UNIVERSAL_MACHINE_PART.get().defaultBlockState());
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new TileEntityDysonLauncher(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return createTickerHelper(type, DysonBlockEntities.LAUNCHER.get(), TileEntityDysonLauncher::tick);
    }

    @Override
    protected InteractionResult onActivated(Level world, BlockPos pos, BlockState state, Player player, ItemStack heldStack, InteractionHand hand) {
        if (world.isClientSide) {
            return InteractionResult.SUCCESS;
        } else if (!player.isShiftKeyDown()) {
            if (!(world.getBlockEntity(pos) instanceof TileEntityDysonLauncher launcher)) return InteractionResult.FAIL;

            ItemStack chip = launcher.getInventory().getStackInSlot(1);

            if (!heldStack.isEmpty() && heldStack.getItem() == ModItems.SAT_CHIP.get()) {
                if (!chip.isEmpty()) return InteractionResult.FAIL;

                launcher.getInventory().setStackInSlot(1, heldStack.copy());
                heldStack.setCount(0);
                world.playSound(null, pos, ModSounds.UPGRADE_PLUG.get(), SoundSource.PLAYERS, 1.0F, 1.0F);
            } else if (heldStack.isEmpty() && !chip.isEmpty()) {
                if (player.getInventory().add(chip.copy())) {
                    launcher.getInventory().setStackInSlot(1, ItemStack.EMPTY);
                    launcher.setChanged();
                    world.playSound(null, pos, ModSounds.UPGRADE_PLUG.get(), SoundSource.PLAYERS, 1.0F, 1.0F);
                }
            }

            if (!launcher.sunsetOverdrive && !heldStack.isEmpty() && isFatTeto(heldStack)) {
                launcher.sunsetOverdrive = true;
                heldStack.shrink(1);
            }
        }

        return InteractionResult.SUCCESS;
    }

    /** Original {@code block instanceof BlockPlushie && damage == PlushieType.FATO.ordinal()}. */
    private static boolean isFatTeto(ItemStack stack) {
        if (!(stack.getItem() instanceof TrinketBlockItem) || stack.getItem() != ModBlocks.PLUSHIE.get().asItem()) return false;
        int type = TrinketBlockItem.getType(stack);
        TrinketTypes.PlushieType[] types = TrinketTypes.PlushieType.values();
        return type >= 0 && type < types.length && types[type].name().equals("FATO");
    }

    @Override
    public void printHook(GuiGraphics g, Level world, BlockPos pos) {
        if (!(world.getBlockEntity(pos) instanceof TileEntityDysonLauncher launcher)) return;

        List<Component> text = new ArrayList<>();

        if (launcher.swarmId > 0) {
            text.add(Component.literal("ID: " + launcher.swarmId));
            text.add(Component.literal("Swarm: " + launcher.swarmCount + " members"));
            text.add(Component.literal("Power: " + BobMathUtil.getShortNumber(launcher.power()) + "HE")
                    .withStyle(launcher.power() < TileEntityDysonLauncher.MAX_POWER ? ChatFormatting.RED : ChatFormatting.GREEN));
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
    /*public static final com.mojang.serialization.MapCodec<MachineDysonLauncher> CODEC = simpleCodec(MachineDysonLauncher::new);
    @Override protected com.mojang.serialization.MapCodec<? extends net.minecraft.world.level.block.BaseEntityBlock> codec() { return CODEC; }
    *///?}
}
