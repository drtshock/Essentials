package com.earth2me.essentials.commands;

import com.earth2me.essentials.User;
import com.google.common.collect.Lists;
import org.bukkit.Server;
import org.bukkit.World;

import java.util.Collections;
import java.util.List;


public class Commandthunder extends EssentialsCommand {
    public Commandthunder() {
        super("thunder");
    }

    @Override
    public void run(final Server server, final User user, final String commandLabel, final String[] args) throws Exception {
        if (args.length < 1) {
            throw new NotEnoughArgumentsException();
        }

        final World world = user.getWorld();
        final boolean setThunder = args[0].equalsIgnoreCase("true");
        if (args.length > 1) {

            world.setThundering(setThunder);
            world.setThunderDuration(Integer.parseInt(args[1]) * 20);
            user.sendTl("thunderDuration", (setThunder ? user.tl("enabled") : user.tl("disabled")), Integer.parseInt(args[1]));

        } else {
            world.setThundering(setThunder);
            user.sendTl("thunder", setThunder ? user.tl("enabled") : user.tl("disabled"));
        }
    }

    @Override
    protected List<String> getTabCompleteOptions(Server server, User user, String commandLabel, String[] args) {
        if (args.length == 1) {
            return Lists.newArrayList("true", "false");
        } else if (args.length == 2) {
            return COMMON_DATE_DIFFS;
        } else {
            return Collections.emptyList();
        }
    }
}
