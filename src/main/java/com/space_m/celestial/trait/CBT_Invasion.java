package com.space_m.celestial.trait;

import java.util.HashMap;

import com.space_m.celestial.CelestialBody;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

/**
 * UFO-Invasion eines Himmelskoerpers (Wellen, Abschusszaehler, Endgegner), NTM Space {@code CBT_Invasion}.
 * Original-Klasse: CBT_Invasion
 *
 * <p>Wellenlogik, Zaehler, Zeitgeber, NBT/Netz und Nachrichten sind 1:1 portiert. Alles, was Mobs braucht
 * (Drop-Pods mit Glyphiden, Siege-UFOs/-Craft, Boss-UFO, Abschusswertung), laeuft ueber {@link Spawner}; die
 * Mobs selbst kommen in R8 (Krieg/Invasion), bis dahin ist der Standard ohne Wirkung. Clientseitig (Warnton
 * {@code alarm.ping}, Chat, Meteore) ueber {@link ClientHooks}, gesetzt vom Client-Teil des Addons.
 * {@code IBossDisplayData} (Boss-Leiste) -> {@link #getMaxHealth()}/{@link #getHealth()}/{@link #getBossName()},
 * die Leiste selbst baut R8.
 */
public class CBT_Invasion extends CelestialBodyTrait {

    // while i could polymorphize to the heavens, this event is more-or-less "scripted"
    // in the sense that you would be fighting the ufo types we HAVE sequentially to the boss

    /** Mob-Anbindung (R8). Original-Methoden {@code spawnCattle}, {@code spawnAttempt}-Spawnteil, {@code spawnBoss}, {@code onKill}-Typpruefung. */
    public interface Spawner {
        /** Original {@code spawnCattle}: Drop-Pod mit Glyphiden je Welle ueber einem Spieler. */
        default void spawnCattle(CBT_Invasion invasion, ServerLevel world) { }

        /** Original {@code spawnAttempt} ab Spawnposition: Siege-Craft (bigUfo) oder Siege-UFO, Stufe nach Wurf. */
        default boolean spawnSiege(CBT_Invasion invasion, ServerLevel world, double x, double y, double z, boolean bigUfo, float waveFactor) { return false; }

        /** Original {@code spawnBoss}: Boss-UFO 50 Bloecke ueber einem Spieler. */
        default void spawnBoss(CBT_Invasion invasion, ServerLevel world, Player player) { }

        /** Abschusswert: Siege-UFO 1 (ab GOLD 2), Siege-Craft 10, sonst 0. */
        default int killValue(LivingEntity entity) { return 0; }

        /** true = Boss-UFO ({@code EntityUFO}). */
        default boolean isBoss(LivingEntity entity) { return false; }
    }

    /** Clientseitige Wirkungen (Warnung, Meteore). */
    public interface ClientHooks {
        default void warn(CBT_Invasion invasion) { }

        default void addMeteor() { }
    }

    public static Spawner spawner = new Spawner() { };
    public static ClientHooks clientHooks = new ClientHooks() { };

    public int wave;
    public int kills;
    public int killreq;
    public double waveTime;
    public boolean isInvading;
    public int lastSpawns; // prevent over-lagging the server
    public int spawndelay;
    public int podBurstCounter = 0;
    public int podCooldown = 0;
    public boolean bossSpawned = false;
    public boolean warningPlayed;

    private final RandomSource rand = RandomSource.create();

    public CBT_Invasion() {
    }

    public CBT_Invasion(int wave, double waveTime, boolean isInvading) {
        this.wave = wave;
        this.waveTime = waveTime;
        this.isInvading = isInvading;
    }

    public void prepare() {
        if (!isInvading && waveTime >= 0) {
            waveTime--;
            warningPlayed = true;
            if (waveTime <= 5) {
                warningPlayed = false;
            }
            if (waveTime <= 0) {
                isInvading = true;
            }
        }
    }

    @Override
    public void update(boolean isRemote, CelestialBody body) {
        if (!isRemote) {
            prepare();

            if (isInvading) {
                MinecraftServer server = CelestialBody.server();
                ServerLevel world = server != null && body.dimension != null ? server.getLevel(body.dimension) : null;

                if (world == null || world.players().isEmpty())
                    return;

                logicTick(world);
                handleBurstSpawning(world);
                spawnAttempt(world);
            }
        } else {
            if (!isInvading && !warningPlayed) {
                warningPlayed = true;
                clientHooks.warn(this);
            }

            if (rand.nextInt(Math.max(1, 5 - wave)) == 0 && isInvading) {
                clientHooks.addMeteor();
            }
        }
    }

    public void spawnCattle(ServerLevel world) {
        spawner.spawnCattle(this, world);
    }

    private void handleBurstSpawning(ServerLevel world) {
        if (wave > 3)
            return;

        if (podCooldown > 0) {
            podCooldown--;
            return;
        }

        if (world.getGameTime() % 10 + world.random.nextInt(3) == 0) {
            spawnCattle(world);
            podBurstCounter++;
            if (podBurstCounter >= 3 + (wave - 1)) {
                podBurstCounter = 0;
                podCooldown = 500;
            }
        }
    }

    private void logicTick(ServerLevel world) {
        if (!isInvading)
            return;

        switch (wave) {
        case 0:
            advanceWave(world);
            break;
        case 1:
            killreq = 20;
            if (kills >= killreq)
                advanceWave(world);
            break;
        case 2:
            killreq = 100;
            if (kills >= killreq)
                advanceWave(world);
            break;
        case 3:
            killreq = 150;
            if (kills >= killreq)
                advanceWave(world);
            break;
        case 4:
            killreq = 1;
            if (!bossSpawned) {
                spawnBoss(world);
                bossSpawned = true;
            }
            break;
        default:
        }
    }

    private void advanceWave(Level world) {
        wave++;
        kills = 0;
        broadcast(world, "Wave " + (wave == 4 ? "FINAL" : wave) + " is starting!", ChatFormatting.GOLD);
    }

    public void spawnAttempt(ServerLevel world) {
        if (wave > 3)
            return;
        int timer = 200;
        if (wave == 2)
            timer = 100;
        if (wave == 3)
            timer = 80;

        if (world.getGameTime() % timer == 0) {
            Player player = world.players().get(world.random.nextInt(world.players().size()));

            if (player.getY() < 50 && !world.isFlat())
                return;

            double spawnX = player.getX() + world.random.nextGaussian() * 30;
            double spawnZ = player.getZ() + world.random.nextGaussian() * 30;
            double spawnY = player.getY() + 30 + world.random.nextInt(20);

            float bigUfoChance = 0.0F;
            if (wave == 2)
                bigUfoChance = 0.1F;
            else if (wave == 3)
                bigUfoChance = 0.2F;

            float waveFactor = wave * 0.05F;
            if (waveFactor > 0.2F) waveFactor = 0.2F;

            boolean bigUfo = world.random.nextFloat() < bigUfoChance;
            spawner.spawnSiege(this, world, spawnX, spawnY, spawnZ, bigUfo, waveFactor);

            lastSpawns++;
        }
    }

    public void onKill(LivingEntity entity, CelestialBody body) {
        int value = spawner.killValue(entity);
        if (value > 0) {
            kills += value;
            body.modifyTraits(this);
        }

        if (wave >= 4 && spawner.isBoss(entity)) {
            HashMap<Class<? extends CelestialBodyTrait>, CelestialBodyTrait> currentTraits = body.getTraits();

            currentTraits.remove(CBT_Invasion.class);

            for (Player player : entity.level().players()) {
                player.sendSystemMessage(Component.literal("The Invasion Is Over!").withStyle(ChatFormatting.YELLOW));
            }

            body.setTraits(currentTraits);
        }
    }

    private void spawnBoss(ServerLevel world) {
        Player player = world.players().get(world.random.nextInt(world.players().size()));
        spawner.spawnBoss(this, world, player);
    }

    private void broadcast(Level world, String text, ChatFormatting color) {
        for (Player p : world.players()) {
            if (p instanceof ServerPlayer) {
                p.sendSystemMessage(Component.literal(text).withStyle(color, ChatFormatting.BOLD));
            }
        }
    }

    @Override
    public void writeToNBT(CompoundTag nbt) {
        nbt.putInt("wave", wave);
        nbt.putInt("kills", kills);
        nbt.putInt("killreq", killreq);
        nbt.putDouble("waveTime", waveTime);
        nbt.putBoolean("isInvading", isInvading);
        nbt.putBoolean("warningPlayed", warningPlayed);
        nbt.putInt("podBurst", podBurstCounter);
        nbt.putInt("podCooldown", podCooldown);
        nbt.putBoolean("bossSpawned", bossSpawned);
    }

    @Override
    public void readFromNBT(CompoundTag nbt) {
        wave = nbt.getInt("wave");
        kills = nbt.getInt("kills");
        killreq = nbt.getInt("killreq");
        waveTime = nbt.getDouble("waveTime");
        isInvading = nbt.getBoolean("isInvading");
        warningPlayed = nbt.getBoolean("warningPlayed");
        podBurstCounter = nbt.getInt("podBurst");
        podCooldown = nbt.getInt("podCooldown");
        bossSpawned = nbt.getBoolean("bossSpawned");
    }

    @Override
    public void writeToBytes(FriendlyByteBuf buf) {
        buf.writeInt(wave);
        buf.writeInt(kills);
        buf.writeInt(killreq);
        buf.writeDouble(waveTime);
        buf.writeBoolean(isInvading);
        buf.writeBoolean(warningPlayed);
        buf.writeInt(podBurstCounter);
        buf.writeInt(podCooldown);
        buf.writeBoolean(bossSpawned);
    }

    @Override
    public void readFromBytes(FriendlyByteBuf buf) {
        wave = buf.readInt();
        kills = buf.readInt();
        killreq = buf.readInt();
        waveTime = buf.readDouble();
        isInvading = buf.readBoolean();
        warningPlayed = buf.readBoolean();
        podBurstCounter = buf.readInt();
        podCooldown = buf.readInt();
        bossSpawned = buf.readBoolean();
    }

    /** Original {@code IBossDisplayData.getMaxHealth}. */
    public float getMaxHealth() {
        return killreq;
    }

    /** Original {@code IBossDisplayData.getHealth}. */
    public float getHealth() {
        return killreq - kills;
    }

    /** Original {@code IBossDisplayData.func_145748_c_}. */
    public Component getBossName() {
        return Component.literal("Wave " + wave);
    }
}
