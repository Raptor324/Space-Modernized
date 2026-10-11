package com.space_m.celestial.trait;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import com.space_m.celestial.CelestialBody;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;

/**
 * Planetenkrieg (Lebenspunkte, Schild, fliegende Projektile), 1:1 NTM Space {@code CBT_War}.
 * Original-Klasse: CBT_War
 *
 * <p>Abweichung: das Projektilziel ist die Dimensions-ID ({@link ResourceLocation} von {@code ResourceKey<Level>})
 * statt der int-Dimension; Schaden geht ueber {@link CelestialBody#getBody(ResourceLocation)} direkt an den Koerper
 * (Original: {@code worldServerForDimension(target)} -> {@code CelestialBody.damage(dmg, world)}, gleiche Wirkung,
 * ohne die Zieldimension laden zu muessen).
 */
public class CBT_War extends CelestialBodyTrait {

    //when you engage with one planet or the other, they are at war.
    //both planets will have this file to dictate the engagment.
    //that way they can share generic values to dictate who has what.

    //effects are all client side. these variables dictate shit like health and shield or whatevs

    public int health;
    public int shield;

    public List<Projectile> projectiles;

    public CBT_War() {
        this.health = 100;
        this.shield = 0;
        this.projectiles = new ArrayList<>();
    }

    public CBT_War(int health, int shield) {
        this.health = health;
        this.shield = shield;
        this.projectiles = new ArrayList<>();
    }

    public void launchProjectile(Projectile proj) {
        projectiles.add(proj);
    }

    public void launchProjectile(float traveltime, int size, int damage, float x, double y, double z, ProjectileType type, ResourceLocation target) {
        Projectile projectile = new Projectile(traveltime, size, damage, x, y, z, type, target);
        projectiles.add(projectile);
    }

    public void split(int amount, Projectile projectile, ProjectileType type) {

        //currently kind of temp, there might be a better way to generalize this
        if (projectile.getTravel() <= 0) {
            for (int j = 0; j < amount; j++) {
                Random rand = new Random();
                float randX = rand.nextFloat() * 160 - 80;
                float randY = rand.nextFloat() * 90 - 80;

                this.launchProjectile(Math.abs(20 + j * 10), projectile.getSize(), projectile.getDamage(), (float) (projectile.getTranslateX()), projectile.getTranslateY() - randY * j, projectile.getTranslateZ() + randX * j, type, projectile.getTarget());
                projectile.GUIangle = projectile.GUIangle;
            }

            this.destroyProjectile(projectile);
        }
    }

    @Override
    public void update(boolean isremote, CelestialBody body) {
        if (!isremote) {
            for (int i = 0; i < this.getProjectiles().size(); i++) {
                Projectile projectile = this.getProjectiles().get(i);

                projectile.update();

                if (projectile.getTravel() <= 0) {
                    projectile.impact();
                }

                if (projectile.getAnimtime() >= 100) {
                    this.destroyProjectile(projectile);
                    CelestialBody target = CelestialBody.getBody(projectile.getTarget());
                    i--;

                    if (this.health > 0) {
                        target.damage(projectile.getDamage());
                    } else if (this.health <= 0) {
                        target.getPlanet().modifyTraits(new CBT_Destroyed());
                        this.health = 0;
                    }
                }

                if (projectile.getType() == ProjectileType.SPLITSHOT) {
                    if (projectile.getTravel() <= 0) {
                        this.split(4, projectile, ProjectileType.SMALL);
                        this.destroyProjectile(projectile);
                        i--;
                    }
                }
            }
        }
    }

    public void destroyProjectile(Projectile proj) {
        projectiles.remove(proj);
    }

    public List<Projectile> getProjectiles() {
        return projectiles;
    }

    @Override
    public void writeToNBT(CompoundTag nbt) {
        nbt.putInt("health", health);
        nbt.putInt("shield", shield);

        CompoundTag projectilesTag = new CompoundTag();
        for (int i = 0; i < projectiles.size(); i++) {
            CompoundTag projTag = new CompoundTag();
            projectiles.get(i).writeToNBT(projTag);
            projectilesTag.put("projectile" + i, projTag);
        }
        nbt.put("projectiles", projectilesTag);
    }

    @Override
    public void readFromNBT(CompoundTag nbt) {
        shield = nbt.getInt("shield");
        health = nbt.getInt("health");
        CompoundTag projectilesTag = nbt.getCompound("projectiles");
        projectiles = new ArrayList<>();
        for (int i = 0; projectilesTag.contains("projectile" + i); i++) {
            CompoundTag projTag = projectilesTag.getCompound("projectile" + i);
            Projectile projectile = new Projectile();
            projectile.readFromNBT(projTag);
            projectiles.add(projectile);
        }
    }

    @Override
    public void writeToBytes(FriendlyByteBuf buf) {
        buf.writeInt(health);
        buf.writeInt(shield);
        buf.writeInt(projectiles.size());
        for (Projectile projectile : projectiles) {
            projectile.writeToBytes(buf);
        }
    }

    @Override
    public void readFromBytes(FriendlyByteBuf buf) {
        // 1:1 Original: liest shield vor health (gegenueber writeToBytes vertauscht)
        shield = buf.readInt();
        health = buf.readInt();
        int numProjectiles = buf.readInt();
        projectiles = new ArrayList<>(numProjectiles);
        for (int i = 0; i < numProjectiles; i++) {
            Projectile projectile = new Projectile();
            projectile.readFromBytes(buf);
            projectiles.add(projectile);
        }
    }

    public static class Projectile {

        private float traveltime;
        private int size;
        private int damage;
        private int animtime;
        private float flashtime;
        private double translateX;
        private double translateY;
        private double translateZ;
        private ProjectileType type;
        public int GUIangle;
        private ResourceLocation target = Level.OVERWORLD.location();

        public Projectile() {
            this.animtime = 0;
            this.flashtime = 0;
            this.type = ProjectileType.MEDIUM; // Default type
        }

        public Projectile(float traveltime, int size, int damage, double posX, double posY, double posZ, ProjectileType type, ResourceLocation target) {
            this.traveltime = traveltime;
            this.size = size;
            this.damage = damage;
            this.animtime = 0;
            this.flashtime = 0;
            this.translateX = posX;
            this.translateY = posY;
            this.translateZ = posZ;
            this.type = type;
            this.target = target;
        }

        public void writeToNBT(CompoundTag nbt) {
            nbt.putInt("damage", damage);
            nbt.putFloat("traveltime", traveltime);
            nbt.putInt("size", size);
            nbt.putDouble("translateX", translateX);
            nbt.putDouble("translateY", translateY);
            nbt.putDouble("translateZ", translateZ);
            nbt.putInt("animtime", animtime);
            nbt.putString("type", type.name());
            nbt.putString("target", target.toString());
        }

        public void readFromNBT(CompoundTag nbt) {
            damage = nbt.getInt("damage");
            traveltime = nbt.getFloat("traveltime");
            size = nbt.getInt("size");
            translateX = nbt.getDouble("translateX");
            translateY = nbt.getDouble("translateY");
            translateZ = nbt.getDouble("translateZ");
            animtime = nbt.getInt("animtime");
            type = ProjectileType.valueOf(nbt.getString("type"));
            ResourceLocation t = ResourceLocation.tryParse(nbt.getString("target"));
            target = t != null ? t : Level.OVERWORLD.location();
        }

        public void writeToBytes(FriendlyByteBuf buf) {
            buf.writeInt(damage);
            buf.writeFloat(traveltime);
            buf.writeInt(size);
            buf.writeDouble(translateX);
            buf.writeDouble(translateY);
            buf.writeDouble(translateZ);
            buf.writeInt(animtime);
            buf.writeByte(type.ordinal());
            buf.writeInt(GUIangle);
            buf.writeResourceLocation(target);
        }

        public void readFromBytes(FriendlyByteBuf buf) {
            damage = buf.readInt();
            traveltime = buf.readFloat();
            size = buf.readInt();
            translateX = buf.readDouble();
            translateY = buf.readDouble();
            translateZ = buf.readDouble();
            animtime = buf.readInt();
            type = ProjectileType.values()[buf.readByte()];
            GUIangle = buf.readInt();
            target = buf.readResourceLocation();
        }

        public ProjectileType getType() { return type; }
        public void setType(ProjectileType type) { this.type = type; }
        public float getFlashtime() { return flashtime; }

        public void update() {
            if (traveltime > 0) {
                traveltime--;
            } else {
                traveltime = 0;
                if (this.getType() != ProjectileType.SPLITSHOT) {
                    animtime = Math.min(100, animtime + 1);
                }
            }
        }

        public void impact() {
            flashtime += 0.6f;
            flashtime = Math.min(100.0f, flashtime + 0.1f * (100.0f - flashtime) * 0.15f);

            if (flashtime >= 100) {
                flashtime = 100;
            }

            if (animtime == 0) {
                flashtime = 0;
            }
        }

        public int getSize() { return size; }
        public void setSize(int size) { this.size = size; }
        public int getDamage() { return damage; }
        public float getTravel() { return traveltime; }
        public void setTravel(float travel) { this.traveltime = travel; }
        public void setDamage(int damage) { this.damage = damage; }
        public int getAnimtime() { return animtime; }
        public void setAnimtime(int animtime) { this.animtime = animtime; }
        public void setFlashtime(float flashtime) { this.flashtime = flashtime; }
        public double getTranslateX() { return translateX; }
        public ResourceLocation getTarget() { return target; }
        public double getTranslateY() { return translateY; }
        public double getTranslateZ() { return translateZ; }
    }

    public enum ProjectileType {
        SMALL,
        MEDIUM,
        HUGE,
        INCENDIARY,
        NUCLEAR,
        SPLITSHOT
    }
}
