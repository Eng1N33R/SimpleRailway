package net.engin33r.bukkit.SimpleRailway;

import com.j256.ormlite.jdbc.JdbcConnectionSource;
import com.j256.ormlite.support.ConnectionSource;
import com.j256.ormlite.table.TableUtils;
import org.bukkit.plugin.java.JavaPlugin;

import java.sql.SQLException;
import java.util.logging.Level;

public class SimpleRailway extends JavaPlugin {
    private ConnectionSource source;
    private SimpleRailwayListener listener;

    @Override
    public void onEnable() {
        this.saveDefaultConfig();

        try {
            this.source = new JdbcConnectionSource("jdbc:sqlite:plugins/SimpleRailway/SimpleRailway.db");
            TableUtils.createTableIfNotExists(source, SimpleRailwaySpawner.class);
            TableUtils.createTableIfNotExists(source, SimpleRailwayDespawner.class);
            TableUtils.createTableIfNotExists(source, SimpleRailwayCart.class);
        } catch (SQLException e) {
            getLogger().log(Level.INFO, "An exception has occurred", e);
        }

        this.listener = new SimpleRailwayListener(this, source);

        getCommand("spawner").setExecutor(new SimpleRailwaySpawnerExecutor(this, source));
        getCommand("despawner").setExecutor(new SimpleRailwayDespawnerExecutor(this, source));
    }

    @Override
    public void onDisable() {
        try {
            source.close();
        } catch (SQLException e) {
            getLogger().log(Level.INFO, "An exception has occurred", e);
        }
    }

    public SimpleRailwayListener getListener() {
        return this.listener;
    }
}
