package com.earth2me.essentials.commands;

import com.earth2me.essentials.CommandSource;
import com.earth2me.essentials.I18n;
import com.earth2me.essentials.User;
import org.bukkit.Server;

import java.util.Collections;
import java.util.List;
import java.util.Locale;


public class Commandlocale extends EssentialsLoopCommand {
    public Commandlocale() {
        super("locale");
    }

    @Override
    public void run(final Server server, final User user, final String commandLabel, final String[] args) throws Exception {
        if (!ess.getSettings().changeLocale()) {
            throw new Exception(user.tl("localeChange"));
        }
        if (args.length < 1) {
            throw new NotEnoughArgumentsException();
        }

        if (args.length > 1 && user.isAuthorized("essentials.locale.others")) {
            loopOfflinePlayers(server, user.getSource(), false, true, args[0], new String[]{args[1]});
            user.sendTl("localeChanged");
        } else {
            updatePlayer(server, user.getSource(), user, args);
        }
    }

    @Override
    public void run(final Server server, final CommandSource sender, final String commandLabel, final String[] args) throws Exception {
        if (!ess.getSettings().changeLocale()) {
            throw new Exception(sender.tl("localeChange"));
        }
        if (args.length < 1) {
            throw new NotEnoughArgumentsException();
        }
        loopOfflinePlayers(server, sender, false, true, args[0], new String[]{args[1]});
        sender.sendTl("localeChanged");
    }

    @Override
    protected void updatePlayer(final Server server, final CommandSource sender, final User target, final String[] args) throws NotEnoughArgumentsException {
        if ("none".equalsIgnoreCase(args[0]) || "default".equalsIgnoreCase(args[0])) {
            target.setLocale(null);
            target.sendTl("localeNoMore");
            return;
        }

        final Locale locale = I18n.getLocale(args[0]);
        if (locale == null) {
            throw new NotEnoughArgumentsException(sender.tl("localeUnknown", args[0]));
        } else if (locale.equals(target.getLocale())) {
            sender.sendTl("localeInUse", target.getLocale().getDisplayName());
        } else {
            target.setLocale(locale);
            target.sendTl("localeSet", locale.getDisplayName());
        }
    }

    @Override
    protected List<String> getTabCompleteOptions(final Server server, final User user, final String commandLabel, final String[] args) {
        if (args.length == 1 && user.isAuthorized("essentials.locale.others")) {
            return getPlayers(server, user);
        } else {
            return Collections.emptyList();
        }
    }

    @Override
    protected List<String> getTabCompleteOptions(final Server server, final CommandSource sender, final String commandLabel, final String[] args) {
        if (args.length == 1) {
            return getPlayers(server, sender);
        } else {
            return Collections.emptyList();
        }
    }
}
