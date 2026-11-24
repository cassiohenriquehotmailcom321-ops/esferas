package com.arsenal.core;

import com.arsenal.ArsenalPlugin;
import org.bukkit.Bukkit;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.inventory.EquipmentSlot;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Entrada: botão direito, shift + botão direito e tecla F (trocar de mão). */
public final class InputListener implements Listener {

    private final ArsenalPlugin plugin;
    private final Map<UUID, Integer> lastCastTick = new HashMap<>();

    public InputListener(ArsenalPlugin plugin) {
        this.plugin = plugin;
    }

    private boolean duplicate(Player p) {
        int now = Bukkit.getCurrentTick();
        Integer last = lastCastTick.put(p.getUniqueId(), now);
        return last != null && last == now;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onInteract(PlayerInteractEvent e) {
        if (e.getHand() != EquipmentSlot.HAND) {
            return;
        }
        Action action = e.getAction();
        if (action != Action.RIGHT_CLICK_AIR && action != Action.RIGHT_CLICK_BLOCK) {
            return;
        }
        Weapon weapon = plugin.registry().fromItem(e.getItem());
        if (weapon == null) {
            return;
        }
        e.setUseItemInHand(Event.Result.DENY);
        e.setUseInteractedBlock(Event.Result.DENY);
        Player p = e.getPlayer();
        if (duplicate(p)) {
            return;
        }
        Trigger trigger = p.isSneaking() ? Trigger.SNEAK_RIGHT : Trigger.RIGHT;
        plugin.engine().tryCast(p, weapon, trigger);
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onSwap(PlayerSwapHandItemsEvent e) {
        // getOffHandItem() = item que ia para a mão secundária = o que estava na mão principal
        Weapon weapon = plugin.registry().fromItem(e.getOffHandItem());
        if (weapon == null) {
            return;
        }
        e.setCancelled(true);
        Player p = e.getPlayer();
        if (duplicate(p)) {
            return;
        }
        plugin.engine().tryCast(p, weapon, Trigger.SWAP);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onHit(EntityDamageByEntityEvent e) {
        if (Combat.isInternal() || e.getCause() != EntityDamageEvent.DamageCause.ENTITY_ATTACK) {
            return;
        }
        if (!(e.getDamager() instanceof Player attacker) || !(e.getEntity() instanceof LivingEntity victim)) {
            return;
        }
        Weapon weapon = plugin.registry().fromItem(attacker.getInventory().getItemInMainHand());
        if (weapon != null) {
            weapon.onHit(attacker, victim, e);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onDamaged(EntityDamageEvent e) {
        if (!(e.getEntity() instanceof Player victim)) {
            return;
        }
        Weapon weapon = plugin.registry().fromItem(victim.getInventory().getItemInMainHand());
        if (weapon != null) {
            weapon.onDamaged(victim, e);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onBreak(BlockBreakEvent e) {
        if (plugin.blocks().isTemp(e.getBlock())) {
            e.setDropItems(false);
            plugin.blocks().forget(e.getBlock());
        }
    }

    @EventHandler
    public void onDeath(PlayerDeathEvent e) {
        UUID id = e.getEntity().getUniqueId();
        plugin.tasks().cancelAll(id);
        plugin.engine().forget(id);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        UUID id = e.getPlayer().getUniqueId();
        plugin.tasks().cancelAll(id);
        plugin.engine().forget(id);
        plugin.cooldowns().purge(id);
        lastCastTick.remove(id);
        for (Weapon w : plugin.registry().all()) {
            w.onQuit(id);
        }
    }
}
