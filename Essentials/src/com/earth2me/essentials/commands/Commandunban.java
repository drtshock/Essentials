package com.earth2me.essentials.commands;

import com.earth2me.essentials.CommandSource;
import com.earth2me.essentials.Console;
import com.earth2me.essentials.User;
import org.bukkit.BanList;
import org.bukkit.OfflinePlayer;
import org.bukkit.Server;

import java.util.logging.Level;


public class Commandunban extends EssentialsCommand {
    public Commandunban() {
        super("unban");
    }

    @Override
    public void run(final Server server, final CommandSource sender, final String commandLabel, final String[] args) throws Exception {
        if (args.length < 1) {
            throw new NotEnoughArgumentsException();
        }
        String name;
        try {
            final User user = getPlayer(server, args, 0, true, true);
            name = user.getName();
            ess.getServer().getBanList(BanList.Type.NAME).pardon(name);
        } catch (NoSuchFieldException e) {
            final OfflinePlayer player = server.getOfflinePlayer(args[0]);
            name = player.getName();
            if (!player.isBanned()) {
                throw new Exception(sender.tl("playerNotFound"), e);
            }
            ess.getServer().getBanList(BanList.Type.NAME).pardon(name);
        }

        final String senderName = sender.isPlayer() ? sender.getPlayer().getDisplayName() : Console.NAME;
        server.getLogger().log(Level.INFO, sender.tl("playerUnbanned", senderName, name));

        ess.broadcastMessage("essentials.ban.notify", sender.tl("playerUnbanned", senderName, name));
    }
}
