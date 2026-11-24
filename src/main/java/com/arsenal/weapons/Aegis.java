package com.arsenal.weapons;

import com.arsenal.ArsenalPlugin;
import com.arsenal.core.Ability;
import com.arsenal.core.Combat;
import com.arsenal.core.Fx;
import com.arsenal.core.Trigger;
import com.arsenal.core.Weapon;
import org.bukkit.FluidCollisionMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.projectiles.ProjectileSource;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;

import java.util.Comparator;
import java.util.List;

/** LUZ: raio solar, barreira de cúpula e pulso da aurora. */
public final class Aegis extends Weapon {

    private final List<Ability> abilities;

    public Aegis(ArsenalPlugin plugin) {
        super(plugin);
        abilities = List.of(
                new Ability(Trigger.RIGHT, "Raio Solar",
                        "Feixe de luz de 22 blocos que atravessa até 3 inimigos (7 de dano + cegueira).",
                        12, 0, this::beam),
                new Ability(Trigger.SNEAK_RIGHT, "Barreira Solar",
                        "Cúpula (raio 5, 7s): destrói projéteis inimigos, expulsa inimigos e regenera aliados.",
                        30, 0, this::barrier),
                new Ability(Trigger.SWAP, "Pulso da Aurora",
                        "Cura você (3 ♥) e aliados próximos; repele e cega inimigos (3 de dano).",
                        20, 0, this::dawn));
    }

    @Override public String id() { return "aegis"; }
    @Override public String name() { return "&6&lEsfera Solar &8| &eAegis"; }
    @Override public Material material() { return Material.DIAMOND_SWORD; }
    @Override public String tagline() { return "Onde ela brilha, a sombra recua."; }
    @Override public String passiveText() { return "A cada 10s ganha 2 ♥ de absorção. Golpes curam 10% do dano causado."; }
    @Override public List<Ability> abilities() { return abilities; }

    @Override
    public void passiveTick(Player p, int tick) {
        if (tick % 200 == 0 && p.getAbsorptionAmount() < 4) {
            p.setAbsorptionAmount(4);
            Fx.particle(p.getLocation().add(0, 1, 0), Particle.TOTEM_OF_UNDYING, 12, 0.4, 0.7, 0.4, 0.2);
        }
        if (tick % 20 == 0) {
            Fx.particle(p.getLocation().add(0, 1, 0), Particle.END_ROD, 3, 0.5, 0.7, 0.5, 0.01);
        }
    }

    @Override
    public void onHit(Player attacker, LivingEntity victim, EntityDamageByEntityEvent e) {
        Combat.heal(attacker, e.getFinalDamage() * 0.10);
        Fx.particle(victim.getLocation().add(0, 1, 0), Particle.END_ROD, 8, 0.3, 0.5, 0.3, 0.05);
    }

    // ---------- Raio Solar ----------

    private boolean beam(Player p) {
        Location eye = p.getEyeLocation();
        Vector dir = eye.getDirection().normalize();
        RayTraceResult r = p.getWorld().rayTraceBlocks(eye, dir, 22, FluidCollisionMode.NEVER, true);
        double len = r == null ? 22 : r.getHitPosition().distance(eye.toVector());
        Location start = eye.clone().add(dir.clone().multiply(0.8)).add(0, -0.25, 0);
        Location end = eye.clone().add(dir.clone().multiply(len));

        Location cur = start.clone();
        Vector step = dir.clone().multiply(0.35);
        for (double d = 0; d < len; d += 0.35) {
            Fx.particle(cur, Particle.END_ROD, 1, 0.02, 0.02, 0.02, 0);
            Fx.particle(cur, Particle.DUST, 2, 0.08, 0.08, 0.08, 0, Fx.dust(255, 220, 80, 1.8f));
            Fx.particle(cur, Particle.DUST, 1, 0, 0, 0, 0, Fx.dust(255, 255, 255, 1.0f));
            cur.add(step);
        }

        List<LivingEntity> targets = Combat.inSegment(p, start, end, 1.1);
        targets.sort(Comparator.comparingDouble(e -> e.getLocation().distanceSquared(start)));
        int count = 0;
        for (LivingEntity e : targets) {
            if (count++ >= 3) {
                break;
            }
            Combat.damage(plugin, p, e, 7);
            Combat.addEffect(e, PotionEffectType.BLINDNESS, 30, 0);
            e.setFireTicks(40);
            Fx.particle(e.getLocation().add(0, 1, 0), Particle.END_ROD, 25, 0.3, 0.6, 0.3, 0.2);
        }
        Fx.particle(end, Particle.END_ROD, 25, 0.3, 0.3, 0.3, 0.2);
        Fx.ring(end, 1.2, 16, Particle.DUST, Fx.dust(255, 230, 120, 1.4f));
        Fx.sound(eye, Sound.ENTITY_WARDEN_SONIC_BOOM, 1f, 1.8f);
        Fx.sound(eye, Sound.BLOCK_BEACON_ACTIVATE, 1f, 2f);
        return true;
    }

    // ---------- Barreira Solar ----------

    private boolean barrier(Player p) {
        Location c = p.getLocation().clone().add(0, 1, 0);
        double radius = 5;
        Fx.sound(c, Sound.BLOCK_BEACON_ACTIVATE, 1.5f, 1.4f);

        plugin.tasks().repeat(p, 0, 2, 70, (i, job) -> {
            if (i % 3 == 0) {
                Fx.sphere(c, radius, 70, Particle.DUST, Fx.dust(255, 215, 90, 1.4f));
            }
            if (i % 10 == 0) {
                Fx.ring(c.clone().subtract(0, 0.9, 0), radius, 50, Particle.END_ROD, null);
            }
            for (Entity en : c.getWorld().getNearbyEntities(c, radius + 1, radius + 1, radius + 1)) {
                double d = en.getLocation().distance(c);
                if (en instanceof Projectile proj) {
                    if (d > radius + 0.3) {
                        continue;
                    }
                    ProjectileSource shooter = proj.getShooter();
                    if (shooter instanceof Entity se && (se.equals(p) || Combat.isAlly(p, se))) {
                        continue;
                    }
                    Fx.particle(proj.getLocation(), Particle.FIREWORK, 6, 0.1, 0.1, 0.1, 0.1);
                    Fx.sound(proj.getLocation(), Sound.BLOCK_AMETHYST_BLOCK_CHIME, 1f, 1.5f);
                    proj.remove();
                } else if (en instanceof LivingEntity le && Combat.canHarm(p, le)) {
                    if (d < radius) {
                        Vector out = le.getLocation().toVector().subtract(c.toVector()).setY(0);
                        if (out.lengthSquared() < 0.01) {
                            out = new Vector(1, 0, 0);
                        }
                        le.setVelocity(out.normalize().multiply(0.7).setY(0.2));
                    }
                } else if (en instanceof Player ally && d < radius && (ally.equals(p) || Combat.isAlly(p, ally))) {
                    Combat.passiveEffect(ally, PotionEffectType.REGENERATION, 30, 1);
                    Combat.passiveEffect(ally, PotionEffectType.RESISTANCE, 30, 0);
                }
            }
        }, () -> {
            Fx.particle(c, Particle.END_ROD, 100, radius / 2, radius / 2, radius / 2, 0.2);
            Fx.sound(c, Sound.BLOCK_BEACON_DEACTIVATE, 1.5f, 1.2f);
        });
        return true;
    }

    // ---------- Pulso da Aurora ----------

    private boolean dawn(Player p) {
        Location c = p.getLocation().add(0, 1, 0);
        Combat.heal(p, 6);
        for (Entity en : c.getWorld().getNearbyEntities(c, 8, 8, 8)) {
            if (en instanceof Player ally && !ally.equals(p) && Combat.isAlly(p, ally)) {
                Combat.heal(ally, 4);
                Fx.particle(ally.getLocation().add(0, 1, 0), Particle.TOTEM_OF_UNDYING, 15, 0.4, 0.7, 0.4, 0.3);
            }
        }
        for (LivingEntity e : Combat.near(p, c, 8)) {
            Combat.damage(plugin, p, e, 3);
            Combat.addEffect(e, PotionEffectType.BLINDNESS, 40, 0);
            Combat.push(e, c, 1.3, 0.4);
        }
        Fx.particle(c, Particle.TOTEM_OF_UNDYING, 60, 0.5, 1, 0.5, 0.5);
        Fx.sound(c, Sound.ITEM_TOTEM_USE, 0.8f, 1.4f);
        Fx.sound(c, Sound.BLOCK_BEACON_POWER_SELECT, 1.5f, 1.6f);
        plugin.tasks().repeat(p, 0, 1, 8, (i, job) -> {
            double r = i + 1;
            int pts = Math.max(24, (int) (r * 10));
            Fx.ring(c.clone().subtract(0, 0.8, 0), r, pts, Particle.DUST, Fx.dust(255, 215, 90, 1.5f));
            Fx.ring(c.clone().subtract(0, 0.8, 0), r, pts / 2, Particle.END_ROD, null);
        }, null);
        return true;
    }
}
