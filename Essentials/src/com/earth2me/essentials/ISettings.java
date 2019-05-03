package com.earth2me.essentials;

import com.earth2me.essentials.commands.IEssentialsCommand;
import com.earth2me.essentials.signs.EssentialsSign;
import com.earth2me.essentials.textreader.IText;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.event.EventPriority;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.regex.Pattern;


public interface ISettings extends IConf {
    boolean areSignsDisabled();

    IText getAnnounceNewPlayerFormat();

    boolean getAnnounceNewPlayers();

    String getNewPlayerKit();

    String getBackupCommand();

    long getBackupInterval();

    String getChatFormat(String group);

    int getChatRadius();

    int getNearRadius();

    char getChatShout();

    char getChatQuestion();

    BigDecimal getCommandCost(IEssentialsCommand cmd);

    BigDecimal getCommandCost(String label);

    String getCurrencySymbol();

    int getOversizedStackSize();

    int getDefaultStackSize();

    double getHealCooldown();

    Set<String> getSocialSpyCommands();

    boolean getSocialSpyListenMutedPlayers();

    Set<String> getMuteCommands();

    /**
     * @Deprecated in favor of {@link Kits#getKits()}
     */
    @Deprecated
    ConfigurationSection getKits();

    /**
     * @Deprecated in favor of {@link Kits#getKit(String)}
     */
    @Deprecated
    Map<String, Object> getKit(String kit);

    /**
     * @Deprecated in favor of {@link Kits#addKit(String, List, long)}}
     */
    @Deprecated
    void addKit(String name, List<String> lines, long delay);

    @Deprecated
    ConfigurationSection getKitSection();

    boolean isSkippingUsedOneTimeKitsFromKitList();

    String getLocale();

    String getNewbieSpawn();

    String getNicknamePrefix();

    ChatColor getOperatorColor() throws Exception;

    boolean getPerWarpPermission();

    boolean getProtectBoolean(final String configName, boolean def);

    int getProtectCreeperMaxHeight();

    List<Material> getProtectList(final String configName);

    boolean getProtectPreventSpawn(final String creatureName);

    String getProtectString(final String configName);

    boolean getRespawnAtHome();

    Set getMultipleHomes();

    int getHomeLimit(String set);

    int getHomeLimit(User user);

    int getSpawnMobLimit();

    BigDecimal getStartingBalance();

    boolean isTeleportSafetyEnabled();

    boolean isForceDisableTeleportSafety();

    double getTeleportCooldown();

    double getTeleportDelay();

    boolean hidePermissionlessHelp();

    boolean isCommandDisabled(final IEssentialsCommand cmd);

    boolean isCommandDisabled(String label);

    boolean isCommandOverridden(String name);

    boolean isDebug();

    boolean isEcoDisabled();

    @Deprecated
    boolean isTradeInStacks(int id);

    boolean isTradeInStacks(Material type);

    List<Material> itemSpawnBlacklist();

    List<EssentialsSign> enabledSigns();

    boolean permissionBasedItemSpawn();

    boolean showNonEssCommandsInHelp();

    boolean warnOnBuildDisallow();

    boolean warnOnSmite();

    BigDecimal getMaxMoney();

    BigDecimal getMinMoney();

    boolean isEcoLogEnabled();

    boolean isEcoLogUpdateEnabled();

    boolean removeGodOnDisconnect();

    boolean changeDisplayName();

    boolean changeLocale();

    boolean changePlayerListName();

    boolean isPlayerCommand(String string);

    boolean useBukkitPermissions();

    boolean addPrefixSuffix();

    boolean disablePrefix();

    boolean disableSuffix();

    long getAutoAfk();

    long getAutoAfkKick();

    boolean getFreezeAfkPlayers();

    boolean cancelAfkOnMove();

    boolean cancelAfkOnInteract();

    boolean sleepIgnoresAfkPlayers();

    boolean isAfkListName();

    String getAfkListName();

    boolean areDeathMessagesEnabled();

    void setDebug(boolean debug);

    Set<String> getNoGodWorlds();

    boolean getUpdateBedAtDaytime();

    boolean allowUnsafeEnchantments();

    boolean getRepairEnchanted();

    boolean isWorldTeleportPermissions();

    boolean isWorldHomePermissions();

    boolean registerBackInListener();

    boolean getDisableItemPickupWhileAfk();

    EventPriority getRespawnPriority();

    EventPriority getSpawnJoinPriority();

    long getTpaAcceptCancellation();

    long getTeleportInvulnerability();

    boolean isTeleportInvulnerability();

    long getLoginAttackDelay();

    int getSignUsePerSecond();

    double getMaxFlySpeed();

    double getMaxWalkSpeed();

    int getMailsPerMinute();

    long getEconomyLagWarning();

    long getPermissionsLagWarning();

    void setEssentialsChatActive(boolean b);

    long getMaxTempban();

    Map<String, Object> getListGroupConfig();

    int getMaxNickLength();

    boolean ignoreColorsInMaxLength();

    boolean hideDisplayNameInVanish();

    int getMaxUserCacheCount();

    boolean allowSilentJoinQuit();

    boolean isCustomJoinMessage();

    String getCustomJoinMessage();

    boolean isCustomQuitMessage();

    String getCustomQuitMessage();

    boolean isNotifyNoNewMail();

    boolean isDropItemsIfFull();

    boolean isLastMessageReplyRecipient();

    BigDecimal getMinimumPayAmount();

    long getLastMessageReplyRecipientTimeout();

    boolean isMilkBucketEasterEggEnabled();

    boolean isSendFlyEnableOnJoin();

    boolean isWorldTimePermissions();

    boolean isSpawnOnJoin();

    List<String> getSpawnOnJoinGroups();

    boolean isUserInSpawnOnJoinGroup(IUser user);

    boolean isTeleportToCenterLocation();

    boolean isCommandCooldownsEnabled();

    long getCommandCooldownMs(String label);

    Entry<Pattern, Long> getCommandCooldownEntry(String label);

    boolean isCommandCooldownPersistent(String label);

    boolean isNpcsInBalanceRanking();

    NumberFormat getCurrencyFormat();

    List<EssentialsSign> getUnprotectedSignNames();

    boolean isPastebinCreateKit();

    boolean isAllowBulkBuySell();

    boolean isAddingPrefixInPlayerlist();

    boolean isAddingSuffixInPlayerlist();

    int getNotifyPlayerOfMailCooldown();

    int getMotdDelay();

    boolean isDirectHatAllowed();

    List<String> getDefaultEnabledConfirmCommands();

    boolean isConfirmCommandEnabledByDefault(String commandName);

    boolean isTeleportBackWhenFreedFromJail();

    boolean isCompassTowardsHomePerm();

    boolean isAllowWorldInBroadcastworld();

    String getItemDbType();

    boolean isForceEnableRecipe();

    boolean allowOldIdSigns();
}
