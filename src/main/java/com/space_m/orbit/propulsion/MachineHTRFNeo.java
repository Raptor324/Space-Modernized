package com.space_m.orbit.propulsion;

import java.util.ArrayList;
import java.util.List;

import org.jetbrains.annotations.Nullable;

import com.hbm_m.block.ModBlocks;
import com.hbm_m.multiblock.DummyableStructureBuilder;
import com.hbm_m.multiblock.MultiblockStructureHelper;
import com.hbm_m.util.BobMathUtil;
import com.space_m.celestial.CelestialBody;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 1:1 NTM Space {@code MachineHTRFNeo} ({@code machine_htrf4neo}, "HTR-F4 NEO Station Thruster"): Struktur wie HTR-F4
 * ({@code {2, 2, 2, 2, 11, 9}}, Offset 11, Hoehenversatz 2, gedrehte Platzierung). Anschlusszellen: {@code -rot*9}
 * (Plasma vom Torus) und vier Zellen zwei Ebenen tiefer fuer Kuehlmittel/Strom. Das Original rechnet diese vier mit
 * {@code x ... dir.offsetX * 3}, aber {@code z ... dir.offsetZ * 2}; wirksam (Zelle gehoert zur Struktur) sind sie nur bei
 * Nord-/Sued-Ausrichtung - also genau bei der prograden Ausrichtung SOUTH - und liegen dann bei {@code +-dir*2}. Diese
 * Zellen werden hier fest gesetzt (technisch noetig: der Builder kennt keine ausrichtungsabhaengigen Zellen).
 * Original-Klasse: MachineHTRFNeo
 */
public class MachineHTRFNeo extends StationThrusterBlock {

    private static final int[][] EXTRAS = { {0, 0, -9}, {-2, -2, -5}, {-2, -2, 1}, {2, -2, -5}, {2, -2, 1} };

    public MachineHTRFNeo(Properties properties) {
        super(properties);
    }

    @Override
    protected boolean rotatedPlacement() {
        return true;
    }

    @Override
    protected int[][] extras() {
        return EXTRAS;
    }

    @Override
    protected MultiblockStructureHelper defineStructure() {
        DummyableStructureBuilder b = DummyableStructureBuilder.create().box(2, 2, 2, 2, 11, 9);
        for (int[] e : EXTRAS) b.extra(e[0], e[1], e[2]);
        return b.placementOffset(11).build(() -> ModBlocks.UNIVERSAL_MACHINE_PART.get().defaultBlockState());
    }

    @Override
    public int getHeightOffset() {
        return 2;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new TileEntityMachineHTRNeo(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return createTickerHelper(type, PropulsionBlockEntities.HTRF4_NEO.get(), TileEntityMachineHTRNeo::tick);
    }

    @Override
    public void printHook(GuiGraphics g, Level world, BlockPos pos) {
        if (!CelestialBody.inOrbit(world)) return;

        if (!(world.getBlockEntity(pos) instanceof TileEntityMachineHTRNeo thruster)) return;

        List<Component> text = new ArrayList<>();

        if (!thruster.isFacingPrograde()) {
            text.add(PropulsionUtil.blinkWarning("atmosphere.engineFacing"));
        } else {
            text.add(Component.literal("Plasma Energy: ").append(Component.literal(BobMathUtil.getShortNumber(thruster.plasmaEnergy) + "TU")
                    .withStyle(thruster.plasmaEnergy == 0 ? ChatFormatting.RED : ChatFormatting.GREEN)));

            text.add(Component.literal("Power: ").append(Component.literal(BobMathUtil.getShortNumber(thruster.getEnergyStored()) + "HE")
                    .withStyle(thruster.getEnergyStored() < TileEntityMachineHTRNeo.maxPower ? ChatFormatting.RED : ChatFormatting.GREEN)));

            int heat = (int) Math.ceil(thruster.getTemperature());
            text.add(Component.literal("Temperature: ").append(Component.literal(heat + "K")
                    .withStyle(heat > 123 ? ChatFormatting.RED : ChatFormatting.AQUA)));

            text.add(PropulsionUtil.tankLine(thruster.getCoolantTanks()[0], true));
            text.add(PropulsionUtil.tankLine(thruster.getCoolantTanks()[1], false));

            if (!thruster.isCool()) text.add(PropulsionUtil.blinkLiteral("INSUFFICIENT COOLING"));

            BlockPos port = lookedAtPort(pos, world.getBlockState(pos));
            if (port != null) {
                if (pos.getX() == port.getX() || pos.getZ() == port.getZ()) {
                    text.add(Component.literal("Connect to Fusion Reactor from here"));
                }
            }
        }

        print(g, this, text);
    }

    /** Original {@code addInformation}: {@code addStandardInfo} (Umschalttaste zeigt {@code .desc}). */
    @Override
    //? if < 1.21.1 {
    public void appendHoverText(ItemStack stack, @Nullable net.minecraft.world.level.BlockGetter level,
                                List<Component> list, net.minecraft.world.item.TooltipFlag flag) {
    //?} else {
    /*public void appendHoverText(ItemStack stack, net.minecraft.world.item.Item.TooltipContext hbmTooltipCtx, List<Component> list, net.minecraft.world.item.TooltipFlag flag) {
    *///?}
        com.hbm_m.util.StandardInfo.add(list, getDescriptionId() + ".desc");
    }

    //? if >1.20.1 {
    /*public static final com.mojang.serialization.MapCodec<MachineHTRFNeo> CODEC = simpleCodec(MachineHTRFNeo::new);
    @Override protected com.mojang.serialization.MapCodec<? extends net.minecraft.world.level.block.BaseEntityBlock> codec() { return CODEC; }
    *///?}
}
