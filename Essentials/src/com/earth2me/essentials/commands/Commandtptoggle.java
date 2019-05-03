package com.earth2me.essentials.commands;

import com.earth2me.essentials.CommandSource;
import com.earth2me.essentials.User;
import org.bukkit.Server;


public class Commandtptoggle extends EssentialsToggleCommand {
    public Commandtptoggle() {
        super("tptoggle", "essentials.tptoggle.others");
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
            enabled = !user.isTeleportEnabled();
        }

        user.setTeleportEnabled(enabled);

        user.sendMessage(enabled ? user.tl("teleportationEnabled") : user.tl("teleportationDisabled"));
        if (!sender.isPlayer() || !user.getBase().equals(sender.getPlayer())) {
            sender.sendMessage(enabled ? user.tl("teleportationEnabledFor", user.getDisplayName()) : user.tl("teleportationDisabledFor", user.getDisplayName()));
        }
    }
}
