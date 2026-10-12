package com.space_m.entity.mob.client;

import java.util.Locale;
import java.util.function.Function;

import com.space_m.SpaceMod;
import com.space_m.entity.mob.IEntityEnumMulti;

import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Mob;

/**
 * 1:1 {@code RenderEntityMulti} / {@code RenderScutter}: OBJ-Modell mit Textur je Variante
 * ({@code textures/entity/<enum>_<wert>.png}) bzw. fester Textur.
 */
public class RenderEntityMulti<T extends Mob> extends MobRenderer<T, ObjMobModel<T>> {

    private final Function<T, ResourceLocation> texture;

    public RenderEntityMulti(EntityRendererProvider.Context ctx, ObjMobModel<T> model, float shadowSize, Function<T, ResourceLocation> texture) {
        super(ctx, model, shadowSize);
        this.texture = texture;
    }

    /** Texturen nach {@code theEnum.getSimpleName().toLowerCase() + "_" + wert.name().toLowerCase()}. */
    public static <T extends Mob> Function<T, ResourceLocation> byEnum(Class<? extends Enum<?>> theEnum) {
        Enum<?>[] order = theEnum.getEnumConstants();
        ResourceLocation[] textures = new ResourceLocation[order.length];
        for (int i = 0; i < order.length; i++) {
            textures[i] = SpaceMod.id("textures/entity/" + theEnum.getSimpleName().toLowerCase(Locale.US) + "_" + order[i].name().toLowerCase(Locale.US) + ".png");
        }
        return e -> textures[((IEntityEnumMulti) e).getEnum().ordinal()];
    }

    @Override
    public ResourceLocation getTextureLocation(T entity) {
        return texture.apply(entity);
    }
}
