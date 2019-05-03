package com.earth2me.essentials.commands;

import com.earth2me.essentials.CommandSource;
import com.earth2me.essentials.Console;
import com.earth2me.essentials.Trade;
import com.earth2me.essentials.User;
import org.bukkit.Location;
import org.bukkit.Server;
import org.bukkit.event.player.PlayerTeleportEvent.TeleportCause;

import java.util.Collections;
import java.util.List;


public class Commandtp extends EssentialsCommand {
    public Commandtp() {
        super("tp");
    }

    @Override
    public void run(final Server server, final User user, final String commandLabel, final String[] args) throws Exception {
        switch (args.length) {
            case 0:
                throw new NotEnoughArgumentsException();

            case 1:
                final User player = getPlayer(server, user, args, 0);
                if (!player.isTeleportEnabled()) {
                    throw new Exception(user.tl("teleportDisabled", player.getDisplayName()));
                }
                if (user.getWorld() != player.getWorld() && ess.getSettings().isWorldTeleportPermissions() && !user.isAuthorized("essentials.worlds." + player.getWorld().getName())) {
                    throw new Exception(user.tl("noPerm", "essentials.worlds." + player.getWorld().getName()));
                }
                final Trade charge = new Trade(this.getName(), ess);
                charge.isAffordableFor(user);
                user.getTeleport().teleport(player.getBase(), charge, TeleportCause.COMMAND);
                throw new NoChargeException();
            case 3:
                if (!user.isAuthorized("essentials.tp.position")) {
                    throw new Exception(user.tl("noPerm", "essentials.tp.position"));
                }
                final double x2 = args[0].startsWith("~") ? user.getLocation().getX() + (args[0].length() > 1 ? Double.parseDouble(args[0].substring(1)) : 0) : Double.parseDouble(args[0]);
                final double y2 = args[1].startsWith("~") ? user.getLocation().getY() + (args[1].length() > 1 ? Double.parseDouble(args[1].substring(1)) : 0) : Double.parseDouble(args[1]);
                final double z2 = args[2].startsWith("~") ? user.getLocation().getZ() + (args[2].length() > 1 ? Double.parseDouble(args[2].substring(1)) : 0) : Double.parseDouble(args[2]);
                if (x2 > 30000000 || y2 > 30000000 || z2 > 30000000 || x2 < -30000000 || y2 < -30000000 || z2 < -30000000) {
                    throw new NotEnoughArgumentsException(user.tl("teleportInvalidLocation"));
                }
                final Location locpos = new Location(user.getWorld(), x2, y2, z2, user.getLocation().getYaw(), user.getLocation().getPitch());
                user.getTeleport().now(locpos, false, TeleportCause.COMMAND);
                user.sendTl("teleporting", locpos.getWorld().getName(), locpos.getBlockX(), locpos.getBlockY(), locpos.getBlockZ());
                break;
            case 4:
                if (!user.isAuthorized("essentials.tp.others")) {
                    throw new Exception(user.tl("noPerm", "essentials.tp.others"));
                }
                if (!user.isAuthorized("essentials.tp.position")) {
                    throw new Exception(user.tl("noPerm", "essentials.tp.position"));
                }
                final User target2 = getPlayer(server, user, args, 0);
                final double x = args[1].startsWith("~") ? target2.getLocation().getX() + (args[1].length() > 1 ? Double.parseDouble(args[1].substring(1)) : 0) : Double.parseDouble(args[1]);
                final double y = args[2].startsWith("~") ? target2.getLocation().getY() + (args[2].length() > 1 ? Double.parseDouble(args[2].substring(1)) : 0) : Double.parseDouble(args[2]);
                final double z = args[3].startsWith("~") ? target2.getLocation().getZ() + (args[3].length() > 1 ? Double.parseDouble(args[3].substring(1)) : 0) : Double.parseDouble(args[3]);
                if (x > 30000000 || y > 30000000 || z > 30000000 || x < -30000000 || y < -30000000 || z < -30000000) {
                    throw new NotEnoughArgumentsException(user.tl("teleportInvalidLocation"));
                }
                final Location locposother = new Location(target2.getWorld(), x, y, z, target2.getLocation().getYaw(), target2.getLocation().getPitch());
                if (!target2.isTeleportEnabled()) {
                    throw new Exception(user.tl("teleportDisabled", target2.getDisplayName()));
                }
                user.sendTl("teleporting", locposother.getWorld().getName(), locposother.getBlockX(), locposother.getBlockY(), locposother.getBlockZ());
                target2.getTeleport().now(locposother, false, TeleportCause.COMMAND);
                target2.sendTl("teleporting", locposother.getWorld().getName(), locposother.getBlockX(), locposother.getBlockY(), locposother.getBlockZ());
                break;
            case 2:
            default:
                if (!user.isAuthorized("essentials.tp.others")) {
                    throw new Exception(user.tl("noPerm", "essentials.tp.others"));
                }
                final User target = getPlayer(server, user, args, 0);
                final User toPlayer = getPlayer(server, user, args, 1);
                if (!target.isTeleportEnabled()) {
                    throw new Exception(user.tl("teleportDisabled", target.getDisplayName()));
                }
                if (!toPlayer.isTeleportEnabled()) {
                    throw new Exception(user.tl("teleportDisabled", toPlayer.getDisplayName()));
                }
                if (target.getWorld() != toPlayer.getWorld() && ess.getSettings().isWorldTeleportPermissions() && !user.isAuthorized("essentials.worlds." + toPlayer.getWorld().getName())) {
                    throw new Exception(user.tl("noPerm", "essentials.worlds." + toPlayer.getWorld().getName()));
                }
                target.sendTl("teleportAtoB", user.getDisplayName(), toPlayer.getDisplayName());
                target.getTeleport().now(toPlayer.getBase(), false, TeleportCause.COMMAND);
                break;
        }
    }

    @Override
    public void run(final Server server, final CommandSource sender, final String commandLabel, final String[] args) throws Exception {
        if (args.length < 2) {
            throw new NotEnoughArgumentsException();
        }

        final User target = getPlayer(server, args, 0, true, false);
        if (args.length == 2) {
            final User toPlayer = getPlayer(server, args, 1, true, false);
            target.sendTl("teleportAtoB", Console.NAME, toPlayer.getDisplayName());
            target.getTeleport().now(toPlayer.getBase(), false, TeleportCause.COMMAND);
        } else if (args.length > 3) {
            final double x = args[1].startsWith("~") ? target.getLocation().getX() + (args[1].length() > 1 ? Double.parseDouble(args[1].substring(1)) : 0) : Double.parseDouble(args[1]);
            final double y = args[2].startsWith("~") ? target.getLocation().getY() + (args[2].length() > 1 ? Double.parseDouble(args[2].substring(1)) : 0) : Double.parseDouble(args[2]);
            final double z = args[3].startsWith("~") ? target.getLocation().getZ() + (args[3].length() > 1 ? Double.parseDouble(args[3].substring(1)) : 0) : Double.parseDouble(args[3]);
            if (x > 30000000 || y > 30000000 || z > 30000000 || x < -30000000 || y < -30000000 || z < -30000000) {
                throw new NotEnoughArgumentsException(sender.tl("teleportInvalidLocation"));
            }
            final Location loc = new Location(target.getWorld(), x, y, z, target.getLocation().getYaw(), target.getLocation().getPitch());
            sender.sendTl("teleporting", loc.getWorld().getName(), loc.getBlockX(), loc.getBlockY(), loc.getBlockZ());
            target.getTeleport().now(loc, false, TeleportCause.COMMAND);
            target.sendTl("teleporting", loc.getWorld().getName(), loc.getBlockX(), loc.getBlockY(), loc.getBlockZ());
        } else {
            throw new NotEnoughArgumentsException();
        }
    }

    @Override
    protected List<String> getTabCompleteOptions(Server server, User user, String commandLabel, String[] args) {
        // Don't handle coords
        if (args.length == 1 || (args.length == 2 && user.isAuthorized("essentials.tp.others"))) {
            return getPlayers(server, user);
        } else {
            return Collections.emptyList();
        }
    }

    @Override
    protected List<String> getTabCompleteOptions(Server server, CommandSource sender, String commandLabel, String[] args) {
        // Don't handle coords
        if (args.length == 1 || args.length == 2) {
            return getPlayers(server, sender);
        } else {
            return Collections.emptyList();
        }
    }
}
