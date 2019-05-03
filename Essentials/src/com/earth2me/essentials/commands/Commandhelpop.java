package com.earth2me.essentials.commands;

import com.earth2me.essentials.CommandSource;
import com.earth2me.essentials.Console;
import com.earth2me.essentials.User;
import com.earth2me.essentials.utils.FormatUtil;
import org.bukkit.Server;

import java.util.List;
import java.util.logging.Level;

import static com.earth2me.essentials.I18n.tl;


public class Commandhelpop extends EssentialsCommand {
    public Commandhelpop() {
        super("helpop");
    }

    @Override
    public void run(final Server server, final User user, final String commandLabel, final String[] args) throws Exception {
        user.setDisplayNick();
        final String message = sendMessage(server, user.getSource(), user.getDisplayName(), args);
        if (!user.isAuthorized("essentials.helpop.receive")) {
            user.sendMessage(message);
        }
    }

    @Override
    public void run(final Server server, final CommandSource sender, final String commandLabel, final String[] args) throws Exception {
        sendMessage(server, sender, Console.NAME, args);
    }

    private String sendMessage(final Server server, final CommandSource sender, final String from, final String[] args) throws Exception {
        if (args.length < 1) {
            throw new NotEnoughArgumentsException();
        }
        final String message = FormatUtil.stripFormat(getFinalArg(args, 0));
        ess.broadcastTl("essentials.helpop.receive", "helpOp", from, message);
        server.getLogger().log(Level.INFO, tl("helpOp", from, message));
        return sender.tl("helpOp", from, message);
    }

    @Override
    protected List<String> getTabCompleteOptions(Server server, CommandSource sender, String commandLabel, String[] args) {
        return null;  // Use vanilla handler for message
    }
}
