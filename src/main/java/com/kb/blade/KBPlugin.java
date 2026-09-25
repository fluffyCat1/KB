package com.kb.blade;

import org.bukkit.plugin.java.JavaPlugin;

public final class KBPlugin extends JavaPlugin {

    private KBKeys keys;
    private KBItem item;
    private AbilityManager abilityManager;

    @Override
    public void onEnable() {
        this.keys = new KBKeys(this);
        this.item = new KBItem(this);
        this.abilityManager = new AbilityManager(this);

        SwordCommand command = new SwordCommand(this);
        getCommand("kb").setExecutor(command);
        getCommand("kb").setTabCompleter(command);

        getServer().getPluginManager().registerEvents(new KillListener(this), this);
        getServer().getPluginManager().registerEvents(new AbilityListener(this), this);

        getLogger().info("KBBlade включен.");
    }

    @Override
    public void onDisable() {
        if (abilityManager != null) {
            abilityManager.shutdown();
        }
    }

    public KBKeys keys() {
        return keys;
    }

    public KBItem item() {
        return item;
    }

    public AbilityManager abilityManager() {
        return abilityManager;
    }
}
