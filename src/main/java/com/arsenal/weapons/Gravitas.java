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
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.util.Vector;

import java.util.List;

/** GRAVIDADE: poço gravitacional, colapso sísmico e pulso antigravidade. */
public final class Gravitas extends Weapon {

    private final List<Ability> abilities;

    public Gravitas(ArsenalPlugin plugin) {
        super(plugin);
        abilities = List.of(
                new Ability(Trigger.RIGHT, "Poço Gravitacional",
                        "Cria um buraco negro por 5s que suga inimigos e implode (8 de dano final).",
                        22, 0, this::well),
                new Ability(Trigger.SNEAK_RIGHT, "Colapso Sísmico",
                        "Salta e despenca como um meteoro: onda de choque de 6 blocos (9 de dano).",
                        16, 30, this::slam),
                new Ability(Trigger.SWAP, "Pulso Antigravidade",
                        "Ergue e prende inimigos no ar por 1,2s (3 de dano).",
                        18, 0, this::antigravity));
    }

    @Override public String id() { return "gravitas"; }
    @Override public String name() { return "&d&lEsfera do Colapso &8| &5Gravitas"; }
    @Override public Material material() { return Material.MACE; }
    @Override public String tagline() { return "Cada golpe pesa como um planeta."; }
    @Override public String passiveText() { return "Imune a dano de queda. Golpes esmagam o alvo contra o chão."; }
    @Override public List<Ability> abilities() { return abilities; }

    @Override
    public void passiveTick(Player p, int tick) {
        if (tick % 20 == 0) {
            Fx.particle(p.getLocation().add(0, 0.1, 0), Particle.PORTAL, 8, 0.5, 0.1, 0.5, 0.3);
        }
    }

    @Override
    public void onHit(Player attacker, LivingEntity victim, EntityDamageByEntityEvent e) {
        victim.setVelocity(victim.getVelocity().add(new Vector(0, -0.35, 0)));
        Fx.particle(victim.getLocation().add(0, 0.2, 0), Particle.PORTAL, 15, 0.3, 0.1, 0.3, 0.3);
    }

    @Override
    public void onDamaged(Player victim, EntityDamageEvent e) {
        if (e.getCause() == EntityDamageEvent.DamageCause.FALL) {
            e.setCancelled(true);
            Fx.particle(victim.getLocation(), Particle.PORTAL, 20, 0.5, 0.1, 0.5, 0.3);
        }
    }

    // ---------- Poço Gravitacional ----------

    private boolean well(Player p) {
        Location c = Combat.targetPoint(p, 25).add(0, 1.8, 0);
        Fx.sound(c, Sound.BLOCK_RESPAWN_ANCHOR_CHARGE, 2f, 0.5f);

        plugin.tasks().repeat(p, 0, 2, 50, (i, job) -> {
            Fx.sphere(c, 0.7, 30, Particle.DUST, Fx.dust(10, 0, 20, 2.5f));
            for (int k = 0; k < 3; k++) {
                double ang = i * 0.5 + k * 2.094;
                double r = 5 - (i % 10) * 0.5;
                Location l = c.clone().add(Math.cos(ang) * r, Math.sin(ang * 0.7) * r * 0.4, Math.sin(ang) * r);
                Fx.particle(l, Particle.PORTAL, 6, 0.1, 0.1, 0.1, 0.3);
                Fx.particle(l, Particle.DUST, 1, 0, 0, 0, 0, Fx.dust(150, 60, 220, 1.5f));
            }
            Fx.ring(c, 2.5, 30, Particle.DUST_COLOR_TRANSITION,
                    Fx.trans(Color.PURPLE, Color.BLACK, 1.4f));
            for (LivingEntity e : Combat.near(p, c, 8)) {
                Vector v = c.toVector().subtract(e.getBoundingBox().getCenter());
                double d = v.length();
                if (d < 0.3) {
                    continue;
                }
                v.normalize().multiply(Math.min(0.55, 0.15 + 0.05 * (8 - d)));
                e.setVelocity(e.getVelocity().multiply(0.6).add(v));
                if (i % 10 == 5) {
                    Combat.damage(plugin, p, e, 2);
                }
            }
        }, () -> implode(p, c));
        return true;
    }

    private void implode(Player p, Location c) {
        if (!p.isValid()) {
            return;
        }
        Fx.explosion(c);
        Fx.sound(c, Sound.ENTITY_GENERIC_EXPLODE, 2f, 0.5f);
        Fx.sphere(c, 4, 120, Particle.DUST_COLOR_TRANSITION, Fx.trans(Color.PURPLE, Color.BLACK, 2f));
        Fx.particle(c, Particle.REVERSE_PORTAL, 150, 0.2, 0.2, 0.2, 2);
        for (LivingEntity e : Combat.near(p, c, 5)) {
            Combat.damage(plugin, p, e, 8);
            Combat.push(e, c, 1.4, 0.5);
        }
    }

    // ---------- Colapso Sísmico ----------

    private boolean slam(Player p) {
        p.setVelocity(new Vector(0, 1.5, 0));
        p.setFallDistance(0);
        Fx.sound(p.getLocation(), Sound.ENTITY_ENDER_DRAGON_FLAP, 1.5f, 0.6f);
        Fx.ring(p.getLocation().add(0, 0.2, 0), 2, 24, Particle.PORTAL, null);

        plugin.tasks().repeat(p, 0, 1, 60, (i, job) -> {
            Location l = p.getLocation();
            if (i < 12) {
                Fx.particle(l, Particle.PORTAL, 6, 0.4, 0.2, 0.4, 0.3);
                return;
            }
            p.setVelocity(new Vector(0, -2.2, 0));
            p.setFallDistance(0);
            Fx.particle(l.clone().add(0, 1, 0), Particle.DUST, 8, 0.3, 0.8, 0.3, 0, Fx.dust(140, 60, 220, 1.8f));
            if (i > 14 && Combat.grounded(p)) {
                impact(p, l);
                job.end();
            }
        }, null);
        return true;
    }

    private void impact(Player p, Location l) {
        Fx.explosion(l);
        Fx.sound(l, Sound.ENTITY_WARDEN_SONIC_BOOM, 1.5f, 0.6f);
        Fx.debris(l, 120, 3);
        Fx.particle(l, Particle.POOF, 40, 2, 0.2, 2, 0.2);
        for (LivingEntity e : Combat.near(p, l, 6)) {
            Combat.damage(plugin, p, e, 9);
            Combat.push(e, l, 0.8, 0.9);
        }
        plugin.tasks().repeat(p, 0, 1, 6, (i, job) ->
                Fx.ring(l.clone().add(0, 0.2, 0), 1 + i, 20 + i * 8, Particle.DUST,
                        Fx.dust(160, 90, 230, 1.6f)), null);
    }

    // ---------- Pulso Antigravidade ----------

    private boolean antigravity(Player p) {
        List<LivingEntity> targets = Combat.near(p, p.getLocation(), 7);
        if (targets.isEmpty()) {
            Fx.bar(p, "&cNenhum inimigo por perto");
            return false;
        }
        Fx.sound(p.getLocation(), Sound.BLOCK_BEACON_POWER_SELECT, 1.5f, 0.5f);
        for (LivingEntity e : targets) {
            Combat.damage(plugin, p, e, 3);
            e.setVelocity(new Vector(0, 0.9, 0));
        }
        plugin.tasks().repeat(p, 3, 2, 12, (i, job) -> {
            for (LivingEntity e : targets) {
                if (!e.isValid()) {
                    continue;
                }
                Vector v = e.getVelocity();
                e.setVelocity(new Vector(v.getX() * 0.5, i < 8 ? 0.08 : v.getY(), v.getZ() * 0.5));
                Fx.particle(e.getLocation(), Particle.END_ROD, 3, 0.3, 0.1, 0.3, 0.02);
            }
            Fx.ring(p.getLocation().add(0, 0.2 + i * 0.3, 0), 7, 60, Particle.DUST, Fx.dust(200, 150, 255, 1.3f));
        }, null);
        return true;
    }
}
