package com.earth2me.essentials.signs;

import com.earth2me.essentials.User;
import net.ess3.api.IEssentials;

import java.util.List;


public class SignMail extends EssentialsSign {
    public SignMail() {
        super("Mail");
    }

    @Override
    protected boolean onSignInteract(final ISign sign, final User player, final String username, final IEssentials ess) throws SignException {
        final List<String> mail = player.getMails();
        if (mail.isEmpty()) {
            player.sendTl("noNewMail");
            return false;
        }
        for (String s : mail) {
            player.sendMessage(s);
        }
        player.sendTl("markMailAsRead");
        return true;
    }
}
