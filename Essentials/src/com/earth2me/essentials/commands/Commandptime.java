package com.earth2me.essentials.commands;

import com.google.common.collect.Lists;

import com.earth2me.essentials.CommandSource;
import com.earth2me.essentials.User;
import com.earth2me.essentials.utils.DescParseTickFormat;
import org.bukkit.Server;
import org.bukkit.World;
import org.bukkit.entity.Player;

import java.util.*;


public class Commandptime extends EssentialsCommand {
    private static final Set<String> getAliases = new HashSet<>();

    static {
        getAliases.add("get");
        getAliases.add("list");
        getAliases.add("show");
        getAliases.add("display");
    }

    public Commandptime() {
        super("ptime");
    }

    @Override
    public void run(final Server server, final CommandSource sender, final String commandLabel, final String[] args) throws Exception {
        // Which Players(s) / Users(s) are we interested in?
        String userSelector = null;
        if (args.length == 2) {
            userSelector = args[1];
        }
        Set<User> users = getUsers(server, sender, userSelector);

        // If no arguments we are reading the time
        if (args.length == 0) {
            getUsersTime(sender, users);
            return;
        }

        if (sender.isPlayer()) {
            User user = ess.getUser(sender.getPlayer());
            if (user != null && (!users.contains(user) || users.size() > 1) && !user.isAuthorized("essentials.ptime.others")) {
                user.sendTl("pTimeOthersPermission");
                return;
            }
        }

        Long ticks;
        // Parse the target time int ticks from args[0]
        String timeParam = args[0];
        boolean relative = true;
        if (timeParam.startsWith("@")) {
            relative = false;
            timeParam = timeParam.substring(1);
        }

        if (getAliases.contains(timeParam)) {
            getUsersTime(sender, users);
            return;
        } else if (DescParseTickFormat.meansReset(timeParam)) {
            ticks = null;
        } else {
            try {
                ticks = DescParseTickFormat.parse(timeParam);
            } catch (NumberFormatException e) {
                throw new NotEnoughArgumentsException(e);
            }
        }

        setUsersTime(sender, users, ticks, relative);
    }

    /**
     * Used to get the time and inform
     */
    private void getUsersTime(final CommandSource sender, final Collection<User> users) {
        if (users.size() > 1) {
            sender.sendTl("pTimePlayers");
        }

        for (User user : users) {
            if (user.getBase().getPlayerTimeOffset() == 0) {
                sender.sendTl("pTimeNormal", user.getName());
            } else {
                String time = DescParseTickFormat.format(user.getBase().getPlayerTime());
                if (!user.getBase().isPlayerTimeRelative()) {
                    sender.sendTl("pTimeCurrentFixed", user.getName(), time);
                } else {
                    sender.sendTl("pTimeCurrent", user.getName(), time);
                }
            }
        }
    }

    /**
     * Used to set the time and inform of the change
     */
    private void setUsersTime(final CommandSource sender, final Collection<User> users, final Long ticks, Boolean relative) {
        // Update the time
        if (ticks == null) {
            // Reset
            for (User user : users) {
                user.getBase().resetPlayerTime();
            }
        } else {
            // Set
            for (User user : users) {
                final World world = user.getWorld();
                long time = user.getBase().getPlayerTime();
                time -= time % 24000;
                time += 24000 + ticks;
                if (relative) {
                    time -= world.getTime();
                }
                user.getBase().setPlayerTime(time, relative);
            }
        }

        final StringBuilder msg = new StringBuilder();
        for (User user : users) {
            if (msg.length() > 0) {
                msg.append(", ");
            }

            msg.append(user.getName());
        }

        // Inform the sender of the change
        if (ticks == null) {
            sender.sendTl("pTimeReset", msg.toString());
        } else {
            String time = DescParseTickFormat.format(ticks);
            if (!relative) {
                sender.sendTl("pTimeSetFixed", time, msg.toString());
            } else {
                sender.sendTl("pTimeSet", time, msg.toString());
            }
        }
    }

    /**
     * Used to parse an argument of the type "users(s) selector"
     */
    private Set<User> getUsers(final Server server, final CommandSource sender, final String selector) throws Exception {
        final Set<User> users = new TreeSet<>(new UserNameComparator());
        // If there is no selector we want the sender itself. Or all users if sender isn't a user.
        if (selector == null) {
            if (sender.isPlayer()) {
                final User user = ess.getUser(sender.getPlayer());
                users.add(user);
            } else {
                for (User user : ess.getOnlineUsers()) {
                    users.add(user);
                }
            }
            return users;
        }

        // Try to find the user with name = selector
        User user = null;
        final List<Player> matchedPlayers = server.matchPlayer(selector);
        if (!matchedPlayers.isEmpty()) {
            user = ess.getUser(matchedPlayers.get(0));
        }

        if (user != null) {
            users.add(user);
        }
        // If that fails, Is the argument something like "*" or "all"?
        else if (selector.equalsIgnoreCase("*") || selector.equalsIgnoreCase("all")) {
            for (User u : ess.getOnlineUsers()) {
                users.add(u);
            }
        }
        // We failed to understand the world target...
        else {
            throw new PlayerNotFoundException();
        }

        return users;
    }

    @Override
    protected List<String> getTabCompleteOptions(Server server, CommandSource sender, String commandLabel, String[] args) {
        final User user = ess.getUser(sender.getPlayer());

        if (args.length == 1) {
            return Lists.newArrayList("get", "reset", "sunrise", "day", "morning", "noon", "afternoon", "sunset", "night", "midnight");
        } else if (args.length == 2 && (getAliases.contains(args[0]) || user == null || user.isAuthorized("essentials.ptime.others"))) {
            return getPlayers(server, sender);
        } else {
            return Collections.emptyList();
        }
    }
}


class UserNameComparator implements Comparator<User> {
    @Override
    public int compare(User a, User b) {
        return a.getName().compareTo(b.getName());
    }
}
