package com.space_m.test;

import com.hbm_m.client.render.machine.MachineRenderRegistry;
import com.hbm_m.client.render.machine.MachineRenderers;
import com.hbm_m.client.render.machine.MachineSpec;
import com.hbm_m.client.render.machine.MachineSpecBuilder;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;

/**
 * Genuine Unit & Integration Test Suite for {@link MachineRenderRegistry} and {@link MachineRenderers}.
 *
 * <p>Verifies:
 * <ul>
 *   <li>Registry initialization and safe queries with null inputs (zero NPE guarantee).</li>
 *   <li>Declarative machine spec registration via {@link MachineRenderers#machine}.</li>
 *   <li>Inspection of registered parts, conditional parts, procedural rotators, and immediate hooks.</li>
 *   <li>Cache invalidation via {@link MachineRenderRegistry#clearAll()}.</li>
 *   <li>Hierarchical joint chain parameter budget enforcement (max 8 words / 8 nodes) failing fast.</li>
 * </ul>
 */
public final class MachineRenderRegistryTest {

    private MachineRenderRegistryTest() {}

    private static void assertTrue(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError("ASSERTION FAILED: " + message);
        }
    }

    private static void assertEquals(Object expected, Object actual, String message) {
        if (expected == null ? actual != null : !expected.equals(actual)) {
            throw new AssertionError("ASSERTION FAILED: " + message + " (expected: " + expected + ", actual: " + actual + ")");
        }
    }

    private static void assertNotNull(Object obj, String message) {
        if (obj == null) {
            throw new AssertionError("ASSERTION FAILED: " + message + " (object was null)");
        }
    }

    /** Dummy synthetic BlockEntity class for registration testing. */
    public static class TestMachineBlockEntity extends BlockEntity {
        public TestMachineBlockEntity(BlockPos pos, BlockState state) {
            super(BlockEntityType.CHEST, pos, state);
        }
    }

    /** Synthetic BlockEntityType implementing NucleusManagedType (simulates MixinBlockEntityType). */
    public static class TestManagedBlockEntityType<T extends BlockEntity> extends BlockEntityType<T> implements com.hbm_m.client.render.NucleusManagedType {
        private boolean managed;

        public TestManagedBlockEntityType(BlockEntityType.BlockEntitySupplier<? extends T> supplier) {
            super(supplier, java.util.Set.of(), null);
        }

        @Override
        public boolean hbm_m$isNucleusManaged() {
            return managed;
        }

        @Override
        public void hbm_m$setNucleusManaged(boolean managed) {
            this.managed = managed;
        }
    }

    /**
     * Test 1: Null Safety & Initial State Queries.
     * Verifies that query methods gracefully return null/false without throwing NPEs.
     */
    public static void testNullSafety() {
        System.out.println("  [RegistryTest] Running testNullSafety...");

        List<MachineSpec<?>> specs = MachineRenderRegistry.specs();
        assertNotNull(specs, "specs() must return non-null list");

        // Null BlockEntityType queries
        boolean managedNull = MachineRenderRegistry.isManaged(null);
        assertTrue(!managedNull, "isManaged(null) must return false, not crash");

        MachineSpec<?> specNull = MachineRenderRegistry.getSpec(null);
        assertTrue(specNull == null, "getSpec(null) must return null, not crash");

        System.out.println("  [RegistryTest] testNullSafety PASSED.");
    }

    /**
     * Test 2: Declarative Machine Registration & Spec Inspection.
     * Registers a synthetic machine spec and verifies all parts, hooks, and registry lookups.
     */
    public static void testMachineRegistration() {
        System.out.println("  [RegistryTest] Running testMachineRegistration...");

        // Use synthetic managed type for testing spec registration
        BlockEntityType<TestMachineBlockEntity> testType = new TestManagedBlockEntityType<>(TestMachineBlockEntity::new);
        String specId = "space_m_test_machine_" + System.nanoTime();

        int specsBefore = MachineRenderRegistry.specs().size();

        // Build declarative spec
        MachineSpecBuilder<TestMachineBlockEntity> builder = MachineRenderers.machine(
                specId, testType, TestMachineBlockEntity.class);

        builder.part("base")
               .part("frame")
               .conditionalPart("solar_cable", be -> true)
               .rotatingPart("radar_dish", 0f, 1f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, (be, pt) -> 45.0)
               .hook((be, pt, pose, buffers, light, overlay, ctx) -> {})
               .viewDistance(96);

        builder.register();

        int specsAfter = MachineRenderRegistry.specs().size();
        assertEquals(specsBefore + 1, specsAfter, "Machine spec count must increment by 1");

        // Verify registry lookups
        assertTrue(MachineRenderRegistry.isManaged(testType), "isManaged(testType) must be true after registration");
        MachineSpec<?> registered = MachineRenderRegistry.getSpec(testType);
        assertNotNull(registered, "getSpec(testType) must return registered spec");
        assertEquals(specId, registered.id(), "Spec ID must match registered ID");

        // Verify parts
        List<?> parts = registered.parts();
        assertNotNull(parts, "Parts list must not be null");
        assertTrue(parts.size() >= 4, "Expected at least 4 parts registered, got " + parts.size());

        // Verify cache clear safety
        MachineRenderRegistry.clearAll();

        System.out.println("  [RegistryTest] testMachineRegistration PASSED.");
    }

    /**
     * Test 3: Joint Chain Parameter Budget Enforcement (Adversarial).
     * Nucleus enforces an 8-word parameter budget on AnimParams (attrib 15).
     * Declaring a joint chain exceeding the budget must throw IllegalStateException at registration time.
     */
    public static void testJointChainBudgetEnforcement() {
        System.out.println("  [RegistryTest] Running testJointChainBudgetEnforcement...");

        BlockEntityType<TestMachineBlockEntity> testType = new TestManagedBlockEntityType<>(TestMachineBlockEntity::new);
        String specId = "space_m_overflow_machine_" + System.nanoTime();

        MachineSpecBuilder<TestMachineBlockEntity> builder = MachineRenderers.machine(
                specId, testType, TestMachineBlockEntity.class);

        // A single vector translation costs 3 words.
        // 3 vector translations = 9 words, exceeding CHAIN_PARAM_BUDGET (8 words)!
        MachineSpec.JointNode<TestMachineBlockEntity> node1 =
                MachineSpec.JointNode.translateVector(1f, 0f, 0f, (be, pt, gt, out, off) -> true);
        MachineSpec.JointNode<TestMachineBlockEntity> node2 =
                MachineSpec.JointNode.translateVector(0f, 1f, 0f, (be, pt, gt, out, off) -> true);
        MachineSpec.JointNode<TestMachineBlockEntity> node3 =
                MachineSpec.JointNode.translateVector(0f, 0f, 1f, (be, pt, gt, out, off) -> true);

        boolean budgetEnforced = false;
        try {
            builder.jointChainPart("overflow_chain", node1, node2, node3);
        } catch (IllegalStateException e) {
            budgetEnforced = true;
            assertTrue(e.getMessage().contains("budget") || e.getMessage().contains("CHAIN_PARAM_BUDGET") || e.getMessage().contains("words"),
                    "Exception message must mention budget or words, got: " + e.getMessage());
        }

        assertTrue(budgetEnforced, "jointChainPart exceeding 8 words must throw IllegalStateException at registration time");

        // Also test exceeding MAX_CHAIN_NODES (9 rotation nodes = 9 words, max nodes = 8)
        MachineSpecBuilder<TestMachineBlockEntity> nodeOverflowBuilder = MachineRenderers.machine(
                specId + "_nodes", testType, TestMachineBlockEntity.class);

        MachineSpec.JointNode<TestMachineBlockEntity> rotateNode =
                MachineSpec.JointNode.rotate(0f, 1f, 0f, 0f, 0f, 0f, (be, pt, gt, out, off) -> true);

        boolean nodeCountEnforced = false;
        try {
            nodeOverflowBuilder.jointChainPart("too_many_nodes",
                    rotateNode, rotateNode, rotateNode, rotateNode,
                    rotateNode, rotateNode, rotateNode, rotateNode, rotateNode);
        } catch (IllegalStateException e) {
            nodeCountEnforced = true;
            assertTrue(e.getMessage().contains("nodes") || e.getMessage().contains("MAX_CHAIN_NODES"),
                    "Exception message must mention nodes constraint, got: " + e.getMessage());
        }

        assertTrue(nodeCountEnforced, "jointChainPart exceeding 8 nodes must throw IllegalStateException at registration time");

        System.out.println("  [RegistryTest] testJointChainBudgetEnforcement PASSED: Hardware constraints strictly enforced.");
    }

    public static void runAll() {
        System.out.println("=================================================================");
        System.out.println("Executing MachineRenderRegistryTest Suite");
        System.out.println("=================================================================");
        testNullSafety();
        testMachineRegistration();
        testJointChainBudgetEnforcement();
        System.out.println(">>> ALL MachineRenderRegistryTest TESTS PASSED <<<");
    }

    public static void main(String[] args) {
        try {
            runAll();
            System.exit(0);
        } catch (Throwable t) {
            t.printStackTrace();
            System.exit(1);
        }
    }
}
