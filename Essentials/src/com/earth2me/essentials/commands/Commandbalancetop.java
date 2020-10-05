package com.earth2me.essentials.commands;

import com.earth2me.essentials.CommandSource;
import com.earth2me.essentials.textreader.SimpleTextInput;
import com.earth2me.essentials.textreader.TextPager;
import com.earth2me.essentials.utils.NumberUtil;
import com.google.common.collect.Lists;
import org.bukkit.Server;

import java.math.BigDecimal;
import java.text.DateFormat;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import static com.earth2me.essentials.I18n.tl;

public class Commandbalancetop extends EssentialsCommand {
    public static final int MINUSERS = 50;
    private static final int CACHETIME = 2 * 60 * 1000;
    private static final SimpleTextInput cache = new SimpleTextInput();
    private static final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
    private static long cacheage = 0;

    public Commandbalancetop() {
        super("balancetop");
    }

    private static void outputCache(final CommandSource sender, final int page) {
        final Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(cacheage);
        final DateFormat format = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT);
        sender.sendMessage(tl("balanceTop", format.format(cal.getTime())));
        new TextPager(cache).showPage(Integer.toString(page), null, "balancetop", sender);
    }

    @Override
    protected void run(final Server server, final CommandSource sender, final String commandLabel, final String[] args) throws Exception {
        int page = 0;
        boolean force = false;
        if (args.length > 0) {
            try {
                page = Integer.parseInt(args[0]);
            } catch (final NumberFormatException ex) {
                if (args[0].equalsIgnoreCase("force") && (!sender.isPlayer() || ess.getUser(sender.getPlayer()).isAuthorized("essentials.balancetop.force"))) {
                    force = true;
                }
            }
        }

        if (!force && lock.readLock().tryLock()) {
            try {
                if (cacheage > System.currentTimeMillis() - CACHETIME) {
                    outputCache(sender, page);
                    return;
                }
                if (ess.getUserMap().getUniqueUsers() > MINUSERS) {
                    sender.sendMessage(tl("orderBalances", ess.getUserMap().getUniqueUsers()));
                }
            } finally {
                lock.readLock().unlock();
            }
        } else {
            if (ess.getUserMap().getUniqueUsers() > MINUSERS) {
                sender.sendMessage(tl("orderBalances", ess.getUserMap().getUniqueUsers()));
            }
        }
        ess.runTaskAsynchronously(new Viewer(sender, commandLabel, page, force));

    }

    @Override
    protected List<String> getTabCompleteOptions(final Server server, final CommandSource sender, final String commandLabel, final String[] args) {
        if (args.length == 1) {
            final List<String> options = Lists.newArrayList("1");
            if (!sender.isPlayer() || ess.getUser(sender.getPlayer()).isAuthorized("essentials.balancetop.force")) {
                options.add("force");
            }
            return options;
        } else {
            return Collections.emptyList();
        }
    }

    private class Calculator implements Runnable {
        private final transient Viewer viewer;
        private final boolean force;

        Calculator(final Viewer viewer, final boolean force) {
            this.viewer = viewer;
            this.force = force;
        }

        @Override
        public void run() {
            lock.writeLock().lock();
            try {
                if (force || cacheage <= System.currentTimeMillis() - CACHETIME) {
                    if (ess.getSettings().isEcoDisabled()) {
                        if (ess.getSettings().isDebug()) {
                            ess.getLogger().info("Internal economy functions disabled, aborting baltop.");
                        }
                    } else {
                        viewer.getSender().calculateBalanceTopMap(ess);
                    }

                    cache.getLines().add(tl("serverTotal", NumberUtil.displayCurrency(ess.getBalanceTop().getBalanceTopTotal(), ess)));
                    int pos = 1;
                    for (Map.Entry<String, BigDecimal> entry : ess.getBalanceTop().getBalanceTopCache()) {
                        cache.getLines().add(tl("balanceTopLine", pos, entry.getKey(), NumberUtil.displayCurrency(entry.getValue(), ess)));
                        pos++;
                    }
                    cacheage = System.currentTimeMillis();
                }
            } finally {
                lock.writeLock().unlock();
            }
            ess.runTaskAsynchronously(viewer);
        }
    }

    private class Viewer implements Runnable {
        private final transient CommandSource sender;
        private final transient int page;
        private final transient boolean force;
        private final transient String commandLabel;

        Viewer(final CommandSource sender, final String commandLabel, final int page, final boolean force) {
            this.sender = sender;
            this.page = page;
            this.force = force;
            this.commandLabel = commandLabel;
        }

        @Override
        public void run() {
            lock.readLock().lock();
            try {
                if (!force && cacheage > System.currentTimeMillis() - CACHETIME) {
                    outputCache(sender, page);
                    return;
                }
            } finally {
                lock.readLock().unlock();
            }
            ess.runTaskAsynchronously(new Calculator(new Viewer(sender, commandLabel, page, false), force));
        }

        public CommandSource getSender() {
            return sender;
        }
    }
}
