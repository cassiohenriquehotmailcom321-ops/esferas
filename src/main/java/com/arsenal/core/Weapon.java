package com.arsenal.core;

import com.arsenal.ArsenalPlugin;
import org.bukkit.Material;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;

import java.util.List;
import java.util.UUID;

/** Base de toda arma lendária. Cada arma é uma classe com id único. */
public abstract class Weapon {

    protected final ArsenalPlugin plugin;

    protected Weapon(ArsenalPlugin plugin) {
        this.plugin = plugin;
    }

    /** Identificador único (gravado no item via PersistentDataContainer). */
    public abstract String id();

    /** Nome com códigos &amp;. */
    public abstract String name();

    public abstract Material material();

    public abstract String tagline();

    public abstract String passiveText();

    public abstract List<Ability> abilities();

    /** Chamado a cada 5 ticks enquanto o jogador segura a arma na mão principal. */
    public void passiveTick(Player holder, int tick) {
    }

    /** Golpe corpo a corpo normal com a arma (não é chamado para dano de habilidades). */
    public void onHit(Player attacker, LivingEntity victim, EntityDamageByEntityEvent event) {
    }

    /** Dano recebido por quem segura a arma. */
    public void onDamaged(Player victim, EntityDamageEvent event) {
    }

    public void onQuit(UUID id) {
    }

    public final Ability ability(Trigger trigger) {
        for (Ability a : abilities()) {
            if (a.trigger() == trigger) {
                return a;
            }
        }
        return null;
    }
}
