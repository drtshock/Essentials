package com.earth2me.essentials.commands;

import com.earth2me.essentials.Enchantments;
import com.earth2me.essentials.MetaItemStack;
import com.earth2me.essentials.User;
import com.earth2me.essentials.craftbukkit.InventoryWorkaround;
import com.earth2me.essentials.utils.StringUtil;
import com.google.common.collect.Lists;
import org.bukkit.Material;
import org.bukkit.Server;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;

import java.util.Collections;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;


public class Commandenchant extends EssentialsCommand {
    public Commandenchant() {
        super("enchant");
    }

    //TODO: Implement charge costs: final Trade charge = new Trade("enchant-" + enchantmentName, ess);
    @Override
    protected void run(final Server server, final User user, final String commandLabel, final String[] args) throws Exception {
        final ItemStack stack = user.getItemInHand();
        if (stack == null || stack.getType() == Material.AIR) {
            throw new Exception(user.tl("nothingInHand"));
        }
        if (args.length == 0) {
            final Set<String> enchantmentslist = new TreeSet<>();
            for (Map.Entry<String, Enchantment> entry : Enchantments.entrySet()) {
                final String enchantmentName = entry.getValue().getName().toLowerCase(Locale.ENGLISH);
                if (enchantmentslist.contains(enchantmentName) || (user.isAuthorized("essentials.enchantments." + enchantmentName) && entry.getValue().canEnchantItem(stack))) {
                    enchantmentslist.add(entry.getKey());
                    //enchantmentslist.add(enchantmentName);
                }
            }
            throw new NotEnoughArgumentsException(user.tl("enchantments", StringUtil.joinList(enchantmentslist.toArray())));
        }

        int level = 1;
        if (args.length > 1) {
            try {
                level = Integer.parseInt(args[1]);
            } catch (NumberFormatException ex) {
                level = -1;
            }
        }

        final boolean allowUnsafe = ess.getSettings().allowUnsafeEnchantments() && user.isAuthorized("essentials.enchantments.allowunsafe");

        final MetaItemStack metaStack = new MetaItemStack(stack);
        final Enchantment enchantment = metaStack.getEnchantment(user, args[0]);
        metaStack.addEnchantment(user.getSource(), allowUnsafe, enchantment, level);
        InventoryWorkaround.setItemInMainHand(user.getBase(), metaStack.getItemStack());

        user.getBase().updateInventory();
        final String enchantmentName = enchantment.getName().toLowerCase(Locale.ENGLISH);
        if (level == 0) {
            user.sendTl("enchantmentRemoved", enchantmentName.replace('_', ' '));
        } else {
            user.sendTl("enchantmentApplied", enchantmentName.replace('_', ' '));
        }
    }

    @Override
    protected List<String> getTabCompleteOptions(final Server server, final User user, final String commandLabel, final String[] args) {
        if (args.length == 1) {
            return new ArrayList<>(Enchantments.keySet());
        } else if (args.length == 2) {
            Enchantment enchantment = Enchantments.getByName(args[0]);
            if (enchantment == null) {
                return Collections.emptyList();
            }
            int min = enchantment.getStartLevel();
            int max = enchantment.getMaxLevel();
            List<String> options = Lists.newArrayList();
            for (int i = min; i <= max; i++) {
                options.add(Integer.toString(i));
            }
            return options;
        } else {
            return Collections.emptyList();
        }
    }
}
