package com.earth2me.essentials.commands;

import com.earth2me.essentials.User;
import org.bukkit.Server;

import java.util.Collections;
import java.util.List;


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
        if (!player.isTeleportEnabled()) {
            throw new Exception(user.tl("teleportDisabled", player.getDisplayName()));
        }
        if (user.getWorld() != player.getWorld() && ess.getSettings().isWorldTeleportPermissions() && !user.isAuthorized("essentials.worlds." + user.getWorld().getName())) {
            throw new Exception(user.tl("noPerm", "essentials.worlds." + user.getWorld().getName()));
        }
        // Don't let sender request teleport twice to the same player.
        if (user.getConfigUUID().equals(player.getTeleportRequest()) && player.hasOutstandingTeleportRequest() // Check timeout
            && player.isTpRequestHere() == true) { // Make sure the last teleport request was actually tpahere and not tpa
            throw new Exception(user.tl("requestSentAlready", player.getDisplayName()));
        }
        if (!player.isIgnoredPlayer(user)) {
            player.requestTeleport(user, true);
            player.sendTl("teleportHereRequest", user.getDisplayName());
            player.sendTl("typeTpaccept");
            player.sendTl("typeTpdeny");
            if (ess.getSettings().getTpaAcceptCancellation() != 0) {
                player.sendTl("teleportRequestTimeoutInfo", ess.getSettings().getTpaAcceptCancellation());
            }
        }
        user.sendTl("requestSent", player.getDisplayName());
        user.sendTl("typeTpacancel");
    }

    @Override
    protected List<String> getTabCompleteOptions(Server server, User user, String commandLabel, String[] args) {
        if (args.length == 1) {
            return getPlayers(server, user);
        } else {
            return Collections.emptyList();
        }
    }
}
