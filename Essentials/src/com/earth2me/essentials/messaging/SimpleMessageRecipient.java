package com.earth2me.essentials.messaging;

import com.earth2me.essentials.IEssentials;
import com.earth2me.essentials.IUser;
import com.earth2me.essentials.User;

import java.lang.ref.WeakReference;
import java.util.Locale;

import static com.earth2me.essentials.I18n.tl;

/**
 * Represents a simple reusable implementation of {@link IMessageRecipient}. This class provides functionality for the following methods:
 * <ul>
 *     <li>{@link IMessageRecipient#sendMessage(IMessageRecipient, String)}</li>
 *     <li>{@link IMessageRecipient#onReceiveMessage(IMessageRecipient, String)}</li>
 *     <li>{@link IMessageRecipient#getReplyRecipient()}</li>
 *     <li>{@link IMessageRecipient#setReplyRecipient(IMessageRecipient)}</li>
 * </ul>
 *
 * <b>The given {@code parent} must implement the following methods to prevent overflow:</b>
 * <ul>
 *     <li>{@link IMessageRecipient#sendMessage(String)}</li>
 *     <li>{@link IMessageRecipient#getName()}</li>
 *     <li>{@link IMessageRecipient#getDisplayName()}</li>
 *     <li>{@link IMessageRecipient#isReachable()}</li>
 * </ul>
 *
 * The reply-recipient is wrapped in a {@link WeakReference}.
 */
public class SimpleMessageRecipient implements IMessageRecipient {

    private final IEssentials ess;
    private final IMessageRecipient parent;

    private long lastMessageMs;
    private WeakReference<IMessageRecipient> replyRecipient;

    protected static User getUser(IMessageRecipient recipient) {
        if (recipient instanceof SimpleMessageRecipient) {
            return ((SimpleMessageRecipient) recipient).parent instanceof User ? (User) ((SimpleMessageRecipient) recipient).parent : null;
        }
        return recipient instanceof User ? (User) recipient : null;
    }

    public SimpleMessageRecipient(IEssentials ess, IMessageRecipient parent) {
        this.ess = ess;
        this.parent = parent;
    }

    @Override
    public void sendMessage(String message) {
        this.parent.sendMessage(message);
    }

    @Override
    public String getName() {
        return this.parent.getName();
    }

    @Override public String getDisplayName() {
        return this.parent.getDisplayName();
    }

    @Override public MessageResponse sendMessage(IMessageRecipient recipient, String message) {
        MessageResponse messageResponse = recipient.onReceiveMessage(this.parent, message);
        switch (messageResponse) {
            case UNREACHABLE:
                sendMessage(tl(getLocale(), "recentlyForeverAlone", recipient.getDisplayName()));
                break;
            case MESSAGES_IGNORED:
                sendMessage(tl(getLocale(), "msgIgnore", recipient.getDisplayName()));
                break;
            case SENDER_IGNORED:
                break;
            // When this recipient is AFK, notify the sender. Then, proceed to send the message.
            case SUCCESS_BUT_AFK:
                // Currently, only IUser can be afk, so we unsafely cast to get the afk message.
                if (((IUser) recipient).getAfkMessage() != null) {
                    sendMessage(tl(getLocale(), "userAFKWithMessage", recipient.getDisplayName(), ((IUser) recipient).getAfkMessage()));
                } else {
                    sendMessage(tl(getLocale(), "userAFK", recipient.getDisplayName()));
                }
            default:
                sendMessage(tl(getLocale(), "msgFormat", tl(getLocale(), "me"), recipient.getDisplayName(), message));

                // Better Social Spy
                User senderUser = getUser(this);
                User recipientUser = getUser(recipient);
                if (senderUser != null // not null if player.
                    // Dont spy on chats involving socialspy exempt players
                    && !senderUser.isAuthorized("essentials.chat.spy.exempt")
                    && (recipientUser != null && !recipientUser.isAuthorized("essentials.chat.spy.exempt"))) {
                    for (User onlineUser : ess.getOnlineUsers()) {
                        if (onlineUser.isSocialSpyEnabled()
                            // Don't send socialspy messages to message sender/receiver to prevent spam
                            && !onlineUser.equals(senderUser)
                            && !onlineUser.equals(recipient)) {
                            if (senderUser.isMuted() && ess.getSettings().getSocialSpyListenMutedPlayers()) {
                                onlineUser.sendMessage(onlineUser.tl("socialMutedSpyPrefix") + onlineUser.tl("socialSpyMsgFormat", getDisplayName(), recipient.getDisplayName(), message));
                            } else {
                                onlineUser.sendMessage(onlineUser.tl("socialSpyPrefix") + onlineUser.tl("socialSpyMsgFormat", getDisplayName(), recipient.getDisplayName(), message));
                            }
                        }
                    }
                }
        }
        // If the message was a success, set this sender's reply-recipient to the current recipient.
        if (messageResponse.isSuccess()) {
            setReplyRecipient(recipient);
        }
        return messageResponse;
    }

    @Override
    public MessageResponse onReceiveMessage(IMessageRecipient sender, String message) {
        if (!isReachable()) {
            return MessageResponse.UNREACHABLE;
        }

        User user = getUser(this);
        boolean afk = false;
        boolean isLastMessageReplyRecipient = ess.getSettings().isLastMessageReplyRecipient();
        if (user != null) {
            if (user.isIgnoreMsg() && sender instanceof IUser && !((IUser) sender).isAuthorized("essentials.msgtoggle.bypass")) { // Don't ignore console and senders with permission
                return MessageResponse.MESSAGES_IGNORED;
            }
            afk = user.isAfk();
            isLastMessageReplyRecipient = user.isLastMessageReplyRecipient();
            // Check whether this recipient ignores the sender, only if the sender is not the console.
            if (sender instanceof IUser && user.isIgnoredPlayer((IUser) sender)) {
                return MessageResponse.SENDER_IGNORED;
            }
        }
        // Display the formatted message to this recipient.
        sendMessage(tl(getLocale(), "msgFormat", sender.getDisplayName(), tl(getLocale(), "me"), message));

        if (isLastMessageReplyRecipient) {
            // If this recipient doesn't have a reply recipient, initiate by setting the first
            // message sender to this recipient's replyRecipient.
            long timeout = ess.getSettings().getLastMessageReplyRecipientTimeout() * 1000;
            if (getReplyRecipient() == null || !getReplyRecipient().isReachable()
                || System.currentTimeMillis() - this.lastMessageMs > timeout) {
                setReplyRecipient(sender);
            }
        } else { // Old message functionality, always set the reply recipient to the last person who sent us a message.
            setReplyRecipient(sender);
        }
        this.lastMessageMs = System.currentTimeMillis();
        return afk ? MessageResponse.SUCCESS_BUT_AFK : MessageResponse.SUCCESS;
    }

    @Override public boolean isReachable() {
        return this.parent.isReachable();
    }

    @Override
    public Locale getLocale() {
        User user = getUser(this);
        return user != null ? user.getApplicableLocale() : ess.getI18n().getCurrentLocale();
    }

    /**
     * {@inheritDoc}
     * <p />
     * <b>This {@link com.earth2me.essentials.messaging.SimpleMessageRecipient} implementation stores the a weak reference to the recipient.</b>
     */
    @Override
    public IMessageRecipient getReplyRecipient() {
        return replyRecipient == null ? null : replyRecipient.get();
    }

    /**
     * {@inheritDoc}
     * <p />
     * <b>This {@link com.earth2me.essentials.messaging.SimpleMessageRecipient} implementation stores the a weak reference to the recipient.</b>
     */
    @Override
    public void setReplyRecipient(final IMessageRecipient replyRecipient) {
        this.replyRecipient = new WeakReference<>(replyRecipient);
    }
}
