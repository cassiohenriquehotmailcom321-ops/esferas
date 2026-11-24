package com.arsenal.core;

import com.arsenal.ArsenalPlugin;
import net.kyori.adventure.text.Component;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Registro de armas + identificação de itens via PersistentDataContainer (à prova de renomear na bigorna). */
public final class WeaponRegistry {

    private final ArsenalPlugin plugin;
    private final NamespacedKey key;
    private final Map<String, Weapon> byId = new LinkedHashMap<>();

    public WeaponRegistry(ArsenalPlugin plugin) {
        this.plugin = plugin;
        this.key = new NamespacedKey(plugin, "weapon_id");
    }

    public void register(Weapon weapon) {
        byId.put(weapon.id(), weapon);
    }

    public Weapon get(String id) {
        return id == null ? null : byId.get(id.toLowerCase(Locale.ROOT));
    }

    public Collection<Weapon> all() {
        return byId.values();
    }

    /** Descobre qual arma é o item (ou null se não for uma arma do arsenal). */
    public Weapon fromItem(ItemStack item) {
        if (item == null || !item.hasItemMeta()) {
            return null;
        }
        String id = item.getItemMeta().getPersistentDataContainer().get(key, PersistentDataType.STRING);
        return id == null ? null : byId.get(id);
    }

    public ItemStack create(Weapon w) {
        ItemStack item = new ItemStack(w.material());
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Fx.text(w.name()));

        List<Component> lore = new ArrayList<>();
        lore.add(Fx.text("&8" + w.tagline()));
        lore.add(Component.empty());
        lore.add(Fx.text("&5✦ Passiva: &7" + w.passiveText()));
        lore.add(Component.empty());
        for (Ability a : w.abilities()) {
            lore.add(Fx.text("&6" + a.trigger().label() + " &f" + a.name() + " &8(" + fmt(a.cooldownSeconds()) + "s)"));
            lore.add(Fx.text("  &7" + a.description()));
        }
        meta.lore(lore);
        meta.setUnbreakable(true);
        meta.addItemFlags(ItemFlag.HIDE_UNBREAKABLE);
        meta.setEnchantmentGlintOverride(true);
        if (plugin.getConfig().getBoolean("custom-models", true)) {
            // textura própria: assets/arsenal/items/<id>.json do resource pack
            meta.setItemModel(new NamespacedKey("arsenal", w.id()));
        }
        meta.getPersistentDataContainer().set(key, PersistentDataType.STRING, w.id());
        item.setItemMeta(meta);
        return item;
    }

    private static String fmt(double s) {
        return s == Math.floor(s) ? String.valueOf((int) s) : String.format(Locale.US, "%.1f", s);
    }
}
