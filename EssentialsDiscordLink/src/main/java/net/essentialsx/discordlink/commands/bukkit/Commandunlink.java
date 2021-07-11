package net.essentialsx.discordlink.commands.bukkit;

import com.earth2me.essentials.User;
import com.earth2me.essentials.commands.EssentialsCommand;
import net.essentialsx.discordlink.AccountLinkManager;
import org.bukkit.Server;

import static com.earth2me.essentials.I18n.tl;

public class Commandunlink extends EssentialsCommand {
    public Commandunlink() {
        super("unlink");
    }

    @Override
    protected void run(Server server, User user, String commandLabel, String[] args) {
        final AccountLinkManager manager = (AccountLinkManager) module;
        if (!manager.removeAccount(user)) {
            user.sendMessage(tl("discordLinkNoAccount"));
            return;
        }

        user.sendMessage(tl("discordLinkUnlinked"));
    }
}
