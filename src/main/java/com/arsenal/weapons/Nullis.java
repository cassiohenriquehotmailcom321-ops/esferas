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
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/** VAZIO: fenda dimensional, singularidade e troca de posições. */
public final class Nullis extends Weapon {

    private static final Color VOID_PURPLE = Color.fromRGB(190, 70, 255);
    private static final Color VOID_BLACK = Color.fromRGB(10, 0, 20);

    private final List<Ability> abilities;

    public Nullis(ArsenalPlugin plugin) {
        super(plugin);
        abilities = List.of(
                new Ability(Trigger.RIGHT, "Fenda Dimensional",
                        "Teleporte de 12 blocos; deixa uma fenda por 3s que fere e puxa inimigos (3 de dano/pulso).",
                        8, 0, this::rift),
                new Ability(Trigger.SNEAK_RIGHT, "Singularidade",
                        "Esfera sombria que puxa inimigos e detona (8 de dano, escuridão e levitação).",
                        20, 0, this::orb),
                new Ability(Trigger.SWAP, "Troca Dimensional",
                        "Troca de lugar com o inimigo na mira (lentidão + náusea nele).",
                        18, 0, this::swap));
    }

    @Override public String id() { return "nullis"; }
    @Override public String name() { return "&5&lEsfera do Vazio &8| &dNullis"; }
    @Override public Material material() { return Material.END_ROD; }
    @Override public String tagline() { return "Um pedaço do nada, condensado."; }
    @Override public String passiveText() { return "12% de chance de o vazio engolir um ataque ou projétil recebido."; }
    @Override public List<Ability> abilities() { return abilities; }

    @Override
    public void passiveTick(Player p, int tick) {
        double a = tick * 0.2;
        Location l = p.getLocation().add(Math.cos(a) * 0.8, 1.0 + Math.sin(a * 0.5) * 0.4, Math.sin(a) * 0.8);
        Fx.particle(l, Particle.PORTAL, 3, 0.05, 0.05, 0.05, 0.1);
    }

    @Override
    public void onDamaged(Player victim, EntityDamageEvent e) {
        EntityDamageEvent.DamageCause cause = e.getCause();
        if (cause != EntityDamageEvent.DamageCause.ENTITY_ATTACK
                && cause != EntityDamageEvent.DamageCause.PROJECTILE) {
            return;
        }
        if (ThreadLocalRandom.current().nextDouble() < 0.12) {
            e.setCancelled(true);
            Location l = victim.getLocation().add(0, 1, 0);
            Fx.particle(l, Particle.REVERSE_PORTAL, 30, 0.4, 0.7, 0.4, 0.2);
            Fx.sound(l, Sound.ENTITY_ENDERMAN_TELEPORT, 0.8f, 1.6f);
            Fx.bar(victim, "&5✦ Esquiva do Vazio");
        }
    }

    // ---------- Fenda Dimensional ----------

    private boolean rift(Player p) {
        Location dest = Combat.blink(p, 12);
        if (dest.distanceSquared(p.getLocation()) < 1) {
            Fx.bar(p, "&cCaminho bloqueado");
            return false;
        }
        Location from = p.getLocation().add(0, 1, 0);
        Fx.sphere(from, 1.2, 40, Particle.DUST_COLOR_TRANSITION, Fx.trans(VOID_PURPLE, VOID_BLACK, 1.6f));
        Fx.particle(from, Particle.REVERSE_PORTAL, 60, 0.5, 0.8, 0.5, 0.3);
        Fx.line(from, dest.clone().add(0, 1, 0), 0.4, Particle.DUST_COLOR_TRANSITION,
                Fx.trans(VOID_PURPLE, VOID_BLACK, 1.2f));
        Fx.sound(from, Sound.ENTITY_ENDERMAN_TELEPORT, 1f, 0.5f);

        p.teleport(dest);
        p.setFallDistance(0);
        Fx.particle(dest.clone().add(0, 1, 0), Particle.REVERSE_PORTAL, 60, 0.5, 0.8, 0.5, 0.3);
        Fx.sound(dest, Sound.ENTITY_ENDERMAN_TELEPORT, 1f, 1.4f);

        Location rc = from.clone();
        plugin.tasks().repeat(p, 5, 10, 6, (i, job) -> {
            Fx.sphere(rc, 1.5 + (i % 2) * 0.3, 30, Particle.DUST_COLOR_TRANSITION,
                    Fx.trans(VOID_PURPLE, VOID_BLACK, 1.4f));
            Fx.particle(rc, Particle.PORTAL, 20, 1, 1, 1, 0.5);
            Fx.sound(rc, Sound.BLOCK_RESPAWN_ANCHOR_CHARGE, 0.5f, 1.5f);
            for (LivingEntity e : Combat.near(p, rc, 3.2)) {
                Combat.damage(plugin, p, e, 3);
                Vector v = rc.toVector().subtract(e.getLocation().toVector()).setY(0.1);
                if (v.lengthSquared() > 0.01) {
                    e.setVelocity(v.normalize().multiply(0.5));
                }
            }
        }, null);
        return true;
    }

    // ---------- Singularidade ----------

    private boolean orb(Player p) {
        Location eye = p.getEyeLocation();
        Location pos = eye.clone().add(eye.getDirection().multiply(1.2));
        Vector vel = eye.getDirection().multiply(0.65);
        Fx.sound(pos, Sound.ENTITY_ENDER_DRAGON_SHOOT, 1f, 0.6f);

        plugin.tasks().repeat(p, 0, 1, 45, (i, job) -> {
            pos.add(vel);
            Fx.sphere(pos, 0.6, 20, Particle.DUST, Fx.dust(15, 0, 30, 2.2f));
            Fx.particle(pos, Particle.REVERSE_PORTAL, 6, 0.3, 0.3, 0.3, 0.15);
            Fx.particle(pos, Particle.DUST_COLOR_TRANSITION, 3, 0.8, 0.8, 0.8, 0,
                    Fx.trans(Color.fromRGB(200, 80, 255), VOID_BLACK, 1.2f));
            if (i % 8 == 0) {
                for (LivingEntity e : Combat.near(p, pos, 4)) {
                    Vector v = pos.toVector().subtract(e.getLocation().toVector());
                    if (v.lengthSquared() > 0.01) {
                        e.setVelocity(v.normalize().multiply(0.35));
                    }
                }
            }
            boolean blocked = !pos.getBlock().isPassable();
            boolean enemy = !Combat.near(p, pos, 1.4).isEmpty();
            if (blocked || enemy) {
                job.end();
            }
        }, () -> detonate(p, pos.clone()));
        return true;
    }

    private void detonate(Player p, Location pos) {
        if (!p.isValid()) {
            return;
        }
        Fx.explosion(pos);
        Fx.sound(pos, Sound.ENTITY_WITHER_SPAWN, 0.8f, 1.6f);
        Fx.sphere(pos, 4, 100, Particle.DUST_COLOR_TRANSITION, Fx.trans(VOID_PURPLE, VOID_BLACK, 2f));
        Fx.particle(pos, Particle.REVERSE_PORTAL, 200, 0.2, 0.2, 0.2, 2);
        for (LivingEntity e : Combat.near(p, pos, 4.5)) {
            Combat.damage(plugin, p, e, 8);
            Combat.addEffect(e, PotionEffectType.DARKNESS, 80, 0);
            Combat.addEffect(e, PotionEffectType.LEVITATION, 30, 0);
        }
    }

    // ---------- Troca Dimensional ----------

    private boolean swap(Player p) {
        LivingEntity t = Combat.aimTarget(p, 25, 0.9);
        if (t == null) {
            Fx.bar(p, "&cNenhum alvo à vista");
            return false;
        }
        Location a = p.getLocation().clone();
        Location b = t.getLocation().clone();
        Fx.magicCircle(a.clone().add(0, 0.1, 0), 1.6, VOID_PURPLE, 0);
        Fx.magicCircle(b.clone().add(0, 0.1, 0), 1.6, VOID_PURPLE, 0);
        Fx.line(a.clone().add(0, 1, 0), b.clone().add(0, 1, 0), 0.4, Particle.DUST_COLOR_TRANSITION,
                Fx.trans(VOID_PURPLE, VOID_BLACK, 1.3f));
        Fx.sound(a, Sound.ENTITY_ENDERMAN_TELEPORT, 1f, 0.7f);
        Fx.sound(b, Sound.ENTITY_ENDERMAN_TELEPORT, 1f, 0.7f);

        Location toCaster = b.clone();
        toCaster.setYaw(a.getYaw());
        toCaster.setPitch(a.getPitch());
        Location toTarget = a.clone();
        toTarget.setYaw(b.getYaw());
        toTarget.setPitch(b.getPitch());
        p.teleport(toCaster);
        t.teleport(toTarget);
        p.setFallDistance(0);

        Combat.addEffect(t, PotionEffectType.SLOWNESS, 50, 1);
        Combat.addEffect(t, PotionEffectType.NAUSEA, 60, 0);
        Combat.passiveEffect(p, PotionEffectType.SPEED, 40, 1);
        Fx.particle(a.clone().add(0, 1, 0), Particle.REVERSE_PORTAL, 50, 0.4, 0.7, 0.4, 0.3);
        Fx.particle(b.clone().add(0, 1, 0), Particle.REVERSE_PORTAL, 50, 0.4, 0.7, 0.4, 0.3);
        return true;
    }
}
