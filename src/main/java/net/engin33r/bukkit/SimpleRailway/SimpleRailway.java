package net.engin33r.bukkit.SimpleRailway;

import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.dao.DaoManager;
import com.j256.ormlite.jdbc.JdbcConnectionSource;
import com.j256.ormlite.support.ConnectionSource;
import com.j256.ormlite.table.TableUtils;
import org.bukkit.event.HandlerList;
import org.bukkit.plugin.java.JavaPlugin;

import java.sql.SQLException;

public class SimpleRailway extends JavaPlugin {
    private ConnectionSource source;
    private SimpleRailwayListener listener;

    @Override
    public void onEnable() {
        this.saveDefaultConfig();

        /*try {
            getDatabase().find(SimpleRailwaySpawner.class).findRowCount();
            getDatabase().find(SimpleRailwayDespawner.class).findRowCount();
        } catch (PersistenceException ex) {
            System.out.println("Installing database for SimpleRailway due to first time usage");
            installDDL();
        }*/

        try {
            this.source = new JdbcConnectionSource("jdbc:sqlite:plugins/SimpleRailway/SimpleRailway.db");
            try {
                Dao<SimpleRailwaySpawner, String> dao = DaoManager.createDao(source, SimpleRailwaySpawner.class);
                dao.queryBuilder().query();
            } catch (SQLException e) {
                TableUtils.createTable(source, SimpleRailwaySpawner.class);
            }
            try {
                Dao<SimpleRailwayDespawner, String> dao = DaoManager.createDao(source, SimpleRailwayDespawner.class);
                dao.queryBuilder().query();
            } catch (SQLException e) {
                TableUtils.createTable(source, SimpleRailwayDespawner.class);
            }
            try {
                Dao<SimpleRailwayCart, Integer> dao = DaoManager.createDao(source, SimpleRailwayCart.class);
                dao.queryBuilder().query();
            } catch (SQLException e) {
                TableUtils.createTable(source, SimpleRailwayCart.class);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        this.listener = new SimpleRailwayListener(this);

        getCommand("spawner").setExecutor(new SimpleRailwaySpawnerExecutor(this));
        getCommand("despawner").setExecutor(new SimpleRailwayDespawnerExecutor(this));
    }

    @Override
    public void onDisable() {
        HandlerList.unregisterAll(this);
        try {
            source.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /*@Override
    public List<Class<?>> getDatabaseClasses() {
        List<Class<?>> list = new ArrayList<Class<?>>();
        list.add(SimpleRailwaySpawner.class);
        list.add(SimpleRailwayDespawner.class);
        return list;
    }*/

    public ConnectionSource getConnectionSource() {
        return this.source;
    }

    public SimpleRailwayListener getListener() {
        return this.listener;
    }
}
