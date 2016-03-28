package net.engin33r.bukkit.SimpleRailway;

import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;

@DatabaseTable(tableName="sr_spawners")
public class SimpleRailwaySpawner {
    @DatabaseField(id = true)
    private String name;

    @DatabaseField(canBeNull = false)
    private double x;
    @DatabaseField(canBeNull = false)
    private double y;
    @DatabaseField(canBeNull = false)
    private double z;

    @DatabaseField(canBeNull = false)
    private String direction;

    @DatabaseField(canBeNull = false)
    private String worldName;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public double getX() {
        return x;
    }

    public void setX(double x) {
        this.x = x;
    }

    public double getY() {
        return y;
    }

    public void setY(double y) {
        this.y = y;
    }

    public double getZ() {
        return z;
    }

    public void setZ(double z) {
        this.z = z;
    }

    public String getDirection() {
        return direction;
    }

    public void setDirection(String direction) {
        this.direction = direction;
    }

    public String getWorldName() {
        return worldName;
    }

    public void setWorldName(String worldName) {
        this.worldName = worldName;
    }

    public void setLocation(Location loc) {
        this.worldName = loc.getWorld().getName();
        this.x = loc.getX();
        this.y = loc.getY();
        this.z = loc.getZ();
    }

    public Location getLocation() {
        World world = Bukkit.getServer().getWorld(worldName);
        return new Location(world, x, y, z);
    }
}
