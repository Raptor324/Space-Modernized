package com.space_m.orbit.propulsion;

import java.util.ArrayList;
import java.util.List;

import org.jetbrains.annotations.Nullable;

import com.hbm_m.block.ModBlocks;
import com.hbm_m.block.machines.DummyableMachineBlock;
import com.hbm_m.interfaces.ILookOverlay;
import com.hbm_m.multiblock.DummyableStructureBuilder;
import com.hbm_m.multiblock.MultiblockStructureHelper;
import com.hbm_m.sound.HbmSoundsNT;
import com.space_m.celestial.CelestialBody;
import com.space_m.celestial.SolarSystem;
import com.space_m.orbit.IPropulsion;
import com.space_m.orbit.OrbitalStation;
import com.space_m.orbit.OrbitalStation.StationState;
import com.space_m.rocket.ItemVOTVdrive;

import dev.architectury.registry.menu.MenuRegistry;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/**
 * 1:1 NTM Space {@code BlockOrbitalStationComputer} ({@code orbital_station_computer}): Einzelblock (BlockDummyable
 * {@code {0,0,0,0,0,0}}), nur im Orbit benutzbar. Rechtsklick waehrend der Reise: nichts. Mit Laufwerk in der Hand (Slot
 * leer, Ziel kein ORBIT): Reise zum Zielkoerper starten, Laufwerk verbrauchen, {@code item.upgradePlug}. Mit leerer Hand
 * und Laufwerk im Slot: Laufwerk zurueckgeben. Sonst GUI (Name/Schwerkraft).
 * Overlay: ausserhalb des Orbits "MUST BE IN ORBIT"; sonst keine Triebwerke / Fehler je Triebwerk / Fortschritt
 * (Zuendung, Reise, Abschalten, Prozent) / Laufwerk-Hinweise.
 * Original-Klasse: BlockOrbitalStationComputer
 */
public class BlockOrbitalStationComputer extends DummyableMachineBlock implements ILookOverlay {

    public BlockOrbitalStationComputer(Properties properties) {
        super(properties);
    }

    @Override
    protected MultiblockStructureHelper defineStructure() {
        return DummyableStructureBuilder.create().box(0, 0, 0, 0, 0, 0).placementOffset(0)
                .build(() -> ModBlocks.UNIVERSAL_MACHINE_PART.get().defaultBlockState());
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new TileEntityOrbitalStationComputer(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return createTickerHelper(type, PropulsionBlockEntities.ORBITAL_STATION_COMPUTER.get(), TileEntityOrbitalStationComputer::tick);
    }

    /** Original TESR ({@code RenderOrbitalComputer}). */
    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.ENTITYBLOCK_ANIMATED;
    }

    //? if < 1.21.1 {
    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        return onActivated(level, pos, player, player.getItemInHand(hand));
    }
    //?} else {
    /*@Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        return onActivated(level, pos, player, player.getMainHandItem());
    }

    @Override
    protected net.minecraft.world.ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        InteractionResult r = onActivated(level, pos, player, stack);
        return r.consumesAction() ? net.minecraft.world.ItemInteractionResult.sidedSuccess(level.isClientSide)
                : net.minecraft.world.ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }
    *///?}

    private InteractionResult onActivated(Level world, BlockPos pos, Player player, ItemStack heldStack) {
        if (!CelestialBody.inOrbit(world)) return InteractionResult.PASS;

        if (world.isClientSide) {
            return InteractionResult.SUCCESS;
        }

        if (!(world.getBlockEntity(pos) instanceof TileEntityOrbitalStationComputer computer)) return InteractionResult.PASS;

        if (computer.isTravelling()) return InteractionResult.PASS;

        if (!heldStack.isEmpty() && heldStack.getItem() instanceof ItemVOTVdrive) {
            if (!computer.getDrive().isEmpty()) return InteractionResult.PASS;

            ItemVOTVdrive.Destination destination = ItemVOTVdrive.getDestination(heldStack);

            if (destination.body() == SolarSystem.Body.ORBIT) return InteractionResult.PASS;

            if (computer.travelTo(destination.body().getBody(), heldStack.copy())) {
                heldStack.setCount(0);
                world.playSound(null, pos, HbmSoundsNT.get("item.upgradePlug"), SoundSource.BLOCKS, 1.0F, 1.0F);
            } else {
                return InteractionResult.PASS;
            }
        } else if (heldStack.isEmpty() && !computer.getDrive().isEmpty()) {
            ItemStack drive = computer.getDrive().copy();
            if (!player.getInventory().add(drive)) {
                player.drop(drive, false);
            }
            computer.setDrive(ItemStack.EMPTY);
        } else {
            MenuRegistry.openExtendedMenu((ServerPlayer) player, computer, buf -> buf.writeBlockPos(pos));
        }

        return InteractionResult.CONSUME;
    }

    @Override
    public void printHook(GuiGraphics g, Level world, BlockPos pos) {
        if (!CelestialBody.inOrbit(world)) {
            List<Component> text = new ArrayList<>();
            text.add(PropulsionUtil.blinkWarning("atmosphere.noOrbit"));
            ILookOverlay.printGeneric(g, Component.translatable(getDescriptionId()), 0xffff00, 0x404000, text);
            return;
        }

        if (!(world.getBlockEntity(pos) instanceof TileEntityOrbitalStationComputer computer)) return;

        OrbitalStation station = OrbitalStation.clientStation();
        double progress = station.getUnscaledProgress(0);
        List<Component> text = new ArrayList<>();

        if (!station.hasEngines) {
            text.add(Component.literal("No engines available").withStyle(ChatFormatting.RED));
        } else if (station.errorsAt.size() > 0) {
            for (BlockPos errorAt : station.errorsAt) {
                BlockEntity error = world.getBlockEntity(errorAt);
                if (!(error instanceof IPropulsion engine)) continue;

                List<Component> errors = new ArrayList<>();
                engine.addErrors(errors);

                if (!errors.isEmpty()) {
                    text.add(Component.translatable(error.getBlockState().getBlock().getDescriptionId()).withStyle(ChatFormatting.RED));
                    text.addAll(errors);
                }
            }
        } else if (progress > 0) {
            if (station.state == StationState.LEAVING) {
                text.add(Component.translatable("station.engage").append(": ").withStyle(ChatFormatting.AQUA)
                        .append(Component.translatable("body." + station.target.name).withStyle(ChatFormatting.RESET)));
            } else if (station.state == StationState.ARRIVING) {
                text.add(Component.translatable("station.disengage").withStyle(ChatFormatting.AQUA));
            } else {
                text.add(Component.translatable("station.travelling").append(": ").withStyle(ChatFormatting.AQUA)
                        .append(Component.translatable("body." + station.target.name).withStyle(ChatFormatting.RESET)));
            }
            text.add(Component.translatable("station.progress").append(": ").withStyle(ChatFormatting.AQUA)
                    .append(Component.literal(Math.round(progress * 100) + "%").withStyle(ChatFormatting.RESET)));
        } else if (computer.hasDrive) {
            Player player = (Player) (Object) Minecraft.getInstance().player; // R6-A: ueber Object, sonst laedt der Verifier LocalPlayer auf dem Server
            if (player != null && !player.getMainHandItem().isEmpty()) {
                text.add(Component.translatable("station.removeDrive"));
            } else {
                text.add(Component.translatable("station.interactDrive"));
            }
        } else {
            text.add(Component.translatable("station.insertDrive"));
        }

        ILookOverlay.printGeneric(g, Component.translatable(getDescriptionId()), 0xffff00, 0x404000, text);
    }

    //? if >1.20.1 {
    /*public static final com.mojang.serialization.MapCodec<BlockOrbitalStationComputer> CODEC = simpleCodec(BlockOrbitalStationComputer::new);
    @Override protected com.mojang.serialization.MapCodec<? extends net.minecraft.world.level.block.BaseEntityBlock> codec() { return CODEC; }
    *///?}
}
