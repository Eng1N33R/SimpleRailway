package net.engin33r.bukkit.SimpleRailway;

import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.dao.DaoManager;
import com.j256.ormlite.support.ConnectionSource;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Minecart;
import org.bukkit.entity.Player;
import org.bukkit.entity.Vehicle;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.vehicle.*;
import org.bukkit.scheduler.BukkitScheduler;

import java.sql.SQLException;
import java.util.LinkedList;
import java.util.List;

public final class SimpleRailwayListener implements Listener {
    private final SimpleRailway plugin;
    private List<SimpleRailwayDespawner> list;
    private Dao<SimpleRailwayDespawner, String> despawnerDao;
    private Dao<SimpleRailwayCart, Integer> cartDao;
    private List<Vehicle> spawnedCarts = new LinkedList<>();
    private BukkitScheduler scheduler = Bukkit.getServer().getScheduler();

    public SimpleRailwayListener(SimpleRailway plugin) {
        this.plugin = plugin;
        plugin.getServer().getPluginManager().registerEvents(this, plugin);

        final ConnectionSource source = SimpleRailwayListener.this.plugin.getConnectionSource();
        scheduler.runTaskAsynchronously(plugin, new Runnable() {
            @Override
            public void run() {
                try {
                    // Instantiate DAO's and form a list of all despawners
                    despawnerDao = DaoManager.createDao(source, SimpleRailwayDespawner.class);
                    cartDao = DaoManager.createDao(source, SimpleRailwayCart.class);
                    SimpleRailwayListener.this.list = despawnerDao.queryBuilder().query();

                    // Form a list of all spawner-created carts
                    List<SimpleRailwayCart> carts = cartDao.queryBuilder().query();
                    for (World w : Bukkit.getServer().getWorlds()) {
                        for (Entity e : w.getEntitiesByClass(Minecart.class)) {
                            for (SimpleRailwayCart c : carts) {
                                if (e.getEntityId() == c.getId()) {
                                    spawnedCarts.add((Vehicle) e);
                                }
                            }
                        }
                    }
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    public void updateList() {
        scheduler.runTaskAsynchronously(plugin, new Runnable() {
            @Override
            public void run() {
                try {
                    SimpleRailwayListener.this.list = despawnerDao.queryBuilder().query();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    public void registerCart(final Vehicle v) {
        spawnedCarts.add(v);
        final int entityID = v.getEntityId();
        scheduler.runTaskAsynchronously(plugin, new Runnable() {
            @Override
            public void run() {
                try {
                    SimpleRailwayCart cart = new SimpleRailwayCart();
                    cart.setId(entityID);
                    cartDao.create(cart);
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    private void unregisterCart(final Vehicle v) {
        spawnedCarts.remove(v);
        final int entityID = v.getEntityId();
        scheduler.runTaskAsynchronously(plugin, new Runnable() {
            @Override
            public void run() {
                try {
                    SimpleRailwayCart cart = cartDao.queryBuilder().where().eq("id", entityID)
                            .queryForFirst();
                    cartDao.delete(cart);
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    @EventHandler
    public void onMinecartHit(VehicleDamageEvent event) {
        if (event.getAttacker() instanceof Player) {
            if (event.getVehicle() instanceof Minecart) {
                // Prevent player damage to spawner-created carts unless the player has permission
                if (spawnedCarts.contains(event.getVehicle()) &&
                        !((Player) event.getAttacker()).hasPermission("simplerailway.cartdamage")) {
                    event.setCancelled(true);
                }
            }
        }
    }

    @EventHandler
    public void onMinecartMove(VehicleMoveEvent event) {
        if (event.getVehicle() instanceof Minecart) {
            for (SimpleRailwayDespawner despawner : list) {
                if (despawner.getLocation().distance(event.getVehicle().getLocation())
                        <= plugin.getConfig().getDouble("min-despawn-distance")) {
                    unregisterCart(event.getVehicle());
                    event.getVehicle().eject();
                    event.getVehicle().remove();
                }
            }
        }
    }

    @EventHandler
    public void onMinecartDespawn(VehicleDestroyEvent event) {
        if (event.getVehicle() instanceof Minecart) {
            unregisterCart(event.getVehicle());
        }
    }

    @EventHandler
    public void onMinecartExit(final VehicleExitEvent event) {
        if (event.getVehicle() instanceof Minecart && event.getExited() instanceof Player) {
            // Cancel dismount from a spawner-created cart unless they have permission
            if (spawnedCarts.contains(event.getVehicle()) &&
                    !((Player) event.getExited()).hasPermission("simplerailway.cartexit")) {
                event.setCancelled(true);
            }
        }
    }
}
