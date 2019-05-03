package com.earth2me.essentials.commands;

import com.earth2me.essentials.CommandSource;
import com.earth2me.essentials.User;
import com.earth2me.essentials.utils.FormatUtil;
import org.bukkit.Server;
import org.bukkit.entity.Player;


public class Commandkickall extends EssentialsCommand {
    public Commandkickall() {
        super("kickall");
    }

    @Override
    public void run(final Server server, final CommandSource sender, final String commandLabel, final String[] args) throws Exception {
        for (Player onlinePlayer : ess.getOnlinePlayers()) {
            if (!sender.isPlayer() || !onlinePlayer.getName().equalsIgnoreCase(sender.getPlayer().getName())) {
                final User kuser = ess.getUser(onlinePlayer);
                if (!kuser.isAuthorized("essentials.kickall.exempt")) {
                    String kickReason = args.length > 0 ? getFinalArg(args, 0) : kuser.tl("kickDefault");
                    kickReason = FormatUtil.replaceFormat(kickReason.replace("\\n", "\n").replace("|", "\n"));
                    onlinePlayer.kickPlayer(kickReason);
                }
            }
        }
        sender.sendTl("kickedAll");
    }
}
