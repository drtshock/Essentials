package net.essentialsx.api.v2.services.discord;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Represents the interaction command executor as a guild member.
 */
public interface InteractionMember {
    /**
     * Gets the username of this member.
     * @return this member's username.
     */
    String getName();

    /**
     * Gets the four numbers after the {@code #} in the member's username.
     * @return this member's discriminator.
     */
    String getDiscriminator();

    /**
     * Gets this member's name and discriminator split by a {@code #}.
     * @return this member's tag.
     */
    default String getTag() {
        return getName() + "#" + getDiscriminator();
    }

    /**
     * Gets the discord mention of this member.
     * @return this member's mention.
     */
    String getAsMention();

    /**
     * Gets the nickname of this member or their username if they don't have one.
     * @return this member's nickname or username if none is present.
     */
    String getEffectiveName();

    /**
     * Gets the nickname of this member or null if they do not have one.
     * @return this member's nickname or null.
     */
    String getNickname();

    /**
     * Gets the ID of this member.
     * @return this member's ID.
     */
    String getId();

    /**
     * Checks if this member has the administrator permission on Discord.
     * @return true if this user has administrative permissions.
     */
    boolean isAdmin();

    /**
     * Returns true if the user has one of the specified roles.
     * @param roleDefinitions A list of role definitions from the config.
     * @return true if the member has one of the given roles.
     */
    boolean hasRoles(List<String> roleDefinitions);

    /**
     * Sends a private message to this member with the given content.
     * @param content The message to send.
     * @return A future which will complete a boolean stating the success of the message.
     */
    CompletableFuture<Boolean> sendPrivateMessage(String content);
}
