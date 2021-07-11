package com.earth2me.essentials.commands;

import com.earth2me.essentials.IUser;
import com.earth2me.essentials.User;
import net.essentialsx.api.v2.events.TeleportRequestResponseEvent;
import org.bukkit.Bukkit;
import org.bukkit.Server;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static com.earth2me.essentials.I18n.tl;

public class Commandtpdeny extends EssentialsCommand {
    public Commandtpdeny() {
        super("tpdeny");
    }

    @Override
    public void run(final Server server, final User user, final String commandLabel, final String[] args) throws Exception {
        final boolean excludeOthers;
        if (args.length > 0) {
            excludeOthers = args[0].equals("*") || args[0].equalsIgnoreCase("all");
        } else {
            excludeOthers = false;
        }

        if (!user.hasPendingTpaRequests(false, excludeOthers)) {
            throw new Exception(tl("noPendingRequest"));
        }

        final IUser.TpaRequest denyRequest;
        if (args.length > 0) {
            if (excludeOthers) {
                IUser.TpaRequest request;
                int count = 0;
                while ((request = user.getNextTpaRequest(false, true, true)) != null) {
                    final User player = ess.getUser(request.getRequesterUuid());

                    final TeleportRequestResponseEvent event = new TeleportRequestResponseEvent(user, player, request, false);
                    Bukkit.getPluginManager().callEvent(event);
                    if (event.isCancelled()) {
                        if (ess.getSettings().isDebug()) {
                            logger.info("TPA deny cancelled by API for " + user.getName() + " (requested by " + player.getName() + ")");
                        }
                        continue;
                    }

                    if (player != null && player.getBase().isOnline()) {
                        player.sendMessage(tl("requestDeniedFrom", user.getDisplayName()));
                    }

                    user.removeTpaRequest(request.getName());
                    count++;
                }
                user.sendMessage(tl("requestDeniedAll", count));
                return;
            }
            denyRequest = user.getOutstandingTpaRequest(getPlayer(server, user, args, 0).getName(), false);
        } else {
            denyRequest = user.getNextTpaRequest(false, true, false);
        }

        final User player = ess.getUser(denyRequest.getRequesterUuid());
        if (player == null || !player.getBase().isOnline()) {
            throw new Exception(tl("noPendingRequest"));
        }

        final TeleportRequestResponseEvent event = new TeleportRequestResponseEvent(user, player, denyRequest, false);
        Bukkit.getPluginManager().callEvent(event);
        if (event.isCancelled()) {
            if (ess.getSettings().isDebug()) {
                logger.info("TPA deny cancelled by API for " + user.getName() + " (requested by " + player.getName() + ")");
            }
            return;
        }

        user.sendMessage(tl("requestDenied"));
        player.sendMessage(tl("requestDeniedFrom", user.getDisplayName()));
        user.removeTpaRequest(denyRequest.getName());
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
}
