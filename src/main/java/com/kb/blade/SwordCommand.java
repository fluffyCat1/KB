package com.kb.blade;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.List;

/**
 * Обрабатывает "/kb <id> [игрок]".
 * Выдаёт клинок KBBlade с указанным ID отправителю команды или указанному игроку.
 */
public final class SwordCommand implements CommandExecutor, TabCompleter {

    private final KBPlugin plugin;

    public SwordCommand(KBPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {
        if (args.length < 1) {
            sender.sendMessage(Component.text("Использование: /kb <id> [игрок]", NamedTextColor.RED));
            return true;
        }

        String id = args[0];
        Player target;

        if (args.length >= 2) {
            target = Bukkit.getPlayerExact(args[1]);
            if (target == null) {
                sender.sendMessage(Component.text("Игрок '" + args[1] + "' не найден или не в сети.", NamedTextColor.RED));
                return true;
            }
        } else if (sender instanceof Player player) {
            target = player;
        } else {
            sender.sendMessage(Component.text("Укажите игрока: /kb <id> <игрок> (консоль не может получить предмет).", NamedTextColor.RED));
            return true;
        }

        ItemStack sword = plugin.item().create(id);
        target.getInventory().addItem(sword).values().forEach(leftover ->
                target.getWorld().dropItemNaturally(target.getLocation(), leftover));

        target.sendMessage(Component.text("Вы получили Клинок Бездны (ID: " + id + ").", NamedTextColor.LIGHT_PURPLE));
        if (!sender.equals(target)) {
            sender.sendMessage(Component.text("Клинок (ID: " + id + ") выдан игроку " + target.getName() + ".", NamedTextColor.GRAY));
        }
        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, String[] args) {
        if (args.length == 1) {
            return Collections.singletonList("<id>");
        }
        if (args.length == 2) {
            return null; // стандартный список игроков онлайн
        }
        return Collections.emptyList();
    }
}
