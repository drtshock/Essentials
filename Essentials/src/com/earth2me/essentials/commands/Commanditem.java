package com.earth2me.essentials.commands;

import com.earth2me.essentials.MetaItemStack;
import com.earth2me.essentials.User;
import com.earth2me.essentials.craftbukkit.InventoryWorkaround;
import com.google.common.collect.Lists;
import org.bukkit.Material;
import org.bukkit.Server;
import org.bukkit.inventory.ItemStack;

import java.util.Collections;
import java.util.List;
import java.util.Locale;


public class Commanditem extends EssentialsCommand {
    public Commanditem() {
        super("item");
    }

    @Override
    public void run(final Server server, final User user, final String commandLabel, final String[] args) throws Exception {
        if (args.length < 1) {
            throw new NotEnoughArgumentsException();
        }

        ItemStack stack = ess.getItemDb().get(args[0]);

        final String itemname = stack.getType().toString().toLowerCase(Locale.ENGLISH).replace("_", "");
        if (!user.canSpawnItem(stack.getType())) {
            throw new Exception(user.tl("cantSpawnItem", itemname));
        }

        try {
            if (args.length > 1 && Integer.parseInt(args[1]) > 0) {
                stack.setAmount(Integer.parseInt(args[1]));
            } else if (ess.getSettings().getDefaultStackSize() > 0) {
                stack.setAmount(ess.getSettings().getDefaultStackSize());
            } else if (ess.getSettings().getOversizedStackSize() > 0 && user.isAuthorized("essentials.oversizedstacks")) {
                stack.setAmount(ess.getSettings().getOversizedStackSize());
            }
        } catch (NumberFormatException e) {
            throw new NotEnoughArgumentsException();
        }

        MetaItemStack metaStack = new MetaItemStack(stack);
        if (!metaStack.canSpawn(ess)) {
            throw new Exception(user.tl("unableToSpawnItem", itemname));
        }

        if (args.length > 2) {
            final boolean allowUnsafe = ess.getSettings().allowUnsafeEnchantments() && user.isAuthorized("essentials.enchantments.allowunsafe");

            metaStack.parseStringMeta(user.getSource(), allowUnsafe, args, 2, ess);

            stack = metaStack.getItemStack();
        }


        if (stack.getType() == Material.AIR) {
            throw new Exception(user.tl("cantSpawnItem", "Air"));
        }

        final String displayName = stack.getType().toString().toLowerCase(Locale.ENGLISH).replace('_', ' ');
        user.sendTl("itemSpawn", stack.getAmount(), displayName);
        if (user.isAuthorized("essentials.oversizedstacks")) {
            InventoryWorkaround.addOversizedItems(user.getBase().getInventory(), ess.getSettings().getOversizedStackSize(), stack);
        } else {
            InventoryWorkaround.addItems(user.getBase().getInventory(), stack);
        }
        user.getBase().updateInventory();
    }

    @Override
    protected List<String> getTabCompleteOptions(final Server server, final User user, final String commandLabel, final String[] args) {
        if (args.length == 1) {
            return getItems();
        } else if (args.length == 2) {
            return Lists.newArrayList("1", "64");  // TODO: get actual max size
        } else if (args.length == 3) {
            return Lists.newArrayList("0");
        } else {
            return Collections.emptyList();
        }
    }
}
