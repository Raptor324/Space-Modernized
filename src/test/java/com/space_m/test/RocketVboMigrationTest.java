package com.space_m.test;

import com.space_m.rocket.client.RocketModels;
import com.space_m.rocket.client.RocketPronter;
import com.space_m.rocket.client.RenderDropPod;
import com.space_m.rocket.transport.client.CombatDropPodRenderer;
import net.minecraft.resources.ResourceLocation;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;

/**
 * Milestone 3 Verification Suite: Modular Rockets & Drop Pods VBO Migration.
 * Verifies sprite normalization, RocketModels cache semantics, RenderDropPod,
 * CombatDropPodRenderer skins, and atlas stitching declarations.
 */
public final class RocketVboMigrationTest {

    private RocketVboMigrationTest() {}

    public static void runAll() {
        System.out.println("=================================================================");
        System.out.println("Executing RocketVboMigrationTest (Milestone 3)");
        System.out.println("=================================================================");

        testSpriteNormalization();
        testRocketModelsCompatibility();
        testDropPodConfiguration();
        testCombatDropPodSkins();
        testBlockAtlasStitchingJson();

        System.out.println(">>> ALL RocketVboMigrationTest TESTS PASSED <<<");
    }

    private static void testSpriteNormalization() {
        System.out.println("  [RocketTest] Running testSpriteNormalization...");

        // null safety
        check(RocketModels.normalizeSpriteLocation(null) == null, "null input must return null");

        // Standard texture path with "textures/" prefix and ".png" suffix
        ResourceLocation standard = ResourceLocation.fromNamespaceAndPath("space_m", "textures/models/missile_parts/warheads/rp_drop_pod.png");
        ResourceLocation normStandard = RocketModels.normalizeSpriteLocation(standard);
        check(normStandard != null, "Normalized standard must not be null");
        check(normStandard.getNamespace().equals("space_m"), "Namespace must be preserved");
        check(normStandard.getPath().equals("models/missile_parts/warheads/rp_drop_pod"),
                "Expected 'models/missile_parts/warheads/rp_drop_pod', got: " + normStandard.getPath());

        // hbm_m universal texture
        ResourceLocation hbmTex = ResourceLocation.fromNamespaceAndPath("hbm_m", "textures/models/thegadget3_.png");
        ResourceLocation normHbm = RocketModels.normalizeSpriteLocation(hbmTex);
        check(normHbm.getPath().equals("models/thegadget3_"),
                "Expected 'models/thegadget3_', got: " + normHbm.getPath());

        // Already normalized path without textures/ and without .png
        ResourceLocation alreadyNorm = ResourceLocation.fromNamespaceAndPath("space_m", "misc/space/kerbol");
        ResourceLocation normAlready = RocketModels.normalizeSpriteLocation(alreadyNorm);
        check(normAlready.getPath().equals("misc/space/kerbol"),
                "Expected 'misc/space/kerbol', got: " + normAlready.getPath());

        System.out.println("  [RocketTest] testSpriteNormalization PASSED.");
    }

    private static void testRocketModelsCompatibility() {
        System.out.println("  [RocketTest] Running testRocketModelsCompatibility...");

        // texture caching
        ResourceLocation t1 = RocketModels.texture("space_m:textures/models/bombs/combat_drop_pod.blue.png");
        ResourceLocation t2 = RocketModels.texture("space_m:textures/models/bombs/combat_drop_pod.blue.png");
        check(t1 != null && t1.equals(t2), "Texture locations must resolve and be equal");
        check(t1 == t2, "Texture locations must be cached identically");

        // null safety on getVbo
        check(RocketModels.getVbo(null, null, (ResourceLocation) null) == null, "null model must return null VBO");

        System.out.println("  [RocketTest] testRocketModelsCompatibility PASSED.");
    }

    private static void testDropPodConfiguration() {
        System.out.println("  [RocketTest] Running testDropPodConfiguration...");

        RenderDropPod dropPod = new RenderDropPod();
        check(dropPod instanceof com.space_m.rocket.client.RocketPartRenderer,
                "RenderDropPod must implement RocketPartRenderer");

        System.out.println("  [RocketTest] testDropPodConfiguration PASSED.");
    }

    private static void testCombatDropPodSkins() {
        System.out.println("  [RocketTest] Running testCombatDropPodSkins...");

        ResourceLocation red = CombatDropPodRenderer.skin(1);
        ResourceLocation yellow = CombatDropPodRenderer.skin(2);
        ResourceLocation white0 = CombatDropPodRenderer.skin(0);
        ResourceLocation whiteDefault = CombatDropPodRenderer.skin(99);

        check(red.equals(CombatDropPodRenderer.SKIN_RED), "Color 1 must be SKIN_RED");
        check(yellow.equals(CombatDropPodRenderer.SKIN_YELLOW), "Color 2 must be SKIN_YELLOW");
        check(white0.equals(CombatDropPodRenderer.SKIN_WHITE), "Color 0 must be SKIN_WHITE");
        check(whiteDefault.equals(CombatDropPodRenderer.SKIN_WHITE), "Unknown color must fallback to SKIN_WHITE");

        check(CombatDropPodRenderer.MODEL_LOC.equals("space_m:models/combat_drop_pod.obj"),
                "Combat drop pod model location must match space_m:models/combat_drop_pod.obj");

        System.out.println("  [RocketTest] testCombatDropPodSkins PASSED.");
    }

    private static void testBlockAtlasStitchingJson() {
        System.out.println("  [RocketTest] Running testBlockAtlasStitchingJson...");

        try {
            java.nio.file.Path jsonPath = java.nio.file.Path.of("src/main/resources/assets/minecraft/atlases/blocks.json");
            check(java.nio.file.Files.exists(jsonPath), "assets/minecraft/atlases/blocks.json must exist");

            String content = java.nio.file.Files.readString(jsonPath, StandardCharsets.UTF_8);
            check(content.contains("models/missile_parts"), "blocks.json must include models/missile_parts");
            check(content.contains("models/bombs"), "blocks.json must include models/bombs");
            check(content.contains("models/trinkets"), "blocks.json must include models/trinkets");
            check(content.contains("entity"), "blocks.json must include entity directory");
        } catch (Exception e) {
            throw new RuntimeException("Failed reading blocks.json: " + e.getMessage(), e);
        }

        System.out.println("  [RocketTest] testBlockAtlasStitchingJson PASSED.");
    }

    private static void check(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError("Assertion failed: " + message);
        }
    }

    public static void main(String[] args) {
        runAll();
    }
}
