package com.earth2me.essentials.commands;

import com.google.common.collect.Lists;

import com.earth2me.essentials.CommandSource;
import com.earth2me.essentials.User;
import org.bukkit.Server;
import org.bukkit.WeatherType;
import org.bukkit.entity.Player;

import java.util.*;


public class Commandpweather extends EssentialsCommand {
    public static final Set<String> getAliases = new HashSet<>();
    public static final Map<String, WeatherType> weatherAliases = new HashMap<>();

    static {
        getAliases.add("get");
        getAliases.add("list");
        getAliases.add("show");
        getAliases.add("display");
        weatherAliases.put("sun", WeatherType.CLEAR);
        weatherAliases.put("clear", WeatherType.CLEAR);
        weatherAliases.put("storm", WeatherType.DOWNFALL);
        weatherAliases.put("thunder", WeatherType.DOWNFALL);
    }

    public Commandpweather() {
        super("pweather");
    }

    @Override
    public void run(final Server server, final CommandSource sender, final String commandLabel, final String[] args) throws Exception {
        // Which Players(s) / Users(s) are we interested in?
        String userSelector = null;
        if (args.length == 2) {
            userSelector = args[1];
        }
        Set<User> users = getUsers(server, sender, userSelector);

        if (args.length == 0) {
            getUsersWeather(sender, users);
            return;
        }

        if (getAliases.contains(args[0])) {
            getUsersWeather(sender, users);
            return;
        }

        if (sender.isPlayer()) {
            User user = ess.getUser(sender.getPlayer());
            if (user != null && (!users.contains(user) || users.size() > 1) && !user.isAuthorized("essentials.pweather.others")) {
                user.sendTl("pWeatherOthersPermission");
                return;
            }
        }

        setUsersWeather(sender, users, args[0].toLowerCase());
    }

    /**
     * Used to get the time and inform
     */
    private void getUsersWeather(final CommandSource sender, final Collection<User> users) {
        if (users.size() > 1) {
            sender.sendTl("pWeatherPlayers");
        }

        for (User user : users) {
            if (user.getBase().getPlayerWeather() == null) {
                sender.sendTl("pWeatherNormal", user.getName());
            } else {
                sender.sendTl("pWeatherCurrent", user.getName(), user.getBase().getPlayerWeather().toString().toLowerCase(Locale.ENGLISH));
            }
        }
    }

    /**
     * Used to set the time and inform of the change
     */
    private void setUsersWeather(final CommandSource sender, final Collection<User> users, final String weatherType) throws Exception {

        final StringBuilder msg = new StringBuilder();
        for (User user : users) {
            if (msg.length() > 0) {
                msg.append(", ");
            }

            msg.append(user.getName());
        }

        if (weatherType.equalsIgnoreCase("reset")) {
            for (User user : users) {
                user.getBase().resetPlayerWeather();
            }

            sender.sendTl("pWeatherReset", msg);
        } else {
            if (!weatherAliases.containsKey(weatherType)) {
                throw new NotEnoughArgumentsException(sender.tl("pWeatherInvalidAlias"));
            }

            for (User user : users) {
                user.getBase().setPlayerWeather(weatherAliases.get(weatherType));
            }
            sender.sendTl("pWeatherSet", weatherType, msg.toString());
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
    protected List<String> getTabCompleteOptions(Server server, User user, String commandLabel, String[] args) {
        if (args.length == 1) {
            return Lists.newArrayList("get", "reset", "storm", "sun");
        } else if (args.length == 2 && (getAliases.contains(args[0]) || user == null || user.isAuthorized("essentials.pweather.others"))) {
            return getPlayers(server, user);
        } else {
            return Collections.emptyList();
        }
    }
}
