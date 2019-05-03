package com.earth2me.essentials.signs;

import com.earth2me.essentials.ChargeException;
import com.earth2me.essentials.Trade;
import com.earth2me.essentials.Trade.OverflowType;
import com.earth2me.essentials.User;
import net.ess3.api.IEssentials;
import net.ess3.api.MaxMoneyException;

import org.bukkit.inventory.ItemStack;

import java.math.BigDecimal;


public class SignSell extends EssentialsSign {
    public SignSell() {
        super("Sell");
    }

    @Override
    protected boolean onSignCreate(final ISign sign, final User player, final String username, final IEssentials ess) throws SignException {
        validateTrade(sign, 1, 2, player, ess);
        validateTrade(sign, 3, player, ess);
        return true;
    }

    @Override
    protected boolean onSignInteract(final ISign sign, final User player, final String username, final IEssentials ess) throws SignException, ChargeException, MaxMoneyException {
        Trade charge = getTrade(sign, 1, 2, player, ess);
        Trade money = getTrade(player, sign, 3, ess);

        // Check if the player is trying to sell in bulk.
        if (ess.getSettings().isAllowBulkBuySell() && player.getBase().isSneaking()) {
            ItemStack heldItem = player.getItemInHand();
            if (charge.getItemStack().isSimilar(heldItem)) {
                int initialItemAmount = charge.getItemStack().getAmount();
                int newItemAmount = heldItem.getAmount();
                ItemStack item = charge.getItemStack();
                item.setAmount(newItemAmount);
                charge = new Trade(item, ess);

                BigDecimal chargeAmount = money.getMoney();
                BigDecimal pricePerSingleItem = chargeAmount.divide(new BigDecimal(initialItemAmount));
                pricePerSingleItem = pricePerSingleItem.multiply(new BigDecimal(newItemAmount));
                money = new Trade(pricePerSingleItem, ess);
            }
        }

        charge.isAffordableFor(player);
        money.pay(player, OverflowType.DROP);
        charge.charge(player);
        Trade.log("Sign", "Sell", "Interact", username, charge, username, money, sign.getBlock().getLocation(), ess);
        return true;
    }
}
