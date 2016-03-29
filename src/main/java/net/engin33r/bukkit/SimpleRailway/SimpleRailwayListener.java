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
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.event.world.ChunkUnloadEvent;
import org.bukkit.scheduler.BukkitScheduler;

import java.sql.SQLException;
import java.util.*;
import java.util.logging.Level;

public final class SimpleRailwayListener implements Listener {
    private final SimpleRailway plugin;
    private List<SimpleRailwayDespawner> list;
    private Dao<SimpleRailwayDespawner, String> despawnerDao;
    private Dao<SimpleRailwayCart, Integer> cartDao;
    private final Set<Vehicle> spawnedCarts = Collections.newSetFromMap(new WeakHashMap<Vehicle, Boolean>());
    private final BukkitScheduler scheduler = Bukkit.getServer().getScheduler();

    private static Vehicle getCartByUUID(UUID id) {
        for (World w : Bukkit.getServer().getWorlds()) {
            for (Minecart e : w.getEntitiesByClass(Minecart.class)) {
                if (e.getUniqueId().equals(id)) {
                    return e;
                }
            }
        }
        return null;
    }

    public SimpleRailwayListener(final SimpleRailway plugin, final ConnectionSource source) {
        this.plugin = plugin;
        plugin.getServer().getPluginManager().registerEvents(this, plugin);

        try {
            // Instantiate DAO's and form a list of all despawners
            despawnerDao = DaoManager.createDao(source, SimpleRailwayDespawner.class);
            cartDao = DaoManager.createDao(source, SimpleRailwayCart.class);
            SimpleRailwayListener.this.list = despawnerDao.queryBuilder().query();

            // Form a list of all spawner-created carts
            List<SimpleRailwayCart> carts = cartDao.queryBuilder().query();
            for (SimpleRailwayCart c : carts) {
                Vehicle v = getCartByUUID(c.getId());
                if (v != null) spawnedCarts.add(v);
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.INFO, "An exception has occurred", e);
        }
    }

    public void updateList() {
        scheduler.runTaskAsynchronously(plugin, new Runnable() {
            @Override
            public void run() {
                try {
                    SimpleRailwayListener.this.list = despawnerDao.queryBuilder().query();
                } catch (SQLException e) {
                    plugin.getLogger().log(Level.INFO, "An exception has occurred", e);
                }
            }
        });
    }

    public void registerCart(final Vehicle v) {
        spawnedCarts.add(v);
        final UUID entityID = v.getUniqueId();
        scheduler.runTaskAsynchronously(plugin, new Runnable() {
            @Override
            public void run() {
                try {
                    SimpleRailwayCart cart = new SimpleRailwayCart();
                    cart.setId(entityID);
                    cartDao.create(cart);
                } catch (SQLException e) {
                    plugin.getLogger().log(Level.INFO, "An exception has occurred", e);
                }
            }
        });
    }

    private void unregisterCart(final Vehicle v) {
        spawnedCarts.remove(v);
        final UUID entityID = v.getUniqueId();
        scheduler.runTaskAsynchronously(plugin, new Runnable() {
            @Override
            public void run() {
                try {
                    SimpleRailwayCart cart = new SimpleRailwayQuery(cartDao)
                            .getCartByUUID(entityID);
                    cartDao.delete(cart);
                } catch (SQLException e) {
                    plugin.getLogger().log(Level.INFO, "An exception has occurred", e);
                }
            }
        });
    }

    @EventHandler
    public void onChunkLoad(final ChunkLoadEvent event) {
        scheduler.runTaskAsynchronously(plugin, new Runnable() {
            @Override
            public void run() {
                try {
                    for (Entity e : event.getChunk().getEntities()) {
                        if (e instanceof Minecart && !spawnedCarts.contains(e)) {
                            SimpleRailwayCart cart = new SimpleRailwayQuery(cartDao)
                                    .getCartByUUID(e.getUniqueId());
                            if (cart != null) {
                                spawnedCarts.add((Vehicle) e);
                            }
                        }
                    }
                } catch (SQLException e) {
                    plugin.getLogger().log(Level.INFO, "An exception has occurred", e);
                }
            }
        });
    }

    @EventHandler
    public void onChunkUnload(final ChunkUnloadEvent event) {
        for (Entity e : event.getChunk().getEntities()) {
            if (e instanceof Minecart && spawnedCarts.contains(e)) {
                spawnedCarts.remove(e);
            }
        }
    }

    @EventHandler
    public void onMinecartHit(VehicleDamageEvent event) {
        if (event.getAttacker() instanceof Player && event.getVehicle() instanceof Minecart) {
            // Prevent player damage to spawner-created carts unless the player has permission
            if (spawnedCarts.contains(event.getVehicle()) &&
                    !((Player) event.getAttacker()).hasPermission("simplerailway.cartdamage")) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void onMinecartEnter(VehicleEnterEvent event) {
        if (event.getVehicle() instanceof Minecart && event.getEntered() instanceof Player) {
            // Logout posterity check: if a player enters a cart registered in the DB but not the in-memory set,
            // add it to the set
            final Vehicle v = event.getVehicle();
            scheduler.runTaskAsynchronously(plugin, new Runnable() {
                @Override
                public void run() {
                    try {
                        SimpleRailwayCart cart = new SimpleRailwayQuery(cartDao).getCartByUUID(v.getUniqueId());

                        if (cart != null && !spawnedCarts.contains(v)) {
                            spawnedCarts.add(v);
                        }
                    } catch (SQLException e) {
                        plugin.getLogger().log(Level.INFO, "An exception has occurred", e);
                    }
                }
            });
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
