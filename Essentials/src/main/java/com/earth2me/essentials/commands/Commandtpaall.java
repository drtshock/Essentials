package com.earth2me.essentials.commands;

import com.earth2me.essentials.CommandSource;
import com.earth2me.essentials.User;
import net.ess3.api.events.TPARequestEvent;
import org.bukkit.Server;

import java.util.Collections;
import java.util.List;

import static com.earth2me.essentials.I18n.tl;

public class Commandtpaall extends EssentialsCommand {
    public Commandtpaall() {
        super("tpaall");
    }

    @Override
    public void run(final Server server, final CommandSource sender, final String commandLabel, final String[] args) throws Exception {
        if (args.length < 1) {
            if (sender.isPlayer()) {
                teleportAAllPlayers(sender, ess.getUser(sender.getPlayer()));
                return;
            }
            throw new NotEnoughArgumentsException();
        }

        final User target = getPlayer(server, sender, args, 0);
        teleportAAllPlayers(sender, target);
    }

    private void teleportAAllPlayers(final CommandSource sender, final User target) {
        sender.sendMessage(tl("teleportAAll"));
        for (final User player : ess.getOnlineUsers()) {
            if (target == player) {
                continue;
            }
            if (!player.isTeleportEnabled()) {
                continue;
            }
            if (sender.getSender().equals(target.getBase()) && target.getWorld() != player.getWorld() && ess.getSettings().isWorldTeleportPermissions() && !target.isAuthorized("essentials.worlds." + target.getWorld().getName())) {
                continue;
            }

            try {
                final TPARequestEvent tpaEvent = new TPARequestEvent(sender, player, true);
                ess.getServer().getPluginManager().callEvent(tpaEvent);
                if (tpaEvent.isCancelled()) {
                    sender.sendMessage(tl("teleportRequestCancelled", player.getDisplayName()));
                    continue;
                }
                player.requestTeleport(target, true);
                player.sendMessage(tl("teleportHereRequest", target.getDisplayName()));
                player.sendMessage(tl("typeTpaccept"));
                if (ess.getSettings().getTpaAcceptCancellation() != 0) {
                    player.sendMessage(tl("teleportRequestTimeoutInfo", ess.getSettings().getTpaAcceptCancellation()));
                }
            } catch (final Exception ex) {
                ess.showError(sender, ex, getName());
            }
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
