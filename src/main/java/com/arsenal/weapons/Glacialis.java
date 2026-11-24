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
import org.bukkit.block.Block;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.Vector;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

/** GELO: espinhos, domínio do inverno e muralha. */
public final class Glacialis extends Weapon {

    private final List<Ability> abilities;

    public Glacialis(ArsenalPlugin plugin) {
        super(plugin);
        abilities = List.of(
                new Ability(Trigger.RIGHT, "Espinhos Glaciais",
                        "Espinhos de gelo brotam em linha por 14 blocos (6 de dano + lentidão III).",
                        9, 0, this::spikes),
                new Ability(Trigger.SNEAK_RIGHT, "Domínio do Inverno",
                        "Zona de neve (raio 7, 6s): inimigos congelam e sofrem dano; você ganha velocidade.",
                        28, 0, this::domain),
                new Ability(Trigger.SWAP, "Muralha de Gelo",
                        "Ergue uma parede de gelo 5x3 à frente por 7s.",
                        14, 0, this::wall));
    }

    @Override public String id() { return "glacialis"; }
    @Override public String name() { return "&b&lEsfera do Inverno Eterno &8| &3Glacialis"; }
    @Override public Material material() { return Material.DIAMOND_AXE; }
    @Override public String tagline() { return "Seu gelo nunca derrete."; }
    @Override public String passiveText() { return "Golpes congelam o alvo (dano de gelo gradual) e o deixam lento."; }
    @Override public List<Ability> abilities() { return abilities; }

    @Override
    public void passiveTick(Player p, int tick) {
        if (tick % 10 == 0) {
            Fx.particle(p.getLocation().add(0, 1, 0), Particle.SNOWFLAKE, 4, 0.5, 0.7, 0.5, 0.01);
        }
    }

    @Override
    public void onHit(Player attacker, LivingEntity victim, EntityDamageByEntityEvent e) {
        victim.setFreezeTicks(victim.getMaxFreezeTicks() + 40);
        Combat.addEffect(victim, PotionEffectType.SLOWNESS, 40, 0);
        Fx.particle(victim.getLocation().add(0, 1, 0), Particle.SNOWFLAKE, 20, 0.3, 0.6, 0.3, 0.05);
        Fx.sound(victim.getLocation(), Sound.ENTITY_PLAYER_HURT_FREEZE, 0.6f, 1.2f);
    }

    // ---------- Espinhos Glaciais ----------

    private boolean spikes(Player p) {
        Vector dir = Combat.flatDir(p);
        Location origin = p.getLocation();
        Set<UUID> hit = new HashSet<>();
        Fx.sound(origin, Sound.BLOCK_GLASS_BREAK, 1f, 0.6f);
        Fx.sound(origin, Sound.ENTITY_PLAYER_HURT_FREEZE, 1f, 0.8f);

        plugin.tasks().repeat(p, 0, 2, 7, (i, job) -> {
            for (int k = 0; k < 2; k++) {
                double dist = 2 + i * 2 + k;
                Location l = origin.clone().add(dir.clone().multiply(dist));
                for (double y = 0; y < 2.6; y += 0.25) {
                    double w = Math.max(0.05, 0.55 - y * 0.2);
                    Fx.particle(l.clone().add(0, y, 0), Particle.DUST, 3, w, 0, w, 0, Fx.dust(150, 220, 255, 1.3f));
                }
                Fx.particle(l.clone().add(0, 1, 0), Particle.SNOWFLAKE, 12, 0.4, 0.8, 0.4, 0.02);
                for (LivingEntity e : Combat.near(p, l.clone().add(0, 1, 0), 1.8)) {
                    if (!hit.add(e.getUniqueId())) {
                        continue;
                    }
                    Combat.damage(plugin, p, e, 6);
                    Combat.addEffect(e, PotionEffectType.SLOWNESS, 50, 2);
                    e.setFreezeTicks(e.getMaxFreezeTicks());
                    e.setVelocity(new Vector(0, 0.6, 0));
                }
            }
            Fx.sound(origin.clone().add(dir.clone().multiply(2 + i * 2)), Sound.BLOCK_GLASS_BREAK, 0.8f, 1.5f);
        }, null);
        return true;
    }

    // ---------- Domínio do Inverno ----------

    private boolean domain(Player p) {
        Location c = p.getLocation().clone();
        double radius = 7;
        Fx.sound(c, Sound.BLOCK_BEACON_ACTIVATE, 1f, 0.5f);
        Fx.sound(c, Sound.ENTITY_PLAYER_HURT_FREEZE, 1.5f, 0.5f);
        ThreadLocalRandom rnd = ThreadLocalRandom.current();

        plugin.tasks().repeat(p, 0, 5, 24, (i, job) -> {
            Fx.magicCircle(c.clone().add(0, 0.1, 0), radius, Color.fromRGB(120, 200, 255), i * 0.15);
            for (int k = 0; k < 30; k++) {
                double ang = rnd.nextDouble() * Math.PI * 2;
                double r = Math.sqrt(rnd.nextDouble()) * radius;
                Fx.particle(c.clone().add(Math.cos(ang) * r, rnd.nextDouble() * 4, Math.sin(ang) * r),
                        Particle.SNOWFLAKE, 1, 0, 0, 0, 0.02);
            }
            for (int k = 0; k < 20; k++) {
                double th = rnd.nextDouble() * Math.PI * 2;
                double ph = rnd.nextDouble() * Math.PI / 2;
                Fx.particle(c.clone().add(radius * Math.cos(ph) * Math.cos(th), radius * Math.sin(ph) * 0.6,
                                radius * Math.cos(ph) * Math.sin(th)),
                        Particle.DUST, 1, 0, 0, 0, 0, Fx.dust(190, 235, 255, 1.4f));
            }
            for (LivingEntity e : Combat.near(p, c, radius)) {
                Combat.addEffect(e, PotionEffectType.SLOWNESS, 15, 1);
                e.setFreezeTicks(e.getMaxFreezeTicks() + 20);
                if (i % 4 == 0) {
                    Combat.damage(plugin, p, e, 2);
                }
            }
            if (p.getWorld().equals(c.getWorld()) && p.getLocation().distanceSquared(c) <= radius * radius) {
                Combat.passiveEffect(p, PotionEffectType.SPEED, 15, 1);
            }
        }, () -> {
            Fx.particle(c.clone().add(0, 1, 0), Particle.SNOWFLAKE, 150, 3, 1.5, 3, 0.1);
            Fx.sound(c, Sound.BLOCK_BEACON_DEACTIVATE, 1f, 0.8f);
        });
        return true;
    }

    // ---------- Muralha de Gelo ----------

    private boolean wall(Player p) {
        Vector dir = Combat.flatDir(p);
        Vector right = new Vector(-dir.getZ(), 0, dir.getX());
        Location base = p.getLocation().add(dir.clone().multiply(3));
        int placed = 0;
        for (double w = -2.5; w <= 2.5; w += 0.5) {
            for (int h = 0; h < 3; h++) {
                Block b = base.clone().add(right.clone().multiply(w)).add(0, h, 0).getBlock();
                if (!b.getWorld().getNearbyEntities(BoundingBox.of(b)).isEmpty()) {
                    continue;
                }
                if (plugin.blocks().place(b, Material.PACKED_ICE, 140)) {
                    placed++;
                    Fx.particle(b.getLocation().add(0.5, 0.5, 0.5), Particle.SNOWFLAKE, 10, 0.4, 0.4, 0.4, 0.05);
                }
            }
        }
        if (placed == 0) {
            Fx.bar(p, "&cEspaço bloqueado");
            return false;
        }
        Fx.sound(base, Sound.BLOCK_GLASS_BREAK, 1.5f, 0.5f);
        Fx.sound(base, Sound.BLOCK_BEACON_POWER_SELECT, 1f, 0.6f);
        return true;
    }
}
