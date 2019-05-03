package com.earth2me.essentials.commands;

import com.earth2me.essentials.CommandSource;
import com.earth2me.essentials.User;
import com.earth2me.essentials.utils.FormatUtil;
import org.bukkit.Server;

import static com.earth2me.essentials.I18n.tl;


public class Commandbroadcast extends EssentialsCommand {
    public Commandbroadcast() {
        super("broadcast");
    }

    @Override
    public void run(final Server server, final User user, final String commandLabel, final String[] args) throws Exception {
        sendBroadcast(user.getSource(), user.getDisplayName(), args);
    }

    @Override
    public void run(final Server server, final CommandSource sender, final String commandLabel, final String[] args) throws Exception {
        sendBroadcast(sender, sender.getSender().getName(), args);
    }

    private void sendBroadcast(final CommandSource sender, final String name, final String[] args) throws NotEnoughArgumentsException {
        if (args.length < 1) {
            throw new NotEnoughArgumentsException();
        }

        ess.broadcastTl(sender, "broadcast", FormatUtil.replaceFormat(getFinalArg(args, 0)).replace("\\n", "\n"), name);
    }
}
