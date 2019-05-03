package com.earth2me.essentials.commands;

import com.earth2me.essentials.CommandSource;
import com.earth2me.essentials.Console;
import com.earth2me.essentials.User;
import com.earth2me.essentials.utils.FormatUtil;
import org.bukkit.BanList;
import org.bukkit.Server;
import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.List;
import java.util.logging.Level;

import static com.earth2me.essentials.I18n.tl;


public class Commandbanip extends EssentialsCommand {
    public Commandbanip() {
        super("banip");
    }

    @Override
    public void run(final Server server, final CommandSource sender, final String commandLabel, final String[] args) throws Exception {
        if (args.length < 1) {
            throw new NotEnoughArgumentsException();
        }

        final String senderName = sender.isPlayer() ? sender.getPlayer().getDisplayName() : Console.NAME;

        String ipAddress;
        if (FormatUtil.validIP(args[0])) {
            ipAddress = args[0];
        } else {
            try {
                User player = getPlayer(server, args, 0, true, true);
                ipAddress = player.getLastLoginAddress();
            } catch (PlayerNotFoundException ex) {
                ipAddress = args[0];
            }
        }

        if (ipAddress.isEmpty()) {
            throw new PlayerNotFoundException();
        }

        String banReason;
        if (args.length > 1) {
            banReason = FormatUtil.replaceFormat(getFinalArg(args, 1).replace("\\n", "\n").replace("|", "\n"));
        } else {
            banReason = sender.tl("defaultBanReason");
        }

        ess.getServer().getBanList(BanList.Type.IP).addBan(ipAddress, banReason, null, senderName);
        server.getLogger().log(Level.INFO, tl("playerBanIpAddress", senderName, ipAddress, banReason));

        for (Player player : ess.getServer().getOnlinePlayers()) {
            if (player.getAddress().getAddress().getHostAddress().equalsIgnoreCase(ipAddress)) {
                final User buser = ess.getUser(player);
                player.kickPlayer(buser.tl("banFormat", banReason, senderName));
            }
        }

        ess.broadcastTl("essentials.banip.notify", "playerBanIpAddress", senderName, ipAddress, banReason);
    }

    @Override
    protected List<String> getTabCompleteOptions(Server server, CommandSource sender, String commandLabel, String[] args) {
        if (args.length == 1) {
            // TODO: Also list IP addresses?
            return getPlayers(server, sender);
        } else {
            return Collections.emptyList();
        }
    }
}
