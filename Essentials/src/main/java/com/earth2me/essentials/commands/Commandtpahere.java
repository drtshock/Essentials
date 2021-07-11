package com.earth2me.essentials.commands;

import com.earth2me.essentials.User;
import net.ess3.api.events.TPARequestEvent;
import org.bukkit.Server;

import java.util.Collections;
import java.util.List;

import static com.earth2me.essentials.I18n.tl;

public class Commandtpahere extends EssentialsCommand {
    public Commandtpahere() {
        super("tpahere");
    }

    @Override
    public void run(final Server server, final User user, final String commandLabel, final String[] args) throws Exception {
        if (args.length < 1) {
            throw new NotEnoughArgumentsException();
        }

        final User player = getPlayer(server, user, args, 0);
        if (user.getName().equalsIgnoreCase(player.getName())) {
            throw new NotEnoughArgumentsException();
        }
        if (!player.isAuthorized("essentials.tpaccept")) {
            throw new Exception(tl("teleportNoAcceptPermission", player.getDisplayName()));
        }
        if (!player.isTeleportEnabled()) {
            throw new Exception(tl("teleportDisabled", player.getDisplayName()));
        }
        if (user.getWorld() != player.getWorld() && ess.getSettings().isWorldTeleportPermissions() && !user.isAuthorized("essentials.worlds." + user.getWorld().getName())) {
            throw new Exception(tl("noPerm", "essentials.worlds." + user.getWorld().getName()));
        }

        // Don't let sender request teleport twice to the same player.
        if (player.hasOutstandingTpaRequest(user.getName(), true)) {
            throw new Exception(tl("requestSentAlready", player.getDisplayName()));
        }

        if (!player.isIgnoredPlayer(user)) {
            final TPARequestEvent tpaEvent = new TPARequestEvent(user.getSource(), player, true);
            ess.getServer().getPluginManager().callEvent(tpaEvent);
            if (tpaEvent.isCancelled()) {
                throw new Exception(tl("teleportRequestCancelled", player.getDisplayName()));
            }
            player.requestTeleport(user, true);
            player.sendMessage(tl("teleportHereRequest", user.getDisplayName()));
            player.sendMessage(tl("typeTpaccept"));
            player.sendMessage(tl("typeTpdeny"));
            if (ess.getSettings().getTpaAcceptCancellation() != 0) {
                player.sendMessage(tl("teleportRequestTimeoutInfo", ess.getSettings().getTpaAcceptCancellation()));
            }
        }
        user.sendMessage(tl("requestSent", player.getDisplayName()));
        user.sendMessage(tl("typeTpacancel"));
    }

    @Override
    protected List<String> getTabCompleteOptions(final Server server, final User user, final String commandLabel, final String[] args) {
        if (args.length == 1) {
            return getPlayers(server, user);
        } else {
            return Collections.emptyList();
        }
    }
}
