package com.earth2me.essentials.commands;

import com.earth2me.essentials.CommandSource;
import com.earth2me.essentials.User;
import com.earth2me.essentials.craftbukkit.SetExpFix;
import com.earth2me.essentials.utils.DateUtil;
import com.earth2me.essentials.utils.EnumUtil;
import com.earth2me.essentials.utils.NumberUtil;
import org.bukkit.Server;
import org.bukkit.Statistic;

import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import static com.earth2me.essentials.I18n.tl;

public class Commandwhois extends EssentialsCommand {
    // For some reason, in 1.13 PLAY_ONE_MINUTE = ticks played = what used to be PLAY_ONE_TICK
    // https://hub.spigotmc.org/stash/projects/SPIGOT/repos/bukkit/commits/b848d8ce633871b52115247b089029749c02f579
    private static final Statistic PLAY_ONE_TICK = EnumUtil.getStatistic("PLAY_ONE_MINUTE", "PLAY_ONE_TICK");

    public Commandwhois() {
        super("whois");
    }

    @Override
    public void run(final Server server, final CommandSource sender, final String commandLabel, final String[] args) throws Exception {
        if (args.length < 1) {
            throw new NotEnoughArgumentsException();
        }

        final User user = getPlayer(server, sender, args, 0);

        sender.sendMessage(tl("whoisTop", user.getName()));
        user.setDisplayNick();
        sender.sendMessage(tl("whoisNick", user.getDisplayName()));
        sender.sendMessage(tl("whoisJoined", DateUtil.formatDateDiff(user.getBase().getFirstPlayed()), new SimpleDateFormat("yyyy/MM/dd HH:mm").format(user.getBase().getFirstPlayed())));
        sender.sendMessage(tl("whoisUuid", user.getBase().getUniqueId().toString()));
        sender.sendMessage(tl("whoisHealth", user.getBase().getHealth()));
        sender.sendMessage(tl("whoisHunger", user.getBase().getFoodLevel(), user.getBase().getSaturation()));
        sender.sendMessage(tl("whoisExp", SetExpFix.getTotalExperience(user.getBase()), user.getBase().getLevel()));
        sender.sendMessage(tl("whoisLocation", user.getLocation().getWorld().getName(), user.getLocation().getBlockX(), user.getLocation().getBlockY(), user.getLocation().getBlockZ()));
        final long playtimeMs = System.currentTimeMillis() - (user.getBase().getStatistic(PLAY_ONE_TICK) * 50);
        sender.sendMessage(tl("whoisPlaytime", DateUtil.formatDateDiff(playtimeMs)));
        if (!ess.getSettings().isEcoDisabled()) {
            sender.sendMessage(tl("whoisMoney", NumberUtil.displayCurrency(user.getMoney(), ess)));
        }
        if (!sender.isPlayer() || ess.getUser(sender.getPlayer()).isAuthorized("essentials.whois.ip")) {
            sender.sendMessage(tl("whoisIPAddress", user.getBase().getAddress().getAddress().toString()));
        }
        final String location = user.getGeoLocation();
        if (location != null && (!sender.isPlayer() || ess.getUser(sender.getPlayer()).isAuthorized("essentials.geoip.show"))) {
            sender.sendMessage(tl("whoisGeoLocation", location));
        }
        sender.sendMessage(tl("whoisGamemode", tl(user.getBase().getGameMode().toString().toLowerCase(Locale.ENGLISH))));
        sender.sendMessage(tl("whoisGod", user.isGodModeEnabled() ? tl("true") : tl("false")));
        sender.sendMessage(tl("whoisOp", user.getBase().isOp() ? tl("true") : tl("false")));
        sender.sendMessage(tl("whoisFly", user.getBase().getAllowFlight() ? tl("true") : tl("false"), user.getBase().isFlying() ? tl("flying") : tl("notFlying")));
        sender.sendMessage(tl("whoisSpeed", user.getBase().isFlying() ? user.getBase().getFlySpeed() : user.getBase().getWalkSpeed()));
        if (user.isAfk()) {
            sender.sendMessage(tl("whoisAFKSince", tl("true"), DateUtil.formatDateDiff(user.getAfkSince())));
        } else {
            sender.sendMessage(tl("whoisAFK", tl("false")));
        }
        sender.sendMessage(tl("whoisJail", user.isJailed() ? user.getJailTimeout() > 0 ? user.getFormattedJailTime() : tl("true") : tl("false")));

        final long muteTimeout = user.getMuteTimeout();
        if (!user.hasMuteReason()) {
            sender.sendMessage(tl("whoisMuted", user.isMuted() ? muteTimeout > 0 ? DateUtil.formatDateDiff(muteTimeout) : tl("true") : tl("false")));
        } else {
            sender.sendMessage(tl("whoisMutedReason", user.isMuted() ? muteTimeout > 0 ? DateUtil.formatDateDiff(muteTimeout) : tl("true") : tl("false"),
                user.getMuteReason()));
        }
    }

    @Override
    protected List<String> getTabCompleteOptions(final Server server, final CommandSource sender, final String commandLabel, final String[] args) {
        if (args.length == 1) {
            return getPlayers(server, sender);
        } else {
            return Collections.emptyList();
        }
    }
}
