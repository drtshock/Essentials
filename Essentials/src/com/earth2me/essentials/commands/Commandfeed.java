package com.earth2me.essentials.commands;

import com.earth2me.essentials.CommandSource;
import com.earth2me.essentials.User;
import org.bukkit.Server;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.FoodLevelChangeEvent;

import java.util.Collections;
import java.util.List;


public class Commandfeed extends EssentialsLoopCommand {
    public Commandfeed() {
        super("feed");
    }

    @Override
    protected void run(final Server server, final User user, final String commandLabel, final String[] args) throws Exception {
        if (!user.isAuthorized("essentials.feed.cooldown.bypass")) {
            user.healCooldown();
        }

        if (args.length > 0 && user.isAuthorized("essentials.feed.others")) {
            loopOnlinePlayers(server, user.getSource(), true, true, args[0], null);
            return;
        }

        feedPlayer(user.getBase());
        user.sendTl("feed");
    }

    @Override
    protected void run(final Server server, final CommandSource sender, final String commandLabel, final String[] args) throws Exception {
        if (args.length < 1) {
            throw new NotEnoughArgumentsException();
        }

        loopOnlinePlayers(server, sender, true, true, args[0], null);
    }

    @Override
    protected void updatePlayer(final Server server, final CommandSource sender, final User player, final String[] args) throws PlayerExemptException {
        try {
            feedPlayer(player.getBase());
            sender.sendTl("feedOther", player.getDisplayName());
        } catch (QuietAbortException e) {
            //Handle Quietly
        }
    }

    private void feedPlayer(final Player player) throws QuietAbortException {
        final int amount = 30;

        final FoodLevelChangeEvent flce = new FoodLevelChangeEvent(player, amount);
        ess.getServer().getPluginManager().callEvent(flce);
        if (flce.isCancelled()) {
            throw new QuietAbortException();
        }

        player.setFoodLevel(flce.getFoodLevel() > 20 ? 20 : flce.getFoodLevel());
        player.setSaturation(10);
        player.setExhaustion(0F);
    }

    @Override
    protected List<String> getTabCompleteOptions(Server server, User user, String commandLabel, String[] args) {
        if (args.length == 1 && user.isAuthorized("essentials.feed.others")) {
            return getPlayers(server, user);
        } else {
            return Collections.emptyList();
        }
    }

    @Override
    protected List<String> getTabCompleteOptions(Server server, CommandSource sender, String commandLabel, String[] args) {
        if (args.length == 1) {
            return getPlayers(server, sender);
        } else {
            return Collections.emptyList();
        }
    }
}
