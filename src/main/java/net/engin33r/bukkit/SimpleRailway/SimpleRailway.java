package net.engin33r.bukkit.SimpleRailway;

import org.bukkit.event.HandlerList;
import org.bukkit.plugin.java.JavaPlugin;

import javax.persistence.PersistenceException;
import java.util.ArrayList;
import java.util.List;

public class SimpleRailway extends JavaPlugin {
    @Override
    public void onEnable() {
        new SimpleRailwayListener(this);

        try {
            getDatabase().find(SimpleRailwaySpawner.class).findRowCount();
            getDatabase().find(SimpleRailwayDespawner.class).findRowCount();
        } catch (PersistenceException ex) {
            System.out.println("Installing database for SimpleRailway due to first time usage");
            installDDL();
        }

        getCommand("spawner").setExecutor(new SimpleRailwaySpawnerExecutor(this));
        getCommand("despawner").setExecutor(new SimpleRailwayDespawnerExecutor(this));
    }

    @Override
    public List<Class<?>> getDatabaseClasses() {
        List<Class<?>> list = new ArrayList<Class<?>>();
        list.add(SimpleRailwaySpawner.class);
        list.add(SimpleRailwayDespawner.class);
        return list;
    }

    @Override
    public void onDisable() {
        HandlerList.unregisterAll(this);
    }
}
