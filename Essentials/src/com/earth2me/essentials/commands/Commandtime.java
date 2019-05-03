package com.earth2me.essentials.commands;

import com.earth2me.essentials.CommandSource;
import com.earth2me.essentials.User;
import com.earth2me.essentials.utils.DescParseTickFormat;
import com.earth2me.essentials.utils.NumberUtil;
import com.google.common.collect.Lists;
import org.bukkit.Server;
import org.bukkit.World;

import java.util.*;


public class Commandtime extends EssentialsCommand {
    public Commandtime() {
        super("time");
    }

    @Override
    public void run(final Server server, final CommandSource sender, final String commandLabel, final String[] args) throws Exception {
        boolean add = false;
        final List<String> argList = new ArrayList<>(Arrays.asList(args));
        if (argList.remove("set") && !argList.isEmpty() && NumberUtil.isInt(argList.get(0))) {
            argList.set(0, argList.get(0) + "t");
        }
        if (argList.remove("add") && !argList.isEmpty() && NumberUtil.isInt(argList.get(0))) {
            add = true;
            argList.set(0, argList.get(0) + "t");
        }
        final String[] validArgs = argList.toArray(new String[argList.size()]);

        // Which World(s) are we interested in?
        String worldSelector = null;
        if (validArgs.length == 2) {
            worldSelector = validArgs[1];
        }
        final Set<World> worlds = getWorlds(server, sender, worldSelector);
        final String setTime;

        // If no arguments we are reading the time
        if (validArgs.length == 0) {
            if (commandLabel.equalsIgnoreCase("day") || commandLabel.equalsIgnoreCase("eday")) {
                setTime = "day";
            } else if (commandLabel.equalsIgnoreCase("night") || commandLabel.equalsIgnoreCase("enight")) {
                setTime = "night";
            } else {
                getWorldsTime(sender, worlds);
                throw new NoChargeException();
            }
        } else {
            setTime = validArgs[0];
        }

        final User user = ess.getUser(sender.getPlayer());
        if (user != null && !user.isAuthorized("essentials.time.set")) {
            throw new Exception(sender.tl("timeSetPermission"));
        }

        // Parse the target time int ticks from args[0]
        long ticks;
        try {
            ticks = DescParseTickFormat.parse(setTime);
        } catch (NumberFormatException e) {
            throw new NotEnoughArgumentsException(e);
        }

        setWorldsTime(sender, worlds, ticks, add);
    }

    /**
     * Used to get the time and inform
     */
    private void getWorldsTime(final CommandSource sender, final Collection<World> worlds) {
        if (worlds.size() == 1) {
            final Iterator<World> iter = worlds.iterator();
            sender.sendMessage(DescParseTickFormat.format(iter.next().getTime()));
            return;
        }

        for (World world : worlds) {
            sender.sendTl("timeWorldCurrent", world.getName(), DescParseTickFormat.format(world.getTime()));
        }
    }

    /**
     * Used to set the time and inform of the change
     */
    private void setWorldsTime(final CommandSource sender, final Collection<World> worlds, final long ticks, final boolean add) throws Exception {
        User user = ess.getUser(sender.getPlayer());
        for (World world : worlds) {
            if (!canUpdateWorld(user, world)) {
                throw new Exception(user.tl("timeSetWorldPermission", user.getWorld().getName()));
            }
        }

        // Update the time
        for (World world : worlds) {
            long time = world.getTime();
            if (!add) {
                time -= time % 24000;
            }
            world.setTime(time + (add ? 0 : 24000) + ticks);
        }

        final StringBuilder output = new StringBuilder();
        for (World world : worlds) {
            if (output.length() > 0) {
                output.append(", ");
            }

            output.append(world.getName());
        }

        sender.sendTl("timeWorldSet", DescParseTickFormat.format(ticks), output.toString());
    }

    /**
     * Used to parse an argument of the type "world(s) selector"
     */
    private Set<World> getWorlds(final Server server, final CommandSource sender, final String selector) throws Exception {
        final Set<World> worlds = new TreeSet<>(new WorldNameComparator());

        // If there is no selector we want the world the user is currently in. Or all worlds if it isn't a user.
        if (selector == null) {
            if (sender.isPlayer()) {

                final User user = ess.getUser(sender.getPlayer());
                worlds.add(user.getWorld());
            } else {
                worlds.addAll(server.getWorlds());
            }
            return worlds;
        }

        // Try to find the world with name = selector
        final World world = server.getWorld(selector);
        if (world != null) {
            worlds.add(world);
        }
        // If that fails, Is the argument something like "*" or "all"?
        else if (selector.equalsIgnoreCase("*") || selector.equalsIgnoreCase("all")) {
            worlds.addAll(server.getWorlds());
        }
        // We failed to understand the world target...
        else {
            throw new Exception(sender.tl("invalidWorld"));
        }

        return worlds;
    }
    
    private boolean canUpdateAll(User user) {
        return !ess.getSettings().isWorldTimePermissions() // First check if per world permissions are enabled, if not, return true. 
            || user == null || user.isAuthorized("essentials.time.world.all");
    }

    private boolean canUpdateWorld(User user, World world) {
        return canUpdateAll(user) || user.isAuthorized("essentials.time.world." + normalizeWorldName(world));
    }

    private String normalizeWorldName(World world) {
        return world.getName().toLowerCase().replaceAll("\\s+", "_");
    }

    @Override
    protected List<String> getTabCompleteOptions(Server server, CommandSource sender, String commandLabel, String[] args) {
        final User user = ess.getUser(sender.getPlayer());

        if (args.length == 1) {
            if (user == null || user.isAuthorized("essentials.time.set")) {
                return Lists.newArrayList("set", "add");
            } else {
                return Collections.emptyList();
            }
        } else if (args.length == 2 && args[0].equalsIgnoreCase("set")) {
            return Lists.newArrayList("sunrise", "day", "morning", "noon", "afternoon", "sunset", "night", "midnight");
            // TODO: handle tab completion for add
        } else if (args.length == 3 && (args[0].equalsIgnoreCase("set") || args[0].equalsIgnoreCase("add"))) {
            List<String> worlds = Lists.newArrayList();
            for (World world : server.getWorlds()) {
                if (user == null || user.isAuthorized("essentials.time.world." + normalizeWorldName(world))) {
                    worlds.add(world.getName());
                }
            }
            if (user == null || user.isAuthorized("essentials.time.world.all")) {
                worlds.add("*");
            }
            return worlds;
        } else {
            return Collections.emptyList();
        }
    }
}


class WorldNameComparator implements Comparator<World> {
    @Override
    public int compare(final World a, final World b) {
        return a.getName().compareTo(b.getName());
    }
}
