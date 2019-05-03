package com.earth2me.essentials;

import com.earth2me.essentials.messaging.IMessageRecipient;
import com.earth2me.essentials.messaging.SimpleMessageRecipient;

import org.bukkit.Server;
import org.bukkit.command.CommandSender;

import java.util.Locale;


public final class Console implements IMessageRecipient {
    public static final String NAME = "Console";
    private static Console instance; // Set in essentials
    
    private final IEssentials ess;
    private final IMessageRecipient messageRecipient;

    public static Console getInstance() {
        return instance;
    }

    static void setInstance(IEssentials ess) { // Called in Essentials#onEnable()
        instance = new Console(ess);
    }
    
    /**
     * @deprecated Use {@link Console#getCommandSender()}
     */
    @Deprecated
    public static CommandSender getCommandSender(Server server) throws Exception {
        return server.getConsoleSender();
    }

    private Console(IEssentials ess) {
        this.ess = ess;
        this.messageRecipient = new SimpleMessageRecipient(ess, this);
    }

    public CommandSender getCommandSender() {
        return ess.getServer().getConsoleSender();
    }

    @Override public String getName() {
        return Console.NAME;
    }

    @Override public String getDisplayName() {
        return Console.NAME;
    }

    @Override public void sendMessage(String message) {
        getCommandSender().sendMessage(message);
    }

    @Override public boolean isReachable() {
        return true;
    }


    /* ================================
     * >> DELEGATE METHODS
     * ================================ */
    @Override public MessageResponse sendMessage(IMessageRecipient recipient, String message) {
        return this.messageRecipient.sendMessage(recipient, message);
    }

    @Override public MessageResponse onReceiveMessage(IMessageRecipient sender, String message) {
        return this.messageRecipient.onReceiveMessage(sender, message);
    }

    @Override public IMessageRecipient getReplyRecipient() {
        return this.messageRecipient.getReplyRecipient();
    }

    @Override public void setReplyRecipient(IMessageRecipient recipient) {
        this.messageRecipient.setReplyRecipient(recipient);
    }

    @Override public Locale getLocale() {
        return this.messageRecipient.getLocale();
    }
}
