package com.earth2me.essentials.commands;

import com.earth2me.essentials.User;
import org.bukkit.Server;

import static com.earth2me.essentials.I18n.tl;

public class Commandtpacancel extends EssentialsCommand {

    public Commandtpacancel() {
        super("tpacancel");
    }

    /**
     * Cancel {@link User}'s tp request if its {@code requester} is equal to the given {@code requester}.
     *
     * @param user      user holding tp request
     * @param requester tp requester
     * @return whether tp was cancelled
     */
    public static boolean cancelTeleportRequest(final User user, final User requester) {
        return user.removeTpaRequest(requester.getName()) != null;
    }

    @Override
    public void run(final Server server, final User user, final String commandLabel, final String[] args) throws Exception {
        if (args.length == 0) {
            int cancellations = 0;
            for (final User onlineUser : ess.getOnlineUsers()) {
                if (onlineUser == user) continue;
                if (cancelTeleportRequest(onlineUser, user)) {
                    cancellations++;
                }
            }
            if (cancellations > 0) {
                user.sendMessage(tl("teleportRequestAllCancelled", cancellations));
            } else {
                throw new Exception(tl("noPendingRequest"));
            }
        } else {
            final User targetPlayer = getPlayer(server, user, args, 0);
            if (cancelTeleportRequest(targetPlayer, user)) {
                user.sendMessage(tl("teleportRequestSpecificCancelled", targetPlayer.getName()));
            }
        }
    }
}
