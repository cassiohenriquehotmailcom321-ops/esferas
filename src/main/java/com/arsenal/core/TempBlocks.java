package com.arsenal.core;

import com.arsenal.ArsenalPlugin;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Blocos temporários (muralhas de gelo etc.). Sempre restaurados, inclusive ao desligar o plugin. */
public final class TempBlocks {

    private record Key(UUID world, int x, int y, int z) {
    }

    private record Entry(Block block, BlockData original, Material placed) {
    }

    private final ArsenalPlugin plugin;
    private final Map<Key, Entry> entries = new HashMap<>();

    public TempBlocks(ArsenalPlugin plugin) {
        this.plugin = plugin;
    }

    private static Key key(Block b) {
        return new Key(b.getWorld().getUID(), b.getX(), b.getY(), b.getZ());
    }

    /** Só substitui ar. Retorna true se colocou. */
    public boolean place(Block block, Material material, int ticks) {
        if (!block.getType().isAir()) {
            return false;
        }
        Key k = key(block);
        if (entries.containsKey(k)) {
            return false;
        }
        entries.put(k, new Entry(block, block.getBlockData(), material));
        block.setType(material, false);
        Bukkit.getScheduler().runTaskLater(plugin, () -> restore(k), ticks);
        return true;
    }

    private void restore(Key k) {
        Entry e = entries.remove(k);
        if (e == null) {
            return;
        }
        if (e.block().getType() == e.placed()) {
            e.block().setBlockData(e.original(), false);
        }
    }

    public boolean isTemp(Block b) {
        return entries.containsKey(key(b));
    }

    /** Esquece o bloco (ex.: quebrado por um jogador). */
    public void forget(Block b) {
        entries.remove(key(b));
    }

    public void restoreAll() {
        for (Key k : new ArrayList<>(entries.keySet())) {
            restore(k);
        }
    }
}
