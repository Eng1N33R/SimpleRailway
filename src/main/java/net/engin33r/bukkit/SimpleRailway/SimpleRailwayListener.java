package net.engin33r.bukkit.SimpleRailway;

import org.bukkit.entity.Minecart;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.vehicle.VehicleDamageEvent;
import org.bukkit.event.vehicle.VehicleMoveEvent;
import org.bukkit.metadata.MetadataValue;
import org.bukkit.metadata.Metadatable;
import org.bukkit.plugin.Plugin;

import java.util.List;

public final class SimpleRailwayListener implements Listener {
    private final SimpleRailway plugin;
    private List<SimpleRailwayDespawner> list;

    public SimpleRailwayListener(SimpleRailway plugin) {
        this.plugin = plugin;
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
        this.list = plugin.getDatabase().find(SimpleRailwayDespawner.class).findList();
    }

    public Object getMetadata(Metadatable object, String key, Plugin plugin) {
        List<MetadataValue> values = object.getMetadata(key);
        for (MetadataValue value : values) {
            if (value.getOwningPlugin() == plugin) {
                return value.value();
            }
        }
        return null;
    }

    @EventHandler
    public void onMinecartHit(VehicleDamageEvent event) {
        if (event.getAttacker() instanceof Player) {
            if (event.getVehicle() instanceof Minecart) {
                if (event.getVehicle().hasMetadata("fromSpawner")
                        && ((boolean) getMetadata(event.getVehicle(), "fromSpawner", plugin)) &&
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
                if (despawner.getLocation().distance(event.getVehicle().getLocation()) <= 0.3) {
                    event.getVehicle().eject();
                    event.getVehicle().remove();
                }
            }
        }
    }
}
