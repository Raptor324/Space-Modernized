package com.space_m.rocket;

import java.util.ArrayList;
import java.util.List;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.hbm_m.api.tile.IControlReceiver;
import com.hbm_m.blockentity.BaseHbmBlockEntity;
import com.hbm_m.item.ISatChip;
import com.hbm_m.platform.ModItemStackHandler;
import com.hbm_m.platform.RenderBounds;
import com.space_m.blockentity.SpaceBlockEntities;
import com.space_m.rocket.RocketStruct.RocketStage;
import com.space_m.rocket.menu.RocketAssemblyMenu;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Containers;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

/**
 * Blockentity der VAB, 1:1 NTM Space {@code TileEntityMachineRocketAssembly}: Slot 0 Kapsel/Satellit, je Stufe Rumpf/
 * Landebeine/Triebwerk (Slots 1-15), Slot 16 fertige Rakete, Slots 17-26 Start-/Ziel-Laufwerk je Stufe (Geisterslots,
 * nur Anzeige der Reiseberechnung). Jeden Tick (Server) wird aus den Slots der {@link RocketStruct} gebaut, leere Stufen
 * werden nachgerueckt, bei geaenderter Hoehe Arbeitsbuehnen/Stuetzsaeulen neu gesetzt (oder "VAB ceiling too low" gemeldet).
 * Knoepfe (GUI -> {@code NBTControlPacket}): {@code construct} verbaut alles zu {@code rocket_custom}, {@code deconstruct}
 * zerlegt eine eingelegte Rakete wieder in ihre Teile.
 *
 * <p>Sync: Original {@code networkPackNT(250)} jeden Tick; hier Block-Update, sobald sich der Bauplan aendert (gleicher
 * Inhalt: Kapsel, Stufen, Hinweise). Slots sieht der Client ueber das Menue.
 * Original-Klasse: TileEntityMachineRocketAssembly
 */
public class MachineRocketAssemblyBlockEntity extends BaseHbmBlockEntity implements MenuProvider, IControlReceiver {

    public static final int SLOT_CAPSULE = 0;
    /** {@code 1 + MAX_STAGES * 3 + 1 + MAX_STAGES * 2}: capsule + stages + result + drives */
    public static final int SLOT_COUNT = 1 + RocketStruct.MAX_STAGES * 3 + 1 + RocketStruct.MAX_STAGES * 2;
    public static final int SLOT_RESULT = SLOT_COUNT - RocketStruct.MAX_STAGES * 2 - 1;
    public static final int SLOT_DRIVES = SLOT_RESULT + 1;

    /** Original {@code BlockDummyable.safeRem}: Plattformen umbauen, ohne dass deren Abbau die VAB zerstoert. */
    public static boolean safeRem = false;

    public RocketStruct rocket = new RocketStruct();

    private int previousHeight = 0;
    private List<Integer> platforms = new ArrayList<>();
    /** Hoehe der zuletzt gesetzten Saeulen (fuer den Abbau). */
    private int spireHeight = 0;

    private boolean platformFailed = false;

    public int currentStage;

    public boolean isBreaking;

    private CompoundTag lastSync = null;

    private final ModItemStackHandler inventory = new ModItemStackHandler(SLOT_COUNT) {
        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
        }

        @Override
        public int getSlotLimit(int slot) {
            return 8; // Original getInventoryStackLimit
        }

        @Override
        public boolean isItemValid(int slot, @NotNull ItemStack stack) {
            return true;
        }
    };

    public MachineRocketAssemblyBlockEntity(BlockPos pos, BlockState state) {
        super(SpaceBlockEntities.ROCKET_ASSEMBLY.get(), pos, state);
    }

    public ModItemStackHandler getInventory() {
        return inventory;
    }

    public ItemStack slot(int i) {
        return inventory.getStackInSlot(i);
    }

    private void set(int i, ItemStack stack) {
        inventory.setStackInSlot(i, stack == null ? ItemStack.EMPTY : stack);
    }

    /** Start-Laufwerk der aktuellen Stufe (Original {@code slots[slots.length - (MAX_STAGES - currentStage) * 2]}). */
    public ItemStack fromDrive(int stage) {
        return slot(SLOT_COUNT - (RocketStruct.MAX_STAGES - stage) * 2);
    }

    public ItemStack toDrive(int stage) {
        return slot(SLOT_COUNT - (RocketStruct.MAX_STAGES - stage) * 2 + 1);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, MachineRocketAssemblyBlockEntity be) {
        be.updateEntity();
    }

    public void updateEntity() {
        if (level == null || level.isClientSide) return;

        ItemStack fromStack = fromDrive(currentStage);
        ItemStack toStack = toDrive(currentStage);

        // updates the orbital station information and syncs it to the client, if necessary
        if (!fromStack.isEmpty()) ItemVOTVdrive.getTarget(fromStack, level);
        if (!toStack.isEmpty()) ItemVOTVdrive.getTarget(toStack, level);

        rocket = new RocketStruct(slot(0));
        if (!slot(0).isEmpty() && slot(0).getItem() instanceof ISatChip) {
            rocket.satFreq = ISatChip.getFreqS(slot(0));
        }
        for (int i = 1; i < RocketStruct.MAX_STAGES * 3; i += 3) {
            if (slot(i).isEmpty() && slot(i + 1).isEmpty() && slot(i + 2).isEmpty()) {
                // Check for later stages and shift them up into empty stages
                if (i + 3 < RocketStruct.MAX_STAGES * 3 && (!slot(i + 3).isEmpty() || !slot(i + 4).isEmpty() || !slot(i + 5).isEmpty())) {
                    set(i, slot(i + 3));
                    set(i + 1, slot(i + 4));
                    set(i + 2, slot(i + 5));
                    set(i + 3, ItemStack.EMPTY);
                    set(i + 4, ItemStack.EMPTY);
                    set(i + 5, ItemStack.EMPTY);
                } else {
                    break;
                }
            }
            rocket.addStage(slot(i), slot(i + 1), slot(i + 2));
        }

        int height = (int) rocket.getHeight();
        if (height != previousHeight) {
            safeRem = true;
            try {
                // Delete previously generated platforms
                for (int platform : platforms) {
                    deletePlatform(platform);
                }
                platforms = new ArrayList<>();

                // Check headroom
                int maxHeight = Integer.MAX_VALUE;
                for (int h = 1; h < 256; h++) {
                    BlockPos p = worldPosition.above(h);
                    if (p.getY() >= level.getMaxBuildHeight()) break;
                    BlockState block = level.getBlockState(p);
                    if (!block.canBeReplaced() && !isOwnBlock(block)) {
                        maxHeight = h;
                        break;
                    }
                }

                double checkHeight = rocket.getHeight();
                if (rocket.capsule != null) checkHeight -= rocket.capsule.height;
                if (rocket.stages.size() > 0 && rocket.stages.get(0).thruster != null) checkHeight -= rocket.stages.get(0).thruster.height;

                if (checkHeight < maxHeight) {
                    // Create platforms to stand on
                    int targetHeight = 0;
                    for (int i = 0; i < rocket.stages.size(); i++) {
                        RocketStage stage = rocket.stages.get(i);
                        RocketStage nextStage = i < rocket.stages.size() - 1 ? rocket.stages.get(i + 1) : null;

                        if (stage.fuselage != null) targetHeight += stage.fuselage.height * stage.getStack();
                        if (nextStage != null && nextStage.thruster != null) targetHeight += nextStage.thruster.height;

                        int platform = Math.round(targetHeight);

                        if (platform > 0) {
                            addPlatform(platform);
                            platforms.add(platform);
                        }
                    }

                    // Create a central spire (required so the VAB can be broken properly)
                    BlockState up = SpaceRocketBlocks.ROCKET_ASSEMBLY_PART.get().defaultBlockState().setValue(RocketAssemblyPartBlock.FACING, Direction.UP);
                    for (int i = 1; i < targetHeight + 1; i++) {
                        if (worldPosition.getY() + i >= level.getMaxBuildHeight()) break;
                        for (int[] o : SPIRES) level.setBlock(worldPosition.offset(o[0], i, o[1]), up, 3);
                    }

                    for (int[] o : SPIRES) {
                        for (int i = targetHeight + 1; i < 256 && level.getBlockState(worldPosition.offset(o[0], i, o[1])).getBlock() instanceof RocketAssemblyPartBlock; i++) {
                            level.setBlock(worldPosition.offset(o[0], i, o[1]), net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), 3);
                        }
                    }
                    spireHeight = targetHeight;

                    platformFailed = false;
                } else {
                    platformFailed = true;
                }
            } finally {
                safeRem = false;
            }
            previousHeight = height;
            setChanged();
        }

        if (platformFailed) {
            rocket.addIssue(ChatFormatting.RED + "VAB ceiling too low ");
        }

        // networkPackNT(250) -> Block-Update bei Aenderung
        CompoundTag sync = syncTag();
        if (!sync.equals(lastSync)) {
            lastSync = sync;
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    /** Mitte und die vier Ecken (Original: x/z, x-4/z-4, x+4/z-4, x-4/z+4, x+4/z+4). */
    private static final int[][] SPIRES = { { 0, 0 }, { -4, -4 }, { 4, -4 }, { -4, 4 }, { 4, 4 } };

    private static boolean isOwnBlock(BlockState state) {
        Block b = state.getBlock();
        if (b instanceof MachineRocketAssemblyBlock || b instanceof RocketAssemblyPartBlock) return true;
        return false;
    }

    public static void runSafe(Runnable r) {
        boolean old = safeRem;
        safeRem = true;
        try {
            r.run();
        } finally {
            safeRem = old;
        }
    }

    public void addPlatform(int height) {
        for (int x = -4; x <= 4; x++) {
            for (int z = -4; z <= 4; z++) {
                Direction dir;

                if ((x == -4 || x == 4) && (z == -4 || z == 4)) continue;

                if (x < 0) {
                    dir = Direction.WEST;
                } else if (x > 0) {
                    dir = Direction.EAST;
                } else if (z < 0) {
                    dir = Direction.NORTH;
                } else if (z > 0) {
                    dir = Direction.SOUTH;
                } else {
                    continue;
                }

                BlockPos p = worldPosition.offset(x, height, z);
                if (p.getY() >= level.getMaxBuildHeight()) continue;
                level.setBlock(p, SpaceRocketBlocks.ROCKET_ASSEMBLY_PART.get().defaultBlockState().setValue(RocketAssemblyPartBlock.FACING, dir), 3);
            }
        }
    }

    public void deletePlatform(int height) {
        for (int x = -4; x <= 4; x++) {
            for (int z = -4; z <= 4; z++) {
                if (x == 0 && z == 0) continue;
                if ((x == -4 || x == 4) && (z == -4 || z == 4)) continue;

                BlockPos p = worldPosition.offset(x, height, z);
                if (level.getBlockState(p).getBlock() instanceof RocketAssemblyPartBlock)
                    level.setBlock(p, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), 3);
            }
        }
    }

    /** Abbau der VAB: alle gesetzten Buehnen und Saeulen entfernen (Original: Dummys zerfallen ohne Kern). */
    public void removeGenerated() {
        if (level == null) return;
        runSafe(() -> {
            for (int platform : platforms) deletePlatform(platform);
            for (int[] o : SPIRES) {
                for (int i = 1; i < 256 && level.getBlockState(worldPosition.offset(o[0], i, o[1])).getBlock() instanceof RocketAssemblyPartBlock; i++) {
                    level.setBlock(worldPosition.offset(o[0], i, o[1]), net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), 3);
                }
            }
        });
        platforms = new ArrayList<>();
    }

    // ── Original getSizeInventory: beim Abbau ohne die Laufwerk-Geisterslots ─────────────────

    public void dropInventoryContents() {
        if (level == null) return;
        int size = isBreaking ? SLOT_COUNT - RocketStruct.MAX_STAGES * 2 : SLOT_COUNT;
        for (int i = 0; i < size; i++) {
            ItemStack stack = slot(i);
            if (!stack.isEmpty()) Containers.dropItemStack(level, worldPosition.getX(), worldPosition.getY(), worldPosition.getZ(), stack.copy());
            set(i, ItemStack.EMPTY);
        }
    }

    // ── Bauen / Zerlegen ─────────────────────────────────────────────────────────────────────

    public void construct() {
        if (!rocket.validate()) return;

        ItemStack stack = ItemCustomRocket.build(rocket);
        if (!slot(0).isEmpty() && RocketPayloadHooks.get().isSatelliteItem(slot(0).getItem())) {
            RocketPayloadHooks.get().copyItemData(slot(0), stack);
        }
        if (rocket.satFreq != 0) ISatChip.setFreqS(stack, rocket.satFreq);
        set(SLOT_RESULT, stack);

        for (int i = 0; i < SLOT_RESULT; i++) {
            set(i, ItemStack.EMPTY);
        }
    }

    public boolean canDeconstruct() {
        RocketStruct rocket = ItemCustomRocket.get(slot(SLOT_RESULT));
        if (rocket == null) return false;
        for (int i = 0; i < SLOT_RESULT; i++) {
            if (!slot(i).isEmpty()) return false;
        }

        return true;
    }

    public void deconstruct() {
        if (!canDeconstruct()) return;
        ItemStack source = slot(SLOT_RESULT);
        int satFreq = ISatChip.getFreqS(source);
        RocketStruct rocket = ItemCustomRocket.get(source);
        if (rocket.capsule == null || rocket.capsule.part == null) return;

        set(0, new ItemStack(rocket.capsule.part));
        if (slot(0).getItem() instanceof ISatChip) {
            ISatChip.setFreqS(slot(0), satFreq);
            RocketPayloadHooks.get().copyItemData(source, slot(0));
        }
        for (int i = 0; i < rocket.stages.size(); i++) {
            int o = i * 3;
            RocketStage stage = rocket.stages.get(rocket.stages.size() - 1 - i);
            if (stage.fuselage != null && stage.fuselage.part != null) set(o + 1, new ItemStack(stage.fuselage.part, stage.fuselageCount));
            if (stage.fins != null && stage.fins.part != null) set(o + 2, new ItemStack(stage.fins.part));
            if (stage.thruster != null && stage.thruster.part != null) set(o + 3, new ItemStack(stage.thruster.part, stage.thrusterCount));
        }

        set(SLOT_RESULT, ItemStack.EMPTY);
    }

    // ── IControlReceiver ─────────────────────────────────────────────────────────────────────

    @Override
    public boolean hasPermission(Player player) {
        return level != null && level.getBlockEntity(worldPosition) == this
                && player.distanceToSqr(worldPosition.getX() + 0.5, worldPosition.getY() + 0.5, worldPosition.getZ() + 0.5) <= 64 * 64;
    }

    @Override
    public void receiveControl(CompoundTag data) {
        if (data.getBoolean("construct")) {
            construct();
        }
        if (data.getBoolean("deconstruct")) {
            deconstruct();
        }
    }

    /** Original {@code IStage.setCurrentStage} (aus {@code SlotDrive.setLayer}). */
    public void setCurrentStage(int stage) {
        currentStage = Math.max(0, Math.min(stage, RocketStruct.MAX_STAGES - 1));
    }

    // ── NBT / Sync ───────────────────────────────────────────────────────────────────────────

    private CompoundTag syncTag() {
        CompoundTag tag = new CompoundTag();
        rocket.writeToNBT(tag);
        net.minecraft.nbt.ListTag issues = new net.minecraft.nbt.ListTag();
        for (String s : rocket.extraIssues) issues.add(net.minecraft.nbt.StringTag.valueOf(s));
        tag.put("issues", issues);
        return tag;
    }

    @Override
    protected void writeNbtData(@NotNull CompoundTag tag, @Nullable net.minecraft.core.HolderLookup.Provider registries) {
        super.writeNbtData(tag, registries);
        tag.put("inventory", com.hbm_m.platform.ItemStackSerialization.serialize(inventory, registries));
        tag.putIntArray("platforms", platforms.stream().mapToInt(Integer::intValue).toArray());
        tag.putInt("prevHeight", previousHeight);
        tag.putBoolean("platformFailed", platformFailed);
        tag.put("rocket", syncTag());
    }

    @Override
    protected void readNbtData(@NotNull CompoundTag tag, @Nullable net.minecraft.core.HolderLookup.Provider registries) {
        super.readNbtData(tag, registries);
        if (tag.contains("inventory")) {
            com.hbm_m.platform.ItemStackSerialization.deserialize(inventory, tag.getCompound("inventory"), registries);
        }
        platforms = new ArrayList<>();
        for (int i : tag.getIntArray("platforms")) platforms.add(i);
        previousHeight = tag.getInt("prevHeight");
        platformFailed = tag.getBoolean("platformFailed");
        if (tag.contains("rocket")) {
            CompoundTag r = tag.getCompound("rocket");
            rocket = RocketStruct.readFromNBT(r);
            net.minecraft.nbt.ListTag issues = r.getList("issues", net.minecraft.nbt.Tag.TAG_STRING);
            for (int i = 0; i < issues.size(); i++) rocket.extraIssues.add(issues.getString(i));
        }
    }

    //? if forge {
    @Override
    //?}
    public AABB getRenderBoundingBox() {
        return RenderBounds.INFINITE;
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.space_m.machine_rocket_assembly");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inv, Player player) {
        return new RocketAssemblyMenu(id, inv, this);
    }
}
