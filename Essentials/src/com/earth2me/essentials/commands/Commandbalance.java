package com.earth2me.essentials.commands;

import com.earth2me.essentials.CommandSource;
import com.earth2me.essentials.User;
import com.earth2me.essentials.utils.NumberUtil;
import org.bukkit.Server;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;


public class Commandbalance extends EssentialsCommand {
    public Commandbalance() {
        super("balance");
    }

    @Override
    protected void run(final Server server, final CommandSource sender, final String commandLabel, final String[] args) throws Exception {
        if (args.length < 1) {
            throw new NotEnoughArgumentsException();
        }

        User target = getPlayer(server, args, 0, true, true);
        sender.sendTl("balanceOther", target.isHidden() ? target.getName() : target.getDisplayName(), NumberUtil.displayCurrency(target.getMoney(), ess));
    }

    @Override
    public void run(final Server server, final User user, final String commandLabel, final String[] args) throws Exception {
        if (args.length == 1 && user.isAuthorized("essentials.balance.others")) {
            final User target = getPlayer(server, args, 0, true, true);
            final BigDecimal bal = target.getMoney();
            user.sendTl("balanceOther", target.isHidden() ? target.getName() : target.getDisplayName(), NumberUtil.displayCurrency(bal, ess));
        } else if (args.length < 2) {
            final BigDecimal bal = user.getMoney();
            user.sendTl("balance", NumberUtil.displayCurrency(bal, ess));
        } else {
            throw new NotEnoughArgumentsException();
        }
    }

    @Override
    protected List<String> getTabCompleteOptions(Server server, User user, String commandLabel, String[] args) {
        if (args.length == 1 && user.isAuthorized("essentials.balance.others")) {
            return getPlayers(server, user);
        } else {
            return Collections.emptyList();
        }
    }

    @Override
    protected List<String> getTabCompleteOptions(Server server, CommandSource sender, String commandLabel, String[] args) {
        if (args.length == 1) {
            return getPlayers(server, sender);
        } else {
            return Collections.emptyList();
        }
    }
}
