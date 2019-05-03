package com.earth2me.essentials.commands;

import com.earth2me.essentials.User;
import org.bukkit.Server;


public class Commandtpdeny extends EssentialsCommand {
    public Commandtpdeny() {
        super("tpdeny");
    }

    @Override
    public void run(final Server server, final User user, final String commandLabel, final String[] args) throws Exception {
        if (user.getTeleportRequest() == null) {
            throw new Exception(user.tl("noPendingRequest"));
        }
        final User player = ess.getUser(user.getTeleportRequest());
        if (player == null) {
            throw new Exception(user.tl("noPendingRequest"));
        }

        user.sendTl("requestDenied");
        player.sendTl("requestDeniedFrom", user.getDisplayName());
        user.requestTeleport(null, false);
    }
}
