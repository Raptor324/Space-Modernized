package com.space_m.machine.chem.client;

import com.hbm_m.client.render.SimpleObjModel;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.space_m.SpaceMod;

import net.minecraft.resources.ResourceLocation;

/**
 * Modelle/Texturen der Chemie-Maschinen (Original {@code ResourceManager}: {@code alkyl}, {@code cryo_distill},
 * {@code milk_reformer} ({@code milker.obj}), {@code vac_cir_station} ({@code vacuum_solderer.obj}), {@code hydroponic}).
 */
public final class ChemModels {
    private ChemModels() {}

    public static final SimpleObjModel ALKYL = new SimpleObjModel(SpaceMod.id("models/machines/alkylation_unit.obj"));
    public static final SimpleObjModel CRYO_DISTILL = new SimpleObjModel(SpaceMod.id("models/machines/cryo_distill.obj"));
    public static final SimpleObjModel MILK_REFORMER = new SimpleObjModel(SpaceMod.id("models/machines/milker.obj"));
    public static final SimpleObjModel VAC_CIR_STATION = new SimpleObjModel(SpaceMod.id("models/machines/vacuum_solderer.obj"));
    public static final SimpleObjModel HYDROPONIC = new SimpleObjModel(SpaceMod.id("models/machines/hydroponic.obj"));

    public static final ResourceLocation ALKYL_TEX = SpaceMod.id("textures/models/machines/alkylation_unit.png");
    public static final ResourceLocation CRYODISTILL_TEX = SpaceMod.id("textures/models/machines/cryo_distiller.png");
    public static final ResourceLocation MILK_REFORMER_TEX = SpaceMod.id("textures/models/machines/milker.png");
    public static final ResourceLocation VAC_CIR_STATION_TEX = SpaceMod.id("textures/models/machines/vacuum_solderer.png");
    public static final ResourceLocation HYDROPONIC_TEX = SpaceMod.id("textures/models/machines/hydroponic.png");

    /** {@code case 2: 0; case 4: 90; case 3: 180; case 5: 270}. */
    public static void rotate0(PoseStack ps, int meta) {
        switch (meta) {
            case 4 -> ps.mulPose(Axis.YP.rotationDegrees(90));
            case 3 -> ps.mulPose(Axis.YP.rotationDegrees(180));
            case 5 -> ps.mulPose(Axis.YP.rotationDegrees(270));
            default -> { }
        }
    }

    /** {@code case 2: 90; case 4: 180; case 3: 270; case 5: 0}. */
    public static void rotate90(PoseStack ps, int meta) {
        switch (meta) {
            case 2 -> ps.mulPose(Axis.YP.rotationDegrees(90));
            case 4 -> ps.mulPose(Axis.YP.rotationDegrees(180));
            case 3 -> ps.mulPose(Axis.YP.rotationDegrees(270));
            default -> { }
        }
    }
}
