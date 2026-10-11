package com.space_m.test;

import com.space_m.SpaceMod;
import com.space_m.config.ConfigEntry;
import com.space_m.config.SpaceConfig;
import com.space_m.config.SpaceConfigStore;
import com.space_m.platform.GravityHooks;
import com.space_m.platform.SavedDataHooks;
import com.space_m.platform.SpaceSavedData;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestAssertException;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Pig;
//? if forge {
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
//?} elif neoforge {
/*import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
*///?}

/**
 * R0-Fundament: prueft Config-Schema/Standardwerte und die {@code platform}-Fassaden auf beiden Loadern.
 */
@GameTestHolder(SpaceMod.MOD_ID)
@PrefixGameTestTemplate(false)
public final class SpaceFoundationGameTest {

    private SpaceFoundationGameTest() {}

    private static void check(boolean cond, String msg) {
        if (!cond) throw new GameTestAssertException(msg);
    }

    /** Standardwerte = Ladewerte des Originals; jeder Schluessel kommt genau einmal vor. */
    @GameTest(template = "empty3x3x3", batch = "space_foundation", timeoutTicks = 20)
    public static void configDefaultsMatchOriginal(GameTestHelper helper) {
        SpaceConfig c = new SpaceConfig();
        check(c.maxProbeDistance == 32_000 && c.maxStationDistance == 32_000, "SpaceConfig probe/station distance 32000");
        check(!c.allowNetherPortals && c.enableVolcanoGen && c.crashOnBiomeConflict && c.showOreLocations && !c.combatPodDespawn,
                "SpaceConfig switches");
        check(c.nickelSpawn == 12 && c.mineralSpawn == 4 && c.bedrockCadmiumSpawn == 400, "WorldConfig load defaults");
        check(c.celestialRadMultiplier == 0.25D && c.disableNeutron && c.enableMunCows, "Radiation/Mob defaults");
        check(c.audioShipHumVolume == 0.1F, "ClientConfig AUDIO_SHIP_HUM_VOLUME");
        java.util.Set<String> keys = new java.util.HashSet<>();
        int n = 0;
        for (boolean client : new boolean[] { false, true }) {
            for (var f : SpaceConfigStore.fields(client)) {
                n++;
                check(keys.add(f.getAnnotation(ConfigEntry.class).key()), "duplicate config key " + f.getName());
            }
        }
        check(n == 61, "61 config entries, found " + n);
        helper.succeed();
    }

    /** GravityHooks: Grundwert 0.08, Faktor wirkt multiplikativ, clear stellt zurueck. */
    @GameTest(template = "empty3x3x3", batch = "space_foundation", timeoutTicks = 20)
    public static void gravityFacade(GameTestHelper helper) {
        Pig pig = helper.spawn(EntityType.PIG, new BlockPos(1, 1, 1));
        double base = GravityHooks.get(pig);
        check(Math.abs(base - GravityHooks.BASE) < 1e-9, "base gravity 0.08, got " + base);
        GravityHooks.setSurfaceGravity(pig, 1.63D); // Mun-Oberflaechenschwere (Beispielwert m/s^2)
        double mun = GravityHooks.get(pig);
        check(Math.abs(mun - GravityHooks.BASE * 1.63D / GravityHooks.STANDARD_GRAVITY) < 1e-9, "scaled gravity, got " + mun);
        GravityHooks.setFactor(pig, 0.0D);
        check(Math.abs(GravityHooks.get(pig)) < 1e-9, "zero gravity in orbit");
        GravityHooks.clear(pig);
        check(Math.abs(GravityHooks.get(pig) - GravityHooks.BASE) < 1e-9, "cleared gravity back to 0.08");
        pig.discard();
        helper.succeed();
    }

    /** SavedDataHooks liefert je Dimension und Name dieselbe Instanz. */
    @GameTest(template = "empty3x3x3", batch = "space_foundation", timeoutTicks = 20)
    public static void savedDataFacade(GameTestHelper helper) {
        TestData a = SavedDataHooks.get(helper.getLevel(), "space_m_gametest", TestData::new, TestData::load);
        TestData b = SavedDataHooks.get(helper.getLevel(), "space_m_gametest", TestData::new, TestData::load);
        check(a == b, "same SavedData instance");
        check(a.write(new CompoundTag()).getInt("v") == 7, "SpaceSavedData.write");
        helper.succeed();
    }

    private static final class TestData extends SpaceSavedData {
        int v = 7;

        static TestData load(CompoundTag tag) {
            TestData d = new TestData();
            d.v = tag.getInt("v");
            return d;
        }

        @Override
        protected CompoundTag write(CompoundTag tag) {
            tag.putInt("v", v);
            return tag;
        }
    }
}
