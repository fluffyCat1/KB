package com.kb.blade;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

public final class AbilityListener implements Listener {

    private final KBPlugin plugin;

    public AbilityListener(KBPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(ignoreCancelled = false)
    public void onRightClick(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) {
            return; // не дублируем на оффхенд-событие
        }
        Action action = event.getAction();
        if (action != Action.RIGHT_CLICK_AIR && action != Action.RIGHT_CLICK_BLOCK) {
            return;
        }

        Player player = event.getPlayer();
        ItemStack item = event.getItem();
        if (!plugin.item().isKbSword(item)) {
            return;
        }

        // Это ПКМ способностью клинка — гасим стандартное взаимодействие (открытие дверей и т.п.)
        event.setCancelled(true);

        AbilityManager ability = plugin.abilityManager();
        if (ability.isOnCooldown(player)) {
            player.sendActionBar(Component.text(
                    "Способность перезаряжается: " + ability.secondsRemaining(player) + "с",
                    NamedTextColor.RED));
            return;
        }

        ability.activate(player);
        player.sendActionBar(Component.text("Способность активирована!", NamedTextColor.LIGHT_PURPLE));
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onAttack(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player attacker)) {
            return;
        }
        if (!plugin.abilityManager().canAttack(attacker)) {
            event.setCancelled(true);
        }
    }
}
