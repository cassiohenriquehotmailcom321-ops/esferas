package com.arsenal.core;

import com.arsenal.ArsenalPlugin;
import org.bukkit.FluidCollisionMode;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scoreboard.Team;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.List;

/** Regras de combate, alvos e utilidades geométricas compartilhadas pelas armas. */
public final class Combat {

    private static boolean internal = false;

    private Combat() {
    }

    /** true enquanto uma habilidade aplica dano (evita que passivas de golpe disparem em cadeia). */
    public static boolean isInternal() {
        return internal;
    }

    // ---------- alvos ----------

    public static boolean isAlly(Player caster, Entity other) {
        if (!(other instanceof Player op)) {
            return false;
        }
        if (op.equals(caster)) {
            return true;
        }
        Team team = caster.getScoreboard().getEntryTeam(caster.getName());
        return team != null && team.hasEntry(op.getName());
    }

    public static boolean canHarm(Player caster, Entity e) {
        if (!(e instanceof LivingEntity le) || e instanceof ArmorStand) {
            return false;
        }
        if (e.getUniqueId().equals(caster.getUniqueId()) || !le.isValid() || le.isDead() || le.isInvulnerable()) {
            return false;
        }
        if (e instanceof Player tp) {
            if (tp.getGameMode() == GameMode.CREATIVE || tp.getGameMode() == GameMode.SPECTATOR) {
                return false;
            }
            Team team = caster.getScoreboard().getEntryTeam(caster.getName());
            if (team != null && team.hasEntry(tp.getName()) && !team.allowFriendlyFire()) {
                return false;
            }
        }
        return true;
    }

    /** Inimigos válidos dentro de uma esfera. */
    public static List<LivingEntity> near(Player caster, Location center, double radius) {
        List<LivingEntity> out = new ArrayList<>();
        double r2 = radius * radius;
        for (Entity e : center.getWorld().getNearbyEntities(center, radius, radius, radius)) {
            if (!canHarm(caster, e)) {
                continue;
            }
            LivingEntity le = (LivingEntity) e;
            if (le.getBoundingBox().getCenter().distanceSquared(center.toVector()) <= r2) {
                out.add(le);
            }
        }
        return out;
    }

    /** Inimigos a até {@code radius} de um segmento (raios, dashes, feixes). */
    public static List<LivingEntity> inSegment(Player caster, Location a, Location b, double radius) {
        Vector av = a.toVector();
        Vector ab = b.toVector().subtract(av);
        double len2 = ab.lengthSquared();
        Location mid = a.clone().add(ab.clone().multiply(0.5));
        List<LivingEntity> out = new ArrayList<>();
        for (LivingEntity le : near(caster, mid, Math.sqrt(len2) / 2 + radius + 1)) {
            Vector c = le.getBoundingBox().getCenter().subtract(av);
            double t = len2 == 0 ? 0 : Math.max(0, Math.min(1, c.dot(ab) / len2));
            double d = c.subtract(ab.clone().multiply(t)).length();
            if (d <= radius) {
                out.add(le);
            }
        }
        return out;
    }

    /** Inimigo mais "na mira" do jogador (com linha de visão). */
    public static LivingEntity aimTarget(Player p, double range, double minDot) {
        Location eye = p.getEyeLocation();
        Vector dir = eye.getDirection();
        LivingEntity best = null;
        double bestScore = -10;
        for (LivingEntity le : near(p, eye, range)) {
            Vector to = le.getBoundingBox().getCenter().subtract(eye.toVector());
            double dist = to.length();
            if (dist < 0.5) {
                continue;
            }
            double dot = to.clone().normalize().dot(dir);
            if (dot < minDot || !p.hasLineOfSight(le)) {
                continue;
            }
            double score = dot - dist / (range * 4);
            if (score > bestScore) {
                bestScore = score;
                best = le;
            }
        }
        return best;
    }

    // ---------- dano / efeitos ----------

    public static void damage(ArsenalPlugin plugin, Player caster, LivingEntity target, double amount) {
        if (!canHarm(caster, target)) {
            return;
        }
        internal = true;
        try {
            target.damage(amount * plugin.damageMultiplier(), caster);
        } finally {
            internal = false;
        }
    }

    public static void addEffect(LivingEntity e, PotionEffectType type, int ticks, int amplifier) {
        e.addPotionEffect(new PotionEffect(type, ticks, amplifier, false, true, true));
    }

    /** Efeito discreto (sem partículas/ícone) para passivas e buffs. */
    public static void passiveEffect(LivingEntity e, PotionEffectType type, int ticks, int amplifier) {
        e.addPotionEffect(new PotionEffect(type, ticks, amplifier, true, false, false));
    }

    public static void heal(Player p, double amount) {
        AttributeInstance max = p.getAttribute(Attribute.MAX_HEALTH);
        double cap = max == null ? 20 : max.getValue();
        p.setHealth(Math.min(cap, p.getHealth() + amount));
    }

    /** Empurra a entidade para longe de um ponto. */
    public static void push(LivingEntity e, Location from, double horizontal, double up) {
        Vector v = e.getLocation().toVector().subtract(from.toVector()).setY(0);
        if (v.lengthSquared() < 1.0E-4) {
            v = new Vector(0, 0, 0);
        } else {
            v.normalize().multiply(horizontal);
        }
        v.setY(up);
        e.setVelocity(v);
    }

    // ---------- geometria ----------

    /** Direção horizontal para onde o jogador olha. */
    public static Vector flatDir(Player p) {
        Vector d = p.getLocation().getDirection().setY(0);
        if (d.lengthSquared() < 1.0E-4) {
            return new Vector(0, 0, 1);
        }
        return d.normalize();
    }

    public static boolean grounded(Entity e) {
        return !e.getLocation().clone().subtract(0, 0.2, 0).getBlock().isPassable();
    }

    /** Ponto que o jogador está mirando (bloco atingido, ou chão sob o ponto final). */
    public static Location targetPoint(Player p, double range) {
        Location eye = p.getEyeLocation();
        World w = eye.getWorld();
        Vector dir = eye.getDirection();
        RayTraceResult r = w.rayTraceBlocks(eye, dir, range, FluidCollisionMode.NEVER, true);
        if (r != null) {
            Location hit = r.getHitPosition().toLocation(w);
            if (r.getHitBlockFace() != null) {
                hit.add(r.getHitBlockFace().getDirection().multiply(0.1));
            }
            return hit;
        }
        Location end = eye.clone().add(dir.clone().multiply(range));
        for (int i = 0; i < 24 && end.getBlock().isPassable() && end.getY() > w.getMinHeight(); i++) {
            end.subtract(0, 1, 0);
        }
        end.setY(end.getBlockY() + 1.05);
        return end;
    }

    /** Destino seguro de um teleporte em linha reta (para na primeira parede). */
    public static Location blink(Player p, double distance) {
        Location eye = p.getEyeLocation();
        Vector dir = eye.getDirection().normalize();
        RayTraceResult r = p.getWorld().rayTraceBlocks(eye, dir, distance, FluidCollisionMode.NEVER, true);
        double max = r == null ? distance : Math.max(0, r.getHitPosition().distance(eye.toVector()) - 0.6);
        for (double d = max; d >= 0; d -= 0.25) {
            Location head = eye.clone().add(dir.clone().multiply(d));
            Location feet = head.clone().subtract(0, 1.62, 0);
            if (head.getBlock().isPassable() && feet.getBlock().isPassable()
                    && feet.clone().add(0, 1, 0).getBlock().isPassable()) {
                feet.setYaw(eye.getYaw());
                feet.setPitch(eye.getPitch());
                return feet;
            }
        }
        return p.getLocation();
    }
}
