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
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/** VENTO: rajada cortante, tornado e salto da rajada. */
public final class Zephyria extends Weapon {

    private final List<Ability> abilities;

    public Zephyria(ArsenalPlugin plugin) {
        super(plugin);
        abilities = List.of(
                new Ability(Trigger.RIGHT, "Rajada Cortante",
                        "Lâmina de vento que atravessa inimigos e os arremessa (6 de dano).",
                        7, 0, this::bolt),
                new Ability(Trigger.SNEAK_RIGHT, "Tornado",
                        "Tornado que avança por 25 blocos erguendo e girando inimigos (1,5 de dano/pulso).",
                        24, 0, this::tornado),
                new Ability(Trigger.SWAP, "Salto da Rajada",
                        "Salto gigante na direção da mira com queda suave; afasta inimigos ao decolar.",
                        12, 20, this::leap));
    }

    @Override public String id() { return "zephyria"; }
    @Override public String name() { return "&f&lEsfera do Ciclone &8| &bZephyria"; }
    @Override public Material material() { return Material.BREEZE_ROD; }
    @Override public String tagline() { return "O vento obedece a quem a empunha."; }
    @Override public String passiveText() { return "Velocidade I, imune a dano de queda. Golpes lançam o alvo para o alto."; }
    @Override public List<Ability> abilities() { return abilities; }

    @Override
    public void passiveTick(Player p, int tick) {
        Combat.passiveEffect(p, PotionEffectType.SPEED, 60, 0);
        if (tick % 10 == 0 && p.getVelocity().lengthSquared() > 0.01) {
            Fx.particle(p.getLocation().add(0, 0.2, 0), Particle.CLOUD, 4, 0.3, 0.1, 0.3, 0.01);
        }
    }

    @Override
    public void onHit(Player attacker, LivingEntity victim, EntityDamageByEntityEvent e) {
        victim.setVelocity(victim.getVelocity().add(new Vector(0, 0.35, 0)));
        Fx.particle(victim.getLocation().add(0, 0.5, 0), Particle.POOF, 10, 0.3, 0.2, 0.3, 0.1);
    }

    @Override
    public void onDamaged(Player victim, EntityDamageEvent e) {
        if (e.getCause() == EntityDamageEvent.DamageCause.FALL) {
            e.setCancelled(true);
            Fx.particle(victim.getLocation(), Particle.CLOUD, 15, 0.5, 0.1, 0.5, 0.1);
        }
    }

    // ---------- Rajada Cortante ----------

    private boolean bolt(Player p) {
        Location eye = p.getEyeLocation();
        Vector dir = eye.getDirection().normalize();
        Location pos = eye.clone().add(dir.clone().multiply(1));
        Set<UUID> hit = new HashSet<>();
        Fx.sound(eye, Sound.ENTITY_WIND_CHARGE_WIND_BURST, 1f, 1.4f);
        Fx.sound(eye, Sound.ENTITY_PLAYER_ATTACK_SWEEP, 1f, 1.6f);

        plugin.tasks().repeat(p, 0, 1, 16, (i, job) -> {
            for (int k = 0; k < 2; k++) {
                pos.add(dir.clone().multiply(0.7));
                if (!pos.getBlock().isPassable()) {
                    Fx.particle(pos, Particle.POOF, 15, 0.3, 0.3, 0.3, 0.1);
                    job.end();
                    return;
                }
                Fx.particle(pos, Particle.CLOUD, 4, 0.15, 0.15, 0.15, 0.01);
                Fx.particle(pos, Particle.DUST, 2, 0.2, 0.2, 0.2, 0, Fx.dust(200, 255, 235, 1.5f));
                for (LivingEntity e : Combat.near(p, pos, 1.5)) {
                    if (!hit.add(e.getUniqueId())) {
                        continue;
                    }
                    Combat.damage(plugin, p, e, 6);
                    e.setVelocity(dir.clone().multiply(1.6).setY(0.45));
                    Fx.particle(e.getLocation().add(0, 1, 0), Particle.POOF, 20, 0.3, 0.5, 0.3, 0.15);
                    Fx.sound(e.getLocation(), Sound.ENTITY_WIND_CHARGE_WIND_BURST, 0.8f, 1.8f);
                }
            }
        }, null);
        return true;
    }

    // ---------- Tornado ----------

    private boolean tornado(Player p) {
        Vector dir = Combat.flatDir(p);
        Location pos = p.getLocation().add(dir.clone().multiply(2.5));
        Fx.sound(pos, Sound.ENTITY_WIND_CHARGE_WIND_BURST, 2f, 0.5f);

        plugin.tasks().repeat(p, 0, 2, 50, (i, job) -> {
            pos.add(dir.clone().multiply(0.5));
            if (!pos.getBlock().isPassable()) {
                pos.add(0, 1, 0);
                if (!pos.getBlock().isPassable()) {
                    job.end();
                    return;
                }
            }
            for (double h = 0; h < 7; h += 0.5) {
                double r = 0.6 + h * 0.4;
                for (int k = 0; k < 2; k++) {
                    double ang = i * 0.6 + h * 0.9 + k * Math.PI;
                    Location l = pos.clone().add(Math.cos(ang) * r, h, Math.sin(ang) * r);
                    Fx.particle(l, Particle.CLOUD, 1, 0, 0, 0, 0);
                    if (((int) (h * 2)) % 3 == 0) {
                        Fx.particle(l, Particle.DUST, 1, 0, 0, 0, 0, Fx.dust(220, 255, 240, 1.6f));
                    }
                }
            }
            if (i % 4 == 0) {
                Fx.sound(pos, Sound.ENTITY_PHANTOM_FLAP, 1.5f, 0.6f);
            }
            for (LivingEntity e : Combat.near(p, pos.clone().add(0, 3.5, 0), 4.5)) {
                double dx = e.getLocation().getX() - pos.getX();
                double dz = e.getLocation().getZ() - pos.getZ();
                if (Math.hypot(dx, dz) > 3.6) {
                    continue;
                }
                Vector tangent = new Vector(-dz, 0, dx);
                if (tangent.lengthSquared() > 0.01) {
                    tangent.normalize().multiply(0.35);
                }
                Vector inward = new Vector(-dx, 0, -dz);
                if (inward.lengthSquared() > 0.01) {
                    inward.normalize().multiply(0.15);
                }
                double rel = e.getLocation().getY() - pos.getY();
                e.setVelocity(new Vector(tangent.getX() + inward.getX(), rel > 6.5 ? 0.0 : 0.55,
                        tangent.getZ() + inward.getZ()));
                if (i % 5 == 0) {
                    Combat.damage(plugin, p, e, 1.5);
                }
            }
        }, null);
        return true;
    }

    // ---------- Salto da Rajada ----------

    private boolean leap(Player p) {
        Vector v = p.getEyeLocation().getDirection().normalize().multiply(1.5);
        v.setY(Math.max(0.6, v.getY() + 0.5));
        Location l = p.getLocation();
        for (LivingEntity e : Combat.near(p, l, 3)) {
            Combat.push(e, l, 0.9, 0.4);
        }
        p.setVelocity(v);
        p.setFallDistance(0);
        Combat.passiveEffect(p, PotionEffectType.SLOW_FALLING, 80, 0);
        Fx.ring(l.clone().add(0, 0.2, 0), 2.5, 30, Particle.CLOUD, null);
        Fx.particle(l, Particle.POOF, 30, 0.5, 0.1, 0.5, 0.2);
        Fx.sound(l, Sound.ENTITY_WIND_CHARGE_WIND_BURST, 1.5f, 0.8f);

        plugin.tasks().repeat(p, 1, 1, 25, (i, job) -> {
            Location cur = p.getLocation();
            Fx.particle(cur, Particle.CLOUD, 6, 0.3, 0.1, 0.3, 0.02);
            Fx.particle(cur, Particle.DUST, 3, 0.3, 0.2, 0.3, 0, Fx.dust(210, 255, 240, 1.4f));
            if (i > 6 && Combat.grounded(p)) {
                Fx.ring(cur.clone().add(0, 0.2, 0), 2, 24, Particle.CLOUD, null);
                job.end();
            }
        }, null);
        return true;
    }
}
