package com.earth2me.essentials.commands;

import com.earth2me.essentials.User;
import org.bukkit.Server;


public class Commandcompass extends EssentialsCommand {
    public Commandcompass() {
        super("compass");
    }

    @Override
    public void run(final Server server, final User user, final String commandLabel, final String[] args) throws Exception {
        final int bearing = (int) (user.getLocation().getYaw() + 180 + 360) % 360;
        String dir;
        if (bearing < 23) {
            dir = user.tl("north");
        } else if (bearing < 68) {
            dir = user.tl("northEast");
        } else if (bearing < 113) {
            dir = user.tl("east");
        } else if (bearing < 158) {
            dir = user.tl("southEast");
        } else if (bearing < 203) {
            dir = user.tl("south");
        } else if (bearing < 248) {
            dir = user.tl("southWest");
        } else if (bearing < 293) {
            dir = user.tl("west");
        } else if (bearing < 338) {
            dir = user.tl("northWest");
        } else {
            dir = user.tl("north");
        }
        user.sendTl("compassBearing", dir, bearing);
    }
}
