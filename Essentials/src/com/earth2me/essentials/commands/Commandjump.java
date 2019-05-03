package com.earth2me.essentials.commands;

import com.earth2me.essentials.Trade;
import com.earth2me.essentials.User;
import com.earth2me.essentials.utils.LocationUtil;
import com.google.common.collect.Lists;
import org.bukkit.Location;
import org.bukkit.Server;
import org.bukkit.event.player.PlayerTeleportEvent.TeleportCause;

import java.util.Collections;
import java.util.List;

// This method contains an undocumented sub command #EasterEgg
public class Commandjump extends EssentialsCommand {
    public Commandjump() {
        super("jump");
    }

    @Override
    public void run(final Server server, final User user, final String commandLabel, final String[] args) throws Exception {
        if (args.length > 0 && args[0].contains("lock") && user.isAuthorized("essentials.jump.lock")) {
            if (user.isFlyClickJump()) {
                user.setRightClickJump(false);
                user.sendMessage("Flying wizard mode disabled");
            } else {
                user.setRightClickJump(true);
                user.sendMessage("Enabling flying wizard mode");
            }
            return;
        }

        Location loc;
        final Location cloc = user.getLocation();

        try {
            loc = LocationUtil.getTarget(user.getBase());
            loc.setYaw(cloc.getYaw());
            loc.setPitch(cloc.getPitch());
            loc.setY(loc.getY() + 1);
        } catch (NullPointerException ex) {
            throw new Exception(user.tl("jumpError"), ex);
        }

        final Trade charge = new Trade(this.getName(), ess);
        charge.isAffordableFor(user);
        user.getTeleport().teleport(loc, charge, TeleportCause.COMMAND);
        throw new NoChargeException();
    }

    @Override
    protected List<String> getTabCompleteOptions(Server server, User user, String commandLabel, String[] args) {
        if (args.length == 1 && user.isAuthorized("essentials.jump.lock")) {
            // XXX these actually do the same thing
            return Lists.newArrayList("lock", "unlock");
        } else {
            return Collections.emptyList();
        }
    }
}
