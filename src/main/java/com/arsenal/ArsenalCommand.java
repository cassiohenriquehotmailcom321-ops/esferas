package com.arsenal;

import com.arsenal.core.Ability;
import com.arsenal.core.Fx;
import com.arsenal.core.Weapon;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** /arsenal list | give &lt;id&gt; [jogador] | all [jogador] | reload */
public final class ArsenalCommand implements CommandExecutor, TabCompleter {

    private final ArsenalPlugin plugin;

    public ArsenalCommand(ArsenalPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (args.length == 0) {
            sender.sendMessage(Fx.text("&6Uso: &f/arsenal <list|give|all|reload>"));
            return true;
        }
        switch (args[0].toLowerCase(Locale.ROOT)) {
            case "list" -> {
                sender.sendMessage(Fx.text("&6&l=== Arsenal Lendário ==="));
                for (Weapon w : plugin.registry().all()) {
                    sender.sendMessage(Fx.text("&e" + w.id() + " &8- " + w.name()));
                    for (Ability a : w.abilities()) {
                        sender.sendMessage(Fx.text("   &7" + a.trigger().label() + ": &f" + a.name()));
                    }
                }
            }
            case "give" -> {
                if (args.length < 2) {
                    sender.sendMessage(Fx.text("&cUso: /arsenal give <id> [jogador]"));
                    return true;
                }
                Weapon w = plugin.registry().get(args[1]);
                if (w == null) {
                    sender.sendMessage(Fx.text("&cArma desconhecida: " + args[1]));
                    return true;
                }
                Player target = resolveTarget(sender, args.length > 2 ? args[2] : null);
                if (target == null) {
                    return true;
                }
                give(target, plugin.registry().create(w));
                sender.sendMessage(Fx.text("&aEntregue: " + w.name() + " &apara &f" + target.getName()));
            }
            case "all" -> {
                Player target = resolveTarget(sender, args.length > 1 ? args[1] : null);
                if (target == null) {
                    return true;
                }
                for (Weapon w : plugin.registry().all()) {
                    give(target, plugin.registry().create(w));
                }
                sender.sendMessage(Fx.text("&aArsenal completo entregue para &f" + target.getName()));
            }
            case "reload" -> {
                plugin.reloadSettings();
                sender.sendMessage(Fx.text("&aConfiguração recarregada."));
            }
            default -> sender.sendMessage(Fx.text("&cSubcomando desconhecido."));
        }
        return true;
    }

    private Player resolveTarget(CommandSender sender, String name) {
        if (name != null) {
            Player p = Bukkit.getPlayerExact(name);
            if (p == null) {
                sender.sendMessage(Fx.text("&cJogador não encontrado: " + name));
            }
            return p;
        }
        if (sender instanceof Player p) {
            return p;
        }
        sender.sendMessage(Fx.text("&cInforme um jogador."));
        return null;
    }

    private void give(Player target, ItemStack item) {
        for (ItemStack left : target.getInventory().addItem(item).values()) {
            target.getWorld().dropItemNaturally(target.getLocation(), left);
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd, String alias, String[] args) {
        List<String> out = new ArrayList<>();
        if (args.length == 1) {
            out.addAll(List.of("list", "give", "all", "reload"));
        } else if (args.length == 2 && args[0].equalsIgnoreCase("give")) {
            plugin.registry().all().forEach(w -> out.add(w.id()));
        } else if ((args.length == 3 && args[0].equalsIgnoreCase("give"))
                || (args.length == 2 && args[0].equalsIgnoreCase("all"))) {
            Bukkit.getOnlinePlayers().forEach(p -> out.add(p.getName()));
        }
        String last = args[args.length - 1].toLowerCase(Locale.ROOT);
        out.removeIf(s -> !s.toLowerCase(Locale.ROOT).startsWith(last));
        return out;
    }
}
