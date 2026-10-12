package com.space_m.rocket.transport;

import java.util.Arrays;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.hbm_m.api.fluids.ConnectionPriority;
import com.hbm_m.api.fluids.IFluidStandardReceiverMK2;
import com.hbm_m.api.fluids.IFluidStandardTransceiverMK2;
import com.hbm_m.api.tile.IControlReceiver;
import com.hbm_m.blockentity.BaseMachineBlockEntity;
import com.hbm_m.inventory.fluid.ModFluids;
import com.hbm_m.inventory.fluid.tank.FluidTank;
import com.hbm_m.inventory.menu.MultiblockMenuReach;
import com.hbm_m.network.NBTControlPacket;
import com.hbm_m.platform.ModItemStackHandler;
import com.hbm_m.platform.StackNbt;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;

/**
 * 1:1 NTM Space {@code TileEntityTransporterBase}: Sender/Empfaenger fuer Items und Fluide zwischen zwei verknuepften
 * Transportern (auch ueber Dimensionen/Himmelskoerper hinweg).
 *
 * <p>Inventar: {@code slotCount} Item-Plaetze (erste Haelfte Eingabe, zweite Ausgabe), danach je Eingabetank ein
 * Fluid-Identifikator-Platz und je Zusatztank (Treibstoff) einer. Tanks: erste Haelfte Eingabe (wird gesendet), zweite
 * Ausgabe (empfaengt), danach Zusatztanks (Treibstoff, eigener {@link FuelReceiver} mit Prioritaet HIGH).
 * Jeden Server-Tick: Tanktypen aus den Identifikatoren, gleiche Tanks gleichmaessig verteilen, Anschluesse
 * (Fluidnetz, Inserter/Extraktor), Partner holen, bei {@link #canSend} alles uebertragen und {@link #hasSent} melden.
 *
 * <p>Ueber Dimensionen hinweg robust: der Partner wird ueber {@link TransporterInfo} (Dimension + Position) gesucht;
 * beide Rampen halten ihren Chunk erzwungen ({@link TransporterChunkLoader}, wie im Original), dadurch sind sie in jeder
 * Dimension geladen. Eine zwischengespeicherte Referenz wird verworfen, sobald der Partner entladen/entfernt ist
 * ({@code isRemoved}); ungeladene Partner-Chunks werden hoechstens alle {@value #REFETCH_COOLDOWN} Ticks synchron
 * nachgeladen (Original: {@code DimensionManager.getWorld(..).getTileEntity} jeden Tick).
 * Original-Klasse: TileEntityTransporterBase
 */
public abstract class TransporterBlockEntityBase extends BaseMachineBlockEntity implements IControlReceiver, IFluidStandardTransceiverMK2 {

    private static final int REFETCH_COOLDOWN = 100;

    private String name = "Transporter";

    public FluidTank[] tanks;

    // The transporter we're sending our contents to
    @Nullable protected TransporterBlockEntityBase linkedTransporter;
    @Nullable private TransporterInfo linkedTransporterInfo;

    int inputSlotMax;
    int outputSlotMax;

    private int inputTankMax;
    private int outputTankMax;

    private int refetchCooldown;
    private boolean chunkForced;

    private final FuelReceiver fuelReceiver = new FuelReceiver();

    protected TransporterBlockEntityBase(BlockEntityType<?> type, BlockPos pos, BlockState state,
                                         int slotCount, int tankCount, int tankSize) {
        this(type, pos, state, slotCount, tankCount, tankSize, 0, 0, 0);
    }

    protected TransporterBlockEntityBase(BlockEntityType<?> type, BlockPos pos, BlockState state,
                                         int slotCount, int tankCount, int tankSize, int extraSlots, int extraTanks, int extraTankSize) {
        super(type, pos, state, slotCount + tankCount / 2 + extraSlots + extraTanks, 0L, 0L);

        tanks = new FluidTank[tankCount + extraTanks];
        for (int i = 0; i < tankCount; i++) {
            tanks[i] = new FluidTank(ModFluids.NONE.getSource(), tankSize);
        }
        for (int i = tankCount; i < tankCount + extraTanks; i++) {
            tanks[i] = new FluidTank(ModFluids.NONE.getSource(), extraTankSize);
        }

        inputSlotMax = slotCount / 2;
        outputSlotMax = slotCount;

        inputTankMax = tankCount / 2;
        outputTankMax = tankCount;
    }

    /**
     * Automatisierung (Original {@code ISidedInventory}): einsetzen nur in die Eingabe ({@code isItemValidForSlot}),
     * entnehmen nur aus der Ausgabe ({@code canExtractItem}). GUI und interne Logik gehen an den Regeln vorbei.
     */
    @Override
    protected ModItemStackHandler createInventoryHandler(int size) {
        return new ModItemStackHandler(size) {
            @Override
            protected void onContentsChanged(int slot) {
                setChanged();
            }

            @Override
            public boolean isItemValid(int slot, @NotNull ItemStack stack) {
                return isItemValidForSlot(slot, stack);
            }

            @Override
            public @NotNull ItemStack extractItem(int slot, int amount, boolean simulate) {
                if (slot < inputSlotMax || slot >= outputSlotMax) return ItemStack.EMPTY;
                return super.extractItem(slot, amount, simulate);
            }
        };
    }

    @Override
    protected boolean isItemValidForSlot(int slot, ItemStack stack) {
        return slot < inputSlotMax;
    }

    public ItemStack getStack(int slot) {
        return inventory.getStackInSlot(slot);
    }

    public void setStack(int slot, ItemStack stack) {
        inventory.setStackInSlot(slot, stack);
    }

    public int getSlotCount() {
        return inventory.getSlots();
    }

    /** Kein Stromanschluss (Original ohne {@code IEnergyReceiverMK2}). */
    @Override
    public boolean canConnectEnergy(Direction side) {
        return false;
    }

    // ==================== Tick ====================

    public static void tick(Level level, BlockPos pos, BlockState state, TransporterBlockEntityBase be) {
        if (level.isClientSide) be.clientTick();
        else be.serverTick((ServerLevel) level);
    }

    protected void clientTick() { }

    protected void serverTick(ServerLevel level) {
        if (!chunkForced) {
            // Original BlockTransporterRocket.onBlockPlacedBy -> ChunkLoaderManager.forceChunk (idempotent, auch fuer Altbestand)
            TransporterChunkLoader.forceChunk(level, worldPosition);
            chunkForced = true;
        }

        // Set tank types and split fills
        ItemStack[] slots = slotArray();
        for (int i = 0; i < inputTankMax; i++) {
            tanks[i].setType(outputSlotMax + i, slots);

            // Evenly distribute fluids between all matching tanks
            for (int o = i + 1; o < inputTankMax; o++) {
                splitFill(tanks[i], tanks[o]);
            }
        }
        for (int i = inputTankMax; i < outputTankMax; i++) {
            for (int o = i + 1; o < outputTankMax; o++) {
                splitFill(tanks[i], tanks[o]);
            }
        }
        for (int i = outputTankMax; i < tanks.length; i++) {
            tanks[i].setType(outputSlotMax + inputTankMax + i - outputTankMax, slots);
        }

        updateConnections(level);

        fetchLinkedTransporter();

        if (linkedTransporter != null && canSend(linkedTransporter)) {
            boolean isDirty = false;

            int sentItems = 0;
            int sentFluid = 0;

            // Move all items into the target
            for (int i = 0; i < inputSlotMax; i++) {
                ItemStack stack = getStack(i);
                if (!stack.isEmpty()) {
                    int beforeSize = stack.getCount();
                    ItemStack rest = linkedTransporter.addToInput(stack.copy());
                    setStack(i, rest);
                    int afterSize = rest.getCount();
                    sentItems += beforeSize - afterSize;
                    isDirty = true;
                }
            }

            // Move all fluids into the target
            for (int i = 0; i < inputTankMax; i++) {
                int o = i + inputTankMax;

                linkedTransporter.tanks[o].setTankType(tanks[i].getTankType());

                int sourceFillLevel = tanks[i].getFill();
                int targetFillLevel = linkedTransporter.tanks[o].getFill();

                int spaceAvailable = linkedTransporter.tanks[o].getMaxFill() - targetFillLevel;
                int amountToSend = Math.min(sourceFillLevel, spaceAvailable);

                if (amountToSend > 0) {
                    linkedTransporter.tanks[o].setFill(targetFillLevel + amountToSend);
                    tanks[i].setFill(sourceFillLevel - amountToSend);
                    sentFluid += amountToSend;
                    isDirty = true;
                }
            }

            hasSent(linkedTransporter, sentItems + (sentFluid / 1000));

            if (isDirty) {
                setChanged();
                linkedTransporter.setChanged();
            }
            linkedTransporter.sendUpdateToClient();
        }

        sendUpdateToClient(); // networkPackNT(250)
    }

    /** Original {@code InventoryUtil.tryAddItemToInventory(slots, inputSlotMax, outputSlotMax - 1, stack)} beim Empfaenger. */
    ItemStack addToInput(ItemStack stack) {
        for (int i = inputSlotMax; i < outputSlotMax && !stack.isEmpty(); i++) {
            ItemStack own = getStack(i);
            if (!own.isEmpty() && StackNbt.sameItemSameTags(own, stack) && own.getCount() < own.getMaxStackSize()) {
                int move = Math.min(stack.getCount(), own.getMaxStackSize() - own.getCount());
                own.grow(move);
                stack.shrink(move);
                setStack(i, own);
            }
        }
        for (int i = inputSlotMax; i < outputSlotMax && !stack.isEmpty(); i++) {
            if (getStack(i).isEmpty()) {
                int move = Math.min(stack.getCount(), stack.getMaxStackSize());
                setStack(i, stack.split(move));
            }
        }
        return stack.isEmpty() ? ItemStack.EMPTY : stack;
    }

    private ItemStack[] slotArray() {
        ItemStack[] slots = new ItemStack[inventory.getSlots()];
        for (int i = 0; i < slots.length; i++) slots[i] = inventory.getStackInSlot(i);
        return slots;
    }

    private void updateConnections(ServerLevel level) {
        // Sending/Receiving tanks
        for (DirPos pos : getConPos()) {
            for (int i = 0; i < outputTankMax; i++) {
                if (tanks[i].getTankType() != ModFluids.NONE.getSource()) {
                    trySubscribe(tanks[i].getTankType(), level, pos.pos(), pos.dir());
                    tryProvide(tanks[i], level, pos.pos(), pos.dir());
                }
            }
        }

        // Fuel tanks
        for (DirPos pos : getTankPos()) {
            for (int i = outputTankMax; i < tanks.length; i++) {
                if (tanks[i].getTankType() != ModFluids.NONE.getSource()) {
                    fuelReceiver.trySubscribe(tanks[i].getTankType(), level, pos.pos(), pos.dir());
                }
            }
        }

        // Inserter
        for (DirPos pos : getInsertPos()) {
            if (level.isLoaded(pos.pos())) TransportInventory.tryLoad(level, pos.pos(), pos.dir(), this);
        }

        // Extractor
        for (DirPos pos : getExtractPos()) {
            if (level.isLoaded(pos.pos())) TransportInventory.tryUnload(level, pos.pos(), pos.dir(), this);
        }
    }

    /** Original {@code FuelReceiver}: nur die Zusatztanks, Prioritaet HIGH. */
    private class FuelReceiver implements IFluidStandardReceiverMK2 {

        boolean valid = true;

        @Override
        public boolean isLoaded() {
            return valid && TransporterBlockEntityBase.this.isLoaded();
        }

        @Override
        public FluidTank[] getAllTanks() {
            return TransporterBlockEntityBase.this.getAllTanks();
        }

        @Override
        public FluidTank[] getReceivingTanks() {
            return Arrays.copyOfRange(tanks, outputTankMax, tanks.length);
        }

        @Override
        public ConnectionPriority getFluidPriority() {
            return ConnectionPriority.HIGH;
        }
    }

    @Override
    public void setRemoved() {
        super.setRemoved();
        fuelReceiver.valid = false;
    }

    @Override
    public void clearRemoved() {
        super.clearRemoved();
        fuelReceiver.valid = true;
    }

    @Override
    public boolean isLoaded() {
        return level != null && !isRemoved() && level.isLoaded(worldPosition);
    }

    // splitting is commutative, order don't matter
    private void splitFill(FluidTank in, FluidTank out) {
        if (in.getTankType() == out.getTankType()) {
            int fill = in.getFill() + out.getFill();

            float iFill = in.getMaxFill();
            float oFill = out.getMaxFill();
            float total = iFill + oFill;
            float iFrac = iFill / total;
            float oFrac = oFill / total;

            in.setFill(Mth.ceil(iFrac * (float) fill));
            out.setFill(Mth.floor(oFrac * (float) fill));

            // cap filling (this will generate 1mB of fluid in rare cases)
            if (out.getFill() == out.getMaxFill() - 1) out.setFill(out.getMaxFill());
        }
    }

    @Override
    public FluidTank[] getSendingTanks() {
        return Arrays.copyOfRange(tanks, inputTankMax, outputTankMax);
    }

    @Override
    public FluidTank[] getReceivingTanks() {
        return Arrays.copyOfRange(tanks, 0, inputTankMax);
    }

    @Override
    public FluidTank[] getAllTanks() {
        return tanks;
    }

    public record DirPos(BlockPos pos, Direction dir) {}

    protected abstract DirPos[] getConPos();
    protected abstract DirPos[] getTankPos();
    protected abstract DirPos[] getInsertPos();
    protected abstract DirPos[] getExtractPos();

    // Designated overrides for delaying sending or requiring fuel
    protected abstract boolean canSend(TransporterBlockEntityBase linkedTransporter);
    protected abstract void hasSent(TransporterBlockEntityBase linkedTransporter, int quantitySent);
    protected abstract void hasConnected(TransporterBlockEntityBase linkedTransporter);

    // Turns items and fluids into a "mass" of sorts
    protected int itemCount() {
        int count = 0;
        for (int i = 0; i < inputSlotMax; i++) {
            ItemStack stack = getStack(i);
            if (!stack.isEmpty()) count += stack.getCount();
        }
        for (int i = 0; i < inputTankMax; i++) {
            count += tanks[i].getFill() / 1000;
        }
        return count;
    }

    public String getTransporterName() {
        return name;
    }

    /** Original {@code setTransporterName}: lokal setzen + {@code NBTControlPacket} an den Server (nur Client). */
    public void setTransporterName(String name) {
        this.name = name;
        CompoundTag data = new CompoundTag();
        data.putString("name", name);
        NBTControlPacket.sendToServer(worldPosition, data);
    }

    @Nullable
    public static MinecraftServer server(@Nullable Level level) {
        return level != null ? level.getServer() : null;
    }

    private void fetchLinkedTransporter() {
        if (linkedTransporter != null && (linkedTransporter.isRemoved() || linkedTransporter.getLevel() == null)) {
            linkedTransporter = null; // Partner-Chunk entladen oder Partner abgebaut -> neu suchen
        }

        if (linkedTransporter == null && linkedTransporterInfo != null) {
            MinecraftServer server = server(level);
            if (server == null) return;
            ServerLevel transporterWorld = server.getLevel(linkedTransporterInfo.levelKey());
            if (transporterWorld == null) return;

            BlockPos target = linkedTransporterInfo.pos();
            if (!transporterWorld.isLoaded(target)) {
                if (refetchCooldown > 0) {
                    refetchCooldown--;
                    return;
                }
                refetchCooldown = REFETCH_COOLDOWN;
            }

            BlockEntity te = transporterWorld.getBlockEntity(target); // laedt den Chunk wie das Original
            if (te instanceof TransporterBlockEntityBase transporter) {
                linkedTransporter = transporter;
            }
        }
    }

    @Nullable
    public TransporterInfo getLinkedTransporter() {
        return linkedTransporterInfo;
    }

    @Nullable
    public TransporterBlockEntityBase getLinkedTransporterEntity() {
        return linkedTransporter;
    }

    public ResourceLocation dimensionId() {
        return level != null ? level.dimension().location() : Level.OVERWORLD.location();
    }

    // ==================== NBT (Original-Schluessel; Dimension als String) ====================

    @Override
    protected void writeNbtData(@NotNull CompoundTag nbt, @Nullable net.minecraft.core.HolderLookup.Provider registries) {
        super.writeNbtData(nbt, registries);
        nbt.putString("name", name);
        if (linkedTransporterInfo != null) {
            nbt.putString("dimension", linkedTransporterInfo.dimension.toString());
            nbt.putIntArray("linkedTo", new int[] { linkedTransporterInfo.x, linkedTransporterInfo.y, linkedTransporterInfo.z });
        }
        for (int i = 0; i < tanks.length; i++) tanks[i].writeToNBT(nbt, "t" + i);
    }

    @Override
    protected void readNbtData(@NotNull CompoundTag nbt, @Nullable net.minecraft.core.HolderLookup.Provider registries) {
        super.readNbtData(nbt, registries);
        name = nbt.getString("name");
        linkedTransporter = null;
        int[] coords = nbt.getIntArray("linkedTo");
        ResourceLocation dim = ResourceLocation.tryParse(nbt.getString("dimension"));
        if (coords.length > 2 && dim != null) {
            linkedTransporterInfo = new TransporterInfo("Linked Transporter", dim, coords[0], coords[1], coords[2]);
        } else {
            linkedTransporterInfo = null;
        }
        for (int i = 0; i < tanks.length; i++) tanks[i].readFromNBT(nbt, "t" + i);
    }

    public void unlinkTransporter() {
        fetchLinkedTransporter();
        if (linkedTransporter != null) {
            linkedTransporter.linkedTransporter = null;
            linkedTransporter.linkedTransporterInfo = null;
            linkedTransporter.setChanged();
            linkedTransporter.sendUpdateToClient();
        }

        linkedTransporter = null;
        linkedTransporterInfo = null;
    }

    // Is commutative, will automatically link and unlink its pair
    @Override
    public void receiveControl(CompoundTag nbt) {
        if (nbt.contains("name")) name = nbt.getString("name");
        if (nbt.contains("unlink")) {
            unlinkTransporter();
        }
        if (nbt.contains("linkedTo")) {
            // If already linked, unlink the target
            fetchLinkedTransporter();
            if (linkedTransporter != null) {
                linkedTransporter.linkedTransporter = null;
                linkedTransporter.linkedTransporterInfo = null;
                linkedTransporter.setChanged();
                linkedTransporter.sendUpdateToClient();
            }

            linkedTransporter = null;

            int[] coords = nbt.getIntArray("linkedTo");
            ResourceLocation dim = ResourceLocation.tryParse(nbt.getString("dimension"));
            if (coords.length < 3 || dim == null) return;
            linkedTransporterInfo = new TransporterInfo("Linked Transporter", dim, coords[0], coords[1], coords[2]);
            refetchCooldown = 0;

            fetchLinkedTransporter();

            if (linkedTransporter != null) {
                linkedTransporter.linkedTransporterInfo = TransporterInfo.from(dimensionId(), this);
                linkedTransporter.fetchLinkedTransporter();

                hasConnected(linkedTransporter);
                linkedTransporter.setChanged();
                linkedTransporter.sendUpdateToClient();
            }
        }

        setChanged();
        sendUpdateToClient();
    }

    /** Original {@code isUseableByPlayer} (TileEntityMachineBase: 128 Bloecke zum Kern). */
    @Override
    public boolean hasPermission(Player player) {
        return MultiblockMenuReach.stillValidCore(this, player, 128.0D);
    }

    // Fluide fuer die Tankanzeige (Client)
    public Fluid tankType(int i) {
        return tanks[i].getTankType();
    }
}
