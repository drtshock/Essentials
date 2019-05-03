package com.earth2me.essentials.commands;

import com.earth2me.essentials.Teleport;
import com.earth2me.essentials.Trade;
import com.earth2me.essentials.User;
import org.bukkit.Server;
import org.bukkit.event.player.PlayerTeleportEvent;

import java.util.Collections;
import java.util.List;


public class Commandtpa extends EssentialsCommand {
    public Commandtpa() {
        super("tpa");
    }

    @Override
    public void run(Server server, User user, String commandLabel, String[] args) throws Exception {
        if (args.length < 1) {
            throw new NotEnoughArgumentsException();
        }

        User player = getPlayer(server, user, args, 0);
        if (user.getName().equalsIgnoreCase(player.getName())) {
            throw new NotEnoughArgumentsException();
        }
        if (!player.isTeleportEnabled()) {
            throw new Exception(user.tl("teleportDisabled", player.getDisplayName()));
        }
        if (user.getWorld() != player.getWorld() && ess.getSettings().isWorldTeleportPermissions() && !user.isAuthorized("essentials.worlds." + player.getWorld().getName())) {
            throw new Exception(user.tl("noPerm", "essentials.worlds." + player.getWorld().getName()));
        }
        // Don't let sender request teleport twice to the same player.
        if (user.getConfigUUID().equals(player.getTeleportRequest()) && player.hasOutstandingTeleportRequest() // Check timeout
            && player.isTpRequestHere() == false) { // Make sure the last teleport request was actually tpa and not tpahere
            throw new Exception(user.tl("requestSentAlready", player.getDisplayName()));
        }
        if (player.isAutoTeleportEnabled() && !player.isIgnoredPlayer(user)) {
            final Trade charge = new Trade(this.getName(), ess);
            Teleport teleport = user.getTeleport();
            teleport.setTpType(Teleport.TeleportType.TPA);
            teleport.teleport(player.getBase(), charge, PlayerTeleportEvent.TeleportCause.COMMAND);
            player.sendTl("requestAcceptedAuto", user.getDisplayName());
            user.sendTl("requestAcceptedFromAuto", player.getDisplayName());
            return;
        }

        if (!player.isIgnoredPlayer(user)) {
            player.requestTeleport(user, false);
            player.sendTl("teleportRequest", user.getDisplayName());
            player.sendTl("typeTpaccept");
            player.sendTl("typeTpdeny");
            if (ess.getSettings().getTpaAcceptCancellation() != 0) {
                player.sendTl("teleportRequestTimeoutInfo", ess.getSettings().getTpaAcceptCancellation());
            }
        }
        user.sendTl("requestSent", player.getDisplayName());
        if (user.isAuthorized("essentials.tpacancel")) {
            user.sendTl("typeTpacancel");
        }
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
