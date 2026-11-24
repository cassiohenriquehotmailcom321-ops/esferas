package com.arsenal.core;

import com.arsenal.ArsenalPlugin;
import org.bukkit.GameMode;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;

/** Executa habilidades aplicando cooldown e bloqueio anti-conflito. */
public final class AbilityEngine {

    private final ArsenalPlugin plugin;
    private final Map<UUID, Long> locks = new HashMap<>();

    public AbilityEngine(ArsenalPlugin plugin) {
        this.plugin = plugin;
    }

    public void tryCast(Player p, Weapon weapon, Trigger trigger) {
        Ability ability = weapon.ability(trigger);
        if (ability == null || p.isDead() || p.getGameMode() == GameMode.SPECTATOR) {
            return;
        }
        UUID id = p.getUniqueId();
        long now = System.currentTimeMillis();

        Long lock = locks.get(id);
        if (lock != null && lock > now) {
            Fx.bar(p, "&7Habilidade em execução...");
            return;
        }

        String key = weapon.id() + ":" + trigger.name();
        long remaining = plugin.cooldowns().remaining(id, key);
        if (remaining > 0) {
            Fx.bar(p, "&c⏳ " + ability.name() + " &7— " + String.format(Locale.US, "%.1f", remaining / 1000.0) + "s");
            p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, SoundCategory.PLAYERS, 0.5f, 0.6f);
            return;
        }

        boolean ok;
        try {
            ok = ability.cast().cast(p);
        } catch (Throwable t) {
            plugin.getLogger().log(Level.SEVERE, "Erro na habilidade " + ability.name(), t);
            ok = false;
        }
        if (!ok) {
            return;
        }

        plugin.cooldowns().set(id, key, (long) (ability.cooldownSeconds() * 1000 * plugin.cooldownMultiplier()));
        if (ability.lockTicks() > 0) {
            locks.put(id, now + ability.lockTicks() * 50L);
        }
        Fx.bar(p, "&6✦ &e" + ability.name());
    }

    public void forget(UUID id) {
        locks.remove(id);
    }
}
