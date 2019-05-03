package com.earth2me.essentials.commands;

import com.earth2me.essentials.CommandSource;
import com.earth2me.essentials.User;
import org.bukkit.Server;

import net.ess3.api.events.VanishStatusChangeEvent;


public class Commandvanish extends EssentialsToggleCommand {
    public Commandvanish() {
        super("vanish", "essentials.vanish.others");
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
            enabled = !user.isVanished();
        }

        final User controller = sender.isPlayer() ? ess.getUser(sender.getPlayer()) : null;
        VanishStatusChangeEvent vanishEvent = new VanishStatusChangeEvent(controller, user, enabled);
        ess.getServer().getPluginManager().callEvent(vanishEvent);
        if (vanishEvent.isCancelled()) {
            return;
        }

        user.setVanished(enabled);
        user.sendTl("vanish", user.getDisplayName(), enabled ? user.tl("enabled") : user.tl("disabled"));

        if (enabled) {
            user.sendTl("vanished");
        }
        if (!sender.isPlayer() || !sender.getPlayer().equals(user.getBase())) {
            sender.sendTl("vanish", user.getDisplayName(), enabled ? user.tl("enabled") : user.tl("disabled"));
        }
    }
}
