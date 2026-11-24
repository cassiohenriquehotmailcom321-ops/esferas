package com.arsenal.core;

import com.arsenal.ArsenalPlugin;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Level;

/**
 * Agenda tarefas repetitivas das habilidades e as associa ao dono.
 * Se o dono sair/morrer, todas as tarefas são encerradas e o {@code finish} de cada uma roda
 * (restaurando estados como IA de mobs congelados). Nenhuma habilidade fica "órfã".
 */
public final class TaskTracker {

    @FunctionalInterface
    public interface Body {
        void run(int index, Job job);
    }

    private final ArsenalPlugin plugin;
    private final Map<UUID, Set<Job>> jobs = new HashMap<>();

    public TaskTracker(ArsenalPlugin plugin) {
        this.plugin = plugin;
    }

    public final class Job {
        private final UUID owner;
        private final Body body;
        private final Runnable finish;
        private final int max;
        private int index;
        private boolean done;
        private BukkitTask task;

        private Job(UUID owner, int max, Body body, Runnable finish) {
            this.owner = owner;
            this.max = max;
            this.body = body;
            this.finish = finish;
        }

        /** Encerra a tarefa e executa o finish (apenas uma vez). */
        public void end() {
            if (done) {
                return;
            }
            done = true;
            if (task != null) {
                task.cancel();
            }
            Set<Job> set = jobs.get(owner);
            if (set != null) {
                set.remove(this);
                if (set.isEmpty()) {
                    jobs.remove(owner);
                }
            }
            if (finish != null) {
                try {
                    finish.run();
                } catch (Throwable t) {
                    plugin.getLogger().log(Level.SEVERE, "Erro ao finalizar habilidade", t);
                }
            }
        }

        private void tick() {
            if (done) {
                return;
            }
            try {
                body.run(index++, this);
            } catch (Throwable t) {
                plugin.getLogger().log(Level.SEVERE, "Erro em tarefa de habilidade", t);
                end();
                return;
            }
            if (!done && index >= max) {
                end();
            }
        }
    }

    /** Executa {@code body} {@code runs} vezes (a cada {@code period} ticks) e depois {@code finish}. */
    public Job repeat(Player owner, long delay, long period, int runs, Body body, Runnable finish) {
        Job job = new Job(owner.getUniqueId(), runs, body, finish);
        jobs.computeIfAbsent(owner.getUniqueId(), k -> new HashSet<>()).add(job);
        job.task = Bukkit.getScheduler().runTaskTimer(plugin, job::tick, delay, period);
        return job;
    }

    public Job later(Player owner, long delay, Runnable action) {
        return repeat(owner, delay, 1, 1, (i, j) -> action.run(), null);
    }

    public void cancelAll(UUID owner) {
        Set<Job> set = jobs.remove(owner);
        if (set == null) {
            return;
        }
        for (Job job : new ArrayList<>(set)) {
            job.end();
        }
    }

    public void cancelEverything() {
        for (UUID id : new ArrayList<>(jobs.keySet())) {
            cancelAll(id);
        }
    }
}
