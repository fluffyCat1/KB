package com.kb.blade;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.attribute.AttributeModifier.Operation;
import org.bukkit.attribute.EquipmentSlotGroup;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.List;

/**
 * Всё, что связано с самим предметом клинка: создание, распознавание,
 * хранение прогресса убийств и перерисовка лора.
 */
public final class KBItem {

    private final KBPlugin plugin;
    private final KBKeys keys;

    public KBItem(KBPlugin plugin) {
        this.plugin = plugin;
        this.keys = plugin.keys();
    }

    /**
     * Создаёт новый клинок с указанным ID и нулевым прогрессом.
     */
    public ItemStack create(String id) {
        ItemStack item = new ItemStack(Material.NETHERITE_SWORD);
        ItemMeta meta = item.getItemMeta();

        meta.displayName(Component.text("Клинок Бездны", NamedTextColor.DARK_PURPLE)
                .decoration(TextDecoration.ITALIC, false));

        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        pdc.set(keys.itemId, PersistentDataType.STRING, id);
        pdc.set(keys.killBonus, PersistentDataType.DOUBLE, 0.0D);
        pdc.set(keys.killsPlayers, PersistentDataType.INTEGER, 0);
        pdc.set(keys.killsAnimals, PersistentDataType.INTEGER, 0);

        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
        item.setItemMeta(meta);

        refreshLore(item);
        return item;
    }

    /**
     * Проверяет, является ли предмет клинком KBBlade.
     */
    public boolean isKbSword(ItemStack item) {
        if (item == null || item.getType() != Material.NETHERITE_SWORD || !item.hasItemMeta()) {
            return false;
        }
        return item.getItemMeta().getPersistentDataContainer().has(keys.itemId, PersistentDataType.STRING);
    }

    /**
     * Начисляет бонус урона за убийство и обновляет предмет.
     * amount — прибавка к атрибуту урона (+2 за игрока, +0.25 за мирное животное).
     */
    public void addKillBonus(ItemStack item, double amount, boolean wasPlayer) {
        ItemMeta meta = item.getItemMeta();
        PersistentDataContainer pdc = meta.getPersistentDataContainer();

        double current = pdc.getOrDefault(keys.killBonus, PersistentDataType.DOUBLE, 0.0D);
        double updated = current + amount;
        pdc.set(keys.killBonus, PersistentDataType.DOUBLE, updated);

        if (wasPlayer) {
            int kills = pdc.getOrDefault(keys.killsPlayers, PersistentDataType.INTEGER, 0);
            pdc.set(keys.killsPlayers, PersistentDataType.INTEGER, kills + 1);
        } else {
            int kills = pdc.getOrDefault(keys.killsAnimals, PersistentDataType.INTEGER, 0);
            pdc.set(keys.killsAnimals, PersistentDataType.INTEGER, kills + 1);
        }

        // Убираем старый модификатор урона (если он был) и добавляем новый с суммарным бонусом.
        // Модификатор урона от убийств — единственный AttributeModifier, который плагин
        // вешает на ATTACK_DAMAGE этого предмета, поэтому чистим все и добавляем актуальный.
        meta.removeAttributeModifier(Attribute.ATTACK_DAMAGE);
        if (updated != 0.0D) {
            meta.addAttributeModifier(Attribute.ATTACK_DAMAGE, buildKillModifier(updated));
        }

        item.setItemMeta(meta);
        refreshLore(item);
    }

    private AttributeModifier buildKillModifier(double amount) {
        return new AttributeModifier(keys.attrKillBonus, amount, Operation.ADD_NUMBER, EquipmentSlotGroup.MAINHAND);
    }

    /**
     * Перерисовывает лор клинка на основании текущего прогресса, хранящегося в PDC.
     */
    public void refreshLore(ItemStack item) {
        ItemMeta meta = item.getItemMeta();
        PersistentDataContainer pdc = meta.getPersistentDataContainer();

        String id = pdc.getOrDefault(keys.itemId, PersistentDataType.STRING, "?");
        double bonus = pdc.getOrDefault(keys.killBonus, PersistentDataType.DOUBLE, 0.0D);
        int killedPlayers = pdc.getOrDefault(keys.killsPlayers, PersistentDataType.INTEGER, 0);
        int killedAnimals = pdc.getOrDefault(keys.killsAnimals, PersistentDataType.INTEGER, 0);

        List<Component> lore = new ArrayList<>();
        lore.add(Component.text("ID: " + id, NamedTextColor.DARK_GRAY).decoration(TextDecoration.ITALIC, false));
        lore.add(Component.text("Бонус урона: +" + trim(bonus), NamedTextColor.RED).decoration(TextDecoration.ITALIC, false));
        lore.add(Component.text("Убито игроков: " + killedPlayers, NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false));
        lore.add(Component.text("Убито животных: " + killedAnimals, NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false));
        lore.add(Component.empty());
        lore.add(Component.text("ПКМ: ", NamedTextColor.LIGHT_PURPLE)
                .decoration(TextDecoration.ITALIC, false)
                .append(Component.text("невидимость на 6с, рывок и след из тьмы", NamedTextColor.GRAY)
                        .decoration(TextDecoration.ITALIC, false)));
        lore.add(Component.text("Перезарядка: 15с", NamedTextColor.DARK_GRAY).decoration(TextDecoration.ITALIC, false));

        meta.lore(lore);
        item.setItemMeta(meta);
    }

    private String trim(double value) {
        if (value == Math.floor(value)) {
            return String.valueOf((long) value);
        }
        return String.valueOf(Math.round(value * 100.0D) / 100.0D);
    }
}
