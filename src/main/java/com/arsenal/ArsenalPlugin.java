package com.arsenal;

import com.arsenal.core.AbilityEngine;
import com.arsenal.core.CooldownManager;
import com.arsenal.core.InputListener;
import com.arsenal.core.TaskTracker;
import com.arsenal.core.TempBlocks;
import com.arsenal.core.Weapon;
import com.arsenal.core.WeaponRegistry;
import com.arsenal.weapons.Aegis;
import com.arsenal.weapons.Chronos;
import com.arsenal.weapons.Glacialis;
import com.arsenal.weapons.Gravitas;
import com.arsenal.weapons.Ignaris;
import com.arsenal.weapons.Nullis;
import com.arsenal.weapons.Umbra;
import com.arsenal.weapons.Venenum;
import com.arsenal.weapons.Zephyria;
import com.arsenal.weapons.Zeraphiel;
import org.bukkit.Bukkit;
import org.bukkit.command.PluginCommand;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.logging.Level;

public final class ArsenalPlugin extends JavaPlugin {

    private WeaponRegistry registry;
    private CooldownManager cooldowns;
    private TaskTracker tasks;
    private TempBlocks blocks;
    private AbilityEngine engine;

    private double damageMultiplier = 1.0;
    private double cooldownMultiplier = 1.0;
    private int passiveTick = 0;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        reloadSettings();

        cooldowns = new CooldownManager();
        tasks = new TaskTracker(this);
        blocks = new TempBlocks(this);
        registry = new WeaponRegistry(this);
        engine = new AbilityEngine(this);

        registry.register(new Zeraphiel(this));
        registry.register(new Ignaris(this));
        registry.register(new Glacialis(this));
        registry.register(new Umbra(this));
        registry.register(new Gravitas(this));
        registry.register(new Aegis(this));
        registry.register(new Nullis(this));
        registry.register(new Zephyria(this));
        registry.register(new Venenum(this));
        registry.register(new Chronos(this));

        getServer().getPluginManager().registerEvents(new InputListener(this), this);

        PluginCommand command = getCommand("arsenal");
        if (command != null) {
            ArsenalCommand handler = new ArsenalCommand(this);
            command.setExecutor(handler);
            command.setTabCompleter(handler);
        }

        // Um único loop para todas as passivas (não há tarefas por jogador)
        Bukkit.getScheduler().runTaskTimer(this, this::passiveLoop, 20L, 5L);
        getLogger().info(registry.all().size() + " armas lendárias carregadas.");
    }

    @Override
    public void onDisable() {
        if (tasks != null) {
            tasks.cancelEverything();
        }
        if (blocks != null) {
            blocks.restoreAll();
        }
    }

    private void passiveLoop() {
        passiveTick += 5;
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (p.isDead()) {
                continue;
            }
            Weapon w = registry.fromItem(p.getInventory().getItemInMainHand());
            if (w == null) {
                continue;
            }
            try {
                w.passiveTick(p, passiveTick);
            } catch (Throwable t) {
                getLogger().log(Level.SEVERE, "Erro na passiva de " + w.id(), t);
            }
        }
    }

    public void reloadSettings() {
        reloadConfig();
        damageMultiplier = getConfig().getDouble("damage-multiplier", 1.0);
        cooldownMultiplier = getConfig().getDouble("cooldown-multiplier", 1.0);
    }

    public WeaponRegistry registry() {
        return registry;
    }

    public CooldownManager cooldowns() {
        return cooldowns;
    }

    public TaskTracker tasks() {
        return tasks;
    }

    public TempBlocks blocks() {
        return blocks;
    }

    public AbilityEngine engine() {
        return engine;
    }

    public double damageMultiplier() {
        return damageMultiplier;
    }

    public double cooldownMultiplier() {
        return cooldownMultiplier;
    }
}
