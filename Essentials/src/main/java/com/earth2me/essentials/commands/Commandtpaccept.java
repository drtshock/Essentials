package com.earth2me.essentials.commands;

import com.earth2me.essentials.AsyncTeleport;
import com.earth2me.essentials.IUser;
import com.earth2me.essentials.Trade;
import com.earth2me.essentials.User;
import net.essentialsx.api.v2.events.TeleportRequestResponseEvent;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Server;
import org.bukkit.event.player.PlayerTeleportEvent.TeleportCause;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import static com.earth2me.essentials.I18n.tl;

public class Commandtpaccept extends EssentialsCommand {
    public Commandtpaccept() {
        super("tpaccept");
    }

    @Override
    public void run(final Server server, final User user, final String commandLabel, final String[] args) throws Exception {
        final boolean excludeHere;
        if (args.length > 0) {
            excludeHere = args[0].equals("*") || args[0].equalsIgnoreCase("all");
        } else {
            excludeHere = false;
        }

        if (!user.hasPendingTpaRequests(true, excludeHere)) {
            throw new Exception(tl("noPendingRequest"));
        }

        if (args.length > 0) {
            if (excludeHere) {
                IUser.TpaRequest request;
                int count = 0;
                while ((request = user.getNextTpaRequest(true, true, true)) != null) {
                    try {
                        handleTeleport(user, request, commandLabel);
                        count++;
                    } catch (Exception e) {
                        ess.showError(user.getSource(), e, commandLabel);
                    } finally {
                        user.removeTpaRequest(request.getName());
                    }
                }
                user.sendMessage(tl("requestAcceptedAll", count));
                throw new NoChargeException();
            }
            user.sendMessage(tl("requestAccepted"));
            handleTeleport(user, user.getOutstandingTpaRequest(getPlayer(server, user, args, 0).getName(), true), commandLabel);
        } else {
            user.sendMessage(tl("requestAccepted"));
            handleTeleport(user, user.getNextTpaRequest(true, false, false), commandLabel);
        }
        throw new NoChargeException();
    }

    @Override
    protected List<String> getTabCompleteOptions(Server server, User user, String commandLabel, String[] args) {
        if (args.length == 1) {
            final List<String> options = new ArrayList<>(user.getPendingTpaKeys());
            options.add("*");
            return options;
        } else {
            return Collections.emptyList();
        }
    }

    private void handleTeleport(final User user, final IUser.TpaRequest request, String commandLabel) throws Exception {
        if (request == null) {
            throw new Exception(tl("noPendingRequest"));
        }
        final User requester = ess.getUser(request.getRequesterUuid());

        if (!requester.getBase().isOnline()) {
            user.removeTpaRequest(request.getName());
            throw new Exception(tl("noPendingRequest"));
        }

        if (request.isHere() && ((!requester.isAuthorized("essentials.tpahere") && !requester.isAuthorized("essentials.tpaall")) || (user.getWorld() != requester.getWorld() && ess.getSettings().isWorldTeleportPermissions() && !user.isAuthorized("essentials.worlds." + user.getWorld().getName())))) {
            throw new Exception(tl("noPendingRequest"));
        }

        if (!request.isHere() && (!requester.isAuthorized("essentials.tpa") || (user.getWorld() != requester.getWorld() && ess.getSettings().isWorldTeleportPermissions() && !user.isAuthorized("essentials.worlds." + requester.getWorld().getName())))) {
            throw new Exception(tl("noPendingRequest"));
        }

        final TeleportRequestResponseEvent event = new TeleportRequestResponseEvent(user, requester, request, true);
        Bukkit.getPluginManager().callEvent(event);
        if (event.isCancelled()) {
            if (ess.getSettings().isDebug()) {
                logger.info("TPA accept cancelled by API for " + user.getName() + " (requested by " + requester.getName() + ")");
            }
            return;
        }

        final Trade charge = new Trade(this.getName(), ess);
        requester.sendMessage(tl("requestAcceptedFrom", user.getDisplayName()));

        final CompletableFuture<Boolean> future = getNewExceptionFuture(requester.getSource(), commandLabel);
        future.exceptionally(e -> {
            user.sendMessage(tl("pendingTeleportCancelled"));
            return false;
        });
        if (request.isHere()) {
            final Location loc = request.getLocation();
            final AsyncTeleport teleport = requester.getAsyncTeleport();
            teleport.setTpType(AsyncTeleport.TeleportType.TPA);
            future.thenAccept(success -> {
                if (success) {
                    requester.sendMessage(tl("teleporting", loc.getWorld().getName(), loc.getBlockX(), loc.getBlockY(), loc.getBlockZ()));
                }
            });
            teleport.teleportPlayer(user, loc, charge, TeleportCause.COMMAND, future);
        } else {
            final AsyncTeleport teleport = requester.getAsyncTeleport();
            teleport.setTpType(AsyncTeleport.TeleportType.TPA);
            teleport.teleport(user.getBase(), charge, TeleportCause.COMMAND, future);
        }
        user.removeTpaRequest(request.getName());
    }
}
