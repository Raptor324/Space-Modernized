package com.space_m.rocket;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jetbrains.annotations.Nullable;

import com.space_m.celestial.CelestialBody;
import com.space_m.celestial.SolarSystem;
import com.space_m.rocket.RocketPart.FuelType;
import com.space_m.rocket.RocketPart.PartType;
import com.space_m.rocket.RocketPart.WarheadType;

import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.Fluid;

/**
 * Bauplan einer mehrstufigen Rakete, 1:1 NTM Space {@code handler/RocketStruct}: Kapsel/Satellit oben, bis zu
 * {@link #MAX_STAGES} Stufen (Rumpf x n, Landebeine/Leitwerk, Triebwerk x m). Stufe 0 ist die unterste (zuerst zuendende).
 * Massen, Schub, ISP und Hoehen wie im Original; der Treibstoffbedarf kommt aus {@link SolarSystem#getCostBetween}
 * (Hohmann-Transfer + Start-/Landekosten, R1-A).
 *
 * <p>Abweichungen (technisch): Teile sind {@link RocketPart} statt {@code MissilePart}; im NBT stehen Registry-Namen statt
 * numerischer Item-IDs (die in 1.20+ zwischen Spielstaenden nicht stabil sind; alte int-Werte werden noch gelesen).
 * {@code DataWatcher} -> {@link #writeToWatchable()}/{@link #readFromWatchable(int[])} mit derselben Bit-Packung.
 * Hinweise sind Strings mit Formatcodes wie im Original ({@code EnumChatFormatting} -> {@link ChatFormatting}).
 * Original-Klasse: RocketStruct
 */
public class RocketStruct {

    @Nullable public RocketPart capsule;
    public ArrayList<RocketStage> stages = new ArrayList<>();
    public int satFreq = 0;

    public List<String> extraIssues = new ArrayList<>();

    public static final int MAX_STAGES = 5;

    /** Groesse des Entity-Sync-Arrays (Original: 2 + 2 * MAX_STAGES DataWatcher-Eintraege). */
    public static final int WATCHABLE_SIZE = 2 + MAX_STAGES * 2;

    public RocketStruct() {}

    public RocketStruct(@Nullable ItemStack capsule) {
        this.capsule = RocketPart.getPart(capsule);
    }

    public RocketStruct(@Nullable RocketPart capsule) {
        this.capsule = capsule;
    }

    public void addStage(@Nullable ItemStack fuselage, @Nullable ItemStack fins, @Nullable ItemStack thruster) {
        addStage(RocketPart.getPart(fuselage), RocketPart.getPart(fins), RocketPart.getPart(thruster),
                fuselage != null && !fuselage.isEmpty() ? fuselage.getCount() : 1,
                thruster != null && !thruster.isEmpty() ? thruster.getCount() : 1);
    }

    public void addStage(@Nullable RocketPart fuselage, @Nullable RocketPart fins, @Nullable RocketPart thruster, int fuselageCount, int thrusterCount) {
        RocketStage stage = new RocketStage();
        stage.fuselage = fuselage;
        stage.fins = fins;
        stage.thruster = thruster;
        stage.fuselageCount = fuselageCount;
        stage.thrusterCount = thrusterCount;
        stages.add(0, stage);
    }

    public boolean validate() {
        if (extraIssues.size() > 0)
            return false;

        if (capsule == null || capsule.type != PartType.WARHEAD)
            return false;

        if (capsule.warhead != WarheadType.APOLLO && capsule.warhead != WarheadType.SATELLITE)
            return false;

        if (stages.size() == 0)
            return false;

        for (RocketStage stage : stages) {
            if (stage.fuselage == null || stage.fuselage.type != PartType.FUSELAGE) return false;
            if (stage.fins != null && stage.fins.type != PartType.FINS) return false;
            if (stage.thruster == null || stage.thruster.type != PartType.THRUSTER) return false;

            if (stage.thrusterCount > stage.fuselageCount || stage.fuselageCount % stage.thrusterCount != 0) return false;

            if (stage.fuselage.fuel != FuelType.ANY && stage.fuselage.fuel != stage.thruster.fuel) return false;
        }

        return true;
    }

    public void addIssue(String issue) {
        extraIssues.add(issue);
    }

    // Lists any validation issues so the player can rectify easily
    public List<String> findIssues(int stageNum, @Nullable CelestialBody from, @Nullable CelestialBody to, boolean fromOrbit, boolean toOrbit) {
        List<String> issues = new ArrayList<>();

        // If we have no parts, we have no worries
        if (capsule == null && stages.size() == 0) return issues;

        if (capsule == null || (capsule.warhead != WarheadType.APOLLO && capsule.warhead != WarheadType.SATELLITE))
            issues.add(ChatFormatting.RED + "Invalid Capsule/Satellite");

        // Current stage stats
        if (stageNum < stages.size()) {
            RocketStage stage = stages.get(stageNum);
            issues.add("Dry mass: " + getLaunchMass(stageNum) + "kg");
            issues.add("Wet mass: " + getWetMass(stageNum) + "kg");
            if (stage.thruster != null) {
                issues.add("Thrust: " + getThrust(stage) + "N");
                issues.add("ISP: " + getISP(stage) + "s");
            }
        }

        for (int i = 0; i < stages.size(); i++) {
            RocketStage stage = stages.get(i);
            if (stage.fuselage == null)
                issues.add(ChatFormatting.RED + "Stage " + (i + 1) + " missing fuselage");
            if (stage.thruster == null)
                issues.add(ChatFormatting.RED + "Stage " + (i + 1) + " missing thruster");

            if (stage.fuselage == null || stage.thruster == null)
                continue;

            if (stage.thrusterCount > stage.fuselageCount)
                issues.add(ChatFormatting.RED + "Stage " + (i + 1) + " too many thrusters");
            if (stage.fuselageCount % stage.thrusterCount != 0)
                issues.add(ChatFormatting.RED + "Stage " + (i + 1) + " uneven thrusters");

            if (stage.fuselage.fuel != FuelType.ANY && stage.fuselage.fuel != stage.thruster.fuel)
                issues.add(ChatFormatting.RED + "Stage " + (i + 1) + " fuel mismatch");

            if (i > 0 && stage.fins == null)
                issues.add(ChatFormatting.YELLOW + "Stage " + (i + 1) + " lacks landing legs");

            // I was gonna add all sorts of realistic restrictions but then realised
            // KSP lets you shit any part onto any part, and that's fun
            // so who am I to kill your creative spirit
            // put that ant engine on your rhino fuselage
        }

        if (from != null && to != null) {
            int fuelRequirement = getFuelRequired(stageNum, from, to, fromOrbit, toOrbit);
            int fuelCapacity = getFuelCapacity(stageNum);

            if (fuelRequirement == Integer.MAX_VALUE) {
                issues.add(ChatFormatting.YELLOW + "Insufficient thrust");
            } else if (fuelCapacity < fuelRequirement) {
                issues.add(ChatFormatting.YELLOW + "Insufficient fuel: " + fuelCapacity + "/" + fuelRequirement + "mB");
            } else if (fuelCapacity > 0 && fuelRequirement > 0) {
                issues.add(ChatFormatting.GREEN + "Trip possible! " + fuelCapacity + "/" + fuelRequirement + "mB");
            }
        }

        issues.addAll(extraIssues);

        return issues;
    }

    // NONE fluid is solid fuel  (hier: Fluids.EMPTY)
    public Map<Fluid, Integer> getFillRequirement() {
        Map<Fluid, Integer> tanks = new HashMap<>();

        for (RocketStage stage : stages) {
            if (stage.thruster == null || stage.fuselage == null) continue;

            Fluid fuel = stage.thruster.getFuel();
            Fluid oxidizer = stage.thruster.getOxidizer();

            if (fuel != null) {
                int amount = stage.fuselage.getTankSize() * stage.fuselageCount;
                if (tanks.containsKey(fuel)) amount += tanks.get(fuel);
                tanks.put(fuel, amount);
            }

            if (oxidizer != null) {
                int amount = stage.fuselage.getTankSize() * stage.fuselageCount;
                if (tanks.containsKey(oxidizer)) amount += tanks.get(oxidizer);
                tanks.put(oxidizer, amount);
            }
        }

        return tanks;
    }

    public boolean hasSufficientFuel(CelestialBody from, CelestialBody to, boolean fromOrbit, boolean toOrbit) {
        if (capsule != null && capsule.part != null && capsule.part == SpaceRocketItems.RP_POD_20.get()) {
            return from == to && (fromOrbit || toOrbit); // Pods can transfer, fall to orbited body, and return to station, but NOT hop on the surface
        }

        if (stages.size() == 0) {
            return from == to && fromOrbit && !toOrbit; // Capsules can return to orbited body from orbit only
        }

        int fuelRequirement = getFuelRequired(0, from, to, fromOrbit, toOrbit);
        int fuelCapacity = getFuelCapacity(0);

        return fuelCapacity >= fuelRequirement;
    }

    public int getFuelCapacity(int stageNum) {
        if (stageNum >= stages.size()) return -1;

        RocketStage stage = stages.get(stageNum);

        if (stage.fuselage == null) return -1;

        return stage.fuselage.getTankSize() * stage.fuselageCount;
    }

    public int getFuelRequired(int stageNum, CelestialBody from, CelestialBody to, boolean fromOrbit, boolean toOrbit) {
        if (stageNum >= stages.size()) return -1;

        RocketStage stage = stages.get(stageNum);

        if (stage.fuselage == null || stage.thruster == null) return -1;

        int rocketMass = getLaunchMass(stageNum);
        int thrust = getThrust(stage);
        int isp = getISP(stage);

        return SolarSystem.getCostBetween(from, to, rocketMass, thrust, isp, fromOrbit, toOrbit);
    }

    public int getThrust() {
        return getThrust(stages.get(0));
    }

    private int getThrust(RocketStage stage) {
        return stage.thruster.getThrust() * stage.thrusterCount;
    }

    private int getISP(RocketStage stage) {
        return stage.thruster.getISP();
    }

    // Gets the dry mass of the active stage + the wet mass of the stages above it
    public int getLaunchMass() {
        return getMass(0, false);
    }

    // Gets the dry mass of the selected stage + the wet mass of the stages above it
    public int getLaunchMass(int stageNum) {
        return getMass(stageNum, false);
    }

    public int getWetMass(int stageNum) {
        return getMass(stageNum, true);
    }

    private int getMass(int stageNum, boolean wet) {
        int mass = 0;

        if (capsule != null) mass += capsule.mass;

        for (int i = stageNum; i < stages.size(); i++) {
            RocketStage stage = stages.get(i);
            if (stage.fuselage != null) mass += stage.fuselage.mass * stage.fuselageCount;
            if (stage.thruster != null) mass += stage.thruster.mass * stage.thrusterCount;

            if (stage.fuselage != null && (i > stageNum || wet)) {
                mass += stage.fuselage.getTankSize() * stage.fuselageCount / 4; // Reduced fuel weight to improve multi-stages
            }
        }

        return Mth.ceil((float) mass);
    }

    public double getHeight() {
        double height = 0;

        if (capsule != null) height += capsule.height;

        boolean isDeployed = true;

        for (RocketStage stage : stages) {
            if (stage.fuselage != null) height += stage.fuselage.height * stage.getStack();
            height += Math.max(stage.thruster != null ? stage.thruster.height : 0, isDeployed && stage.fins != null ? stage.fins.height : 0);
            isDeployed = false;
        }

        return height;
    }

    public double getHeight(int stageNum) {
        double height = 0;

        if (stages.size() > 0) {
            RocketStage stage = stages.get(Math.min(stageNum, stages.size() - 1));
            if (stage.fuselage != null) height += stage.fuselage.height * stage.getStack();
            height += Math.max(stage.thruster != null ? stage.thruster.height : 0, stageNum == 0 && stage.fins != null ? stage.fins.height : 0);
        }

        if (stages.size() == 0 || stageNum == stages.size() - 1) {
            if (capsule != null) height += capsule.height;
        }

        return height;
    }

    public double getOffset(int stageNum) {
        double height = 0;

        for (int i = 0; i < Math.min(stageNum, stages.size() - 1); i++) {
            RocketStage stage = stages.get(i);
            if (stage.fuselage != null) height += stage.fuselage.height * stage.getStack();
            height += Math.max(stage.thruster != null ? stage.thruster.height : 0, i == 0 && stage.fins != null ? stage.fins.height : 0);
        }

        return height;
    }

    // ── Netzwerk (Original ByteBuf, numerische IDs) ──────────────────────────────────────────

    public void writeToByteBuffer(FriendlyByteBuf buf) {
        buf.writeInt(RocketPart.getId(capsule));

        buf.writeInt(stages.size());
        for (RocketStage stage : stages) {
            buf.writeInt(RocketPart.getId(stage.fuselage));
            buf.writeInt(RocketPart.getId(stage.fins));
            buf.writeInt(RocketPart.getId(stage.thruster));
            buf.writeByte(stage.fuselageCount);
            buf.writeByte(stage.thrusterCount);
        }

        buf.writeInt(extraIssues.size());
        for (String issue : extraIssues) {
            buf.writeUtf(issue);
        }
    }

    public static RocketStruct readFromByteBuffer(FriendlyByteBuf buf) {
        RocketStruct rocket = new RocketStruct();

        rocket.capsule = RocketPart.getPart(buf.readInt());

        int count = buf.readInt();
        for (int i = 0; i < count; i++) {
            RocketStage stage = new RocketStage();
            stage.fuselage = RocketPart.getPart(buf.readInt());
            stage.fins = RocketPart.getPart(buf.readInt());
            stage.thruster = RocketPart.getPart(buf.readInt());
            stage.fuselageCount = buf.readByte();
            stage.thrusterCount = buf.readByte();

            rocket.stages.add(stage);
        }

        count = buf.readInt();
        for (int i = 0; i < count; i++) {
            rocket.extraIssues.add(buf.readUtf());
        }

        return rocket;
    }

    // ── NBT ──────────────────────────────────────────────────────────────────────────────────

    public void writeToNBT(CompoundTag nbt) {
        nbt.putString("capsule", RocketPart.getName(capsule));

        ListTag stagesTag = new ListTag();
        for (RocketStage stage : stages) {
            CompoundTag stageTag = new CompoundTag();
            stageTag.putString("fuselage", RocketPart.getName(stage.fuselage));
            stageTag.putString("fins", RocketPart.getName(stage.fins));
            stageTag.putString("thruster", RocketPart.getName(stage.thruster));
            stageTag.putInt("fc", stage.fuselageCount);
            stageTag.putInt("tc", stage.thrusterCount);
            stagesTag.add(stageTag);
        }
        nbt.put("stages", stagesTag);

        nbt.putInt("freq", satFreq);
    }

    public static RocketStruct readFromNBT(CompoundTag nbt) {
        RocketStruct rocket = new RocketStruct();
        rocket.capsule = readPart(nbt, "capsule");

        ListTag stagesTag = nbt.getList("stages", Tag.TAG_COMPOUND);
        for (int i = 0; i < stagesTag.size(); i++) {
            CompoundTag stageTag = stagesTag.getCompound(i);
            RocketStage stage = new RocketStage();
            stage.fuselage = readPart(stageTag, "fuselage");
            stage.fins = readPart(stageTag, "fins");
            stage.thruster = readPart(stageTag, "thruster");
            stage.fuselageCount = Math.max(stageTag.getInt("fc"), 1);
            stage.thrusterCount = Math.max(stageTag.getInt("tc"), 1);
            rocket.stages.add(stage);
        }

        return rocket;
    }

    @Nullable
    private static RocketPart readPart(CompoundTag tag, String key) {
        if (tag.contains(key, Tag.TAG_STRING)) return RocketPart.getPart(tag.getString(key));
        return RocketPart.getPart(tag.getInt(key));
    }

    // ── Entity-Sync (Original DataWatcher, 32-Werte-Grenze -> gepackte ints) ─────────────────

    /** Original {@code writeToDataWatcher}: [0] Kapsel, [1] Stufenanzahl, je Stufe zwei gepackte ints. */
    public int[] writeToWatchable() {
        int[] data = new int[WATCHABLE_SIZE];
        data[0] = RocketPart.getId(capsule);
        data[1] = stages.size();
        for (int i = 0; i < stages.size() && i < MAX_STAGES; i++) {
            int[] watchable = stages.get(i).zipWatchable();
            data[i * 2 + 2] = watchable[0];
            data[i * 2 + 3] = watchable[1];
        }
        return data;
    }

    public static RocketStruct readFromWatchable(int[] data) {
        RocketStruct rocket = new RocketStruct();

        rocket.capsule = RocketPart.getPart(data[0]);

        int count = Math.min(data[1], MAX_STAGES);
        for (int i = 0; i < count; i++) {
            rocket.stages.add(RocketStage.unzipWatchable(data[i * 2 + 2], data[i * 2 + 3]));
        }

        // In case of emergency, default to regular capsule
        if (rocket.capsule == null) {
            rocket.capsule = RocketPart.getPart(SpaceRocketItems.RP_CAPSULE_20.get());
        }

        return rocket;
    }

    public static class RocketStage {

        @Nullable public RocketPart fuselage;
        @Nullable public RocketPart fins;
        @Nullable public RocketPart thruster;

        public int fuselageCount = 1;
        public int thrusterCount = 1;

        // fucking datawatchers and their limit of 32 synced values
        // I'm a crafty bastard though and can utilise the fact our values are always positive shorts and bytes
        public int[] zipWatchable() {
            int first = RocketPart.getId(fuselage) << 16 | RocketPart.getId(fins);
            int second = RocketPart.getId(thruster) << 16 | fuselageCount << 8 | thrusterCount;
            return new int[] { first, second };
        }

        public static RocketStage unzipWatchable(int first, int second) {
            RocketStage stage = new RocketStage();
            stage.fuselage = RocketPart.getPart(first >>> 16);
            stage.fins = RocketPart.getPart(first & 0xFFFF);
            stage.thruster = RocketPart.getPart(second >>> 16);
            stage.fuselageCount = (second >> 8) & 0xFF;
            stage.thrusterCount = second & 0xFF;
            return stage;
        }

        public int getStack() {
            return Math.max(fuselageCount / thrusterCount, 1);
        }

        public int getCluster() {
            return Math.max(fuselageCount / getStack(), 1);
        }

    }

}
