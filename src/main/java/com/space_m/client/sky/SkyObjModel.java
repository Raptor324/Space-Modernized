package com.space_m.client.sky;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.space_m.SpaceMod;
import com.space_m.main.SpaceRegistry;

import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;

/**
 * Minimaler Wavefront-Lader fuer die drei OBJ-Modelle des Himmels ({@code ResourceManager.plane},
 * {@code sphere_v2}, {@code sat_rail} in NTM Space). Entspricht {@code IModelCustom.renderAll()} des Forge-1.7.10-Laders
 * ({@code WavefrontObject}: Texturkoordinate {@code v} gespiegelt, Flaechen als Dreiecke/Vierecke).
 * Gezeichnet wird ueber {@link SkyTess} (aktueller {@link SkyGL}-Zustand, aktiver Rohshader moeglich).
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
        List<float[]> v = new ArrayList<>();
        List<float[]> vt = new ArrayList<>();
        List<Float> out = new ArrayList<>();
        Optional<Resource> res = Minecraft.getInstance().getResourceManager().getResource(location);
        if (res.isEmpty()) {
            SpaceRegistry.LOGGER.error("Missing OBJ model {}", location);
            tris = new float[0];
            return;
        }
        try (BufferedReader r = new BufferedReader(new InputStreamReader(res.get().open(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = r.readLine()) != null) {
                line = line.trim();
                if (line.startsWith("v ")) {
                    String[] t = line.split("\\s+");
                    v.add(new float[] {Float.parseFloat(t[1]), Float.parseFloat(t[2]), Float.parseFloat(t[3])});
                } else if (line.startsWith("vt ")) {
                    String[] t = line.split("\\s+");
                    vt.add(new float[] {Float.parseFloat(t[1]), 1F - Float.parseFloat(t[2])});
                } else if (line.startsWith("f ")) {
                    String[] t = line.split("\\s+");
                    int n = t.length - 1;
                    int[][] idx = new int[n][2];
                    for (int i = 0; i < n; i++) {
                        String[] p = t[i + 1].split("/");
                        idx[i][0] = Integer.parseInt(p[0]) - 1;
                        idx[i][1] = p.length > 1 && !p[1].isEmpty() ? Integer.parseInt(p[1]) - 1 : -1;
                    }
                    // Faecher-Triangulierung (Dreiecke und Vierecke)
                    for (int i = 1; i + 1 < n; i++) {
                        for (int k : new int[] {0, i, i + 1}) {
                            float[] pos = v.get(idx[k][0]);
                            float[] uv = idx[k][1] >= 0 ? vt.get(idx[k][1]) : new float[] {0, 0};
                            out.add(pos[0]); out.add(pos[1]); out.add(pos[2]); out.add(uv[0]); out.add(uv[1]);
                        }
                    }
                }
            }
        } catch (Exception e) {
            SpaceRegistry.LOGGER.error("Failed to read OBJ model {}", location, e);
        }
        tris = new float[out.size()];
        for (int i = 0; i < tris.length; i++) tris[i] = out.get(i);
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
