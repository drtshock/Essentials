package com.earth2me.essentials.commands;

import com.earth2me.essentials.User;

import org.bukkit.Server;

public class Commandpayconfirmtoggle extends EssentialsCommand {

    public Commandpayconfirmtoggle() {
        super("payconfirmtoggle");
    }

    @Override
    public void run(Server server, User user, String commandLabel, String[] args) throws Exception {
        boolean confirmingPay = !user.isPromptingPayConfirm();
        if (commandLabel.contains("payconfirmon")) {
            confirmingPay = true;
        } else if (commandLabel.contains("payconfirmoff")) {
            confirmingPay = false;
        }
        user.setPromptingPayConfirm(confirmingPay);
        if (confirmingPay) {
            user.sendTl("payConfirmToggleOn");
        } else {
            user.sendTl("payConfirmToggleOff");
        }
        user.getConfirmingPayments().clear(); // Clear any outstanding confirmations.
    }
}

