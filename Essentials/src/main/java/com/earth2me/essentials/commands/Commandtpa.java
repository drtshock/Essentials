package com.earth2me.essentials.commands;

import com.earth2me.essentials.AsyncTeleport;
import com.earth2me.essentials.Trade;
import com.earth2me.essentials.User;
import net.ess3.api.events.TPARequestEvent;
import org.bukkit.Server;
import org.bukkit.event.player.PlayerTeleportEvent;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import static com.earth2me.essentials.I18n.tl;

public class Commandtpa extends EssentialsCommand {
    public Commandtpa() {
        super("tpa");
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
        if (user.getWorld() != player.getWorld() && ess.getSettings().isWorldTeleportPermissions() && !user.isAuthorized("essentials.worlds." + player.getWorld().getName())) {
            throw new Exception(tl("noPerm", "essentials.worlds." + player.getWorld().getName()));
        }

        // Don't let sender request teleport twice to the same player.
        if (player.hasOutstandingTpaRequest(user.getName(), false)) {
            throw new Exception(tl("requestSentAlready", player.getDisplayName()));
        }

        if (player.isAutoTeleportEnabled() && !player.isIgnoredPlayer(user)) {
            final Trade charge = new Trade(this.getName(), ess);
            final AsyncTeleport teleport = user.getAsyncTeleport();
            teleport.setTpType(AsyncTeleport.TeleportType.TPA);
            final CompletableFuture<Boolean> future = getNewExceptionFuture(user.getSource(), commandLabel);
            teleport.teleport(player.getBase(), charge, PlayerTeleportEvent.TeleportCause.COMMAND, future);
            future.thenAccept(success -> {
                if (success) {
                    player.sendMessage(tl("requestAcceptedAuto", user.getDisplayName()));
                    user.sendMessage(tl("requestAcceptedFromAuto", player.getDisplayName()));
                }
            });
            return;
        }

        if (!player.isIgnoredPlayer(user)) {
            final TPARequestEvent tpaEvent = new TPARequestEvent(user.getSource(), player, false);
            ess.getServer().getPluginManager().callEvent(tpaEvent);
            if (tpaEvent.isCancelled()) {
                throw new Exception(tl("teleportRequestCancelled", player.getDisplayName()));
            }
            player.requestTeleport(user, false);
            player.sendMessage(tl("teleportRequest", user.getDisplayName()));
            player.sendMessage(tl("typeTpaccept"));
            player.sendMessage(tl("typeTpdeny"));
            if (ess.getSettings().getTpaAcceptCancellation() != 0) {
                player.sendMessage(tl("teleportRequestTimeoutInfo", ess.getSettings().getTpaAcceptCancellation()));
            }
        }

        user.sendMessage(tl("requestSent", player.getDisplayName()));
        if (user.isAuthorized("essentials.tpacancel")) {
            user.sendMessage(tl("typeTpacancel"));
        }
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
