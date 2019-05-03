package com.earth2me.essentials.commands;

import com.earth2me.essentials.CommandSource;
import com.earth2me.essentials.User;
import com.earth2me.essentials.craftbukkit.InventoryWorkaround;
import com.earth2me.essentials.utils.NumberUtil;
import com.earth2me.essentials.utils.StringUtil;

import org.bukkit.Material;
import org.bukkit.Server;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.*;


public class Commandclearinventory extends EssentialsCommand {

    public Commandclearinventory() {
        super("clearinventory");
    }

    private static final int BASE_AMOUNT = 100000;
    private static final int EXTENDED_CAP = 8;

    @Override
    public void run(Server server, User user, String commandLabel, String[] args) throws Exception {
        parseCommand(server, user.getSource(), commandLabel, args, user.isAuthorized("essentials.clearinventory.others"),
            user.isAuthorized("essentials.clearinventory.all") || user.isAuthorized("essentials.clearinventory.multiple"));
    }

    @Override
    protected void run(Server server, CommandSource sender, String commandLabel, String[] args) throws Exception {
        parseCommand(server, sender, commandLabel, args, true, true);
    }

    private void parseCommand(Server server, CommandSource sender, String commandLabel, String[] args, boolean allowOthers, boolean allowAll)
        throws Exception {
        Collection<Player> players = new ArrayList<Player>();
        User senderUser = ess.getUser(sender.getPlayer());
        String previousClearCommand = "";
        
        int offset = 0;

        if (sender.isPlayer()) {
            players.add(sender.getPlayer());
            // Clear previous command execution before potential errors to reset confirmation.
            previousClearCommand = senderUser.getConfirmingClearCommand();
            senderUser.setConfirmingClearCommand(null);
        }

        if (allowAll && args.length > 0 && args[0].contentEquals("*")) {
            sender.sendTl("inventoryClearingFromAll");
            offset = 1;
            players = ess.getOnlinePlayers();
        } else if (allowOthers && args.length > 0 && args[0].trim().length() > 2) {
            offset = 1;
            players = server.matchPlayer(args[0].trim());
        }

        if (players.size() < 1) {
            throw new PlayerNotFoundException();
        }


        // Confirm
        String formattedCommand = formatCommand(commandLabel, args);
        if (senderUser != null && senderUser.isPromptingClearConfirm()) {
            if (!formattedCommand.equals(previousClearCommand)) {
                senderUser.setConfirmingClearCommand(formattedCommand);
                senderUser.sendTl("confirmClear", formattedCommand);
                return;
            }
        }

        for (Player player : players) {
            clearHandler(sender, player, args, offset, players.size() < EXTENDED_CAP);
        }
    }

    protected void clearHandler(CommandSource sender, Player player, String[] args, int offset, boolean showExtended) throws Exception {
        short data = -1;
        int type = -1;
        int amount = -1;
        final Set<Material> mats = new HashSet<>();

        if (args.length > (offset + 1) && NumberUtil.isInt(args[(offset + 1)])) {
            amount = Integer.parseInt(args[(offset + 1)]);
        }
        if (args.length > offset) {
            if (args[offset].equalsIgnoreCase("**")) {
                type = -2;
            } else if (!args[offset].equalsIgnoreCase("*")) {
                final String[] split = args[offset].split(",");

                for (String name : split) {
                    try {
                        mats.add(ess.getItemDb().get(name).getType());
                    } catch (Exception ignored) {}
                }

                type = 1;
            }
        }

        if (type == -1) // type -1 represents wildcard or all items
        {
            if (showExtended) {
                sender.sendTl("inventoryClearingAllItems", player.getDisplayName());
            }
            InventoryWorkaround.clearInventoryNoArmor(player.getInventory());
            InventoryWorkaround.setItemInOffHand(player, null);
        } else if (type == -2) // type -2 represents double wildcard or all items and armor
        {
            if (showExtended) {
                sender.sendTl("inventoryClearingAllArmor", player.getDisplayName());
            }
            InventoryWorkaround.clearInventoryNoArmor(player.getInventory());
            InventoryWorkaround.setItemInOffHand(player, null);
            player.getInventory().setArmorContents(null);
        } else {
            for (Material mat : mats) {
                if (amount == -1) // amount -1 means all items will be cleared
                {
                    ItemStack stack = new ItemStack(mat, BASE_AMOUNT, data);
                    ItemStack removedStack = player.getInventory().removeItem(stack).get(0);
                    final int removedAmount = (BASE_AMOUNT - removedStack.getAmount());
                    if (removedAmount > 0 || showExtended) {
                        sender.sendTl("inventoryClearingStack", removedAmount, stack.getType().toString().toLowerCase(Locale.ENGLISH), player.getDisplayName());
                    }
                } else {
                    if (amount < 0) {
                        amount = 1;
                    }
                    ItemStack stack = new ItemStack(mat, amount);
                    if (player.getInventory().containsAtLeast(stack, amount)) {
                        sender.sendTl("inventoryClearingStack", amount, stack.getType().toString().toLowerCase(Locale.ENGLISH), player.getDisplayName());
                        player.getInventory().removeItem(stack);
                    } else {
                        if (showExtended) {
                            sender.sendTl("inventoryClearFail", player.getDisplayName(), amount, stack.getType().toString().toLowerCase(Locale.ENGLISH));
                        }
                    }
                }
            }
        }
    }

    @Override
    protected List<String> getTabCompleteOptions(Server server, User user, String commandLabel, String[] args) {
        if (user.isAuthorized("essentials.clearinventory.others")) {
            if (args.length == 1) {
                List<String> options = getPlayers(server, user);
                if (user.isAuthorized("essentials.clearinventory.all") || user.isAuthorized("essentials.clearinventory.multiple")) {
                    // Assume that nobody will have the 'all' permission without the 'others' permission
                    options.add("*");
                }
                return options;
            } else if (args.length == 2) {
                List<String> items = new ArrayList<>(getItems());
                items.add("*");
                items.add("**");
                return items;
            } else {
                return Collections.emptyList();
            }
        } else {
            if (args.length == 1) {
                List<String> items = new ArrayList<>(getItems());
                items.add("*");
                items.add("**");
                return items;
            } else {
                return Collections.emptyList();
            }
        }
    }

    @Override
    protected List<String> getTabCompleteOptions(Server server, CommandSource sender, String commandLabel, String[] args) {
        if (args.length == 1) {
            List<String> options = getPlayers(server, sender);
            options.add("*");
            return options;
        } else if (args.length == 2) {
            List<String> items = new ArrayList<>(getItems());
            items.add("*");
            items.add("**");
            return items;
        } else {
            return Collections.emptyList();
        }
    }

    private String formatCommand(String commandLabel, String[] args) {
        return "/" + commandLabel + " " + StringUtil.joinList(" ", (Object[]) args);
    }
}
