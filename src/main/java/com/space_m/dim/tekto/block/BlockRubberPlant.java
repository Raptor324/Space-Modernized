package com.space_m.dim.tekto.block;

import java.util.function.Predicate;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.BushBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * 1:1 {@code com.hbm.blocks.generic.BlockRubberPlant} ({@code rubber_plant} GRASS/FERN/SHRUB, "Short Rubber Plant")
 * und {@code BlockRubberCacti} ({@code rubber_tall}/spike_cacti CACT/BUSH/FLOWER, "Tall Rubber Plant"): je Sorte ein
 * Block. Haelt nur auf den Original-Boeden ({@code canPlaceBlockOn}), sonst faellt er ab; keine Kollision, voller
 * Auswahlrahmen wie das Original. Beute ueber die Loot-Tabellen (Rubber-Plant: nichts, Kakteen: sich selbst,
 * Blume: Paraffin-Samen, R9-B2).
 *
 * <p>{@code RenderRubberCactus}: OBJ {@code plant_spike} mit der Blocktextur der Sorte ({@code rubber_tall_<sorte>}),
 * im Blockmodell {@code space_m:block/rubber_tall_<sorte>} ({@code forge:obj}). Das Original zeichnet das Modell zweimal
 * (Drehung -PI und 0); die OBJ-Eckpunkte sind unter 180 Grad punktsymmetrisch, der zweite Durchgang deckt sich also
 * mit dem ersten. {@code ResourceManager.spike_plant_tex} ({@code textures/models/rubber_cacti.png}) wird im Original
 * nirgends benutzt.
 */
// Original-Klasse: BlockRubberCacti
// Original-Klasse: RenderRubberCactus
public class BlockRubberPlant extends BushBlock {

    private final Predicate<BlockState> soil;

    public BlockRubberPlant(Properties properties, Predicate<BlockState> soil) {
        super(properties);
        this.soil = soil;
    }

    @Override
    protected boolean mayPlaceOn(BlockState state, BlockGetter level, BlockPos pos) {
        return soil.test(state);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return Shapes.block();
    }

    //? if >= 1.21.1 {
    /*public static final com.mojang.serialization.MapCodec<BlockRubberPlant> CODEC = simpleCodec(p -> new BlockRubberPlant(p, s -> false));
    @Override protected com.mojang.serialization.MapCodec<? extends BushBlock> codec() { return CODEC; }
    *///?}
}
