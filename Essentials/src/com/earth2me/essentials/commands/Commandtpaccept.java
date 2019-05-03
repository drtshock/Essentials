package com.earth2me.essentials.commands;

import com.earth2me.essentials.Teleport;
import com.earth2me.essentials.Trade;
import com.earth2me.essentials.User;
import org.bukkit.Location;
import org.bukkit.Server;
import org.bukkit.event.player.PlayerTeleportEvent.TeleportCause;


public class Commandtpaccept extends EssentialsCommand {
    public Commandtpaccept() {
        super("tpaccept");
    }

    @Override
    public void run(final Server server, final User user, final String commandLabel, final String[] args) throws Exception {
        final User requester;
        try {
            requester = ess.getUser(user.getTeleportRequest());
        } catch (Exception ex) {
            throw new Exception(user.tl("noPendingRequest"));
        }

        if (!requester.getBase().isOnline()) {
            throw new Exception(user.tl("noPendingRequest"));
        }

        if (user.isTpRequestHere() && ((!requester.isAuthorized("essentials.tpahere") && !requester.isAuthorized("essentials.tpaall")) || (user.getWorld() != requester.getWorld() && ess.getSettings().isWorldTeleportPermissions() && !user.isAuthorized("essentials.worlds." + user.getWorld().getName())))) {
            throw new Exception(user.tl("noPendingRequest"));
        }

        if (!user.isTpRequestHere() && (!requester.isAuthorized("essentials.tpa") || (user.getWorld() != requester.getWorld() && ess.getSettings().isWorldTeleportPermissions() && !user.isAuthorized("essentials.worlds." + requester.getWorld().getName())))) {
            throw new Exception(user.tl("noPendingRequest"));
        }

        if (args.length > 0 && !requester.getName().contains(args[0])) {
            throw new Exception(user.tl("noPendingRequest"));
        }

        if (!user.hasOutstandingTeleportRequest()) {
            user.requestTeleport(null, false);
            throw new Exception(user.tl("requestTimedOut"));
        }

        final Trade charge = new Trade(this.getName(), ess);
        user.sendTl("requestAccepted");
        requester.sendTl("requestAcceptedFrom", user.getDisplayName());

        try {
            if (user.isTpRequestHere()) {
                final Location loc = user.getTpRequestLocation();
                Teleport teleport = requester.getTeleport();
                teleport.setTpType(Teleport.TeleportType.TPA);
                teleport.teleportPlayer(user, user.getTpRequestLocation(), charge, TeleportCause.COMMAND);
                requester.sendTl("teleporting", loc.getWorld().getName(), loc.getBlockX(), loc.getBlockY(), loc.getBlockZ());
            } else {
                Teleport teleport = requester.getTeleport();
                teleport.setTpType(Teleport.TeleportType.TPA);
                teleport.teleport(user.getBase(), charge, TeleportCause.COMMAND);
            }
        } catch (Exception ex) {
            user.sendTl("pendingTeleportCancelled");
            ess.showError(requester.getSource(), ex, commandLabel);
        }
        user.requestTeleport(null, false);
        throw new NoChargeException();
    }

}
