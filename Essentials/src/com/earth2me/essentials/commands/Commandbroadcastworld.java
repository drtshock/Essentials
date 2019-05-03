package com.earth2me.essentials.commands;

import static com.earth2me.essentials.I18n.tl;

import com.earth2me.essentials.CommandSource;
import com.earth2me.essentials.User;
import com.earth2me.essentials.textreader.IText;
import com.earth2me.essentials.textreader.KeywordReplacer;
import com.earth2me.essentials.textreader.SimpleTextInput;
import com.earth2me.essentials.utils.FormatUtil;
import com.google.common.collect.Lists;

import org.bukkit.Server;
import org.bukkit.World;
import org.bukkit.entity.Player;

import java.util.Collection;
import java.util.Collections;
import java.util.List;


public class Commandbroadcastworld extends EssentialsCommand {

    public Commandbroadcastworld() {
        super("broadcastworld");
    }

    @Override
    public void run(final Server server, final User user, final String commandLabel, final String[] args) throws Exception {
        World world = user.getWorld();
        String message = getFinalArg(args, 0);

        if (args.length < 1) {
            throw new NotEnoughArgumentsException();
        } else if (args.length > 1 && ess.getSettings().isAllowWorldInBroadcastworld()) {
            World argWorld = ess.getWorld(args[0]);
            if (argWorld != null) {
                world = argWorld;
                message = getFinalArg(args, 1);
            }
        }

        if (world == null) {
            world = user.getWorld();
            message = getFinalArg(args, 0);
        }

        sendBroadcast(user.getSource(), world.getName(), user.getDisplayName(), message);
    }

    @Override
    public void run(final Server server, final CommandSource sender, final String commandLabel, final String[] args) throws Exception {
        if (args.length < 2) {
            throw new NotEnoughArgumentsException("world");
        }
        sendBroadcast(sender, args[0], sender.getSender().getName(), getFinalArg(args, 1));
    }

    private void sendBroadcast(final CommandSource sender, final String worldName, final String name, final String message) throws Exception {
        World world = ess.getWorld(worldName);
        if (world == null) {
            throw new Exception(sender.tl("invalidWorld"));
        }
        if (message.isEmpty()) {
            throw new NotEnoughArgumentsException();
        }
        sendToWorld(world, "broadcast", FormatUtil.replaceFormat(message).replace("\\n", "\n"), name);
    }

    private void sendToWorld(World world, String string, Object... objects) {
        final Collection<Player> players = ess.getOnlinePlayers();

        for (Player player : players) {
            if (player.getWorld().equals(world)) {
                final User user = ess.getUser(player);
                IText broadcast = new SimpleTextInput(user.tl(string, objects));
                broadcast = new KeywordReplacer(broadcast, new CommandSource(player), ess, false);
                for (String messageText : broadcast.getLines()) {
                    user.sendMessage(messageText);
                }
            }
        }
    }

    @Override
    protected List<String> getTabCompleteOptions(Server server, User user, String commandLabel, String[] args) {
        if (args.length == 1 && ess.getSettings().isAllowWorldInBroadcastworld()) {
            List<String> worlds = Lists.newArrayList();
            for (World world : server.getWorlds()) {
                worlds.add(world.getName());
            }
            return worlds;
        } else {
            return Collections.emptyList();
        }
    }

    @Override
    protected List<String> getTabCompleteOptions(Server server, CommandSource sender, String commandLabel, String[] args) {
        if (args.length == 1) {
            List<String> worlds = Lists.newArrayList();
            for (World world : server.getWorlds()) {
                worlds.add(world.getName());
            }
            return worlds;
        } else {
            return Collections.emptyList();
        }
    }
}
