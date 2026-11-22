package com.space_m.client.sky;

import com.hbm_m.client.model.nmesh.NMeshData;
import com.hbm_m.client.model.nmesh.NMeshLoader;
import com.space_m.SpaceMod;
import com.space_m.main.SpaceRegistry;

import net.minecraft.resources.ResourceLocation;

/**
 * Minimaler Lader fuer die drei Himmelsmodelle ({@code ResourceManager.plane},
 * {@code sphere_v2}, {@code sat_rail} in NTM Space) auf Basis des {@link NMeshLoader} aus HBM.
 * Entspricht {@code IModelCustom.renderAll()} des Forge-1.7.10-Laders
 * ({@code WavefrontObject}: Texturkoordinate {@code v} gespiegelt, Dreiecke ueber {@link SkyTess}).
 */
public final class SkyObjModel {

    public static final SkyObjModel PLANE = new SkyObjModel(SpaceMod.id("models/obj/plane.obj"));
    public static final SkyObjModel SPHERE_V2 = new SkyObjModel(SpaceMod.id("models/obj/spherev2.obj"));
    public static final SkyObjModel SAT_RAIL = new SkyObjModel(SpaceMod.id("models/obj/railgun_main.obj"));
    /** Original {@code ResourceManager.sat_rail_tex}. */
    public static final ResourceLocation SAT_RAIL_TEX = SpaceMod.id("textures/models/railgun_main.png");

    private final ResourceLocation location;
    /** Dreiecke: je Ecke x, y, z, u, v. */
    private float[] tris;

    public SkyObjModel(ResourceLocation location) {
        this.location = location;
    }

    private void load() {
        NMeshData nmesh = NMeshLoader.load(location);
        if (nmesh == null) {
            SpaceRegistry.LOGGER.error("Missing sky model {}", location);
            tris = new float[0];
            return;
        }
        int totalVerts = 0;
        for (NMeshData.SubMesh sm : nmesh.subMeshes()) {
            totalVerts += sm.indexCount();
        }
        float[] out = new float[totalVerts * 5];
        int cursor = 0;
        for (NMeshData.SubMesh sm : nmesh.subMeshes()) {
            int iCount = sm.indexCount();
            for (int i = 0; i < iCount; i++) {
                int idx = sm.index(i);
                out[cursor++] = sm.posX(idx);
                out[cursor++] = sm.posY(idx);
                out[cursor++] = sm.posZ(idx);
                out[cursor++] = sm.u(idx);
                out[cursor++] = 1F - sm.v(idx);
            }
        }
        tris = out;
    }

    /** {@code IModelCustom.renderAll()}. */
    public void renderAll() {
        if (tris == null) load();
        if (tris.length == 0) return;
        SkyTess.startDrawing(SkyTess.TRIANGLES);
        for (int i = 0; i < tris.length; i += 5) {
            SkyTess.addVertexWithUV(tris[i], tris[i + 1], tris[i + 2], tris[i + 3], tris[i + 4]);
        }
        SkyTess.draw();
    }

    /** Ressourcen neu geladen. */
    public void reset() {
        tris = null;
    }
}
