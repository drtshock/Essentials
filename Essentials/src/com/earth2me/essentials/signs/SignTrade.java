package com.earth2me.essentials.signs;

import com.earth2me.essentials.ChargeException;
import com.earth2me.essentials.Trade;
import com.earth2me.essentials.Trade.OverflowType;
import com.earth2me.essentials.Trade.TradeType;
import com.earth2me.essentials.User;
import com.earth2me.essentials.utils.NumberUtil;
import net.ess3.api.IEssentials;
import net.ess3.api.MaxMoneyException;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

import java.math.BigDecimal;
import java.util.Map;

//TODO: TL exceptions
public class SignTrade extends EssentialsSign {
    public SignTrade() {
        super("Trade");
    }

    @Override
    protected boolean onSignCreate(final ISign sign, final User player, final String username, final IEssentials ess) throws SignException, ChargeException {
        validateTrade(sign, 1, false, player, ess);
        validateTrade(sign, 2, true, player, ess);
        final Trade trade = getTrade(sign, 2, AmountType.ROUNDED, true, true, player, ess);
        final Trade charge = getTrade(sign, 1, AmountType.ROUNDED, false, true, player, ess);
        if (trade.getType() == charge.getType() && (trade.getType() != TradeType.ITEM || trade.getItemStack().isSimilar(charge.getItemStack()))) {
            throw new SignException("You cannot trade for the same item type.");
        }
        trade.isAffordableFor(player);
        sign.setLine(3, "§8" + username);
        trade.charge(player);
        Trade.log("Sign", "Trade", "Create", username, trade, username, null, sign.getBlock().getLocation(), ess);
        return true;
    }

    @Override
    protected boolean onSignInteract(final ISign sign, final User player, final String username, final IEssentials ess) throws SignException, ChargeException, MaxMoneyException {
        if (sign.getLine(3).substring(2).equalsIgnoreCase(username)) {
            final Trade store = rechargeSign(sign, ess, player);
            Trade stored;
            try {
                stored = getTrade(sign, 1, AmountType.TOTAL, true, true, player, ess);
                subtractAmount(sign, 1, stored, player, ess);

                Map<Integer, ItemStack> withdraw = stored.pay(player, OverflowType.RETURN);

                if (withdraw == null) {
                    Trade.log("Sign", "Trade", "Withdraw", username, store, username, null, sign.getBlock().getLocation(), ess);
                } else {
                    setAmount(sign, 1, BigDecimal.valueOf(withdraw.get(0).getAmount()), player, ess);
                    Trade.log("Sign", "Trade", "Withdraw", username, stored, username, new Trade(withdraw.get(0), ess), sign.getBlock().getLocation(), ess);
                }
            } catch (SignException e) {
                if (store == null) {
                    throw new SignException(player.tl("tradeSignEmptyOwner"), e);
                }
            }
            Trade.log("Sign", "Trade", "Deposit", username, store, username, null, sign.getBlock().getLocation(), ess);
        } else {
            final Trade charge = getTrade(sign, 1, AmountType.COST, false, true, player, ess);
            final Trade trade = getTrade(sign, 2, AmountType.COST, true, true, player, ess);
            charge.isAffordableFor(player);
            addAmount(sign, 1, charge, player, ess);
            subtractAmount(sign, 2, trade, player, ess);
            if (!trade.pay(player)) {
                subtractAmount(sign, 1, charge, player, ess);
                addAmount(sign, 2, trade, player, ess);
                throw new ChargeException("Full inventory");
            }
            charge.charge(player);
            Trade.log("Sign", "Trade", "Interact", sign.getLine(3), charge, username, trade, sign.getBlock().getLocation(), ess);
        }
        sign.updateSign();
        return true;
    }

    private Trade rechargeSign(final ISign sign, final IEssentials ess, final User player) throws SignException, ChargeException {
        final Trade trade = getTrade(sign, 2, AmountType.COST, false, true, player, ess);
        if (trade.getItemStack() != null && player.getBase().getItemInHand() != null && trade.getItemStack().getType() == player.getBase().getItemInHand().getType() && trade.getItemStack().getDurability() == player.getBase().getItemInHand().getDurability() && trade.getItemStack().getEnchantments().equals(player.getBase().getItemInHand().getEnchantments())) {
            int amount = player.getBase().getItemInHand().getAmount();
            amount -= amount % trade.getItemStack().getAmount();
            if (amount > 0) {
                final ItemStack stack = player.getBase().getItemInHand().clone();
                stack.setAmount(amount);
                final Trade store = new Trade(stack, ess);
                addAmount(sign, 2, store, player, ess);
                store.charge(player);
                return store;
            }
        }
        return null;
    }

    @Override
    protected boolean onSignBreak(final ISign sign, final User player, final String username, final IEssentials ess) throws SignException, MaxMoneyException {
        final String signOwner = sign.getLine(3);

        final boolean isOwner = (signOwner.length() > 3 && signOwner.substring(2).equalsIgnoreCase(username));
        final boolean canBreak = isOwner ? true : player.isAuthorized("essentials.signs.trade.override");
        final boolean canCollect = isOwner ? true : player.isAuthorized("essentials.signs.trade.override.collect");

        if (canBreak) {
            try {
                final Trade stored1 = getTrade(sign, 1, AmountType.TOTAL, false, true, player, ess);
                final Trade stored2 = getTrade(sign, 2, AmountType.TOTAL, false, true, player, ess);

                if (!canCollect) {
                    Trade.log("Sign", "Trade", "Destroy", signOwner, stored2, username, stored1, sign.getBlock().getLocation(), ess);
                    return true;
                }

                final Map<Integer, ItemStack> withdraw1 = stored1.pay(player, OverflowType.RETURN);
                final Map<Integer, ItemStack> withdraw2 = stored2.pay(player, OverflowType.RETURN);

                if (withdraw1 == null && withdraw2 == null) {
                    Trade.log("Sign", "Trade", "Break", signOwner, stored2, username, stored1, sign.getBlock().getLocation(), ess);
                    return true;
                }

                setAmount(sign, 1, BigDecimal.valueOf(withdraw1 == null ? 0L : withdraw1.get(0).getAmount()), player, ess);
                Trade.log("Sign", "Trade", "Withdraw", signOwner, stored1, username, withdraw1 == null ? null : new Trade(withdraw1.get(0), ess), sign.getBlock().getLocation(), ess);

                setAmount(sign, 2, BigDecimal.valueOf(withdraw2 == null ? 0L : withdraw2.get(0).getAmount()), player, ess);
                Trade.log("Sign", "Trade", "Withdraw", signOwner, stored2, username, withdraw2 == null ? null : new Trade(withdraw2.get(0), ess), sign.getBlock().getLocation(), ess);

                sign.updateSign();
            } catch (SignException e) {
                if (player.isAuthorized("essentials.signs.trade.override")) {
                    return true;
                }
                throw e;
            }
            return false;
        } else {
            return false;
        }
    }

    protected final void validateTrade(final ISign sign, final int index, final boolean amountNeeded, final User player, final IEssentials ess) throws SignException {
        final String line = sign.getLine(index).trim();
        if (line.isEmpty()) {
            throw new SignException("Empty line");
        }
        final String[] split = line.split("[ :]+");

        if (split.length == 1 && !amountNeeded) {
            final BigDecimal money = getMoney(player, split[0]);
            if (money != null) {
                if (NumberUtil.shortCurrency(money, ess).length() * 2 > 15) {
                    throw new SignException("Line can be too long!");
                }
                sign.setLine(index, NumberUtil.shortCurrency(money, ess) + ":0");
                return;
            }
        }

        if (split.length == 2 && amountNeeded) {
            final BigDecimal money = getMoney(player, split[0]);
            BigDecimal amount = getBigDecimalPositive(player, split[1]);
            if (money != null && amount != null) {
                amount = amount.subtract(amount.remainder(money));
                if (amount.compareTo(MINTRANSACTION) < 0 || money.compareTo(MINTRANSACTION) < 0) {
                    throw new SignException(player.tl("moreThanZero"));
                }
                sign.setLine(index, NumberUtil.shortCurrency(money, ess) + ":" + NumberUtil.shortCurrency(amount, ess).substring(1));
                return;
            }
        }

        if (split.length == 2 && !amountNeeded) {
            final int amount = getIntegerPositive(player, split[0]);

            if (amount < 1) {
                throw new SignException(player.tl("moreThanZero"));
            }
            if (!(split[1].equalsIgnoreCase("exp") || split[1].equalsIgnoreCase("xp")) && getItemStack(split[1], amount, ess).getType() == Material.AIR) {
                throw new SignException(player.tl("moreThanZero"));
            }
            String newline = amount + " " + split[1] + ":0";
            if ((newline + amount).length() > 15) {
                throw new SignException("Line can be too long!");
            }
            sign.setLine(index, newline);
            return;
        }

        if (split.length == 3 && amountNeeded) {
            final int stackamount = getIntegerPositive(player, split[0]);
            int amount = getIntegerPositive(player, split[2]);
            amount -= amount % stackamount;
            if (amount < 1 || stackamount < 1) {
                throw new SignException(player.tl("moreThanZero"));
            }
            if (!(split[1].equalsIgnoreCase("exp") || split[1].equalsIgnoreCase("xp")) && getItemStack(split[1], stackamount, ess).getType() == Material.AIR) {
                throw new SignException(player.tl("moreThanZero"));
            }
            sign.setLine(index, stackamount + " " + split[1] + ":" + amount);
            return;
        }
        throw new SignException(player.tl("invalidSignLine", index + 1));
    }

    protected final Trade getTrade(final ISign sign, final int index, final AmountType amountType, final boolean notEmpty, final User player, final IEssentials ess) throws SignException {
        return getTrade(sign, index, amountType, notEmpty, false, player, ess);
    }

    protected final Trade getTrade(final ISign sign, final int index, final AmountType amountType, final boolean notEmpty, final boolean allowId, final User player, final IEssentials ess) throws SignException {
        final String line = sign.getLine(index).trim();
        if (line.isEmpty()) {
            throw new SignException("Empty line");
        }
        final String[] split = line.split("[ :]+");

        if (split.length == 2) {
            try {
                final BigDecimal money = getMoney(player, split[0]);
                final BigDecimal amount = notEmpty ? getBigDecimalPositive(player, split[1]) : getBigDecimal(split[1]);
                if (money != null && amount != null) {
                    return new Trade(amountType == AmountType.COST ? money : amount, ess);
                }
            } catch (SignException e) {
                throw new SignException(player.tl("tradeSignEmpty"), e);
            }
        }

        if (split.length == 3) {
            if (split[1].equalsIgnoreCase("exp") || split[1].equalsIgnoreCase("xp")) {
                final int stackamount = getIntegerPositive(player, split[0]);
                int amount = getInteger(split[2]);
                if (amountType == AmountType.ROUNDED) {
                    amount -= amount % stackamount;
                }
                if (notEmpty && (amount < 1 || stackamount < 1)) {
                    throw new SignException(player.tl("tradeSignEmpty"));
                }
                return new Trade((amountType == AmountType.COST ? stackamount : amount), ess);
            } else {
                final int stackamount = getIntegerPositive(player, split[0]);
                final ItemStack item = getItemStack(split[1], stackamount, allowId, ess);
                int amount = getInteger(split[2]);
                if (amountType == AmountType.ROUNDED) {
                    amount -= amount % stackamount;
                }
                if (notEmpty && (amount < 1 || stackamount < 1 || item.getType() == Material.AIR || amount < stackamount)) {
                    throw new SignException(player.tl("tradeSignEmpty"));
                }
                item.setAmount(amountType == AmountType.COST ? stackamount : amount);
                return new Trade(item, ess);
            }
        }
        throw new SignException(player.tl("invalidSignLine", index + 1));
    }

    protected final void subtractAmount(final ISign sign, final int index, final Trade trade, final User player, final IEssentials ess) throws SignException {
        final BigDecimal money = trade.getMoney();
        if (money != null) {
            changeAmount(sign, index, money.negate(), player, ess);
        }
        final ItemStack item = trade.getItemStack();
        if (item != null) {
            changeAmount(sign, index, BigDecimal.valueOf(-item.getAmount()), player, ess);
        }
        final Integer exp = trade.getExperience();
        if (exp != null) {
            changeAmount(sign, index, BigDecimal.valueOf(-exp.intValue()), player, ess);
        }
    }

    protected final void addAmount(final ISign sign, final int index, final Trade trade, final User player, final IEssentials ess) throws SignException {
        final BigDecimal money = trade.getMoney();
        if (money != null) {
            changeAmount(sign, index, money, player, ess);
        }
        final ItemStack item = trade.getItemStack();
        if (item != null) {
            changeAmount(sign, index, BigDecimal.valueOf(item.getAmount()), player, ess);
        }
        final Integer exp = trade.getExperience();
        if (exp != null) {
            changeAmount(sign, index, BigDecimal.valueOf(exp.intValue()), player, ess);
        }
    }

    //TODO: Translate these exceptions.
    private void changeAmount(final ISign sign, final int index, final BigDecimal value, final User player, final IEssentials ess) throws SignException {
        final String line = sign.getLine(index).trim();
        if (line.isEmpty()) {
            throw new SignException("Empty line");
        }
        final String[] split = line.split("[ :]+");

        if (split.length == 2) {
            final BigDecimal amount = getBigDecimal(split[1]).add(value);
            setAmount(sign, index, amount, player, ess);
            return;
        }
        if (split.length == 3) {
            final BigDecimal amount = getBigDecimal(split[2]).add(value);
            setAmount(sign, index, amount, player, ess);
            return;
        }
        throw new SignException(player.tl("invalidSignLine", index + 1));
    }

    //TODO: Translate these exceptions.
    private void setAmount(final ISign sign, final int index, final BigDecimal value, final User player, final IEssentials ess) throws SignException {

        final String line = sign.getLine(index).trim();
        if (line.isEmpty()) {
            throw new SignException("Empty line");
        }
        final String[] split = line.split("[ :]+");

        if (split.length == 2) {
            final BigDecimal money = getMoney(player, split[0]);
            final BigDecimal amount = getBigDecimal(split[1]);
            if (money != null && amount != null) {
                final String newline = NumberUtil.shortCurrency(money, ess) + ":" + NumberUtil.shortCurrency(value, ess).substring(1);
                if (newline.length() > 15) {
                    throw new SignException("This sign is full: Line too long!");
                }
                sign.setLine(index, newline);
                return;
            }
        }

        if (split.length == 3) {
            if (split[1].equalsIgnoreCase("exp") || split[1].equalsIgnoreCase("xp")) {
                final int stackamount = getIntegerPositive(player, split[0]);
                final String newline = stackamount + " " + split[1] + ":" + (value.intValueExact());
                if (newline.length() > 15) {
                    throw new SignException("This sign is full: Line too long!");
                }
                sign.setLine(index, newline);
                return;
            } else {
                final int stackamount = getIntegerPositive(player, split[0]);
                getItemStack(split[1], stackamount, ess);
                final String newline = stackamount + " " + split[1] + ":" + (value.intValueExact());
                if (newline.length() > 15) {
                    throw new SignException("This sign is full: Line too long!");
                }
                sign.setLine(index, newline);
                return;
            }
        }
        throw new SignException(player.tl("invalidSignLine", index + 1));
    }


    public enum AmountType {
        TOTAL,
        ROUNDED,
        COST
    }
}
