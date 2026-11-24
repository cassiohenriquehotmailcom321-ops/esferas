package com.arsenal.core;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Cooldowns por jogador e por habilidade (chave = idArma:TRIGGER). */
public final class CooldownManager {

    private final Map<UUID, Map<String, Long>> map = new HashMap<>();

    public long remaining(UUID id, String key) {
        Map<String, Long> m = map.get(id);
        if (m == null) {
            return 0;
        }
        Long end = m.get(key);
        if (end == null) {
            return 0;
        }
        long left = end - System.currentTimeMillis();
        if (left <= 0) {
            m.remove(key);
            return 0;
        }
        return left;
    }

    public void set(UUID id, String key, long millis) {
        map.computeIfAbsent(id, k -> new HashMap<>()).put(key, System.currentTimeMillis() + millis);
    }

    /** Reduz todos os cooldowns cuja chave começa com o prefixo. */
    public void reduce(UUID id, String prefix, long millis) {
        Map<String, Long> m = map.get(id);
        if (m == null) {
            return;
        }
        m.replaceAll((k, v) -> k.startsWith(prefix) ? v - millis : v);
    }

    /** Remove entradas expiradas (chamado ao sair do servidor). Cooldowns ativos permanecem (anti-relog). */
    public void purge(UUID id) {
        Map<String, Long> m = map.get(id);
        if (m == null) {
            return;
        }
        long now = System.currentTimeMillis();
        m.values().removeIf(v -> v <= now);
        if (m.isEmpty()) {
            map.remove(id);
        }
    }
}
