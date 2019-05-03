package com.earth2me.essentials.commands;

import com.earth2me.essentials.ChargeException;
import com.earth2me.essentials.Trade;
import com.earth2me.essentials.User;
import com.earth2me.essentials.utils.StringUtil;
import com.earth2me.essentials.utils.VersionUtil;
import com.google.common.collect.Lists;
import net.ess3.api.IUser;
import org.bukkit.Material;
import org.bukkit.Server;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;


public class Commandrepair extends EssentialsCommand {
    public Commandrepair() {
        super("repair");
    }

    @Override
    public void run(final Server server, final User user, final String commandLabel, final String[] args) throws Exception {
        if (args.length < 1 || args[0].equalsIgnoreCase("hand") || !user.isAuthorized("essentials.repair.all")) {
            repairHand(user);
        } else if (args[0].equalsIgnoreCase("all")) {
            final Trade charge = new Trade("repair-all", ess);
            charge.isAffordableFor(user);
            repairAll(user);
            charge.charge(user);
        } else {
            throw new NotEnoughArgumentsException();
        }
    }

    public void repairHand(User user) throws Exception {
        final ItemStack item = user.getItemInHand();
        if (item == null || item.getType().isBlock() || item.getDurability() == 0) {
            throw new Exception(user.tl("repairInvalidType"));
        }

        if (!item.getEnchantments().isEmpty() && !ess.getSettings().getRepairEnchanted() && !user.isAuthorized("essentials.repair.enchanted")) {
            throw new Exception(user.tl("repairEnchanted"));
        }

        final String itemName = item.getType().toString().toLowerCase(Locale.ENGLISH);
        final Trade charge = getCharge(item.getType());

        charge.isAffordableFor(user);

        repairItem(user, item);

        charge.charge(user);
        user.getBase().updateInventory();
        user.sendTl("repair", itemName.replace('_', ' '));
    }

    public void repairAll(User user) throws Exception {
        final List<String> repaired = new ArrayList<>();
        repairItems(user.getBase().getInventory().getContents(), user, repaired);

        if (user.isAuthorized("essentials.repair.armor")) {
            repairItems(user.getBase().getInventory().getArmorContents(), user, repaired);
        }

        user.getBase().updateInventory();
        if (repaired.isEmpty()) {
            throw new Exception(user.tl("repairNone"));
        } else {
            user.sendTl("repair", StringUtil.joinList(repaired));
        }
    }

    private void repairItem(IUser user, final ItemStack item) throws Exception {
        final Material material = item.getType();
        if (material.isBlock() || material.getMaxDurability() < 1) {
            throw new Exception(user.tl("repairInvalidType"));
        }

        if (item.getDurability() == 0) {
            throw new Exception(user.tl("repairAlreadyFixed"));
        }

        item.setDurability((short) 0);
    }

    private void repairItems(final ItemStack[] items, final IUser user, final List<String> repaired) throws Exception {
        for (ItemStack item : items) {
            if (item == null || item.getType().isBlock() || item.getDurability() == 0) {
                continue;
            }

            final String itemName = item.getType().toString().toLowerCase(Locale.ENGLISH);
            final Trade charge = getCharge(item.getType());

            try {
                charge.isAffordableFor(user);
            } catch (ChargeException ex) {
                user.sendMessage(ex.getMessage());
                continue;
            }
            if (!item.getEnchantments().isEmpty() && !ess.getSettings().getRepairEnchanted() && !user.isAuthorized("essentials.repair.enchanted")) {
                continue;
            }

            try {
                repairItem(user, item);
            } catch (Exception e) {
                continue;
            }
            try {
                charge.charge(user);
            } catch (ChargeException ex) {
                user.sendMessage(ex.getMessage());
            }
            repaired.add(itemName.replace('_', ' '));
        }
    }

    private Trade getCharge(final Material material) {
        final String itemName = material.toString().toLowerCase(Locale.ENGLISH);
        if (VersionUtil.getServerBukkitVersion().isLowerThan(VersionUtil.v1_13_0_R01)) {
            final int itemId = material.getId();
            return new Trade("repair-" + itemName.replace('_', '-'), new Trade("repair-" + itemId, new Trade("repair-item", ess), ess), ess);
        } else {
            return new Trade("repair-" + itemName.replace('_', '-'), new Trade("repair-item", ess), ess);
        }
    }

    @Override
    protected List<String> getTabCompleteOptions(Server server, User user, String commandLabel, String[] args) {
        if (args.length == 1) {
            List<String> options = Lists.newArrayList("hand");
            if (user.isAuthorized("essentials.repair.all")) {
                options.add("all");
            }
            return options;
        } else {
            return Collections.emptyList();
        }
    }
}
