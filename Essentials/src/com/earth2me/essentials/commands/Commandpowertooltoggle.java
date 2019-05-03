package com.earth2me.essentials.commands;

import com.earth2me.essentials.User;
import org.bukkit.Server;


public class Commandpowertooltoggle extends EssentialsCommand {
    public Commandpowertooltoggle() {
        super("powertooltoggle");
    }

    @Override
    protected void run(final Server server, final User user, final String commandLabel, final String[] args) throws Exception {
        if (!user.hasPowerTools()) {
            user.sendTl("noPowerTools");
            return;
        }
        user.sendMessage(user.togglePowerToolsEnabled() ? user.tl("powerToolsEnabled") : user.tl("powerToolsDisabled"));
    }
}
