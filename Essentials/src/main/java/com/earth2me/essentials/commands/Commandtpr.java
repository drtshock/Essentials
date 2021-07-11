package com.earth2me.essentials.commands;

import com.earth2me.essentials.CommandSource;
import com.earth2me.essentials.RandomTeleport;
import com.earth2me.essentials.Trade;
import com.earth2me.essentials.User;
import net.ess3.api.events.UserRandomTeleportEvent;
import org.bukkit.Server;
import org.bukkit.event.player.PlayerTeleportEvent;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import static com.earth2me.essentials.I18n.tl;

public class Commandtpr extends EssentialsCommand {

    public Commandtpr() {
        super("tpr");
    }

    @Override
    protected void run(final Server server, final User user, final String commandLabel, final String[] args) throws Exception {
        final Trade charge = new Trade(this.getName(), ess);
        charge.isAffordableFor(user);
        final RandomTeleport randomTeleport = ess.getRandomTeleport();
        final String defaultLocation = randomTeleport.getDefaultLocation().replace("{world}", user.getLocation().getWorld().getName());
        final String name = args.length > 0 ? args[0] : defaultLocation;
        final User userToTeleport = args.length > 1 && user.isAuthorized("essentials.tpr.others") ? getPlayer(server, user, args, 1) : user;
        if (randomTeleport.isPerLocationPermission() && !user.isAuthorized("essentials.tpr.location." + name)) {
            throw new Exception(tl("warpUsePermission"));
        }
        final UserRandomTeleportEvent event = new UserRandomTeleportEvent(userToTeleport, name, randomTeleport.getCenter(name), randomTeleport.getMinRange(name), randomTeleport.getMaxRange(name));
        server.getPluginManager().callEvent(event);
        if (event.isCancelled()) {
            return;
        }
        (event.isModified() ? randomTeleport.getRandomLocation(event.getCenter(), event.getMinRange(), event.getMaxRange()) : randomTeleport.getRandomLocation(name))
                .thenAccept(location -> {
                    final CompletableFuture<Boolean> future = getNewExceptionFuture(user.getSource(), commandLabel);
                    future.thenAccept(success -> {
                        if (success) {
                            userToTeleport.sendMessage(tl("tprSuccess"));
                        }
                    });
                    userToTeleport.getAsyncTeleport().teleport(location, charge, PlayerTeleportEvent.TeleportCause.COMMAND, future);
                });
        throw new NoChargeException();
    }

    @Override
    protected void run(final Server server, final CommandSource sender, final String commandLabel, final String[] args) throws Exception {
        if (args.length < 2) {
            throw new NotEnoughArgumentsException();
        }
        final RandomTeleport randomTeleport = ess.getRandomTeleport();
        final User userToTeleport = getPlayer(server, sender, args, 1);
        final String name = args[0];
        final UserRandomTeleportEvent event = new UserRandomTeleportEvent(userToTeleport, name, randomTeleport.getCenter(name), randomTeleport.getMinRange(name), randomTeleport.getMaxRange(name));
        server.getPluginManager().callEvent(event);
        if (event.isCancelled()) {
            return;
        }
        (event.isModified() ? randomTeleport.getRandomLocation(event.getCenter(), event.getMinRange(), event.getMaxRange()) : randomTeleport.getRandomLocation(name))
                .thenAccept(location -> {
                    final CompletableFuture<Boolean> future = getNewExceptionFuture(sender, commandLabel);
                    future.thenAccept(success -> {
                        if (success) {
                            userToTeleport.sendMessage(tl("tprSuccess"));
                        }
                    });
                    userToTeleport.getAsyncTeleport().now(location, false, PlayerTeleportEvent.TeleportCause.COMMAND, future);
                });
    }

    @Override
    protected List<String> getTabCompleteOptions(final Server server, final CommandSource sender, final String commandLabel, final String[] args) {
        final RandomTeleport randomTeleport = ess.getRandomTeleport();
        if (args.length == 1) {
            if (randomTeleport.isPerLocationPermission()) {
                return randomTeleport.listLocations().stream().filter(name -> sender.isAuthorized("essentials.tpr.location." + name, ess)).collect(Collectors.toList());
            } else {
                return randomTeleport.listLocations();
            }
        } else if (args.length == 2 && sender.isAuthorized("essentials.tpr.others", ess)) {
            return getPlayers(server, sender);
        }
        return Collections.emptyList();
    }
}
