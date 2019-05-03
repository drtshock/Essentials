package com.earth2me.essentials.commands;

import com.earth2me.essentials.User;
import com.earth2me.essentials.craftbukkit.InventoryWorkaround;
import com.google.common.collect.Lists;
import org.bukkit.Material;
import org.bukkit.Server;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

import java.util.Collections;
import java.util.List;


public class Commandhat extends EssentialsCommand {
    public Commandhat() {
        super("hat");
    }

    @Override
    protected void run(final Server server, final User user, final String commandLabel, final String[] args) throws Exception {
        if (args.length > 0 && (args[0].contains("rem") || args[0].contains("off") || args[0].equalsIgnoreCase("0"))) {
            final PlayerInventory inv = user.getBase().getInventory();
            final ItemStack head = inv.getHelmet();
            if (head == null || head.getType() == Material.AIR) {
                user.sendTl("hatEmpty");
            } else {
                final ItemStack air = new ItemStack(Material.AIR);
                inv.setHelmet(air);
                InventoryWorkaround.addItems(user.getBase().getInventory(), head);
                user.sendTl("hatRemoved");
            }
        } else {
            final ItemStack hand = user.getItemInHand();
            if (hand != null && hand.getType() != Material.AIR) {
                if (hand.getType().getMaxDurability() == 0) {
                    final PlayerInventory inv = user.getBase().getInventory();
                    final ItemStack head = inv.getHelmet();
                    inv.setHelmet(hand);
                    inv.setItemInHand(head);
                    user.sendTl("hatPlaced");
                } else {
                    user.sendTl("hatArmor");
                }
            } else {
                user.sendTl("hatFail");
            }
        }
    }

    @Override
    protected List<String> getTabCompleteOptions(final Server server, final User user, final String commandLabel, final String[] args) {
        if (args.length == 1) {
            return Lists.newArrayList("remove", "wear"); // "wear" isn't real
        } else {
            return Collections.emptyList();
        }
    }
}