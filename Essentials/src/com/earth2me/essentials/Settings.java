package com.earth2me.essentials;

import com.earth2me.essentials.api.IItemDb;
import com.earth2me.essentials.commands.IEssentialsCommand;
import com.earth2me.essentials.signs.EssentialsSign;
import com.earth2me.essentials.signs.Signs;
import com.earth2me.essentials.textreader.IText;
import com.earth2me.essentials.textreader.SimpleTextInput;
import com.earth2me.essentials.utils.EnumUtil;
import com.earth2me.essentials.utils.FormatUtil;
import com.earth2me.essentials.utils.NumberUtil;

import net.ess3.api.IEssentials;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.MemoryConfiguration;
import org.bukkit.event.EventPriority;
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.NumberFormat;
import java.util.*;
import java.util.Map.Entry;
import java.util.logging.Level;
import java.util.logging.Logger;

import static com.earth2me.essentials.I18n.tl;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;


public class Settings implements net.ess3.api.ISettings {
    private final transient EssentialsConf config;
    private static final Logger logger = Logger.getLogger("Essentials");
    private final transient IEssentials ess;

    public Settings(IEssentials ess) {
        this.ess = ess;
        config = new EssentialsConf(new File(ess.getDataFolder(), "config.yml"));
        config.setTemplateName("/config.yml");
        reloadConfig();
    }

    @Override
    public boolean getRespawnAtHome() {
        return config.getBoolean("respawn-at-home", false);
    }

    @Override
    public boolean getUpdateBedAtDaytime() {
        return config.getBoolean("update-bed-at-daytime", true);
    }

    @Override
    public Set<String> getMultipleHomes() {
        final ConfigurationSection section = config.getConfigurationSection("sethome-multiple");
        return section == null ? null : section.getKeys(false);
    }

    @Override
    public int getHomeLimit(final User user) {
        int limit = 1;
        if (user.isAuthorized("essentials.sethome.multiple")) {
            limit = getHomeLimit("default");
        }

        final Set<String> homeList = getMultipleHomes();
        if (homeList != null) {
            for (String set : homeList) {
                if (user.isAuthorized("essentials.sethome.multiple." + set) && (limit < getHomeLimit(set))) {
                    limit = getHomeLimit(set);
                }
            }
        }
        return limit;
    }

    @Override
    public int getHomeLimit(final String set) {
        return config.getInt("sethome-multiple." + set, config.getInt("sethome-multiple.default", 3));
    }

    private int chatRadius = 0;

    private int _getChatRadius() {
        return config.getInt("chat.radius", config.getInt("chat-radius", 0));
    }

    @Override
    public int getChatRadius() {
        return chatRadius;
    }

    @Override
    public int getNearRadius() {
        return config.getInt("near-radius", 200);
    }

    // #easteregg
    private char chatShout = '!';

    private char _getChatShout() {
        return config.getString("chat.shout", "!").charAt(0);
    }

    @Override
    public char getChatShout() {
        return chatShout;
    }

    // #easteregg
    private char chatQuestion = '?';

    private char _getChatQuestion() {
        return config.getString("chat.question", "?").charAt(0);
    }

    @Override
    public char getChatQuestion() {
        return chatQuestion;
    }

    private boolean teleportSafety;

    public boolean _isTeleportSafetyEnabled() {
        return config.getBoolean("teleport-safety", true);
    }

    @Override
    public boolean isTeleportSafetyEnabled() {
        return teleportSafety;
    }

    private boolean forceDisableTeleportSafety;

    private boolean _isForceDisableTeleportSafety() {
        return config.getBoolean("force-disable-teleport-safety", false);
    }

    @Override
    public boolean isForceDisableTeleportSafety() {
        return forceDisableTeleportSafety;
    }

    @Override
    public double getTeleportDelay() {
        return config.getDouble("teleport-delay", 0);
    }

    @Override
    public int getOversizedStackSize() {
        return config.getInt("oversized-stacksize", 64);
    }

    @Override
    public int getDefaultStackSize() {
        return config.getInt("default-stack-size", -1);
    }

    @Override
    public BigDecimal getStartingBalance() {
        return config.getBigDecimal("starting-balance", BigDecimal.ZERO);
    }

    @Override
    public boolean isCommandDisabled(final IEssentialsCommand cmd) {
        return isCommandDisabled(cmd.getName());
    }

    private Set<String> disabledCommands = new HashSet<String>();

    @Override
    public boolean isCommandDisabled(String label) {
        return disabledCommands.contains(label);
    }

    private Set<String> getDisabledCommands() {
        Set<String> disCommands = new HashSet<String>();
        for (String c : config.getStringList("disabled-commands")) {
            disCommands.add(c.toLowerCase(Locale.ENGLISH));
        }
        for (String c : config.getKeys(false)) {
            if (c.startsWith("disable-")) {
                disCommands.add(c.substring(8).toLowerCase(Locale.ENGLISH));
            }
        }
        return disCommands;
    }

    @Override
    public boolean isPlayerCommand(String label) {
        for (String c : config.getStringList("player-commands")) {
            if (!c.equalsIgnoreCase(label)) {
                continue;
            }
            return true;
        }
        return false;
    }

    @Override
    public boolean isCommandOverridden(String name) {
        for (String c : config.getStringList("overridden-commands")) {
            if (!c.equalsIgnoreCase(name)) {
                continue;
            }
            return true;
        }
        return config.getBoolean("override-" + name.toLowerCase(Locale.ENGLISH), false);
    }

    private ConfigurationSection commandCosts;

    @Override
    public BigDecimal getCommandCost(IEssentialsCommand cmd) {
        return getCommandCost(cmd.getName());
    }

    private ConfigurationSection _getCommandCosts() {
        if (config.isConfigurationSection("command-costs")) {
            final ConfigurationSection section = config.getConfigurationSection("command-costs");
            final ConfigurationSection newSection = new MemoryConfiguration();
            for (String command : section.getKeys(false)) {
                if (command.charAt(0) == '/') {
                    ess.getLogger().warning("Invalid command cost. '" + command + "' should not start with '/'.");
                }
                if (section.isDouble(command)) {
                    newSection.set(command.toLowerCase(Locale.ENGLISH), section.getDouble(command));
                } else if (section.isInt(command)) {
                    newSection.set(command.toLowerCase(Locale.ENGLISH), (double) section.getInt(command));
                } else if (section.isString(command)) {
                    String costString = section.getString(command);
                    try {
                        double cost = Double.parseDouble(costString.trim().replace(getCurrencySymbol(), "").replaceAll("\\W", ""));
                        newSection.set(command.toLowerCase(Locale.ENGLISH), cost);
                    } catch (NumberFormatException ex) {
                        ess.getLogger().warning("Invalid command cost for: " + command + " (" + costString + ")");
                    }

                } else {
                    ess.getLogger().warning("Invalid command cost for: " + command);
                }
            }
            return newSection;
        }
        return null;
    }

    @Override
    public BigDecimal getCommandCost(String name) {
        name = name.replace('.', '_').replace('/', '_');
        if (commandCosts != null) {
            return EssentialsConf.toBigDecimal(commandCosts.getString(name), BigDecimal.ZERO);
        }
        return BigDecimal.ZERO;
    }

    private Set<String> socialSpyCommands = new HashSet<String>();

    private Set<String> _getSocialSpyCommands() {
        Set<String> socialspyCommands = new HashSet<String>();

        if (config.isList("socialspy-commands")) {
            for (String c : config.getStringList("socialspy-commands")) {
                socialspyCommands.add(c.toLowerCase(Locale.ENGLISH));
            }
        } else {
            socialspyCommands.addAll(Arrays.asList("msg", "r", "mail", "m", "whisper", "emsg", "t", "tell", "er", "reply", "ereply", "email", "action", "describe", "eme", "eaction", "edescribe", "etell", "ewhisper", "pm"));
        }

        return socialspyCommands;
    }

    @Override
    public Set<String> getSocialSpyCommands() {
        return socialSpyCommands;
    }

    @Override
    public boolean getSocialSpyListenMutedPlayers() {
        return config.getBoolean("socialspy-listen-muted-players", true);
    }

    private Set<String> muteCommands = new HashSet<String>();

    private Set<String> _getMuteCommands() {
        Set<String> muteCommands = new HashSet<String>();
        if (config.isList("mute-commands")) {
            for (String s : config.getStringList("mute-commands")) {
                muteCommands.add(s.toLowerCase(Locale.ENGLISH));
            }
        }

        return muteCommands;
    }

    @Override
    public Set<String> getMuteCommands() {
        return muteCommands;
    }

    private String nicknamePrefix = "~";

    private String _getNicknamePrefix() {
        return config.getString("nickname-prefix", "~");
    }

    @Override
    public String getNicknamePrefix() {
        return nicknamePrefix;
    }

    @Override
    public double getTeleportCooldown() {
        return config.getDouble("teleport-cooldown", 0);
    }

    @Override
    public double getHealCooldown() {
        return config.getDouble("heal-cooldown", 0);
    }

    @Override
    public ConfigurationSection getKits() {
        return ess.getKits().getKits();
    }

    @Override
    public Map<String, Object> getKit(String name) {
        return ess.getKits().getKit(name);
    }

    @Override
    public void addKit(String name, List<String> lines, long delay) {
        ess.getKits().addKit(name, lines, delay);
    }

    @Override
    public ConfigurationSection getKitSection() {
        return config.getConfigurationSection("kits");
    }

    @Override
    public boolean isSkippingUsedOneTimeKitsFromKitList() {
        return config.getBoolean("skip-used-one-time-kits-from-kit-list", false);
    }

    private ChatColor operatorColor = null;

    @Override
    public ChatColor getOperatorColor() {
        return operatorColor;
    }

    private ChatColor _getOperatorColor() {
        String colorName = config.getString("ops-name-color", null);

        if (colorName == null) {
            return ChatColor.DARK_RED;
        }
        if ("none".equalsIgnoreCase(colorName) || colorName.isEmpty()) {
            return null;
        }

        try {
            return ChatColor.valueOf(colorName.toUpperCase(Locale.ENGLISH));
        } catch (IllegalArgumentException ex) {
        }

        return ChatColor.getByChar(colorName);
    }

    @Override
    public int getSpawnMobLimit() {
        return config.getInt("spawnmob-limit", 10);
    }

    @Override
    public boolean showNonEssCommandsInHelp() {
        return config.getBoolean("non-ess-in-help", true);
    }

    @Override
    public boolean hidePermissionlessHelp() {
        return config.getBoolean("hide-permissionless-help", true);
    }

    @Override
    public int getProtectCreeperMaxHeight() {
        return config.getInt("protect.creeper.max-height", -1);
    }

    @Override
    public boolean areSignsDisabled() {
        return !signsEnabled;
    }

    @Override
    public long getBackupInterval() {
        return config.getInt("backup.interval", 1440); // 1440 = 24 * 60
    }

    @Override
    public String getBackupCommand() {
        return config.getString("backup.command", null);
    }

    private final Map<String, String> chatFormats = Collections.synchronizedMap(new HashMap<String, String>());

    @Override
    public String getChatFormat(String group) {
        String mFormat = chatFormats.get(group);
        if (mFormat == null) {
            mFormat = config.getString("chat.group-formats." + (group == null ? "Default" : group), config.getString("chat.format", "&7[{GROUP}]&r {DISPLAYNAME}&7:&r {MESSAGE}"));
            mFormat = FormatUtil.replaceFormat(mFormat);
            mFormat = mFormat.replace("{DISPLAYNAME}", "%1$s");
            mFormat = mFormat.replace("{MESSAGE}", "%2$s");
            mFormat = mFormat.replace("{GROUP}", "{0}");
            mFormat = mFormat.replace("{WORLD}", "{1}");
            mFormat = mFormat.replace("{WORLDNAME}", "{1}");
            mFormat = mFormat.replace("{SHORTWORLDNAME}", "{2}");
            mFormat = mFormat.replace("{TEAMPREFIX}", "{3}");
            mFormat = mFormat.replace("{TEAMSUFFIX}", "{4}");
            mFormat = mFormat.replace("{TEAMNAME}", "{5}");
            mFormat = mFormat.replace("{PREFIX}", "{6}");
            mFormat = mFormat.replace("{SUFFIX}", "{7}");
            mFormat = "§r".concat(mFormat);
            chatFormats.put(group, mFormat);
        }
        if (isDebug()) {
            ess.getLogger().info(String.format("Found format '%s' for group '%s'", mFormat, group));
        }
        return mFormat;
    }

    @Override
    public boolean getAnnounceNewPlayers() {
        return !config.getString("newbies.announce-format", "-").isEmpty();
    }

    @Override
    public IText getAnnounceNewPlayerFormat() {
        return new SimpleTextInput(FormatUtil.replaceFormat(config.getString("newbies.announce-format", "&dWelcome {DISPLAYNAME} to the server!")));
    }

    @Override
    public String getNewPlayerKit() {
        return config.getString("newbies.kit", "");
    }

    @Override
    public String getNewbieSpawn() {
        return config.getString("newbies.spawnpoint", "default");
    }

    @Override
    public boolean getPerWarpPermission() {
        return config.getBoolean("per-warp-permission", false);
    }

    @Override
    public Map<String, Object> getListGroupConfig() {
        if (config.isConfigurationSection("list")) {
            Map<String, Object> values = config.getConfigurationSection("list").getValues(false);
            if (!values.isEmpty()) {
                return values;
            }
        }
        Map<String, Object> defaultMap = new HashMap<String, Object>();
        if (config.getBoolean("sort-list-by-groups", false)) {
            defaultMap.put("ListByGroup", "ListByGroup");
        } else {
            defaultMap.put("Players", "*");
        }
        return defaultMap;
    }

    @Override
    public void reloadConfig() {
        config.load();
        noGodWorlds = new HashSet<String>(config.getStringList("no-god-in-worlds"));
        enabledSigns = _getEnabledSigns();
        teleportSafety = _isTeleportSafetyEnabled();
        forceDisableTeleportSafety = _isForceDisableTeleportSafety();
        teleportInvulnerabilityTime = _getTeleportInvulnerability();
        teleportInvulnerability = _isTeleportInvulnerability();
        disableItemPickupWhileAfk = _getDisableItemPickupWhileAfk();
        registerBackInListener = _registerBackInListener();
        cancelAfkOnInteract = _cancelAfkOnInteract();
        cancelAfkOnMove = _cancelAfkOnMove();
        getFreezeAfkPlayers = _getFreezeAfkPlayers();
        sleepIgnoresAfkPlayers = _sleepIgnoresAfkPlayers();
        afkListName = _getAfkListName();
        isAfkListName = !afkListName.equalsIgnoreCase("none");
        itemSpawnBl = _getItemSpawnBlacklist();
        loginAttackDelay = _getLoginAttackDelay();
        signUsePerSecond = _getSignUsePerSecond();
        chatFormats.clear();
        changeDisplayName = _changeDisplayName();
        disabledCommands = getDisabledCommands();
        nicknamePrefix = _getNicknamePrefix();
        operatorColor = _getOperatorColor();
        changePlayerListName = _changePlayerListName();
        configDebug = _isDebug();
        prefixsuffixconfigured = _isPrefixSuffixConfigured();
        addprefixsuffix = _addPrefixSuffix();
        disablePrefix = _disablePrefix();
        disableSuffix = _disableSuffix();
        chatRadius = _getChatRadius();
        chatShout = _getChatShout();
        chatQuestion = _getChatQuestion();
        commandCosts = _getCommandCosts();
        socialSpyCommands = _getSocialSpyCommands();
        warnOnBuildDisallow = _warnOnBuildDisallow();
        mailsPerMinute = _getMailsPerMinute();
        maxMoney = _getMaxMoney();
        minMoney = _getMinMoney();
        permissionsLagWarning = _getPermissionsLagWarning();
        economyLagWarning = _getEconomyLagWarning();
        economyLog = _isEcoLogEnabled();
        economyLogUpdate = _isEcoLogUpdateEnabled();
        economyDisabled = _isEcoDisabled();
        allowSilentJoin = _allowSilentJoinQuit();
        customJoinMessage = _getCustomJoinMessage();
        isCustomJoinMessage = !customJoinMessage.equals("none");
        customQuitMessage = _getCustomQuitMessage();
        isCustomQuitMessage = !customQuitMessage.equals("none");
        muteCommands = _getMuteCommands();
        spawnOnJoinGroups = _getSpawnOnJoinGroups();
        commandCooldowns = _getCommandCooldowns();
        npcsInBalanceRanking = _isNpcsInBalanceRanking();
        currencyFormat = _getCurrencyFormat();
        unprotectedSigns = _getUnprotectedSign();
        defaultEnabledConfirmCommands = _getDefaultEnabledConfirmCommands();
        teleportBackWhenFreedFromJail = _isTeleportBackWhenFreedFromJail();
        isCompassTowardsHomePerm = _isCompassTowardsHomePerm();
        isAllowWorldInBroadcastworld = _isAllowWorldInBroadcastworld();
        itemDbType = _getItemDbType();
        forceEnableRecipe = _isForceEnableRecipe();
        allowOldIdSigns = _allowOldIdSigns();
    }

    void _lateLoadItemSpawnBlacklist() {
        itemSpawnBl = _getItemSpawnBlacklist();
    }

    private List<Material> itemSpawnBl = new ArrayList<>();

    @Override
    public List<Material> itemSpawnBlacklist() {
        return itemSpawnBl;
    }

    private List<Material> _getItemSpawnBlacklist() {
        final List<Material> epItemSpwn = new ArrayList<>();
        final IItemDb itemDb = ess.getItemDb();
        if (itemDb == null || !itemDb.isReady()) {
            logger.log(Level.FINE, "Skipping item spawn blacklist read; item DB not yet loaded.");
            return epItemSpwn;
        }
        for (String itemName : config.getString("item-spawn-blacklist", "").split(",")) {
            itemName = itemName.trim();
            if (itemName.isEmpty()) {
                continue;
            }
            try {
                final ItemStack iStack = itemDb.get(itemName);
                epItemSpwn.add(iStack.getType());
            } catch (Exception ex) {
                logger.log(Level.SEVERE, tl("unknownItemInList", itemName, "item-spawn-blacklist"), ex);
            }
        }
        return epItemSpwn;
    }

    private List<EssentialsSign> enabledSigns = new ArrayList<EssentialsSign>();
    private boolean signsEnabled = false;

    @Override
    public List<EssentialsSign> enabledSigns() {
        return enabledSigns;
    }

    private List<EssentialsSign> _getEnabledSigns() {
        this.signsEnabled = false; // Ensure boolean resets on reload.

        List<EssentialsSign> newSigns = new ArrayList<EssentialsSign>();

        for (String signName : config.getStringList("enabledSigns")) {
            signName = signName.trim().toUpperCase(Locale.ENGLISH);
            if (signName.isEmpty()) {
                continue;
            }
            if (signName.equals("COLOR") || signName.equals("COLOUR")) {
                signsEnabled = true;
                continue;
            }
            try {
                newSigns.add(Signs.valueOf(signName).getSign());
            } catch (Exception ex) {
                logger.log(Level.SEVERE, tl("unknownItemInList", signName, "enabledSigns"));
                continue;
            }
            signsEnabled = true;
        }
        return newSigns;
    }

    private boolean warnOnBuildDisallow;

    private boolean _warnOnBuildDisallow() {
        return config.getBoolean("protect.disable.warn-on-build-disallow", false);
    }

    @Override
    public boolean warnOnBuildDisallow() {
        return warnOnBuildDisallow;
    }

    private boolean debug = false;
    private boolean configDebug = false;

    private boolean _isDebug() {
        return config.getBoolean("debug", false);
    }

    @Override
    public boolean isDebug() {
        return debug || configDebug;
    }

    @Override
    public boolean warnOnSmite() {
        return config.getBoolean("warn-on-smite", true);
    }

    @Override
    public boolean permissionBasedItemSpawn() {
        return config.getBoolean("permission-based-item-spawn", false);
    }

    @Override
    public String getLocale() {
        return config.getString("locale", "");
    }

    //This method should always only return one character due to the implementation of the calling methods
    //If you need to use a string currency, for example "coins", use the translation key 'currency'.
    @Override
    public String getCurrencySymbol() {
        return config.getString("currency-symbol", "$").concat("$").substring(0, 1).replaceAll("[0-9]", "$");
    }

    // #easteregg
    @Override
    @Deprecated
    public boolean isTradeInStacks(int id) {
        return config.getBoolean("trade-in-stacks-" + id, false);
    }

    // #easteregg
    @Override
    public boolean isTradeInStacks(Material type) {
        return config.getBoolean("trade-in-stacks." + type.toString().toLowerCase().replace("_", ""), false);
    }

    // #easteregg
    private boolean economyDisabled = false;

    public boolean _isEcoDisabled() {
        return config.getBoolean("disable-eco", false);
    }

    @Override
    public boolean isEcoDisabled() {
        return economyDisabled;
    }

    @Override
    public boolean getProtectPreventSpawn(final String creatureName) {
        return config.getBoolean("protect.prevent.spawn." + creatureName, false);
    }

    @Override
    public List<Material> getProtectList(final String configName) {
        final List<Material> list = new ArrayList<>();
        for (String itemName : config.getString(configName, "").split(",")) {
            itemName = itemName.trim();
            if (itemName.isEmpty()) {
                continue;
            }

            Material mat = EnumUtil.getMaterial(itemName.toUpperCase());
            
            if (mat == null) {
                try {
                    ItemStack itemStack = ess.getItemDb().get(itemName);
                    mat = itemStack.getType();
                } catch (Exception ignored) {}
            }

            if (mat == null) {
                logger.log(Level.SEVERE, tl("unknownItemInList", itemName, configName));
            } else {
                list.add(mat);
            }
        }
        return list;
    }

    @Override
    public String getProtectString(final String configName) {
        return config.getString(configName, null);
    }

    @Override
    public boolean getProtectBoolean(final String configName, boolean def) {
        return config.getBoolean(configName, def);
    }

    private static final BigDecimal MAXMONEY = new BigDecimal("10000000000000");
    private BigDecimal maxMoney = MAXMONEY;

    private BigDecimal _getMaxMoney() {
        return config.getBigDecimal("max-money", MAXMONEY);
    }

    @Override
    public BigDecimal getMaxMoney() {
        return maxMoney;
    }

    private static final BigDecimal MINMONEY = new BigDecimal("-10000000000000");
    private BigDecimal minMoney = MINMONEY;

    private BigDecimal _getMinMoney() {
        BigDecimal min = config.getBigDecimal("min-money", MINMONEY);
        if (min.signum() > 0) {
            min = min.negate();
        }
        return min;
    }

    @Override
    public BigDecimal getMinMoney() {
        return minMoney;
    }

    private boolean economyLog = false;

    @Override
    public boolean isEcoLogEnabled() {
        return economyLog;
    }

    public boolean _isEcoLogEnabled() {
        return config.getBoolean("economy-log-enabled", false);
    }

    // #easteregg
    private boolean economyLogUpdate = false;

    @Override
    public boolean isEcoLogUpdateEnabled() {
        return economyLogUpdate;
    }

    public boolean _isEcoLogUpdateEnabled() {
        return config.getBoolean("economy-log-update-enabled", false);
    }

    @Override
    public boolean removeGodOnDisconnect() {
        return config.getBoolean("remove-god-on-disconnect", false);
    }

    private boolean changeDisplayName = true;

    private boolean _changeDisplayName() {
        return config.getBoolean("change-displayname", true);
    }

    @Override
    public boolean changeDisplayName() {
        return changeDisplayName;
    }

    private boolean changeLocale = true;

    private boolean _changeLocale() {
        return config.getBoolean("change-locale", true);
    }

    @Override
    public boolean changeLocale() {
        return changeLocale;
    }

    private boolean changePlayerListName = false;

    private boolean _changePlayerListName() {
        return config.getBoolean("change-playerlist", false);
    }

    @Override
    public boolean changePlayerListName() {
        return changePlayerListName;
    }

    @Override
    public boolean useBukkitPermissions() {
        return config.getBoolean("use-bukkit-permissions", false);
    }

    private boolean prefixsuffixconfigured = false;
    private boolean addprefixsuffix = false;
    private boolean essentialsChatActive = false;

    private boolean _addPrefixSuffix() {
        return config.getBoolean("add-prefix-suffix", false);
    }

    private boolean _isPrefixSuffixConfigured() {
        return config.hasProperty("add-prefix-suffix");
    }

    @Override
    public void setEssentialsChatActive(boolean essentialsChatActive) {
        this.essentialsChatActive = essentialsChatActive;
    }

    @Override
    public boolean addPrefixSuffix() {
        return prefixsuffixconfigured ? addprefixsuffix : essentialsChatActive;
    }

    // #easteregg
    private boolean disablePrefix = false;

    private boolean _disablePrefix() {
        return config.getBoolean("disablePrefix", false);
    }

    @Override
    public boolean disablePrefix() {
        return disablePrefix;
    }

    // #easteregg
    private boolean disableSuffix = false;

    private boolean _disableSuffix() {
        return config.getBoolean("disableSuffix", false);
    }

    @Override
    public boolean disableSuffix() {
        return disableSuffix;
    }

    @Override
    public long getAutoAfk() {
        return config.getLong("auto-afk", 300);
    }

    @Override
    public long getAutoAfkKick() {
        return config.getLong("auto-afk-kick", -1);
    }

    private boolean getFreezeAfkPlayers;

    @Override
    public boolean getFreezeAfkPlayers() {
        return getFreezeAfkPlayers;
    }

    private boolean _getFreezeAfkPlayers() {
        return config.getBoolean("freeze-afk-players", false);
    }

    private boolean cancelAfkOnMove;

    @Override
    public boolean cancelAfkOnMove() {
        return cancelAfkOnMove;
    }

    private boolean _cancelAfkOnMove() {
        return config.getBoolean("cancel-afk-on-move", true);
    }

    private boolean cancelAfkOnInteract;

    @Override
    public boolean cancelAfkOnInteract() {
        return cancelAfkOnInteract;
    }

    private boolean _cancelAfkOnInteract() {
        return config.getBoolean("cancel-afk-on-interact", true);
    }

    private boolean sleepIgnoresAfkPlayers;

    @Override
    public boolean sleepIgnoresAfkPlayers() {
        return sleepIgnoresAfkPlayers;
    }

    private boolean _sleepIgnoresAfkPlayers() {
        return config.getBoolean("sleep-ignores-afk-players", true);
    }

    private String afkListName;
    private boolean isAfkListName;

    public String _getAfkListName() {
        return FormatUtil.replaceFormat(config.getString("afk-list-name", "none"));
    }

    @Override
    public boolean isAfkListName() {
        return isAfkListName;
    }

    @Override
    public String getAfkListName() {
        return afkListName;
    }

    @Override
    public boolean areDeathMessagesEnabled() {
        return config.getBoolean("death-messages", true);
    }

    private Set<String> noGodWorlds = new HashSet<String>();

    @Override
    public Set<String> getNoGodWorlds() {
        return noGodWorlds;
    }

    @Override
    public void setDebug(final boolean debug) {
        this.debug = debug;
    }

    @Override
    public boolean getRepairEnchanted() {
        return config.getBoolean("repair-enchanted", true);
    }

    @Override
    public boolean allowUnsafeEnchantments() {
        return config.getBoolean("unsafe-enchantments", false);
    }

    @Override
    public boolean isWorldTeleportPermissions() {
        return config.getBoolean("world-teleport-permissions", false);
    }

    @Override
    public boolean isWorldHomePermissions() {
        return config.getBoolean("world-home-permissions", false);
    }

    private boolean registerBackInListener;

    @Override
    public boolean registerBackInListener() {
        return registerBackInListener;
    }

    private boolean _registerBackInListener() {
        return config.getBoolean("register-back-in-listener", false);
    }

    private boolean disableItemPickupWhileAfk;

    @Override
    public boolean getDisableItemPickupWhileAfk() {
        return disableItemPickupWhileAfk;
    }

    private boolean _getDisableItemPickupWhileAfk() {
        return config.getBoolean("disable-item-pickup-while-afk", false);
    }

    private EventPriority getPriority(String priority) {
        if ("none".equals(priority)) {
            return null;
        }
        if ("lowest".equals(priority)) {
            return EventPriority.LOWEST;
        }
        if ("low".equals(priority)) {
            return EventPriority.LOW;
        }
        if ("normal".equals(priority)) {
            return EventPriority.NORMAL;
        }
        if ("high".equals(priority)) {
            return EventPriority.HIGH;
        }
        if ("highest".equals(priority)) {
            return EventPriority.HIGHEST;
        }
        return EventPriority.NORMAL;
    }

    @Override
    public EventPriority getRespawnPriority() {
        String priority = config.getString("respawn-listener-priority", "normal").toLowerCase(Locale.ENGLISH);
        return getPriority(priority);
    }

    @Override
    public EventPriority getSpawnJoinPriority() {
        String priority = config.getString("spawn-join-listener-priority", "normal").toLowerCase(Locale.ENGLISH);
        return getPriority(priority);
    }

    @Override
    public long getTpaAcceptCancellation() {
        return config.getLong("tpa-accept-cancellation", 120);
    }

    private long teleportInvulnerabilityTime;

    private long _getTeleportInvulnerability() {
        return config.getLong("teleport-invulnerability", 0) * 1000;
    }

    @Override
    public long getTeleportInvulnerability() {
        return teleportInvulnerabilityTime;
    }

    private boolean teleportInvulnerability;

    private boolean _isTeleportInvulnerability() {
        return (config.getLong("teleport-invulnerability", 0) > 0);
    }

    @Override
    public boolean isTeleportInvulnerability() {
        return teleportInvulnerability;
    }

    private long loginAttackDelay;

    private long _getLoginAttackDelay() {
        return config.getLong("login-attack-delay", 0) * 1000;
    }

    @Override
    public long getLoginAttackDelay() {
        return loginAttackDelay;
    }

    private int signUsePerSecond;

    private int _getSignUsePerSecond() {
        final int perSec = config.getInt("sign-use-per-second", 4);
        return perSec > 0 ? perSec : 1;
    }

    @Override
    public int getSignUsePerSecond() {
        return signUsePerSecond;
    }

    @Override
    public double getMaxFlySpeed() {
        double maxSpeed = config.getDouble("max-fly-speed", 0.8);
        return maxSpeed > 1.0 ? 1.0 : Math.abs(maxSpeed);
    }

    @Override
    public double getMaxWalkSpeed() {
        double maxSpeed = config.getDouble("max-walk-speed", 0.8);
        return maxSpeed > 1.0 ? 1.0 : Math.abs(maxSpeed);
    }

    private int mailsPerMinute;

    private int _getMailsPerMinute() {
        return config.getInt("mails-per-minute", 1000);
    }

    @Override
    public int getMailsPerMinute() {
        return mailsPerMinute;
    }

    // #easteregg
    private long economyLagWarning;

    private long _getEconomyLagWarning() {
        // Default to 25ms
        final long value = (long) (config.getDouble("economy-lag-warning", 25.0) * 1000000);
        return value;
    }

    @Override
    public long getEconomyLagWarning() {
        return economyLagWarning;
    }

    // #easteregg
    private long permissionsLagWarning;

    private long _getPermissionsLagWarning() {
        // Default to 25ms
        final long value = (long) (config.getDouble("permissions-lag-warning", 25.0) * 1000000);
        return value;
    }

    @Override
    public long getPermissionsLagWarning() {
        return permissionsLagWarning;
    }

    @Override
    public long getMaxTempban() {
        return config.getLong("max-tempban-time", -1);
    }

    @Override
    public int getMaxNickLength() {
        return config.getInt("max-nick-length", 30);
    }

    @Override
    public boolean ignoreColorsInMaxLength() {
        return config.getBoolean("ignore-colors-in-max-nick-length", false);
    }

    @Override
    public boolean hideDisplayNameInVanish() {
        return config.getBoolean("hide-displayname-in-vanish", false);
    }

    private boolean allowSilentJoin;

    public boolean _allowSilentJoinQuit() {
        return config.getBoolean("allow-silent-join-quit", false);
    }

    @Override
    public boolean allowSilentJoinQuit() {
        return allowSilentJoin;
    }

    private String customJoinMessage;
    private boolean isCustomJoinMessage;

    public String _getCustomJoinMessage() {
        return FormatUtil.replaceFormat(config.getString("custom-join-message", "none"));
    }

    @Override
    public String getCustomJoinMessage() {
        return customJoinMessage;
    }

    @Override
    public boolean isCustomJoinMessage() {
        return isCustomJoinMessage;
    }

    private String customQuitMessage;
    private boolean isCustomQuitMessage;

    public String _getCustomQuitMessage() {
        return FormatUtil.replaceFormat(config.getString("custom-quit-message", "none"));
    }

    @Override
    public String getCustomQuitMessage() {
        return customQuitMessage;
    }

    @Override
    public boolean isCustomQuitMessage() {
        return isCustomQuitMessage;
    }

    @Override
    public boolean isNotifyNoNewMail() {
        return config.getBoolean("notify-no-new-mail", true);
    }

    @Override
    public boolean isDropItemsIfFull() {
        return config.getBoolean("drop-items-if-full", false);
    }

    // #easteregg
    @Override
    public int getMaxUserCacheCount() {
        long count = Runtime.getRuntime().maxMemory() / 1024 / 96;
        return config.getInt("max-user-cache-count", (int) count);
    }

    @Override public boolean isLastMessageReplyRecipient() {
        return config.getBoolean("last-message-reply-recipient", false);
    }

    @Override public BigDecimal getMinimumPayAmount() {
        return new BigDecimal(config.getString("minimum-pay-amount", "0.001"));
    }

    @Override public long getLastMessageReplyRecipientTimeout() {
        return config.getLong("last-message-reply-recipient-timeout", 180);
    }

    @Override public boolean isMilkBucketEasterEggEnabled() {
        return config.getBoolean("milk-bucket-easter-egg", true);
    }

    @Override public boolean isSendFlyEnableOnJoin() {
        return config.getBoolean("send-fly-enable-on-join", true);
    }

    @Override
    public boolean isWorldTimePermissions() {
        return config.getBoolean("world-time-permissions", false);
    }

    @Override
    public boolean isSpawnOnJoin() {
        return !this.spawnOnJoinGroups.isEmpty();
    }

    private List<String> spawnOnJoinGroups;

    public List<String> _getSpawnOnJoinGroups() {
        List<String> def = Collections.emptyList();
        if (config.isSet("spawn-on-join")) {
            if (config.isList("spawn-on-join")) {
                return new ArrayList<>(config.getStringList("spawn-on-join"));
            } else if (config.isBoolean("spawn-on-join")) { // List of [*] to make all groups go to spawn on join.
                // This also maintains backwards compatibility with initial impl of single boolean value.
                return config.getBoolean("spawn-on-join") ? Collections.singletonList("*") : def;
            }
            // Take whatever the value is, convert to string and add it to a list as a single value.
            String val = config.get("spawn-on-join").toString();
            return !val.isEmpty() ? Collections.singletonList(val) : def;
        } else {
            return def;
        }
    }

    @Override
    public List<String> getSpawnOnJoinGroups() {
        return this.spawnOnJoinGroups;
    }

    @Override
    public boolean isUserInSpawnOnJoinGroup(IUser user) {
        for (String group : this.spawnOnJoinGroups) {
            if (group.equals("*") || user.inGroup(group)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean isTeleportToCenterLocation() {
        return config.getBoolean("teleport-to-center", true);
    }

    private Map<Pattern, Long> commandCooldowns;

    private Map<Pattern, Long> _getCommandCooldowns() {
        if (!config.isConfigurationSection("command-cooldowns")) {
            return null;
        }
        ConfigurationSection section = config.getConfigurationSection("command-cooldowns");
        Map<Pattern, Long> result = new LinkedHashMap<>();
        for (String cmdEntry : section.getKeys(false)) {
            Pattern pattern = null;

            /* ================================
             * >> Regex
             * ================================ */
            if (cmdEntry.startsWith("^")) {
                try {
                    pattern = Pattern.compile(cmdEntry.substring(1));
                } catch (PatternSyntaxException e) {
                    ess.getLogger().warning("Command cooldown error: " + e.getMessage());
                }
            } else {
                // Escape above Regex
                if (cmdEntry.startsWith("\\^")) {
                    cmdEntry = cmdEntry.substring(1);
                }
                String cmd = cmdEntry
                        .replaceAll("\\*", ".*"); // Wildcards are accepted as asterisk * as known universally.
                pattern = Pattern.compile(cmd + "( .*)?"); // This matches arguments, if present, to "ignore" them from the feature.
            }

            /* ================================
             * >> Process cooldown value
             * ================================ */
            Object value = section.get(cmdEntry);
            if (!(value instanceof Number) && value instanceof String) {
                try {
                    value = Double.parseDouble(value.toString());
                } catch (NumberFormatException ignored) {
                }
            }
            if (!(value instanceof Number)) {
                ess.getLogger().warning("Command cooldown error: '" + value + "' is not a valid cooldown");
                continue;
            }
            double cooldown = ((Number) value).doubleValue();
            if (cooldown < 1) {
                ess.getLogger().warning("Command cooldown with very short " + cooldown + " cooldown.");
            }

            result.put(pattern, (long) cooldown * 1000); // convert to milliseconds
        }
        return result;
    }

    @Override
    public boolean isCommandCooldownsEnabled() {
        return commandCooldowns != null;
    }

    @Override
    public long getCommandCooldownMs(String label) {
        Entry<Pattern, Long> result = getCommandCooldownEntry(label);
        return result != null ? result.getValue() : -1; // return cooldown in milliseconds
    }

    @Override
    public Entry<Pattern, Long> getCommandCooldownEntry(String label) {
        if (isCommandCooldownsEnabled()) {
            for (Entry<Pattern, Long> entry : this.commandCooldowns.entrySet()) {
                // Check if label matches current pattern (command-cooldown in config)
                boolean matches = entry.getKey().matcher(label).matches();
                if (isDebug()) {
                    ess.getLogger().info(String.format("Checking command '%s' against cooldown '%s': %s", label, entry.getKey(), matches));
                }

                if (matches) {
                    return entry;
                }
            }
        }
        return null;
    }

    @Override
    public boolean isCommandCooldownPersistent(String label) {
        // TODO: enable per command cooldown specification for persistence.
        return config.getBoolean("command-cooldown-persistence", true);
    }

    private boolean npcsInBalanceRanking = false;

    private boolean _isNpcsInBalanceRanking() {
        return config.getBoolean("npcs-in-balance-ranking", false);
    }

    @Override
    public boolean isNpcsInBalanceRanking() {
        return npcsInBalanceRanking;
    }

    private NumberFormat currencyFormat;

    private NumberFormat _getCurrencyFormat() {
        String currencyFormatString = config.getString("currency-format", "#,##0.00");

        String symbolLocaleString = config.getString("currency-symbol-format-locale");
        DecimalFormatSymbols decimalFormatSymbols;
        if (symbolLocaleString != null) {
            decimalFormatSymbols = DecimalFormatSymbols.getInstance(Locale.forLanguageTag(symbolLocaleString));
        } else {
            // Fallback to the JVM's default locale
            decimalFormatSymbols = DecimalFormatSymbols.getInstance(Locale.US);
        }

        DecimalFormat currencyFormat = new DecimalFormat(currencyFormatString, decimalFormatSymbols);
        currencyFormat.setRoundingMode(RoundingMode.FLOOR);

        // Updates NumberUtil#PRETTY_FORMAT field so that all of Essentials can follow a single format.
        NumberUtil.internalSetPrettyFormat(currencyFormat);
        return currencyFormat;
    }

    @Override
    public NumberFormat getCurrencyFormat() {
        return this.currencyFormat;
    }

    private List<EssentialsSign> unprotectedSigns = Collections.emptyList();

    @Override
    public List<EssentialsSign> getUnprotectedSignNames() {
        return this.unprotectedSigns;
    }

    private List<EssentialsSign> _getUnprotectedSign() {
        List<EssentialsSign> newSigns = new ArrayList<>();

        for (String signName : config.getStringList("unprotected-sign-names")) {
            signName = signName.trim().toUpperCase(Locale.ENGLISH);
            if (signName.isEmpty()) {
                continue;
            }
            try {
                newSigns.add(Signs.valueOf(signName).getSign());
            } catch (Exception ex) {
                logger.log(Level.SEVERE, tl("unknownItemInList", signName, "unprotected-sign-names"));
                continue;
            }
        }
        return newSigns;
    }

    @Override
    public boolean isPastebinCreateKit() {
        return config.getBoolean("pastebin-createkit", false);
    }

    @Override
    public boolean isAllowBulkBuySell() {
        return config.getBoolean("allow-bulk-buy-sell", false);
    }

    @Override
    public boolean isAddingPrefixInPlayerlist() {
        return config.getBoolean("add-prefix-in-playerlist", false);
    }

    @Override
    public boolean isAddingSuffixInPlayerlist() {
        return config.getBoolean("add-suffix-in-playerlist", false);
    }

    @Override
    public int getNotifyPlayerOfMailCooldown() {
        return config.getInt("notify-player-of-mail-cooldown", 0);
    }

    @Override
    public int getMotdDelay() {
        return config.getInt("delay-motd", 0);
    }

    @Override
    public boolean isDirectHatAllowed() {
        return config.getBoolean("allow-direct-hat", true);
    }

    private List<String> defaultEnabledConfirmCommands;

    private List<String> _getDefaultEnabledConfirmCommands() {
        List<String> commands = config.getStringList("default-enabled-confirm-commands");
        for (int i = 0; i < commands.size(); i++) {
            commands.set(i, commands.get(i).toLowerCase());
        }
        return commands;
    }

    @Override
    public List<String> getDefaultEnabledConfirmCommands() {
        return defaultEnabledConfirmCommands;
    }
    
    @Override
    public boolean isConfirmCommandEnabledByDefault(String commandName) {
        return getDefaultEnabledConfirmCommands().contains(commandName.toLowerCase());
    }

    private boolean teleportBackWhenFreedFromJail;

    private boolean _isTeleportBackWhenFreedFromJail() {
        return config.getBoolean("teleport-back-when-freed-from-jail", true);
    }

    @Override
    public boolean isTeleportBackWhenFreedFromJail() {
        return teleportBackWhenFreedFromJail;
    }

    private boolean isCompassTowardsHomePerm;

    private boolean _isCompassTowardsHomePerm() {
        return config.getBoolean("compass-towards-home-perm", false);
    }

    @Override
    public boolean isCompassTowardsHomePerm() {
        return isCompassTowardsHomePerm;
    }

    private boolean isAllowWorldInBroadcastworld;

    private boolean _isAllowWorldInBroadcastworld() {
        return config.getBoolean("allow-world-in-broadcastworld", false);
    }

    @Override
    public boolean isAllowWorldInBroadcastworld() {
        return isAllowWorldInBroadcastworld;
    }

    private String itemDbType; // #EasterEgg - admins can manually switch items provider if they want

    private String _getItemDbType() {
        return config.getString("item-db-type", "auto");
    }

    @Override
    public String getItemDbType() {
        return itemDbType;
    }

    private boolean forceEnableRecipe; // https://github.com/EssentialsX/Essentials/issues/1397

    private boolean _isForceEnableRecipe() {
        return config.getBoolean("force-enable-recipe", false);
    }

    @Override
    public boolean isForceEnableRecipe() {
        return forceEnableRecipe;
    }

    private boolean allowOldIdSigns;

    private boolean _allowOldIdSigns() {
        return config.getBoolean("allow-old-id-signs", false);
    }

    @Override
    public boolean allowOldIdSigns() {
        return allowOldIdSigns;
    }
}
