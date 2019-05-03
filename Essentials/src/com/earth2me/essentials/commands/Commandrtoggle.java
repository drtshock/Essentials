package com.earth2me.essentials.commands;

import com.earth2me.essentials.CommandSource;
import com.earth2me.essentials.User;
import org.bukkit.Server;

public class Commandrtoggle extends EssentialsToggleCommand {
    public Commandrtoggle() {
        super("rtoggle", "essentials.rtoggle.others");
    }

    @Override
    public void run(Server server, User user, String commandLabel, String[] args) throws Exception {
        handleToggleWithArgs(server, user, args);
    }

    @Override
    public void run(Server server, CommandSource sender, String commandLabel, String[] args) throws Exception {
        toggleOtherPlayers(server, sender, args);
    }

    @Override
    void togglePlayer(CommandSource sender, User user, Boolean enabled) throws NotEnoughArgumentsException {
        if (enabled == null) {
            enabled = !user.isLastMessageReplyRecipient();
        }

        user.setLastMessageReplyRecipient(enabled);

        user.sendTl(!enabled ? "replyLastRecipientDisabled" : "replyLastRecipientEnabled");
        if (!sender.isPlayer() || !user.getBase().equals(sender.getPlayer())) {
            sender.sendTl(!enabled ? "replyLastRecipientDisabledFor" : "replyLastRecipientEnabledFor", user.getDisplayName());
        }
    }
}
