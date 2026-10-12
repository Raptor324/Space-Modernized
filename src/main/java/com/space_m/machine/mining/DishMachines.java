package com.space_m.machine.mining;

import java.util.List;
import java.util.function.Supplier;

import org.jetbrains.annotations.Nullable;

import com.hbm_m.interfaces.IMultiblockPart;
import com.space_m.SpaceMod;
import com.space_m.block.SpaceBlocks;
import com.space_m.blockentity.SpaceBlockEntities;
import com.space_m.machine.chem.SpaceMachineBlockItem;
import com.space_m.main.SpaceCreativeTabs;

import dev.architectury.event.EventResult;
import dev.architectury.event.events.common.InteractionEvent;
import dev.architectury.registry.registries.RegistrySupplier;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.MapColor;

/**
 * R7-B StarDar-Fernkonsole ({@code dish_control}: Original {@code Material.iron}, Haerte 5, Widerstand 10 -> 6,
 * machineTab, Textur {@code block_steel_machine}; BE {@code dish_control}) und der Reaktorfuehler-Link des Space-Zweigs
 * ({@code ItemReactorSensor}): Rechtsklick auf einen StarDar speichert x/y/z im Fuehler ("Position Set!"), Rechtsklick auf
 * die Konsole verknuepft ("Stardar Linked!"), jeweils {@code techBoop}. Der Fuehler ist ein HBM-Gegenstand; die beiden
 * Space-Zweige haengen sich per Interaktions-Event davor (keine HBM-Aenderung noetig). Der StarDar selbst gehoert R7-A
 * ({@code space_m:machine_stardar}) - hier nur ueber die Block-ID angesprochen.
 */
public final class DishMachines {
    private DishMachines() {}

    public static final ResourceLocation STARDAR_ID = SpaceMod.id("machine_stardar");

    public static final RegistrySupplier<MachineDishControl> DISH_CONTROL = SpaceBlocks.dropsSelf(SpaceBlocks.registerWithItem("dish_control",
            () -> new MachineDishControl(BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(5.0F, 6.0F).sound(SoundType.STONE)
                    .requiresCorrectToolForDrops().noOcclusion().isSuffocating((s, w, p) -> false)),
            SpaceCreativeTabs.Tab.MACHINE, b -> new SpaceMachineBlockItem(b, new Item.Properties())));

    public static final RegistrySupplier<BlockEntityType<TileEntityDishControl>> DISH_CONTROL_BE =
            SpaceBlockEntities.register("dish_control", TileEntityDishControl::new, DISH_CONTROL);

    /** Spitzhacken-Bloecke (Datagen {@code mineable/pickaxe}). */
    public static List<Supplier<? extends Block>> pickaxeBlocks() {
        return List.of(DISH_CONTROL);
    }

    /** Block {@code space_m:machine_stardar} (R7-A). */
    public static boolean isStardar(BlockState state) {
        return STARDAR_ID.equals(BuiltInRegistries.BLOCK.getKey(state.getBlock()));
    }

    /** Original {@code findCore} am StarDar: Kern selbst oder Phantomzelle mit StarDar-Kern; sonst null. */
    @Nullable
    public static BlockPos stardarCore(BlockGetter level, BlockPos pos) {
        if (isStardar(level.getBlockState(pos))) return level.getBlockEntity(pos) != null ? pos : null;
        BlockEntity be = level.getBlockEntity(pos);
        if (be instanceof IMultiblockPart part && part.getControllerPos() != null && isStardar(level.getBlockState(part.getControllerPos()))) {
            return level.getBlockEntity(part.getControllerPos()) != null ? part.getControllerPos() : null;
        }
        return null;
    }

    /** Kern der Konsole (Kern selbst oder Phantomzelle). */
    @Nullable
    static BlockPos dishCore(BlockGetter level, BlockPos pos) {
        if (level.getBlockState(pos).getBlock() instanceof MachineDishControl) return pos;
        if (level.getBlockEntity(pos) instanceof IMultiblockPart part && part.getControllerPos() != null
                && level.getBlockState(part.getControllerPos()).getBlock() instanceof MachineDishControl) return part.getControllerPos();
        return null;
    }

    /** Aus {@code SpaceBlocks.init()} vor {@code BLOCKS.register()} (Klasse laden). */
    public static void init() { }

    /** Aus {@code SpaceRegistry.init()}: Reaktorfuehler-Erweiterung. */
    public static void commonInit() {
        InteractionEvent.RIGHT_CLICK_BLOCK.register((player, hand, pos, face) -> {
            ItemStack stack = player.getItemInHand(hand);
            if (stack.isEmpty() || stack.getItem() != com.hbm_m.item.ModItems.REACTOR_SENSOR.get()) return EventResult.pass();
            Level world = player.level();

            if (stardarCore(world, pos) != null) {
                if (!world.isClientSide) {
                    sendMessage(player, stack, "Position Set!");
                    CompoundTag tag = com.hbm_m.platform.StackNbt.orCreate(stack);
                    tag.putInt("x", pos.getX());
                    tag.putInt("y", pos.getY());
                    tag.putInt("z", pos.getZ());
                    com.hbm_m.platform.StackNbt.set(stack, tag);
                    boop(world, player);
                }
                player.swing(hand);
                return EventResult.interruptTrue();
            }

            BlockPos dish = dishCore(world, pos);
            if (dish != null) {
                if (!com.hbm_m.platform.StackNbt.has(stack)) return EventResult.pass();
                if (!(world.getBlockEntity(dish) instanceof TileEntityDishControl entity)) return EventResult.pass();

                if (!world.isClientSide) {
                    sendMessage(player, stack, "Stardar Linked!");
                    entity.tryLink(stack);
                    boop(world, player);
                }
                player.swing(hand);
                return EventResult.interruptTrue();
            }

            return EventResult.pass();
        });
    }

    private static void boop(Level world, Player player) {
        world.playSound(null, player.getX(), player.getY(), player.getZ(), com.hbm_m.sound.ModSounds.TOOL_TECH_BOOP.get(), SoundSource.PLAYERS, 1.0F, 1.0F);
    }

    /** Original {@code SendMessage}: "[<Fuehler>] text" (Klammern/Name dunkeltuerkis, Text gruen). */
    private static void sendMessage(Player player, ItemStack stack, String text) {
        player.sendSystemMessage(Component.literal("[").withStyle(ChatFormatting.DARK_AQUA)
                .append(Component.translatable(stack.getDescriptionId()).withStyle(ChatFormatting.DARK_AQUA))
                .append(Component.literal("] ").withStyle(ChatFormatting.DARK_AQUA))
                .append(Component.literal(text).withStyle(ChatFormatting.GREEN)));
    }
}
