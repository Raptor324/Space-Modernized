package com.space_m.celestial;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.jetbrains.annotations.Nullable;

import com.hbm_m.inventory.fluid.trait.FluidTraitSimple;
import com.space_m.SpaceMod;
import com.space_m.celestial.trait.CBT_Atmosphere;
import com.space_m.celestial.trait.CBT_Atmosphere.FluidEntry;
import com.space_m.celestial.trait.CBT_Dyson;
import com.space_m.celestial.trait.CBT_War;
import com.space_m.celestial.trait.CBT_Water;
import com.space_m.celestial.trait.CelestialBodyTrait;
import com.space_m.dim.SpaceDimensions;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.WaterAnimal;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.levelgen.Heightmap;

/**
 * Himmelskoerper (Baumstruktur Stern -> Planeten -> Monde) mit Bahn-, Masse-, Radius- und Rotationsdaten und
 * Eigenschaften (Traits), 1:1 NTM Space {@code com.hbm.dim.CelestialBody}.
 * Original-Klasse: CelestialBody
 *
 * <p>Uebersetzungen (technisch noetig):
 * <ul>
 *   <li>{@code int dimensionId} -> {@link #dimension} ({@code ResourceKey<Level>}, s. {@link SpaceDimensions});
 *       {@link #legacyDimensionId} behaelt die Original-Zahl (Kerbin 0, Mun 413015 ...) fuer Rechnungen, die sie
 *       verwenden (Wetter-Speicherintervall). Unbekannte Dimensionen fallen wie im Original auf Kerbin zurueck
 *       ({@code getBody(int)}: {@code dimToBodyMap.get(0)}), das gilt auch fuer die Orbit-Dimension.</li>
 *   <li>{@code FluidType} -> Fluid-Registry-ID ({@link CelestialFluids}).</li>
 *   <li>{@code FMLCommonHandler.getEffectiveSide()} -> {@link #isServerSide()} (Server-Thread des laufenden Servers);
 *       {@code SolarSystemWorldSavedData.get()} (Welt 0) -> {@link #server()}.</li>
 *   <li>{@code Shader} (nur Client) -> {@link #shaderLocation}/{@link #shaderScale}; den Shader baut der Himmel.</li>
 * </ul>
 */
// Original-Klasse: FT_Gaseous (release/capture -> emitGas/consumeGas, Trait = HBM FluidTraitSimple.FT_Gaseous)
public class CelestialBody {

    /*
     * Stores planet data in a tree structure, allowing for bodies orbiting bodies
     * Unit suffixes added when they differ from SI units, for clarity
     */

    public String name;

    /** Dimension des Koerpers, null = keine Landung moeglich (Original {@code dimensionId = 0} bei {@code canLand = false}). */
    @Nullable
    public ResourceKey<Level> dimension = null;
    /** Original-Dimensionsnummer (SpaceConfig-Standard), 0 = Overworld/keine. */
    public int legacyDimensionId = 0;

    public boolean canLand = false; // does this body have an associated dimension and a solid surface?

    // Orbital elements
    public float massKg = 0;
    public float radiusKm = 0;
    public float semiMajorAxisKm = 0; // Distance to the parent body
    public float semiMinorAxisFactor = 0; // has a sqrt so done ahead of time
    public float eccentricity = 0;
    public float inclination = 0;
    public float ascendingNode = 0;
    public float argumentPeriapsis = 0;

    private int rotationalPeriod = 6 * 60 * 60; // Day length in seconds

    public float axialTilt = 0;

    private int minProcessingLevel = 0; // What level of technology can locate this body? This defines the minimum level, automatically adjusted based on stardar location

    public ResourceLocation texture = null;
    public ResourceLocation biomeMask = null;
    public ResourceLocation cityMask = null;
    public float[] color = new float[] {0.4F, 0.4F, 0.4F}; // When too small to render the texture

    public String tidallyLockedTo = null;

    public boolean hasRings = false; // put a ring on it
    public float ringTilt = 0;
    public float[] ringColor = new float[] {0.5F, 0.5F, 0.5F};
    public float ringSize = 2;

    public boolean hasIce = false; // has bedrock ice?

    /** Gasriese: Gas der Atmosphaere (Original {@code FluidType gas}), null = fester Koerper. */
    @Nullable
    public ResourceLocation gas;

    public List<CelestialBody> satellites = new ArrayList<>(); // moon boyes
    public CelestialBody parent = null;

    private final HashMap<Class<? extends CelestialBodyTrait>, CelestialBodyTrait> traits = new LinkedHashMap<>();

    public ResourceLocation stoneTexture;
    public ResourceLocation surfaceTexture;
    public SolarSystem.Body type;

    /** Fragment-Shader des Koerpers (Original {@code withShader}), null = keiner. */
    @Nullable
    public ResourceLocation shaderLocation;
    public float shaderScale = 1; // If the shader renders the item within the quad (not filling it entirely), scale it up from the true size

    public CelestialBody(String name) {
        this.name = name;
        this.texture = SpaceMod.id("textures/misc/space/" + name + ".png");

        nameToBodyMap.put(name, this);
    }

    public CelestialBody(String name, ResourceKey<Level> dimension, int legacyDimensionId, SolarSystem.Body type) {
        this(name);
        this.dimension = dimension;
        this.legacyDimensionId = legacyDimensionId;
        this.canLand = true;
        this.type = type;

        dimToBodyMap.put(dimension, this);
    }


    // Chainables for construction

    public CelestialBody withMassRadius(float kg, float km) {
        this.massKg = kg;
        this.radiusKm = km;
        return this;
    }

    public CelestialBody withOrbitalParameters(float semiMajorAxisKm, float eccentricity, float argumentPeriapsisDegrees, float inclinationDegrees, float ascendingNodeDegrees) {
        this.semiMajorAxisKm = semiMajorAxisKm;
        this.semiMinorAxisFactor = (float) Math.sqrt(1 - eccentricity * eccentricity);
        this.eccentricity = eccentricity;
        this.argumentPeriapsis = (float) Math.toRadians(argumentPeriapsisDegrees);
        this.inclination = (float) Math.toRadians(inclinationDegrees);
        this.ascendingNode = (float) Math.toRadians(ascendingNodeDegrees);
        return this;
    }

    public CelestialBody withRotationalPeriod(int seconds) {
        this.rotationalPeriod = seconds;
        return this;
    }

    public CelestialBody withAxialTilt(float degrees) {
        this.axialTilt = degrees;
        return this;
    }

    public CelestialBody withMinProcessingLevel(int level) {
        this.minProcessingLevel = level;
        return this;
    }

    public CelestialBody withTexture(ResourceLocation location) {
        this.texture = location;
        return this;
    }

    public CelestialBody withCityMask(ResourceLocation location) {
        this.cityMask = location;
        return this;
    }

    public CelestialBody withBiomeMask(ResourceLocation location) {
        this.biomeMask = location;
        return this;
    }

    public CelestialBody withBlockTextures(ResourceLocation stone, ResourceLocation surface) {
        this.stoneTexture = stone;
        this.surfaceTexture = surface;
        return this;
    }

    public CelestialBody withColor(float... color) {
        this.color = color;
        return this;
    }

    public CelestialBody withTidalLockingTo(String name) {
        tidallyLockedTo = name;
        return this;
    }

    public CelestialBody withRings(float tilt, float size, float... color) {
        this.hasRings = true;
        this.ringTilt = tilt;
        this.ringSize = size;
        this.ringColor = color;
        return this;
    }

    public CelestialBody withGas(ResourceLocation gas) {
        this.gas = gas;
        return this;
    }

    public CelestialBody withSatellites(CelestialBody... bodies) {
        Collections.addAll(satellites, bodies);
        for (CelestialBody body : bodies) {
            body.parent = this;
        }
        return this;
    }

    public CelestialBody withTraits(CelestialBodyTrait... traits) {
        for (CelestialBodyTrait trait : traits) this.traits.put(trait.getClass(), trait);
        return this;
    }

    public CelestialBody withShader(ResourceLocation fragmentShader) {
        return withShader(fragmentShader, 1);
    }

    public CelestialBody withShader(ResourceLocation fragmentShader, float scale) {
        shaderLocation = fragmentShader;
        shaderScale = scale;
        return this;
    }

    public CelestialBody withIce(boolean hasIce) {
        this.hasIce = hasIce;
        return this;
    }

    // /Chainables


    // ── Server / Seite ──────────────────────────────────────────────────────────────────────────────────

    private static volatile MinecraftServer server;

    /** Laufender (integrierter oder dedizierter) Server, null im Hauptmenue / auf reinen Clients. */
    @Nullable
    public static MinecraftServer server() {
        return server;
    }

    /** Vom Lebenszyklus gesetzt ({@link CelestialEvents}); null beim Stoppen. */
    public static void setServer(@Nullable MinecraftServer s) {
        server = s;
    }

    /** Original {@code FMLCommonHandler.getEffectiveSide() == Side.SERVER}: Aufruf auf dem Server-Thread. */
    public static boolean isServerSide() {
        MinecraftServer s = server;
        return s != null && s.isSameThread();
    }


    // Terraforming - trait overrides
    // If trait overrides exist, delete existing traits from the world, and replace them with the saved ones

    // Prefer statics over instance methods, for performance
    // but if you need to update a _different_ body (like, blowing up the sun)
    // instance members are the go

    public static void setTraits(Level world, CelestialBodyTrait... traits) {
        SolarSystemWorldSavedData traitsData = SolarSystemWorldSavedData.get(world);
        if (traitsData == null) return;

        traitsData.setTraits(getBody(world).name, traits);
    }

    public static void setTraits(Level world, Map<Class<? extends CelestialBodyTrait>, CelestialBodyTrait> traits) {
        setTraits(world, traits.values().toArray(new CelestialBodyTrait[0]));
    }

    public void setTraits(CelestialBodyTrait... traits) {
        SolarSystemWorldSavedData traitsData = SolarSystemWorldSavedData.get();
        if (traitsData == null) return;

        traitsData.setTraits(name, traits);
    }

    public void setTraits(Map<Class<? extends CelestialBodyTrait>, CelestialBodyTrait> traits) {
        setTraits(traits.values().toArray(new CelestialBodyTrait[0]));
    }

    /** Gets a clone of the body traits that are SAFE for modifying. */
    public static HashMap<Class<? extends CelestialBodyTrait>, CelestialBodyTrait> getTraits(Level world) {
        SolarSystemWorldSavedData traitsData = SolarSystemWorldSavedData.get(world);
        HashMap<Class<? extends CelestialBodyTrait>, CelestialBodyTrait> currentTraits = traitsData != null ? traitsData.getTraits(getBody(world).name) : null;

        if (currentTraits == null) {
            currentTraits = new HashMap<>();
            CelestialBody body = getBody(world);
            for (CelestialBodyTrait trait : body.traits.values()) {
                currentTraits.put(trait.getClass(), copy(trait));
            }
        }

        return currentTraits;
    }

    public HashMap<Class<? extends CelestialBodyTrait>, CelestialBodyTrait> getTraits() {
        SolarSystemWorldSavedData traitsData = SolarSystemWorldSavedData.get();
        HashMap<Class<? extends CelestialBodyTrait>, CelestialBodyTrait> currentTraits = traitsData != null ? traitsData.getTraits(name) : null;

        if (currentTraits == null) {
            currentTraits = new HashMap<>();
            for (CelestialBodyTrait trait : traits.values()) {
                currentTraits.put(trait.getClass(), copy(trait));
            }
        }

        return currentTraits;
    }

    /**
     * Kopie einer Standard-Eigenschaft (NBT-Rundreise). Abweichung: das Original legt hier die Standard-Instanzen
     * selbst ab, so dass {@code emitGas}/{@code consumeGas} ohne Ueberschreibung die Standardwerte bis zum Neustart
     * mitveraenderten (z.B. blieb nach {@code clearTraits} der veraenderte Druck). Gespeichert wird dasselbe.
     */
    private static CelestialBodyTrait copy(CelestialBodyTrait trait) {
        CelestialBodyTrait copy = CelestialBodyTrait.create(trait.getClass());
        net.minecraft.nbt.CompoundTag tag = new net.minecraft.nbt.CompoundTag();
        trait.writeToNBT(tag);
        copy.readFromNBT(tag);
        return copy;
    }

    public static void modifyTraits(Level world, CelestialBodyTrait... traits) {
        HashMap<Class<? extends CelestialBodyTrait>, CelestialBodyTrait> currentTraits = getTraits(world);

        for (CelestialBodyTrait trait : traits) {
            currentTraits.put(trait.getClass(), trait);
        }

        setTraits(world, currentTraits);
    }

    public void modifyTraits(CelestialBodyTrait... traits) {
        HashMap<Class<? extends CelestialBodyTrait>, CelestialBodyTrait> currentTraits = getTraits();

        for (CelestialBodyTrait trait : traits) {
            currentTraits.put(trait.getClass(), trait);
        }

        setTraits(currentTraits);
    }

    public static void clearTraits(Level world) {
        SolarSystemWorldSavedData traitsData = SolarSystemWorldSavedData.get(world);
        if (traitsData == null) return;

        traitsData.clearTraits(getBody(world).name);
    }

    public void clearTraits() {
        SolarSystemWorldSavedData traitsData = SolarSystemWorldSavedData.get();
        if (traitsData == null) return;

        traitsData.clearTraits(name);
    }

    // he has ceased to be
    public static void degas(Level world) {
        HashMap<Class<? extends CelestialBodyTrait>, CelestialBodyTrait> currentTraits = getTraits(world);

        currentTraits.remove(CBT_Atmosphere.class);

        setTraits(world, currentTraits);
    }

    public static boolean consumeGas(Level world, ResourceLocation fluid, double amount) {
        HashMap<Class<? extends CelestialBodyTrait>, CelestialBodyTrait> currentTraits = getTraits(world);

        CBT_Atmosphere atmosphere = (CBT_Atmosphere) currentTraits.get(CBT_Atmosphere.class);
        if (atmosphere == null) return false;

        boolean didConsume = false;
        int emptyIndex = -1;
        for (int i = 0; i < atmosphere.fluids.size(); i++) {
            FluidEntry entry = atmosphere.fluids.get(i);
            if (entry.fluid.equals(fluid)) {
                entry.pressure -= amount / AstronomyUtil.MB_PER_ATM;
                didConsume = true;
                emptyIndex = entry.pressure <= 0 ? i : -1;
                break;
            }
        }

        if (emptyIndex >= 0) {
            atmosphere.fluids.remove(emptyIndex);

            if (atmosphere.fluids.size() == 0) {
                currentTraits.remove(CBT_Atmosphere.class);
            }
        }

        setTraits(world, currentTraits);

        return didConsume;
    }

    public static void emitGas(Level world, ResourceLocation fluid, double amount) {
        HashMap<Class<? extends CelestialBodyTrait>, CelestialBodyTrait> currentTraits = getTraits(world);

        CBT_Atmosphere atmosphere = (CBT_Atmosphere) currentTraits.get(CBT_Atmosphere.class);
        if (atmosphere == null) {
            atmosphere = new CBT_Atmosphere();
            currentTraits.put(CBT_Atmosphere.class, atmosphere);
        }

        boolean hasFluid = false;
        for (FluidEntry entry : atmosphere.fluids) {
            if (entry.fluid.equals(fluid)) {
                entry.pressure += amount / AstronomyUtil.MB_PER_ATM;
                hasFluid = true;
                break;
            }
        }

        if (!hasFluid) {
            // Sort existing fluids and remove the lowest fraction
            if (atmosphere.fluids.size() >= 8) {
                atmosphere.sortDescending();
                atmosphere.fluids.remove(atmosphere.fluids.size() - 1);
            }

            atmosphere.fluids.add(new FluidEntry(fluid, amount / AstronomyUtil.MB_PER_ATM));
        }

        setTraits(world, currentTraits);
    }

    /** Original {@code FT_Gaseous.release}: nur Gase ({@code FT_Gaseous}/{@code FT_Gaseous_ART}) in die Atmosphaere. */
    public static void releaseGas(Level world, ResourceLocation fluid, double mB) {
        if (world.isClientSide) return;
        if (!isGas(fluid)) return;
        emitGas(world, fluid, mB);
    }

    /** Original {@code FT_Gaseous.capture}: Gase aus der Atmosphaere entnehmen. */
    public static void captureGas(Level world, ResourceLocation fluid, double mB) {
        if (world.isClientSide) return;
        if (!isGas(fluid)) return;
        consumeGas(world, fluid, mB);
    }

    public static boolean isGas(ResourceLocation fluid) {
        return CelestialFluids.hasTrait(fluid, FluidTraitSimple.FT_Gaseous.class)
            || CelestialFluids.hasTrait(fluid, FluidTraitSimple.FT_Gaseous_ART.class);
    }

    public static boolean reactAtmosphere(Level world, AtmosphereReaction recipe) {
        HashMap<Class<? extends CelestialBodyTrait>, CelestialBodyTrait> currentTraits = getTraits(world);
        CBT_Atmosphere atmosphere = (CBT_Atmosphere) currentTraits.get(CBT_Atmosphere.class);
        if (atmosphere == null) return false;

        int scale = 64;

        // because atmochem runs infrequently, we will automatically scale this to react all it can immediately
        for (AtmosphereReaction.GasStack recipeFluid : recipe.inputs()) {
            boolean hasInput = false;

            for (FluidEntry entry : atmosphere.fluids) {
                if (entry.fluid.equals(recipeFluid.fluid()) && entry.pressure * AstronomyUtil.MB_PER_ATM >= recipeFluid.fill() * scale) hasInput = true;
            }

            if (!hasInput) return false;
        }

        for (AtmosphereReaction.GasStack recipeFluid : recipe.inputs()) {
            captureGas(world, recipeFluid.fluid(), recipeFluid.fill() * scale);
        }

        releaseGas(world, recipe.output().fluid(), recipe.output().fill() * scale);

        return true;
    }

    public static void updateChemistry(Level world) {
        boolean hasUpdated = false;
        HashMap<Class<? extends CelestialBodyTrait>, CelestialBodyTrait> currentTraits = getTraits(world);
        CBT_Atmosphere atmosphere = (CBT_Atmosphere) currentTraits.get(CBT_Atmosphere.class);

        CBT_Water water = (CBT_Water) currentTraits.get(CBT_Water.class);
        if (water == null) {

            if (atmosphere != null) {
                double pressure = 0;
                for (FluidEntry entry : atmosphere.fluids) {
                    if (entry.fluid.equals(CelestialFluids.STEAM)
                    || entry.fluid.equals(CelestialFluids.HOTSTEAM)
                    || entry.fluid.equals(CelestialFluids.SUPERHOTSTEAM)
                    || entry.fluid.equals(CelestialFluids.ULTRAHOTSTEAM)
                    || entry.fluid.equals(CelestialFluids.SPENTSTEAM)) {
                        pressure += entry.pressure;
                    }
                }

                if (pressure > 0.2D) {
                    currentTraits.put(CBT_Water.class, new CBT_Water());
                    hasUpdated = true;
                }
            }
        }

        if (atmosphere != null) {
            for (AtmosphereReaction recipe : AtmosphereReaction.REACTIONS) {
                if (reactAtmosphere(world, recipe)) {
                    hasUpdated = true;
                }
            }
        }

        if (hasUpdated)
            setTraits(world, currentTraits);
    }

    /** Called once per tick to attenuate swarm counts based on a swarm half-life. */
    public static void updateSwarms() {
        // We currently only have the one solar body, so we just update that directly
        HashMap<Class<? extends CelestialBodyTrait>, CelestialBodyTrait> currentTraits = SolarSystem.kerbol.getTraits();

        CBT_Dyson dyson = (CBT_Dyson) currentTraits.get(CBT_Dyson.class);
        if (dyson == null) return;

        dyson.attenuate();
    }

    // /Terraforming


    public static void damage(int dmg, Level world) {
        HashMap<Class<? extends CelestialBodyTrait>, CelestialBodyTrait> currentTraits = getTraits(world);

        CBT_War war = (CBT_War) currentTraits.get(CBT_War.class);
        if (war == null) {
            war = new CBT_War();
            currentTraits.put(CBT_War.class, war);
        }

        if (war.shield > 0) {
            war.shield -= dmg;
        } else {
            war.health -= dmg;
        }

        setTraits(world, currentTraits);
    }

    /** Wie {@link #damage(int, Level)}, aber direkt am Koerper (ohne dessen Dimension laden zu muessen). */
    public void damage(int dmg) {
        HashMap<Class<? extends CelestialBodyTrait>, CelestialBodyTrait> currentTraits = getTraits();

        CBT_War war = (CBT_War) currentTraits.get(CBT_War.class);
        if (war == null) {
            war = new CBT_War();
            currentTraits.put(CBT_War.class, war);
        }

        if (war.shield > 0) {
            war.shield -= dmg;
        } else {
            war.health -= dmg;
        }

        setTraits(currentTraits);
    }


    // Static getters
    // A lot of these are member getters but without having to check the celestial body exists
    // If it doesn't exist, return the overworld as the default, may cause issues with terraforming the overworld

    private static final HashMap<ResourceKey<Level>, CelestialBody> dimToBodyMap = new LinkedHashMap<>();
    private static final HashMap<String, CelestialBody> nameToBodyMap = new LinkedHashMap<>();

    public static Collection<CelestialBody> getAllBodies() {
        return nameToBodyMap.values();
    }

    /** Koerper mit Dimension und Landeflag (Thatmo hat eine Dimension, ist aber wie im Original nicht "landbar"). */
    public static Collection<CelestialBody> getLandableBodies() {
        List<CelestialBody> list = new ArrayList<>();
        for (CelestialBody body : dimToBodyMap.values()) if (body.canLand) list.add(body);
        return list;
    }

    public static CelestialBody getBody(String name) {
        CelestialBody body = nameToBodyMap.get(name);
        return body != null ? body : dimToBodyMap.get(Level.OVERWORLD);
    }

    public static CelestialBody getBody(@Nullable ResourceKey<Level> dimension) {
        CelestialBody body = dimension != null ? dimToBodyMap.get(dimension) : null;
        return body != null ? body : dimToBodyMap.get(Level.OVERWORLD);
    }

    /** Nach Dimensions-ID ({@code ResourceKey<Level>.location()}). */
    public static CelestialBody getBody(ResourceLocation dimensionId) {
        for (Map.Entry<ResourceKey<Level>, CelestialBody> e : dimToBodyMap.entrySet()) {
            if (e.getKey().location().equals(dimensionId)) return e.getValue();
        }
        return dimToBodyMap.get(Level.OVERWORLD);
    }

    // bit of a dumb one but the other function is already used widely
    @Nullable
    public static CelestialBody getBodyOrNull(String name) {
        return nameToBodyMap.get(name);
    }

    @Nullable
    public static CelestialBody getBodyOrNull(@Nullable ResourceKey<Level> dimension) {
        return dimension == null ? null : dimToBodyMap.get(dimension);
    }

    public static CelestialBody getBody(Level world) {
        return getBody(world.dimension());
    }

    /** Original {@code ItemVOTVdrive.Target}: Ziel (Koerper), im Orbit?, Station vorhanden?. */
    public static final class Target {
        public final CelestialBody body;
        public final boolean inOrbit;
        public final boolean hasStation;

        public Target(CelestialBody body, boolean inOrbit, boolean hasStation) {
            this.body = body;
            this.inOrbit = inOrbit;
            this.hasStation = hasStation;
        }

        public CelestialBody body() { return body; }
        public boolean inOrbit() { return inOrbit; }
        public boolean hasStation() { return hasStation; }
    }

    public static Target getTarget(Level world, int x, int z) {
        if (inOrbit(world)) {
            OrbitHooks hooks = OrbitHooks.get();
            CelestialBody orbiting = hooks.orbiting(world, x, z);
            return new Target(orbiting != null ? orbiting : getBody("kerbin"), true, hooks.hasStation(world, x, z));
        }

        return new Target(getBody(world), false, true);
    }

    public static CelestialBody getStar(Level world) {
        return getBody(world).getStar();
    }

    public static CelestialBody getPlanet(Level world) {
        return getBody(world).getPlanet();
    }

    public static float getGravity(LivingEntity entity) {
        if (entity instanceof WaterAnimal) return AstronomyUtil.STANDARD_GRAVITY;

        Level world = entity.level();
        if (inOrbit(world)) {
            OrbitHooks hooks = OrbitHooks.get();
            if (hooks.hasGravity(entity)) {
                float gravity = AstronomyUtil.STANDARD_GRAVITY * hooks.gravityMultiplier(world, (int) entity.getX(), (int) entity.getZ());
                if (gravity < 0.2) return 0;
                return gravity;
            }

            return 0;
        }

        CelestialBody body = CelestialBody.getBody(world);
        return body.getSurfaceGravity() * AstronomyUtil.PLAYER_GRAVITY_MODIFIER;
    }

    public static boolean inOrbit(Level world) {
        return SpaceDimensions.isOrbit(world.dimension());
    }

    public static SolarSystem.Body getEnum(Level world) {
        return getBody(world).getEnum();
    }

    public static int getMeta(Level world) {
        return getBody(world).getEnum().ordinal();
    }

    public static double getRotationalPeriod(Level world) {
        return getBody(world).getRotationalPeriod();
    }

    public static float getSemiMajorAxis(Level world) {
        return getBody(world).semiMajorAxisKm;
    }

    public static boolean hasTrait(Level world, Class<? extends CelestialBodyTrait> trait) {
        return getBody(world).hasTrait(trait);
    }

    @Nullable
    public static <T extends CelestialBodyTrait> T getTrait(Level world, Class<? extends T> trait) {
        return getBody(world).getTrait(trait);
    }

    public static boolean hasDefaultTrait(Level world, Class<? extends CelestialBodyTrait> trait) {
        return getBody(world).hasDefaultTrait(trait);
    }

    @Nullable
    public static <T extends CelestialBodyTrait> T getDefaultTrait(Level world, Class<? extends T> trait) {
        return getBody(world).getDefaultTrait(trait);
    }

    // /Statics


    public String getUnlocalizedName() {
        return name;
    }

    public SolarSystem.Body getEnum() {
        return type;
    }

    public CelestialBody getStar() {
        CelestialBody body = this;
        while (body.parent != null)
            body = body.parent;

        return body;
    }

    public CelestialBody getPlanet() {
        if (this.parent == null) return this;
        CelestialBody body = this;
        while (body.parent.parent != null)
            body = body.parent;

        return body;
    }

    /** Returns the day length in ticks, adjusted for the 20 minute minecraft day. */
    public double getRotationalPeriod() {
        return (double) rotationalPeriod * (AstronomyUtil.DAY_FACTOR / (double) AstronomyUtil.TIME_MULTIPLIER) * 20;
    }

    /** Rotationsperiode in Sekunden (KSP-Wert). */
    public int getRotationalPeriodSeconds() {
        return rotationalPeriod;
    }

    /** Returns the year length in days, derived from semi-major axis. */
    public double getOrbitalPeriod() {
        double semiMajorAxis = semiMajorAxisKm * 1_000;
        double orbitalPeriod = 2 * Math.PI * Math.sqrt((semiMajorAxis * semiMajorAxis * semiMajorAxis) / (AstronomyUtil.GRAVITATIONAL_CONSTANT * parent.massKg));
        return orbitalPeriod / (double) AstronomyUtil.SECONDS_IN_KSP_DAY;
    }

    /** Get the gravitational force at the surface, derived from mass and radius (m/s^2). */
    public float getSurfaceGravity() {
        float radius = radiusKm * 1000;
        return AstronomyUtil.GRAVITATIONAL_CONSTANT * massKg / (radius * radius);
    }

    /** Get the power multiplier for sun based machines. */
    public float getSunPower() {
        float distanceAU = getPlanet().semiMajorAxisKm / AstronomyUtil.KM_IN_AU;
        return 1 / (distanceAU * distanceAU);
    }

    /** Processing level is based off of where you are processing from, so if you're on Duna, Ike will be tier 0. */
    public int getProcessingLevel(CelestialBody from) {
        int level;

        if (this == from) {
            // If self, tier 0
            level = 0;
        } else if (this == from.parent || this.parent == from) {
            // If going to/from a moon, tier 0
            level = 0;
        } else {
            // Otherwise, tier 1
            level = 1;
        }

        // Unless a minimum processing level is set
        return Math.max(level, minProcessingLevel);
    }

    public int getMinProcessingLevel() {
        return minProcessingLevel;
    }


    public boolean hasTrait(Class<? extends CelestialBodyTrait> trait) {
        return getTraitsUnsafe().containsKey(trait);
    }

    @SuppressWarnings("unchecked")
    @Nullable
    public <T extends CelestialBodyTrait> T getTrait(Class<? extends T> trait) {
        return (T) getTraitsUnsafe().get(trait);
    }

    // Don't modify traits returned from this!
    private HashMap<Class<? extends CelestialBodyTrait>, CelestialBodyTrait> getTraitsUnsafe() {
        HashMap<Class<? extends CelestialBodyTrait>, CelestialBodyTrait> traits;
        if (!isServerSide()) {
            traits = SolarSystemWorldSavedData.getClientTraits(name);
        } else {
            SolarSystemWorldSavedData data = SolarSystemWorldSavedData.get();
            traits = data != null ? data.getTraits(name) : null;
        }

        if (traits != null)
            return traits;

        return this.traits;
    }

    public boolean hasDefaultTrait(Class<? extends CelestialBodyTrait> trait) {
        return traits.containsKey(trait);
    }

    @SuppressWarnings("unchecked")
    @Nullable
    public <T extends CelestialBodyTrait> T getDefaultTrait(Class<? extends T> trait) {
        return (T) traits.get(trait);
    }


    /** Loads in the heightmap data for a given chunk (Original {@code Chunk.heightMap}, Index {@code z << 4 | x}). */
    @Nullable
    public int[] getHeightmap(int chunkX, int chunkZ) {
        MinecraftServer s = server;
        if (s == null || dimension == null) return null;
        ServerLevel world = s.getLevel(dimension);
        if (world == null) return null;

        // Load OR generate the desired chunk
        ChunkAccess chunk = world.getChunk(chunkX, chunkZ);
        int[] heightMap = new int[256];
        for (int z = 0; z < 16; z++) {
            for (int x = 0; x < 16; x++) {
                heightMap[z << 4 | x] = chunk.getHeight(Heightmap.Types.WORLD_SURFACE, x, z) + 1;
            }
        }
        return heightMap;
    }

    /** Hilfsfunktion fuer die Orbit-Dimension: Koerper ueber der Position (Original {@code getTarget(...).body}). */
    public static CelestialBody getTargetBody(Level world, BlockPos pos) {
        return getTarget(world, pos.getX(), pos.getZ()).body();
    }
}
