package com.earth2me.essentials.commands;

import com.earth2me.essentials.User;
import org.bukkit.Server;
import org.bukkit.inventory.ItemStack;

import java.util.Locale;


public class Commandmore extends EssentialsCommand {
    public Commandmore() {
        super("more");
    }

    @Override
    public void run(final Server server, final User user, final String commandLabel, final String[] args) throws Exception {
        final ItemStack stack = user.getItemInHand();
        if (stack == null) {
            throw new Exception(user.tl("cantSpawnItem", "Air"));
        }

        if (stack.getAmount() >= ((user.isAuthorized("essentials.oversizedstacks")) ? ess.getSettings().getOversizedStackSize() : stack.getMaxStackSize())) {
            throw new Exception(user.tl("fullStack"));
        }
        final String itemname = stack.getType().toString().toLowerCase(Locale.ENGLISH).replace("_", "");
        if (!user.canSpawnItem(stack.getType())) {
            throw new Exception(user.tl("cantSpawnItem", itemname));
        }
        if (user.isAuthorized("essentials.oversizedstacks")) {
            stack.setAmount(ess.getSettings().getOversizedStackSize());
        } else {
            stack.setAmount(stack.getMaxStackSize());
        }
        user.getBase().updateInventory();
    }
}