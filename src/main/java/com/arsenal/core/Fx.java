package com.arsenal.core;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import java.util.concurrent.ThreadLocalRandom;

/** Biblioteca de efeitos visuais e sonoros: anéis, círculos mágicos, raios, esferas, linhas... */
public final class Fx {

    private Fx() {
    }

    // ---------- texto ----------

    public static Component text(String legacy) {
        return LegacyComponentSerializer.legacyAmpersand().deserialize(legacy)
                .decoration(TextDecoration.ITALIC, false);
    }

    public static void bar(Player p, String legacy) {
        p.sendActionBar(text(legacy));
    }

    // ---------- som / partículas básicas ----------

    public static void sound(Location l, Sound s, float volume, float pitch) {
        l.getWorld().playSound(l, s, SoundCategory.PLAYERS, volume, pitch);
    }

    /** {@code data} pode ser null para partículas sem dados. */
    public static void particle(Location l, Particle p, int count, double dx, double dy, double dz,
                                double speed, Object data) {
        if (data == null) {
            l.getWorld().spawnParticle(p, l, count, dx, dy, dz, speed);
        } else {
            l.getWorld().spawnParticle(p, l, count, dx, dy, dz, speed, data);
        }
    }

    public static void particle(Location l, Particle p, int count, double dx, double dy, double dz, double speed) {
        l.getWorld().spawnParticle(p, l, count, dx, dy, dz, speed);
    }

    public static Particle.DustOptions dust(int r, int g, int b, float size) {
        return new Particle.DustOptions(Color.fromRGB(r, g, b), size);
    }

    public static Particle.DustTransition trans(Color from, Color to, float size) {
        return new Particle.DustTransition(from, to, size);
    }

    // ---------- formas ----------

    /** Anel horizontal. */
    public static void ring(Location c, double radius, int points, Particle p, Object data) {
        for (int i = 0; i < points; i++) {
            double a = 2 * Math.PI * i / points;
            particle(c.clone().add(Math.cos(a) * radius, 0, Math.sin(a) * radius), p, 1, 0, 0, 0, 0, data);
        }
    }

    public static void line(Location a, Location b, double step, Particle p, Object data) {
        Vector v = b.toVector().subtract(a.toVector());
        double len = v.length();
        if (len < 0.01) {
            return;
        }
        v.normalize().multiply(step);
        Location cur = a.clone();
        for (double d = 0; d <= len; d += step) {
            particle(cur, p, 1, 0, 0, 0, 0, data);
            cur.add(v);
        }
    }

    /** Esfera de pontos distribuídos (espiral de Fibonacci). */
    public static void sphere(Location c, double r, int points, Particle p, Object data) {
        double golden = Math.PI * (3 - Math.sqrt(5));
        for (int i = 0; i < points; i++) {
            double y = 1 - 2.0 * (i + 0.5) / points;
            double rad = Math.sqrt(Math.max(0, 1 - y * y));
            double th = golden * i;
            particle(c.clone().add(Math.cos(th) * rad * r, y * r, Math.sin(th) * rad * r), p, 1, 0, 0, 0, 0, data);
        }
    }

    /** Círculo mágico: dois anéis + hexagrama girando. */
    public static void magicCircle(Location c, double r, Color color, double rotation) {
        Particle.DustOptions d = new Particle.DustOptions(color, 1.2f);
        double inner = r * 0.86;
        ring(c, r, Math.max(24, (int) (r * 10)), Particle.DUST, d);
        ring(c, inner, Math.max(20, (int) (r * 8)), Particle.DUST, d);
        for (int tri = 0; tri < 2; tri++) {
            for (int k = 0; k < 3; k++) {
                double a1 = rotation + tri * Math.PI / 3 + k * 2 * Math.PI / 3;
                double a2 = a1 + 2 * Math.PI / 3;
                Location p1 = c.clone().add(Math.cos(a1) * inner, 0, Math.sin(a1) * inner);
                Location p2 = c.clone().add(Math.cos(a2) * inner, 0, Math.sin(a2) * inner);
                line(p1, p2, 0.5, Particle.DUST, d);
            }
        }
        particle(c.clone().add(0, 0.1, 0), Particle.END_ROD, 2, r * 0.3, 0, r * 0.3, 0.01);
    }

    /** Raio irregular entre dois pontos. */
    public static void lightning(Location a, Location b) {
        ThreadLocalRandom rnd = ThreadLocalRandom.current();
        double dist = a.distance(b);
        int segments = Math.max(2, (int) (dist / 1.2));
        Location prev = a.clone();
        for (int i = 1; i <= segments; i++) {
            double t = (double) i / segments;
            Location next = a.clone().add(b.clone().subtract(a).toVector().multiply(t));
            if (i < segments) {
                next.add(rnd.nextDouble(-0.4, 0.4), rnd.nextDouble(-0.4, 0.4), rnd.nextDouble(-0.4, 0.4));
            }
            line(prev, next, 0.25, Particle.ELECTRIC_SPARK, null);
            if (i % 2 == 0) {
                particle(next, Particle.END_ROD, 1, 0.05, 0.05, 0.05, 0.01);
            }
            prev = next;
        }
    }

    public static void explosion(Location l) {
        particle(l, Particle.EXPLOSION_EMITTER, 1, 0, 0, 0, 0);
        sound(l, Sound.ENTITY_GENERIC_EXPLODE, 1.4f, 0.9f);
    }

    /** Detritos do bloco abaixo do local. */
    public static void debris(Location l, int count, double spread) {
        Block below = l.clone().subtract(0, 1, 0).getBlock();
        BlockData data = below.getBlockData();
        if (data.getMaterial().isAir()) {
            return;
        }
        particle(l, Particle.BLOCK, count, spread, 0.2, spread, 0.15, data);
    }
}
