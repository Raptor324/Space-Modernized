package com.space_m.war;

import java.util.List;

import com.hbm_m.entity.ModEntities;
import com.hbm_m.entity.mob.EntityUFO;
import com.hbm_m.entity.mob.glyphid.EntityGlyphid;
import com.hbm_m.entity.mob.glyphid.EntityGlyphidBehemoth;
import com.hbm_m.entity.mob.glyphid.EntityGlyphidBlaster;
import com.hbm_m.entity.mob.glyphid.EntityGlyphidBrawler;
import com.hbm_m.entity.mob.glyphid.EntityGlyphidDigger;
import com.hbm_m.entity.mob.siege.EntitySiegeCraft;
import com.hbm_m.entity.mob.siege.SiegeTier;
import com.space_m.celestial.trait.CBT_Invasion;
import com.space_m.rocket.transport.EntityCombatDropPod;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.player.Player;

/**
 * Mob-Teil von NTM Space {@code CBT_Invasion} (die Wellenlogik selbst steckt in {@link CBT_Invasion}, R1-A):
 * <ul>
 *   <li>{@code spawnCattle}: Kampf-Drop-Pod 15 Bloecke (Gauss) neben einem Spieler auf y 250, -1,5 Fall, Glyphiden je
 *       Welle (1: 2 normale; 2: 1-3 normal/Schlaeger/Graeber; ab 3: 1-5 inkl. Blaster, oder 1 Behemoth).</li>
 *   <li>{@code spawnAttempt}: Siege-Craft (Stufe Stein/Eisen) oder Siege-UFO ({@link EntitySiegeUFO}, Stufe nach Wurf
 *       und Wellenfaktor).</li>
 *   <li>{@code spawnBoss}: Mars-Invasionsschiff ({@link EntityUFO}) 50 Bloecke ueber einem Spieler, Suchpause 100.</li>
 *   <li>{@code onKill}: Siege-UFO 1 Punkt (ab 25 Leben = ab GOLD 2), Siege-Craft 10; Ende bei Boss-Abschuss.</li>
 * </ul>
 */
public class InvasionSpawner implements CBT_Invasion.Spawner {

    @Override
    public void spawnCattle(CBT_Invasion invasion, ServerLevel world) {
        List<ServerPlayer> players = world.players();
        if (players.isEmpty()) return;
        Player player = players.get(world.random.nextInt(players.size()));

        if (!(player instanceof ServerPlayer playerMP))
            return;
        if (player.getY() < 50 && !world.isFlat())
            return;

        RandomSource rand = world.random;

        EntityCombatDropPod pod = new EntityCombatDropPod(world);
        pod.setPos(playerMP.getX() + (rand.nextGaussian() * 15), 250, playerMP.getZ() + (rand.nextGaussian() * 15));
        pod.setDeltaMovement(0, -1.5, 0);

        int wave = invasion.wave;
        LivingEntity glyph;
        int amount = 1;
        if (wave == 1) {
            amount = 2;
            glyph = new EntityGlyphid(ModEntities.GLYPHID.get(), world);
        } else if (wave == 2) {
            int roll = rand.nextInt(3);
            amount = Math.max(1, rand.nextInt(4));
            switch (roll) {
                case 0: glyph = new EntityGlyphid(ModEntities.GLYPHID.get(), world); break;
                case 1: glyph = new EntityGlyphidBrawler(ModEntities.GLYPHID_BRAWLER.get(), world); break;
                default: glyph = new EntityGlyphidDigger(ModEntities.GLYPHID_DIGGER.get(), world); break;
            }
        } else if (wave >= 3) {
            int roll = rand.nextInt(5);
            amount = Math.max(1, rand.nextInt(6));
            switch (roll) {
                case 0: glyph = new EntityGlyphid(ModEntities.GLYPHID.get(), world); break;
                case 1: glyph = new EntityGlyphidBrawler(ModEntities.GLYPHID_BRAWLER.get(), world); break;
                case 2: glyph = new EntityGlyphidDigger(ModEntities.GLYPHID_DIGGER.get(), world); break;
                case 3: glyph = new EntityGlyphidBlaster(ModEntities.GLYPHID_BLASTER.get(), world); break;
                default:
                    glyph = new EntityGlyphidBehemoth(ModEntities.GLYPHID_BEHEMOTH.get(), world);
                    amount = 1;
                    break;
            }
        } else {
            glyph = new EntityGlyphid(ModEntities.GLYPHID.get(), world);
        }

        CompoundTag nbt = new CompoundTag();
        glyph.save(nbt); // schreibt "id" (Original: nbt.setString("id", EntityList.getEntityString(glyph)) + writeToNBT)

        pod.setPayload(nbt, amount, 2);
        world.addFreshEntity(pod);
    }

    @Override
    public boolean spawnSiege(CBT_Invasion invasion, ServerLevel world, double spawnX, double spawnY, double spawnZ, boolean bigUfo, float waveFactor) {
        if (bigUfo) {
            // Original new EntitySiegeCraft(world) = Space-Fassung (4 min Lebenszeit, Rueckzug, Tempo 1 + speed*3)
            EntitySiegeCraft craft = new com.space_m.entity.mob.EntitySiegeCraftSpace(com.space_m.entity.mob.SpaceMobs.MICRO_UFO.get(), world);
            craft.moveTo(spawnX, spawnY, spawnZ, world.random.nextFloat() * 360.0F, 0.0F);

            float bigRoll = world.random.nextFloat();
            SiegeTier chosen = (bigRoll < 0.5F - waveFactor) ? SiegeTier.STONE : SiegeTier.IRON;

            craft.setTier(chosen);
            return world.addFreshEntity(craft);
        } else {
            EntitySiegeUFO smallUfo = new EntitySiegeUFO(WarEntities.SIEGE_UFO.get(), world);
            smallUfo.moveTo(spawnX, spawnY, spawnZ, world.random.nextFloat() * 360.0F, 0.0F);

            float smallRoll = world.random.nextFloat();
            SiegeTier chosen;

            if (smallRoll < 0.15F - (waveFactor * 1.5F))        chosen = SiegeTier.CLAY;
            else if (smallRoll < 0.35F - (waveFactor * 1.2F))   chosen = SiegeTier.STONE;
            else if (smallRoll < 0.50F - waveFactor)            chosen = SiegeTier.IRON;
            else if (smallRoll < 0.65F - (waveFactor * 0.8F))   chosen = SiegeTier.SILVER;
            else if (smallRoll < 0.80F - (waveFactor * 0.5F))   chosen = SiegeTier.GOLD;
            else if (smallRoll < 0.90F - (waveFactor * 0.2F))   chosen = SiegeTier.DESH;
            else if (smallRoll < 0.97F)                         chosen = SiegeTier.SCHRAB;
            else                                                chosen = SiegeTier.DNT;

            smallUfo.setTier(chosen);
            return world.addFreshEntity(smallUfo);
        }
    }

    @Override
    public void spawnBoss(CBT_Invasion invasion, ServerLevel world, Player player) {
        EntityUFO entity = new EntityUFO(ModEntities.UFO.get(), world);
        entity.scanCooldown = 100;
        entity.moveTo(player.getX(), player.getY() + 50, player.getZ(), Mth.wrapDegrees(world.random.nextFloat() * 360.0F), 0.0F);
        entity.yHeadRot = entity.getYRot();
        entity.yBodyRot = entity.getYRot();
        //? if < 1.21.1 {
        entity.finalizeSpawn(world, world.getCurrentDifficultyAt(entity.blockPosition()), MobSpawnType.EVENT, null, null);
        //?} else {
        /*entity.finalizeSpawn(world, world.getCurrentDifficultyAt(entity.blockPosition()), MobSpawnType.EVENT, null);
        *///?}
        world.addFreshEntity(entity);
    }

    @Override
    public int killValue(LivingEntity entity) {
        if (entity instanceof EntitySiegeUFO) {
            // GOLD and above are worth 2 kills
            return entity.getMaxHealth() >= 100 * 0.25 ? 2 : 1;
        } else if (entity instanceof EntitySiegeCraft) {
            return 10;
        }
        return 0;
    }

    @Override
    public boolean isBoss(LivingEntity entity) {
        return entity instanceof EntityUFO;
    }

}
