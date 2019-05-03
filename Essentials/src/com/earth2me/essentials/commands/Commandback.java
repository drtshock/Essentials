package com.earth2me.essentials.commands;

import com.earth2me.essentials.CommandSource;
import com.earth2me.essentials.Trade;
import com.earth2me.essentials.User;
import org.bukkit.Server;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;


public class Commandback extends EssentialsCommand {
    public Commandback() {
        super("back");
    }

    @Override
    protected void run(Server server, User user, String commandLabel, String[] args) throws Exception {
        CommandSource sender = user.getSource();
        if (args.length > 0 && user.isAuthorized("essentials.back.others")) {
            this.parseCommand(server, sender, args, true);
            return;
        }

        teleportBack(sender, user);
    }

    @Override
    protected void run(Server server, CommandSource sender, String commandLabel, String[] args) throws Exception {
        if (args.length < 1) {
            throw new NotEnoughArgumentsException();
        }

        this.parseCommand(server, sender, args, true);
    }

    private void parseCommand(Server server, CommandSource sender, String[] args, boolean allowOthers) throws Exception {
        Collection<Player> players = new ArrayList<>();

        if (allowOthers && args.length > 0 && args[0].trim().length() > 2) {
            players = server.matchPlayer(args[0].trim());
        }

        if (players.size() < 1) {
            throw new PlayerNotFoundException();
        }

        for (Player player : players) {
            sender.sendTl("backOther", player.getName());
            teleportBack(sender, ess.getUser(player));
        }
    }

    private void teleportBack(CommandSource sender, User user) throws Exception {
        if (user.getLastLocation() == null) {
            throw new Exception(sender.tl("noLocationFound"));
        }

        String lastWorldName = user.getLastLocation().getWorld().getName();

        User requester = null;
        if (sender.isPlayer()) {
            requester = ess.getUser(sender.getPlayer());

            if (user.getWorld() != user.getLastLocation().getWorld() && this.ess.getSettings().isWorldTeleportPermissions() && !user.isAuthorized("essentials.worlds." + lastWorldName)) {
                throw new Exception(sender.tl("noPerm", "essentials.worlds." + lastWorldName));
            }

            if (!requester.isAuthorized("essentials.back.into." + lastWorldName)) {
                throw new Exception(sender.tl("noPerm", "essentials.back.into." + lastWorldName));
            }
        }

        if (requester == null) {
            user.getTeleport().back(null, null);
        } else if (!requester.equals(user)) {
            Trade charge = new Trade(this.getName(), this.ess);
            charge.isAffordableFor(requester);
            user.getTeleport().back(requester, charge);
        } else {
            Trade charge = new Trade(this.getName(), this.ess);
            charge.isAffordableFor(user);
            user.getTeleport().back(charge);
        }
        throw new NoChargeException();
    }

    @Override
    protected List<String> getTabCompleteOptions(Server server, User user, String commandLabel, String[] args) {
        if (user.isAuthorized("essentials.back.others") && args.length == 1) {
            return getPlayers(server, user);
        } else {
            return Collections.emptyList();
        }
    }
}
