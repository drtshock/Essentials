package com.earth2me.essentials;

import com.earth2me.essentials.utils.FormatUtil;
import org.bukkit.Bukkit;
import org.bukkit.Server;

import java.util.*;

import static com.earth2me.essentials.I18n.tl;


public class PlayerList {
    // Cosmetic list formatting
    @Deprecated
    public static String listUsers(final IEssentials ess, final List<User> users, final String seperator) {
        return listUsers(ess, new CommandSource(Bukkit.getConsoleSender()), users, seperator);
    }

    public static String listUsers(final IEssentials ess, final CommandSource sender, final List<User> users, final String seperator) {
        final StringBuilder groupString = new StringBuilder();
        Collections.sort(users);
        boolean needComma = false;
        for (User user : users) {
            if (needComma) {
                groupString.append(seperator);
            }
            needComma = true;
            if (user.isAfk()) {
                groupString.append(sender.tl("listAfkTag"));
            }
            if (user.isHidden()) {
                groupString.append(sender.tl("listHiddenTag"));
            }
            user.setDisplayNick();
            groupString.append(user.getDisplayName());
            groupString.append("\u00a7f");
        }
        return groupString.toString();
    }

    @Deprecated
    public static String listSummary(final IEssentials ess, final User user, final boolean showHidden) {
        return listSummary(ess, new CommandSource(user), showHidden);
    }

    public static String listSummary(final IEssentials ess, final CommandSource sender, final boolean showHidden) {
        Server server = ess.getServer();
        int playerHidden = 0;
        int hiddenCount = 0;
        for (User onlinePlayer : ess.getOnlineUsers()) {
            if (onlinePlayer.isHidden() || (sender.getUser() != null && !sender.getUser().getBase().canSee(onlinePlayer.getBase()))) {
                playerHidden++;
                if (showHidden || sender.getUser().getBase().canSee(onlinePlayer.getBase())) {
                    hiddenCount++;
                }
            }
        }
        String online;
        if (hiddenCount > 0) {
            online = sender.tl("listAmountHidden", ess.getOnlinePlayers().size() - playerHidden, hiddenCount, server.getMaxPlayers());
        } else {
            online = sender.tl("listAmount", ess.getOnlinePlayers().size() - playerHidden, server.getMaxPlayers());
        }
        return online;
    }

    // Build the basic player list, divided by groups.
    public static Map<String, List<User>> getPlayerLists(final IEssentials ess, final User sender, final boolean showHidden) {
        Server server = ess.getServer();
        final Map<String, List<User>> playerList = new HashMap<String, List<User>>();
        for (User onlineUser : ess.getOnlineUsers()) {
            if ((sender == null && !showHidden && onlineUser.isHidden()) || (sender != null && !showHidden && !sender.getBase().canSee(onlineUser.getBase()))) {
                continue;
            }
            final String group = FormatUtil.stripFormat(FormatUtil.stripEssentialsFormat(onlineUser.getGroup().toLowerCase()));
            List<User> list = playerList.get(group);
            if (list == null) {
                list = new ArrayList<User>();
                playerList.put(group, list);
            }
            list.add(onlineUser);
        }
        return playerList;
    }

    // Handle the merging of groups
    public static List<User> getMergedList(final IEssentials ess, final Map<String, List<User>> playerList, final String groupName) {
        final Set<String> configGroups = ess.getSettings().getListGroupConfig().keySet();
        final List<User> users = new ArrayList<User>();
        for (String configGroup : configGroups) {
            if (configGroup.equalsIgnoreCase(groupName)) {
                String[] groupValues = ess.getSettings().getListGroupConfig().get(configGroup).toString().trim().split(" ");
                for (String groupValue : groupValues) {
                    groupValue = groupValue.toLowerCase(Locale.ENGLISH);
                    if (groupValue == null || groupValue.isEmpty()) {
                        continue;
                    }
                    List<User> u = playerList.get(groupValue.trim());
                    if (u == null || u.isEmpty()) {
                        continue;
                    }
                    playerList.remove(groupValue);
                    users.addAll(u);
                }
            }
        }
        return users;
    }

    // Output a playerlist of just a single group, /list <groupname>
    @Deprecated
    public static String listGroupUsers(final IEssentials ess, final Map<String, List<User>> playerList, final String groupName) throws Exception {
        return listGroupUsers(ess, new CommandSource(Bukkit.getConsoleSender()), playerList, groupName);
    }

    public static String listGroupUsers(final IEssentials ess, final CommandSource sender, final Map<String, List<User>> playerList, final String groupName) throws Exception {
        final List<User> users = getMergedList(ess, playerList, groupName);
        final List<User> groupUsers = playerList.get(groupName);
        if (groupUsers != null && !groupUsers.isEmpty()) {
            users.addAll(groupUsers);
        }
        if (users == null || users.isEmpty()) {
            throw new Exception(sender.tl("groupDoesNotExist"));
        }
        final StringBuilder displayGroupName = new StringBuilder();
        displayGroupName.append(Character.toTitleCase(groupName.charAt(0)));
        displayGroupName.append(groupName.substring(1));
        return outputFormat(sender, displayGroupName.toString(), listUsers(ess, users, ", "));
    }

    // Build the output string
    @Deprecated
    public static String outputFormat(final String group, final String message) {
        return outputFormat(new CommandSource(Bukkit.getConsoleSender()), group, message);
    }

    public static String outputFormat(final CommandSource sender, final String group, final String message) {
        final StringBuilder outputString = new StringBuilder();
        outputString.append(sender.tl("listGroupTag", FormatUtil.replaceFormat(group)));
        outputString.append(message);
        return outputString.toString();
    }
}
