package net.engin33r.bukkit.SimpleRailway;

import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.dao.DaoManager;
import com.j256.ormlite.support.ConnectionSource;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.BlockFace;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Minecart;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitScheduler;
import org.bukkit.util.Vector;

import java.sql.SQLException;
import java.util.List;
import java.util.logging.Level;

/**
 * A {@link CommandExecutor} implementation for the /spawner command.
 */
public class SimpleRailwaySpawnerExecutor implements CommandExecutor {
    private final SimpleRailway plugin;
    private Dao<SimpleRailwaySpawner, String> spawnerDao;
    private final BukkitScheduler scheduler = Bukkit.getServer().getScheduler();

    private static BlockFace stringToBlockFace(String s) {
        switch (s.toLowerCase()) {
            case "n":
                return BlockFace.NORTH;
            case "w":
                return BlockFace.WEST;
            case "s":
                return BlockFace.SOUTH;
            case "e":
                return BlockFace.EAST;
            default:
                return BlockFace.SELF;
        }
    }

    public SimpleRailwaySpawnerExecutor(SimpleRailway plugin, ConnectionSource source) {
        this.plugin = plugin;
        try {
            this.spawnerDao = DaoManager.createDao(source, SimpleRailwaySpawner.class);
        } catch (SQLException e) {
            plugin.getLogger().log(Level.INFO, "An exception has occurred", e);
        }
    }

    @Override
    public boolean onCommand(final CommandSender sender, Command command, String label, final String[] args) {
        if (args.length == 0) {
            sender.sendMessage(ChatColor.RED + "Usage: /spawner <create|delete|edit|activate|list>");
            return true;
        }

        if (args[0].equalsIgnoreCase("create")) {
            if (sender instanceof Player && !sender.hasPermission("simplerailway.spawner.create")) {
                sender.sendMessage(ChatColor.RED + "You do not have permission to use /spawner create");
                return true;
            }

            // Name and direction are always mandatory. If x, y and z aren't specified, the sender's current position
            // is used.
            final String name, direction;
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

            World tworld = Bukkit.getServer().getWorlds().get(0);
            if (sender instanceof Entity) tworld = ((Entity) sender).getWorld();

            final World world = tworld;
            final Location loc = new Location(world, x, y, z);

            // Find spawner by name and insert if one doesn't exist
            scheduler.runTaskAsynchronously(plugin, new Runnable() {
                @Override
                public void run() {
                    try {
                        SimpleRailwaySpawner spawner = new SimpleRailwayQuery(spawnerDao)
                                .getSpawnerByName(name);

                        if (spawner != null) {
                            if (sender instanceof Player) {
                                sender.sendMessage(ChatColor.RED + "A spawner with this name already exists!");
                                return;
                            }
                        }

                        // Set the fields and save the spawner entry into the database.
                        spawner = new SimpleRailwaySpawner();
                        spawner.setName(name);
                        spawner.setLocation(loc);
                        spawner.setWorldName(world.getName());
                        spawner.setDirection(stringToBlockFace(direction));

                        spawnerDao.create(spawner);
                        if (sender instanceof Player)
                            sender.sendMessage(ChatColor.GREEN + "Created spawner "
                                    + ChatColor.BOLD + "'" + name + "'");
                        plugin.getLogger().info("Created spawner '" + name + "'");
                    } catch (SQLException e) {
                        plugin.getLogger().log(Level.INFO, "An exception has occurred", e);
                    }
                }
            });
            return true;
        } else if (args[0].equalsIgnoreCase("delete")) {
            if (sender instanceof Player && !sender.hasPermission("simplerailway.spawner.delete")) {
                sender.sendMessage(ChatColor.RED + "You do not have permission to use /spawner delete");
                return true;
            }

            if (args.length < 2) {
                if (sender instanceof Player) {
                    sender.sendMessage(ChatColor.RED + "Usage: /spawner delete <name>");
                }
                return true;
            }
            final String name = args[1];

            // Find spawner by name and delete if found
            scheduler.runTaskAsynchronously(plugin, new Runnable() {
                @Override
                public void run() {
                    try {
                        SimpleRailwaySpawner spawner = new SimpleRailwayQuery(spawnerDao)
                                .getSpawnerByName(name);

                        if (spawner == null) {
                            if (sender instanceof Player) {
                                sender.sendMessage(ChatColor.RED + "No spawner was found with the specified name!");
                                return;
                            }
                        }

                        spawnerDao.delete(spawner);
                        if (sender instanceof Player)
                            sender.sendMessage(ChatColor.GREEN + "Deleted spawner "
                                    + ChatColor.BOLD + "'" + name + "'");
                        plugin.getLogger().info("Deleted spawner '" + name + "'");
                    } catch (SQLException e) {
                        plugin.getLogger().log(Level.INFO, "An exception has occurred", e);
                    }
                }
            });
            return true;
        } else if (args[0].equalsIgnoreCase("activate")) {
            if (sender instanceof Player && !sender.hasPermission("simplerailway.spawner.activate")) {
                sender.sendMessage(ChatColor.RED + "You do not have permission to use /spawner activate");
                return true;
            }

            if (args.length < 2) {
                if (sender instanceof Player) {
                    sender.sendMessage(ChatColor.RED + "Usage: /spawner activate <name> [<push (true|false)> "
                        + "<player>]");
                }
                return true;
            }
            final String name = args[1];

            // Process spawning; first, find the spawner with the given name
            scheduler.runTaskAsynchronously(plugin, new Runnable() {
                @Override
                public void run() {
                    try {
                        final SimpleRailwaySpawner spawner = new SimpleRailwayQuery(spawnerDao)
                                .getSpawnerByName(name);

                        if (spawner == null) {
                            if (sender instanceof Player) {
                                sender.sendMessage(ChatColor.RED + "No spawner was found with the specified name!");
                            }
                        } else {
                            // Spawn minecart, force mount player and apply force if necessary
                            scheduler.runTask(plugin, new Runnable() {
                                @Override
                                public void run() {
                                    final Minecart cart = Bukkit.getServer().getWorld(spawner.getWorldName())
                                            .spawn(spawner.getLocation(), Minecart.class);

                                    // Register cart in the database and add it to the list
                                    plugin.getListener().registerCart(cart);

                                    // If "push" is specified, assign a normal direction vector according to
                                    // the cardinal directions as the velocity and apply it to the cart
                                    if (args.length > 2) {
                                        if (args.length > 3) {
                                            Player p = Bukkit.getPlayer(args[3]);
                                            if (p != null) {
                                                if (cart.getLocation().distance(p.getLocation())
                                                        < plugin.getConfig().getDouble("min-mount-distance")) {
                                                    cart.setPassenger(p);
                                                }
                                            }
                                        }
                                        if (args[2].equalsIgnoreCase("true")) {
                                            BlockFace d = spawner.getDirection();
                                            cart.setVelocity(new Vector(d.getModX(), d.getModY(), d.getModZ()));
                                        }
                                    }
                                }
                            });

                            plugin.getLogger().info("Activated spawner '" + name + "'");
                        }
                    } catch (SQLException e) {
                        plugin.getLogger().log(Level.INFO, "An exception has occurred", e);
                    }
                }
            });
            return true;
        } else if (args[0].equalsIgnoreCase("edit")) {
            if (sender instanceof Player && !sender.hasPermission("simplerailway.spawner.edit")) {
                sender.sendMessage(ChatColor.RED + "You do not have permission to use /spawner edit");
                return true;
            }

            if (args.length == 4) {
                // Find spawner by name and edit field if found
                scheduler.runTaskAsynchronously(plugin, new Runnable() {
                    @Override
                    public void run() {
                        try {
                            SimpleRailwaySpawner spawner = new SimpleRailwayQuery(spawnerDao)
                                    .getSpawnerByName(args[1]);

                            if (spawner == null) {
                                if (sender instanceof Player) {
                                    sender.sendMessage(ChatColor.RED
                                            + "No spawner was found with the specified name!");
                                }
                            } else {
                                if (args[2].equalsIgnoreCase("x")) {
                                    spawner.setX(Double.valueOf(args[3]));
                                } else if (args[2].equalsIgnoreCase("y")) {
                                    spawner.setY(Double.valueOf(args[3]));
                                } else if (args[2].equalsIgnoreCase("z")) {
                                    spawner.setZ(Double.valueOf(args[3]));
                                } else if (args[2].equalsIgnoreCase("direction")) {
                                    spawner.setDirection(stringToBlockFace(args[2]));
                                } else if (args[2].equalsIgnoreCase("world")) {
                                    spawner.setWorldName(args[3]);
                                }

                                spawnerDao.update(spawner);
                                if (sender instanceof Player)
                                    sender.sendMessage(ChatColor.GREEN + "Updated spawner " + ChatColor.BOLD
                                            + "'" + args[1] + "'");
                                plugin.getLogger().info("Updated spawner '" + args[1] + "'");
                            }
                        } catch (SQLException e) {
                            plugin.getLogger().log(Level.INFO, "An exception has occurred", e);
                        }
                    }
                });
                return true;
            } else {
                if (sender instanceof Player) {
                    sender.sendMessage(ChatColor.RED + "Usage: /spawner edit <name> <x|y|z|direction|world> <value>");
                }
                return true;
            }

        } else if (args[0].equalsIgnoreCase("list")) {
            if (sender instanceof Player && !sender.hasPermission("simplerailway.spawner.list")) {
                sender.sendMessage(ChatColor.RED + "You do not have permission to use /spawner list");
                return true;
            }

            // Find all of the spawners and list them to the executor
            scheduler.runTaskAsynchronously(plugin, new Runnable() {
                @Override
                public void run() {
                    try {
                        List<SimpleRailwaySpawner> list = spawnerDao.queryBuilder().query();
                        if (sender instanceof Player) {
                            for (SimpleRailwaySpawner spawner : list) {
                                sender.sendMessage(spawner.getName() + ": at ["
                                        + spawner.getX() + ";" + spawner.getY() + ";"
                                        + spawner.getZ() + "] facing " + spawner.getDirection().toString()
                                        + " in world '" + spawner.getWorldName() + "'");
                            }
                        }
                    } catch (SQLException e) {
                        plugin.getLogger().log(Level.INFO, "An exception has occurred", e);
                    }
                }
            });
            return true;
        }

        sender.sendMessage(ChatColor.RED + "Usage: /spawner <create|delete|edit|activate|list>");
        return true;
    }
}
