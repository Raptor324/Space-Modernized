package com.space_m.rocket.client;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.jetbrains.annotations.Nullable;

import com.hbm_m.client.model.nmesh.NMeshData;
import com.hbm_m.client.model.nmesh.NMeshLoader;
import com.hbm_m.client.model.nmesh.NMeshPartBakedModel;
import com.hbm_m.client.render.MeshRenderCache;
import com.hbm_m.client.render.SingleMeshVboRenderer;
import com.hbm_m.client.render.SimpleObjModel;
import com.hbm_m.client.render.cache.RenderCacheManager;
import com.mojang.math.Transformation;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.client.renderer.texture.MissingTextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.resources.ResourceLocation;

/**
 * Modell-, Textur- und GPU-VBO-Cache der Raketenteile ("ns:pfad" aus {@link com.space_m.rocket.RocketPart}).
 * Verwendet den Nucleus-Grafikengine-Cache (SingleMeshVboRenderer und MeshRenderCache).
 */
public final class RocketModels {

    private RocketModels() {}

    private static final Map<String, SimpleObjModel> MODELS = new HashMap<>();
    private static final Map<String, ResourceLocation> TEXTURES = new HashMap<>();
    private static final ConcurrentHashMap<String, SingleMeshVboRenderer> VBO_CACHE = new ConcurrentHashMap<>();

    static {
        RenderCacheManager.register(reason -> {
            VBO_CACHE.clear();
        });
    }

    @Deprecated
    public static SimpleObjModel model(String loc) {
        return MODELS.computeIfAbsent(loc, l -> new SimpleObjModel(ResourceLocation.tryParse(l)));
    }

    public static ResourceLocation texture(String loc) {
        return TEXTURES.computeIfAbsent(loc, ResourceLocation::tryParse);
    }

    public static ResourceLocation normalizeSpriteLocation(ResourceLocation loc) {
        if (loc == null) return null;
        String path = loc.getPath();
        if (path.startsWith("textures/")) {
            path = path.substring("textures/".length());
        }
        if (path.endsWith(".png")) {
            path = path.substring(0, path.length() - 4);
        }
        return ResourceLocation.fromNamespaceAndPath(loc.getNamespace(), path);
    }

    public static @Nullable SingleMeshVboRenderer getVbo(String modelLoc, @Nullable String partName, @Nullable ResourceLocation textureLoc) {
        if (modelLoc == null) return null;
        String partTag = (partName != null && !partName.isEmpty()) ? partName : "all";
        String texTag = (textureLoc != null) ? textureLoc.toString().replace(':', '_') : "none";
        String sanitizedModel = modelLoc.replace(':', '_');
        String cacheKey = "space_rocket:" + sanitizedModel + "#" + partTag + "@" + texTag;

        SingleMeshVboRenderer cached = VBO_CACHE.get(cacheKey);
        if (cached != null) {
            return cached;
        }

        SingleMeshVboRenderer peeked = MeshRenderCache.peekRenderer(cacheKey);
        if (peeked != null) {
            VBO_CACHE.put(cacheKey, peeked);
            return peeked;
        }

        SingleMeshVboRenderer compiled = compileVbo(cacheKey, modelLoc, partName, textureLoc);
        if (compiled != null) {
            VBO_CACHE.put(cacheKey, compiled);
        }
        return compiled;
    }

    public static @Nullable SingleMeshVboRenderer getVbo(String modelLoc, @Nullable ResourceLocation textureLoc) {
        return getVbo(modelLoc, null, textureLoc);
    }

    public static @Nullable SingleMeshVboRenderer getVbo(String modelLoc, @Nullable String partName, @Nullable String textureLoc) {
        return getVbo(modelLoc, partName, textureLoc != null ? texture(textureLoc) : null);
    }

    public static @Nullable SingleMeshVboRenderer getVbo(String modelLoc, @Nullable String textureLoc) {
        return getVbo(modelLoc, null, textureLoc != null ? texture(textureLoc) : null);
    }

    private static synchronized SingleMeshVboRenderer compileVbo(String cacheKey, String modelLoc, @Nullable String partName, @Nullable ResourceLocation textureLoc) {
        SingleMeshVboRenderer existing = MeshRenderCache.peekRenderer(cacheKey);
        if (existing != null) {
            return existing;
        }

        ResourceLocation modelRl = ResourceLocation.tryParse(modelLoc);
        if (modelRl == null) return null;

        NMeshData nmesh = NMeshLoader.load(modelRl);
        if (nmesh == null) {
            return null;
        }

        List<NMeshData.SubMesh> subs;
        if (partName == null || partName.isEmpty()) {
            subs = nmesh.subMeshes();
        } else {
            subs = nmesh.getSubMeshesForPart(partName);
            if (subs.isEmpty()) {
                for (NMeshData.SubMesh sm : nmesh.subMeshes()) {
                    if (sm.objectName.equalsIgnoreCase(partName) || sm.groupName.equalsIgnoreCase(partName)) {
                        if (subs.isEmpty()) subs = new ArrayList<>();
                        subs.add(sm);
                    }
                }
            }
        }

        if (subs.isEmpty()) {
            return null;
        }

        TextureAtlasSprite defaultSprite = resolveSprite(textureLoc);
        List<NMeshPartBakedModel.BoundSubMesh> bound = new ArrayList<>(subs.size());
        for (NMeshData.SubMesh sm : subs) {
            TextureAtlasSprite sprite = defaultSprite;
            if (textureLoc == null && !sm.mapKdTexture.isEmpty()) {
                ResourceLocation mtlTex = ResourceLocation.tryParse(sm.mapKdTexture);
                if (mtlTex != null) {
                    sprite = resolveSprite(mtlTex);
                }
            }
            bound.add(new NMeshPartBakedModel.BoundSubMesh(sm, sprite));
        }

        BakedModel bakedModel = new NMeshPartBakedModel(bound, true, Transformation.identity(), ItemTransforms.NO_TRANSFORMS);
        return MeshRenderCache.getOrCreateRenderer(cacheKey, bakedModel);
    }

    private static @Nullable TextureAtlasSprite resolveSprite(@Nullable ResourceLocation textureLoc) {
        try {
            Minecraft mc = Minecraft.getInstance();
            if (mc == null) return null;
            var mm = mc.getModelManager();
            if (mm == null) return null;
            var atlas = mm.getAtlas(TextureAtlas.LOCATION_BLOCKS);
            if (atlas == null) return null;
            if (textureLoc == null) {
                return atlas.getSprite(MissingTextureAtlasSprite.getLocation());
            }
            ResourceLocation normalized = normalizeSpriteLocation(textureLoc);
            return atlas.getSprite(normalized);
        } catch (Throwable t) {
            return null;
        }
    }
}
