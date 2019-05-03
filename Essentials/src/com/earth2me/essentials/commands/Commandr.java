package com.earth2me.essentials.commands;

import com.earth2me.essentials.CommandSource;
import com.earth2me.essentials.Console;
import com.earth2me.essentials.User;
import com.earth2me.essentials.messaging.IMessageRecipient;
import com.earth2me.essentials.utils.FormatUtil;
import org.bukkit.Server;


public class Commandr extends EssentialsCommand {
    public Commandr() {
        super("r");
    }

    @Override
    public void run(final Server server, final CommandSource sender, final String commandLabel, final String[] args) throws Exception {
        if (args.length < 1) {
            throw new NotEnoughArgumentsException();
        }

        String message = getFinalArg(args, 0);
        IMessageRecipient messageSender;

        if (sender.isPlayer()) {
            User user = ess.getUser(sender.getPlayer());

            if (user.isMuted()) {
                throw new Exception(user.hasMuteReason() ? user.tl("voiceSilencedReason", user.getMuteReason()) : user.tl("voiceSilenced"));
            }

            message = FormatUtil.formatMessage(user, "essentials.msg", message);
            messageSender = user;
        } else {
            message = FormatUtil.replaceFormat(message);
            messageSender = Console.getInstance();
        }

        final IMessageRecipient target = messageSender.getReplyRecipient();
        // Check to make sure the sender does have a quick-reply recipient
        if (target == null) {
            throw new Exception(sender.tl("foreverAlone"));
        }
        messageSender.sendMessage(target, message);
    }
}
