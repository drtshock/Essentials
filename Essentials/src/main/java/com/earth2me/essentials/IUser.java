package com.earth2me.essentials;

import com.earth2me.essentials.api.IAsyncTeleport;
import com.earth2me.essentials.commands.IEssentialsCommand;
import com.earth2me.essentials.config.entities.CommandCooldown;
import net.ess3.api.ITeleport;
import net.ess3.api.MaxMoneyException;
import net.ess3.api.events.AfkStatusChangeEvent;
import net.essentialsx.api.v2.services.mail.MailMessage;
import net.essentialsx.api.v2.services.mail.MailSender;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.math.BigDecimal;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * Provides access to the user abstraction and stored data. Maintainers should add methods to <i>this interface</i>.
 *
 * @deprecated External plugins should use {@link net.ess3.api.IUser} instead of this interface, in case future APIs are added.
 */
@Deprecated
public interface IUser {
    boolean isAuthorized(String node);

    boolean isAuthorized(IEssentialsCommand cmd);

    boolean isAuthorized(IEssentialsCommand cmd, String permissionPrefix);

    boolean isPermissionSet(String node);

    void healCooldown() throws Exception;

    void giveMoney(BigDecimal value) throws MaxMoneyException;

    void giveMoney(final BigDecimal value, final CommandSource initiator) throws MaxMoneyException;

    void payUser(final User reciever, final BigDecimal value) throws Exception;

    void takeMoney(BigDecimal value);

    void takeMoney(final BigDecimal value, final CommandSource initiator);

    boolean canAfford(BigDecimal value);

    Boolean canSpawnItem(final Material material);

    void setLastLocation();

    void setLogoutLocation();

    void requestTeleport(final User player, final boolean here);

    /**
     * Returns whether this user has an outstanding teleport request to deal with.
     *
     * @deprecated The teleport request system has been moved into a multi-user teleport request queue.
     * @see IUser#hasPendingTpaRequests(boolean, boolean)
     * @return whether there is a teleport request
     */
    @Deprecated
    boolean hasOutstandingTeleportRequest();

    /**
     * @deprecated This API is not asynchronous. Use {@link com.earth2me.essentials.api.IAsyncTeleport IAsyncTeleport} with {@link IUser#getAsyncTeleport()}
     */
    @Deprecated
    ITeleport getTeleport();

    IAsyncTeleport getAsyncTeleport();

    BigDecimal getMoney();

    void setMoney(final BigDecimal value) throws MaxMoneyException;

    void setAfk(final boolean set, final AfkStatusChangeEvent.Cause cause);

    /**
     * 'Hidden' Represents when a player is hidden from others. This status includes when the player is hidden via other
     * supported plugins. Use isVanished() if you want to check if a user is vanished by Essentials.
     *
     * @return If the user is hidden or not
     * @see IUser#isVanished()
     */
    boolean isHidden();

    void setHidden(boolean vanish);

    boolean isGodModeEnabled();

    String getGroup();

    boolean inGroup(final String group);

    boolean canBuild();

    /**
     * @deprecated The teleport request system has been moved into a multi-user teleport request queue.
     * @see IUser#getNextTpaRequest(boolean, boolean, boolean)
     */
    @Deprecated
    long getTeleportRequestTime();

    void enableInvulnerabilityAfterTeleport();

    void resetInvulnerabilityAfterTeleport();

    boolean hasInvulnerabilityAfterTeleport();

    /**
     * 'Vanished' Represents when a player is hidden from others by Essentials. This status does NOT include when the
     * player is hidden via other plugins. Use isHidden() if you want to check if a user is vanished by any supported
     * plugin.
     *
     * @return If the user is vanished or not
     * @see IUser#isHidden()
     */
    boolean isVanished();

    void setVanished(boolean vanish);

    boolean isIgnoreExempt();

    void sendMessage(String message);

    /*
     * UserData
     */
    Location getHome(String name) throws Exception;

    Location getHome(Location loc) throws Exception;

    List<String> getHomes();

    void setHome(String name, Location loc);

    void delHome(String name) throws Exception;

    boolean hasHome();

    Location getLastLocation();

    Location getLogoutLocation();

    long getLastTeleportTimestamp();

    void setLastTeleportTimestamp(long time);

    String getJail();

    void setJail(String jail);

    String getFormattedJailTime();

    @Deprecated
    List<String> getMails();

    @Deprecated
    void addMail(String mail);

    void sendMail(MailSender sender, String message);

    void sendMail(MailSender sender, String message, long expireAt);

    ArrayList<MailMessage> getMailMessages();

    void setMailList(ArrayList<MailMessage> messages);

    int getMailAmount();

    boolean isAfk();

    @Deprecated
    void setAfk(final boolean set);

    boolean isIgnoreMsg();

    void setIgnoreMsg(boolean ignoreMsg);

    @Deprecated
    void setConfigProperty(String node, Object object);

    Set<String> getConfigKeys();

    Map<String, Object> getConfigMap();

    Map<String, Object> getConfigMap(String node);

    @Deprecated
    Map<Pattern, Long> getCommandCooldowns();

    List<CommandCooldown> getCooldownsList();

    Date getCommandCooldownExpiry(String label);

    void addCommandCooldown(Pattern pattern, Date expiresAt, boolean save);

    boolean clearCommandCooldown(Pattern pattern);

    /*
     *  PlayerExtension
     */
    Player getBase();

    CommandSource getSource();

    String getName();

    UUID getUUID();

    String getDisplayName();

    String getFormattedNickname();

    String getAfkMessage();

    void setAfkMessage(final String message);

    long getAfkSince();

    boolean isAcceptingPay();

    void setAcceptingPay(boolean acceptingPay);

    boolean isPromptingPayConfirm();

    void setPromptingPayConfirm(boolean prompt);

    boolean isPromptingClearConfirm();

    void setPromptingClearConfirm(boolean prompt);

    boolean isLastMessageReplyRecipient();

    void setLastMessageReplyRecipient(boolean enabled);

    Map<User, BigDecimal> getConfirmingPayments();

    Block getTargetBlock(int maxDistance);

    void setToggleShout(boolean toggleShout);

    boolean isToggleShout();

    /**
     * Gets information about the most-recently-made, non-expired TPA request in the tpa queue of this {@link IUser}.
     * <p>
     * The TPA Queue is Last-In-First-Out queue which stores all the active pending teleport
     * requests of this {@link IUser}. Timeout calculations are also done during the
     * iteration process of this method, ensuring that teleport requests made past the timeout
     * period are removed from queue and therefore not returned here. The maximum size of this
     * queue is determined by {@link ISettings#getTpaMaxRequests()}.
     *
     * @param inform             true if the underlying {@link IUser} should be informed if a request expires during iteration.
     * @param performExpirations true if this method should not spend time validating time for all items in the queue and just return the first item in the queue.
     * @param excludeHere        true if /tphere requests should be ignored in fetching the next tpa request.
     * @return A {@link TpaRequest} corresponding to the next available request or null if no valid request is present.
     */
    @Nullable TpaRequest getNextTpaRequest(boolean inform, boolean performExpirations, boolean excludeHere);

    /**
     * Whether or not this {@link IUser} has any valid TPA requests in queue.
     *
     * @param inform      true if the user should be informed if a request expires during iteration.
     * @param excludeHere true if /tpahere requests should be ignored in checking if a tpa request is available.
     * @return true if the user has an available pending request in queue.
     */
    boolean hasPendingTpaRequests(boolean inform, boolean excludeHere);

    class TpaRequest {
        private final String name;
        private final UUID requesterUuid;
        private boolean here;
        private Location location;
        private long time;

        public TpaRequest(String name, UUID requesterUuid) {
            this.name = name;
            this.requesterUuid = requesterUuid;
        }

        public String getName() {
            return name;
        }

        public UUID getRequesterUuid() {
            return requesterUuid;
        }

        public boolean isHere() {
            return here;
        }

        public void setHere(boolean here) {
            this.here = here;
        }

        public Location getLocation() {
            return location;
        }

        public void setLocation(Location location) {
            this.location = location;
        }

        public long getTime() {
            return time;
        }

        public void setTime(long time) {
            this.time = time;
        }
    }
}
