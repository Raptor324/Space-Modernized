package com.space_m.machine.misc;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.hbm_m.api.tile.IControlReceiver;
import com.hbm_m.blockentity.BaseMachineBlockEntity;
import com.hbm_m.platform.ModItemStackHandler;
import com.hbm_m.platform.PlatformHooks;
import com.hbm_m.platform.RenderBounds;
import com.space_m.celestial.CelestialBody;
import com.space_m.celestial.SolarSystem;
import com.space_m.celestial.SolarSystemWorldSavedData;
import com.space_m.dim.SpaceDimensions;
import com.space_m.item.SpaceMaterialItems;
import com.space_m.orbit.OrbitalStation;
import com.space_m.rocket.ItemVOTVdrive;
import com.space_m.rocket.ItemVOTVdrive.Destination;
import com.space_m.rocket.SpaceRocketItems;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

/**
 * 1:1 NTM Space {@code TileEntityMachineStardar}: ein Slot (Stapelgrenze 1) fuer das Laufwerk.
 *
 * <p>Server: alle 300-599 Ticks ein neues Ziel fuer die Schuessel ({@code targetYaw} zufaellig, {@code targetPitch}
 * 0-80; statisch, alle StarDars zeigen gemeinsam). Steckt ein beschriebenes Laufwerk ({@code hard_drive_full}) im Slot
 * und hat sich dessen Koerper geaendert, wird um die ungefaehre Landestelle ({@code getApproximateDestination}) eine
 * 256x256-Hoehenkarte aus 16x16 Chunks des Zielkoerpers gebaut ({@link CelestialBody#getHeightmap}) und an die Clients
 * geschickt; ohne Laufwerk wird sie geloescht. Steuerung ({@code NBTControlPacket}):
 * <ul>
 *   <li>{@code pid} (+ {@code ix}/{@code iz}): leeres Laufwerk ({@code hard_drive}) auf den Koerper der Dimension
 *       {@code pid} beschreiben; Orbit-Dimension = Station (auf einer Station deren Position, sonst
 *       {@code findFreeSpace}); danach neues Schuessel-Ziel.</li>
 *   <li>{@code px}/{@code pz}: Landekoordinaten des Laufwerks = ungefaehre Stelle + Kartenpunkt - 128.</li>
 *   <li>{@code radarmode}.</li>
 * </ul>
 * Client: die Schuessel dreht mit hoechstens 0,5 Grad/Tick zum Ziel, die Neigung so, dass beide gleichzeitig ankommen.
 * Renderbereich unendlich, Sichtweite 256.
 *
 * <p>Abweichungen (technisch):
 * <ul>
 *   <li>{@code pid} ist die Dimensions-ID als Text ({@code ResourceKey<Level>}) statt der 1.7.10-Dimensionsnummer.</li>
 *   <li>{@code networkPackNT(250)} -> Update-Paket des Blockentities ({@link #sendUpdateToClient}); die Hoehenkarte
 *       reist nur mit, wenn sie neu ist ({@code updateHeightmap}, wie im Original). Sie wird als Ganzzahlfeld statt als
 *       Bytes uebertragen, weil Welthoehen seit 1.18 ausserhalb 0-255 liegen (Bytes wuerden umbrechen).</li>
 *   <li>Die OpenComputers-Rueckrufe ({@code ntm_stardar}: {@code getPlanetStats}, {@code getCurrentPlanet},
 *       {@code getSatellites}) entfallen: OpenComputers gibt es fuer 1.20.1/1.21.1 nicht, es gibt keinen Aufrufer.</li>
 * </ul>
 * Original-Klasse: TileEntityMachineStardar
 */
public class TileEntityMachineStardar extends BaseMachineBlockEntity implements IControlReceiver {

    private static long pointAtTime = 0;

    // Used to point the dish on the client
    public float dishYaw = 0;
    public float dishPitch = 0;
    public float prevDishYaw = 0;
    public float prevDishPitch = 0;

    public boolean radarMode;

    // Sent by the server for the client to smoothly lerp to
    public static float targetYaw = 0;
    public static float targetPitch = 0;

    private float maxSpeedYaw = 0.5F;

    public int[] heightmap;
    public boolean updateHeightmap = false;
    /** Original {@code previousStack} ({@code isItemEqual}: Item + Metadaten = Koerper). */
    private Item previousItem;
    private SolarSystem.Body previousBody;
    /**
     * Die Hoehenkarte gehoert in das naechste Update-Paket. Das Paket baut der Server erst beim Verteilen der Chunk-
     * Aenderungen zu Beginn des naechsten Ticks, darum wird die Markierung erst im naechsten Tick zurueckgesetzt.
     */
    private boolean heightmapInPacket = false;

    public TileEntityMachineStardar(BlockPos pos, BlockState state) {
        super(MiscBlockEntities.STARDAR.get(), pos, state, 1, 0, 0);
    }

    /** Original {@code getInventoryStackLimit() = 1}. */
    @Override
    protected ModItemStackHandler createInventoryHandler(int size) {
        return new ModItemStackHandler(size) {
            @Override
            protected void onContentsChanged(int slot) {
                setChanged();
            }

            @Override
            public boolean isItemValid(int slot, @NotNull ItemStack stack) {
                return isItemValidForSlot(slot, stack);
            }

            @Override
            public int getSlotLimit(int slot) {
                return 1;
            }
        };
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.machineStardar");
    }

    @Override
    public Component getDisplayName() {
        return getDefaultName();
    }

    @Override
    protected boolean isItemValidForSlot(int slot, ItemStack stack) {
        return true;
    }

    private ItemStack slot() {
        return inventory.getStackInSlot(0);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, TileEntityMachineStardar be) {
        be.updateEntity(level);
    }

    private void updateEntity(Level level) {
        if (!level.isClientSide) {
            heightmapInPacket = false;

            if (level.getGameTime() >= pointAtTime) {
                pointAtTime = level.getGameTime() + level.random.nextInt(300) + 300;

                targetYaw = Mth.wrapDegrees(level.random.nextFloat() * 360);
                targetPitch = level.random.nextFloat() * 80;
            }

            ItemStack stack = slot();
            if (!stack.isEmpty() && stack.getItem() == SpaceRocketItems.FULL_DRIVE.get()) {
                if (heightmap == null || !isItemEqual(stack)) {
                    previousItem = stack.getItem();
                    previousBody = ItemVOTVdrive.getBody(stack);

                    Destination destination = ItemVOTVdrive.getApproximateDestination(stack);
                    CelestialBody body = destination.body().getBody();
                    ChunkPos chunk = destination.getChunk();

                    if (body != null) {
                        heightmap = new int[256 * 256];
                        updateHeightmap = true;

                        for (int cx = 0; cx < 16; cx++) {
                            for (int cz = 0; cz < 16; cz++) {
                                int[] map = body.getHeightmap(chunk.x + cx - 8, chunk.z + cz - 8);
                                if (map == null) continue; // Original: kein Server/keine Dimension wuerde hier abstuerzen
                                int ox = cx * 16;
                                int oz = cz * 16;

                                for (int x = 0; x < 16; x++) {
                                    for (int z = 0; z < 16; z++) {
                                        heightmap[(z + oz) * 256 + (x + ox)] = map[z * 16 + x];
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                if (heightmap != null) {
                    heightmap = null;
                    updateHeightmap = true;
                }
            }

            // networkPackNT(250)
            if (updateHeightmap) heightmapInPacket = true;
            sendUpdateToClient();
            updateHeightmap = false;
        } else {
            float yawOffset = Mth.wrapDegrees(targetYaw - dishYaw);
            float moveYaw = clamp(yawOffset, -maxSpeedYaw, maxSpeedYaw);

            float pitchOffset = targetPitch - dishPitch;
            float pitchSpeed = (moveYaw / yawOffset) * Math.abs(pitchOffset);
            float movePitch = clamp(pitchOffset, -pitchSpeed, pitchSpeed);

            prevDishYaw = dishYaw;
            prevDishPitch = dishPitch;
            dishYaw += moveYaw;
            dishPitch += movePitch;
        }
    }

    /**
     * Original {@code MathHelper.clamp_float}: {@code v < min ? min : (v > max ? max : v)}. Bei NaN-Grenzen (Ziel genau
     * erreicht, {@code 0 / 0}) bleibt {@code v} erhalten; {@code Mth.clamp} gaebe NaN zurueck und die Schuessel waere weg.
     */
    private static float clamp(float v, float min, float max) {
        return v < min ? min : (v > max ? max : v);
    }

    /** Original {@code slots[0].isItemEqual(previousStack)}: gleiches Item und gleiche Metadaten (= Koerper). */
    private boolean isItemEqual(ItemStack stack) {
        return previousItem != null && stack.getItem() == previousItem && ItemVOTVdrive.getBody(stack) == previousBody;
    }

    /** Original {@code INFINITE_EXTENT_AABB}. */
    //? if forge {
    @Override
    //?}
    public AABB getRenderBoundingBox() {
        return RenderBounds.INFINITE;
    }

    /** Original {@code provideContainer}: neuer Betrachter -> Hoehenkarte vorsichtshalber neu schicken. */
    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inv, Player player) {
        if (level != null && !level.isClientSide) updateHeightmap = true; // new viewer, send them the heightmap just in case
        return new ContainerStardar(id, inv, this);
    }

    // ── NBT / Sync ───────────────────────────────────────────────────────────────────────────

    @Override
    protected void writeNbtData(@NotNull CompoundTag nbt, @Nullable net.minecraft.core.HolderLookup.Provider registries) {
        super.writeNbtData(nbt, registries);
        //nbt.setInteger("time", timeUntilPoint);

        nbt.putBoolean("radarmode", radarMode);
        nbt.putFloat("yaw", targetYaw);
        nbt.putFloat("pitch", targetPitch);

        // Original serialize(): updateHeightmap + Hoehenkarte (nur im Paket nach einer Aenderung)
        if (heightmapInPacket && level != null && !level.isClientSide) {
            nbt.putBoolean("updateHeightmap", true);
            nbt.putIntArray("heightmap", heightmap != null ? heightmap : new int[0]);
        }
    }

    @Override
    protected void readNbtData(@NotNull CompoundTag nbt, @Nullable net.minecraft.core.HolderLookup.Provider registries) {
        super.readNbtData(nbt, registries);
        //timeUntilPoint = nbt.getInteger("time");

        radarMode = nbt.getBoolean("radarmode");
        targetYaw = nbt.getFloat("yaw");
        targetPitch = nbt.getFloat("pitch");

        // Original deserialize()
        if (nbt.getBoolean("updateHeightmap") && (level == null || level.isClientSide)) {
            updateHeightmap = true;
            int[] count = nbt.getIntArray("heightmap");
            heightmap = count.length > 0 ? count : null;
        }
    }

    // ── Steuerung ────────────────────────────────────────────────────────────────────────────

    private void processDrive(ResourceKey<Level> targetDimension, int ix, int iz) {
        if (level == null) return;

        CelestialBody body = CelestialBody.getBodyOrNull(targetDimension);
        if (body == null && !SpaceDimensions.isOrbit(targetDimension)) return;

        if (slot().isEmpty() || slot().getItem() != SpaceMaterialItems.HARD_DRIVE.get()) return;
        SolarSystem.Body meta = body != null && body.getEnum() != null ? body.getEnum() : SolarSystem.Body.ORBIT;

        ItemStack drive = ItemVOTVdrive.of(meta);

        if ((ix != 0 || iz != 0) && !CelestialBody.inOrbit(level)) {
            PlatformHooks.editItemTag(drive, t -> {
                t.putInt("ax", ix);
                t.putInt("az", iz);
                t.putBoolean("Processed", true);
            });
        } else if (SpaceDimensions.isOrbit(targetDimension)) {
            ChunkPos pos;

            // if we're on a station, return our current station as a drive
            if (CelestialBody.inOrbit(level)) {
                pos = new ChunkPos(Mth.floor((float) worldPosition.getX() / OrbitalStation.STATION_SIZE), Mth.floor((float) worldPosition.getZ() / OrbitalStation.STATION_SIZE));
            } else {
                SolarSystemWorldSavedData data = SolarSystemWorldSavedData.get(level);
                if (data == null) return;
                pos = data.findFreeSpace();
            }

            ChunkPos station = pos;
            PlatformHooks.editItemTag(drive, t -> {
                t.putInt("x", station.x);
                t.putInt("z", station.z);
                t.putBoolean("Processed", true);
            });
        }

        inventory.setStackInSlot(0, drive);

        // Now point the dish at the target planet
        pointAtTime = level.random.nextInt(300) + 300;
        targetYaw = Mth.wrapDegrees(level.random.nextFloat() * 360);
        targetPitch = level.random.nextFloat() * 80;

        this.setChanged();
    }

    private void updateDriveCoords(int x, int z) {
        if (slot().isEmpty() || slot().getItem() != SpaceRocketItems.FULL_DRIVE.get()) return;

        Destination destination = ItemVOTVdrive.getApproximateDestination(slot());
        ItemVOTVdrive.setCoordinates(slot(), destination.x() + x - 8 * 16, destination.z() + z - 8 * 16);

        this.setChanged();
    }

    @Override
    public void receiveControl(CompoundTag data) {
        if (data.contains("pid")) {
            ResourceLocation id = ResourceLocation.tryParse(data.getString("pid"));
            if (id != null) processDrive(ResourceKey.create(Registries.DIMENSION, id), data.getInt("ix"), data.getInt("iz"));
        }

        if (data.contains("px") && data.contains("pz")) {
            updateDriveCoords(data.getInt("px"), data.getInt("pz"));
        }
        if (data.contains("radarmode")) {
            radarMode = data.getBoolean("radarmode");
        }
    }

    /** Original {@code hasPermission -> isUseableByPlayer} = immer true. */
    @Override
    public boolean hasPermission(Player player) {
        return true;
    }
}
