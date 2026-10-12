package com.space_m.rocket.transport;

import java.util.List;

import com.hbm_m.block.machines.DummyableMachineBlock;
import com.hbm_m.inventory.fluid.ModFluids;
import com.space_m.SpaceMod;
import com.space_m.block.SpaceBlocks;
import com.space_m.celestial.CelestialBody;
import com.space_m.celestial.SolarSystem;
import com.space_m.fluid.trait.FT_Rocket;
import com.space_m.item.SpaceItems;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestAssertException;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Pig;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
//? if forge {
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
//?} elif neoforge {
/*import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
*///?}

/**
 * R5-C: Transporter-Rampe (Struktur, Senden/Treibstoff/Schwelle, Raketenwechsel), Verknuepfung ueber Dimensionen mit
 * Chunk-Zwang, Linker-NBT/-Paket, Kampfkapsel (Fall, Landeblock, Nutzlast nach 40 Ticks).
 */
@GameTestHolder(SpaceMod.MOD_ID)
@PrefixGameTestTemplate(false)
public final class TransportGameTest {

    private TransportGameTest() {}

    private static void check(boolean cond, String msg) {
        if (!cond) throw new GameTestAssertException(msg);
    }

    private static TransporterRocketBlockEntity free(GameTestHelper helper, BlockPos rel) {
        BlockPos pos = helper.absolutePos(rel);
        BlockState state = SpaceBlocks.TRANSPORTER_ROCKET.get().defaultBlockState().setValue(DummyableMachineBlock.FACING, Direction.NORTH);
        TransporterRocketBlockEntity be = new TransporterRocketBlockEntity(pos, state);
        be.setLevel(helper.getLevel());
        return be;
    }

    /** Registrierung, Struktur 18 Zellen (getDimensions + fillSpace), Inventar 22 Plaetze, 10 Tanks. */
    @GameTest(template = "empty3x3x3", batch = "space_transport", timeoutTicks = 20)
    public static void registrationAndStructure(GameTestHelper helper) {
        int cells = SpaceBlocks.TRANSPORTER_ROCKET.get().getStructureHelper().getPartOffsets().size() + 1;
        check(cells == 18, "Rampe 18 Zellen, ist " + cells);
        TransporterRocketBlockEntity be = free(helper, new BlockPos(1, 1, 1));
        check(be.getSlotCount() == 22, "16 + 4 + 2 Plaetze, ist " + be.getSlotCount());
        check(be.tanks.length == 10 && be.tanks[0].getMaxFill() == 128_000 && be.tanks[8].getMaxFill() == 64_000, "Tanks 8x128k + 2x64k");
        check(be.tanks[8].getTankType() == ModFluids.HYDROGEN.getSource() && be.tanks[9].getTankType() == ModFluids.OXYGEN.getSource(), "Treibstoff H2/O2");
        check(be.getThreshold() == 0, "Schwelle 0");
        be.threshold = 10;
        check(be.getThreshold() == 512, "Schwelle 10 -> 512");
        check(new ItemStack(SpaceItems.TRANSPORTER_LINKER.get()).getMaxStackSize() == 1, "Linker Stapel 1");
        helper.succeed();
    }

    /** Senden 1:1: Items/Fluid wandern, Treibstoff nach getCostBetween, Rakete wechselt die Seite; Schwelle blockiert. */
    @GameTest(template = "empty3x3x3", batch = "space_transport", timeoutTicks = 20)
    public static void sendCargo(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        TransporterRocketBlockEntity a = free(helper, new BlockPos(0, 1, 0));
        TransporterRocketBlockEntity b = free(helper, new BlockPos(2, 1, 2));
        a.linkedTransporter = b;
        b.linkedTransporter = a;

        a.hasRocket = true;
        a.launchTicks = -20;
        b.hasRocket = false;
        b.launchTicks = 100;

        a.setStack(0, new ItemStack(Items.DIRT, 10));
        a.tanks[0].setTankType(ModFluids.WATER.getSource());
        a.tanks[0].setFill(5000);
        a.tanks[8].setFill(64_000);
        a.tanks[9].setFill(64_000);

        // Schwelle 5 -> 16 > Masse 15: nichts
        a.threshold = 5;
        a.serverTick(level);
        check(a.getStack(0).getCount() == 10 && b.getStack(8).isEmpty(), "Schwelle 16 blockiert Masse 15");

        a.threshold = 0;
        a.launchTicks = -20;
        FT_Rocket fuel = com.hbm_m.inventory.fluid.FluidType.getTrait(ModFluids.HYDROGEN.getSource(), FT_Rocket.class);
        check(fuel != null, "Wasserstoff hat FT_Rocket");
        CelestialBody.Target t = CelestialBody.getTarget(level, 0, 0);
        int cost = Math.min(64_000, SolarSystem.getCostBetween(t.body, t.body, 15 * 100, (int) fuel.getThrust(), fuel.getISP(), t.inOrbit, t.inOrbit));

        a.serverTick(level);
        check(a.getStack(0).isEmpty(), "Items gesendet");
        check(b.getStack(8).is(Items.DIRT) && b.getStack(8).getCount() == 10, "10 Erde in der Ausgabe des Empfaengers");
        check(b.tanks[4].getTankType() == ModFluids.WATER.getSource() && b.tanks[4].getFill() == 5000, "5000 mB Wasser empfangen, ist " + b.tanks[4].getFill());
        check(a.tanks[0].getFill() == 0, "Eingabetank leer");
        check(a.tanks[8].getFill() == 64_000 - cost && a.tanks[9].getFill() == 64_000 - cost,
                "Treibstoff -" + cost + ", ist " + a.tanks[8].getFill() + "/" + a.tanks[9].getFill());
        check(!a.hasRocket && b.hasRocket, "Rakete wechselt zum Empfaenger");

        // ohne Rakete kein zweites Senden
        a.setStack(1, new ItemStack(Items.STONE, 1));
        a.serverTick(level);
        check(a.getStack(1).getCount() == 1, "ohne Rakete kein Senden");

        TransporterChunkLoader.unforceChunk(level, a.getBlockPos());
        helper.succeed();
    }

    /** Verknuepfung Nether <-> End ueber NBTControl "linkedTo" (beidseitig), Chunk-Zwang, Abbau loest beide. */
    @GameTest(template = "empty3x3x3", batch = "space_transport", timeoutTicks = 60)
    public static void crossDimensionLink(GameTestHelper helper) {
        ServerLevel nether = helper.getLevel().getServer().getLevel(Level.NETHER);
        ServerLevel end = helper.getLevel().getServer().getLevel(Level.END);
        check(nether != null && end != null, "Nether/End geladen");

        BlockPos pa = new BlockPos(20_000, 80, 20_000);
        BlockPos pb = new BlockPos(20_000, 80, -20_000);
        BlockState state = SpaceBlocks.TRANSPORTER_ROCKET.get().defaultBlockState().setValue(DummyableMachineBlock.FACING, Direction.NORTH);
        nether.setBlockAndUpdate(pa, state);
        end.setBlockAndUpdate(pb, state);

        check(nether.getBlockEntity(pa) instanceof TransporterRocketBlockEntity, "Rampe im Nether");
        check(end.getBlockEntity(pb) instanceof TransporterRocketBlockEntity, "Rampe im End");
        TransporterRocketBlockEntity a = (TransporterRocketBlockEntity) nether.getBlockEntity(pa);
        TransporterRocketBlockEntity b = (TransporterRocketBlockEntity) end.getBlockEntity(pb);

        // Linker speichert beide
        ItemStack linker = new ItemStack(SpaceItems.TRANSPORTER_LINKER.get());
        ItemTransporterLinker.addTransporter(linker, nether, a);
        ItemTransporterLinker.addTransporter(linker, end, b);
        check(ItemTransporterLinker.saved(linker, helper.getLevel().getServer()).size() == 2, "Linker kennt 2 Rampen");

        CompoundTag data = new CompoundTag();
        data.putString("dimension", end.dimension().location().toString());
        data.putIntArray("linkedTo", new int[] { pb.getX(), pb.getY(), pb.getZ() });
        a.receiveControl(data);

        check(a.getLinkedTransporterEntity() == b, "Nether-Rampe haelt End-Rampe");
        check(b.getLinkedTransporter() != null && b.getLinkedTransporter().dimension.equals(Level.NETHER.location())
                && b.getLinkedTransporter().pos().equals(pa), "End-Rampe kennt die Nether-Rampe");
        check(a.hasRocket && !b.hasRocket, "hasConnected: Rakete beim Verknuepfenden");

        // Chunk-Zwang (Original ChunkLoaderManager) ueber den ersten Server-Tick
        a.serverTick(nether);
        check(TransporterChunkLoader.isForcedBy(nether, pa) && nether.getForcedChunks().contains(new net.minecraft.world.level.ChunkPos(pa).toLong()),
                "Nether-Chunk erzwungen");

        // Abbau: loest beide und gibt den Chunk frei
        nether.setBlockAndUpdate(pa, Blocks.AIR.defaultBlockState());
        check(b.getLinkedTransporter() == null, "Abbau loest die Gegenseite");
        check(!nether.getForcedChunks().contains(new net.minecraft.world.level.ChunkPos(pa).toLong()), "Chunk freigegeben");
        end.setBlockAndUpdate(pb, Blocks.AIR.defaultBlockState());
        helper.succeed();
    }

    /** Linker-Paket: NBT-Rundweg inkl. Partner. */
    @GameTest(template = "empty3x3x3", batch = "space_transport", timeoutTicks = 20)
    public static void linkerPacket(GameTestHelper helper) {
        TransporterInfo a = new TransporterInfo("Alpha", Level.OVERWORLD.location(), 1, 2, 3);
        TransporterInfo b = new TransporterInfo("Beta", Level.NETHER.location(), -4, 5, -6);
        a.linkedTo = b;
        List<TransporterInfo> back = TransporterLinkerPacket.of(List.of(a, b)).read();
        check(back.size() == 2 && back.get(0).equals(a) && back.get(0).name.equals("Alpha"), "Rundweg Alpha");
        check(back.get(0).linkedTo != null && back.get(0).linkedTo.equals(b), "Partner mitgeschickt");
        check(!a.equals(b), "Vergleich ueber Dimension+Position");
        helper.succeed();
    }

    /** Kampfkapsel: faellt, setzt combat_drop, nach 40 Ticks steigt die Nutzlast (2 Schweine) aus. */
    @GameTest(template = "empty3x3x3", batch = "space_transport", timeoutTicks = 140)
    public static void combatDropPod(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        for (int x = 0; x < 3; x++) for (int z = 0; z < 3; z++) helper.setBlock(new BlockPos(x, 0, z), Blocks.STONE);

        Pig pig = EntityType.PIG.create(level);
        CompoundTag nbt = new CompoundTag();
        pig.saveWithoutId(nbt);
        nbt.putString("id", "minecraft:pig");

        // Original prueft den Block ueber der Kapsel ((int)(y+1), bei negativen y zur Null gerundet): Saeule darueber freihalten
        for (int y = 1; y <= 4; y++) helper.setBlock(new BlockPos(1, y, 1), Blocks.AIR);
        BlockPos top = helper.absolutePos(new BlockPos(1, 1, 1));
        EntityCombatDropPod pod = new EntityCombatDropPod(level);
        pod.setPos(top.getX() + 0.5, top.getY() + 0.5, top.getZ() + 0.5);
        pod.setPayload(nbt, 2, 2);
        level.addFreshEntity(pod);

        BlockPos lo = helper.absolutePos(new BlockPos(-1, -2, -1));
        BlockPos hi = helper.absolutePos(new BlockPos(3, 5, 3));
        helper.succeedWhen(() -> {
            boolean found = false;
            for (BlockPos p : BlockPos.betweenClosed(lo, hi)) {
                if (level.getBlockState(p).is(SpaceBlocks.COMBAT_DROP.get())) {
                    found = true;
                    break;
                }
            }
            check(found, "Landeblock combat_drop fehlt");
            check(!pod.isAlive(), "Kapsel-Entity entfernt");
            List<Pig> pigs = level.getEntitiesOfClass(Pig.class, AABB.of(net.minecraft.world.level.levelgen.structure.BoundingBox.fromCorners(lo, hi)).inflate(2));
            check(pigs.size() >= 2, "2 Schweine ausgestiegen, sind " + pigs.size());
            pigs.forEach(Pig::discard);
        });
    }
}
