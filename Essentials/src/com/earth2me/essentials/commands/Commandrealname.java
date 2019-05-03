package com.earth2me.essentials.commands;

import com.earth2me.essentials.CommandSource;
import com.earth2me.essentials.User;
import com.earth2me.essentials.utils.FormatUtil;
import org.bukkit.Server;

import java.util.Locale;


public class Commandrealname extends EssentialsCommand {
    public Commandrealname() {
        super("realname");
    }

    @Override
    protected void run(final Server server, final CommandSource sender, final String commandLabel, final String[] args) throws Exception {
        if (args.length < 1) {
            throw new NotEnoughArgumentsException();
        }
        final String whois = args[0].toLowerCase(Locale.ENGLISH);
        boolean skipHidden = sender.isPlayer() && !ess.getUser(sender.getPlayer()).canInteractVanished();
        boolean foundUser = false;
        for (User u : ess.getOnlineUsers()) {
            if (skipHidden && u.isHidden(sender.getPlayer()) && !sender.getPlayer().canSee(u.getBase())) {
                continue;
            }
            u.setDisplayNick();
            final String displayName = FormatUtil.stripFormat(u.getDisplayName()).toLowerCase(Locale.ENGLISH);
            if (displayName.contains(whois)) {
                foundUser = true;
                sender.sendTl("realName", u.getDisplayName(), u.getName());
            }
        }
        if (!foundUser) {
            throw new PlayerNotFoundException();
        }
    }
}
