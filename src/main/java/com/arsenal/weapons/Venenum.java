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

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** VENENO: toxina acumulativa, cuspe, espinhos e elo parasita. */
public final class Venenum extends Weapon {

    private static final class Toxin {
        int stacks;
        long last;
    }

    private final Map<UUID, Toxin> toxins = new HashMap<>();
    private final List<Ability> abilities;

    public Venenum(ArsenalPlugin plugin) {
        super(plugin);
        abilities = List.of(
                new Ability(Trigger.RIGHT, "Cuspe da Víbora",
                        "Três jatos de veneno em leque (4 de dano + veneno + 2 cargas de toxina).",
                        8, 0, this::spit),
                new Ability(Trigger.SNEAK_RIGHT, "Erupção de Espinhos",
                        "Espinhos brotam ao redor (raio 6): 5 de dano e imobiliza por 2s.",
                        18, 0, this::thorns),
                new Ability(Trigger.SWAP, "Elo Parasita",
                        "Liga-se ao alvo por 6s, drenando vida (2 de dano/s) e curando você.",
                        22, 0, this::link));
    }

    @Override public String id() { return "venenum"; }
    @Override public String name() { return "&a&lEsfera Venenosa &8| &2Venenum"; }
    @Override public Material material() { return Material.IRON_SWORD; }
    @Override public String tagline() { return "Uma única gota basta."; }
    @Override public String passiveText() { return "Cada golpe acumula Toxina; com 5 cargas o alvo sofre uma explosão venenosa (5 de dano + Veneno II)."; }
    @Override public List<Ability> abilities() { return abilities; }

    @Override
    public void passiveTick(Player p, int tick) {
        if (tick % 100 == 0) {
            long now = System.currentTimeMillis();
            toxins.values().removeIf(t -> now - t.last > 5000);
        }
        if (tick % 20 == 0) {
            Fx.particle(p.getLocation().add(0, 1, 0), Particle.DUST, 4, 0.5, 0.7, 0.5, 0, Fx.dust(90, 220, 60, 1.2f));
        }
    }

    @Override
    public void onHit(Player attacker, LivingEntity victim, EntityDamageByEntityEvent e) {
        addToxin(attacker, victim, 1);
    }

    @Override
    public void onQuit(UUID id) {
        toxins.remove(id);
    }

    private void addToxin(Player attacker, LivingEntity victim, int amount) {
        Toxin t = toxins.computeIfAbsent(victim.getUniqueId(), k -> new Toxin());
        long now = System.currentTimeMillis();
        if (now - t.last > 5000) {
            t.stacks = 0;
        }
        t.last = now;
        t.stacks += amount;
        Location l = victim.getLocation().add(0, 1.2, 0);
        Fx.particle(l, Particle.DUST, 8, 0.3, 0.5, 0.3, 0, Fx.dust(90, 220, 60, 1.3f));
        if (t.stacks >= 5) {
            t.stacks = 0;
            Combat.damage(plugin, attacker, victim, 5);
            Combat.addEffect(victim, PotionEffectType.POISON, 80, 1);
            Fx.sphere(l, 1.2, 40, Particle.DUST, Fx.dust(70, 255, 50, 1.8f));
            Fx.particle(l, Particle.ITEM_SLIME, 25, 0.4, 0.6, 0.4, 0.1);
            Fx.sound(l, Sound.ENTITY_SLIME_SQUISH, 1.2f, 0.6f);
        }
    }

    // ---------- Cuspe da Víbora ----------

    private boolean spit(Player p) {
        Location eye = p.getEyeLocation();
        Fx.sound(eye, Sound.ENTITY_LLAMA_SPIT, 1.2f, 0.6f);
        for (int s = -1; s <= 1; s++) {
            Vector dir = eye.getDirection().clone().rotateAroundY(Math.toRadians(s * 9));
            Location pos = eye.clone().add(dir.clone().multiply(1));
            plugin.tasks().repeat(p, 0, 1, 20, (i, job) -> {
                dir.add(new Vector(0, -0.02, 0));
                pos.add(dir);
                if (!pos.getBlock().isPassable()) {
                    Fx.particle(pos, Particle.ITEM_SLIME, 10, 0.2, 0.2, 0.2, 0.05);
                    job.end();
                    return;
                }
                Fx.particle(pos, Particle.DUST, 3, 0.1, 0.1, 0.1, 0, Fx.dust(80, 240, 50, 1.6f));
                Fx.particle(pos, Particle.ITEM_SLIME, 1, 0.05, 0.05, 0.05, 0);
                for (LivingEntity e : Combat.near(p, pos, 1.3)) {
                    Combat.damage(plugin, p, e, 4);
                    Combat.addEffect(e, PotionEffectType.POISON, 100, 0);
                    addToxin(p, e, 2);
                    Fx.particle(e.getLocation().add(0, 1, 0), Particle.ITEM_SLIME, 15, 0.3, 0.5, 0.3, 0.1);
                    Fx.sound(e.getLocation(), Sound.ENTITY_SLIME_SQUISH, 1f, 1.2f);
                    job.end();
                    return;
                }
            }, null);
        }
        return true;
    }

    // ---------- Erupção de Espinhos ----------

    private boolean thorns(Player p) {
        Location c = p.getLocation().clone();
        Fx.sound(c, Sound.ENTITY_EVOKER_FANGS_ATTACK, 1.2f, 0.6f);
        Fx.sound(c, Sound.BLOCK_SWEET_BERRY_BUSH_BREAK, 1.5f, 0.5f);

        plugin.tasks().repeat(p, 0, 1, 6, (i, job) -> {
            double r = 1 + i;
            for (int k = 0; k < 10; k++) {
                double ang = k * Math.PI / 5 + i * 0.15;
                Location base = c.clone().add(Math.cos(ang) * r, 0, Math.sin(ang) * r);
                for (double y = 0; y < 1.6; y += 0.3) {
                    Fx.particle(base.clone().add(0, y, 0), Particle.DUST, 1, 0.05, 0, 0.05, 0, Fx.dust(60, 160, 40, 1.3f));
                }
                Fx.particle(base.clone().add(0, 1.6, 0), Particle.CRIT, 2, 0.05, 0.05, 0.05, 0.02);
            }
            Fx.ring(c, r, Math.max(16, (int) (r * 8)), Particle.DUST, Fx.dust(110, 255, 70, 1.2f));
        }, () -> {
            if (!p.isValid()) {
                return;
            }
            for (LivingEntity e : Combat.near(p, c, 6.2)) {
                Combat.damage(plugin, p, e, 5);
                Combat.addEffect(e, PotionEffectType.SLOWNESS, 40, 6);
                e.setVelocity(new Vector(0, 0.35, 0));
                Fx.particle(e.getLocation().add(0, 1, 0), Particle.CRIT, 20, 0.3, 0.6, 0.3, 0.2);
            }
        });
        return true;
    }

    // ---------- Elo Parasita ----------

    private boolean link(Player p) {
        LivingEntity t = Combat.aimTarget(p, 18, 0.9);
        if (t == null) {
            Fx.bar(p, "&cNenhum alvo à vista");
            return false;
        }
        Combat.addEffect(t, PotionEffectType.GLOWING, 120, 0);
        Fx.sound(p.getLocation(), Sound.ENTITY_EVOKER_CAST_SPELL, 1f, 0.6f);

        plugin.tasks().repeat(p, 0, 5, 24, (i, job) -> {
            if (!t.isValid() || t.isDead() || !t.getWorld().equals(p.getWorld())
                    || t.getLocation().distanceSquared(p.getLocation()) > 26 * 26) {
                job.end();
                return;
            }
            Location a = p.getLocation().add(0, 1.2, 0);
            Location b = t.getBoundingBox().getCenter().toLocation(t.getWorld());
            boolean red = i % 2 == 0;
            Fx.line(b, a, 0.35, Particle.DUST, Fx.dust(red ? 200 : 80, red ? 40 : 220, 40, 1.4f));
            if (i % 4 == 3) {
                Combat.damage(plugin, p, t, 2);
                Combat.heal(p, 1.5);
                Fx.particle(a, Particle.HEART, 3, 0.3, 0.3, 0.3, 0);
                Fx.particle(b, Particle.DUST, 12, 0.3, 0.5, 0.3, 0, Fx.dust(200, 40, 40, 1.5f));
                Fx.sound(b, Sound.ENTITY_SLIME_SQUISH, 0.8f, 0.5f);
            }
        }, null);
        return true;
    }
}
