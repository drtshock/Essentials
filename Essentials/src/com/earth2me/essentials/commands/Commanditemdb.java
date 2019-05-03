package com.earth2me.essentials.commands;

import com.earth2me.essentials.CommandSource;
import com.earth2me.essentials.utils.VersionUtil;
import org.bukkit.Material;
import org.bukkit.Server;
import org.bukkit.inventory.ItemStack;

import java.util.Collections;
import java.util.List;


public class Commanditemdb extends EssentialsCommand {
    public Commanditemdb() {
        super("itemdb");
    }

    @Override
    protected void run(Server server, CommandSource sender, String commandLabel, String[] args) throws Exception {
        ItemStack itemStack = null;
        boolean itemHeld = false;
        if (args.length < 1) {
            if (sender.isPlayer() && sender.getPlayer() != null) {
                itemHeld = true;
                itemStack = ess.getUser(sender.getPlayer()).getItemInHand();
            }
            if (itemStack == null) {
                throw new NotEnoughArgumentsException();
            }
        } else {
            itemStack = ess.getItemDb().get(args[0]);
        }

        String itemId = "none";

        if (VersionUtil.getServerBukkitVersion().isLowerThan(VersionUtil.v1_13_0_R01)) {
            itemId = itemStack.getType().getId() + ":" + itemStack.getDurability();
        }

        sender.sendTl("itemType", itemStack.getType().toString(), itemId);

        // Don't send IDs twice
        if (!sender.tl("itemType").contains("{1}") && !itemId.equals("none")) {
            sender.sendTl("itemId", itemId);
        }

        if (itemHeld && itemStack.getType() != Material.AIR) {
            int maxuses = itemStack.getType().getMaxDurability();
            int durability = ((maxuses + 1) - itemStack.getDurability());
            if (maxuses != 0) {
                sender.sendTl("durability", Integer.toString(durability));
            }
        }
        final String itemNameList = ess.getItemDb().names(itemStack);
        if (itemNameList != null) {
            sender.sendTl("itemNames", ess.getItemDb().names(itemStack));
        }
    }

    @Override
    protected List<String> getTabCompleteOptions(Server server, CommandSource sender, String commandLabel, String[] args) {
        if (args.length == 1) {
            return getItems();
        } else {
            return Collections.emptyList();
        }
    }
}
