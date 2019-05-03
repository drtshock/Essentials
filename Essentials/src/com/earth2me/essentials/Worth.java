package com.earth2me.essentials;

import com.earth2me.essentials.commands.NotEnoughArgumentsException;
import com.earth2me.essentials.utils.VersionUtil;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.math.BigDecimal;
import java.util.Locale;

import static com.earth2me.essentials.I18n.tl;


public class Worth implements IConf {
    private final EssentialsConf config;

    public Worth(File dataFolder) {
        config = new EssentialsConf(new File(dataFolder, "worth.yml"));
        config.setTemplateName("/worth.yml");
        config.load();
    }

    /**
     * Get the value of an item stack from the config.
     *
     * @param ess       The Essentials instance.
     * @param itemStack The item stack to look up in the config.
     * @return The price from the config.
     */
    public BigDecimal getPrice(IEssentials ess, ItemStack itemStack) {
        BigDecimal result;

        String itemname = itemStack.getType().toString().toLowerCase(Locale.ENGLISH).replace("_", "");

        // Check for matches with data value from stack
        // Note that we always default to BigDecimal.ONE.negate(), equivalent to -1
        result = config.getBigDecimal("worth." + itemname + "." + itemStack.getDurability(), BigDecimal.ONE.negate());

        // Check for matches with data value 0
        if (result.signum() < 0) {
            final ConfigurationSection itemNameMatch = config.getConfigurationSection("worth." + itemname);
            if (itemNameMatch != null && itemNameMatch.getKeys(false).size() == 1) {
                result = config.getBigDecimal("worth." + itemname + ".0", BigDecimal.ONE.negate());
            }
        }

        // Check for matches with data value wildcard
        if (result.signum() < 0) {
            result = config.getBigDecimal("worth." + itemname + ".*", BigDecimal.ONE.negate());
        }

        // Check for matches with item name alone
        if (result.signum() < 0) {
            result = config.getBigDecimal("worth." + itemname, BigDecimal.ONE.negate());
        }

        if (result.signum() < 0) {
            return null;
        }
        return result;
    }

    /**
     * Get the amount of items to be sold from a player's inventory.
     *
     * @param ess        The Essentials instance.
     * @param user       The user attempting to sell the item.
     * @param is         A stack of the item to search the inventory for.
     * @param args       The amount to try to sell.
     * @param isBulkSell Whether or not to try and bulk sell all items.
     * @return The amount of items to sell from the player's inventory.
     * @throws Exception Thrown if trying to sell air or an invalid amount.
     */
    public int getAmount(IEssentials ess, User user, ItemStack is, String[] args, boolean isBulkSell) throws Exception {
        if (is == null || is.getType() == Material.AIR) {
            throw new Exception(user.tl("itemSellAir"));
        }

        int amount = 0;

        if (args.length > 1) {
            try {
                amount = Integer.parseInt(args[1].replaceAll("[^0-9]", ""));
            } catch (NumberFormatException ex) {
                throw new NotEnoughArgumentsException(ex);
            }
            if (args[1].startsWith("-")) {
                amount = -amount;
            }
        }

        boolean stack = args.length > 1 && args[1].endsWith("s");
        boolean requireStack = ess.getSettings().isTradeInStacks(is.getType());

        if (requireStack && !stack) {
            throw new Exception(user.tl("itemMustBeStacked"));
        }

        int max = 0;
        for (ItemStack s : user.getBase().getInventory().getContents()) {
            if (s == null || !s.isSimilar(is)) {
                continue;
            }
            max += s.getAmount();
        }

        if (stack) {
            amount *= is.getType().getMaxStackSize();
        }
        if (amount < 1) {
            amount += max;
        }

        if (requireStack) {
            amount -= amount % is.getType().getMaxStackSize();
        }
        if (amount > max || amount < 1) {
            if (!isBulkSell) {
                user.sendTl("itemNotEnough2");
                user.sendTl("itemNotEnough3");
                throw new Exception(user.tl("itemNotEnough1"));
            } else {
                return amount;
            }
        }

        return amount;
    }

    /**
     * Set the price of an item and save it to the config.
     *
     * @param ess       The Essentials instance.
     * @param itemStack A stack of the item to save.
     * @param price     The new price of the item.
     */
    public void setPrice(IEssentials ess, ItemStack itemStack, double price) {
        String path = "worth." + itemStack.getType().toString().toLowerCase(Locale.ENGLISH).replace("_", "");

        // Spigot 1.13+ throws an exception if a 1.13+ plugin even *attempts* to do set data.
        if (VersionUtil.getServerBukkitVersion().isLowerThan(VersionUtil.v1_13_0_R01) && itemStack.getType().getData() == null) {
            // Bukkit-bug: getDurability still contains the correct value, while getData().getData() is 0.
            path = path + "." + itemStack.getDurability();
        }

        config.setProperty(path, price);
        config.save();
    }

    @Override
    public void reloadConfig() {
        config.load();
    }
}
