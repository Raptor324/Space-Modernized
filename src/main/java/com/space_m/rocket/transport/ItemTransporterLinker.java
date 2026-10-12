package com.space_m.rocket.transport;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

import org.jetbrains.annotations.Nullable;

import com.hbm_m.platform.StackNbt;
import com.hbm_m.util.CompatExternal;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

/**
 * 1:1 NTM Space {@code ItemTransporterLinker} ({@code transporter_linker}): Schleich-Klick auf eine Transporter-Rampe
 * speichert sie im Linker ("Added transporter to linker"), normaler Klick oeffnet die Verknuepfungs-GUI fuer diese
 * Rampe. Solange der Linker in der Haupthand ist, schickt der Server alle 2 Ticks die gespeicherten (noch vorhandenen)
 * Rampen an den Client ({@link TransporterLinkerPacket}).
 *
 * <p>NBT (Abweichung, technisch noetig wegen Dimensions-Kennungen statt int-IDs): Liste {@code "transporters"} mit
 * {@code {dimension: "<ns:pfad>", coords: [x,y,z,...]}} statt {@code "dimensions"} + {@code "d<id>"}.
 * Original-Klasse: ItemTransporterLinker
 */
public class ItemTransporterLinker extends Item {

    /** Client: zuletzt empfangene Liste (Original {@code currentTransporters}). */
    public static List<TransporterInfo> currentTransporters = new ArrayList<>();

    /** Client-Hook zum Oeffnen der GUI (setzt {@code TransportClient}); Server: nichts. */
    public static Consumer<TransporterInfo> openGui = info -> {};

    public ItemTransporterLinker(Properties properties) {
        super(properties);
    }

    @Override
    //? if < 1.21.1 {
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> list, TooltipFlag flag) {
    //?} else {
    /*public void appendHoverText(ItemStack stack, Item.TooltipContext ctx, List<Component> list, TooltipFlag flag) {
    *///?}
        list.add(Component.literal("Sneak-click to save transporter"));
        list.add(Component.literal("Use on transporter to link to a saved transporter"));
    }

    @Override
    public InteractionResult useOn(UseOnContext ctx) {
        Level world = ctx.getLevel();
        Player player = ctx.getPlayer();
        BlockEntity tile = CompatExternal.getCoreFromPos(world, ctx.getClickedPos());

        if (!(tile instanceof TransporterBlockEntityBase transporter) || player == null) {
            return InteractionResult.PASS;
        }

        if (player.isShiftKeyDown()) {
            if (!world.isClientSide) {
                addTransporter(ctx.getItemInHand(), world, transporter);
                player.sendSystemMessage(Component.literal("Added transporter to linker"));
            }
        } else if (world.isClientSide) {
            openGui.accept(TransporterInfo.from(world.dimension().location(), transporter));
        }

        return InteractionResult.sidedSuccess(world.isClientSide);
    }

    @Override
    public void inventoryTick(ItemStack stack, Level world, Entity entity, int slot, boolean selected) {
        if (world.isClientSide || !(entity instanceof ServerPlayer player))
            return;

        if (player.getMainHandItem() != stack)
            return;

        if (entity.tickCount % 2 == 0) {
            List<TransporterInfo> transporters = getTransporters(stack, player.server, false);
            TransporterLinkerPacket.send(player, transporters);
        }
    }

    public static void addTransporter(ItemStack stack, Level world, TransporterBlockEntityBase transporter) {
        MinecraftServer server = world.getServer();
        if (server == null) return;
        Set<TransporterInfo> transporters = loadTransporters(stack, server, true);

        transporters.add(TransporterInfo.from(world.dimension().location(), transporter));

        saveTransporters(stack, transporters);
    }

    static List<TransporterInfo> getTransporters(ItemStack stack, MinecraftServer server, boolean allowLoad) {
        return new ArrayList<>(loadTransporters(stack, server, allowLoad));
    }

    /**
     * Original {@code loadTransporters}: nur Eintraege, an denen noch ein Transporter steht. {@code allowLoad} laedt
     * ungeladene Chunks wie das Original (nur beim Speichern); die 2-Tick-Anzeige ueberspringt sie (Rampen halten ihren
     * Chunk ohnehin erzwungen).
     */
    private static Set<TransporterInfo> loadTransporters(ItemStack stack, MinecraftServer server, boolean allowLoad) {
        Set<TransporterInfo> transporterCoordinates = new java.util.LinkedHashSet<>();
        CompoundTag tag = StackNbt.read(stack);
        if (tag == null) return transporterCoordinates;

        ListTag dims = tag.getList("transporters", Tag.TAG_COMPOUND);
        for (int d = 0; d < dims.size(); d++) {
            CompoundTag dimensionTag = dims.getCompound(d);
            ResourceLocation dimensionId = ResourceLocation.tryParse(dimensionTag.getString("dimension"));
            if (dimensionId == null) continue;
            ServerLevel world = server.getLevel(ResourceKey.create(Registries.DIMENSION, dimensionId));

            if (world == null) continue;

            int[] coordinateList = dimensionTag.getIntArray("coords");
            // 3 dimension strides
            for (int i = 0; i + 2 < coordinateList.length; i += 3) {
                BlockPos pos = new BlockPos(coordinateList[i], coordinateList[i + 1], coordinateList[i + 2]);
                if (!allowLoad && !world.isLoaded(pos)) {
                    // nicht geladen: Eintrag behalten, aber ohne aktuelle Daten
                    transporterCoordinates.add(new TransporterInfo("Transporter", dimensionId, pos.getX(), pos.getY(), pos.getZ()));
                    continue;
                }
                BlockEntity te = world.getBlockEntity(pos);
                if (te instanceof TransporterBlockEntityBase transporter) {
                    transporterCoordinates.add(TransporterInfo.from(dimensionId, transporter));
                }
            }
        }

        return transporterCoordinates;
    }

    private static void saveTransporters(ItemStack stack, Set<TransporterInfo> transporters) {
        Map<ResourceLocation, List<Integer>> data = new LinkedHashMap<>();

        for (TransporterInfo info : transporters) {
            List<Integer> list = data.computeIfAbsent(info.dimension, k -> new ArrayList<>());
            list.add(info.x);
            list.add(info.y);
            list.add(info.z);
        }

        ListTag dims = new ListTag();
        for (Map.Entry<ResourceLocation, List<Integer>> entry : data.entrySet()) {
            CompoundTag dimensionTag = new CompoundTag();
            dimensionTag.putString("dimension", entry.getKey().toString());
            dimensionTag.putIntArray("coords", entry.getValue().stream().mapToInt(Integer::intValue).toArray());
            dims.add(dimensionTag);
        }

        StackNbt.orCreate(stack).put("transporters", dims);
    }

    /** Fuer GameTests: gespeicherte Rampen (laedt Chunks wie das Original). */
    public static Set<TransporterInfo> saved(ItemStack stack, MinecraftServer server) {
        return new HashSet<>(loadTransporters(stack, server, true));
    }
}
