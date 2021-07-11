package com.earth2me.essentials.commands;

import com.earth2me.essentials.RandomTeleport;
import com.earth2me.essentials.User;
import org.bukkit.Server;
import org.bukkit.World;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import static com.earth2me.essentials.I18n.tl;

public class Commandsettpr extends EssentialsCommand {
    public Commandsettpr() {
        super("settpr");
    }

    @Override
    protected void run(final Server server, final User user, final String commandLabel, final String[] args) throws Exception {
        if (args.length < 2) {
            throw new NotEnoughArgumentsException();
        }
        final RandomTeleport randomTeleport = ess.getRandomTeleport();
        randomTeleport.reloadConfig();
        if ("center".equalsIgnoreCase(args[1])) {
            randomTeleport.setCenter(args[0], user.getLocation());
            user.sendMessage(tl("settpr"));
        } else if (args.length > 2) {
            if ("minrange".equalsIgnoreCase(args[1])) {
                randomTeleport.setMinRange(args[0], Double.parseDouble(args[2]));
            } else if ("maxrange".equalsIgnoreCase(args[1])) {
                randomTeleport.setMaxRange(args[0], Double.parseDouble(args[2]));
            }
            user.sendMessage(tl("settprValue", args[1].toLowerCase(), args[2].toLowerCase()));
        } else {
            throw new NotEnoughArgumentsException();
        }
    }

    @Override
    protected List<String> getTabCompleteOptions(final Server server, final User user, final String commandLabel, final String[] args) {
        if (args.length == 1) {
            return user.getServer().getWorlds().stream().map(World::getName).collect(Collectors.toList());
        } else if (args.length == 2) {
            return Arrays.asList("center", "minrange", "maxrange");
        }
        return Collections.emptyList();
    }
}
