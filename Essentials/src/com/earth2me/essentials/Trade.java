package com.earth2me.essentials;

import com.earth2me.essentials.craftbukkit.InventoryWorkaround;
import com.earth2me.essentials.craftbukkit.SetExpFix;
import com.earth2me.essentials.utils.NumberUtil;
import net.ess3.api.IEssentials;
import net.ess3.api.IUser;
import net.ess3.api.MaxMoneyException;
import org.bukkit.Location;
import org.bukkit.entity.Item;
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.math.BigDecimal;
import java.text.DateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import static com.earth2me.essentials.I18n.tl;


public class Trade {
    private final transient String command;
    private final transient Trade fallbackTrade;
    private final transient BigDecimal money;
    private final transient ItemStack itemStack;
    private final transient Integer exp;
    private final transient IEssentials ess;


    public enum TradeType {
        MONEY,
        EXP,
        ITEM
    }


    public enum OverflowType {
        ABORT,
        DROP,
        RETURN
    }

    public Trade(final String command, final IEssentials ess) {
        this(command, null, null, null, null, ess);
    }

    public Trade(final String command, final Trade fallback, final IEssentials ess) {
        this(command, fallback, null, null, null, ess);
    }

    @Deprecated
    public Trade(final double money, final com.earth2me.essentials.IEssentials ess) {
        this(null, null, BigDecimal.valueOf(money), null, null, (IEssentials) ess);
    }

    public Trade(final BigDecimal money, final IEssentials ess) {
        this(null, null, money, null, null, ess);
    }

    public Trade(final ItemStack items, final IEssentials ess) {
        this(null, null, null, items, null, ess);
    }

    public Trade(final int exp, final IEssentials ess) {
        this(null, null, null, null, exp, ess);
    }

    private Trade(final String command, final Trade fallback, final BigDecimal money, final ItemStack item, final Integer exp, final IEssentials ess) {
        this.command = command;
        this.fallbackTrade = fallback;
        this.money = money;
        this.itemStack = item;
        this.exp = exp;
        this.ess = ess;
    }

    public void isAffordableFor(final IUser user) throws ChargeException {

        if (ess.getSettings().isDebug()) {
            ess.getLogger().log(Level.INFO, "checking if " + user.getName() + " can afford charge.");
        }

        if (getMoney() != null && getMoney().signum() > 0 && !user.canAfford(getMoney())) {
            throw new ChargeException(user.tl("notEnoughMoney", NumberUtil.displayCurrency(getMoney(), ess)));
        }

        if (getItemStack() != null && !user.getBase().getInventory().containsAtLeast(itemStack, itemStack.getAmount())) {
            throw new ChargeException(user.tl("missingItems", getItemStack().getAmount(), ess.getItemDb().name(getItemStack())));
        }

        BigDecimal money;
        if (command != null && !command.isEmpty() && (money = getCommandCost(user)).signum() > 0 && !user.canAfford(money)) {
            throw new ChargeException(user.tl("notEnoughMoney", NumberUtil.displayCurrency(money, ess)));
        }

        if (exp != null && exp > 0 && SetExpFix.getTotalExperience(user.getBase()) < exp) {
            throw new ChargeException(user.tl("notEnoughExperience"));
        }
    }

    public boolean pay(final IUser user) throws MaxMoneyException {
        return pay(user, OverflowType.ABORT) == null;
    }

    public Map<Integer, ItemStack> pay(final IUser user, final OverflowType type) throws MaxMoneyException {
        if (getMoney() != null && getMoney().signum() > 0) {
            if (ess.getSettings().isDebug()) {
                ess.getLogger().log(Level.INFO, "paying user " + user.getName() + " via trade " + getMoney().toPlainString());
            }
            user.giveMoney(getMoney());
        }
        if (getItemStack() != null) {
            // This stores the would be overflow
            Map<Integer, ItemStack> overFlow = InventoryWorkaround.addAllItems(user.getBase().getInventory(), getItemStack());

            if (overFlow != null) {
                switch (type) {
                    case ABORT:
                        if (ess.getSettings().isDebug()) {
                            ess.getLogger().log(Level.INFO, "abort paying " + user.getName() + " itemstack " + getItemStack().toString() + " due to lack of inventory space ");
                        }

                        return overFlow;

                    case RETURN:
                        // Pay the user the items, and return overflow
                        final Map<Integer, ItemStack> returnStack = InventoryWorkaround.addItems(user.getBase().getInventory(), getItemStack());
                        user.getBase().updateInventory();

                        if (ess.getSettings().isDebug()) {
                            ess.getLogger().log(Level.INFO, "paying " + user.getName() + " partial itemstack " + getItemStack().toString() + " with overflow " + returnStack.get(0).toString());
                        }

                        return returnStack;

                    case DROP:
                        // Pay the users the items directly, and drop the rest, will always return no overflow.
                        final Map<Integer, ItemStack> leftOver = InventoryWorkaround.addItems(user.getBase().getInventory(), getItemStack());
                        final Location loc = user.getBase().getLocation();
                        for (ItemStack loStack : leftOver.values()) {
                            final int maxStackSize = loStack.getType().getMaxStackSize();
                            final int stacks = loStack.getAmount() / maxStackSize;
                            final int leftover = loStack.getAmount() % maxStackSize;
                            final Item[] itemStacks = new Item[stacks + (leftover > 0 ? 1 : 0)];
                            for (int i = 0; i < stacks; i++) {
                                final ItemStack stack = loStack.clone();
                                stack.setAmount(maxStackSize);
                                itemStacks[i] = loc.getWorld().dropItem(loc, stack);
                            }
                            if (leftover > 0) {
                                final ItemStack stack = loStack.clone();
                                stack.setAmount(leftover);
                                itemStacks[stacks] = loc.getWorld().dropItem(loc, stack);
                            }
                        }
                        if (ess.getSettings().isDebug()) {
                            ess.getLogger().log(Level.INFO, "paying " + user.getName() + " partial itemstack " + getItemStack().toString() + " and dropping overflow " + leftOver.get(0).toString());
                        }
                }
            } else if (ess.getSettings().isDebug()) {
                ess.getLogger().log(Level.INFO, "paying " + user.getName() + " itemstack " + getItemStack().toString());
            }
            user.getBase().updateInventory();
        }
        if (getExperience() != null) {
            SetExpFix.setTotalExperience(user.getBase(), SetExpFix.getTotalExperience(user.getBase()) + getExperience());
        }
        return null;
    }

    public void charge(final IUser user) throws ChargeException {
        if (ess.getSettings().isDebug()) {
            ess.getLogger().log(Level.INFO, "attempting to charge user " + user.getName());
        }
        if (getMoney() != null) {
            if (ess.getSettings().isDebug()) {
                ess.getLogger().log(Level.INFO, "charging user " + user.getName() + " money " + getMoney().toPlainString());
            }
            if (!user.canAfford(getMoney()) && getMoney().signum() > 0) {
                throw new ChargeException(user.tl("notEnoughMoney", NumberUtil.displayCurrency(getMoney(), ess)));
            }
            user.takeMoney(getMoney());
        }
        if (getItemStack() != null) {
            if (ess.getSettings().isDebug()) {
                ess.getLogger().log(Level.INFO, "charging user " + user.getName() + " itemstack " + getItemStack().toString());
            }
            if (!user.getBase().getInventory().containsAtLeast(getItemStack(), getItemStack().getAmount())) {
                throw new ChargeException(user.tl("missingItems", getItemStack().getAmount(), getItemStack().getType().toString().toLowerCase(Locale.ENGLISH).replace("_", " ")));
            }
            user.getBase().getInventory().removeItem(getItemStack());
            user.getBase().updateInventory();
        }
        if (command != null) {
            final BigDecimal cost = getCommandCost(user);
            if (!user.canAfford(cost) && cost.signum() > 0) {
                throw new ChargeException(user.tl("notEnoughMoney", NumberUtil.displayCurrency(cost, ess)));
            }
            user.takeMoney(cost);
        }
        if (getExperience() != null) {
            if (ess.getSettings().isDebug()) {
                ess.getLogger().log(Level.INFO, "charging user " + user.getName() + " exp " + getExperience());
            }
            final int experience = SetExpFix.getTotalExperience(user.getBase());
            if (experience < getExperience() && getExperience() > 0) {
                throw new ChargeException(user.tl("notEnoughExperience"));
            }
            SetExpFix.setTotalExperience(user.getBase(), experience - getExperience());
        }
        if (ess.getSettings().isDebug()) {
            ess.getLogger().log(Level.INFO, "charge user " + user.getName() + " completed");
        }
    }

    public BigDecimal getMoney() {
        return money;
    }

    public ItemStack getItemStack() {
        return itemStack;
    }

    public Integer getExperience() {
        return exp;
    }

    public TradeType getType() {
        if (getExperience() != null) {
            return TradeType.EXP;
        }

        if (getItemStack() != null) {
            return TradeType.ITEM;
        }

        return TradeType.MONEY;
    }

    public BigDecimal getCommandCost(final IUser user) {
        BigDecimal cost = BigDecimal.ZERO;
        if (command != null && !command.isEmpty()) {
            cost = ess.getSettings().getCommandCost(command.charAt(0) == '/' ? command.substring(1) : command);
            if (cost.signum() == 0 && fallbackTrade != null) {
                cost = fallbackTrade.getCommandCost(user);
            }

            if (ess.getSettings().isDebug()) {
                ess.getLogger().log(Level.INFO, "calculated command (" + command + ") cost for " + user.getName() + " as " + cost);
            }
        }
        if (cost.signum() != 0 && (user.isAuthorized("essentials.nocommandcost.all") || user.isAuthorized("essentials.nocommandcost." + command))) {
            return BigDecimal.ZERO;
        }
        return cost;
    }

    private static FileWriter fw = null;

    public static void log(String type, String subtype, String event, String sender, Trade charge, String receiver, Trade pay, Location loc, IEssentials ess) {
        //isEcoLogUpdateEnabled() - This refers to log entries with no location, ie API updates #EasterEgg
        //isEcoLogEnabled() - This refers to log entries with with location, ie /pay /sell and eco signs.

        if ((loc == null && !ess.getSettings().isEcoLogUpdateEnabled()) || (loc != null && !ess.getSettings().isEcoLogEnabled())) {
            return;
        }
        if (fw == null) {
            try {
                fw = new FileWriter(new File(ess.getDataFolder(), "trade.log"), true);
            } catch (IOException ex) {
                Logger.getLogger("Essentials").log(Level.SEVERE, null, ex);
            }
        }
        StringBuilder sb = new StringBuilder();
        sb.append(type).append(",").append(subtype).append(",").append(event).append(",\"");
        sb.append(DateFormat.getDateTimeInstance(DateFormat.FULL, DateFormat.FULL).format(new Date()));
        sb.append("\",\"");
        if (sender != null) {
            sb.append(sender);
        }
        sb.append("\",");
        if (charge == null) {
            sb.append("\"\",\"\",\"\"");
        } else {
            if (charge.getItemStack() != null) {
                sb.append(charge.getItemStack().getAmount()).append(",");
                sb.append(charge.getItemStack().getType().toString()).append(",");
                sb.append(charge.getItemStack().getDurability());
            }
            if (charge.getMoney() != null) {
                sb.append(charge.getMoney()).append(",");
                sb.append("money").append(",");
                sb.append(ess.getSettings().getCurrencySymbol());
            }
            if (charge.getExperience() != null) {
                sb.append(charge.getExperience()).append(",");
                sb.append("exp").append(",");
                sb.append("\"\"");
            }
        }
        sb.append(",\"");
        if (receiver != null) {
            sb.append(receiver);
        }
        sb.append("\",");
        if (pay == null) {
            sb.append("\"\",\"\",\"\"");
        } else {
            if (pay.getItemStack() != null) {
                sb.append(pay.getItemStack().getAmount()).append(",");
                sb.append(pay.getItemStack().getType().toString()).append(",");
                sb.append(pay.getItemStack().getDurability());
            }
            if (pay.getMoney() != null) {
                sb.append(pay.getMoney()).append(",");
                sb.append("money").append(",");
                sb.append(ess.getSettings().getCurrencySymbol());
            }
            if (pay.getExperience() != null) {
                sb.append(pay.getExperience()).append(",");
                sb.append("exp").append(",");
                sb.append("\"\"");
            }
        }
        if (loc == null) {
            sb.append(",\"\",\"\",\"\",\"\"");
        } else {
            sb.append(",\"");
            sb.append(loc.getWorld().getName()).append("\",");
            sb.append(loc.getBlockX()).append(",");
            sb.append(loc.getBlockY()).append(",");
            sb.append(loc.getBlockZ()).append(",");
        }
        sb.append("\n");
        try {
            fw.write(sb.toString());
            fw.flush();
        } catch (IOException ex) {
            Logger.getLogger("Essentials").log(Level.SEVERE, null, ex);
        }
    }

    public static void closeLog() {
        if (fw != null) {
            try {
                fw.close();
            } catch (IOException ex) {
                Logger.getLogger("Essentials").log(Level.SEVERE, null, ex);
            }
            fw = null;
        }
    }
}
