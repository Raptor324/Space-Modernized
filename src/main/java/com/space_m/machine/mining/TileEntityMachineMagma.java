package com.space_m.machine.mining;

import java.util.ArrayList;
import java.util.List;

import org.jetbrains.annotations.Nullable;

import com.hbm_m.api.fluids.IFluidStandardTransceiverMK2;
import com.hbm_m.blockentity.BaseMachineBlockEntity;
import com.hbm_m.inventory.fluid.tank.FluidTank;
import com.hbm_m.inventory.material.MaterialShapes;
import com.hbm_m.inventory.material.Mats;
import com.hbm_m.inventory.material.Mats.MaterialStack;
import com.hbm_m.inventory.material.NTMMaterial;
import com.hbm_m.util.CrucibleUtil;
import com.space_m.celestial.CelestialBody;
import com.space_m.machine.chem.ChemUtil;
import com.space_m.material.SpaceMats;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

/**
 * 1:1 NTM Space {@code TileEntityMachineMagma}: bohrt auf Moho in die Lava (3x3 Lavabloecke 4 unter dem Kern) und sammelt
 * alle 10 Ticks 1 Barren Schlacke ({@code MAT_SLAG}) + 4 Quanten reiches Magma ({@code MAT_RICH_MAGMA}), hoechstens 16
 * Bloecke Schmelze; Verbrauch 10 000 HE/Tick, Speicher 1 000 000 HE. Die Schmelze fliesst wie bei einem Tiegel
 * ({@code CrucibleUtil.pourFullStack}, 1 Barren, Reichweite 6) aus dem Auslauf {@code dir*3.875} vor dem Kern, mit
 * {@code foundry}-Partikel. Client: Drehzahl des Bohrkopfs (+0,15 bis 15 / -0,3) und Lavaspiegel (gegen 0,9 bzw. 0).
 * Keine Tanks ({@code tanks = new FluidTank[0]}, Original-TODO "Kuehlfluide").
 * Original-Klasse: TileEntityMachineMagma
 */
public class TileEntityMachineMagma extends BaseMachineBlockEntity implements IFluidStandardTransceiverMK2 {

    public boolean operating;

    public long consumption = 10_000;

    // TODO: probably to handle cooling fluids, remove me if we don't do that
    public FluidTank[] tanks;

    public static final int maxLiquid = MaterialShapes.BLOCK.q(16);
    public List<MaterialStack> liquids = new ArrayList<>();

    public float drillSpeed;
    public float drillRotation;
    public float prevDrillRotation;

    public float lavaHeight;
    public float prevLavaHeight;

    public boolean validPosition = true;

    protected MaterialStack[] defaultOutputs = new MaterialStack[] {
            new MaterialStack(Mats.MAT_SLAG, MaterialShapes.INGOT.q(1)),
            new MaterialStack(SpaceMats.MAT_RICH_MAGMA, MaterialShapes.QUANTUM.q(4)),
    };

    public TileEntityMachineMagma(BlockPos pos, BlockState state) {
        super(MiningMachines.MAGMA_BE.get(), pos, state, 0, 1_000_000L, 1_000_000L);
        tanks = new FluidTank[0];
    }

    public static void tick(Level level, BlockPos pos, BlockState state, TileEntityMachineMagma be) {
        if (!level.isClientSide) {
            be.serverTick((ServerLevel) level);
        } else {
            be.clientTick(level);
        }
    }

    private void serverTick(ServerLevel level) {
        // Strom: Original trySubscribe an getConPos() -> Kabel docken an den Anschlusszellen an (HBM-Netz)

        // baby drill, baby
        operating = canOperate();
        if (operating) {
            setEnergyStored(getEnergyStored() - consumption);

            int timeBetweenOutputs = 10;

            if (level.getGameTime() % timeBetweenOutputs == 0) {
                for (MaterialStack mat : getOutputs()) {
                    int totalLiquid = 0;
                    for (MaterialStack m : liquids) totalLiquid += m.amount;

                    int toAdd = mat.amount;

                    if (totalLiquid + toAdd <= maxLiquid) {
                        addToStack(mat);
                    } else {
                        break;
                    }
                }
            }
        }

        // pour me a drink, barkeep
        if (!liquids.isEmpty()) {
            Direction dir = ChemUtil.dir(getBlockState());
            BlockPos p = worldPosition;

            double[] impact = new double[3];
            MaterialStack didPour = CrucibleUtil.pourFullStack(level, p.getX() + 0.5D + dir.getStepX() * 3.875D, p.getY() + 1.25D,
                    p.getZ() + 0.5D + dir.getStepZ() * 3.875D, 6, true, liquids, MaterialShapes.INGOT.q(1), impact);

            if (didPour != null) {
                CompoundTag data = new CompoundTag();
                data.putString("type", "foundry");
                data.putInt("color", didPour.material.moltenColor);
                data.putByte("dir", (byte) dir.get3DDataValue());
                data.putFloat("off", 0.625F);
                data.putFloat("base", 0.625F);
                data.putFloat("len", Math.max(1F, p.getY() + 1 - (float) (Math.ceil(impact[1]) - 0.875)));
                com.hbm_m.particle.helper.IParticleCreator.sendPacket(level, p.getX() + 0.5D + dir.getStepX() * 3.875D, p.getY() + 1,
                        p.getZ() + 0.5D + dir.getStepZ() * 3.875D, 50, data);
            }
        }

        liquids.removeIf(o -> o.amount <= 0);

        setChanged();
        sendUpdateToClient(); // networkPackNT(250)
    }

    private void clientTick(Level level) {
        prevLavaHeight = lavaHeight;
        prevDrillRotation = drillRotation;

        if (operating) {
            drillSpeed += 0.15F;
            if (drillSpeed > 15F) drillSpeed = 15F;

            lavaHeight += (level.random.nextFloat() - 0.5) * 0.01;
            lavaHeight = (float) Mth.lerp(0.02D, lavaHeight, 0.9D);
        } else {
            drillSpeed -= 0.3F;
            if (drillSpeed < 0F) drillSpeed = 0F;

            lavaHeight = (float) Mth.lerp(0.02D, lavaHeight, 0D);
        }

        drillRotation += drillSpeed;

        if (drillRotation > 360F) {
            drillRotation -= 360F;
            prevDrillRotation -= 360F;
        }
    }

    /** Original {@code getConPos()}: sechs Stellen hinter den Anschlusszellen ({@code -dir*4}, y-1/y-2, {@code +-rot}). */
    public ChemUtil.DirPos[] getConPos() {
        Direction dir = ChemUtil.dir(getBlockState());
        return new ChemUtil.DirPos[] {
                ChemUtil.at(worldPosition, dir, -4, 0, -1, dir),
                ChemUtil.at(worldPosition, dir, -4, 0, -2, dir),
                ChemUtil.at(worldPosition, dir, -4, 1, -1, dir),
                ChemUtil.at(worldPosition, dir, -4, 1, -2, dir),
                ChemUtil.at(worldPosition, dir, -4, -1, -1, dir),
                ChemUtil.at(worldPosition, dir, -4, -1, -2, dir),
        };
    }

    /** Original {@code canOperate}: nur auf Moho, 3x3 Lava unter dem Bohrer, genug Strom. */
    boolean canOperate() {
        // Currently only functions on Moho, so the simplest solution is acceptable
        CelestialBody body = CelestialBody.getBody(level);
        if (!"moho".equals(body.name)) return false;

        validPosition = isValidPosition();
        if (!validPosition) return false;

        if (getEnergyStored() < consumption) return false;

        return true;
    }

    /** Original {@code isValidPosition}: {@code Blocks.lava} auf allen 3x3 Feldern 4 unter dem Kern. */
    boolean isValidPosition() {
        for (int x = -1; x <= 1; x++) {
            for (int z = -1; z <= 1; z++) {
                if (!level.getBlockState(worldPosition.offset(x, -4, z)).is(Blocks.LAVA)) return false;
            }
        }

        return true;
    }

    // Returns materials produced at this location, varied by perlin noise
    private MaterialStack[] getOutputs() {
        return defaultOutputs;
    }

    private void addToStack(MaterialStack matStack) {
        for (MaterialStack mat : liquids) {
            if (mat.material == matStack.material) {
                mat.amount += matStack.amount;
                return;
            }
        }

        liquids.add(matStack.copy());
    }

    // ── NBT (Original writeToNBT "power"/"t<i>"/"liquids"; Sync zusaetzlich operating/validPosition) ──

    @Override
    protected void writeNbtData(CompoundTag nbt, @Nullable net.minecraft.core.HolderLookup.Provider registries) {
        super.writeNbtData(nbt, registries);

        nbt.putLong("power", getEnergyStored());

        for (int i = 0; i < tanks.length; i++) tanks[i].writeToNBT(nbt, "t" + i);

        int[] liquidData = new int[liquids.size() * 2];
        for (int i = 0; i < liquids.size(); i++) { MaterialStack sta = liquids.get(i); liquidData[i * 2] = sta.material.id; liquidData[i * 2 + 1] = sta.amount; }
        nbt.putIntArray("liquids", liquidData);

        // Original serialize (networkPackNT)
        nbt.putBoolean("operating", operating);
        nbt.putBoolean("validPosition", validPosition);
    }

    @Override
    protected void readNbtData(CompoundTag nbt, @Nullable net.minecraft.core.HolderLookup.Provider registries) {
        super.readNbtData(nbt, registries);

        if (nbt.contains("power")) setEnergyStored(nbt.getLong("power"));

        for (int i = 0; i < tanks.length; i++) tanks[i].readFromNBT(nbt, "t" + i);

        liquids.clear(); // Original deserialize: liquids.clear()
        int[] liquidData = nbt.getIntArray("liquids");
        for (int i = 0; i < liquidData.length / 2; i++) {
            NTMMaterial mat = Mats.matById.get(liquidData[i * 2]);
            if (mat == null) continue;
            liquids.add(new MaterialStack(mat, liquidData[i * 2 + 1]));
        }

        if (level != null && level.isClientSide) {
            operating = nbt.getBoolean("operating");
            validPosition = nbt.getBoolean("validPosition");
        }
    }

    // ── Strom / Fluide ───────────────────────────────────────────────────────────────────────

    @Override
    public FluidTank[] getAllTanks() {
        return tanks;
    }

    @Override
    public FluidTank[] getSendingTanks() {
        return tanks;
    }

    @Override
    public FluidTank[] getReceivingTanks() {
        return tanks;
    }

    @Override
    public boolean isLoaded() {
        return level != null && !isRemoved() && level.isLoaded(worldPosition);
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.machineMagma");
    }

    @Override
    public Component getDisplayName() {
        return getDefaultName();
    }

    @Override
    protected boolean isItemValidForSlot(int slot, ItemStack stack) {
        return false;
    }

    /** Kein GUI im Original. */
    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
        return null;
    }

    /** Original {@code getRenderBoundingBox}: x-4..x+5, y-3..y+3, z-4..z+5. */
    //? if forge {
    @Override
    //?}
    public AABB getRenderBoundingBox() {
        return new AABB(worldPosition.getX() - 4, worldPosition.getY() - 3, worldPosition.getZ() - 4,
                worldPosition.getX() + 5, worldPosition.getY() + 3, worldPosition.getZ() + 5);
    }
}
