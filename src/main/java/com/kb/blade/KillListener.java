package com.kb.blade;

import org.bukkit.entity.Animals;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.inventory.ItemStack;

/**
 * Начисляет бонус к атрибуту урона клинку KBBlade:
 *  +2.0  — за убийство игрока
 *  +0.25 — за убийство мирного животного (Animals: корова, овца, курица и т.д.)
 */
public final class KillListener implements Listener {

    private static final double PLAYER_KILL_BONUS = 2.0D;
    private static final double ANIMAL_KILL_BONUS = 0.25D;

    private final KBPlugin plugin;

    public KillListener(KBPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(ignoreCancelled = true)
    public void onEntityDeath(EntityDeathEvent event) {
        LivingEntity victim = event.getEntity();
        Player killer = victim.getKiller();
        if (killer == null) {
            return;
        }

        ItemStack weapon = killer.getInventory().getItemInMainHand();
        if (!plugin.item().isKbSword(weapon)) {
            return;
        }

        boolean isPlayerKill = victim instanceof Player;
        boolean isPeacefulAnimal = victim instanceof Animals;

        if (isPlayerKill) {
            plugin.item().addKillBonus(weapon, PLAYER_KILL_BONUS, true);
        } else if (isPeacefulAnimal) {
            plugin.item().addKillBonus(weapon, ANIMAL_KILL_BONUS, false);
        }
    }
}
