package com.earth2me.essentials.commands;

import com.earth2me.essentials.CommandSource;
import com.earth2me.essentials.User;
import com.google.common.collect.Lists;

import org.bukkit.Material;
import org.bukkit.Server;
import org.bukkit.entity.LightningStrike;

import java.util.Collections;
import java.util.List;
import java.util.Set;


public class Commandlightning extends EssentialsLoopCommand {
    int power = 5;

    public Commandlightning() {
        super("lightning");
    }

    @Override
    public void run(final Server server, final CommandSource sender, final String commandLabel, final String[] args) throws Exception {
        User user;
        if (sender.isPlayer()) {
            user = ess.getUser(sender.getPlayer());
            if ((args.length < 1 || user != null && !user.isAuthorized("essentials.lightning.others"))) {
                user.getWorld().strikeLightning(user.getBase().getTargetBlock((Set<Material>) null, 600).getLocation());
                return;
            }
        }

        if (args.length > 1) {
            try {
                power = Integer.parseInt(args[1]);
            } catch (NumberFormatException ex) {
            }
        }
        loopOnlinePlayers(server, sender, true, true, args[0], null);
    }

    @Override
    protected void updatePlayer(final Server server, final CommandSource sender, final User matchUser, final String[] args) {
        sender.sendTl("lightningUse", matchUser.getDisplayName());
        final LightningStrike strike = matchUser.getBase().getWorld().strikeLightningEffect(matchUser.getBase().getLocation());

        if (!matchUser.isGodModeEnabled()) {
            matchUser.getBase().damage(power, strike);
        }
        if (ess.getSettings().warnOnSmite()) {
            matchUser.sendTl("lightningSmited");
        }
    }

    @Override
    protected List<String> getTabCompleteOptions(final Server server, final User user, final String commandLabel, final String[] args) {
        if (!user.isAuthorized("essentials.lightning.others")) {
            // Can't use any params, including power
            return Collections.emptyList();
        } else {
            return super.getTabCompleteOptions(server, user, commandLabel, args);
        }
    }
    @Override
    protected List<String> getTabCompleteOptions(final Server server, final CommandSource sender, final String commandLabel, final String[] args) {
        if (args.length == 1) {
            return getPlayers(server, sender);
        } else if (args.length == 2) {
            return Lists.newArrayList(Integer.toString(this.power));
        } else {
            return Collections.emptyList();
        }
    }
}
