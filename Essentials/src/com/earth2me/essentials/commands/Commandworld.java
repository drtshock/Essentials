package com.earth2me.essentials.commands;

import com.earth2me.essentials.Trade;
import com.earth2me.essentials.User;
import com.google.common.collect.Lists;
import org.bukkit.Location;
import org.bukkit.Server;
import org.bukkit.World;
import org.bukkit.event.player.PlayerTeleportEvent.TeleportCause;

import java.util.Collections;
import java.util.List;


public class Commandworld extends EssentialsCommand {
    public Commandworld() {
        super("world");
    }

    @Override
    protected void run(final Server server, final User user, final String commandLabel, final String[] args) throws Exception {
        World world;

        if (args.length < 1) {
            World nether = null;

            final List<World> worlds = server.getWorlds();

            for (World world2 : worlds) {
                if (world2.getEnvironment() == World.Environment.NETHER) {
                    nether = world2;
                    break;
                }
            }
            if (nether == null) {
                return;
            }
            world = user.getWorld() == nether ? worlds.get(0) : nether;
        } else {
            world = ess.getWorld(getFinalArg(args, 0));
            if (world == null) {
                user.sendTl("invalidWorld");
                user.sendTl("possibleWorlds", server.getWorlds().size() - 1);
                user.sendTl("typeWorldName");
                throw new NoChargeException();
            }
        }

        if (ess.getSettings().isWorldTeleportPermissions() && !user.isAuthorized("essentials.worlds." + world.getName())) {
            throw new Exception(user.tl("noPerm", "essentials.worlds." + world.getName()));
        }

        double factor;
        if (user.getWorld().getEnvironment() == World.Environment.NETHER && world.getEnvironment() == World.Environment.NORMAL) {
            factor = 8.0;
        } else if (user.getWorld().getEnvironment() == World.Environment.NORMAL && world.getEnvironment() == World.Environment.NETHER) {
            factor = 1.0 / 8.0;
        } else {
            factor = 1.0;
        }

        final Location loc = user.getLocation();
        final Location target = new Location(world, loc.getBlockX() * factor + .5, loc.getBlockY(), loc.getBlockZ() * factor + .5);

        final Trade charge = new Trade(this.getName(), ess);
        charge.isAffordableFor(user);
        user.getTeleport().teleport(target, charge, TeleportCause.COMMAND);
        throw new NoChargeException();
    }

    @Override
    protected List<String> getTabCompleteOptions(Server server, User user, String commandLabel, String[] args) {
        if (args.length == 1) {
            List<String> worlds = Lists.newArrayList();
            for (World world : server.getWorlds()) {
                if (ess.getSettings().isWorldTeleportPermissions() && !user.isAuthorized("essentials.worlds." + world.getName())) {
                    continue;
                }
                worlds.add(world.getName());
            }
            return worlds;
        } else {
            return Collections.emptyList();
        }
    }
}
