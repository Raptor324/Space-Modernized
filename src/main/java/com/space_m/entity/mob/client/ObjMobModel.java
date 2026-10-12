package com.space_m.entity.mob.client;

import com.hbm_m.client.render.SimpleObjModel;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.space_m.SpaceMod;

import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.entity.LivingEntity;

/**
 * Gegenstueck zu den {@code ModelBase}-Klassen der Space-Mobs, die OBJ-Teile ({@code ResourceManager.<mob>.renderPart})
 * mit GL-Drehungen animieren. {@link #setupAnim} merkt sich die Argumente von {@code ModelBase.render(entity,
 * limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch, scale)}; {@link #draw} zeichnet im Modellraum des
 * {@code RenderLiving} (gleiche Lage wie 1.7.10: Spiegelung -1/-1/1, Versatz -1.5).
 */
public abstract class ObjMobModel<T extends LivingEntity> extends EntityModel<T> {

    protected T entity;
    protected float limbSwing;
    protected float limbSwingAmount;
    protected float ageInTicks;
    protected float netHeadYaw;
    protected float headPitch;
    protected float partialTick;
    /** Fuer Modelle mit mehreren Texturen (vom Renderer vor {@code super.render} gesetzt). */
    public MultiBufferSource buffers;

    public static SimpleObjModel obj(String name) {
        return new SimpleObjModel(SpaceMod.id("models/mobs/" + name + ".obj"));
    }

    @Override
    public void prepareMobModel(T entity, float limbSwing, float limbSwingAmount, float partialTick) {
        this.partialTick = partialTick;
    }

    @Override
    public void setupAnim(T entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
        this.entity = entity;
        this.limbSwing = limbSwing;
        this.limbSwingAmount = limbSwingAmount;
        this.ageInTicks = ageInTicks;
        this.netHeadYaw = netHeadYaw;
        this.headPitch = headPitch;
    }

    @Override
    //? if < 1.21.1 {
    public void renderToBuffer(PoseStack ps, VertexConsumer vc, int light, int overlay, float r, float g, float b, float a) {
    //?} else {
    /*public void renderToBuffer(PoseStack ps, VertexConsumer vc, int light, int overlay, int color) {
        float r = net.minecraft.util.FastColor.ARGB32.red(color) / 255F, g = net.minecraft.util.FastColor.ARGB32.green(color) / 255F;
        float b = net.minecraft.util.FastColor.ARGB32.blue(color) / 255F, a = net.minecraft.util.FastColor.ARGB32.alpha(color) / 255F;
    *///?}
        ps.pushPose();
        draw(ps, vc, light, overlay, r, g, b, a);
        ps.popPose();
    }

    protected abstract void draw(PoseStack ps, VertexConsumer vc, int light, int overlay, float r, float g, float b, float a);

    /** Kleiner Helfer: {@code model.renderPart(name)} mit Licht/Overlay/Farbe. */
    protected static void part(SimpleObjModel model, String name, PoseStack ps, VertexConsumer vc, int light, int overlay, float r, float g, float b, float a) {
        model.renderPartEntity(name, ps, vc, light, overlay, r, g, b, a);
    }
}
