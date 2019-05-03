package com.earth2me.essentials.commands;

import com.earth2me.essentials.CommandSource;
import com.earth2me.essentials.User;
import org.bukkit.Location;
import org.bukkit.Server;

import java.util.Collections;
import java.util.List;


public class Commandgetpos extends EssentialsCommand {
    public Commandgetpos() {
        super("getpos");
    }

    @Override
    public void run(final Server server, final User user, final String commandLabel, final String[] args) throws Exception {
        if (args.length > 0 && user.isAuthorized("essentials.getpos.others")) {
            final User otherUser = getPlayer(server, user, args, 0);
            outputPosition(user.getSource(), otherUser.getLocation(), user.getLocation());
            return;
        }
        outputPosition(user.getSource(), user.getLocation(), null);
    }

    @Override
    protected void run(final Server server, final CommandSource sender, final String commandLabel, final String[] args) throws Exception {
        if (args.length < 1) {
            throw new NotEnoughArgumentsException();
        }
        final User user = getPlayer(server, args, 0, true, false);
        outputPosition(sender, user.getLocation(), null);
    }

    private void outputPosition(final CommandSource sender, final Location coords, final Location distance) {
        sender.sendTl("currentWorld", coords.getWorld().getName());
        sender.sendTl("posX", coords.getBlockX());
        sender.sendTl("posY", coords.getBlockY());
        sender.sendTl("posZ", coords.getBlockZ());
        sender.sendTl("posYaw", (coords.getYaw() + 360) % 360);
        sender.sendTl("posPitch", coords.getPitch());
        if (distance != null && coords.getWorld().equals(distance.getWorld())) {
            sender.sendTl("distance", coords.distance(distance));
        }
    }

    @Override
    protected List<String> getTabCompleteOptions(Server server, User user, String commandLabel, String[] args) {
        if (args.length == 1 && user.isAuthorized("essentials.getpos.others")) {
            return getPlayers(server, user);
        } else {
            return Collections.emptyList();
        }
    }

    @Override
    protected List<String> getTabCompleteOptions(Server server, CommandSource sender, String commandLabel, String[] args) {
        if (args.length == 1) {
            return getPlayers(server, sender);
        } else {
            return Collections.emptyList();
        }
    }
}
