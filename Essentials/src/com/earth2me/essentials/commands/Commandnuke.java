package com.earth2me.essentials.commands;

import com.earth2me.essentials.CommandSource;
import org.bukkit.Location;
import org.bukkit.Server;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.entity.TNTPrimed;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;


public class Commandnuke extends EssentialsCommand {
    public Commandnuke() {
        super("nuke");
    }

    @Override
    protected void run(final Server server, final CommandSource sender, final String commandLabel, final String[] args) throws NoSuchFieldException, NotEnoughArgumentsException {
        Collection<Player> targets;
        if (args.length > 0) {
            targets = new ArrayList<>();
            int pos = 0;
            for (String arg : args) {
                targets.add(getPlayer(server, sender, args, pos).getBase());
                pos++;
            }
        } else {
            targets = ess.getOnlinePlayers();
        }
        ess.getTNTListener().enable();
        for (Player player : targets) {
            if (player == null) {
                continue;
            }
            ess.getUser(player).sendTl("nuke");
            final Location loc = player.getLocation();
            final World world = loc.getWorld();
            for (int x = -10; x <= 10; x += 5) {
                for (int z = -10; z <= 10; z += 5) {
                    final Location tntloc = new Location(world, loc.getBlockX() + x, world.getHighestBlockYAt(loc) + 64, loc.getBlockZ() + z);
                    final TNTPrimed tnt = world.spawn(tntloc, TNTPrimed.class);
                }
            }
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
