package com.earth2me.essentials;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import static com.earth2me.essentials.I18n.tl;


public class CommandSource {
    protected CommandSender sender;
    protected User user = null;

    public CommandSource(final CommandSender base) {
        this.sender = base;
    }

    public CommandSource(final User user) {
        this.sender = user.getBase();
        this.user = user;
    }

    public final CommandSender getSender() {
        return sender;
    }

    public final Player getPlayer() {
        if (sender instanceof Player) {
            return (Player) sender;
        }
        return null;
    }

    public final boolean isPlayer() {
        return (sender instanceof Player);
    }

    public final CommandSender setSender(final CommandSender base) {
        return this.sender = base;
    }

    public void sendMessage(String message) {
        if (!message.isEmpty()) {
            sender.sendMessage(message);
        }
    }

    public void setUser(User user) {
        this.user = user;
    }

    public User getUser() {
        return user;
    }

    public void sendTl(String string, Object... objects) {
        sendMessage(tl(string, objects));
    }

    public String tl(String string, Object... objects) {
        if (user != null) {
            return I18n.tl(user.getApplicableLocale(), string, objects);
        } else {
            return I18n.tl(string, objects);
        }
    }
}
