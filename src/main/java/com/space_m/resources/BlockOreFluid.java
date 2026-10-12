package com.space_m.resources;

import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

import org.jetbrains.annotations.Nullable;

import com.hbm_m.api.space.SpaceResources.ReserveType;
import com.space_m.SpaceMod;
import com.space_m.celestial.SolarSystem;
import com.space_m.config.SpaceConfig;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 1:1 NTM Space {@code com.hbm.blocks.generic.BlockOreFluid} fuer die Koerper-Varianten der Lagerstaetten
 * ({@code ore_oil_duna/_laythe}, {@code ore_gas_eve}, {@code ore_brine_mun/_minmus/_ike}, {@code ore_tekto_tekto},
 * {@code ore_bedrock_oil_tekto}). Das Original-Metadatum (= Koerper) steckt hier im Block selbst ({@link #body}).
 *
 * <ul>
 *   <li>Nachbaraenderung: liegt der leere Block darunter, tauschen beide (die Blase "steigt").</li>
 *   <li>Foerderfluide/-mengen/Erschoepfung je Koerper ({@link #getPrimaryFluid} ... {@link #drain}) mit den
 *       {@code SpaceConfig}-Werten (Original {@code WorldConfig}); ohne leeren Block = nur Fracking
 *       ({@link #requiresFracking}).</li>
 *   <li>Abbau liefert den leeren Block (Loot-Tabelle, Original {@code getItemDropped}; Behutsamkeit gilt nicht).</li>
 * </ul>
 * Pumpjack/Oelbohrturm/Fracking-Turm/Oeldetektor sind HBM-Maschinen; sie fragen diesen Block ueber
 * {@code SpaceResources.FluidDepositBlock} ab (W-R8B-2, Original {@code instanceof BlockOreFluid}).
 */
public class BlockOreFluid extends Block implements com.hbm_m.api.space.SpaceResources.FluidDepositBlock {

    @Nullable private final Supplier<Block> empty;
    private final ReserveType type;
    public final SolarSystem.Body body;

    private final Random rand = new Random();

    private static final Map<Block, Block> emptyToFull = new ConcurrentHashMap<>();

    public BlockOreFluid(Properties properties, @Nullable Supplier<Block> empty, ReserveType type, SolarSystem.Body body) {
        super(properties);
        this.empty = empty;
        this.type = type;
        this.body = body;
    }

    @Override
    public ReserveType getReserveType() {
        return type;
    }

    @Nullable
    public Block getEmpty() {
        return empty != null ? empty.get() : null;
    }

    @Override
    public String getUnlocalizedReserveType() {
        switch (type) {
            case GAS: return "_gas";
            case BRINE: return "_brine";
            default: return "";
        }
    }

    /** Original {@code getPrimaryFluid(meta)} als Fluid-Registry-ID ({@code null} = Original {@code Fluids.NONE}). */
    @Nullable
    @Override
    public ResourceLocation getPrimaryFluid() {
        switch (type) {
            case OIL:
                if (body == SolarSystem.Body.LAYTHE) return hbm("oil_ds");
                if (body == SolarSystem.Body.TEKTO) return SpaceMod.id("tcrude");
                return hbm("crude_oil");
            case GAS: return hbm("gas");
            case BRINE: return SpaceMod.id("brine");
            default: return null;
        }
    }

    /** Original {@code getSecondaryFluid(meta)}. */
    @Nullable
    @Override
    public ResourceLocation getSecondaryFluid() {
        switch (type) {
            case OIL:
                if (body == SolarSystem.Body.TEKTO) return SpaceMod.id("hgas");
                return hbm("gas");
            case GAS: return hbm("petroleum");
            default: return null;
        }
    }

    /** how much you can get by putting the block straight into a barrel like a container */
    public int getBlockFluidAmount() {
        switch (type) {
            case OIL: return 250;
            case GAS: return 100;
            default: return 0;
        }
    }

    @Override
    public int getPrimaryFluidAmount() {
        SpaceConfig c = SpaceConfig.get();
        if (empty == null) {
            if (body == SolarSystem.Body.TEKTO) return c.tektoBedrockOilPerDeposit;
            return c.bedrockOilPerDeposit;
        }

        if (body == SolarSystem.Body.DUNA) return c.dunaOilPerDeposit;
        if (body == SolarSystem.Body.LAYTHE) return c.laytheOilPerDeposit;
        if (body == SolarSystem.Body.EVE) return c.eveGasPerDeposit;
        if (body == SolarSystem.Body.MUN) return c.munBrinePerDeposit;
        if (body == SolarSystem.Body.MINMUS) return c.minmusBrinePerDeposit;
        if (body == SolarSystem.Body.IKE) return c.ikeBrinePerDeposit;
        if (body == SolarSystem.Body.TEKTO) return c.tektoOilPerDeposit;
        return c.earthOilPerDeposit;
    }

    @Override
    public int getSecondaryFluidAmount() {
        SpaceConfig c = SpaceConfig.get();
        if (empty == null) {
            if (body == SolarSystem.Body.TEKTO) return randIntBetween(c.tektoBedrockGasPerDepositMin, c.tektoBedrockGasPerDepositMax);
            return randIntBetween(c.bedrockGasPerDepositMin, c.bedrockGasPerDepositMax);
        }

        if (body == SolarSystem.Body.DUNA) return randIntBetween(c.dunaGasPerDepositMin, c.dunaGasPerDepositMax);
        if (body == SolarSystem.Body.LAYTHE) return randIntBetween(c.laytheGasPerDepositMin, c.laytheGasPerDepositMax);
        if (body == SolarSystem.Body.EVE) return randIntBetween(c.evePetPerDepositMin, c.evePetPerDepositMax);
        if (body == SolarSystem.Body.TEKTO) return randIntBetween(c.tektoGasPerDepositMin, c.tektoGasPerDepositMax);
        return randIntBetween(c.earthGasPerDepositMin, c.earthGasPerDepositMax);
    }

    public double getDrainChance() {
        SpaceConfig c = SpaceConfig.get();
        if (empty == null) return 0;
        if (body == SolarSystem.Body.DUNA) return c.dunaOilDrainChance;
        if (body == SolarSystem.Body.LAYTHE) return c.laytheOilDrainChance;
        if (body == SolarSystem.Body.EVE) return c.eveGasDrainChance;
        if (body == SolarSystem.Body.MUN) return c.munBrineDrainChance;
        if (body == SolarSystem.Body.MINMUS) return c.minmusBrineDrainChance;
        if (body == SolarSystem.Body.IKE) return c.ikeBrineDrainChance;
        if (body == SolarSystem.Body.TEKTO) return c.tektoOilDrainChance;
        return c.earthOilDrainChance;
    }

    /** Original {@code BobMathUtil.randIntBetween(rand, min, max)}. */
    private int randIntBetween(int min, int max) {
        return rand.nextInt(max - min + 1) + min;
    }

    @Override
    public void drain(Level world, BlockPos pos, double chanceMultiplier) {
        if (empty == null) return;

        if (world.random.nextDouble() < getDrainChance() * chanceMultiplier) {
            world.setBlock(pos, empty.get().defaultBlockState(), 3);
        }
    }

    @Override
    public boolean requiresFracking() {
        return empty == null;
    }

    @Override
    //? if < 1.21.1 {
    public void neighborChanged(BlockState state, Level world, BlockPos pos, Block block, BlockPos fromPos, boolean moving) {
        super.neighborChanged(state, world, pos, block, fromPos, moving);
    //?} else {
    /*protected void neighborChanged(BlockState state, Level world, BlockPos pos, Block block, BlockPos fromPos, boolean moving) {
        super.neighborChanged(state, world, pos, block, fromPos, moving);
    *///?}
        if (empty == null || world.isClientSide) return;

        Block emptyBlock = empty.get();
        if (world.getBlockState(pos.below()).is(emptyBlock)) {
            world.setBlock(pos, emptyBlock.defaultBlockState(), 3);
            world.setBlock(pos.below(), this.defaultBlockState(), 3);
        }
    }

    /** Original {@code getFullBlock(empty)}. */
    @Nullable
    public static Block getFullBlock(Block block) {
        return emptyToFull.get(block);
    }

    /** Leeren Block mit dem vollen verknuepfen (nach der Registrierung, Original im Konstruktor). */
    void link() {
        if (empty != null) emptyToFull.put(empty.get(), this);
    }

    private static ResourceLocation hbm(String path) {
        return ResourceLocation.fromNamespaceAndPath("hbm_m", path);
    }
}
