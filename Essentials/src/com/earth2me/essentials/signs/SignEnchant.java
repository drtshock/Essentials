package com.earth2me.essentials.signs;

import com.earth2me.essentials.ChargeException;
import com.earth2me.essentials.Enchantments;
import com.earth2me.essentials.Trade;
import com.earth2me.essentials.User;
import net.ess3.api.IEssentials;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;

import java.util.Locale;


public class SignEnchant extends EssentialsSign {
    public SignEnchant() {
        super("Enchant");
    }

    @Override
    protected boolean onSignCreate(final ISign sign, final User player, final String username, final IEssentials ess) throws SignException, ChargeException {
        final ItemStack stack;
        try {
            stack = sign.getLine(1).equals("*") || sign.getLine(1).equalsIgnoreCase("any") ? null : getItemStack(sign.getLine(1), 1, ess);
        } catch (SignException e) {
            sign.setLine(1, "§c<item|any>");
            throw e;
        }
        final String[] enchantLevel = sign.getLine(2).split(":");
        if (enchantLevel.length != 2) {
            sign.setLine(2, "§c<enchant>");
            throw new SignException(player.tl("invalidSignLine", 3));
        }
        final Enchantment enchantment = Enchantments.getByName(enchantLevel[0]);
        if (enchantment == null) {
            sign.setLine(2, "§c<enchant>");
            throw new SignException(player.tl("enchantmentNotFound"));
        }
        int level;
        try {
            level = Integer.parseInt(enchantLevel[1]);
        } catch (NumberFormatException ex) {
            sign.setLine(2, "§c<enchant>");
            throw new SignException(ex.getMessage(), ex);
        }
        final boolean allowUnsafe = ess.getSettings().allowUnsafeEnchantments() && player.isAuthorized("essentials.enchantments.allowunsafe") && player.isAuthorized("essentials.signs.enchant.allowunsafe");
        if (level < 0 || (!allowUnsafe && level > enchantment.getMaxLevel())) {
            level = enchantment.getMaxLevel();
            sign.setLine(2, enchantLevel[0] + ":" + level);
        }
        try {
            if (stack != null) {
                if (allowUnsafe) {
                    stack.addUnsafeEnchantment(enchantment, level);
                } else {
                    stack.addEnchantment(enchantment, level);
                }
            }
        } catch (Throwable ex) {
            throw new SignException(ex.getMessage(), ex);
        }
        getTrade(player, sign, 3, ess);
        return true;
    }

    @Override
    protected boolean onSignInteract(ISign sign, User player, String username, IEssentials ess) throws SignException, ChargeException {
        final ItemStack search = sign.getLine(1).equals("*") || sign.getLine(1).equalsIgnoreCase("any") ? null : getItemStack(sign.getLine(1), 1, ess);
        final Trade charge = getTrade(player, sign, 3, ess);
        charge.isAffordableFor(player);
        final String[] enchantLevel = sign.getLine(2).split(":");
        if (enchantLevel.length != 2) {
            throw new SignException(player.tl("invalidSignLine", 3));
        }
        final Enchantment enchantment = Enchantments.getByName(enchantLevel[0]);
        if (enchantment == null) {
            throw new SignException(player.tl("enchantmentNotFound"));
        }
        int level;
        try {
            level = Integer.parseInt(enchantLevel[1]);
        } catch (NumberFormatException ex) {
            level = enchantment.getMaxLevel();
        }

        final ItemStack playerHand = player.getBase().getItemInHand();
        if (playerHand == null || playerHand.getAmount() != 1 || (playerHand.containsEnchantment(enchantment) && playerHand.getEnchantmentLevel(enchantment) == level)) {
            throw new SignException(player.tl("missingItems", 1, sign.getLine(1)));
        }
        if (search != null && playerHand.getType() != search.getType()) {
            throw new SignException(player.tl("missingItems", 1, search.getType().toString().toLowerCase(Locale.ENGLISH).replace('_', ' ')));
        }

        final ItemStack toEnchant = playerHand;
        try {
            if (level == 0) {
                toEnchant.removeEnchantment(enchantment);
            } else {
                if (ess.getSettings().allowUnsafeEnchantments() && player.isAuthorized("essentials.signs.enchant.allowunsafe")) {
                    toEnchant.addUnsafeEnchantment(enchantment, level);
                } else {
                    toEnchant.addEnchantment(enchantment, level);
                }
            }
        } catch (Exception ex) {
            throw new SignException(ex.getMessage(), ex);
        }

        final String enchantmentName = enchantment.getName().toLowerCase(Locale.ENGLISH);
        if (level == 0) {
            player.sendTl("enchantmentRemoved", enchantmentName.replace('_', ' '));
        } else {
            player.sendTl("enchantmentApplied", enchantmentName.replace('_', ' '));
        }

        charge.charge(player);
        Trade.log("Sign", "Enchant", "Interact", username, charge, username, charge, sign.getBlock().getLocation(), ess);
        player.getBase().updateInventory();
        return true;
    }
}
