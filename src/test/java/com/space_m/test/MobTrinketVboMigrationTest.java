package com.space_m.test;

import com.space_m.entity.mob.client.SpaceMobModels;
import com.space_m.entity.mob.client.SpaceMobVbos;
import com.space_m.fork.trinket.client.ForkTrinketRenderer;
import net.minecraft.resources.ResourceLocation;

import java.nio.charset.StandardCharsets;

/**
 * Milestone 4 Verification Suite: Mobs & Trinkets Nucleus Porting.
 * Verifies sprite normalization for mob/trinket textures, SpaceMobModels instantiation,
 * ForkTrinketRenderer unification constants, and atlas stitching declarations.
 */
public final class MobTrinketVboMigrationTest {

    private MobTrinketVboMigrationTest() {}

    public static void runAll() {
        System.out.println("=================================================================");
        System.out.println("Executing MobTrinketVboMigrationTest (Milestone 4)");
        System.out.println("=================================================================");

        testSpriteNormalization();
        testMobModelsInstantiation();
        testTrinketRendererUnification();
        testBlockAtlasStitchingJson();

        System.out.println(">>> ALL MobTrinketVboMigrationTest TESTS PASSED <<<");
    }

    private static void testSpriteNormalization() {
        System.out.println("  [MobTrinketTest] Running testSpriteNormalization...");

        // null safety
        check(SpaceMobVbos.normalizeSpriteLocation(null) == null, "null input must return null");

        // Standard mob texture with "textures/" prefix and ".png" suffix
        ResourceLocation mobTex = ResourceLocation.fromNamespaceAndPath("space_m", "textures/entity/mobs/scutterfish.png");
        ResourceLocation normMob = SpaceMobVbos.normalizeSpriteLocation(mobTex);
        check(normMob != null, "Normalized mob texture must not be null");
        check(normMob.getNamespace().equals("space_m"), "Namespace must be preserved");
        check(normMob.getPath().equals("entity/mobs/scutterfish"),
                "Expected 'entity/mobs/scutterfish', got: " + normMob.getPath());

        // Trinket texture
        ResourceLocation trinketTex = ResourceLocation.fromNamespaceAndPath("space_m", "textures/models/trinkets/gwen.png");
        ResourceLocation normTrinket = SpaceMobVbos.normalizeSpriteLocation(trinketTex);
        check(normTrinket.getPath().equals("models/trinkets/gwen"),
                "Expected 'models/trinkets/gwen', got: " + normTrinket.getPath());

        // Xenon trail
        ResourceLocation xenonTex = ResourceLocation.fromNamespaceAndPath("space_m", "textures/models/xenon_trail.png");
        ResourceLocation normXenon = SpaceMobVbos.normalizeSpriteLocation(xenonTex);
        check(normXenon.getPath().equals("models/xenon_trail"),
                "Expected 'models/xenon_trail', got: " + normXenon.getPath());

        // Universal HBM texture
        ResourceLocation hbmUniversal = ResourceLocation.fromNamespaceAndPath("hbm_m", "textures/models/trinkets/universal.png");
        ResourceLocation normHbm = SpaceMobVbos.normalizeSpriteLocation(hbmUniversal);
        check(normHbm.getPath().equals("models/trinkets/universal"),
                "Expected 'models/trinkets/universal', got: " + normHbm.getPath());

        System.out.println("  [MobTrinketTest] testSpriteNormalization PASSED.");
    }

    private static void testMobModelsInstantiation() {
        System.out.println("  [MobTrinketTest] Running testMobModelsInstantiation...");

        // Model objects
        check(SpaceMobModels.SCUTTERFISH != null, "SCUTTERFISH model must not be null");
        check(SpaceMobModels.SCUTTLECRAB != null, "SCUTTLECRAB model must not be null");
        check(SpaceMobModels.SCRAPFISH != null, "SCRAPFISH model must not be null");
        check(SpaceMobModels.DEPTHSQUID != null, "DEPTHSQUID model must not be null");
        check(SpaceMobModels.SIFTER_EEL != null, "SIFTER_EEL model must not be null");

        // Model instances
        SpaceMobModels.Scutter<?> scutter = new SpaceMobModels.Scutter<>();
        check(scutter != null, "Scutter instance must be creatable");

        SpaceMobModels.Scuttlecrab scuttlecrab = new SpaceMobModels.Scuttlecrab();
        check(scuttlecrab != null, "Scuttlecrab instance must be creatable");

        SpaceMobModels.ScrapFish<?> scrapFish = new SpaceMobModels.ScrapFish<>();
        check(scrapFish != null, "ScrapFish instance must be creatable");

        SpaceMobModels.SifterEel<?> sifterEel = new SpaceMobModels.SifterEel<>();
        check(sifterEel != null, "SifterEel instance must be creatable");

        SpaceMobModels.DepthSquid depthSquid = new SpaceMobModels.DepthSquid();
        check(depthSquid != null, "DepthSquid instance must be creatable");

        System.out.println("  [MobTrinketTest] testMobModelsInstantiation PASSED.");
    }

    private static void testTrinketRendererUnification() {
        System.out.println("  [MobTrinketTest] Running testTrinketRendererUnification...");

        // Models
        check(ForkTrinketRenderer.BOBBLE != null, "ForkTrinketRenderer.BOBBLE must not be null");
        check(ForkTrinketRenderer.TETO != null, "ForkTrinketRenderer.TETO must not be null");
        check(ForkTrinketRenderer.MIKU != null, "ForkTrinketRenderer.MIKU must not be null");
        check(ForkTrinketRenderer.NERU != null, "ForkTrinketRenderer.NERU must not be null");

        // Unified HBM constants
        check(ForkTrinketRenderer.SOCKET.equals(com.hbm_m.client.render.implementations.TrinketRenderer.SOCKET),
                "ForkTrinketRenderer.SOCKET must unify with HBM TrinketRenderer.SOCKET");
        check(ForkTrinketRenderer.UNIVERSAL.equals(com.hbm_m.client.render.implementations.TrinketRenderer.UNIVERSAL),
                "ForkTrinketRenderer.UNIVERSAL must unify with HBM TrinketRenderer.UNIVERSAL");

        // Unique Space-Modernized bobble skins
        check(ForkTrinketRenderer.BOBBLE_GWEN.getPath().equals("textures/models/trinkets/gwen.png"),
                "BOBBLE_GWEN path must be textures/models/trinkets/gwen.png");
        check(ForkTrinketRenderer.BOBBLE_MRKIMKIMORA.getPath().equals("textures/models/trinkets/mrkimkimora.png"),
                "BOBBLE_MRKIMKIMORA path must be textures/models/trinkets/mrkimkimora.png");

        System.out.println("  [MobTrinketTest] testTrinketRendererUnification PASSED.");
    }

    private static void testBlockAtlasStitchingJson() {
        System.out.println("  [MobTrinketTest] Running testBlockAtlasStitchingJson...");

        try {
            java.nio.file.Path jsonPath = java.nio.file.Path.of("src/main/resources/assets/minecraft/atlases/blocks.json");
            check(java.nio.file.Files.exists(jsonPath), "assets/minecraft/atlases/blocks.json must exist");

            String content = java.nio.file.Files.readString(jsonPath, StandardCharsets.UTF_8);
            check(content.contains("models/trinkets"), "blocks.json must include models/trinkets");
            check(content.contains("entity"), "blocks.json must include entity directory");
            check(content.contains("space_m:models/xenon_trail"), "blocks.json must include space_m:models/xenon_trail");
        } catch (Exception e) {
            throw new RuntimeException("Failed reading blocks.json: " + e.getMessage(), e);
        }

        System.out.println("  [MobTrinketTest] testBlockAtlasStitchingJson PASSED.");
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
