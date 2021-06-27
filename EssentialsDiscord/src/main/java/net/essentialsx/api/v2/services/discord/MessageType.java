package net.essentialsx.api.v2.services.discord;

/**
 * Indicates the type of message being sent and its literal channel name used in the config.
 */
public final class MessageType {
    private final String key;
    private final boolean player;

    /**
     * Creates a {@link MessageType} which will send channels to the specified channel key.
     * <p>
     * The message type key may only contain: lowercase letters, numbers, and dashes.
     * @param key The channel key defined in the {@code message-types} section of the config.
     */
    public MessageType(final String key) {
        this(key, false);
    }

    /**
     * Internal constructor used by EssentialsX Discord
     */
    private MessageType(String key, boolean player) {
        this.key = key;
        this.player = player;
    }

    /**
     * Gets the key used in {@code message-types} section of the config.
     * @return The config key.
     */
    public String getKey() {
        return key;
    }

    /**
     * Checks if this message type should be beholden to player-specific config settings.
     * @return true if message type should be beholden to player-specific config settings.
     */
    public boolean isPlayer() {
        return player;
    }

    @Override
    public String toString() {
        return key;
    }

    /**
     * Default {@link MessageType MessageTypes} provided and documented by EssentialsX Discord.
     */
    public static final class DefaultTypes {
        public final static MessageType JOIN = new MessageType("join", true);
        public final static MessageType LEAVE = new MessageType("leave", true);
        public final static MessageType CHAT = new MessageType("chat", true);
        public final static MessageType DEATH = new MessageType("death", true);
        public final static MessageType AFK = new MessageType("afk", true);
        public final static MessageType KICK = new MessageType("kick", false);
        public final static MessageType MUTE = new MessageType("mute", false);
        private final static MessageType[] VALUES = new MessageType[]{JOIN, LEAVE, CHAT, DEATH, AFK, KICK, MUTE};

        /**
         * Gets an array of all the default {@link MessageType MessageTypes}.
         * @return An array of all the default {@link MessageType MessageTypes}.
         */
        public static MessageType[] values() {
            return VALUES;
        }
    }
}