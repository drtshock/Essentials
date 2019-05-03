package com.earth2me.essentials.commands;

import com.earth2me.essentials.ChargeException;
import com.earth2me.essentials.CommandSource;
import com.earth2me.essentials.Trade;
import com.earth2me.essentials.User;
import com.earth2me.essentials.utils.NumberUtil;
import com.earth2me.essentials.utils.StringUtil;

import com.google.common.collect.Lists;
import net.ess3.api.MaxMoneyException;
import org.bukkit.Server;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;


public class Commandpay extends EssentialsLoopCommand {
    BigDecimal amount;
    boolean informToConfirm;

    public Commandpay() {
        super("pay");
    }

    @Override
    public void run(final Server server, final User user, final String commandLabel, final String[] args) throws Exception {
        informToConfirm = false;
        if (args.length < 2) {
            throw new NotEnoughArgumentsException();
        }
        
        if (args[1].contains("-")) {
            throw new Exception(user.tl("payMustBePositive"));
        }

        String stringAmount = args[1].replaceAll("[^0-9\\.]", "");

        if (stringAmount.length() < 1) {
            throw new NotEnoughArgumentsException();
        }

        amount = new BigDecimal(stringAmount);
        if (amount.compareTo(ess.getSettings().getMinimumPayAmount()) < 0) { // Check if amount is less than minimum-pay-amount
            throw new Exception(user.tl("minimumPayAmount", NumberUtil.displayCurrencyExactly(ess.getSettings().getMinimumPayAmount(), ess)));
        }
        loopOnlinePlayers(server, user.getSource(), false, user.isAuthorized("essentials.pay.multiple"), args[0], args);
        if (informToConfirm) {
            String cmd = "/" + commandLabel + " " + StringUtil.joinList(" ", (Object[]) args);
            user.sendTl("confirmPayment", NumberUtil.displayCurrency(amount, ess), cmd);
        }
    }

    @Override
    protected void updatePlayer(final Server server, final CommandSource sender, final User player, final String[] args) throws ChargeException {
        User user = ess.getUser(sender.getPlayer());
        try {
            if (!player.isAcceptingPay()) {
                sender.sendTl("notAcceptingPay", player.getDisplayName());
                return;
            }
            if (user.isPromptingPayConfirm() && !amount.equals(user.getConfirmingPayments().get(player))) { // checks if exists and if command needs to be repeated.
                // Used to reset confirmations and inform to confirm when a new pay command has been inserted.
                if (!informToConfirm) {
                    // User hasnt been asked to confirm payment to this player, reset all confirmed payments and ask to confirm again.
                    // Clear previous confirmations to ensure that a new confirmation message is brought up.
                    user.getConfirmingPayments().clear();
                    this.informToConfirm = true;
                }
                user.getConfirmingPayments().put(player, amount);
                return;
            }
            user.payUser(player, amount);
            user.getConfirmingPayments().remove(player);
            Trade.log("Command", "Pay", "Player", user.getName(), new Trade(amount, ess), player.getName(), new Trade(amount, ess), user.getLocation(), ess);
        } catch (MaxMoneyException ex) {
            sender.sendTl("maxMoney");
            try {
                user.setMoney(user.getMoney().add(amount));
            } catch (MaxMoneyException ignored) {
                // this should never happen
            }
        } catch (Exception e) {
            sender.sendMessage(e.getMessage());
        }
    }

    @Override
    protected List<String> getTabCompleteOptions(Server server, CommandSource sender, String commandLabel, String[] args) {
        if (args.length == 1) {
            return getPlayers(server, sender);
        } else if (args.length == 2) {
            return Lists.newArrayList(ess.getSettings().getMinimumPayAmount().toString());
        } else {
            return Collections.emptyList();
        }
    }
}
