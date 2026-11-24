package com.arsenal.core;

import org.bukkit.entity.Player;

/**
 * Uma habilidade de arma.
 *
 * @param lockTicks tempo em que o jogador não pode usar outra habilidade
 *                  (usar só em habilidades que reposicionam o caster, evita conflitos)
 * @param cast      retorna false se a habilidade falhou (ex.: sem alvo) — nesse caso não gasta cooldown
 */
public record Ability(Trigger trigger, String name, String description,
                      double cooldownSeconds, int lockTicks, Cast cast) {

    @FunctionalInterface
    public interface Cast {
        boolean cast(Player player);
    }
}
