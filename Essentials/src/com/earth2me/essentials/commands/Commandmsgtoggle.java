package com.earth2me.essentials.commands;

import com.earth2me.essentials.CommandSource;
import com.earth2me.essentials.User;
import org.bukkit.Server;


public class Commandmsgtoggle extends EssentialsToggleCommand {
    public Commandmsgtoggle() {
        super("msgtoggle", "essentials.msgtoggle.others");
    }

    @Override
    protected void run(final Server server, final CommandSource sender, final String commandLabel, final String[] args) throws Exception {
        toggleOtherPlayers(server, sender, args);
    }

    @Override
    protected void run(final Server server, final User user, final String commandLabel, final String[] args) throws Exception {
        handleToggleWithArgs(server, user, args);
    }

    @Override
    void togglePlayer(CommandSource sender, User user, Boolean enabled) {
        if (enabled == null) {
            enabled = !user.isIgnoreMsg();
        }

        user.setIgnoreMsg(enabled);

        user.sendMessage(!enabled ? user.tl("msgEnabled") : user.tl("msgDisabled"));
        if (!sender.isPlayer() || !user.getBase().equals(sender.getPlayer())) {
            sender.sendMessage(!enabled ? user.tl("msgEnabledFor", user.getDisplayName()) : user.tl("msgDisabledFor", user.getDisplayName()));
        }
    }
}
