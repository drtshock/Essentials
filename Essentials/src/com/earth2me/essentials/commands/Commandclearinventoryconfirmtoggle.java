package com.earth2me.essentials.commands;

import com.earth2me.essentials.User;

import org.bukkit.Server;

public class Commandclearinventoryconfirmtoggle extends EssentialsCommand {

    public Commandclearinventoryconfirmtoggle() {
        super("clearinventoryconfirmtoggle");
    }

    @Override
    public void run(Server server, User user, String commandLabel, String[] args) throws Exception {
        boolean confirmingClear = !user.isPromptingClearConfirm();
        if (commandLabel.toLowerCase().endsWith("on")) {
            confirmingClear = true;
        } else if (commandLabel.toLowerCase().endsWith("off")) {
            confirmingClear = false;
        }
        user.setPromptingClearConfirm(confirmingClear);
        if (confirmingClear) {
            user.sendTl("clearInventoryConfirmToggleOn");
        } else {
            user.sendTl("clearInventoryConfirmToggleOff");
        }
        user.setConfirmingClearCommand(null);
    }
}

