package com.arsenal.weapons;

import com.arsenal.ArsenalPlugin;
import com.arsenal.core.Ability;
import com.arsenal.core.Combat;
import com.arsenal.core.Fx;
import com.arsenal.core.Trigger;
import com.arsenal.core.Weapon;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/** FOGO: meteoro, onda flamejante e ascensão da fênix. */
public final class Ignaris extends Weapon {

    private final List<Ability> abilities;

    public Ignaris(ArsenalPlugin plugin) {
        super(plugin);
        abilities = List.of(
                new Ability(Trigger.RIGHT, "Meteoro Rubro",
                        "Invoca um meteoro que cai na mira: até 11 de dano em área + incêndio.",
                        14, 0, this::meteor),
                new Ability(Trigger.SNEAK_RIGHT, "Onda Flamejante",
                        "Cone de fogo de 9 blocos que empurra e queima (6 de dano).",
                        10, 0, this::wave),
                new Ability(Trigger.SWAP, "Ascensão da Fênix",
                        "Impulsiona você aos céus com asas de fogo; queima quem está por perto (5 de dano).",
                        18, 20, this::phoenix));
    }

    @Override public String id() { return "ignaris"; }
    @Override public String name() { return "&c&lEsfera da Fênix &8| &6Ignaris"; }
    @Override public Material material() { return Material.GOLDEN_SWORD; }
    @Override public String tagline() { return "Forjada no coração de um vulcão."; }
    @Override public String passiveText() { return "Resistência ao fogo enquanto empunhada. Golpes incendeiam o alvo por 4s."; }
    @Override public List<Ability> abilities() { return abilities; }

    @Override
    public void passiveTick(Player p, int tick) {
        Combat.passiveEffect(p, PotionEffectType.FIRE_RESISTANCE, 60, 0);
        if (tick % 20 == 0) {
            Fx.particle(p.getLocation().add(0, 0.2, 0), Particle.FLAME, 5, 0.4, 0.1, 0.4, 0.01);
        }
    }

    @Override
    public void onHit(Player attacker, LivingEntity victim, EntityDamageByEntityEvent e) {
        victim.setFireTicks(80);
        Fx.particle(victim.getLocation().add(0, 1, 0), Particle.FLAME, 15, 0.3, 0.5, 0.3, 0.05);
    }

    // ---------- Meteoro Rubro ----------

    private boolean meteor(Player p) {
        Location target = Combat.targetPoint(p, 40);
        Location pos = target.clone().add(10, 28, 6);
        Vector step = target.toVector().subtract(pos.toVector()).multiply(1.0 / 24);
        Fx.sound(p.getLocation(), Sound.ITEM_FIRECHARGE_USE, 1f, 0.6f);
        Fx.sound(target, Sound.ENTITY_GHAST_SHOOT, 2f, 0.5f);

        plugin.tasks().repeat(p, 0, 1, 24, (i, job) -> {
            pos.add(step);
            Fx.particle(pos, Particle.FLAME, 40, 0.6, 0.6, 0.6, 0.05);
            Fx.particle(pos, Particle.LARGE_SMOKE, 12, 0.5, 0.5, 0.5, 0.02);
            Fx.particle(pos, Particle.LAVA, 4, 0.5, 0.5, 0.5, 0);
            Fx.sphere(pos, 1.1, 24, Particle.DUST, Fx.dust(255, 90, 20, 2f));
            if (i % 4 == 0) {
                Fx.ring(target, 5, 40, Particle.DUST, Fx.dust(255, 60, 0, 1.5f));
            }
        }, () -> impact(p, target));
        return true;
    }

    private void impact(Player p, Location t) {
        if (!p.isValid()) {
            return;
        }
        Fx.explosion(t);
        Fx.particle(t, Particle.FLAME, 200, 2.5, 0.5, 2.5, 0.25);
        Fx.particle(t, Particle.LARGE_SMOKE, 60, 2, 1, 2, 0.1);
        Fx.debris(t, 80, 2.5);
        for (LivingEntity e : Combat.near(p, t, 5)) {
            double d = e.getLocation().distance(t);
            Combat.damage(plugin, p, e, d <= 2.5 ? 11 : 8);
            e.setFireTicks(100);
            Combat.push(e, t, 1.1, 0.6);
        }
        plugin.tasks().repeat(p, 0, 1, 6, (i, job) ->
                Fx.ring(t, 1 + i, 20 + i * 8, Particle.FLAME, null), null);
    }

    // ---------- Onda Flamejante ----------

    private boolean wave(Player p) {
        Vector dir = Combat.flatDir(p);
        Location origin = p.getLocation().add(0, 1, 0).add(dir.clone().multiply(0.8));
        Set<UUID> hit = new HashSet<>();
        Fx.sound(origin, Sound.ENTITY_BLAZE_SHOOT, 1.2f, 0.7f);
        Fx.sound(origin, Sound.ITEM_FIRECHARGE_USE, 1f, 0.8f);

        plugin.tasks().repeat(p, 0, 1, 9, (i, job) -> {
            double d = 1.5 + i;
            for (int a = -30; a <= 30; a += 4) {
                Vector v = dir.clone().rotateAroundY(Math.toRadians(a)).multiply(d);
                Location l = origin.clone().add(v);
                Fx.particle(l, Particle.FLAME, 3, 0.15, 0.3, 0.15, 0.02);
                if ((a / 4 + i) % 3 == 0) {
                    Fx.particle(l.clone().add(0, 0.5, 0), Particle.LARGE_SMOKE, 1, 0, 0, 0, 0);
                }
            }
            for (LivingEntity e : Combat.near(p, origin, d + 1.2)) {
                if (hit.contains(e.getUniqueId())) {
                    continue;
                }
                Vector to = e.getLocation().toVector().subtract(origin.toVector()).setY(0);
                double len = to.length();
                if (len < 0.01 || len > d + 1) {
                    continue;
                }
                if (Math.toDegrees(dir.angle(to.normalize())) > 35) {
                    continue;
                }
                hit.add(e.getUniqueId());
                Combat.damage(plugin, p, e, 6);
                e.setFireTicks(80);
                e.setVelocity(dir.clone().multiply(1.0).setY(0.3));
            }
        }, null);
        return true;
    }

    // ---------- Ascensão da Fênix ----------

    private boolean phoenix(Player p) {
        Vector dir = Combat.flatDir(p);
        p.setVelocity(new Vector(dir.getX() * 0.6, 1.25, dir.getZ() * 0.6));
        p.setFallDistance(0);
        Combat.passiveEffect(p, PotionEffectType.SLOW_FALLING, 120, 0);

        Location l = p.getLocation();
        Fx.explosion(l);
        Fx.ring(l, 3, 30, Particle.FLAME, null);
        Fx.sound(l, Sound.ENTITY_ENDER_DRAGON_FLAP, 1.5f, 0.8f);
        for (LivingEntity e : Combat.near(p, l, 4)) {
            Combat.damage(plugin, p, e, 5);
            e.setFireTicks(60);
            Combat.push(e, l, 1.0, 0.5);
        }

        plugin.tasks().repeat(p, 2, 1, 28, (i, job) -> {
            if (i > 8 && Combat.grounded(p)) {
                job.end();
                return;
            }
            Location c = p.getLocation().add(0, 1, 0);
            Vector fwd = Combat.flatDir(p);
            Vector right = new Vector(-fwd.getZ(), 0, fwd.getX());
            double flap = Math.sin(i * 0.5) * 0.5;
            for (int side = -1; side <= 1; side += 2) {
                for (double t = 0; t <= 1.0; t += 0.1) {
                    Location w = c.clone()
                            .add(right.clone().multiply(side * t * 2.4))
                            .add(0, 0.6 * (1 - t) + Math.sin(t * Math.PI) * 0.9 + flap * t, 0)
                            .subtract(fwd.clone().multiply(0.4 + t * 0.5));
                    Fx.particle(w, Particle.FLAME, 2, 0.05, 0.05, 0.05, 0.005);
                    Fx.particle(w, Particle.DUST, 1, 0, 0, 0, 0, Fx.dust(255, 140, 20, 1.4f));
                }
            }
            Fx.particle(c.clone().subtract(0, 1, 0), Particle.LAVA, 2, 0.3, 0.1, 0.3, 0);
        }, null);
        return true;
    }
}
