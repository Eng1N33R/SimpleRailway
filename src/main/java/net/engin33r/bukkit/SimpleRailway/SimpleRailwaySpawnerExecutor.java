package net.engin33r.bukkit.SimpleRailway;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Minecart;
import org.bukkit.entity.Player;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.util.Vector;

import java.util.List;

/**
 * A {@link CommandExecutor} implementation for the /spawner command.
 */
public class SimpleRailwaySpawnerExecutor implements CommandExecutor {
    private final SimpleRailway plugin;

    public SimpleRailwaySpawnerExecutor(SimpleRailway plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sender.sendMessage(ChatColor.RED + "Usage: /spawner <create|delete|edit|activate|list>");
            return true;
        }

        if (args[0].equalsIgnoreCase("create")) {
            if (sender instanceof Player && !((Player) sender).hasPermission("simplerailway.spawner.create")) {
                sender.sendMessage(ChatColor.RED + "You do not have permission to use /spawner create");
                return true;
            }

            // Name and direction are always mandatory. If x, y and z aren't specified, the sender's current position
            // is used.
            String name, direction;
            double x, y, z;
            if (args.length == 3) {
                Location loc = ((Entity) sender).getLocation();
                x = loc.getX();
                y = loc.getY();
                z = loc.getZ();
                name = args[1];
                direction = args[2];
            } else if (args.length == 5) {
                name = args[1];
                x = Double.parseDouble(args[2]);
                y = Double.parseDouble(args[3]);
                z = Double.parseDouble(args[4]);
                direction = args[5];
            } else {
                if (sender instanceof Player) {
                    sender.sendMessage(ChatColor.RED + "Usage: /spawner create <name> [<x> <y> <z>] <n|w|s|e>");
                }
                return true;
            }

            World world = ((Entity) sender).getWorld();
            Location loc = new Location(world, x, y, z);

            SimpleRailwaySpawner spawner = plugin.getDatabase().find(SimpleRailwaySpawner.class).where()
                    .ieq("name", name).findUnique();

            if (spawner != null) {
                if (sender instanceof Player) {
                    sender.sendMessage(ChatColor.RED + "A spawner with this name already exists!");
                    return true;
                }
            }

            // Set the fields and save the spawner entry into the database.
            spawner = new SimpleRailwaySpawner();
            spawner.setName(name);
            spawner.setLocation(loc);
            spawner.setWorldName(world.getName());
            spawner.setDirection(direction);

            plugin.getDatabase().save(spawner);
            sender.sendMessage(ChatColor.GREEN + "Created spawner " + ChatColor.BOLD + name);
            plugin.getLogger().info("Created spawner " + name);
            return true;
        } else if (args[0].equalsIgnoreCase("delete")) {
            if (sender instanceof Player && !((Player) sender).hasPermission("simplerailway.spawner.delete")) {
                sender.sendMessage(ChatColor.RED + "You do not have permission to use /spawner delete");
                return true;
            }

            if (args.length < 2) {
                if (sender instanceof Player) {
                    sender.sendMessage(ChatColor.RED + "Usage: /spawner delete <name>");
                }
                return true;
            }
            String name = args[1];

            SimpleRailwaySpawner spawner = plugin.getDatabase().find(SimpleRailwaySpawner.class).where()
                    .ieq("name", name).findUnique();

            if (spawner == null) {
                if (sender instanceof Player) {
                    sender.sendMessage(ChatColor.RED + "No spawner was found with the specified name!");
                    return true;
                }
            }

            plugin.getDatabase().delete(spawner);
            sender.sendMessage(ChatColor.GREEN + "Deleted spawner " + ChatColor.BOLD + name);
            plugin.getLogger().info("Deleted spawner " + name);
            return true;
        } else if (args[0].equalsIgnoreCase("activate")) {
            if (sender instanceof Player && !((Player) sender).hasPermission("simplerailway.spawner.activate")) {
                sender.sendMessage(ChatColor.RED + "You do not have permission to use /spawner activate");
                return true;
            }

            if (args.length < 2) {
                if (sender instanceof Player) {
                    sender.sendMessage(ChatColor.RED + "Usage: /spawner activate <name> [<push (true|false)>]");
                }
                return true;
            }
            String name = args[1];

            SimpleRailwaySpawner spawner = plugin.getDatabase().find(SimpleRailwaySpawner.class).where()
                    .ieq("name", name).findUnique();

            Minecart cart = Bukkit.getServer().getWorld(spawner.getWorldName()).spawn(spawner.getLocation(), Minecart.class);

            // If "push" is specified, assign a normal direction vector according to the cardinal directions
            // as the velocity and apply it to the cart
            if (args.length > 2 && args[2].equalsIgnoreCase("true")) {
                Vector velocity = new Vector(0, 0, 0);
                String direction = spawner.getDirection();
                if (direction.equalsIgnoreCase("n")) {
                    velocity = new Vector(0, 0, -1);
                } else if (direction.equalsIgnoreCase("w")) {
                    velocity = new Vector(-1, 0, 0);
                } else if (direction.equalsIgnoreCase("s")) {
                    velocity = new Vector(0, 0, 1);
                } else if (direction.equalsIgnoreCase("e")) {
                    velocity = new Vector(1, 0, 0);
                }

                cart.setVelocity(velocity);
            }
            cart.setMetadata("fromSpawner", new FixedMetadataValue(plugin, true));
            plugin.getLogger().info("Activated spawner " + name);
            return true;
        } else if (args[0].equalsIgnoreCase("edit")) {
            if (sender instanceof Player && !((Player) sender).hasPermission("simplerailway.spawner.edit")) {
                sender.sendMessage(ChatColor.RED + "You do not have permission to use /spawner edit");
                return true;
            }

            if (args.length == 4) {
                SimpleRailwaySpawner spawner = plugin.getDatabase().find(SimpleRailwaySpawner.class).where()
                        .ieq("name", args[1]).findUnique();

                if (spawner != null) {
                    if (args[2].equalsIgnoreCase("x")) {
                        spawner.setX(Double.valueOf(args[3]));
                    } else if (args[2].equalsIgnoreCase("y")) {
                        spawner.setY(Double.valueOf(args[3]));
                    } else if (args[2].equalsIgnoreCase("z")) {
                        spawner.setZ(Double.valueOf(args[3]));
                    } else if (args[2].equalsIgnoreCase("direction")) {
                        spawner.setDirection(args[3]);
                    } else if (args[2].equalsIgnoreCase("world")) {
                        spawner.setWorldName(args[3]);
                    }
                }
            } else {
                if (sender instanceof Player) {
                    sender.sendMessage(ChatColor.RED + "Usage: /spawner edit <name> <x|y|z|direction|world> <newValue>");
                }
                return true;
            }
        } else if (args[0].equalsIgnoreCase("list")) {
            if (sender instanceof Player && !((Player) sender).hasPermission("simplerailway.spawner.list")) {
                sender.sendMessage(ChatColor.RED + "You do not have permission to use /spawner list");
                return true;
            }

            List<SimpleRailwaySpawner> list = plugin.getDatabase().find(SimpleRailwaySpawner.class).findList();
            if (sender instanceof Player) {
                for (SimpleRailwaySpawner spawner : list) {
                    sender.sendMessage(spawner.getName() + ": [" + spawner.getX() + ";" + spawner.getY() + ";"
                            + spawner.getZ() + "] facing " + spawner.getDirection().toUpperCase()
                            + " in world " + spawner.getWorldName());
                }
            }
            return true;
        }

        sender.sendMessage(ChatColor.RED + "Usage: /spawner <create|delete|edit|activate|list>");
        return true;
    }
}
