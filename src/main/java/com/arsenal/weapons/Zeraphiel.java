package com.arsenal.weapons;

import com.arsenal.ArsenalPlugin;
import com.arsenal.core.Ability;
import com.arsenal.core.Combat;
import com.arsenal.core.Fx;
import com.arsenal.core.Trigger;
import com.arsenal.core.Weapon;
import org.bukkit.Color;
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
import java.util.concurrent.ThreadLocalRandom;

/** TROVÃO: raios guiados, corrente elétrica e teleporte relâmpago. */
public final class Zeraphiel extends Weapon {

    private final List<Ability> abilities;

    public Zeraphiel(ArsenalPlugin plugin) {
        super(plugin);
        abilities = List.of(
                new Ability(Trigger.RIGHT, "Julgamento do Trovão",
                        "Marca uma área; após 0,7s um raio devastador a atinge (9 de dano + lentidão).",
                        12, 0, this::judgment),
                new Ability(Trigger.SNEAK_RIGHT, "Corrente Fulminante",
                        "Raio que salta entre até 5 inimigos (7 → 3 de dano).",
                        16, 20, this::chain),
                new Ability(Trigger.SWAP, "Passo do Relâmpago",
                        "Teleporte de 9 blocos que eletrocuta tudo no caminho (5 de dano).",
                        10, 0, this::dash));
    }

    @Override public String id() { return "zeraphiel"; }
    @Override public String name() { return "&e&lEsfera do Trovão &8| &6Zeraphiel"; }
    @Override public Material material() { return Material.NETHERITE_SWORD; }
    @Override public String tagline() { return "Uma esfera que ainda guarda a fúria da tempestade."; }
    @Override public String passiveText() { return "20% de chance de cada golpe soltar um raio que salta para 2 inimigos próximos."; }
    @Override public List<Ability> abilities() { return abilities; }

    @Override
    public void passiveTick(Player p, int tick) {
        if (tick % 20 == 0) {
            Fx.particle(p.getLocation().add(0, 1, 0), Particle.ELECTRIC_SPARK, 6, 0.5, 0.8, 0.5, 0.1);
        }
    }

    @Override
    public void onHit(Player attacker, LivingEntity victim, EntityDamageByEntityEvent e) {
        if (ThreadLocalRandom.current().nextDouble() > 0.20) {
            return;
        }
        Location from = victim.getLocation().add(0, 1, 0);
        int jumps = 0;
        for (LivingEntity t : Combat.near(attacker, victim.getLocation(), 6)) {
            if (t.equals(victim)) {
                continue;
            }
            if (jumps++ >= 2) {
                break;
            }
            Fx.lightning(from, t.getLocation().add(0, 1, 0));
            Combat.damage(plugin, attacker, t, 3);
        }
        Fx.particle(from, Particle.ELECTRIC_SPARK, 20, 0.4, 0.6, 0.4, 0.3);
        Fx.sound(from, Sound.ENTITY_LIGHTNING_BOLT_IMPACT, 0.4f, 2f);
    }

    // ---------- Julgamento do Trovão ----------

    private boolean judgment(Player p) {
        Location target = Combat.targetPoint(p, 35);
        Fx.sound(p.getLocation(), Sound.ITEM_TRIDENT_THUNDER, 1f, 1.6f);
        plugin.tasks().repeat(p, 0, 2, 8, (i, job) -> {
            Fx.magicCircle(target.clone().add(0, 0.1, 0), 3.5, Color.YELLOW, i * 0.3);
            double r = 3.5 * (1 - i / 8.0) + 0.4;
            Fx.ring(target, r, 24, Particle.DUST, Fx.dust(255, 255, 120, 1.2f));
            Fx.particle(target.clone().add(0, 0.2, 0), Particle.ELECTRIC_SPARK, 6, 1.5, 0.1, 1.5, 0.05);
        }, () -> strike(p, target));
        return true;
    }

    private void strike(Player p, Location t) {
        if (!p.isValid()) {
            return;
        }
        t.getWorld().strikeLightningEffect(t);
        Fx.sound(t, Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 2f, 1f);
        Fx.explosion(t);
        Fx.particle(t, Particle.ELECTRIC_SPARK, 80, 1.5, 1, 1.5, 0.4);
        Fx.ring(t, 3.5, 40, Particle.ELECTRIC_SPARK, null);
        for (LivingEntity e : Combat.near(p, t, 3.5)) {
            Combat.damage(plugin, p, e, 9);
            Combat.addEffect(e, PotionEffectType.SLOWNESS, 40, 1);
            e.setVelocity(new Vector(0, 0.5, 0));
        }
    }

    // ---------- Corrente Fulminante ----------

    private boolean chain(Player p) {
        LivingEntity first = Combat.aimTarget(p, 16, 0.6);
        if (first == null) {
            Fx.bar(p, "&cNenhum alvo à vista");
            return false;
        }
        Set<UUID> hit = new HashSet<>();
        LivingEntity[] current = {first};
        Location[] from = {p.getEyeLocation().add(0, -0.4, 0)};
        double[] damage = {7, 6, 5, 4, 3};
        Fx.sound(p.getLocation(), Sound.ITEM_TRIDENT_THUNDER, 1f, 1.5f);

        plugin.tasks().repeat(p, 0, 4, 5, (i, job) -> {
            LivingEntity t = current[0];
            if (t == null || !t.isValid()) {
                job.end();
                return;
            }
            Location to = t.getBoundingBox().getCenter().toLocation(t.getWorld());
            Fx.lightning(from[0], to);
            Fx.sound(to, Sound.ENTITY_LIGHTNING_BOLT_IMPACT, 0.7f, 1.6f + i * 0.1f);
            Fx.particle(to, Particle.ELECTRIC_SPARK, 25, 0.4, 0.6, 0.4, 0.3);
            Combat.damage(plugin, p, t, damage[i]);
            hit.add(t.getUniqueId());
            from[0] = to;

            LivingEntity next = null;
            double best = 49;
            for (LivingEntity c : Combat.near(p, to, 7)) {
                if (hit.contains(c.getUniqueId())) {
                    continue;
                }
                double d = c.getLocation().distanceSquared(to);
                if (d < best) {
                    best = d;
                    next = c;
                }
            }
            current[0] = next;
        }, null);
        return true;
    }

    // ---------- Passo do Relâmpago ----------

    private boolean dash(Player p) {
        Location dest = Combat.blink(p, 9);
        if (dest.distanceSquared(p.getLocation()) < 1) {
            Fx.bar(p, "&cCaminho bloqueado");
            return false;
        }
        Location start = p.getLocation().add(0, 1, 0);
        Location end = dest.clone().add(0, 1, 0);
        Fx.lightning(start, end);
        Fx.lightning(start.clone().add(0, 0.3, 0), end.clone().add(0, 0.3, 0));
        for (LivingEntity e : Combat.inSegment(p, start, end, 1.8)) {
            Combat.damage(plugin, p, e, 5);
            Combat.addEffect(e, PotionEffectType.SLOWNESS, 30, 0);
            Fx.particle(e.getLocation().add(0, 1, 0), Particle.ELECTRIC_SPARK, 20, 0.3, 0.5, 0.3, 0.3);
        }
        Fx.sound(start, Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 0.8f, 2f);
        p.teleport(dest);
        p.setFallDistance(0);
        Fx.particle(end, Particle.ELECTRIC_SPARK, 40, 0.5, 0.8, 0.5, 0.4);
        Fx.sound(dest, Sound.ENTITY_ENDERMAN_TELEPORT, 0.8f, 1.8f);
        return true;
    }
}
