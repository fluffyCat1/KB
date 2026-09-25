package com.kb.blade;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.attribute.AttributeModifier.Operation;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EquipmentSlotGroup;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Transformation;
import org.joml.AxisAngle4f;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Логика способности, активируемой ПКМ с клинком в руке:
 *  - невидимость на 6с
 *  - временно: прыжок -1, сопротивление 255 ур., высота шага +1 блок — на 6с
 *  - на время эффекта игрок не может атаковать
 *  - под ногами остаётся след из чёрных block_display (высота/размер 0.01),
 *    которые исчезают через 5с после применения способности
 *  - перезарядка 15с
 */
public final class AbilityManager {

    private static final long COOLDOWN_MS = 15_000L;
    private static final long EFFECT_TICKS = 6L * 20L;   // 6 секунд
    private static final long TRAIL_TICKS = 5L * 20L;    // 5 секунд
    private static final BlockData TRAIL_BLOCK = Material.BLACK_CONCRETE.createBlockData();

    private final KBPlugin plugin;
    private final KBKeys keys;

    private final Map<UUID, Long> cooldownUntil = new HashMap<>();
    private final Set<UUID> attackLocked = new HashSet<>();
    private final Map<UUID, List<BukkitTask>> activeTasks = new HashMap<>();

    public AbilityManager(KBPlugin plugin) {
        this.plugin = plugin;
        this.keys = plugin.keys();
    }

    public boolean isOnCooldown(Player player) {
        Long until = cooldownUntil.get(player.getUniqueId());
        return until != null && until > System.currentTimeMillis();
    }

    public long secondsRemaining(Player player) {
        Long until = cooldownUntil.get(player.getUniqueId());
        if (until == null) {
            return 0L;
        }
        long remainingMs = until - System.currentTimeMillis();
        return Math.max(0L, (remainingMs + 999L) / 1000L);
    }

    public boolean canAttack(Player player) {
        return !attackLocked.contains(player.getUniqueId());
    }

    /**
     * Пытается активировать способность. Возвращает false, если сейчас перезарядка.
     */
    public boolean activate(Player player) {
        if (isOnCooldown(player)) {
            return false;
        }
        cooldownUntil.put(player.getUniqueId(), System.currentTimeMillis() + COOLDOWN_MS);
        runAbility(player);
        return true;
    }

    private void runAbility(Player player) {
        UUID uuid = player.getUniqueId();
        List<BukkitTask> tasks = new ArrayList<>();
        activeTasks.put(uuid, tasks);

        // 1. Невидимость на 6с
        player.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, (int) EFFECT_TICKS, 0, true, false, true));

        // 2. Сопротивление 255 уровня на 6с (уровень 255 -> амплификатор 254)
        player.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, (int) EFFECT_TICKS, 254, true, false, false));

        // 3. Временные атрибуты: прыжок -1, высота шага +1 блок
        AttributeInstance jump = player.getAttribute(Attribute.JUMP_STRENGTH);
        AttributeInstance step = player.getAttribute(Attribute.STEP_HEIGHT);

        AttributeModifier jumpModifier = new AttributeModifier(keys.attrAbilityJump, -1.0D, Operation.ADD_NUMBER, EquipmentSlotGroup.ANY);
        AttributeModifier stepModifier = new AttributeModifier(keys.attrAbilityStep, 1.0D, Operation.ADD_NUMBER, EquipmentSlotGroup.ANY);

        if (jump != null) {
            jump.removeModifier(keys.attrAbilityJump);
            jump.addModifier(jumpModifier);
        }
        if (step != null) {
            step.removeModifier(keys.attrAbilityStep);
            step.addModifier(stepModifier);
        }

        // 4. Блокировка атаки на время эффекта
        attackLocked.add(uuid);

        // 5. След из block_display под ногами, обновляется, пока активна невидимость
        List<BlockDisplay> trail = new ArrayList<>();
        Location[] lastBlock = new Location[1];

        BukkitTask spawnTask = plugin.getServer().getScheduler().runTaskTimer(plugin, () -> {
            if (!player.isOnline()) {
                return;
            }
            Location blockLoc = player.getLocation().getBlock().getLocation();
            if (lastBlock[0] != null && lastBlock[0].equals(blockLoc)) {
                return; // ещё стоим на том же блоке — новую пластину не ставим
            }
            lastBlock[0] = blockLoc;
            trail.add(spawnTrailPlate(blockLoc));
        }, 0L, 2L);
        tasks.add(spawnTask);

        // 6. Через 5с после использования способности — убрать след
        BukkitTask cleanupTask = plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            spawnTask.cancel();
            for (BlockDisplay display : trail) {
                if (display.isValid()) {
                    display.remove();
                }
            }
            trail.clear();
        }, TRAIL_TICKS);
        tasks.add(cleanupTask);

        // 7. Через 6с — снять временные атрибуты и разблокировать атаку
        BukkitTask endTask = plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            if (jump != null) {
                jump.removeModifier(keys.attrAbilityJump);
            }
            if (step != null) {
                step.removeModifier(keys.attrAbilityStep);
            }
            attackLocked.remove(uuid);
            activeTasks.remove(uuid);
        }, EFFECT_TICKS);
        tasks.add(endTask);
    }

    private BlockDisplay spawnTrailPlate(Location blockLocation) {
        return blockLocation.getWorld().spawn(blockLocation, BlockDisplay.class, display -> {
            display.setBlock(TRAIL_BLOCK);
            display.setPersistent(false);
            Transformation transformation = new Transformation(
                    new Vector3f(0f, 0f, 0f),
                    new AxisAngle4f(0f, 0f, 0f, 1f),
                    new Vector3f(1f, 0.01f, 1f),
                    new AxisAngle4f(0f, 0f, 0f, 1f)
            );
            display.setTransformation(transformation);
            display.setBrightness(new org.bukkit.entity.Display.Brightness(15, 15));
        });
    }

    /**
     * Вызывается при выключении плагина — чистим таски и след, чтобы ничего не оставалось в мире.
     */
    public void shutdown() {
        for (List<BukkitTask> tasks : activeTasks.values()) {
            for (BukkitTask task : tasks) {
                task.cancel();
            }
        }
        activeTasks.clear();
        cooldownUntil.clear();
        attackLocked.clear();
    }
}
