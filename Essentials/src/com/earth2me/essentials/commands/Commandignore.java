package com.earth2me.essentials.commands;

import com.earth2me.essentials.User;
import org.bukkit.Server;

import java.util.Collections;
import java.util.List;


public class Commandignore extends EssentialsCommand {
    public Commandignore() {
        super("ignore");
    }

    @Override
    protected void run(final Server server, final User user, final String commandLabel, final String[] args) throws Exception {
        if (args.length < 1) {
            StringBuilder sb = new StringBuilder();
            for (String s : user._getIgnoredPlayers()) {
                sb.append(s).append(" ");
            }
            String ignoredList = sb.toString().trim();
            user.sendMessage(ignoredList.length() > 0 ? user.tl("ignoredList", ignoredList) : user.tl("noIgnored"));
        } else {
            User player;
            try {
                player = getPlayer(server, args, 0, true, true);
            } catch (PlayerNotFoundException ex) {
                player = ess.getOfflineUser(args[0]);
            }
            if (player == null) {
                throw new PlayerNotFoundException();
            }
            if (player.isIgnoreExempt()) {
                user.sendTl("ignoreExempt");
            } else if (user.isIgnoredPlayer(player)) {
                user.setIgnoredPlayer(player, false);
                user.sendTl("unignorePlayer", player.getName());
            } else {
                user.setIgnoredPlayer(player, true);
                user.sendTl("ignorePlayer", player.getName());
            }
        }
    }

    @Override
    protected List<String> getTabCompleteOptions(Server server, User user, String commandLabel, String[] args) {
        if (args.length == 1) {
            return getPlayers(server, user);
        } else {
            return Collections.emptyList();
        }
    }
}
