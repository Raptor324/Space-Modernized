package com.space_m.test;

/**
 * Milestone 5 Unified Verification Suite.
 *
 * <p>Executes all Milestone 5 unit, integration, and adversarial tests:
 * 1. {@link ModItemStackHandlerDummyTest}: Container slot handlers & dummy inventory invariants.
 * 2. {@link MachineRenderRegistryTest}: Declarative machine specs, registration, and budget validation.
 *
 * <p>Exits with code 0 on complete success, or non-zero on assertion failure.
 */
public final class SpaceM5VerificationSuite {

    private SpaceM5VerificationSuite() {}

    public static void main(String[] args) {
        long startTime = System.currentTimeMillis();
        System.out.println("================================================================================");
        System.out.println("SPACE-MODERNIZED: MILESTONE 5 VERIFICATION & TESTING SUITE");
        System.out.println("Track: Container Slot Handlers & Nucleus Engine Registration");
        System.out.println("================================================================================");

        try {
            System.out.println("Initializing Minecraft Bootstrap & FML Paths...");
            java.nio.file.Path runDir = java.nio.file.Path.of("run").toAbsolutePath();
            java.nio.file.Files.createDirectories(runDir.resolve("config"));
            net.minecraftforge.fml.loading.FMLPaths.loadAbsolutePaths(runDir);
            net.minecraft.SharedConstants.tryDetectVersion();
            net.minecraft.server.Bootstrap.bootStrap();
            System.out.println("Bootstrap & Paths initialized successfully.\n");
        } catch (Throwable t) {
            System.out.println("Bootstrap note: " + t.getMessage());
        }

        int passedSuites = 0;
        int totalSuites = 5;

        try {
            // Suite 1: ModItemStackHandler.Dummy
            System.out.println("\n--- [SUITE 1/4] ModItemStackHandler.Dummy Slot Invariants & Stress ---");
            ModItemStackHandlerDummyTest.runAll();
            passedSuites++;

            // Suite 2: MachineRenderRegistry & MachineRenderers
            System.out.println("\n--- [SUITE 2/4] MachineRenderRegistry Declarative Specs & Budget ---");
            MachineRenderRegistryTest.runAll();
            passedSuites++;

            // Suite 3: Challenger Adversarial Stress Suite
            System.out.println("\n--- [SUITE 3/4] Challenger Adversarial Stress Suite (Concurrency, NBT, Edge Cases) ---");
            ChallengerAdversarialStressSuite.runAll();
            passedSuites++;

            // Suite 4: Modular Rockets & Drop Pods VBO Migration
            System.out.println("\n--- [SUITE 4/4] Modular Rockets & Drop Pods VBO Migration (Milestone 3) ---");
            RocketVboMigrationTest.runAll();
            passedSuites++;

            // Suite 5: Mobs & Trinkets VBO Migration
            System.out.println("\n--- [SUITE 5/5] Mobs & Trinkets VBO Migration (Milestone 4) ---");
            MobTrinketVboMigrationTest.runAll();
            passedSuites++;

            long duration = System.currentTimeMillis() - startTime;
            System.out.println("\n================================================================================");
            System.out.printf(">>> ALL %d/%d VERIFICATION SUITES COMPLETED SUCCESSFULLY in %d ms <<<%n",
                    passedSuites, totalSuites, duration);
            System.out.println("Exit code: 0");
            System.out.println("================================================================================");
            System.exit(0);
        } catch (Throwable t) {
            long duration = System.currentTimeMillis() - startTime;
            System.err.println("\n================================================================================");
            System.err.printf(">>> VERIFICATION SUITE FAILED (%d/%d passed) in %d ms <<<%n",
                    passedSuites, totalSuites, duration);
            System.err.println("Failure reason: " + t.getMessage());
            System.err.println("================================================================================");
            t.printStackTrace();
            System.exit(1);
        }
    }
}
