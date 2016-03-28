package net.engin33r.bukkit.SimpleRailway;

import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.dao.DaoManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitScheduler;

import java.sql.SQLException;
import java.util.List;

/**
 * A {@link CommandExecutor} implementation for the /despawner command.
 */
public class SimpleRailwayDespawnerExecutor implements CommandExecutor {
    private final SimpleRailway plugin;
    private Dao<SimpleRailwayDespawner, String> despawnerDao;
    private final BukkitScheduler scheduler = Bukkit.getServer().getScheduler();

    public SimpleRailwayDespawnerExecutor(SimpleRailway plugin) {
        this.plugin = plugin;
        try {
            this.despawnerDao = DaoManager.createDao(plugin.getConnectionSource(), SimpleRailwayDespawner.class);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public boolean onCommand(final CommandSender sender, Command command, String label, final String[] args) {
        if (args.length == 0) {
            sender.sendMessage(ChatColor.RED + "Usage: /despawner <create|delete|edit|list>");
            return true;
        }

        if (args[0].equalsIgnoreCase("create")) {
            if (sender instanceof Player && !sender.hasPermission("simplerailway.despawner.create")) {
                sender.sendMessage(ChatColor.RED + "You do not have permission to use /despawner create");
                return true;
            }

            final String name;
            double x, y, z;
            if (args.length == 2) {
                Location loc = ((Entity) sender).getLocation();
                x = loc.getX();
                y = loc.getY();
                z = loc.getZ();
                name = args[1];
            } else if (args.length == 5) {
                x = Double.parseDouble(args[1]);
                y = Double.parseDouble(args[2]);
                z = Double.parseDouble(args[3]);
                name = args[4];
            } else {
                if (sender instanceof Player) {
                    sender.sendMessage(ChatColor.RED + "Usage: /despawner create <name> [<x> <y> <z>]");
                }
                return true;
            }

            final World world = ((Entity) sender).getWorld();
            final Location loc = new Location(world, x, y, z);

            //SimpleRailwayDespawner despawner = plugin.getDatabase().find(SimpleRailwayDespawner.class).where()
            //        .ieq("name", name).findUnique();
            scheduler.runTaskAsynchronously(plugin, new Runnable() {
                @Override
                public void run() {
                    try {
                        SimpleRailwayDespawner despawner = despawnerDao.queryBuilder().where().eq("name", name).queryForFirst();

                        if (despawner != null) {
                            if (sender instanceof Player) {
                                sender.sendMessage(ChatColor.RED + "A despawner with this name already exists!");
                                return;
                            }
                        }

                        despawner = new SimpleRailwayDespawner();
                        despawner.setName(name);
                        despawner.setLocation(loc);
                        despawner.setWorldName(world.getName());
                        despawnerDao.create(despawner);
                        sender.sendMessage(ChatColor.GREEN + "Created despawner "
                                + ChatColor.BOLD+ "'" + name + "'");
                        plugin.getLogger().info("Created despawner '" + name + "'");
                        plugin.getListener().updateList();
                    } catch (SQLException e) {
                        e.printStackTrace();
                    }
                }
            });
            return true;
        } else if (args[0].equalsIgnoreCase("delete")) {
            if (sender instanceof Player && !sender.hasPermission("simplerailway.despawner.delete")) {
                sender.sendMessage(ChatColor.RED + "You do not have permission to use /despawner delete");
                return true;
            }

            if (args.length < 2) {
                if (sender instanceof Player) {
                    sender.sendMessage(ChatColor.RED + "Usage: /despawner delete <name>");
                }
                return true;
            }
            final String name = args[1];

            //SimpleRailwayDespawner despawner = plugin.getDatabase().find(SimpleRailwayDespawner.class).where()
            //        .ieq("name", name).findUnique();

            scheduler.runTaskAsynchronously(plugin, new Runnable() {
                @Override
                public void run() {
                    try {
                        final SimpleRailwayDespawner despawner = despawnerDao.queryBuilder().where().eq("name", name)
                                .queryForFirst();

                        if (despawner == null) {
                            if (sender instanceof Player) {
                                sender.sendMessage(ChatColor.RED + "No despawner was found with the specified name!");
                                return;
                            }
                        }

                        despawnerDao.delete(despawner);
                        sender.sendMessage(ChatColor.GREEN + "Deleted despawner "
                                + ChatColor.BOLD + "'" + name + "'");
                        plugin.getLogger().info("Deleted despawner '" + name + "'");
                    } catch (SQLException e) {
                        e.printStackTrace();
                    }
                }
            });
            return true;
        } else if (args[0].equalsIgnoreCase("edit")) {
            if (sender instanceof Player && !sender.hasPermission("simplerailway.despawner.edit")) {
                sender.sendMessage(ChatColor.RED + "You do not have permission to use /despawner edit");
                return true;
            }

            if (args.length == 4) {
                //SimpleRailwayDespawner despawner = plugin.getDatabase().find(SimpleRailwayDespawner.class).where()
                //        .ieq("name", args[1]).findUnique();
                scheduler.runTaskAsynchronously(plugin, new Runnable() {
                    @Override
                    public void run() {
                        try {
                            SimpleRailwayDespawner despawner = despawnerDao.queryBuilder().where().eq("name", args[1])
                                    .queryForFirst();

                            if (despawner == null) {
                                if (sender instanceof Player) {
                                    sender.sendMessage(ChatColor.RED
                                            + "No despawner was found with the specified name!");
                                    return;
                                }
                            }

                            if (args[2].equalsIgnoreCase("x")) {
                                despawner.setX(Double.valueOf(args[3]));
                            } else if (args[2].equalsIgnoreCase("y")) {
                                despawner.setY(Double.valueOf(args[3]));
                            } else if (args[2].equalsIgnoreCase("z")) {
                                despawner.setZ(Double.valueOf(args[3]));
                            } else if (args[2].equalsIgnoreCase("world")) {
                                despawner.setWorldName(args[3]);
                            }

                            despawnerDao.update(despawner);
                            if (sender instanceof Player)
                                sender.sendMessage(ChatColor.GREEN + "Updated despawner " + ChatColor.BOLD + "'"
                                        + args[1] + "'");
                            plugin.getLogger().info("Updated despawner '" + args[1] + "'");
                        } catch (SQLException e) {
                            e.printStackTrace();
                        }
                    }
                });
                return true;
            } else {
                if (sender instanceof Player) {
                    sender.sendMessage(ChatColor.RED + "Usage: /despawner edit <name> <x|y|z|world> <newValue>");
                }
                return true;
            }
        } else if (args[0].equalsIgnoreCase("list")) {
            if (sender instanceof Player && !sender.hasPermission("simplerailway.despawner.list")) {
                sender.sendMessage(ChatColor.RED + "You do not have permission to use /despawner list");
                return true;
            }

            //List<SimpleRailwayDespawner> list = plugin.getDatabase().find(SimpleRailwayDespawner.class).findList();
            scheduler.runTaskAsynchronously(plugin, new Runnable() {
                @Override
                public void run() {
                    try {
                        List<SimpleRailwayDespawner> list = despawnerDao.queryBuilder().query();
                        if (sender instanceof Player) {
                            for (SimpleRailwayDespawner despawner : list) {
                                sender.sendMessage(despawner.getName() + ": at [" + despawner.getX() + ";"
                                        + despawner.getY() + ";" + despawner.getZ() + "] in world '"
                                        + despawner.getWorldName() + "'");
                            }
                        }
                    } catch (SQLException e) {
                        plugin.getLogger().severe(e.getMessage());
                    }
                }
            });
            return true;
        }

        sender.sendMessage(ChatColor.RED + "Usage: /despawner <create|delete|edit|list>");
        return true;
    }
}
