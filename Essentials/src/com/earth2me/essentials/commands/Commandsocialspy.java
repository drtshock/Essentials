package com.earth2me.essentials.commands;

import com.earth2me.essentials.CommandSource;
import com.earth2me.essentials.User;
import org.bukkit.Server;


public class Commandsocialspy extends EssentialsToggleCommand {
    public Commandsocialspy() {
        super("socialspy", "essentials.socialspy.others");
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
    void togglePlayer(CommandSource sender, User user, Boolean enabled) throws NotEnoughArgumentsException {
        if (enabled == null) {
            enabled = !user.isSocialSpyEnabled();
        }

        user.setSocialSpyEnabled(enabled);


        user.sendTl("socialSpy", user.getDisplayName(), enabled ? user.tl("enabled") : user.tl("disabled"));
        if (!sender.isPlayer() || !sender.getPlayer().equals(user.getBase())) {
            sender.sendTl("socialSpy", user.getDisplayName(), enabled ? user.tl("enabled") : user.tl("disabled"));
        }
    }
}
