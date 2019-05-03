package com.earth2me.essentials;

import com.earth2me.essentials.utils.NumberUtil;
import com.earth2me.essentials.utils.StringUtil;
import com.google.common.collect.ImmutableMap;
import net.ess3.api.IEssentials;
import net.ess3.api.InvalidWorldException;
import net.ess3.api.MaxMoneyException;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.math.BigDecimal;
import java.util.*;
import java.util.Map.Entry;
import java.util.regex.Pattern;

import static com.earth2me.essentials.I18n.tl;


public abstract class UserData extends PlayerExtension implements IConf {
    protected final transient IEssentials ess;
    private final EssentialsUserConf config;
    private final File folder;

    protected UserData(Player base, IEssentials ess) {
        super(base);
        this.ess = ess;
        folder = new File(ess.getDataFolder(), "userdata");
        if (!folder.exists()) {
            folder.mkdirs();
        }

        String filename;
        try {
            filename = base.getUniqueId().toString();
        } catch (Throwable ex) {
            ess.getLogger().warning("Falling back to old username system for " + base.getName());
            filename = base.getName();
        }

        config = new EssentialsUserConf(base.getName(), base.getUniqueId(), new File(folder, filename + ".yml"));
        reloadConfig();
    }

    public final void reset() {
        config.forceSave();
        config.getFile().delete();
        if (config.username != null) {
            ess.getUserMap().removeUser(config.username);
        }
    }

    public final void cleanup() {
        config.cleanup();
    }

    @Override
    public final void reloadConfig() {
        config.load();
        money = _getMoney();
        unlimited = _getUnlimited();
        powertools = _getPowertools();
        homes = _getHomes();
        lastLocation = _getLastLocation();
        lastTeleportTimestamp = _getLastTeleportTimestamp();
        lastHealTimestamp = _getLastHealTimestamp();
        jail = _getJail();
        mails = _getMails();
        teleportEnabled = _getTeleportEnabled();
        godmode = _getGodModeEnabled();
        muted = _getMuted();
        muteTimeout = _getMuteTimeout();
        muteReason = _getMuteReason();
        jailed = _getJailed();
        jailTimeout = _getJailTimeout();
        lastLogin = _getLastLogin();
        lastLogout = _getLastLogout();
        lastLoginAddress = _getLastLoginAddress();
        afk = _getAfk();
        geolocation = _getGeoLocation();
        isSocialSpyEnabled = _isSocialSpyEnabled();
        isNPC = _isNPC();
        arePowerToolsEnabled = _arePowerToolsEnabled();
        kitTimestamps = _getKitTimestamps();
        nickname = _getNickname();
        ignoredPlayers = _getIgnoredPlayers();
        logoutLocation = _getLogoutLocation();
        lastAccountName = _getLastAccountName();
        commandCooldowns = _getCommandCooldowns();
        acceptingPay = _getAcceptingPay();
        confirmPay = _getConfirmPay();
        confirmClear = _getConfirmClear();
        lastMessageReplyRecipient = _getLastMessageReplyRecipient();
        locale = _getLocale();
    }

    private BigDecimal money;

    private BigDecimal _getMoney() {
        BigDecimal result = ess.getSettings().getStartingBalance();
        BigDecimal maxMoney = ess.getSettings().getMaxMoney();
        BigDecimal minMoney = ess.getSettings().getMinMoney();

        // NPC banks are not actual player banks, as such they do not have player starting balance.
        if (isNPC()) {
            result = BigDecimal.ZERO;
        }

        if (config.hasProperty("money")) {
            result = config.getBigDecimal("money", result);
        }
        if (result.compareTo(maxMoney) > 0) {
            result = maxMoney;
        }
        if (result.compareTo(minMoney) < 0) {
            result = minMoney;
        }
        return result;
    }

    public BigDecimal getMoney() {
        return money;
    }

    public void setMoney(BigDecimal value, boolean throwError) throws MaxMoneyException {
        BigDecimal maxMoney = ess.getSettings().getMaxMoney();
        BigDecimal minMoney = ess.getSettings().getMinMoney();
        if (value.compareTo(maxMoney) > 0) {
            if (throwError) {
                throw new MaxMoneyException();
            }
            money = maxMoney;
        } else {
            money = value;
        }
        if (money.compareTo(minMoney) < 0) {
            money = minMoney;
        }
        config.setProperty("money", money);
        stopTransaction();
    }

    private Map<String, Object> homes;

    private Map<String, Object> _getHomes() {
        if (config.isConfigurationSection("homes")) {
            return config.getConfigurationSection("homes").getValues(false);
        }
        return new HashMap<String, Object>();
    }

    private String getHomeName(String search) {
        if (NumberUtil.isInt(search)) {
            try {
                search = getHomes().get(Integer.parseInt(search) - 1);
            } catch (NumberFormatException e) {
            } catch (IndexOutOfBoundsException e) {
            }
        }
        return search;
    }

    public Location getHome(String name) throws Exception {
        String search = getHomeName(name);
        return config.getLocation("homes." + search, this.getBase().getServer());
    }

    public Location getHome(final Location world) {
        try {
            if (getHomes().isEmpty()) {
                return null;
            }
            Location loc;
            for (String home : getHomes()) {
                loc = config.getLocation("homes." + home, this.getBase().getServer());
                if (world.getWorld() == loc.getWorld()) {
                    return loc;
                }

            }
            loc = config.getLocation("homes." + getHomes().get(0), this.getBase().getServer());
            return loc;
        } catch (InvalidWorldException ex) {
            return null;
        }
    }

    public List<String> getHomes() {
        return new ArrayList<String>(homes.keySet());
    }

    public void setHome(String name, Location loc) {
        //Invalid names will corrupt the yaml
        name = StringUtil.safeString(name);
        homes.put(name, loc);
        config.setProperty("homes." + name, loc);
        config.save();
    }

    public void delHome(String name) throws Exception {
        String search = getHomeName(name);
        if (!homes.containsKey(search)) {
            search = StringUtil.safeString(search);
        }
        if (homes.containsKey(search)) {
            homes.remove(search);
            config.removeProperty("homes." + search);
            config.save();
        } else {
            throw new Exception(((User) this).tl("invalidHome", search));
        }
    }

    public boolean hasHome() {
        return config.hasProperty("home");
    }

    private String nickname;

    public String _getNickname() {
        return config.getString("nickname");
    }

    public String getNickname() {
        return nickname;
    }

    public void setNickname(String nick) {
        nickname = nick;
        config.setProperty("nickname", nick);
        config.save();
    }

    private List<Material> unlimited;

    private List<Material> _getUnlimited() {
        List<Material> retlist = new ArrayList<>();
        List<String> configList = config.getStringList("unlimited");
        for(String s : configList) {
            Material mat = Material.matchMaterial(s);
            if(mat != null) {
                retlist.add(mat);
            }
        }

        return retlist;
    }

    public List<Material> getUnlimited() {
        return unlimited;
    }

    public boolean hasUnlimited(ItemStack stack) {
        return unlimited.contains(stack.getType());
    }

    public void setUnlimited(ItemStack stack, boolean state) {
        if (unlimited.contains(stack.getType())) {
            unlimited.remove(stack.getType());
        }
        if (state) {
            unlimited.add(stack.getType());
        }
        config.setProperty("unlimited", unlimited);
        config.save();
    }

    private Map<String, Object> powertools;

    private Map<String, Object> _getPowertools() {
        if (config.isConfigurationSection("powertools")) {
            return config.getConfigurationSection("powertools").getValues(false);
        }
        return new HashMap<>();
    }

    public void clearAllPowertools() {
        powertools.clear();
        config.setProperty("powertools", powertools);
        config.save();
    }

    @SuppressWarnings("unchecked")
    public List<String> getPowertool(ItemStack stack) {
        return (List<String>) powertools.get(stack.getType().name().toLowerCase(Locale.ENGLISH));
    }

    @SuppressWarnings("unchecked")
    public List<String> getPowertool(Material material) {
        return (List<String>) powertools.get(material.name().toLowerCase(Locale.ENGLISH));
    }

    public void setPowertool(ItemStack stack, List<String> commandList) {
        if (commandList == null || commandList.isEmpty()) {
            powertools.remove(stack.getType().name().toLowerCase(Locale.ENGLISH));
        } else {
            powertools.put(stack.getType().name().toLowerCase(Locale.ENGLISH), commandList);
        }
        config.setProperty("powertools", powertools);
        config.save();
    }

    public boolean hasPowerTools() {
        return !powertools.isEmpty();
    }

    private Location lastLocation;

    private Location _getLastLocation() {
        try {
            return config.getLocation("lastlocation", this.getBase().getServer());
        } catch (InvalidWorldException e) {
            return null;
        }
    }

    public Location getLastLocation() {
        return lastLocation;
    }

    public void setLastLocation(Location loc) {
        if (loc == null || loc.getWorld() == null) {
            return;
        }
        lastLocation = loc;
        config.setProperty("lastlocation", loc);
        config.save();
    }

    private Location logoutLocation;

    private Location _getLogoutLocation() {
        try {
            return config.getLocation("logoutlocation", this.getBase().getServer());
        } catch (InvalidWorldException e) {
            return null;
        }
    }

    public Location getLogoutLocation() {
        return logoutLocation;
    }

    public void setLogoutLocation(Location loc) {
        if (loc == null || loc.getWorld() == null) {
            return;
        }
        logoutLocation = loc;
        config.setProperty("logoutlocation", loc);
        config.save();
    }

    private long lastTeleportTimestamp;

    private long _getLastTeleportTimestamp() {
        return config.getLong("timestamps.lastteleport", 0);
    }

    public long getLastTeleportTimestamp() {
        return lastTeleportTimestamp;
    }

    public void setLastTeleportTimestamp(long time) {
        lastTeleportTimestamp = time;
        config.setProperty("timestamps.lastteleport", time);
        config.save();
    }

    private long lastHealTimestamp;

    private long _getLastHealTimestamp() {
        return config.getLong("timestamps.lastheal", 0);
    }

    public long getLastHealTimestamp() {
        return lastHealTimestamp;
    }

    public void setLastHealTimestamp(long time) {
        lastHealTimestamp = time;
        config.setProperty("timestamps.lastheal", time);
        config.save();
    }

    private String jail;

    private String _getJail() {
        return config.getString("jail");
    }

    public String getJail() {
        return jail;
    }

    public void setJail(String jail) {
        if (jail == null || jail.isEmpty()) {
            this.jail = null;
            config.removeProperty("jail");
        } else {
            this.jail = jail;
            config.setProperty("jail", jail);
        }
        config.save();
    }

    private List<String> mails;

    private List<String> _getMails() {
        return config.getStringList("mail");
    }

    public List<String> getMails() {
        return mails;
    }

    public void setMails(List<String> mails) {
        if (mails == null) {
            config.removeProperty("mail");
            mails = _getMails();
        } else {
            config.setProperty("mail", mails);
        }
        this.mails = mails;
        config.save();
    }

    public void addMail(String mail) {
        mails.add(mail);
        setMails(mails);
    }

    private boolean teleportEnabled;

    private boolean _getTeleportEnabled() {
        return config.getBoolean("teleportenabled", true);
    }

    public boolean isTeleportEnabled() {
        return teleportEnabled;
    }

    public void setTeleportEnabled(boolean set) {
        teleportEnabled = set;
        config.setProperty("teleportenabled", set);
        config.save();
    }

    private boolean autoTeleportEnabled;

    private boolean _getAutoTeleportEnabled() {
        return config.getBoolean("teleportauto", false);
    }

    public boolean isAutoTeleportEnabled() {
        return autoTeleportEnabled;
    }

    public void setAutoTeleportEnabled(boolean set) {
        autoTeleportEnabled = set;
        config.setProperty("teleportauto", set);
        config.save();
    }

    private List<String> ignoredPlayers;

    public List<String> _getIgnoredPlayers() {
        return Collections.synchronizedList(config.getStringList("ignore"));
    }

    public void setIgnoredPlayers(List<String> players) {
        if (players == null || players.isEmpty()) {
            ignoredPlayers = Collections.synchronizedList(new ArrayList<String>());
            config.removeProperty("ignore");
        } else {
            ignoredPlayers = players;
            config.setProperty("ignore", players);
        }
        config.save();
    }

    @Deprecated
    public boolean isIgnoredPlayer(final String userName) {
        final IUser user = ess.getUser(userName);
        if (user == null || !user.getBase().isOnline()) {
            return false;
        }
        return isIgnoredPlayer(user);
    }

    public boolean isIgnoredPlayer(IUser user) {
        return (ignoredPlayers.contains(user.getName().toLowerCase(Locale.ENGLISH)) && !user.isIgnoreExempt());
    }

    public void setIgnoredPlayer(IUser user, boolean set) {
        final String entry = user.getName().toLowerCase(Locale.ENGLISH);
        if (set) {
            if (!ignoredPlayers.contains(entry)) ignoredPlayers.add(entry);
        } else {
            ignoredPlayers.remove(entry);
        }
        setIgnoredPlayers(ignoredPlayers);
    }

    private boolean godmode;

    private boolean _getGodModeEnabled() {
        return config.getBoolean("godmode", false);
    }

    public boolean isGodModeEnabled() {
        return godmode;
    }

    public void setGodModeEnabled(boolean set) {
        godmode = set;
        config.setProperty("godmode", set);
        config.save();
    }

    private boolean muted;
    private String muteReason;

    public boolean _getMuted() {
        return config.getBoolean("muted", false);
    }

    public boolean getMuted() {
        return muted;
    }

    public boolean isMuted() {
        return muted;
    }

    public void setMuted(boolean set) {
        muted = set;
        config.setProperty("muted", set);
        config.save();
    }

    public String _getMuteReason() {
        return config.getString("muteReason");
    }

    public String getMuteReason() {
        return muteReason;
    }

    public void setMuteReason(String reason) {
        if (reason == null) {
            config.removeProperty("muteReason");
            muteReason = null;
        } else {
            muteReason = reason;
            config.setProperty("muteReason", reason);
        }
        config.save();
    }

    public boolean hasMuteReason(){
        return muteReason != null;
    }

    private long muteTimeout;

    private long _getMuteTimeout() {
        return config.getLong("timestamps.mute", 0);
    }

    public long getMuteTimeout() {
        return muteTimeout;
    }

    public void setMuteTimeout(long time) {
        muteTimeout = time;
        config.setProperty("timestamps.mute", time);
        config.save();
    }

    private boolean jailed;

    private boolean _getJailed() {
        return config.getBoolean("jailed", false);
    }

    public boolean isJailed() {
        return jailed;
    }

    public void setJailed(boolean set) {
        jailed = set;
        config.setProperty("jailed", set);
        config.save();
    }

    public boolean toggleJailed() {
        boolean ret = !isJailed();
        setJailed(ret);
        return ret;
    }

    private long jailTimeout;

    private long _getJailTimeout() {
        return config.getLong("timestamps.jail", 0);
    }

    public long getJailTimeout() {
        return jailTimeout;
    }

    public void setJailTimeout(long time) {
        jailTimeout = time;
        config.setProperty("timestamps.jail", time);
        config.save();
    }

    private long lastLogin;

    private long _getLastLogin() {
        return config.getLong("timestamps.login", 0);
    }

    public long getLastLogin() {
        return lastLogin;
    }

    private void _setLastLogin(long time) {
        lastLogin = time;
        config.setProperty("timestamps.login", time);
    }

    public void setLastLogin(long time) {
        _setLastLogin(time);
        if (base.getAddress() != null && base.getAddress().getAddress() != null) {
            _setLastLoginAddress(base.getAddress().getAddress().getHostAddress());
        }
        config.save();
    }

    private long lastLogout;

    private long _getLastLogout() {
        return config.getLong("timestamps.logout", 0);
    }

    public long getLastLogout() {
        return lastLogout;
    }

    public void setLastLogout(long time) {
        lastLogout = time;
        config.setProperty("timestamps.logout", time);
        config.save();
    }

    private String lastLoginAddress;

    private String _getLastLoginAddress() {
        return config.getString("ipAddress", "");
    }

    public String getLastLoginAddress() {
        return lastLoginAddress;
    }

    private void _setLastLoginAddress(String address) {
        lastLoginAddress = address;
        config.setProperty("ipAddress", address);
    }

    private boolean afk;

    private boolean _getAfk() {
        return config.getBoolean("afk", false);
    }

    public boolean isAfk() {
        return afk;
    }

    public void _setAfk(boolean set) {
        afk = set;
        config.setProperty("afk", set);
        config.save();
    }

    private boolean newplayer;
    private String geolocation;

    private String _getGeoLocation() {
        return config.getString("geolocation");
    }

    public String getGeoLocation() {
        return geolocation;
    }

    public void setGeoLocation(String geolocation) {
        if (geolocation == null || geolocation.isEmpty()) {
            this.geolocation = null;
            config.removeProperty("geolocation");
        } else {
            this.geolocation = geolocation;
            config.setProperty("geolocation", geolocation);
        }
        config.save();
    }

    private boolean isSocialSpyEnabled;

    private boolean _isSocialSpyEnabled() {
        return config.getBoolean("socialspy", false);
    }

    public boolean isSocialSpyEnabled() {
        return isSocialSpyEnabled;
    }

    public void setSocialSpyEnabled(boolean status) {
        isSocialSpyEnabled = status;
        config.setProperty("socialspy", status);
        config.save();
    }

    private boolean isNPC;

    private boolean _isNPC() {
        return config.getBoolean("npc", false);
    }

    public boolean isNPC() {
        return isNPC;
    }

    private String lastAccountName = null;

    public String getLastAccountName() {
        return lastAccountName;
    }

    public String _getLastAccountName() {
        return config.getString("lastAccountName", null);
    }

    public void setLastAccountName(String lastAccountName) {
        this.lastAccountName = lastAccountName;
        config.setProperty("lastAccountName", lastAccountName);
        config.save();
        ess.getUserMap().trackUUID(getConfigUUID(), lastAccountName, true);
    }

    public void setNPC(boolean set) {
        isNPC = set;
        config.setProperty("npc", set);
        config.save();
    }

    private boolean arePowerToolsEnabled;

    public boolean arePowerToolsEnabled() {
        return arePowerToolsEnabled;
    }

    public void setPowerToolsEnabled(boolean set) {
        arePowerToolsEnabled = set;
        config.setProperty("powertoolsenabled", set);
        config.save();
    }

    public boolean togglePowerToolsEnabled() {
        boolean ret = !arePowerToolsEnabled();
        setPowerToolsEnabled(ret);
        return ret;
    }

    private boolean _arePowerToolsEnabled() {
        return config.getBoolean("powertoolsenabled", true);
    }

    private Map<String, Long> kitTimestamps;

    private Map<String, Long> _getKitTimestamps() {

        if (config.isConfigurationSection("timestamps.kits")) {
            final ConfigurationSection section = config.getConfigurationSection("timestamps.kits");
            final Map<String, Long> timestamps = new HashMap<String, Long>();
            for (String command : section.getKeys(false)) {
                if (section.isLong(command)) {
                    timestamps.put(command.toLowerCase(Locale.ENGLISH), section.getLong(command));
                } else if (section.isInt(command)) {
                    timestamps.put(command.toLowerCase(Locale.ENGLISH), (long) section.getInt(command));
                }
            }
            return timestamps;
        }
        return new HashMap<String, Long>();
    }

    public long getKitTimestamp(String name) {
        name = name.replace('.', '_').replace('/', '_');
        if (kitTimestamps != null && kitTimestamps.containsKey(name)) {
            return kitTimestamps.get(name);
        }
        return 0l;
    }

    public void setKitTimestamp(final String name, final long time) {
        kitTimestamps.put(name.toLowerCase(Locale.ENGLISH), time);
        config.setProperty("timestamps.kits", kitTimestamps);
        config.save();
    }

    public void setConfigProperty(String node, Object object) {
        final String prefix = "info.";
        node = prefix + node;
        if (object instanceof Map) {
            config.setProperty(node, (Map) object);
        } else if (object instanceof List) {
            config.setProperty(node, (List<String>) object);
        } else if (object instanceof Location) {
            config.setProperty(node, (Location) object);
        } else if (object instanceof ItemStack) {
            config.setProperty(node, (ItemStack) object);
        } else {
            config.setProperty(node, object);
        }
        config.save();
    }

    public Set<String> getConfigKeys() {
        if (config.isConfigurationSection("info")) {
            return config.getConfigurationSection("info").getKeys(true);
        }
        return new HashSet<String>();
    }

    public Map<String, Object> getConfigMap() {
        if (config.isConfigurationSection("info")) {
            return config.getConfigurationSection("info").getValues(true);
        }
        return new HashMap<String, Object>();
    }

    public Map<String, Object> getConfigMap(String node) {
        if (config.isConfigurationSection("info." + node)) {
            return config.getConfigurationSection("info." + node).getValues(true);
        }
        return new HashMap<String, Object>();
    }

    // Pattern, Date. Pattern for less pattern creations
    private Map<Pattern, Long> commandCooldowns;

    private Map<Pattern, Long> _getCommandCooldowns() {
        if (!config.isConfigurationSection("timestamps.command-cooldowns")) {
            return null;
        }

        // See saveCommandCooldowns() for deserialization explanation
        List<Map<?, ?>> section = config.getMapList("timestamps.command-cooldowns");
        HashMap<Pattern, Long> result = new HashMap<>();
        for (Map<?, ?> map : section) {
            Pattern pattern = Pattern.compile(map.get("pattern").toString());
            long expiry = ((Number) map.get("expiry")).longValue();
            result.put(pattern, expiry);
        }
        return result;
    }

    public Map<Pattern, Long> getCommandCooldowns() {
        if (this.commandCooldowns == null) {
            return Collections.emptyMap();
        }
        return Collections.unmodifiableMap(this.commandCooldowns);
    }

    public Date getCommandCooldownExpiry(String label) {
        if (commandCooldowns != null) {
            for (Entry<Pattern, Long> entry : this.commandCooldowns.entrySet()) {
                if (entry.getKey().matcher(label).matches()) {
                    return new Date(entry.getValue());
                }
            }
        }
        return null;
    }

    public void addCommandCooldown(Pattern pattern, Date expiresAt, boolean save) {
        if (this.commandCooldowns == null) {
            this.commandCooldowns = new HashMap<>();
        }
        this.commandCooldowns.put(pattern, expiresAt.getTime());
        if (save) {
            saveCommandCooldowns();
        }
    }

    public boolean clearCommandCooldown(Pattern pattern) {
        if (this.commandCooldowns == null) {
            return false; // false for no modification
        }

        if(this.commandCooldowns.remove(pattern) != null) {
            saveCommandCooldowns();
            return true;
        }
        return false;
    }

    private void saveCommandCooldowns() {
        // Serialization explanation:
        //
        // Serialization is done as a map list instead of a config section due to limitations.
        // When serializing patterns (which commonly include full stops .) Bukkit/Essentials config framework
        // interprets it as a path separator, thus it breaks up the regex into sub nodes causing invalid syntax.
        // Thus each command cooldown is instead stored as a Map of {pattern: .., expiry: ..} to work around this.
        List<Object> serialized = new ArrayList<>();
        for (Entry<Pattern, Long> entry : this.commandCooldowns.entrySet()) {
            // Don't save expired cooldowns
            if (entry.getValue() < System.currentTimeMillis()) {
                continue;
            }

            Map<?, ?> map = ImmutableMap.builder()
                .put("pattern", entry.getKey().pattern())
                .put("expiry", entry.getValue())
                .build();
            serialized.add(map);
        }
        config.setProperty("timestamps.command-cooldowns", serialized);
        save();
    }

    private boolean acceptingPay = true; // players accept pay by default

    public boolean _getAcceptingPay() {
        return config.getBoolean("acceptingPay", true);
    }

    public boolean isAcceptingPay() {
        return acceptingPay;
    }

    public void setAcceptingPay(boolean acceptingPay) {
        this.acceptingPay = acceptingPay;
        config.setProperty("acceptingPay", acceptingPay);
        save();
    }

    private Boolean confirmPay;

    private Boolean _getConfirmPay() {
        return (Boolean) config.get("confirm-pay");
    }

    public boolean isPromptingPayConfirm() {
        return confirmPay != null ? confirmPay : ess.getSettings().isConfirmCommandEnabledByDefault("pay");
    }

    public void setPromptingPayConfirm(boolean prompt) {
        this.confirmPay = prompt;
        config.setProperty("confirm-pay", prompt);
        save();
    }

    private Boolean confirmClear;

    private Boolean _getConfirmClear() {
        return (Boolean) config.get("confirm-clear");
    }

    public boolean isPromptingClearConfirm() {
        return confirmClear != null ? confirmClear : ess.getSettings().isConfirmCommandEnabledByDefault("clearinventory");
    }

    public void setPromptingClearConfirm(boolean prompt) {
        this.confirmClear = prompt;
        config.setProperty("confirm-clear", prompt);
        save();
    }

    private boolean lastMessageReplyRecipient;

    private boolean _getLastMessageReplyRecipient() {
        return config.getBoolean("last-message-reply-recipient", ess.getSettings().isLastMessageReplyRecipient());
    }

    public boolean isLastMessageReplyRecipient() {
        return this.lastMessageReplyRecipient;
    }

    public void setLastMessageReplyRecipient(boolean enabled) {
        this.lastMessageReplyRecipient = enabled;
        config.setProperty("last-message-reply-recipient", enabled);
        save();
    }

    private Locale locale = null;

    private Locale _getLocale() {
        return I18n.getLocale(config.getString("locale", null));
    }

    public Locale getLocale() {
        return locale;
    }

    public void setLocale(Locale locale) {
        this.locale = locale;
        config.setProperty("locale", locale != null ? locale.toString() : null);
        save();
    }

    public UUID getConfigUUID() {
        return config.uuid;
    }

    public void save() {
        config.save();
    }

    public void startTransaction() {
        config.startTransaction();
    }

    public void stopTransaction() {
        config.stopTransaction();
    }
}
