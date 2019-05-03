package com.earth2me.essentials.commands;

import com.earth2me.essentials.User;
import org.bukkit.Server;
import org.bukkit.event.player.PlayerTeleportEvent.TeleportCause;

import java.util.Collections;
import java.util.List;


public class Commandtpohere extends EssentialsCommand {
    public Commandtpohere() {
        super("tpohere");
    }

    @Override
    public void run(final Server server, final User user, final String commandLabel, final String[] args) throws Exception {
        if (args.length < 1) {
            throw new NotEnoughArgumentsException();
        }

        //Just basically the old tphere command
        final User player = getPlayer(server, user, args, 0);

        if (user.getWorld() != player.getWorld() && ess.getSettings().isWorldTeleportPermissions() && !user.isAuthorized("essentials.worlds." + user.getWorld().getName())) {
            throw new Exception(user.tl("noPerm", "essentials.worlds." + user.getWorld().getName()));
        }

        // Verify permission
        player.getTeleport().now(user.getBase(), false, TeleportCause.COMMAND);
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
