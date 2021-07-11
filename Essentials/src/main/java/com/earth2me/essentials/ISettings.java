package com.earth2me.essentials;

import com.earth2me.essentials.commands.IEssentialsCommand;
import com.earth2me.essentials.signs.EssentialsSign;
import com.earth2me.essentials.textreader.IText;
import org.bukkit.Material;
import org.bukkit.event.EventPriority;
import org.spongepowered.configurate.CommentedConfigurationNode;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.function.Predicate;
import java.util.regex.Pattern;

public interface ISettings extends IConf {
    boolean areSignsDisabled();

    IText getAnnounceNewPlayerFormat();

    boolean getAnnounceNewPlayers();

    String getNewPlayerKit();

    String getBackupCommand();

    long getBackupInterval();

    boolean isAlwaysRunBackup();

    String getChatFormat(String group);

    String getWorldAlias(String world);

    int getChatRadius();

    int getNearRadius();

    char getChatShout();

    char getChatQuestion();

    BigDecimal getCommandCost(IEssentialsCommand cmd);

    BigDecimal getCommandCost(String label);

    String getCurrencySymbol();

    boolean isCurrencySymbolSuffixed();

    int getOversizedStackSize();

    int getDefaultStackSize();

    double getHealCooldown();

    Set<String> getSocialSpyCommands();

    boolean getSocialSpyListenMutedPlayers();

    boolean isSocialSpyMessages();

    Set<String> getMuteCommands();

    @Deprecated
    CommentedConfigurationNode getKitSection();

    boolean isSkippingUsedOneTimeKitsFromKitList();

    String getLocale();

    String getNewbieSpawn();

    String getNicknamePrefix();

    String getOperatorColor() throws Exception;

    boolean getPerWarpPermission();

    boolean getProtectBoolean(final String configName, boolean def);

    int getProtectCreeperMaxHeight();

    List<Material> getProtectList(final String configName);

    boolean getProtectPreventSpawn(final String creatureName);

    String getProtectString(final String configName);

    boolean getRespawnAtHome();

    boolean isRespawnAtAnchor();

    Set getMultipleHomes();

    int getHomeLimit(String set);

    int getHomeLimit(User user);

    int getSpawnMobLimit();

    BigDecimal getStartingBalance();

    boolean isTeleportSafetyEnabled();

    boolean isForceDisableTeleportSafety();

    boolean isAlwaysTeleportSafety();

    boolean isTeleportPassengerDismount();

    boolean isForcePassengerTeleport();

    double getTeleportCooldown();

    double getTeleportDelay();

    boolean hidePermissionlessHelp();

    boolean isCommandDisabled(final IEssentialsCommand cmd);

    boolean isCommandDisabled(String label);

    Set<String> getDisabledCommands();

    boolean isCommandOverridden(String name);

    boolean isDebug();

    void setDebug(boolean debug);

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

    boolean realNamesOnList();

    boolean removeGodOnDisconnect();

    boolean changeDisplayName();

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

    boolean cancelAfkOnChat();

    boolean sleepIgnoresAfkPlayers();

    boolean sleepIgnoresVanishedPlayers();

    boolean isAfkListName();

    String getAfkListName();

    boolean broadcastAfkMessage();

    boolean areDeathMessagesEnabled();

    KeepInvPolicy getVanishingItemsPolicy();

    KeepInvPolicy getBindingItemsPolicy();

    int getJoinQuitMessagePlayerCount();

    boolean hasJoinQuitMessagePlayerCount();

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

    int getTpaMaxRequests();

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

    long getMaxMute();

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

    String getCustomNewUsernameMessage();

    boolean isCustomNewUsernameMessage();

    boolean isCustomServerFullMessage();

    boolean isNotifyNoNewMail();

    boolean isDropItemsIfFull();

    boolean isLastMessageReplyRecipient();

    boolean isReplyToVanished();

    BigDecimal getMinimumPayAmount();

    boolean isPayExcludesIgnoreList();

    long getLastMessageReplyRecipientTimeout();

    boolean isMilkBucketEasterEggEnabled();

    boolean isSendFlyEnableOnJoin();

    boolean isWorldTimePermissions();

    boolean isSpawnOnJoin();

    List<String> getSpawnOnJoinGroups();

    boolean isUserInSpawnOnJoinGroup(IUser user);

    boolean isTeleportToCenterLocation();

    boolean isCommandCooldownsEnabled();

    boolean isWorldChangeFlyResetEnabled();

    boolean isWorldChangeSpeedResetEnabled();

    long getCommandCooldownMs(String label);

    Entry<Pattern, Long> getCommandCooldownEntry(String label);

    boolean isCommandCooldownPersistent(String label);

    boolean isNpcsInBalanceRanking();

    NumberFormat getCurrencyFormat();

    List<EssentialsSign> getUnprotectedSignNames();

    boolean isKitAutoEquip();

    boolean isPastebinCreateKit();

    boolean isUseBetterKits();

    boolean isAllowBulkBuySell();

    boolean isAllowSellNamedItems();

    boolean isAddingPrefixInPlayerlist();

    boolean isAddingSuffixInPlayerlist();

    int getNotifyPlayerOfMailCooldown();

    int getMotdDelay();

    boolean isDirectHatAllowed();

    List<String> getDefaultEnabledConfirmCommands();

    boolean isConfirmCommandEnabledByDefault(String commandName);

    TeleportWhenFreePolicy getTeleportWhenFreePolicy();

    boolean isJailOnlineTime();

    boolean isCompassTowardsHomePerm();

    boolean isAllowWorldInBroadcastworld();

    String getItemDbType();

    boolean allowOldIdSigns();

    boolean isWaterSafe();

    boolean isSafeUsermap();

    boolean logCommandBlockCommands();

    Set<Predicate<String>> getNickBlacklist();

    double getMaxProjectileSpeed();

    boolean isRemovingEffectsOnHeal();

    boolean isSpawnIfNoHome();

    boolean isConfirmHomeOverwrite();

    boolean infoAfterDeath();

    boolean isRespawnAtBed();

    boolean isUpdateCheckEnabled();

    boolean showZeroBaltop();

    enum KeepInvPolicy {
        KEEP,
        DELETE,
        DROP
    }

    enum TeleportWhenFreePolicy {
        SPAWN,
        BACK,
        OFF
    }

}
