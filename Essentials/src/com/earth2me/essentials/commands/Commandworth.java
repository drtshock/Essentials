package com.earth2me.essentials.commands;

import com.earth2me.essentials.CommandSource;
import com.earth2me.essentials.User;
import com.earth2me.essentials.utils.NumberUtil;
import com.google.common.collect.Lists;
import org.bukkit.Server;
import org.bukkit.inventory.ItemStack;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Locale;


public class Commandworth extends EssentialsCommand {
    public Commandworth() {
        super("worth");
    }

    @Override
    public void run(final Server server, final User user, final String commandLabel, final String[] args) throws Exception {
        BigDecimal totalWorth = BigDecimal.ZERO;
        String type = "";

        List<ItemStack> is = ess.getItemDb().getMatching(user, args);
        int count = 0;

        boolean isBulk = is.size() > 1;

        for (ItemStack stack : is) {
            try {
                if (stack.getAmount() > 0) {
                    totalWorth = totalWorth.add(itemWorth(user.getSource(), user, stack, args));
                    stack = stack.clone();
                    count++;
                    for (ItemStack zeroStack : is) {
                        if (zeroStack.isSimilar(stack)) {
                            zeroStack.setAmount(0);
                        }
                    }
                }

            } catch (Exception e) {
                if (!isBulk) {
                    throw e;
                }
            }
        }
        if (count > 1) {
            if (args.length > 0 && args[0].equalsIgnoreCase("blocks")) {
                user.sendTl("totalSellableBlocks", type, NumberUtil.displayCurrency(totalWorth, ess));
            } else {
                user.sendTl("totalSellableAll", type, NumberUtil.displayCurrency(totalWorth, ess));
            }
        }
    }

    @Override
    public void run(final Server server, final CommandSource sender, final String commandLabel, final String[] args) throws Exception {
        if (args.length < 1) {
            throw new NotEnoughArgumentsException();
        }

        ItemStack stack = ess.getItemDb().get(args[0]);

        itemWorth(sender, null, stack, args);
    }

    private BigDecimal itemWorth(CommandSource sender, User user, ItemStack is, String[] args) throws Exception {
        int amount = 1;
        if (user == null) {
            if (args.length > 1) {
                try {
                    amount = Integer.parseInt(args[1].replaceAll("[^0-9]", ""));
                } catch (NumberFormatException ex) {
                    throw new NotEnoughArgumentsException(ex);
                }

            }
        } else {
            amount = ess.getWorth().getAmount(ess, user, is, args, true);
        }

        BigDecimal worth = ess.getWorth().getPrice(ess, is);

        if (worth == null) {
            throw new Exception(sender.tl("itemCannotBeSold"));
        }

        if (amount < 0) {
            amount = 0;
        }

        BigDecimal result = worth.multiply(BigDecimal.valueOf(amount));

        sender.sendMessage(is.getDurability() != 0 ? sender.tl("worthMeta", is.getType().toString().toLowerCase(Locale.ENGLISH).replace("_", ""), is.getDurability(), NumberUtil.displayCurrency(result, ess), amount, NumberUtil.displayCurrency(worth, ess)) : sender.tl("worth", is.getType().toString().toLowerCase(Locale.ENGLISH).replace("_", ""), NumberUtil.displayCurrency(result, ess), amount, NumberUtil.displayCurrency(worth, ess)));

        return result;
    }

    @Override
    protected List<String> getTabCompleteOptions(Server server, User user, String commandLabel, String[] args) {
        if (args.length == 1) {
            return getMatchingItems(args[0]);
        } else if (args.length == 2) {
            return Lists.newArrayList("1", "64");
        } else {
            return Collections.emptyList();
        }
    }

    @Override
    protected List<String> getTabCompleteOptions(Server server, CommandSource sender, String commandLabel, String[] args) {
        if (args.length == 1) {
            return getItems();
        } else if (args.length == 2) {
            return Lists.newArrayList("1", "64");
        } else {
            return Collections.emptyList();
        }
    }
}
