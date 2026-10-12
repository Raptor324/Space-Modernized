package com.space_m.rocket.entity;

import java.util.UUID;

import org.jetbrains.annotations.Nullable;

import com.hbm_m.explosion.ExplosionLarge;
import com.hbm_m.item.ISatChip;
import com.hbm_m.platform.PlatformHooks;
import com.hbm_m.satellite.SatelliteManager;
import com.hbm_m.sound.HbmSoundsNT;
import com.hbm_m.util.ParticleUtil;
import com.space_m.celestial.CelestialBody;
import com.space_m.celestial.SolarSystem;
import com.space_m.dim.SpaceDimensions;
import com.space_m.entity.SpaceEntities;
import com.space_m.rocket.INavDriveReceiver;
import com.space_m.rocket.ItemCustomRocket;
import com.space_m.rocket.ItemVOTVdrive;
import com.space_m.rocket.RocketPart;
import com.space_m.rocket.RocketPayloadHooks;
import com.space_m.rocket.RocketStruct;
import com.space_m.rocket.RocketStruct.RocketStage;
import com.space_m.rocket.SpaceRocketItems;
import com.space_m.sound.SpaceSounds;
import com.space_m.travel.CelestialMount;
import com.space_m.travel.CelestialTeleporter;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.TicketType;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * 1:1 NTM Space {@code EntityRideableRocket} (Basis {@code EntityMissileBaseNT}/{@code EntityThrowableInterp}): die
 * bemannte bzw. Satelliten-Rakete aus der Montage ({@link RocketStruct}), gestartet von der Startrampe.
 *
 * <p>Zustaende ({@link RocketState}): AWAITING (bereit; Sprungtaste oder Satellit = Start), LAUNCHING (Aufstieg bis
 * Y 900, Neigung bis 45 Grad), TRANSFER (Flug zu einem anderen Himmelskoerper), LANDING (Abstieg auf die
 * Laufwerkskoordinaten des Ziels, vorher Abwurf der untersten Stufe), LANDED, TIPPING (ohne Landebeine bzw. auf
 * Fluessigkeit umkippen, ggf. Explosion), DOCKING/UNDOCKING (Raumstation, R6), NEEDSFUEL (wiederverwendbare Kapsel).
 * Bewegung wie im Original: {@code pos += motion * motionMult()} mit {@code motionMult = 4} im Flug, Aufprall per
 * Strahlverfolgung auf Bloecke (Fluessigkeiten ignoriert).
 *
 * <p>Abweichungen (technisch noetig): DataWatcher -> {@link SynchedEntityData} (Rakete als gepackte ints wie im
 * Original); Stationsdaten fuer den Transfer werden als Fortschritt/Ziel synchronisiert statt per
 * {@code EntityBufPacket}; Orbit/Stationen ueber {@link RocketTravelHooks} (ohne R6: Transfer in der Rakete);
 * Chunk-Ticket statt ForgeChunkManager; Ausstieg im Flug wird ueber das Loader-Ereignis
 * {@code EntityMountEvent} unterbunden ({@link RocketEvents}) statt im Welt-Tick nachtraeglich.
 */
// Original-Klasse: EntityBufPacket (ersetzt: Original schickt dem Fahrer per EntityBufPacket die Stationsdaten
// (OrbitalStation.clientStation) fuer die Transferanzeige; hier SynchedEntityData WATCHABLE_TRANSFER/_TARGET)
public class EntityRideableRocket extends Entity implements CelestialMount, INavDriveReceiver {

    public enum RocketState {
        AWAITING,   // Prepped for launch, once mounted will transition to launching
        LAUNCHING,  // Ascending through the atmosphere up to the target altitude, at which point it'll teleport to the target body
        LANDING,    // Descending onto the target location
        LANDED,     // Landed on the target, will not launch until the player activates the rocket, at which point it'll transition back to AWAITING
        TIPPING,    // tipping culture is a burden on modern society
        DOCKING,    // Arriving at an orbital station
        UNDOCKING,  // Leaving an orbital station
        NEEDSFUEL,  // Needs fuel, once fueled it will transition to AWAITING
        TRANSFER,   // Flying in space!
    }

    private static final EntityDataAccessor<Integer> WATCHABLE_STATE = SynchedEntityData.defineId(EntityRideableRocket.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<ItemStack> WATCHABLE_DRIVE = SynchedEntityData.defineId(EntityRideableRocket.class, EntityDataSerializers.ITEM_STACK);
    private static final EntityDataAccessor<Integer> WATCHABLE_TIMER = SynchedEntityData.defineId(EntityRideableRocket.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<CompoundTag> WATCHABLE_ROCKET = SynchedEntityData.defineId(EntityRideableRocket.class, EntityDataSerializers.COMPOUND_TAG);
    /** Ersatz fuer {@code OrbitalStation.clientStation} (EntityBufPacket): Transferfortschritt 0..1. */
    private static final EntityDataAccessor<Float> WATCHABLE_TRANSFER = SynchedEntityData.defineId(EntityRideableRocket.class, EntityDataSerializers.FLOAT);
    /** Ersatz fuer {@code OrbitalStation.clientStation.target.name}. */
    private static final EntityDataAccessor<String> WATCHABLE_TRANSFER_TARGET = SynchedEntityData.defineId(EntityRideableRocket.class, EntityDataSerializers.STRING);

    private static final TicketType<Integer> CHUNK_TICKET = TicketType.create("space_m_rocket", Integer::compareTo);
    private static final int CHUNK_TICKET_RADIUS = 3;

    /** Client-Seite (Geraeusche); setzt {@code RocketEntityClient}. Auf dem Server no-op. */
    public static RocketClientHook clientHook = new RocketClientHook() { };

    public ItemStack navDrive = ItemStack.EMPTY;

    @Nullable public EntityRideableRocketDummy capDummy;

    private int stateTimer = 0;
    public int decoupleTimer = 0;
    public int shroudTimer = 0;

    public int forceExitTimer = 0;

    private double rocketVelocity = 0;

    private boolean sizeSet = false;

    /** Client: Geraeusch-Instanz (Original AudioWrapper), verwaltet von {@link #clientHook}. */
    public Object audio;

    private RocketState lastState = RocketState.AWAITING;

    private boolean willExplode = false;

    /** Original EntityMissileBaseNT targetX/targetZ (fuer die Rakete ohne Wirkung, nur NBT). */
    public int targetX;
    public int targetZ;

    private int satFreq = 0;
    /** Original satInclination/satAltitude/satIsBlinking/satBlink/satOwner/satColorR/G/B: die "sat*"-Schluessel des Raketen-Stacks. */
    private CompoundTag satData = new CompoundTag();

    @Nullable private RocketTravelHooks.RocketPort targetPort;

    @Nullable private ItemVOTVdrive.Destination destinationOverride; // for pod recalls, will ignore the current drive if set

    /** Original EntityThrowable.thrower (Startender Spieler, haelt den Spieler im Flug in der Kapsel). */
    @Nullable private LivingEntity thrower;
    @Nullable private UUID throwerId;

    /** Transfer ueber eine R6-Station (Rasterkoordinaten) statt in der Rakete. */
    private boolean stationTransfer = false;
    private int transferTime = 0;

    /** Ein CelestialTeleporter-Wechsel ist eingereiht: Ausstieg nicht blockieren. */
    public boolean teleportPending = false;

    @Nullable private ChunkPos loadedChunk;

    // Client-Interpolation (Original EntityThrowableInterp)
    private int lerpSteps;
    private double lerpX, lerpY, lerpZ, lerpYRot, lerpXRot;

    public EntityRideableRocket(EntityType<? extends EntityRideableRocket> type, Level level) {
        super(type, level);
        this.noCulling = true; // ignoreFrustumCheck
        this.noPhysics = true;
        sizeSet = false;
        targetX = (int) getX() - 10000;
        targetZ = (int) getZ();
    }

    /** Original {@code new EntityRideableRocket(world, x, y, z, stack)}. */
    public static EntityRideableRocket create(Level level, double x, double y, double z, ItemStack stack) {
        EntityRideableRocket rocket = new EntityRideableRocket(SpaceEntities.ENTITY_RIDEABLE_ROCKET.get(), level);
        rocket.moveTo(x, y, z, 0, 0);
        rocket.targetX = (int) x + 10000;
        rocket.targetZ = (int) z;
        RocketStruct struct = ItemCustomRocket.get(stack);
        rocket.satFreq = ISatChip.getFreqS(stack);
        rocket.satData = extractSatData(stack);
        if (struct != null) rocket.setRocket(struct);
        rocket.refreshDimensions();
        return rocket;
    }

    public EntityRideableRocket withProgram(ItemStack stack) {
        this.navDrive = stack.copy();
        setDrive(navDrive); // Original erst im naechsten onUpdate; so stimmt das Ziel schon vor dem ersten Tick
        return this;
    }

    public EntityRideableRocket launchedBy(@Nullable LivingEntity entity) {
        setThrower(entity);
        return this;
    }

    public void setThrower(@Nullable LivingEntity entity) {
        this.thrower = entity;
        this.throwerId = entity != null ? entity.getUUID() : null;
    }

    @Nullable
    public LivingEntity getThrower() {
        if (thrower != null && thrower.isRemoved()) {
            // Spieler wechselt die Dimension/stirbt - neu aufloesen
            thrower = null;
        }
        if (thrower == null && throwerId != null && level() instanceof ServerLevel server) {
            Entity e = server.getEntity(throwerId);
            if (e == null) e = server.getServer().getPlayerList().getPlayer(throwerId);
            if (e instanceof LivingEntity living && living.level() == level()) thrower = living;
        }
        return thrower;
    }

    // ── Reise ──────────────────────────────────────────────────────────────────────────────────

    public void beginLandingSequence(CelestialBody.Target from, ItemVOTVdrive.Target to) {
        setDeltaMovement(Vec3.ZERO);

        RocketStruct rocket = getRocket();
        boolean expendStage = rocket.stages.size() > 0;
        if (getState() == RocketState.UNDOCKING && from.body == to.body()) expendStage = false;

        if (expendStage) {
            rocket.stages.remove(0);

            setRocket(rocket);
            refreshDimensions();
        }

        setState(RocketState.LANDING);

        ItemVOTVdrive.Destination destination = getDestination();

        if (destination != null && level() instanceof ServerLevel serverLevel) {
            int x = destination.x();
            int y = 800;
            int z = destination.z();

            ResourceKey<Level> targetDimension = destination.body().getDimension();

            Player rider = getRider();

            if (canRide()) {
                if (destination.body() == SolarSystem.Body.ORBIT) {
                    setState(RocketState.DOCKING);

                    // Place the station in the middle of the zone, where the docking ring will always be
                    int size = RocketTravelHooks.STATION_SIZE;
                    x = x * size + (size / 2);
                    y = 0;
                    z = z * size + (size / 2);
                }

                if (level().dimension() != targetDimension) {
                    teleportPending = true;
                    if (rider != null) {
                        CelestialTeleporter.teleport(rider, targetDimension, x + 0.5D, y, z + 0.5D, false);
                    } else {
                        CelestialTeleporter.teleport(this, targetDimension, x + 0.5D, y, z + 0.5D, false);
                    }
                } else {
                    setPos(x + 0.5D, y, z + 0.5D);
                }

                // After a successful warp, spawn in a station core if one doesn't yet exist
                if (destination.body() == SolarSystem.Body.ORBIT) {
                    ServerLevel targetWorld = serverLevel.getServer().getLevel(targetDimension);
                    if (targetWorld != null) RocketTravelHooks.get().spawnStation(targetWorld, x, z);
                }
            } else {
                Item capsuleItem = rocket.capsule != null ? rocket.capsule.part : null;
                if (capsuleItem instanceof ISatChip && destination.body() != SolarSystem.Body.ORBIT) {
                    ServerLevel targetWorld = serverLevel.getServer().getLevel(targetDimension); // DimensionManager.initDimension entfaellt
                    if (targetWorld != null) {
                        ItemStack stack = new ItemStack(capsuleItem);
                        applySatData(stack);
                        RocketPayloadHooks.get().orbit(targetWorld, stack, satFreq, getX(), getY(), getZ()); // R6-C: XSatelliteRegistry.orbit
                    }
                } else if (capsuleItem == SpaceRocketItems.RP_STATION_CORE_20.get()) {
                    // We mark the station as travellable, but we don't actually add the station until the player travels to it
                    RocketTravelHooks.get().addStation(x, z, CelestialBody.getBody(level()));
                    // Original: Erfolg achDriveFail, wenn das Laufwerk nicht kopiert wurde (R9-B: space_m:drive_fail)
                    if (thrower instanceof net.minecraft.server.level.ServerPlayer player && !player.isCreative()
                            && !ItemVOTVdrive.wasCopied(navDrive)) {
                        com.hbm_m.platform.PlatformHooks.awardAdvancementIfEligible(player, com.space_m.SpaceMod.id("drive_fail"), true);
                    }
                }

                discard();
            }
        }
    }

    public void beginCelestialTransfer(CelestialBody.Target from, ItemVOTVdrive.Target to) {
        setDeltaMovement(Vec3.ZERO);

        setState(RocketState.TRANSFER);

        RocketStruct rocket = getRocket();

        int size = 10;
        double distance = SolarSystem.calculateDistanceBetweenTwoBodies(level(), from.body, to.body());
        float thrust = rocket.getThrust();
        int time = RocketTravelHooks.calculateTransferTime(distance, size, thrust);

        entityData.set(WATCHABLE_TRANSFER, 0F);
        entityData.set(WATCHABLE_TRANSFER_TARGET, to.body() != null ? to.body().name : "");

        int[] station = RocketTravelHooks.get().beginTransfer((ServerLevel) level(), from.body, to.body(), time);

        if (station == null) {
            // Ohne Orbit-Dimension/Stationen (R6): der Transfer laeuft in der Rakete (s. RocketTravelHooks)
            stationTransfer = false;
            transferTime = Math.max(time, 1);
            return;
        }

        stationTransfer = true;
        transferTime = Math.max(time, 1);

        Player rider = getRider();

        int stationSize = RocketTravelHooks.STATION_SIZE;
        int x = station[0] * stationSize + (stationSize / 2);
        int y = 128;
        int z = station[1] * stationSize + (stationSize / 2);

        if (level().dimension() != SpaceDimensions.ORBIT) {
            teleportPending = true;
            if (rider != null) {
                CelestialTeleporter.teleport(rider, SpaceDimensions.ORBIT, x + 0.5D, y, z + 0.5D, false);
            } else {
                CelestialTeleporter.teleport(this, SpaceDimensions.ORBIT, x + 0.5D, y, z + 0.5D, false);
            }
        } else {
            setPos(x + 0.5D, y, z + 0.5D);
        }
    }

    public void attemptLaunch() {
        CelestialBody.Target from = CelestialBody.getTarget(level(), (int) getX(), (int) getZ());
        ItemVOTVdrive.Target to = getTarget();

        RocketState transitionTo = from.inOrbit ? RocketState.UNDOCKING : RocketState.LAUNCHING;

        targetX = (int) getX() - 10000;
        targetZ = (int) getZ();

        // Will only launch if the rocket has sufficient fuel
        if (to.body() != null && getRocket().hasSufficientFuel(from.body, to.body(), from.inOrbit, to.inOrbit())) {
            setState(transitionTo);
        }
    }

    /** Testhilfe (GameTest): laufenden Transfer ohne Station sofort abschliessen. */
    public void skipTransfer() {
        if (getState() == RocketState.TRANSFER && !stationTransfer) stateTimer = transferTime;
    }

    public int getTransferTime() {
        return transferTime;
    }

    public boolean canExitCapsule() {
        RocketState state = getState();
        return state != RocketState.LANDING
            && state != RocketState.LAUNCHING
            && state != RocketState.DOCKING
            && state != RocketState.UNDOCKING
            && state != RocketState.TRANSFER;
    }

    @Nullable
    public Player getRider() {
        Entity passenger = getFirstPassenger();
        return passenger instanceof Player p ? p : null;
    }

    // ── Tick ───────────────────────────────────────────────────────────────────────────────────

    @Override
    public void tick() {
        // EntityThrowableInterp/EntityThrowableNT.onUpdate
        super.tick();

        if (!level().isClientSide) {
            moveThrowable();
            updateChunkTicket();
        } else {
            clientLerpStep();
            spawnContrail();
        }

        RocketState state = getState();

        if (!sizeSet) {
            refreshDimensions();
            if (!level().isClientSide && (state == RocketState.LANDED || state == RocketState.AWAITING || state == RocketState.NEEDSFUEL)) {
                RocketTravelHooks.get().dockAt(level(), BlockPos.containing(getX(), getY() + getBbHeight() - 1.0D, getZ()), this);
            }
        }

        Player rider = getRider();

        if (!level().isClientSide) {
            setYRot(-90.0F);

            if (!navDrive.isEmpty() && navDrive.getItem() instanceof ItemVOTVdrive) {
                ItemVOTVdrive.getTarget(navDrive, level());
                setDrive(navDrive);
            }

            if (getThrower() == null && rider != null) {
                setThrower(rider);
            }

            // If it's a satellite launcher, launch immediately
            if (state == RocketState.AWAITING && ((rider != null && isJumping(rider)) || !canRide())) {
                attemptLaunch();

                setThrower(rider);
            }

            LivingEntity thrower = getThrower();
            if (thrower != null && rider == null && !canExitCapsule() && forceExitTimer < 60 && thrower.isAlive() && thrower.level() == level()) {
                thrower.startRiding(this, true);
            }

            double motionY = 0;
            double pitch = getXRot();

            if (state == RocketState.LAUNCHING) {
                if (isReusable()) {
                    pitch = Mth.clamp((stateTimer - 60) * 0.3F, 0.0F, 45.0F);
                    if (rocketVelocity < 4) rocketVelocity += Mth.clamp(stateTimer / 120D * 0.05D, 0, 0.05);
                } else {
                    double acceleration = stateTimer / 120D;
                    pitch = Mth.clamp((stateTimer - 80) * 0.3F, 0.0F, 45.0F);
                    if (rocketVelocity < 4) rocketVelocity += Mth.clamp(acceleration * acceleration * 0.05D, 0, 0.05);
                }
                // "FUCK OPTIFINE" (Neigung 0 mit OptiFine) entfaellt - kein OptiFine auf 1.20+
            } else if (state == RocketState.LANDING) {
                double targetHeight = getHeightValue((int) getX(), (int) getZ());
                pitch = 0;

                if (isReusable()) {
                    rocketVelocity = Mth.clamp((targetHeight - getY()) * 0.01, -1.0, -0.005);
                } else {
                    rocketVelocity = Mth.clamp((targetHeight - getY()) * 0.005, -0.5, -0.005);
                }

                if (destinationOverride == null && !navDrive.isEmpty() && navDrive.getItem() instanceof ItemVOTVdrive) {
                    ItemVOTVdrive.Destination destination = ItemVOTVdrive.getDestination(navDrive);
                    int dx = destination.x();
                    int dz = destination.z();

                    // Check if we're about to land on top of another rocket and adjust accordingly
                    AABB bb = getBoundingBox();
                    bb = new AABB(bb.minX, targetHeight, bb.minZ, bb.maxX, bb.maxY, bb.maxZ);
                    if (!level().getEntities(this, bb, entity -> entity instanceof EntityRideableRocket).isEmpty()) {
                        int distance = random.nextBoolean() ? -5 : 5;
                        if (random.nextBoolean()) {
                            dx += distance;
                        } else {
                            dz += distance;
                        }
                        ItemVOTVdrive.setCoordinates(navDrive, dx, dz);
                    }

                    setPos(dx + 0.5D, getY(), dz + 0.5D);
                }
            } else if (state == RocketState.TIPPING) {
                float tipTime = stateTimer * 0.1F;
                pitch = tipTime * tipTime;

                if (pitch > 90) {
                    pitch = 90;

                    if (willExplode) {
                        dropNDie(null);
                        ExplosionLarge.explode(level(), getX(), getY(), getZ(), 5, true, false, true);
                        Vec3 m = getDeltaMovement();
                        ExplosionLarge.spawnShrapnelShower(level(), getX(), getY(), getZ(), m.x, m.y, m.z, 15, 0.075);

                        level().playSound(null, getX(), getY(), getZ(), SpaceSounds.ENTITY_PIPEFAIL.get(), SoundSource.PLAYERS, 10_000, 0.8F + random.nextFloat() * 0.4F);
                    }
                }

                rocketVelocity = 0;
            } else if (state == RocketState.DOCKING) {
                // we have to wait for docking ports and their associated entities to load
                // waiting for loading using timers is bad, so maybe refactor this
                if (stateTimer > 20) {
                    rocketVelocity = 0.1;
                    pitch = 0;

                    if (targetPort == null) targetPort = RocketTravelHooks.get().getPort(level(), (int) getX(), (int) getZ());

                    // Just in case no ports have loaded in time, do nothing until they have
                    if (targetPort != null) {
                        BlockPos port = targetPort.pos();
                        setPos(port.getX() + 0.5D, getY(), port.getZ() + 0.5D);

                        targetPort.despawnRocket();
                        targetPort.reservePort();

                        if (getY() + getBbHeight() > port.getY() + 1.5D) {
                            setState(isReusable() ? RocketState.NEEDSFUEL : RocketState.LANDED);
                            destinationOverride = null;
                            setPos(getX(), port.getY() + 1.5D - getBbHeight(), getZ());

                            targetPort.dockRocket(this);
                            targetPort = null;
                        }
                    } else {
                        rocketVelocity = 0;
                    }
                } else {
                    rocketVelocity = 0;
                    pitch = 0;
                }
            } else if (state == RocketState.UNDOCKING) {
                rocketVelocity = -0.1;
                pitch = 0;
            } else if (state == RocketState.TRANSFER) {
                rocketVelocity = 0;
                pitch = 90;
                forceExitTimer = 0; // never allow exits in transfers under any circumstances!

                double progress;
                boolean arrived;
                if (stationTransfer) {
                    RocketTravelHooks hooks = RocketTravelHooks.get();
                    hooks.tickTransfer(level(), (int) getX(), (int) getZ());
                    progress = hooks.transferProgress(level(), (int) getX(), (int) getZ());
                    arrived = hooks.transferArrived(level(), (int) getX(), (int) getZ());
                    CelestialBody target = hooks.transferTarget(level(), (int) getX(), (int) getZ());
                    if (target != null) entityData.set(WATCHABLE_TRANSFER_TARGET, target.name);
                } else {
                    progress = Mth.clamp((double) stateTimer / (double) Math.max(transferTime, 1), 0, 1);
                    arrived = progress > 0.99;
                }
                entityData.set(WATCHABLE_TRANSFER, (float) progress);

                if (arrived) {
                    CelestialBody.Target from = CelestialBody.getTarget(level(), (int) getX(), (int) getZ());
                    ItemVOTVdrive.Target to = getTarget();

                    int sx = (int) getX();
                    int sz = (int) getZ();
                    boolean wasStation = stationTransfer;

                    beginLandingSequence(from, to);

                    if (wasStation) RocketTravelHooks.get().endTransfer(level(), sx, sz);
                    stationTransfer = false;
                }
            } else {
                rocketVelocity = 0;
                pitch = 0;
            }

            setXRot((float) pitch);

            if (state == RocketState.LAUNCHING) {
                Vec3 motion = getDirectionFromAxisAngle(getXRot() - 90.0F, 180.0F - getYRot(), rocketVelocity);
                setDeltaMovement(motion);
            } else {
                motionY = rocketVelocity;
                setDeltaMovement(0, motionY, 0);
            }

            if (state == RocketState.LANDING) {
                BlockPos at = BlockPos.containing(getX(), getY(), getZ());
                FluidState fluid = level().getFluidState(at);

                if (!fluid.isEmpty()) setState(RocketState.TIPPING);
                if (fluid.is(net.minecraft.tags.FluidTags.LAVA)) {
                    // setOnFireFromLava
                    PlatformHooks.setSecondsOnFire(this, 15);
                    willExplode = true;
                }
            }

            if ((state == RocketState.LAUNCHING && getY() > 900) || (state == RocketState.UNDOCKING && getY() < 32)) {
                CelestialBody.Target from = CelestialBody.getTarget(level(), (int) getX(), (int) getZ());
                ItemVOTVdrive.Target to = getTarget();

                if (!canRide() || to.body() == null || from.body == to.body()) {
                    beginLandingSequence(from, to);
                } else {
                    beginCelestialTransfer(from, to);
                }
            }

            if (getBbHeight() > 8) {
                double offset = getBbHeight() - 4;
                if (capDummy == null || capDummy.isRemoved()) {
                    capDummy = new EntityRideableRocketDummy(SpaceEntities.ENTITY_RIDEABLE_ROCKET_DUMMY.get(), level());
                    capDummy.setParent(this);
                    capDummy.setPos(getX(), getY() + offset, getZ());
                    level().addFreshEntity(capDummy);
                } else {
                    capDummy.setPos(getX(), getY() + offset, getZ());
                }
            } else if (capDummy != null) {
                capDummy.discard();
                capDummy = null;
            }
        } else {
            // ON state transitions
            if (state != lastState) {
                clientHook.onStateChanged(this, state);

                lastState = state;
                stateTimer = 0;
            } else {
                clientHook.tickSounds(this, state);
            }

            if (state == RocketState.TRANSFER) {
                if (getTransferProgress() > 0.2) {
                    if (decoupleTimer == 0) {
                        clientHook.playStage(this, 0.9F + random.nextFloat() * 0.2F);
                    }

                    if (decoupleTimer == 100 && getRocket().stages.size() > 1) {
                        clientHook.playStage(this, 1.2F + random.nextFloat() * 0.2F);
                    } else if (decoupleTimer > 100) {
                        shroudTimer++;
                    }

                    decoupleTimer++;
                }
            } else {
                decoupleTimer = 0;
                shroudTimer = 0;
            }
        }

        setStateTimer(++stateTimer);
    }

    /** Original {@code EntityThrowableNT.onUpdate} (Server): Strahl auf Bloecke, dann {@code pos += motion * motionMult()}. */
    private void moveThrowable() {
        double mult = motionMult();
        Vec3 motion = getDeltaMovement();
        Vec3 pos = position();
        Vec3 nextPos = pos.add(motion.scale(mult));

        if (mult > 0 && motion.lengthSqr() > 0) {
            // func_147447_a(pos, next, false, true, false): keine Fluessigkeiten, Bloecke ohne Kollision ignoriert
            BlockHitResult hit = level().clip(new ClipContext(pos, nextPos, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this));
            if (hit.getType() != HitResult.Type.MISS) {
                onImpact(hit);
            }
        }

        if (isRemoved()) return;

        motion = getDeltaMovement();
        setPos(getX() + motion.x * mult, getY() + motion.y * mult, getZ() + motion.z * mult);
    }

    protected double motionMult() {
        RocketState state = getState();
        if (state == RocketState.AWAITING || state == RocketState.LANDED || state == RocketState.NEEDSFUEL) return 0;
        return 4;
    }

    /** Original {@code worldObj.getHeightValue(x, z)}: oberster nicht-luftartiger Block + 1 (inkl. Fluessigkeiten). */
    private double getHeightValue(int x, int z) {
        return level().getHeight(Heightmap.Types.MOTION_BLOCKING, x, z);
    }

    private static boolean isJumping(Player player) {
        return ((com.space_m.mixin.LivingEntityAccessor) player).space_m$isJumping();
    }

    /** Original {@code BobMathUtil.getDirectionFromAxisAngle}. */
    public static Vec3 getDirectionFromAxisAngle(float pitch, float yaw, double length) {
        double ox = -Mth.sin(yaw / 180.0F * (float) Math.PI) * Mth.cos(pitch / 180.0F * (float) Math.PI) * length;
        double oz = Mth.cos(yaw / 180.0F * (float) Math.PI) * Mth.cos(pitch / 180.0F * (float) Math.PI) * length;
        double oy = -Mth.sin(pitch / 180.0F * (float) Math.PI) * length;
        return new Vec3(ox, oy, oz);
    }

    protected void onImpact(BlockHitResult mop) {
        RocketState state = getState();
        if (state != RocketState.LANDING && state != RocketState.DOCKING)
            return;

        setDeltaMovement(Vec3.ZERO);

        if (state == RocketState.DOCKING) {
            return;
        }

        // Check for a landing gear, if we don't have one, topple over catastrophically
        RocketStruct rocket = getRocket();
        if (rocket.stages.size() > 0 && rocket.stages.get(0).fins == null) {
            setState(RocketState.TIPPING);
            willExplode = true;
        } else {
            setState(RocketState.LANDED);
        }

        destinationOverride = null;

        setPos(getX(), getHeightValue((int) getX(), (int) getZ()), getZ());
    }

    // ── Kapsel / Mitfahrer ─────────────────────────────────────────────────────────────────────

    // Does this rocket accept passengers (is a capsule)
    public boolean canRide() {
        RocketPart capsule = getRocket().capsule;
        return capsule != null && capsule.warhead == RocketPart.WarheadType.APOLLO;
    }

    public boolean isReusable() {
        RocketPart capsule = getRocket().capsule;
        return capsule != null && capsule.part != null && capsule.part == SpaceRocketItems.RP_POD_20.get();
    }

    public void recallPod(ItemVOTVdrive.Destination destination) {
        setThrower(null); // REALLY FUCKED UP SHIT HAPPENING
        destinationOverride = destination;
        attemptLaunch();
    }

    @Override
    public InteractionResult interact(Player player, InteractionHand hand) {
        if (!canRide()) return InteractionResult.PASS;

        if (!level().isClientSide && (getPassengers().isEmpty() || hasPassenger(player))) {
            player.startRiding(this);
            return InteractionResult.SUCCESS;
        }
        return level().isClientSide && getPassengers().isEmpty() ? InteractionResult.SUCCESS : InteractionResult.PASS;
    }

    @Override
    protected boolean canAddPassenger(Entity passenger) {
        return getPassengers().isEmpty();
    }

    /** Original {@code getMountedYOffset}. */
    public double getMountedYOffset() {
        if (isReusable()) return getBbHeight() - 2.5;
        return getBbHeight() - 3.0;
    }

    /** Original {@code updateRiderPosition}: entlang der Raketenachse (Neigung). */
    @Override
    protected void positionRider(Entity passenger, MoveFunction move) {
        if (!hasPassenger(passenger)) return;

        // 1.7.10: riddenByEntity.getYOffset() = Augenhoehe 1.62 des Spielers (posY lag auf Augenhoehe)
        double eye = passenger instanceof Player ? 1.62D : 0;
        double length = getMountedYOffset() + eye;
        Vec3 target = getDirectionFromAxisAngle(getXRot() - 90.0F, 180.0F - getYRot(), length);

        move.accept(passenger, getX() + target.x, getY() + target.y - eye, getZ() + target.z);
    }

    /**
     * Original Ausstieg ({@code ModEventHandler}): im Orbit oben auf der Rakete ({@code height + 1}), sonst neben dem
     * Fuss ({@code height = 1}, dann {@code x + 2}) - "our players will splat upon leaving tall rockets".
     */
    @Override
    public Vec3 getDismountLocationForPassenger(LivingEntity passenger) {
        if (CelestialBody.inOrbit(level())) {
            return new Vec3(getX(), getY() + getBbHeight() + 1.0D, getZ());
        }
        return new Vec3(getX() + 2, getY() + 1.0D, getZ());
    }

    /**
     * Original {@code ModEventHandler}: Schleichen im Flug erhoeht {@code forceExitTimer}; Aussteigen nur, wenn
     * {@link #canExitCapsule()} oder nach 60 Ticks Schleichen. Aufruf aus {@link RocketEvents} (Loader-Ereignis).
     *
     * @return true = Ausstieg verhindern
     */
    public boolean blockDismount(Entity passenger) {
        if (!(passenger instanceof Player player)) return false;
        if (teleportPending || isRemoved() || !player.isAlive() || !player.isShiftKeyDown()) return false;

        // Prevent leaving a rocket in motion, for safety
        if (canExitCapsule() || forceExitTimer >= 60) return false;

        forceExitTimer++;
        return true;
    }

    @Override
    public void onCelestialTeleport(ServerPlayer rider) {
        // Ensure rocket stickiness
        setThrower(rider);
        teleportPending = false;
    }

    // ── Laufwerk ───────────────────────────────────────────────────────────────────────────────

    /** Original {@code ItemVOTVdrive.onItemRightClick} (Laufwerk wechseln, waehrend man in der Rakete sitzt). */
    @Override
    @Nullable
    public ItemStack insertNavDrive(Player player, ItemStack held) {
        if (getRocket().stages.size() > 0 || level().dimension() == SpaceDimensions.ORBIT || isReusable()) {
            if (getState() == RocketState.LANDED || getState() == RocketState.AWAITING) {
                // Replace our held stack with the rocket drive and place our held drive into the rocket
                ItemStack newStack = !navDrive.isEmpty() ? navDrive : ItemStack.EMPTY;

                navDrive = held.copy();
                navDrive.setCount(1);

                if (!level().isClientSide) {
                    setState(RocketState.AWAITING);
                }

                if (!level().isClientSide) {
                    level().playSound(null, player.getX(), player.getY(), player.getZ(), HbmSoundsNT.get("item.upgradePlug"), SoundSource.PLAYERS, 1.0F, 1.0F);
                }

                return newStack;
            }
        }
        return null;
    }

    // ── Schaden / Abbau ────────────────────────────────────────────────────────────────────────

    @Override
    public boolean isPickable() {
        return !isRemoved(); // canBeCollidedWith (EntityMissileBaseNT)
    }

    /** Original {@code getBoundingBox() == null} im Flug: nur stehende Raketen sind fest. */
    @Override
    public boolean canBeCollidedWith() {
        return motionMult() <= 0;
    }

    @Override
    public boolean isPushable() {
        return true; // canBePushed
    }

    @Override
    public boolean isPushedByFluid() {
        return false; // handleWaterMovement/handleLavaMovement = false
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (!level().isClientSide && !isRemoved()) {
            if (isInvulnerableTo(source)) {
                return false;
            } else if (getPassengers().isEmpty() && source.getEntity() instanceof Player player) {
                // A pickaxe is required to break, unless it's just the capsule (or it has tipped over)
                if ((getRocket().stages.size() == 0 && !isReusable()) || getState() == RocketState.TIPPING) {
                    dropNDie(source);
                } else {
                    ItemStack stack = player.getMainHandItem();
                    if (!stack.isEmpty() && stack.isCorrectToolForDrops(Blocks.STONE.defaultBlockState())) {
                        dropNDie(source);
                    }
                }
            }

            return true;
        }
        return true;
    }

    public void dropNDie(@Nullable DamageSource source) {
        discard();

        // Drop the rocket itself, to be taken to a pad and refueled
        // unless it's just the capsule
        RocketStruct rocket = getRocket();
        if (rocket.stages.size() == 0) {
            if (rocket.capsule != null && rocket.capsule.part != null) {
                ItemStack stack = new ItemStack(rocket.capsule.part);
                if (RocketPayloadHooks.get().isSatelliteItem(stack.getItem())) applySatData(stack);
                dropItem(stack);
            }
        } else {
            ItemStack stack = ItemCustomRocket.build(rocket, true);
            if (rocket.capsule != null && rocket.capsule.part != null && RocketPayloadHooks.get().isSatelliteItem(rocket.capsule.part)) applySatData(stack);
            dropItem(stack);
        }

        // Drop the drive if it is still present
        if (!navDrive.isEmpty()) {
            dropItem(navDrive);
        }
    }

    private void dropItem(ItemStack stack) {
        if (level().isClientSide || stack.isEmpty()) return;
        ItemEntity item = new ItemEntity(level(), getX(), getY(), getZ(), stack.copy());
        item.setDefaultPickUpDelay();
        level().addFreshEntity(item);
    }

    @Override
    public void remove(RemovalReason reason) {
        super.remove(reason);
        if (capDummy != null) {
            capDummy.discard();
        }
        releaseChunkTicket();
        clientHook.onRemoved(this);
    }

    // ── Partikel (Client) ──────────────────────────────────────────────────────────────────────

    /** Original {@code spawnContrail}. */
    protected void spawnContrail() {
        RocketState state = getState();
        double motionY = getDeltaMovement().y;

        if (state == RocketState.AWAITING
         || state == RocketState.LANDED
         || (state == RocketState.LANDING && motionY <= -0.4)
         || state == RocketState.DOCKING
         || state == RocketState.UNDOCKING
         || state == RocketState.NEEDSFUEL
         || (state == RocketState.TRANSFER && getTransferProgress() > 0.15))
            return;

        double x = getX();
        double y = getY();
        double z = getZ();

        // Offset particles when travelling upwards, preventing them spawning inside the capsule at high speeds
        if (motionY > 0) {
            x = xo;
            y = yo;
            z = zo;
        }

        RocketStruct rocket = getRocket();
        if (rocket.stages.size() == 0) {
            if (state == RocketState.TIPPING) return;

            if (isReusable()) {
                ParticleUtil.spawnGasFlame(level(), x + 0.5, y, z, 0, -1, 0);
                ParticleUtil.spawnGasFlame(level(), x - 0.5, y, z, 0, -1, 0);
                ParticleUtil.spawnGasFlame(level(), x, y, z + 0.5, 0, -1, 0);
                ParticleUtil.spawnGasFlame(level(), x, y, z - 0.5, 0, -1, 0);
            } else {
                double r = rocket.capsule != null ? rocket.capsule.bottom.radius * 0.5 : 0;
                ParticleUtil.spawnGasFlame(level(), x + r, y, z + r, 0.25, -0.75, 0.25);
                ParticleUtil.spawnGasFlame(level(), x - r, y, z + r, -0.25, -0.75, 0.25);
                ParticleUtil.spawnGasFlame(level(), x + r, y, z - r, 0.25, -0.75, -0.25);
                ParticleUtil.spawnGasFlame(level(), x - r, y, z - r, -0.25, -0.75, -0.25);
            }

            double groundHeight = getHeightValue((int) x, (int) z);
            double distanceToGround = y - groundHeight;
            if (distanceToGround < 10) {
                clientHook.spawnShock(level(), x, groundHeight + 0.5, z, 1 + random.nextInt(3), 1 + random.nextGaussian());
            }

            return;
        }

        RocketStage stage = rocket.stages.get(0);

        // the fuck is a contraol bob
        if (state == RocketState.LANDING) {
            ParticleUtil.spawnGasFlame(level(), x, y, z, 0.0, -1.0, 0.0);

            double groundHeight = getHeightValue((int) x, (int) z);
            double distanceToGround = y - groundHeight;
            if (distanceToGround < 10) {
                clientHook.spawnShock(level(), x, groundHeight + 0.5, z, 1 + random.nextInt(3), 1 + random.nextGaussian());
            }
        } else if (state == RocketState.LAUNCHING || getStateTimer() < 200) {
            spawnContraolWithOffset(0, 0, 0);

            int cluster = stage.getCluster();
            for (int c = 1; c < cluster; c++) {
                float spin = (float) c / (float) (cluster - 1);
                double radius = stage.fuselage != null ? stage.fuselage.bottom.radius : 0;
                double ox = Math.cos(spin * Math.PI * 2) * radius;
                double oz = Math.sin(spin * Math.PI * 2) * radius;
                spawnContraolWithOffset(ox, 0, oz);
            }
        }
    }

    /** Original {@code EntityMissileBaseNT.spawnContraolWithOffset} ("missileContrail"). */
    protected void spawnContraolWithOffset(double offsetX, double offsetY, double offsetZ) {
        Vec3 vec = new Vec3(xo - getX(), yo - getY(), zo - getZ());
        double len = vec.length();
        vec = vec.normalize();

        // Vec3(0,1,0).rotateAroundZ(pitch).rotateAroundY(yaw + 90) (1.7.10-Drehsinn)
        float p = getXRot() * (float) Math.PI / 180F;
        double tx = Math.sin(p);
        double ty = Math.cos(p);
        double tz = 0;
        float a = (getYRot() + 90) * (float) Math.PI / 180F;
        double rx = tx * Math.cos(a) + tz * Math.sin(a);
        double rz = tz * Math.cos(a) - tx * Math.sin(a);

        for (int i = 0; i < Math.max(Math.min(len, 10), 1); i++) {
            double j = i - len;
            CompoundTag data = new CompoundTag();
            data.putDouble("posX", getX() - vec.x * j + offsetX);
            data.putDouble("posY", getY() - vec.y * j + offsetY);
            data.putDouble("posZ", getZ() - vec.z * j + offsetZ);
            data.putString("type", "missileContrail");
            data.putFloat("scale", 1F);
            data.putDouble("moX", -rx);
            data.putDouble("moY", -ty);
            data.putDouble("moZ", -rz);
            data.putInt("maxAge", 60 + random.nextInt(20));
            clientHook.effectNT(data);
        }
    }

    // ── Rakete / Zustand (Original DataWatcher) ────────────────────────────────────────────────

    public RocketStruct getRocket() {
        CompoundTag tag = entityData.get(WATCHABLE_ROCKET);
        int[] data = tag.getIntArray("w");
        if (data.length < RocketStruct.WATCHABLE_SIZE) data = new int[RocketStruct.WATCHABLE_SIZE];
        return RocketStruct.readFromWatchable(data);
    }

    public void setRocket(RocketStruct rocket) {
        CompoundTag tag = new CompoundTag();
        tag.putIntArray("w", rocket.writeToWatchable());
        entityData.set(WATCHABLE_ROCKET, tag);
    }

    public RocketState getState() {
        int i = entityData.get(WATCHABLE_STATE);
        return RocketState.values()[Mth.clamp(i, 0, RocketState.values().length - 1)];
    }

    public void setState(RocketState state) {
        entityData.set(WATCHABLE_STATE, state.ordinal());
        entityData.set(WATCHABLE_TIMER, 0);
        stateTimer = 0;
    }

    public ItemVOTVdrive.Target getTarget() {
        if (destinationOverride != null) {
            return new ItemVOTVdrive.Target(destinationOverride.body().getBody(), false, true);
        }

        ItemStack drive = entityData.get(WATCHABLE_DRIVE);
        if (drive.isEmpty()) return new ItemVOTVdrive.Target(null, false, false);
        return ItemVOTVdrive.getTarget(drive, level());
    }

    @Nullable
    public ItemVOTVdrive.Destination getDestination() {
        if (destinationOverride != null) return destinationOverride;

        ItemStack drive = entityData.get(WATCHABLE_DRIVE);
        if (drive.isEmpty()) return null;
        return ItemVOTVdrive.getDestination(drive);
    }

    public void setDrive(ItemStack drive) {
        // ItemStack-Datenwerte werden nur bei Identitaetswechsel gesendet -> Kopie
        entityData.set(WATCHABLE_DRIVE, drive.copy());
    }

    public int getStateTimer() {
        return entityData.get(WATCHABLE_TIMER);
    }

    public void setStateTimer(int timer) {
        entityData.set(WATCHABLE_TIMER, timer);
    }

    /** Ersatz fuer {@code OrbitalStation.clientStation.getUnscaledProgress(0)}. */
    public double getTransferProgress() {
        return entityData.get(WATCHABLE_TRANSFER);
    }

    /** Ersatz fuer {@code OrbitalStation.clientStation.target}. */
    @Nullable
    public CelestialBody getTransferTarget() {
        String name = entityData.get(WATCHABLE_TRANSFER_TARGET);
        return name.isEmpty() ? null : CelestialBody.getBodyOrNull(name);
    }

    //? if < 1.21.1 {
    @Override
    protected void defineSynchedData() {
        entityData.define(WATCHABLE_STATE, RocketState.AWAITING.ordinal());
        entityData.define(WATCHABLE_DRIVE, ItemStack.EMPTY);
        entityData.define(WATCHABLE_TIMER, 0);
        entityData.define(WATCHABLE_ROCKET, new CompoundTag());
        entityData.define(WATCHABLE_TRANSFER, 0F);
        entityData.define(WATCHABLE_TRANSFER_TARGET, "");
    }
    //?} else {
    /*@Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(WATCHABLE_STATE, RocketState.AWAITING.ordinal());
        builder.define(WATCHABLE_DRIVE, ItemStack.EMPTY);
        builder.define(WATCHABLE_TIMER, 0);
        builder.define(WATCHABLE_ROCKET, new CompoundTag());
        builder.define(WATCHABLE_TRANSFER, 0F);
        builder.define(WATCHABLE_TRANSFER_TARGET, "");
    }
    *///?}

    @Override
    public void onSyncedDataUpdated(EntityDataAccessor<?> key) {
        super.onSyncedDataUpdated(key);
        if (WATCHABLE_ROCKET.equals(key)) refreshDimensions();
    }

    /** Original {@code setSize(2, rocket.getHeight() + 1)}. */
    @Override
    public EntityDimensions getDimensions(Pose pose) {
        RocketStruct rocket = getRocket();
        sizeSet = true;
        return EntityDimensions.scalable(2, (float) rocket.getHeight() + 1);
    }

    // ── NBT ────────────────────────────────────────────────────────────────────────────────────

    @Override
    protected void readAdditionalSaveData(CompoundTag nbt) {
        // EntityMissileBaseNT
        targetX = nbt.getInt("tX");
        targetZ = nbt.getInt("tZ");

        setStateTimer(nbt.getInt("timer"));
        setState(RocketState.values()[Mth.clamp(nbt.getInt("state"), 0, RocketState.values().length - 1)]);

        setRocket(RocketStruct.readFromNBT(nbt.getCompound("rocket")));

        if (nbt.contains("drive")) {
            navDrive = PlatformHooks.itemStackOf(nbt.getCompound("drive"), level().registryAccess());
        } else {
            navDrive = ItemStack.EMPTY;
        }

        satFreq = nbt.getInt("freq");
        satData = nbt.getCompound("satData");

        if (nbt.getBoolean("hasOverride")) {
            SolarSystem.Body body = SolarSystem.Body.values()[Mth.clamp(nbt.getInt("overrideBody"), 0, SolarSystem.Body.values().length - 1)];
            destinationOverride = new ItemVOTVdrive.Destination(body, nbt.getInt("overrideX"), nbt.getInt("overrideZ"));
        } else {
            destinationOverride = null;
        }

        if (nbt.hasUUID("thrower")) throwerId = nbt.getUUID("thrower");
        stationTransfer = nbt.getBoolean("stationTransfer");
        transferTime = nbt.getInt("transferTime");
        entityData.set(WATCHABLE_TRANSFER_TARGET, nbt.getString("transferTarget"));
        refreshDimensions();
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag nbt) {
        nbt.putInt("tX", targetX);
        nbt.putInt("tZ", targetZ);

        nbt.putInt("timer", getStateTimer());
        nbt.putInt("state", getState().ordinal());

        CompoundTag rocketTag = new CompoundTag();
        getRocket().writeToNBT(rocketTag);
        nbt.put("rocket", rocketTag);

        if (!navDrive.isEmpty()) {
            nbt.put("drive", PlatformHooks.saveItemStack(navDrive, new CompoundTag(), level().registryAccess()));
        }

        nbt.putInt("freq", satFreq);
        nbt.put("satData", satData.copy());

        if (destinationOverride != null) {
            nbt.putBoolean("hasOverride", true);
            nbt.putInt("overrideBody", destinationOverride.body().ordinal());
            nbt.putInt("overrideX", destinationOverride.x());
            nbt.putInt("overrideZ", destinationOverride.z());
        }

        if (throwerId != null) nbt.putUUID("thrower", throwerId);
        nbt.putBoolean("stationTransfer", stationTransfer);
        nbt.putInt("transferTime", transferTime);
        nbt.putString("transferTarget", entityData.get(WATCHABLE_TRANSFER_TARGET));
    }

    /** Original {@code SatelliteBase.getInclination/...} des Stacks: alle "sat*"-Schluessel. */
    private static CompoundTag extractSatData(ItemStack stack) {
        CompoundTag out = new CompoundTag();
        CompoundTag tag = PlatformHooks.getItemTag(stack);
        if (tag == null) return out;
        for (String key : tag.getAllKeys()) {
            if (key.startsWith("sat")) {
                Tag value = tag.get(key);
                if (value != null) out.put(key, value.copy());
            }
        }
        return out;
    }

    /** Original {@code applySatData}. */
    private void applySatData(ItemStack stack) {
        if (satData.isEmpty()) return;
        PlatformHooks.editItemTag(stack, t -> {
            for (String key : satData.getAllKeys()) {
                Tag value = satData.get(key);
                if (value != null) t.put(key, value.copy());
            }
        });
    }

    // ── Chunk-Ticket (Original IChunkLoader/ForgeChunkManager) ─────────────────────────────────

    private void updateChunkTicket() {
        if (!(level() instanceof ServerLevel server)) return;
        // Original: ForgeChunkManager-Tickets zaehlen als erzwungene Chunks und halten die Dimension wach
        // (ServerLevel tickt ohne Spieler/erzwungene Chunks nach 300 Ticks keine Entities mehr)
        server.resetEmptyTime();
        ChunkPos pos = new ChunkPos(blockPosition());
        if (!pos.equals(loadedChunk)) {
            if (loadedChunk != null) server.getChunkSource().removeRegionTicket(CHUNK_TICKET, loadedChunk, CHUNK_TICKET_RADIUS, getId());
            loadedChunk = pos;
            server.getChunkSource().addRegionTicket(CHUNK_TICKET, loadedChunk, CHUNK_TICKET_RADIUS, getId());
        }
    }

    private void releaseChunkTicket() {
        if (loadedChunk != null && level() instanceof ServerLevel server) {
            server.getChunkSource().removeRegionTicket(CHUNK_TICKET, loadedChunk, CHUNK_TICKET_RADIUS, getId());
        }
        loadedChunk = null;
    }

    //? if < 1.21.1 {
    @Override
    public void onAddedToWorld() {
        super.onAddedToWorld();
        if (!level().isClientSide) updateChunkTicket();
    }

    /** Auch bei Dimensionswechsel/Entladen (dort laeuft {@link #remove} nicht). */
    @Override
    public void onRemovedFromWorld() {
        super.onRemovedFromWorld();
        releaseChunkTicket();
    }
    //?} else {
    /*@Override
    public void onAddedToLevel() {
        super.onAddedToLevel();
        if (!level().isClientSide) updateChunkTicket();
    }

    @Override
    public void onRemovedFromLevel() {
        super.onRemovedFromLevel();
        releaseChunkTicket();
    }
    *///?}

    // ── Client-Interpolation (Original EntityThrowableInterp) ──────────────────────────────────

    //? if < 1.21.1 {
    @Override
    public void lerpTo(double x, double y, double z, float yRot, float xRot, int steps, boolean teleport) {
        this.lerpX = x;
        this.lerpY = y;
        this.lerpZ = z;
        this.lerpYRot = yRot;
        this.lerpXRot = xRot;
        this.lerpSteps = steps;
    }
    //?} else {
    /*@Override
    public void lerpTo(double x, double y, double z, float yRot, float xRot, int steps) {
        this.lerpX = x;
        this.lerpY = y;
        this.lerpZ = z;
        this.lerpYRot = yRot;
        this.lerpXRot = xRot;
        this.lerpSteps = steps;
    }
    *///?}

    private void clientLerpStep() {
        if (lerpSteps > 0) {
            double d = 1.0D / lerpSteps;
            setPos(getX() + (lerpX - getX()) * d, getY() + (lerpY - getY()) * d, getZ() + (lerpZ - getZ()) * d);
            setYRot((float) (getYRot() + Mth.wrapDegrees(lerpYRot - getYRot()) * d));
            setXRot((float) (getXRot() + (lerpXRot - getXRot()) * d));
            --lerpSteps;
        }
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double distance) {
        return true; // isInRangeToRenderDist
    }

    @Override
    public ItemStack getPickResult() {
        return ItemCustomRocket.build(getRocket());
    }

    // ── Dummy (Kapsel-Trefferbox) ──────────────────────────────────────────────────────────────

    /**
     * Original {@code EntityRideableRocketDummy}: Trefferbox 4 x 2,5 an der Kapsel hoher Raketen (Interaktion,
     * Schaden und Blick-Overlay gehen an die Rakete). Wird nie gespeichert.
     */
    public static class EntityRideableRocketDummy extends Entity {

        private static final EntityDataAccessor<Integer> WATCHABLE_PARENT_ID = SynchedEntityData.defineId(EntityRideableRocketDummy.class, EntityDataSerializers.INT);

        @Nullable public EntityRideableRocket parent;

        public EntityRideableRocketDummy(EntityType<? extends EntityRideableRocketDummy> type, Level level) {
            super(type, level);
            this.noPhysics = true;
        }

        public void setParent(EntityRideableRocket parent) {
            this.parent = parent;
            entityData.set(WATCHABLE_PARENT_ID, parent.getId());
        }

        //? if < 1.21.1 {
        @Override
        protected void defineSynchedData() {
            entityData.define(WATCHABLE_PARENT_ID, 0);
        }
        //?} else {
        /*@Override
        protected void defineSynchedData(SynchedEntityData.Builder builder) {
            builder.define(WATCHABLE_PARENT_ID, 0);
        }
        *///?}

        @Override
        public void tick() {
            if (!level().isClientSide) {
                if (parent == null || parent.isRemoved()) {
                    discard();
                }
            } else {
                if (parent == null) {
                    Entity entity = level().getEntity(entityData.get(WATCHABLE_PARENT_ID));
                    if (entity instanceof EntityRideableRocket rocket) {
                        parent = rocket;
                    }
                }
            }
        }

        @Override protected void addAdditionalSaveData(CompoundTag nbt) {}
        @Override protected void readAdditionalSaveData(CompoundTag nbt) { discard(); }
        @Override public boolean shouldBeSaved() { return false; }

        @Override
        public InteractionResult interact(Player player, InteractionHand hand) {
            if (parent == null) return InteractionResult.PASS;
            return parent.interact(player, hand);
        }

        @Override
        public boolean isPickable() {
            return true; // canBeCollidedWith
        }

        @Override
        public boolean hurt(DamageSource source, float amount) {
            if (parent == null) return false;
            return parent.hurt(source, amount);
        }

        @Override
        public ItemStack getPickResult() {
            return parent != null ? parent.getPickResult() : ItemStack.EMPTY;
        }
    }

    /** Client-Teil (Geraeusche, Partikel); Implementierung in {@code rocket.entity.client.RocketEntityClient}. */
    public interface RocketClientHook {
        default void onStateChanged(EntityRideableRocket rocket, RocketState state) {}
        default void tickSounds(EntityRideableRocket rocket, RocketState state) {}
        default void playStage(EntityRideableRocket rocket, float pitch) {}
        default void onRemoved(EntityRideableRocket rocket) {}
        default void spawnShock(Level level, double x, double y, double z, int count, double strength) {}
        default void effectNT(CompoundTag data) {}
    }
}
