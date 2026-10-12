package com.space_m.machine.chem;

import java.util.List;

import org.jetbrains.annotations.Nullable;

import com.hbm_m.api.fluids.IFluidStandardTransceiverMK2;
import com.hbm_m.blockentity.BaseMachineBlockEntity;
import com.hbm_m.inventory.fluid.tank.FluidTank;
import com.space_m.atmosphere.ChunkAtmosphereHandler;
import com.space_m.item.SpaceCrops;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.BonemealableBlock;
import net.minecraft.world.level.block.BushBlock;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.StemBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.phys.AABB;

/**
 * 1:1 NTM Space {@code TileEntityHydroponic} ("Industrial Greenhouse"): Tanks CO2 16 000 mB (rein), O2 16 000 mB (raus),
 * 2 000 HE (Grundverbrauch 25 HE/Tick), Duenger bis 200 (Knochenmehl 1, {@code powder_fertilizer} 9). Ab 200 HE Licht an
 * (Lampenzellen {@code lit}). Je Pflanzfeld (y+1, rot -1..1) mit Strom >= 200 und CO2 >= 100 mB: Saat (Feldfrucht, ein
 * Block hoch, keine Ranke, {@code canHydro}) einpflanzen, 1/120 Zufallstick (~ x10 Wachstum), mit Duenger 1/60
 * Knochenmehl-Wirkung, je Wachstumsstufe {@code CROP_GROWTH_CONVERSION} (15) mB CO2 -> O2 (R2-A), reife Pflanzen ernten
 * (Plaetze 3-5). Ohne Strom/CO2 wird das Wachstum angehalten (Stufe zurueckgesetzt).
 * Plaetze: 0 Saat, 1 Duenger, 2 Batterie, 3-5 Ernte.
 * Original-Klasse: TileEntityHydroponic
 */
public class TileEntityHydroponic extends BaseMachineBlockEntity implements IFluidStandardTransceiverMK2 {

    public static final long maxPower = 2_000;
    public static final int maxFertilizer = 200;
    public static final int SLOTS = 6;

    public FluidTank[] tanks;
    public int fertilizer;

    private boolean lightsOn = false;
    private final int[] prevMeta = new int[3];

    public TileEntityHydroponic(BlockPos pos, BlockState state) {
        super(ChemMachines.HYDROBAY_BE.get(), pos, state, SLOTS, maxPower, maxPower);
        tanks = new FluidTank[2];
        tanks[0] = new FluidTank(ChemUtil.fluid("CARBONDIOXIDE"), 16_000);
        tanks[1] = new FluidTank(ChemUtil.fluid("OXYGEN"), 16_000);
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.hydrobay");
    }

    public static void tick(Level level, BlockPos pos, BlockState state, TileEntityHydroponic be) {
        if (level.isClientSide || !(level instanceof ServerLevel world)) return;
        be.update(world, state);
    }

    void update(ServerLevel world, BlockState state) {
        Direction dir = ChemUtil.dir(state);
        Direction rot = ChemUtil.rot(dir);

        for (ChemUtil.DirPos pos : getFluidPos()) {
            trySubscribe(tanks[0].getTankType(), world, pos.pos(), pos.dir());
            tryProvide(tanks[1], world, pos.pos(), pos.dir());
        }

        if (getEnergyStored() > 0) {
            setEnergyStored(Math.max(getEnergyStored() - 25, 0));
        }

        ItemStack fert = inventory.getStackInSlot(1);
        if (!fert.isEmpty()) {
            int strength = getFertilizerStrength(fert);
            if (strength > 0 && fertilizer <= maxFertilizer - strength) {
                ItemStack rest = fert.copy();
                rest.shrink(1);
                inventory.setStackInSlot(1, rest.isEmpty() ? ItemStack.EMPTY : rest);
                fertilizer += strength;
                setChanged();
            }
        }

        chargeFromBatterySlot(2);

        // Lampen (Original: Blockwechsel hydrobay <-> dummy_beam)
        boolean lit = getEnergyStored() >= 200;
        if (lit != lightsOn) {
            for (BlockPos lamp : MachineHydroponic.lampCells(worldPosition, dir)) {
                BlockState ls = world.getBlockState(lamp);
                if (ls.getBlock() instanceof BlockDummyableBeam) world.setBlock(lamp, ls.setValue(BlockDummyableBeam.LIT, lit), 3);
            }
            lightsOn = lit;
        }

        for (int i = 0; i < 3; i++) {
            BlockPos p = worldPosition.offset(rot.getStepX() * (i - 1), 1, rot.getStepZ() * (i - 1));

            BlockState plantState = world.getBlockState(p);
            Block currentPlant = plantState.getBlock();

            // Minimum CO2 pressure required to start growing
            if (getEnergyStored() >= 200 && tanks[0].getFill() >= 100) {

                // Attempt planting a new plant
                // Only allows single block crops
                if (!(currentPlant instanceof BonemealableBlock)) {
                    ItemStack seed = inventory.getStackInSlot(0);
                    Block plant = plantFor(seed);
                    if (plant == null) continue;

                    plantState = plant.defaultBlockState();
                    currentPlant = plant;
                    world.setBlock(p, plantState, 3);
                    prevMeta[i] = 0;

                    ItemStack rest = seed.copy();
                    rest.shrink(1);
                    inventory.setStackInSlot(0, rest.isEmpty() ? ItemStack.EMPTY : rest);

                    setChanged();
                }

                BonemealableBlock currentGrowable = (BonemealableBlock) currentPlant;

                // Increase growth speed by about x10
                if (world.random.nextInt(120) == 0) {
                    plantState = world.getBlockState(p);
                    if (plantState.isRandomlyTicking()) plantState.randomTick(world, p, world.random);
                }

                plantState = world.getBlockState(p);
                if (plantState.getBlock() != currentPlant) continue; // Pflanze ist weg (Atmosphaere o.ae.)

                boolean fullyGrown = false;
                if (isValidBonemealTarget(currentGrowable, world, p, plantState)) { // should consume bonemeal, if not, assume fully grown
                    if (fertilizer > 0 && world.random.nextInt(60) == 0) {
                        if (currentGrowable.isBonemealSuccess(world, world.random, p, plantState)) { // does bonemeal apply
                            currentGrowable.performBonemeal(world, world.random, p, plantState); // apply bonemeal
                            world.levelEvent(1505, p, 0);
                        }

                        // now consume the bonemeal
                        fertilizer--;
                    }
                } else {
                    fullyGrown = true;
                }

                plantState = world.getBlockState(p);
                int newMeta = age(plantState);

                if (newMeta != prevMeta[i]) {
                    // each growth stage sequesters 5mb of carbon
                    int toProduce = Math.max(newMeta - prevMeta[i], 0) * ChunkAtmosphereHandler.CROP_GROWTH_CONVERSION;
                    tanks[0].setFill(Math.max(tanks[0].getFill() - toProduce, 0));
                    tanks[1].setFill(Math.min(tanks[1].getFill() + toProduce, tanks[1].getMaxFill()));

                    prevMeta[i] = newMeta;
                }

                // after collecting produced O2, break any fully grown plants
                // unless there is no space to collect the drops
                if (fullyGrown && attemptHarvest(Block.getDrops(plantState, world, p, null))) {
                    world.setBlock(p, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), 3);
                    setChanged();
                }
            } else if (currentPlant instanceof BonemealableBlock) {
                // pause growth until sufficient CO2 added
                BlockState paused = withAge(plantState, prevMeta[i]);
                if (paused != plantState) world.setBlock(p, paused, 2);
            }
        }

        setChanged();
        sendUpdateToClient(); // networkPackNT(15)
    }

    /**
     * Original: Saat ist {@code IPlantable} vom Typ {@code Crop}, Pflanze {@code IGrowable}, keine {@code BlockStem},
     * {@code BlockCrop.canHydro} (nur {@code crop_paraffin} = false). Port: {@code CropBlock} (Forge-Pflanzentyp CROP).
     */
    @Nullable
    public static Block plantFor(ItemStack seed) {
        if (seed.isEmpty() || !(seed.getItem() instanceof BlockItem bi)) return null;
        Block plant = bi.getBlock();
        if (!(plant instanceof CropBlock) || !(plant instanceof BonemealableBlock)) return null;
        if (plant instanceof StemBlock) return null;
        if (plant == SpaceCrops.CROP_PARAFFIN.get()) return null;
        return plant;
    }

    private static boolean isValidBonemealTarget(BonemealableBlock block, Level level, BlockPos pos, BlockState state) {
        //? if < 1.21.1 {
        return block.isValidBonemealTarget(level, pos, state, level.isClientSide);
        //?} else {
        /*return block.isValidBonemealTarget(level, pos, state);
        *///?}
    }

    /** Original {@code getBlockMetadata} einer Feldfrucht = Alter. */
    @Nullable
    private static IntegerProperty ageProperty(BlockState state) {
        for (Property<?> p : state.getProperties()) {
            if (p instanceof IntegerProperty ip && "age".equals(p.getName())) return ip;
        }
        return null;
    }

    static int age(BlockState state) {
        IntegerProperty p = ageProperty(state);
        return p == null ? 0 : state.getValue(p);
    }

    static BlockState withAge(BlockState state, int age) {
        IntegerProperty p = ageProperty(state);
        if (p == null || !p.getPossibleValues().contains(age)) return state;
        return state.setValue(p, age);
    }

    public ItemStack[] getValidFertilizers() {
        return new ItemStack[] {
                new ItemStack(Items.BONE_MEAL),
                new ItemStack(com.hbm_m.item.ModItems.POWDER_FERTILIZER.get()),
        };
    }

    public static int getFertilizerStrength(ItemStack stack) {
        if (stack.isEmpty()) return 0;
        if (stack.getItem() == Items.BONE_MEAL) return 1;
        if (stack.getItem() == com.hbm_m.item.ModItems.POWDER_FERTILIZER.get()) return 9;
        return 0;
    }

    /** Original {@code attemptHarvest}: alle Drops passen in 3-5, sonst nichts veraendern. */
    private boolean attemptHarvest(List<ItemStack> drops) {
        ItemStack[] originals = new ItemStack[3];
        for (int i = 0; i < 3; i++) originals[i] = inventory.getStackInSlot(3 + i).copy();

        for (ItemStack drop : drops) {
            if (!tryAddItemToInventory(3, 5, drop.copy())) {
                for (int i = 0; i < 3; i++) inventory.setStackInSlot(3 + i, originals[i]);
                return false;
            }
        }

        return true;
    }

    /** Original {@code InventoryUtil.tryAddItemToInventory}: erst stapeln, dann leere Plaetze; true = alles untergebracht. */
    private boolean tryAddItemToInventory(int start, int end, ItemStack stack) {
        for (int i = start; i <= end && !stack.isEmpty(); i++) {
            ItemStack s = inventory.getStackInSlot(i);
            if (!s.isEmpty() && ChemUtil.sameItemSameData(s, stack) && s.getCount() < s.getMaxStackSize()) {
                int move = Math.min(stack.getCount(), s.getMaxStackSize() - s.getCount());
                ItemStack grown = s.copy();
                grown.grow(move);
                inventory.setStackInSlot(i, grown);
                stack.shrink(move);
            }
        }
        for (int i = start; i <= end && !stack.isEmpty(); i++) {
            if (inventory.getStackInSlot(i).isEmpty()) {
                inventory.setStackInSlot(i, stack.copy());
                stack.setCount(0);
            }
        }
        return stack.isEmpty();
    }

    private ChemUtil.DirPos[] getFluidPos() {
        Direction dir = ChemUtil.dir(getBlockState());
        BlockPos p = worldPosition;
        return new ChemUtil.DirPos[] {
                ChemUtil.at(p, dir, 1, 2, 0, dir),
                ChemUtil.at(p, dir, 1, -2, 0, dir),
                ChemUtil.at(p, dir, -1, 2, 0, dir.getOpposite()),
                ChemUtil.at(p, dir, -1, -2, 0, dir.getOpposite()),
        };
    }

    // ── Fluide ───────────────────────────────────────────────────────────────────────────────

    @Override public FluidTank[] getReceivingTanks() { return new FluidTank[] { tanks[0] }; }
    @Override public FluidTank[] getSendingTanks() { return new FluidTank[] { tanks[1] }; }
    @Override public FluidTank[] getAllTanks() { return tanks; }

    @Override
    public boolean canConnect(Fluid type, Direction dir) {
        return dir != null;
    }

    @Override
    public boolean isLoaded() {
        return level != null && !isRemoved() && level.isLoaded(worldPosition);
    }

    // ── NBT ──────────────────────────────────────────────────────────────────────────────────

    @Override
    protected void writeNbtData(CompoundTag nbt, @Nullable net.minecraft.core.HolderLookup.Provider registries) {
        super.writeNbtData(nbt, registries);
        for (int i = 0; i < tanks.length; i++) tanks[i].writeToNBT(nbt, "t" + i);
        for (int i = 0; i < 3; i++) nbt.putInt("p" + i, prevMeta[i]);
        nbt.putBoolean("lights", lightsOn);
        nbt.putInt("fertilizer", fertilizer);
    }

    @Override
    protected void readNbtData(CompoundTag nbt, @Nullable net.minecraft.core.HolderLookup.Provider registries) {
        super.readNbtData(nbt, registries);
        for (int i = 0; i < tanks.length; i++) tanks[i].readFromNBT(nbt, "t" + i);
        for (int i = 0; i < 3; i++) prevMeta[i] = nbt.getInt("p" + i);
        lightsOn = nbt.getBoolean("lights");
        fertilizer = nbt.getInt("fertilizer");
    }

    /** Original {@code getRenderBoundingBox}: x-2..x+3, y..y+2, z-2..z+3. */
    //? if forge {
    @Override
    //?}
    public AABB getRenderBoundingBox() {
        return new AABB(worldPosition.getX() - 2, worldPosition.getY(), worldPosition.getZ() - 2,
                worldPosition.getX() + 3, worldPosition.getY() + 2, worldPosition.getZ() + 3);
    }

    // ── Inventar / GUI ───────────────────────────────────────────────────────────────────────

    /** Original: 0 nur {@code IPlantable}, 1 nur Duenger, sonst nichts (Batterie nur ueber die GUI). */
    @Override
    protected boolean isItemValidForSlot(int slot, ItemStack stack) {
        if (slot == 0) return stack.getItem() instanceof BlockItem bi && bi.getBlock() instanceof BushBlock;
        if (slot == 1) return getFertilizerStrength(stack) > 0;
        if (slot == 2) return isEnergyProviderItem(stack);
        return false;
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inv, Player player) {
        return new ContainerHydroponic(id, inv, this);
    }

    @Override
    public Component getDisplayName() {
        return getDefaultName();
    }
}
