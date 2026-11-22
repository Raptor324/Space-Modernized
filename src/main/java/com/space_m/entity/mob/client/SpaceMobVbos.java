package com.space_m.entity.mob.client;

import java.util.ArrayList;
import java.util.List;

import org.jetbrains.annotations.Nullable;

import com.hbm_m.client.model.nmesh.NMeshData;
import com.hbm_m.client.model.nmesh.NMeshLoader;
import com.hbm_m.client.model.nmesh.NMeshPartBakedModel;
import com.hbm_m.client.render.MeshRenderCache;
import com.hbm_m.client.render.SimpleObjModel;
import com.hbm_m.client.render.SingleMeshVboRenderer;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Transformation;
import com.space_m.SpaceMod;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;

/**
 * Nucleus VBO compilation and caching bridge for Space mobs, bosses, and war entities.
 * <p>
 * Connects binary {@link NMeshData} or fallback OBJ models to cached {@link SingleMeshVboRenderer}
 * instances via {@link MeshRenderCache}, binding sprites from {@link TextureAtlas#LOCATION_BLOCKS}.
 * Fully compatible with {@link PoseStack} hierarchical transformations and Iris/Oculus shaders.
 */
public final class SpaceMobVbos {

    private SpaceMobVbos() {}

    /**
     * Normalizes a texture path to a sprite identifier for TextureAtlas.LOCATION_BLOCKS,
     * stripping textures/ prefix and .png suffix.
     */
    @Nullable
    public static ResourceLocation normalizeSpriteLocation(@Nullable ResourceLocation textureLoc) {
        if (textureLoc == null) return null;
        String path = textureLoc.getPath();
        if (path.startsWith("textures/")) {
            path = path.substring(9);
        }
        if (path.endsWith(".png")) {
            path = path.substring(0, path.length() - 4);
        }
        return ResourceLocation.fromNamespaceAndPath(textureLoc.getNamespace(), path);
    }

    /**
     * Resolves the {@link TextureAtlasSprite} stitched into {@link TextureAtlas#LOCATION_BLOCKS}.
     * Normalizes paths by removing {@code textures/} prefix and {@code .png} suffix if present.
     */
    @Nullable
    public static TextureAtlasSprite resolveSprite(@Nullable ResourceLocation textureLoc) {
        if (textureLoc == null) return null;
        try {
            Minecraft mc = Minecraft.getInstance();
            if (mc == null || mc.getModelManager() == null) return null;
            var atlas = mc.getModelManager().getAtlas(TextureAtlas.LOCATION_BLOCKS);
            if (atlas == null) return null;

            ResourceLocation spriteId = normalizeSpriteLocation(textureLoc);
            return atlas.getSprite(spriteId);
        } catch (Throwable ignored) {
            return null;
        }
    }

    /**
     * Retrieves or compiles a {@link SingleMeshVboRenderer} for the specified model part and texture.
     */
    @Nullable
    public static SingleMeshVboRenderer getOrCompile(@Nullable ResourceLocation modelLoc,
                                                     @Nullable String partName,
                                                     @Nullable ResourceLocation textureLoc) {
        if (modelLoc == null) return null;

        String effectivePart = (partName != null && !partName.isEmpty()) ? partName : "all";
        String partKey = modelLoc + "#" + effectivePart + "#" + (textureLoc != null ? textureLoc : "none");

        SingleMeshVboRenderer cached = MeshRenderCache.peekRenderer(partKey);
        if (cached != null) {
            return cached;
        }

        NMeshData nmesh = NMeshLoader.load(modelLoc);
        if (nmesh == null) {
            return null;
        }

        List<NMeshData.SubMesh> subs;
        if (effectivePart.equalsIgnoreCase("all")) {
            subs = nmesh.subMeshes();
        } else {
            subs = nmesh.getSubMeshesForPart(effectivePart);
            if (subs.isEmpty()) {
                // Case-insensitive fallback
                for (NMeshData.SubMesh sm : nmesh.subMeshes()) {
                    if (sm.objectName.equalsIgnoreCase(effectivePart)
                            || sm.groupName.equalsIgnoreCase(effectivePart)
                            || sm.componentName.equalsIgnoreCase(effectivePart)) {
                        if (subs.isEmpty()) subs = new ArrayList<>();
                        subs.add(sm);
                    }
                }
            }
        }

        if (subs.isEmpty()) {
            return null;
        }

        TextureAtlasSprite sprite = resolveSprite(textureLoc);
        List<NMeshPartBakedModel.BoundSubMesh> bound = new ArrayList<>(subs.size());
        for (NMeshData.SubMesh sm : subs) {
            bound.add(new NMeshPartBakedModel.BoundSubMesh(sm, sprite));
        }

        NMeshPartBakedModel bakedModel = new NMeshPartBakedModel(
                bound, true, Transformation.identity(), ItemTransforms.NO_TRANSFORMS);

        return MeshRenderCache.getOrCreateRenderer(partKey, bakedModel);
    }

    /**
     * Convenience overload accepting a mob model name (e.g. {@code "scutterfish"} or {@code "mobs/scutterfish.obj"}).
     */
    @Nullable
    public static SingleMeshVboRenderer getOrCompile(String modelPath,
                                                     @Nullable String partName,
                                                     @Nullable ResourceLocation textureLoc) {
        ResourceLocation loc;
        if (modelPath.contains(":")) {
            loc = ResourceLocation.tryParse(modelPath);
        } else if (modelPath.startsWith("models/")) {
            loc = SpaceMod.id(modelPath);
        } else {
            loc = SpaceMod.id("models/mobs/" + modelPath + (modelPath.endsWith(".obj") ? "" : ".obj"));
        }
        return getOrCompile(loc, partName, textureLoc);
    }

    /**
     * Dispatches a single part render via VBO. Returns true if drawn via VBO.
     */
    public static boolean renderPartVbo(@Nullable ResourceLocation modelLoc,
                                        @Nullable String partName,
                                        @Nullable ResourceLocation textureLoc,
                                        PoseStack ps,
                                        int light,
                                        @Nullable BlockPos pos,
                                        @Nullable MultiBufferSource buffers) {
        if (buffers == null || textureLoc == null || modelLoc == null) return false;
        SingleMeshVboRenderer vbo = getOrCompile(modelLoc, partName, textureLoc);
        if (vbo != null) {
            vbo.render(ps, light, pos != null ? pos : BlockPos.ZERO, null, buffers);
            return true;
        }
        return false;
    }

    /**
     * Renders a part using VBO if available, with immediate-mode {@link SimpleObjModel} fallback.
     */
    public static void renderPart(@Nullable SimpleObjModel fallback,
                                  String partName,
                                  @Nullable ResourceLocation textureLoc,
                                  PoseStack ps,
                                  @Nullable VertexConsumer vc,
                                  int light,
                                  int overlay,
                                  @Nullable BlockPos pos,
                                  @Nullable MultiBufferSource buffers,
                                  float r, float g, float b, float a) {
        if (fallback != null && textureLoc != null && buffers != null) {
            if (renderPartVbo(fallback.getLocation(), partName, textureLoc, ps, light, pos, buffers)) {
                return;
            }
        }
        if (fallback != null && vc != null) {
            fallback.renderPartEntity(partName, ps, vc, light, overlay, r, g, b, a);
        }
    }

    /**
     * Renders all parts using VBO if available, with immediate-mode {@link SimpleObjModel} fallback.
     */
    public static void renderAll(@Nullable SimpleObjModel fallback,
                                 @Nullable ResourceLocation textureLoc,
                                 PoseStack ps,
                                 @Nullable VertexConsumer vc,
                                 int light,
                                 int overlay,
                                 @Nullable BlockPos pos,
                                 @Nullable MultiBufferSource buffers,
                                 float r, float g, float b, float a) {
        if (fallback != null && textureLoc != null && buffers != null) {
            if (renderPartVbo(fallback.getLocation(), null, textureLoc, ps, light, pos, buffers)) {
                return;
            }
        }
        if (fallback != null && vc != null) {
            fallback.renderAllEntity(ps, vc, light, overlay, r, g, b, a);
        }
    }
}
