package com.space_m.test;

import com.hbm_m.client.render.machine.MachineRenderRegistry;
import com.hbm_m.client.render.machine.MachineRenderers;
import com.hbm_m.client.render.machine.MachineSpec;
import com.hbm_m.client.render.machine.MachineSpecBuilder;
import com.hbm_m.inventory.ModItemStackHandlerContainer;
import com.hbm_m.platform.ModItemStackHandler;
import com.hbm_m.platform.StackNbt;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * EMPIRICAL ADVERSARIAL STRESS SUITE (Challenger 1 - Milestone 1)
 *
 * <p>Stress-tests:
 * <ol>
 *   <li>{@link ModItemStackHandler.Dummy}: negative indices, extreme OOB, null vs empty stacks, slot limits, concurrency.</li>
 *   <li>{@link ModItemStackHandlerContainer}: adapter edge cases, clear, boundary operations.</li>
 *   <li>{@link StackNbt}: read/write consistency under empty, missing, or malformed tags, typed helpers, cross-thread consistency.</li>
 *   <li>{@link MachineRenderRegistry}: high churn, duplicate registrations, clearAll lifecycles, null safety, invalid part names.</li>
 *   <li>Memory leak & heap churn under rapid allocation stress.</li>
 * </ol>
 */
public final class ChallengerAdversarialStressSuite {

    private ChallengerAdversarialStressSuite() {}

    private static void assertTrue(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError("[CHALLENGER FAILURE] " + message);
        }
    }

    private static void assertEquals(Object expected, Object actual, String message) {
        if (expected == null ? actual != null : !expected.equals(actual)) {
            throw new AssertionError("[CHALLENGER FAILURE] " + message + " (expected: " + expected + ", got: " + actual + ")");
        }
    }

    private static void assertNotNull(Object obj, String message) {
        if (obj == null) {
            throw new AssertionError("[CHALLENGER FAILURE] " + message + " (was null)");
        }
    }

    // =========================================================================
    // 1. ModItemStackHandler.Dummy Adversarial Boundary Tests
    // =========================================================================

    public static void testDummyNegativeAndExtremeIndices() {
        System.out.println("  [Challenger] 1.1 Testing ModItemStackHandler.Dummy negative & extreme indices...");

        int[] capacities = {0, 1, 5, 26, 64};
        int[] adversarialIndices = {-1, -2, -100, -99999, Integer.MIN_VALUE};

        for (int cap : capacities) {
            ModItemStackHandler.Dummy dummy = new ModItemStackHandler.Dummy(cap);

            for (int idx : adversarialIndices) {
                // 1. getStackInSlot must throw RuntimeException
                boolean caughtGet = false;
                try {
                    dummy.getStackInSlot(idx);
                } catch (RuntimeException e) {
                    caughtGet = true;
                }
                assertTrue(caughtGet, "getStackInSlot(" + idx + ") must throw RuntimeException for cap " + cap);

                // 2. Forge contract: getSlotLimit returns default 64 without bounds check
                int limit = dummy.getSlotLimit(idx);
                assertEquals(64, limit, "getSlotLimit(" + idx + ") returns 64 per Forge contract");

                // 3. setStackInSlot must throw RuntimeException
                boolean caughtSet = false;
                try {
                    dummy.setStackInSlot(idx, ItemStack.EMPTY);
                } catch (RuntimeException e) {
                    caughtSet = true;
                }
                assertTrue(caughtSet, "setStackInSlot(" + idx + ") must throw RuntimeException for cap " + cap);

                // 4. insertItem with non-empty stack must throw RuntimeException
                boolean caughtInsert = false;
                try {
                    dummy.insertItem(idx, new ItemStack(Items.DIAMOND, 1), false);
                } catch (RuntimeException e) {
                    caughtInsert = true;
                }
                assertTrue(caughtInsert, "insertItem(" + idx + ", DIAMOND) must throw RuntimeException for cap " + cap);

                // 5. Forge invariant: insertItem with empty stack returns EMPTY without throw
                ItemStack emptyInsert = dummy.insertItem(idx, ItemStack.EMPTY, false);
                assertTrue(emptyInsert.isEmpty(), "insertItem(" + idx + ", EMPTY) must return empty stack");

                // 6. extractItem with amount > 0 must throw RuntimeException
                boolean caughtExtract = false;
                try {
                    dummy.extractItem(idx, 1, false);
                } catch (RuntimeException e) {
                    caughtExtract = true;
                }
                assertTrue(caughtExtract, "extractItem(" + idx + ", 1) must throw RuntimeException for cap " + cap);

                // 7. Forge invariant: extractItem with amount == 0 returns EMPTY without throw
                ItemStack zeroExtract = dummy.extractItem(idx, 0, false);
                assertTrue(zeroExtract.isEmpty(), "extractItem(" + idx + ", 0) must return empty stack");
            }

            // Extreme positive OOB indices
            int[] oobIndices = {cap, cap + 1, cap + 100, 100_000, Integer.MAX_VALUE};
            for (int idx : oobIndices) {
                boolean caughtGet = false;
                try {
                    dummy.getStackInSlot(idx);
                } catch (RuntimeException e) {
                    caughtGet = true;
                }
                assertTrue(caughtGet, "getStackInSlot(" + idx + ") must throw RuntimeException for cap " + cap);

                boolean caughtSet = false;
                try {
                    dummy.setStackInSlot(idx, ItemStack.EMPTY);
                } catch (RuntimeException e) {
                    caughtSet = true;
                }
                assertTrue(caughtSet, "setStackInSlot(" + idx + ") must throw RuntimeException for cap " + cap);

                boolean caughtInsert = false;
                try {
                    dummy.insertItem(idx, new ItemStack(Items.IRON_INGOT, 1), false);
                } catch (RuntimeException e) {
                    caughtInsert = true;
                }
                assertTrue(caughtInsert, "insertItem(" + idx + ", IRON_INGOT) must throw RuntimeException for cap " + cap);
            }
        }
        System.out.println("  [Challenger] 1.1 Negative & extreme indices PASSED.");
    }

    public static void testDummyNullVsEmptySafety() {
        System.out.println("  [Challenger] 1.2 Testing ModItemStackHandler.Dummy null vs empty safety...");

        ModItemStackHandler.Dummy dummy = new ModItemStackHandler.Dummy(10);

        // Setting ItemStack.EMPTY is safe and maintains invariant
        for (int i = 0; i < 10; i++) {
            dummy.setStackInSlot(i, ItemStack.EMPTY);
            ItemStack s = dummy.getStackInSlot(i);
            assertNotNull(s, "Slot " + i + " must never be null");
            assertTrue(s.isEmpty(), "Slot " + i + " must be empty");
        }

        // Inserting ItemStack.EMPTY (both simulate true and false)
        ItemStack res1 = dummy.insertItem(0, ItemStack.EMPTY, true);
        assertTrue(res1.isEmpty(), "insertItem(EMPTY, true) must return empty stack");
        ItemStack res2 = dummy.insertItem(0, ItemStack.EMPTY, false);
        assertTrue(res2.isEmpty(), "insertItem(EMPTY, false) must return empty stack");

        // Null checks: passing null where ItemStack is expected should throw NullPointerException (fast fail)
        boolean caughtNullInsert = false;
        try {
            dummy.insertItem(0, null, false);
        } catch (NullPointerException e) {
            caughtNullInsert = true;
        }
        assertTrue(caughtNullInsert, "insertItem(0, null) must fail fast with NPE");

        boolean caughtNullSet = false;
        try {
            dummy.setStackInSlot(0, null);
        } catch (NullPointerException | IllegalArgumentException e) {
            caughtNullSet = true;
        }
        assertTrue(caughtNullSet, "setStackInSlot(0, null) must fail fast with NPE/IAE");

        System.out.println("  [Challenger] 1.2 Null vs empty safety PASSED.");
    }

    public static void testDummyModItemStackHandlerContainerAdapter() {
        System.out.println("  [Challenger] 1.3 Testing ModItemStackHandlerContainer adapter invariants...");

        ModItemStackHandler.Dummy dummy = new ModItemStackHandler.Dummy(8);
        AtomicInteger dirtyCalls = new AtomicInteger(0);
        ModItemStackHandlerContainer container = new ModItemStackHandlerContainer(dummy, dirtyCalls::incrementAndGet);

        assertEquals(8, container.getContainerSize(), "Container size must equal dummy slots");
        assertTrue(container.isEmpty(), "Container must initially be empty");

        // Verify stillValid is always true (client dummy)
        assertTrue(container.stillValid(null), "stillValid(null) must return true for client container");

        // Clear content when already empty
        container.clearContent();
        assertTrue(dirtyCalls.get() >= 1, "clearContent must trigger onDirty");
        assertTrue(container.isEmpty(), "Container must remain empty");

        // Adapter OOB boundary checks
        boolean caughtOobGet = false;
        try {
            container.getItem(-1);
        } catch (RuntimeException e) {
            caughtOobGet = true;
        }
        assertTrue(caughtOobGet, "container.getItem(-1) must throw RuntimeException");

        boolean caughtOobSet = false;
        try {
            container.setItem(8, ItemStack.EMPTY);
        } catch (RuntimeException e) {
            caughtOobSet = true;
        }
        assertTrue(caughtOobSet, "container.setItem(8) must throw RuntimeException");

        boolean caughtOobRemove = false;
        try {
            container.removeItem(99, 1);
        } catch (RuntimeException e) {
            caughtOobRemove = true;
        }
        assertTrue(caughtOobRemove, "container.removeItem(99) must throw RuntimeException");

        System.out.println("  [Challenger] 1.3 ModItemStackHandlerContainer adapter PASSED.");
    }

    public static void testDummyConcurrentAccessStress() throws InterruptedException {
        System.out.println("  [Challenger] 1.4 Testing ModItemStackHandler.Dummy multi-threaded concurrency (16 threads, 50,000 ops)...");

        int slots = 32;
        ModItemStackHandler.Dummy dummy = new ModItemStackHandler.Dummy(slots);
        ModItemStackHandlerContainer container = new ModItemStackHandlerContainer(dummy, () -> {});

        int threadCount = 16;
        int opsPerThread = 3125; // 16 * 3125 = 50,000 total ops
        ExecutorService pool = Executors.newFixedThreadPool(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(threadCount);
        AtomicBoolean errorFlag = new AtomicBoolean(false);
        List<Throwable> errors = new java.util.concurrent.CopyOnWriteArrayList<>();

        for (int t = 0; t < threadCount; t++) {
            final int threadId = t;
            pool.submit(() -> {
                try {
                    startLatch.await();
                    Random rnd = new Random(threadId * 7919L);
                    for (int i = 0; i < opsPerThread; i++) {
                        int slot = rnd.nextInt(slots);
                        int action = rnd.nextInt(6);
                        switch (action) {
                            case 0 -> {
                                ItemStack s = dummy.getStackInSlot(slot);
                                if (s == null) throw new AssertionError("Null stack returned from slot " + slot);
                            }
                            case 1 -> {
                                dummy.insertItem(slot, ItemStack.EMPTY, rnd.nextBoolean());
                            }
                            case 2 -> {
                                dummy.extractItem(slot, rnd.nextInt(5), rnd.nextBoolean());
                            }
                            case 3 -> {
                                dummy.setStackInSlot(slot, ItemStack.EMPTY);
                            }
                            case 4 -> {
                                container.getItem(slot);
                            }
                            case 5 -> {
                                dummy.serializeNBT();
                            }
                        }
                    }
                } catch (Throwable ex) {
                    errorFlag.set(true);
                    errors.add(ex);
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        long start = System.nanoTime();
        startLatch.countDown();
        boolean completed = doneLatch.await(10, TimeUnit.SECONDS);
        pool.shutdown();

        assertTrue(completed, "Concurrent stress test timed out after 10s");
        if (errorFlag.get() || !errors.isEmpty()) {
            throw new AssertionError("Concurrent stress encountered exceptions: " + errors.get(0).getMessage());
        }

        long durationMs = (System.nanoTime() - start) / 1_000_000;
        System.out.printf("  [Challenger] 1.4 Concurrency stress PASSED: 50,000 ops across 16 threads in %d ms without errors.%n", durationMs);
    }

    // =========================================================================
    // 2. StackNbt Adversarial Stress Tests
    // =========================================================================

    public static void testStackNbtEmptyAndNullResilience() {
        System.out.println("  [Challenger] 2.1 Testing StackNbt empty & null resilience...");

        // 1. has(EMPTY) must be false
        assertTrue(!StackNbt.has(ItemStack.EMPTY), "StackNbt.has(EMPTY) must be false");

        // 2. read(EMPTY) must be null
        CompoundTag readEmpty = StackNbt.read(ItemStack.EMPTY);
        assertTrue(readEmpty == null, "StackNbt.read(EMPTY) must be null");

        // 3. edit(EMPTY, ...) must be a safe no-op
        AtomicBoolean edited = new AtomicBoolean(false);
        StackNbt.edit(ItemStack.EMPTY, t -> edited.set(true));
        assertTrue(!edited.get(), "StackNbt.edit(EMPTY) must never execute editor consumer");

        // 4. removeKey on EMPTY must not crash
        StackNbt.removeKey(ItemStack.EMPTY, "dummy_key");

        // 5. Typed getters on EMPTY must return clean defaults
        assertEquals(0, StackNbt.getInt(ItemStack.EMPTY, "k"), "getInt(EMPTY) must be 0");
        assertEquals(0L, StackNbt.getLong(ItemStack.EMPTY, "k"), "getLong(EMPTY) must be 0L");
        assertTrue(!StackNbt.getBoolean(ItemStack.EMPTY, "k"), "getBoolean(EMPTY) must be false");
        assertEquals("", StackNbt.getString(ItemStack.EMPTY, "k"), "getString(EMPTY) must be empty string");
        assertNotNull(StackNbt.getCompound(ItemStack.EMPTY, "k"), "getCompound(EMPTY) must be non-null");
        assertTrue(StackNbt.getCompound(ItemStack.EMPTY, "k").isEmpty(), "getCompound(EMPTY) must be empty");
        assertTrue(!StackNbt.contains(ItemStack.EMPTY, "k"), "contains(EMPTY) must be false");

        // 6. fingerprint on EMPTY must be 0L
        assertEquals(0L, StackNbt.fingerprint(ItemStack.EMPTY), "fingerprint(EMPTY) must be 0L");
        assertEquals(0L, StackNbt.fingerprint(null), "fingerprint(null) must be 0L");

        // 7. parse(null) and parse(emptyTag) must return ItemStack.EMPTY
        ItemStack parseNull = StackNbt.parse(null);
        assertTrue(parseNull.isEmpty(), "StackNbt.parse(null) must return ItemStack.EMPTY");
        ItemStack parseEmptyTag = StackNbt.parse(new CompoundTag());
        assertTrue(parseEmptyTag.isEmpty(), "StackNbt.parse(new CompoundTag()) must return ItemStack.EMPTY");

        // 8. save(EMPTY, null) must return new non-null empty tag
        CompoundTag saveNullTag = StackNbt.save(ItemStack.EMPTY, null);
        assertNotNull(saveNullTag, "save(EMPTY, null) must return non-null tag");
        assertTrue(saveNullTag.isEmpty(), "save(EMPTY, null) must return empty tag");

        System.out.println("  [Challenger] 2.1 StackNbt empty & null resilience PASSED.");
    }

    public static void testStackNbtReadWriteConsistency() {
        System.out.println("  [Challenger] 2.2 Testing StackNbt read/write consistency & type conversion...");

        ItemStack stack = new ItemStack(Items.IRON_INGOT, 1);

        // Put typed values
        StackNbt.putInt(stack, "intKey", 42);
        StackNbt.putLong(stack, "longKey", 123456789012345L);
        StackNbt.putBoolean(stack, "boolKey", true);
        StackNbt.putString(stack, "strKey", "SpaceNuclearTech");

        CompoundTag sub = new CompoundTag();
        sub.putString("subField", "NestedValue");
        StackNbt.put(stack, "nested", sub);

        // Read typed values back
        assertEquals(42, StackNbt.getInt(stack, "intKey"), "getInt must return 42");
        assertEquals(123456789012345L, StackNbt.getLong(stack, "longKey"), "getLong must return 123456789012345L");
        assertTrue(StackNbt.getBoolean(stack, "boolKey"), "getBoolean must return true");
        assertEquals("SpaceNuclearTech", StackNbt.getString(stack, "strKey"), "getString must return SpaceNuclearTech");
        assertEquals("NestedValue", StackNbt.getCompound(stack, "nested").getString("subField"), "Nested compound field mismatch");

        // Type mismatch stress: reading String where Int was stored, reading Int where String was stored
        assertEquals(0, StackNbt.getInt(stack, "strKey"), "getInt on String tag must return 0 without exception");
        assertEquals("", StackNbt.getString(stack, "intKey"), "getString on Int tag must return empty or string representation");

        // Remove key and verify
        StackNbt.removeKey(stack, "intKey");
        assertTrue(!StackNbt.contains(stack, "intKey"), "intKey must be removed");
        assertEquals(0, StackNbt.getInt(stack, "intKey"), "getInt after remove must return 0");

        // Verify edit cleanup: if edit leaves tag completely empty, tag must be cleared from stack
        ItemStack cleanupStack = new ItemStack(Items.GOLD_INGOT, 1);
        StackNbt.putInt(cleanupStack, "temp", 1);
        assertTrue(StackNbt.has(cleanupStack), "Stack must have tag");
        StackNbt.edit(cleanupStack, t -> t.remove("temp"));
        assertTrue(!StackNbt.has(cleanupStack), "Stack tag must be removed when left empty by edit");

        System.out.println("  [Challenger] 2.2 StackNbt read/write consistency PASSED.");
    }

    public static void testStackNbtMalformedAndCorruptedNbt() {
        System.out.println("  [Challenger] 2.3 Testing StackNbt resilience against malformed & corrupted NBT...");

        // Malformed 1: ID is invalid item
        CompoundTag badIdTag = new CompoundTag();
        badIdTag.putString("id", "invalid_mod_xyz:imaginary_item_404");
        badIdTag.putByte("Count", (byte) 1);
        ItemStack badIdStack = StackNbt.parse(badIdTag);
        assertTrue(badIdStack.isEmpty(), "Parsing invalid item ID must yield ItemStack.EMPTY without crash");

        // Malformed 2: Missing Count tag
        CompoundTag noCountTag = new CompoundTag();
        noCountTag.putString("id", "minecraft:iron_ingot");
        ItemStack noCountStack = StackNbt.parse(noCountTag);
        // In 1.20.1 vanilla ItemStack.of: missing Count defaults to 1 or EMPTY
        assertNotNull(noCountStack, "Parsing without Count must not crash");

        // Malformed 3: Negative count
        CompoundTag negCountTag = new CompoundTag();
        negCountTag.putString("id", "minecraft:iron_ingot");
        negCountTag.putByte("Count", (byte) -10);
        ItemStack negCountStack = StackNbt.parse(negCountTag);
        assertNotNull(negCountStack, "Parsing negative count must not crash");

        // Malformed 4: Extremely deep nesting (recursive compounds)
        CompoundTag deepRoot = new CompoundTag();
        CompoundTag cur = deepRoot;
        for (int i = 0; i < 50; i++) {
            CompoundTag next = new CompoundTag();
            cur.put("level_" + i, next);
            cur = next;
        }
        cur.putString("payload", "bottom");

        ItemStack deepStack = new ItemStack(Items.COPPER_INGOT, 1);
        StackNbt.put(deepStack, "deep", deepRoot);
        CompoundTag saved = StackNbt.saveNew(deepStack, null);
        ItemStack restored = StackNbt.parse(saved);
        assertNotNull(restored, "Restoring deeply nested stack must succeed");
        CompoundTag t = StackNbt.getCompound(restored, "deep");
        for (int i = 0; i < 50; i++) {
            t = t.getCompound("level_" + i);
        }
        assertEquals("bottom", t.getString("payload"), "Deeply nested compound payload must match");

        System.out.println("  [Challenger] 2.3 StackNbt malformed & corrupted NBT PASSED.");
    }

    public static void testStackNbtEqualityAndFingerprint() {
        System.out.println("  [Challenger] 2.4 Testing StackNbt sameItemSameTags & fingerprint...");

        ItemStack empty1 = ItemStack.EMPTY;
        ItemStack empty2 = ItemStack.EMPTY;
        assertTrue(StackNbt.sameItemSameTags(empty1, empty2), "EMPTY and EMPTY must be same");

        ItemStack ironA = new ItemStack(Items.IRON_INGOT, 2);
        ItemStack ironB = new ItemStack(Items.IRON_INGOT, 5); // Count doesn't matter for sameItemSameTags
        assertTrue(StackNbt.sameItemSameTags(ironA, ironB), "Same item without tags must match");

        StackNbt.putInt(ironA, "charge", 100);
        assertTrue(!StackNbt.sameItemSameTags(ironA, ironB), "Tagged stack must not match untagged stack");

        StackNbt.putInt(ironB, "charge", 100);
        assertTrue(StackNbt.sameItemSameTags(ironA, ironB), "Stacks with identical tags must match");

        StackNbt.putInt(ironB, "charge", 200);
        assertTrue(!StackNbt.sameItemSameTags(ironA, ironB), "Stacks with differing tags must not match");

        // Fingerprint uniqueness check
        long fpA = StackNbt.fingerprint(ironA);
        long fpB = StackNbt.fingerprint(ironB);
        assertTrue(fpA != fpB, "Fingerprint of different tag contents must differ");
        assertTrue(fpA != 0L, "Fingerprint of valid stack must be non-zero");

        System.out.println("  [Challenger] 2.4 StackNbt sameItemSameTags & fingerprint PASSED.");
    }

    // =========================================================================
    // 3. MachineRenderRegistry Adversarial Tests
    // =========================================================================

    public static class SyntheticBlockEntity extends BlockEntity {
        public SyntheticBlockEntity(BlockPos pos, BlockState state) {
            super(BlockEntityType.CHEST, pos, state);
        }
    }

    public static class SyntheticManagedType<T extends BlockEntity> extends BlockEntityType<T> implements com.hbm_m.client.render.NucleusManagedType {
        private boolean managed;

        public SyntheticManagedType(BlockEntityType.BlockEntitySupplier<? extends T> supplier) {
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

    public static void testMachineRenderRegistryChurnAndClearLifecycle() {
        System.out.println("  [Challenger] 3.1 Testing MachineRenderRegistry registration churn & clear lifecycle (500 specs)...");

        int initialSize = MachineRenderRegistry.specs().size();
        int churnCount = 500;

        List<SyntheticManagedType<SyntheticBlockEntity>> registeredTypes = new ArrayList<>();

        for (int i = 0; i < churnCount; i++) {
            final int idx = i;
            SyntheticManagedType<SyntheticBlockEntity> type = new SyntheticManagedType<>(SyntheticBlockEntity::new);
            registeredTypes.add(type);
            String id = "space_m_churn_" + i + "_" + System.nanoTime();

            MachineSpecBuilder<SyntheticBlockEntity> b = MachineRenderers.machine(id, type, SyntheticBlockEntity.class);
            b.part("part_static_" + i)
             .conditionalPart("cond_part_" + i, be -> (idx % 2 == 0))
             .rotatingPart("turntable_" + i, 0f, 1f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, (be, pt) -> (double) idx)
             .hook((be, pt, pose, buffers, light, overlay, ctx) -> {});
            b.register();
        }

        assertEquals(initialSize + churnCount, MachineRenderRegistry.specs().size(),
                "Registry must contain exactly initialSize + churnCount specs");

        // Verify all registered types are queryable
        for (int i = 0; i < churnCount; i++) {
            SyntheticManagedType<SyntheticBlockEntity> type = registeredTypes.get(i);
            assertTrue(MachineRenderRegistry.isManaged(type), "Type " + i + " must be managed");
            MachineSpec<?> spec = MachineRenderRegistry.getSpec(type);
            assertNotNull(spec, "Spec for type " + i + " must be non-null");
            assertTrue(spec.parts().size() >= 3, "Spec for type " + i + " must have >= 3 parts");
        }

        // Test clearAll() - GPU-cache invalidation across all specs
        MachineRenderRegistry.clearAll();
        // Specs remain registered because they are permanent mod registrations; clearAll clears their GPU instance buffers
        assertEquals(initialSize + churnCount, MachineRenderRegistry.specs().size(),
                "Registry specs remain registered after GPU-cache clearAll()");

        // Verify lookups after clearAll() still resolve correctly
        for (SyntheticManagedType<SyntheticBlockEntity> type : registeredTypes) {
            assertTrue(MachineRenderRegistry.isManaged(type), "Type must remain managed after GPU clearAll()");
            assertNotNull(MachineRenderRegistry.getSpec(type), "getSpec must resolve after GPU clearAll()");
        }

        System.out.println("  [Challenger] 3.1 MachineRenderRegistry churn & clear lifecycle PASSED.");
    }

    public static void testMachineRenderRegistryAdversarialInputs() {
        System.out.println("  [Challenger] 3.2 Testing MachineRenderRegistry adversarial inputs & edge cases...");

        // 1. Safe null querying
        assertTrue(!MachineRenderRegistry.isManaged(null), "isManaged(null) must be false");
        assertTrue(MachineRenderRegistry.getSpec(null) == null, "getSpec(null) must be null");

        // 2. Odd and boundary part names
        SyntheticManagedType<SyntheticBlockEntity> type = new SyntheticManagedType<>(SyntheticBlockEntity::new);
        String specId = "space_m_edge_spec_" + System.nanoTime();

        MachineSpecBuilder<SyntheticBlockEntity> b = MachineRenderers.machine(specId, type, SyntheticBlockEntity.class);
        b.part("part/with/slashes")
         .part("part.with.dots")
         .part("деталь_юникод_123")
         .part("   part_with_spaces   ")
         .part("") // empty name
         .viewDistance(0)
         .viewDistance(10000);

        b.register();

        MachineSpec<?> spec = MachineRenderRegistry.getSpec(type);
        assertNotNull(spec, "Registered edge spec must be retrieved");
        // Nucleus optimization: plain static parts are automatically merged into 1 combined static part definition
        assertEquals(1, spec.parts().size(), "Nucleus merges plain static parts into 1 combined part");
        assertTrue(spec.parts().get(0).modelPartName().contains("деталь_юникод_123"), "Merged part must contain unicode part");
        assertTrue(spec.parts().get(0).modelPartName().contains("part/with/slashes"), "Merged part must contain slashed part");

        // 3. Duplicate registration of the same BlockEntityType (must overwrite or replace safely without crash)
        MachineSpecBuilder<SyntheticBlockEntity> b2 = MachineRenderers.machine(specId + "_v2", type, SyntheticBlockEntity.class);
        b2.part("replacement_part");
        b2.register();

        MachineSpec<?> spec2 = MachineRenderRegistry.getSpec(type);
        assertNotNull(spec2, "Overwritten spec must be retrieved");
        assertEquals(specId + "_v2", spec2.id(), "Overwritten spec must have new ID");

        MachineRenderRegistry.clearAll();

        System.out.println("  [Challenger] 3.2 MachineRenderRegistry adversarial inputs PASSED.");
    }

    // =========================================================================
    // 4. Memory Leak & Rapid Churn Stress
    // =========================================================================

    public static void testMemoryLeakAndRapidChurn() {
        System.out.println("  [Challenger] 4.1 Testing heap churn & memory leak absence (100,000 Dummy + Container allocations)...");

        // Force GC before baseline
        System.gc();
        try { Thread.sleep(100); } catch (InterruptedException ignored) {}
        long memBefore = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();

        int iterations = 100_000;
        long start = System.nanoTime();

        for (int i = 0; i < iterations; i++) {
            int size = (i % 32) + 1;
            ModItemStackHandler.Dummy dummy = new ModItemStackHandler.Dummy(size);
            ModItemStackHandlerContainer c = new ModItemStackHandlerContainer(dummy, null);
            c.getItem(0);
            c.setItem(0, ItemStack.EMPTY);
        }

        long elapsedMs = (System.nanoTime() - start) / 1_000_000;

        // Force GC after churn
        System.gc();
        try { Thread.sleep(100); } catch (InterruptedException ignored) {}
        long memAfter = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();

        long diffMb = (memAfter - memBefore) / (1024 * 1024);
        System.out.printf("  [Challenger] 4.1 Churn completed: %d iterations in %d ms (heap delta: %d MB).%n",
                iterations, elapsedMs, diffMb);

        // Memory delta must be bounded (< 100 MB)
        assertTrue(diffMb < 100, "Heap retained memory must not leak under rapid churn (delta was " + diffMb + " MB)");

        System.out.println("  [Challenger] 4.1 Memory leak absence verified PASSED.");
    }

    // =========================================================================
    // Runner & Entrypoint
    // =========================================================================

    public static void runAll() throws Exception {
        System.out.println("================================================================================");
        System.out.println("CHALLENGER 1: EMPIRICAL ADVERSARIAL STRESS SUITE (MILESTONE 1)");
        System.out.println("Target: ModItemStackHandler.Dummy, ModItemStackHandlerContainer, StackNbt, Nucleus Registry");
        System.out.println("================================================================================");

        long start = System.currentTimeMillis();

        // 1. ModItemStackHandler.Dummy
        testDummyNegativeAndExtremeIndices();
        testDummyNullVsEmptySafety();
        testDummyModItemStackHandlerContainerAdapter();
        testDummyConcurrentAccessStress();

        // 2. StackNbt
        testStackNbtEmptyAndNullResilience();
        testStackNbtReadWriteConsistency();
        testStackNbtMalformedAndCorruptedNbt();
        testStackNbtEqualityAndFingerprint();

        // 3. MachineRenderRegistry
        testMachineRenderRegistryChurnAndClearLifecycle();
        testMachineRenderRegistryAdversarialInputs();

        // 4. Memory Churn
        testMemoryLeakAndRapidChurn();

        long elapsed = System.currentTimeMillis() - start;
        System.out.println("================================================================================");
        System.out.printf(">>> ALL 10 ADVERSARIAL STRESS TEST CATEGORIES PASSED EMPIRICALLY in %d ms <<<%n", elapsed);
        System.out.println("Exit code: 0");
        System.out.println("================================================================================");
    }

    public static void main(String[] args) {
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

        try {
            runAll();
            System.exit(0);
        } catch (Throwable t) {
            System.err.println("\n>>> ADVERSARIAL STRESS TEST FAILED <<<");
            t.printStackTrace();
            System.exit(1);
        }
    }
}
