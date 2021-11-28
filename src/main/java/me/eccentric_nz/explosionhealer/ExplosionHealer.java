package me.eccentric_nz.explosionhealer;

import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

public class ExplosionHealer extends JavaPlugin {

    @Override
    public void onDisable() {
        // TODO: Place any custom disable code here.
    }

    @Override
    public void onEnable() {
        saveDefaultConfig();
        PluginManager pm = getServer().getPluginManager();
        pm.registerEvents(new ExplosionHealerListener(this), this);
    }
}
