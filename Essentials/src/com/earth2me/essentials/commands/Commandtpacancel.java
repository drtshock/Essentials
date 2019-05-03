package com.earth2me.essentials.commands;

import com.earth2me.essentials.User;

import net.ess3.api.IEssentials;

import org.bukkit.Server;


public class Commandtpacancel extends EssentialsCommand {

    public Commandtpacancel() {
        super("tpacancel");
    }

    /**
     * Cancel {@link User}'s tp request if its {@code requester} is equal to the given {@code requester}.
     * @param ess ess instance
     * @param user user holding tp request
     * @param requester tp requester
     * @return whether tp was cancelled
     */
    public static boolean cancelTeleportRequest(IEssentials ess, User user, User requester) throws Exception {
        if (user.getTeleportRequest() != null) {
            User userRequester = ess.getUser(user.getTeleportRequest());
            if (requester.equals(userRequester)) {
                user.requestTeleport(null, false);
                return true;
            }
        }
        return false;
    }

    @Override
    public void run(final Server server, final User user, final String commandLabel, final String[] args) throws Exception {
        if (args.length == 0) {
            int cancellations = 0;
            for (User onlineUser : ess.getOnlineUsers()) {
                if (onlineUser == user) continue;
                if (cancelTeleportRequest(ess, onlineUser, user)) {
                    cancellations++;
                }
            }
            if (cancellations > 0) {
                user.sendTl("teleportRequestAllCancelled", cancellations);
            } else {
                throw new Exception(user.tl("noPendingRequest"));
            }
        } else {
            User targetPlayer = getPlayer(server, user, args, 0);
            if (cancelTeleportRequest(ess, targetPlayer, user)) {
                user.sendTl("teleportRequestSpecificCancelled", targetPlayer.getName());
            }
        }
    }
}
