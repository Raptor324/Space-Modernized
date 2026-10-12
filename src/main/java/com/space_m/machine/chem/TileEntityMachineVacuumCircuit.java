package com.space_m.machine.chem;

import java.util.List;
import java.util.Map;

import org.jetbrains.annotations.Nullable;

import com.hbm_m.blockentity.BaseMachineBlockEntity;
import com.hbm_m.interfaces.IUpgradeInfoProvider;
import com.hbm_m.inventory.UpgradeManager;
import com.hbm_m.item.industrial.ItemMachineUpgrade;
import com.hbm_m.item.industrial.ItemMachineUpgrade.UpgradeType;
import com.space_m.atmosphere.ChunkAtmosphereManager;
import com.space_m.celestial.trait.CBT_Atmosphere;
import com.space_m.machine.chem.recipe.VacuumCircuitRecipe;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

/**
 * 1:1 NTM Space {@code TileEntityMachineVacuumCircuit} ("Vacuum Solderer"): 8 Plaetze (0-1 Halbleiter, 2-3 Platinen,
 * 4 Ausgabe, 5 Batterie, 6-7 Aufwertungen). Arbeitet nur im Vakuum ({@code canOperate}: keine Atmosphaere oder Druck
 * &lt;= 0.001 atm). Mit Rezept: {@code processTime = d - d*rot/6 + d*blau/3}, {@code consumption = c + c*rot - c*blau/6},
 * Speicher {@code max(c*20, power)}; ohne Rezept Verbrauch 100, Speicher 2 000. Alle 20 Ticks drei Tau-Funken.
 *
 * <p>Originalfehler uebernommen: {@code upgradeManager.checkSlots(this, slots, 4, 4)} prueft den Ausgabeplatz 4 statt
 * der Aufwertungsplaetze 6-7 - eingesetzte Aufwertungen wirken daher (wie im Original) nicht.
 * Original-Klasse: TileEntityMachineVacuumCircuit
 */
public class TileEntityMachineVacuumCircuit extends BaseMachineBlockEntity implements IUpgradeInfoProvider {

    public static final int SLOTS = 8;

    public long consumption;
    public int progress;
    public int processTime = 1;

    /** Original {@code display} (Rezeptergebnis fuer den Renderer). */
    public ItemStack display = ItemStack.EMPTY;

    public boolean canOperate = true;

    public final UpgradeManager upgradeManager = new UpgradeManager();

    private static final Map<UpgradeType, Integer> VALID_UPGRADES = Map.of(UpgradeType.SPEED, 3, UpgradeType.POWER, 3);

    public TileEntityMachineVacuumCircuit(BlockPos pos, BlockState state) {
        super(ChemMachines.VACUUM_CIRCUIT_BE.get(), pos, state, SLOTS, 2_000, Long.MAX_VALUE);
    }

    /** Original {@code setInventorySlotContents}: Aufwertung einstecken (Platz 6-7) -> {@code hbm:item.upgradePlug}. */
    @Override
    protected com.hbm_m.platform.ModItemStackHandler createInventoryHandler(int size) {
        return new com.hbm_m.platform.ModItemStackHandler(size) {
            @Override
            protected void onContentsChanged(int slot) {
                setChanged();
                ItemStack stack = getStackInSlot(slot);
                if (level != null && !level.isClientSide && slot >= 6 && slot <= 7 && stack.getItem() instanceof ItemMachineUpgrade) {
                    level.playSound(null, worldPosition.getX() + 0.5, worldPosition.getY() + 0.5, worldPosition.getZ() + 0.5,
                            com.hbm_m.sound.HbmSoundsNT.get("hbm:item.upgradePlug"), SoundSource.BLOCKS, 1.0F, 1.0F);
                }
            }

            @Override
            public boolean isItemValid(int slot, ItemStack stack) {
                return isItemValidForSlot(slot, stack);
            }
        };
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.machineVacuumCircuit");
    }

    public static void tick(Level level, BlockPos pos, BlockState state, TileEntityMachineVacuumCircuit be) {
        if (level.isClientSide) return;

        CBT_Atmosphere atmosphere = ChunkAtmosphereManager.proxy.getAtmosphere(level, pos);
        be.canOperate = atmosphere == null || atmosphere.getPressure() <= 0.001;

        be.chargeFromBatterySlot(5);

        ItemStack[] in = new ItemStack[4];
        for (int i = 0; i < 4; i++) in[i] = be.inventory.getStackInSlot(i);
        VacuumCircuitRecipe recipe = VacuumCircuitRecipe.getRecipe(level, in);
        long intendedMaxPower;

        // 1:1 Original: checkSlots(this, slots, 4, 4)
        be.upgradeManager.checkSlots(be.inventory, 4, 4, VALID_UPGRADES);
        int redLevel = be.upgradeManager.getLevel(UpgradeType.SPEED);
        int blueLevel = be.upgradeManager.getLevel(UpgradeType.POWER);

        if (recipe != null) {
            be.processTime = recipe.duration - (recipe.duration * redLevel / 6) + (recipe.duration * blueLevel / 3);
            be.consumption = recipe.consumption + (recipe.consumption * redLevel) - (recipe.consumption * blueLevel / 6);
            intendedMaxPower = recipe.consumption * 20;

            if (be.canProcess(recipe)) {
                be.progress++;
                be.setEnergyStored(be.getEnergyStored() - be.consumption);

                if (be.progress >= be.processTime) {
                    be.progress = 0;
                    be.consumeItems(recipe);

                    ItemStack out = be.inventory.getStackInSlot(4);
                    if (out.isEmpty()) {
                        be.inventory.setStackInSlot(4, recipe.output.copy());
                    } else {
                        ItemStack grown = out.copy();
                        grown.grow(recipe.output.getCount());
                        be.inventory.setStackInSlot(4, grown);
                    }

                    be.setChanged();
                }

                if (level.getGameTime() % 20 == 0 && level instanceof ServerLevel serverLevel) {
                    Direction dir = ChemUtil.dir(state);
                    Direction rot = ChemUtil.rot(dir);
                    CompoundTag dPart = new CompoundTag();
                    dPart.putString("type", "tau");
                    dPart.putByte("count", (byte) 3);
                    com.hbm_m.particle.helper.IParticleCreator.sendPacket(serverLevel,
                            pos.getX() + 0.5 + dir.getStepX() * 0.625 + rot.getStepX() * 0.5, pos.getY() + 1.25,
                            pos.getZ() + 0.5 + dir.getStepZ() * 0.625 + rot.getStepZ() * 0.5, 25, dPart);
                }
            } else {
                be.progress = 0;
            }

        } else {
            be.progress = 0;
            be.consumption = 100;
            intendedMaxPower = 2000;
        }

        be.display = recipe != null ? recipe.output : ItemStack.EMPTY;
        be.setEnergyCapacity(Math.max(intendedMaxPower, be.energy));

        be.setChanged();
        be.sendUpdateToClient(); // networkPackNT(25)
    }

    public boolean canProcess(VacuumCircuitRecipe recipe) {
        if (!canOperate) return false;

        if (getEnergyStored() < consumption) return false;

        ItemStack out = inventory.getStackInSlot(4);
        if (!out.isEmpty()) {
            if (out.getItem() != recipe.output.getItem()) return false;
            if (out.getCount() + recipe.output.getCount() > out.getMaxStackSize()) return false;
        }

        return true;
    }

    /** Original {@code consumeItems}: je Zutat der erste passende Platz der Gruppe. */
    public void consumeItems(VacuumCircuitRecipe recipe) {
        for (VacuumCircuitRecipe.Stack aStack : recipe.wafer) {
            for (int i = 0; i < 2; i++) {
                ItemStack stack = inventory.getStackInSlot(i);
                if (aStack.matches(stack) && stack.getCount() >= aStack.count()) { decr(i, aStack.count()); break; }
            }
        }

        for (VacuumCircuitRecipe.Stack aStack : recipe.pcb) {
            for (int i = 2; i < 4; i++) {
                ItemStack stack = inventory.getStackInSlot(i);
                if (aStack.matches(stack) && stack.getCount() >= aStack.count()) { decr(i, aStack.count()); break; }
            }
        }
    }

    /** Original {@code decrStackSize(slot, n)}. */
    private void decr(int slot, int n) {
        ItemStack s = inventory.getStackInSlot(slot).copy();
        s.shrink(n);
        inventory.setStackInSlot(slot, s.isEmpty() ? ItemStack.EMPTY : s);
    }

    /** Original {@code isItemValidForSlot}: Gruppe passt, gleicher Gegenstand nicht in beiden Plaetzen der Gruppe. */
    @Override
    protected boolean isItemValidForSlot(int slot, ItemStack stack) {
        if (slot < 2) {
            for (int i = 0; i < 2; i++) if (i != slot && ItemStack.isSameItem(inventory.getStackInSlot(i), stack)) return false;
            return VacuumCircuitRecipe.isWafer(level, stack);
        } else if (slot < 4) {
            for (int i = 2; i < 4; i++) if (i != slot && ItemStack.isSameItem(inventory.getStackInSlot(i), stack)) return false;
            return VacuumCircuitRecipe.isPcb(level, stack);
        }
        // Original: false fuer 4-7 (GUI-Plaetze nehmen trotzdem auf: Batterie, Aufwertung)
        if (slot == 5) return isEnergyProviderItem(stack);
        if (slot == 6 || slot == 7) return stack.getItem() instanceof ItemMachineUpgrade;
        return false;
    }

    /** Original {@code getPower()}: auf 0..maxPower begrenzt. */
    public long getPower() {
        return Math.max(Math.min(getEnergyStored(), getMaxEnergyStored()), 0);
    }

    // ── NBT ──────────────────────────────────────────────────────────────────────────────────

    @Override
    protected void writeNbtData(CompoundTag nbt, @Nullable net.minecraft.core.HolderLookup.Provider registries) {
        super.writeNbtData(nbt, registries);
        nbt.putLong("maxPower", getMaxEnergyStored());
        nbt.putInt("progress", progress);
        nbt.putInt("processTime", processTime);
        nbt.putLong("consumption", consumption);
        nbt.putBoolean("canOperate", canOperate);
        if (!display.isEmpty()) nbt.put("display", com.hbm_m.platform.PlatformHooks.saveItemStack(display, new CompoundTag(), registries));
    }

    @Override
    protected void readNbtData(CompoundTag nbt, @Nullable net.minecraft.core.HolderLookup.Provider registries) {
        super.readNbtData(nbt, registries);
        if (nbt.contains("maxPower")) setEnergyCapacity(Math.max(nbt.getLong("maxPower"), 1));
        progress = nbt.getInt("progress");
        processTime = nbt.getInt("processTime");
        consumption = nbt.getLong("consumption");
        canOperate = !nbt.contains("canOperate") || nbt.getBoolean("canOperate");
        display = nbt.contains("display") ? com.hbm_m.platform.PlatformHooks.itemStackOf(nbt.getCompound("display"), registries) : ItemStack.EMPTY;
    }

    /** Original {@code getRenderBoundingBox}: x-1..x+2, y..y+3, z-1..z+2. */
    //? if forge {
    @Override
    //?}
    public AABB getRenderBoundingBox() {
        return new AABB(worldPosition.getX() - 1, worldPosition.getY(), worldPosition.getZ() - 1,
                worldPosition.getX() + 2, worldPosition.getY() + 3, worldPosition.getZ() + 2);
    }

    // ── Aufwertungen (IUpgradeInfoProvider) ──────────────────────────────────────────────────

    @Override
    public Map<UpgradeType, Integer> getValidUpgrades() {
        return VALID_UPGRADES;
    }

    @Override
    public boolean canProvideInfo(UpgradeType type, int level, boolean extendedInfo) {
        return type == UpgradeType.SPEED || type == UpgradeType.POWER;
    }

    @Override
    public void provideInfo(UpgradeType type, int level, List<Component> info, boolean extendedInfo) {
        info.add(IUpgradeInfoProvider.getStandardLabel(ChemMachines.MACHINE_VACUUM_CIRCUIT.get()));
        if (type == UpgradeType.SPEED) {
            info.add(Component.translatable(KEY_DELAY, "-" + (level * 100 / 6) + "%").withStyle(ChatFormatting.GREEN));
            info.add(Component.translatable(KEY_CONSUMPTION, "+" + (level * 100) + "%").withStyle(ChatFormatting.RED));
        }
        if (type == UpgradeType.POWER) {
            info.add(Component.translatable(KEY_CONSUMPTION, "-" + (level * 100 / 6) + "%").withStyle(ChatFormatting.GREEN));
            info.add(Component.translatable(KEY_DELAY, "+" + (level * 100 / 3) + "%").withStyle(ChatFormatting.RED));
        }
    }

    // ── GUI ──────────────────────────────────────────────────────────────────────────────────

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inv, Player player) {
        return new ContainerVacuumCircuit(id, inv, this);
    }

    @Override
    public Component getDisplayName() {
        return getDefaultName();
    }
}
