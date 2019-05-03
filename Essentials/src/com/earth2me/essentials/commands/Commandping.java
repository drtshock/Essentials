package com.earth2me.essentials.commands;

import com.earth2me.essentials.CommandSource;
import com.earth2me.essentials.utils.FormatUtil;
import org.bukkit.Server;

// This command can be used to echo messages to the users screen, mostly useless but also an #EasterEgg
public class Commandping extends EssentialsCommand {
    public Commandping() {
        super("ping");
    }

    @Override
    public void run(final Server server, final CommandSource sender, final String commandLabel, final String[] args) throws Exception {
        if (args.length < 1) {

            sender.sendTl("pong");
        } else {
            sender.sendMessage(FormatUtil.replaceFormat(getFinalArg(args, 0)));
        }
    }
}
