package com.space_m.test;

import com.hbm_m.platform.ModItemStackHandler;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * Genuine Unit & Adversarial Test Suite for {@link ModItemStackHandler.Dummy}.
 *
 * <p>Verifies:
 * <ul>
 *   <li>Slot handler initialization across varied capacities (0, 1, 16, 64, 1000).</li>
 *   <li>Slot invariant: {@code getStackInSlot(i)} returns non-null {@link ItemStack#EMPTY}, never {@code null}.</li>
 *   <li>Insertion and extraction transactions (both simulated and actual) operate without NPEs.</li>
 *   <li>{@code setStackInSlot} safely invokes {@code onContentsChanged} (which is a no-op dummy).</li>
 *   <li>NBT serialization & deserialization round-trip integrity without null pointer exceptions.</li>
 *   <li>Adversarial boundary stress: negative slot indices, out-of-bounds indices, empty stacks, rapid allocations.</li>
 * </ul>
 */
public final class ModItemStackHandlerDummyTest {

    private ModItemStackHandlerDummyTest() {}

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

    /**
     * Test 1: Capacity & Slot Invariant Validation.
     * Verifies that for any requested size, slots count matches and slots contain non-null ItemStack.EMPTY.
     */
    public static void testSlotInvariants() {
        System.out.println("  [DummyTest] Running testSlotInvariants...");
        int[] capacities = {0, 1, 2, 4, 16, 32, 64, 128, 1000};

        for (int cap : capacities) {
            ModItemStackHandler.Dummy dummy = new ModItemStackHandler.Dummy(cap);
            assertEquals(cap, dummy.getSlots(), "Dummy handler slots count must match requested capacity " + cap);

            for (int i = 0; i < cap; i++) {
                ItemStack stack = dummy.getStackInSlot(i);
                assertNotNull(stack, "getStackInSlot(" + i + ") must never return null for capacity " + cap);
                assertTrue(stack.isEmpty(), "getStackInSlot(" + i + ") must return empty stack initially");
                assertEquals(ItemStack.EMPTY, stack, "getStackInSlot(" + i + ") must equal ItemStack.EMPTY");

                int limit = dummy.getSlotLimit(i);
                assertTrue(limit > 0, "Slot limit must be positive, got " + limit);
                assertTrue(dummy.isItemValid(i, ItemStack.EMPTY), "isItemValid must return true for EMPTY stack");
            }
        }
        System.out.println("  [DummyTest] testSlotInvariants PASSED.");
    }

    /**
     * Test 2: Simulated & Actual Transaction Safety.
     * Dummy handler must handle insertions and extractions without null pointers or unexpected state mutations.
     */
    public static void testTransactionSafety() {
        System.out.println("  [DummyTest] Running testTransactionSafety...");
        ModItemStackHandler.Dummy dummy = new ModItemStackHandler.Dummy(8);

        // Simulated insertion of empty stack
        ItemStack remEmpty = dummy.insertItem(0, ItemStack.EMPTY, true);
        assertNotNull(remEmpty, "insertItem(EMPTY, simulate=true) must return non-null");
        assertTrue(remEmpty.isEmpty(), "insertItem(EMPTY) remainder must be empty");

        // Actual insertion of empty stack
        ItemStack remEmptyActual = dummy.insertItem(0, ItemStack.EMPTY, false);
        assertNotNull(remEmptyActual, "insertItem(EMPTY, simulate=false) must return non-null");
        assertTrue(remEmptyActual.isEmpty(), "insertItem(EMPTY) remainder must be empty");

        // Extraction from empty slot
        ItemStack extSim = dummy.extractItem(0, 10, true);
        assertNotNull(extSim, "extractItem(simulate=true) must return non-null");
        assertTrue(extSim.isEmpty(), "extractItem from empty must return empty stack");

        ItemStack extAct = dummy.extractItem(0, 10, false);
        assertNotNull(extAct, "extractItem(simulate=false) must return non-null");
        assertTrue(extAct.isEmpty(), "extractItem from empty must return empty stack");

        // Overwrite via setStackInSlot
        dummy.setStackInSlot(0, ItemStack.EMPTY);
        assertTrue(dummy.getStackInSlot(0).isEmpty(), "Slot must remain empty after setStackInSlot(EMPTY)");

        System.out.println("  [DummyTest] testTransactionSafety PASSED.");
    }

    /**
     * Test 3: Serialization Round-Trip.
     * Verifies that ModItemStackHandler.Dummy serializes to and deserializes from CompoundTag cleanly.
     */
    public static void testSerialization() {
        System.out.println("  [DummyTest] Running testSerialization...");
        ModItemStackHandler.Dummy dummy = new ModItemStackHandler.Dummy(6);

        CompoundTag tag = dummy.serializeNBT();
        assertNotNull(tag, "serializeNBT must produce non-null CompoundTag");
        assertTrue(tag.contains("Size"), "Serialized tag must contain Size key");
        assertEquals(6, tag.getInt("Size"), "Serialized Size must match slot count");

        // Deserialize back into new dummy
        ModItemStackHandler.Dummy restored = new ModItemStackHandler.Dummy(6);
        restored.deserializeNBT(tag);
        assertEquals(6, restored.getSlots(), "Restored handler must maintain slot count");

        for (int i = 0; i < 6; i++) {
            assertNotNull(restored.getStackInSlot(i), "Restored slot must not be null");
            assertTrue(restored.getStackInSlot(i).isEmpty(), "Restored slot must be empty");
        }

        System.out.println("  [DummyTest] testSerialization PASSED.");
    }

    /**
     * Test 4: Adversarial Out-of-Bounds & Negative Index Handling.
     * Tests resilience against negative slot indices and oversized slot indices.
     */
    public static void testAdversarialBoundaries() {
        System.out.println("  [DummyTest] Running testAdversarialBoundaries...");
        ModItemStackHandler.Dummy dummy = new ModItemStackHandler.Dummy(4);

        // Negative slot access must be caught cleanly
        boolean threwNegative = false;
        try {
            dummy.getStackInSlot(-1);
        } catch (RuntimeException e) {
            threwNegative = true;
        }
        assertTrue(threwNegative, "getStackInSlot(-1) must throw RuntimeException (IndexOutOfBoundsException)");

        // Out-of-bounds slot access
        boolean threwOob = false;
        try {
            dummy.getStackInSlot(4);
        } catch (RuntimeException e) {
            threwOob = true;
        }
        assertTrue(threwOob, "getStackInSlot(4) on 4-slot handler must throw RuntimeException");

        // Empty stack early exit invariant (Forge contract: empty stack returns EMPTY without throw)
        ItemStack emptyResult = dummy.insertItem(-1, ItemStack.EMPTY, true);
        assertEquals(ItemStack.EMPTY, emptyResult, "insertItem with empty stack returns EMPTY without throw");

        // Non-empty stack into negative slot must validate and throw RuntimeException
        boolean threwNegativeInsert = false;
        try {
            dummy.insertItem(-1, new ItemStack(Items.IRON_INGOT), true);
        } catch (RuntimeException e) {
            threwNegativeInsert = true;
        }
        assertTrue(threwNegativeInsert, "insertItem(-1, non-empty) must throw RuntimeException");

        // Non-empty stack into oversized slot must validate and throw RuntimeException
        boolean threwOobInsert = false;
        try {
            dummy.insertItem(100, new ItemStack(Items.IRON_INGOT), true);
        } catch (RuntimeException e) {
            threwOobInsert = true;
        }
        assertTrue(threwOobInsert, "insertItem(100, non-empty) on 4-slot handler must throw RuntimeException");

        // Non-zero extraction from invalid slot must throw RuntimeException
        boolean threwNegativeExtract = false;
        try {
            dummy.extractItem(-1, 1, true);
        } catch (RuntimeException e) {
            threwNegativeExtract = true;
        }
        assertTrue(threwNegativeExtract, "extractItem(-1, 1) must throw RuntimeException");

        // Zero-capacity edge case
        ModItemStackHandler.Dummy zeroDummy = new ModItemStackHandler.Dummy(0);
        assertEquals(0, zeroDummy.getSlots(), "Zero dummy must report 0 slots");
        boolean threwZeroAccess = false;
        try {
            zeroDummy.getStackInSlot(0);
        } catch (RuntimeException e) {
            threwZeroAccess = true;
        }
        assertTrue(threwZeroAccess, "getStackInSlot(0) on zero dummy must throw RuntimeException");

        System.out.println("  [DummyTest] testAdversarialBoundaries PASSED.");
    }

    /**
     * Test 5: Rapid Stress Allocation Invariant.
     * Allocates 10,000 dummy handlers and validates slot consistency under rapid churn.
     */
    public static void testStressChurn() {
        System.out.println("  [DummyTest] Running testStressChurn (10,000 allocations)...");
        int count = 10_000;
        long start = System.nanoTime();

        for (int i = 0; i < count; i++) {
            int size = (i % 32) + 1;
            ModItemStackHandler.Dummy d = new ModItemStackHandler.Dummy(size);
            if (d.getSlots() != size || d.getStackInSlot(0) == null) {
                throw new AssertionError("Churn failed at iteration " + i);
            }
        }

        long elapsedMs = (System.nanoTime() - start) / 1_000_000;
        System.out.printf("  [DummyTest] testStressChurn PASSED: 10,000 handlers allocated and verified in %d ms.%n", elapsedMs);
    }

    public static void runAll() {
        System.out.println("=================================================================");
        System.out.println("Executing ModItemStackHandlerDummyTest Suite");
        System.out.println("=================================================================");
        testSlotInvariants();
        testTransactionSafety();
        testSerialization();
        testAdversarialBoundaries();
        testStressChurn();
        System.out.println(">>> ALL ModItemStackHandlerDummyTest TESTS PASSED <<<");
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
