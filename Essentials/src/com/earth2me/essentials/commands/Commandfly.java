package com.earth2me.essentials.commands;

import com.earth2me.essentials.CommandSource;
import com.earth2me.essentials.User;
import org.bukkit.Server;
import net.ess3.api.events.FlyStatusChangeEvent;

public class Commandfly extends EssentialsToggleCommand {
    public Commandfly() {
        super("fly", "essentials.fly.others");
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
            enabled = !user.getBase().getAllowFlight();
        }
        
        final User controller = sender.isPlayer() ? ess.getUser(sender.getPlayer()) : null;
        FlyStatusChangeEvent event = new FlyStatusChangeEvent(user, controller, enabled);
        ess.getServer().getPluginManager().callEvent(event);
        
        if (!event.isCancelled()) {
            user.getBase().setFallDistance(0f);
            user.getBase().setAllowFlight(enabled);

            if (!user.getBase().getAllowFlight()) {
                user.getBase().setFlying(false);
            }

            user.sendTl("flyMode", user.tl(enabled ? "enabled" : "disabled"), user.getDisplayName());
            if (!sender.isPlayer() || !sender.getPlayer().equals(user.getBase())) {
                sender.sendTl("flyMode", sender.tl(enabled ? "enabled" : "disabled"), user.getDisplayName());
            }
        }
    }
}
