package com.earth2me.essentials.commands;

import com.earth2me.essentials.CommandSource;
import com.earth2me.essentials.User;
import com.earth2me.essentials.utils.StringUtil;
import com.google.common.collect.Lists;
import org.bukkit.Material;
import org.bukkit.Server;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;


public class Commandpowertool extends EssentialsCommand {
    public Commandpowertool() {
        super("powertool");
    }

    @Override
    protected void run(final Server server, final User user, final String commandLabel, final String[] args) throws Exception {
        final String command = getFinalArg(args, 0);
        final ItemStack itemStack = user.getBase().getItemInHand();
        powertool(server, user.getSource(), user, commandLabel, itemStack, command);
    }

    @Override
    protected void run(final Server server, final CommandSource sender, final String commandLabel, final String[] args) throws Exception {
        if (args.length < 3) //running from console means inserting a player and item before the standard syntax
        {
            throw new Exception("When running from console, usage is: /" + commandLabel + " <player> <itemid> <command>");
        }

        final User user = getPlayer(server, args, 0, true, true);
        final ItemStack itemStack = ess.getItemDb().get(args[1]);
        final String command = getFinalArg(args, 2);
        powertool(server, sender, user, commandLabel, itemStack, command);
    }

    protected void powertool(final Server server, final CommandSource sender, final User user, final String commandLabel, final ItemStack itemStack, String command) throws Exception {
        // check to see if this is a clear all command
        if (command != null && command.equalsIgnoreCase("d:")) {
            user.clearAllPowertools();
            sender.sendTl("powerToolClearAll");
            return;
        }

        if (itemStack == null || itemStack.getType() == Material.AIR) {
            throw new Exception(user.tl("powerToolAir"));
        }

        final String itemName = itemStack.getType().toString().toLowerCase(Locale.ENGLISH).replaceAll("_", " ");
        List<String> powertools = user.getPowertool(itemStack);
        if (command != null && !command.isEmpty()) {
            if (command.equalsIgnoreCase("l:")) {
                if (powertools == null || powertools.isEmpty()) {
                    throw new Exception(user.tl("powerToolListEmpty", itemName));
                } else {
                    sender.sendTl("powerToolList", StringUtil.joinList(powertools), itemName);
                }
                throw new NoChargeException();
            }
            if (command.startsWith("r:")) {
                command = command.substring(2);
                if (!powertools.contains(command)) {
                    throw new Exception(user.tl("powerToolNoSuchCommandAssigned", command, itemName));
                }

                powertools.remove(command);
                sender.sendTl("powerToolRemove", command, itemName);
            } else {
                if (command.startsWith("a:")) {
                    if (sender.isPlayer() && !ess.getUser(sender.getPlayer()).isAuthorized("essentials.powertool.append")) {
                        throw new Exception(user.tl("noPerm", "essentials.powertool.append"));
                    }
                    command = command.substring(2);
                    if (powertools.contains(command)) {
                        throw new Exception(user.tl("powerToolAlreadySet", command, itemName));
                    }
                } else if (powertools != null && !powertools.isEmpty()) {
                    // Replace all commands with this one
                    powertools.clear();
                } else {
                    powertools = new ArrayList<String>();
                }

                powertools.add(command);
                sender.sendTl("powerToolAttach", StringUtil.joinList(powertools), itemName);
            }
        } else {
            if (powertools != null) {
                powertools.clear();
            }
            sender.sendTl("powerToolRemoveAll", itemName);
        }

        if (!user.arePowerToolsEnabled()) {
            user.setPowerToolsEnabled(true);
            user.sendTl("powerToolsEnabled");
        }
        user.setPowertool(itemStack, powertools);
    }

    @Override
    protected List<String> getTabCompleteOptions(Server server, User user, String commandLabel, String[] args) {
        if (args.length == 1) {
            List<String> options = Lists.newArrayList("d:", "c:", "l:");

            if (user.isAuthorized("essentials.powertool.append")) {
                for (String command : getCommands(server)) {
                    options.add("a:" + command);
                }
            }

            try {
                final ItemStack itemStack = user.getBase().getItemInHand();
                List<String> powertools = user.getPowertool(itemStack);
                for (String tool : powertools) {
                    options.add("r:" + tool);
                }
            } catch (Exception e) {}
            return options;
        } else if (args[0].startsWith("a:")) {
            return tabCompleteCommand(user.getSource(), server, args[0].substring(2), args, 1);
        } else {
            return Collections.emptyList();
        }
    }

    @Override
    protected List<String> getTabCompleteOptions(Server server, CommandSource sender, String commandLabel, String[] args) {
        if (args.length == 1) {
            return getPlayers(server, sender);
        } else if (args.length == 2) {
            return getItems();
        } else if (args.length == 3) {
            List<String> options = Lists.newArrayList("d:", "c:", "l:");

            for (String command : getCommands(server)) {
                options.add("a:" + command);
            }

            try {
                final User user = getPlayer(server, args, 0, true, true);
                final ItemStack itemStack = ess.getItemDb().get(args[1]);
                List<String> powertools = user.getPowertool(itemStack);
                for (String tool : powertools) {
                    options.add("r:" + tool);
                }
            } catch (Exception e) {}
            return options;
        } else if (args[2].startsWith("a:")) {
            return tabCompleteCommand(sender, server, args[2].substring(2), args, 3);
        } else {
            return Collections.emptyList();
        }
    }
}
