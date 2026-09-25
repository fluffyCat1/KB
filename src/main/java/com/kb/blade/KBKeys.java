package com.kb.blade;

import org.bukkit.NamespacedKey;
import org.bukkit.plugin.Plugin;

/**
 * Все NamespacedKey, которые использует плагин, собраны в одном месте,
 * чтобы не плодить опечатки по разным классам.
 */
public final class KBKeys {

    public final NamespacedKey itemId;          // ID клинка (задаётся командой /kb <id>)
    public final NamespacedKey killBonus;        // Текущий суммарный бонус урона (double)
    public final NamespacedKey killsPlayers;     // Счётчик убитых игроков (int, для лора)
    public final NamespacedKey killsAnimals;     // Счётчик убитых мирных животных (int, для лора)

    public final NamespacedKey attrKillBonus;    // Ключ AttributeModifier-а бонуса урона
    public final NamespacedKey attrAbilityJump;   // Ключ AttributeModifier-а прыжка на время способности
    public final NamespacedKey attrAbilityStep;   // Ключ AttributeModifier-а высоты шага на время способности

    public KBKeys(Plugin plugin) {
        this.itemId = new NamespacedKey(plugin, "kb_id");
        this.killBonus = new NamespacedKey(plugin, "kb_kill_bonus");
        this.killsPlayers = new NamespacedKey(plugin, "kb_kills_players");
        this.killsAnimals = new NamespacedKey(plugin, "kb_kills_animals");

        this.attrKillBonus = new NamespacedKey(plugin, "kb_kill_bonus_modifier");
        this.attrAbilityJump = new NamespacedKey(plugin, "kb_ability_jump_modifier");
        this.attrAbilityStep = new NamespacedKey(plugin, "kb_ability_step_modifier");
    }
}
