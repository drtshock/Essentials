package com.earth2me.essentials.commands;

import com.earth2me.essentials.User;

import org.bukkit.Server;

public class Commandpaytoggle extends EssentialsCommand {

    public Commandpaytoggle() {
        super("paytoggle");
    }

    @Override
    public void run(Server server, User user, String commandLabel, String[] args) throws Exception {
        boolean acceptingPay = !user.isAcceptingPay();
        if (commandLabel.contains("payon")) {
            acceptingPay = true;
        } else if (commandLabel.contains("payoff")) {
            acceptingPay = false;
        }
        user.setAcceptingPay(acceptingPay);
        if (acceptingPay) {
            user.sendTl("payToggleOn");
        } else {
            user.sendTl("payToggleOff");
        }
    }
}

