package com.earth2me.essentials.textreader;

import com.earth2me.essentials.CommandSource;
import com.earth2me.essentials.ExecuteTimer;
import com.earth2me.essentials.PlayerList;
import com.earth2me.essentials.User;
import com.earth2me.essentials.utils.DateUtil;
import com.earth2me.essentials.utils.DescParseTickFormat;
import com.earth2me.essentials.utils.NumberUtil;
import net.ess3.api.IEssentials;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.lang.management.ManagementFactory;
import java.text.DateFormat;
import java.text.NumberFormat;
import java.util.*;
import java.util.logging.Level;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class KeywordReplacer implements IText {
    private static final Pattern KEYWORD = Pattern.compile("\\{([^\\{\\}]+)\\}");
    private static final Pattern KEYWORDSPLIT = Pattern.compile("\\:");
    private final transient IText input;
    private final transient List<String> replaced;
    private final transient IEssentials ess;
    private final transient boolean includePrivate;
    private transient ExecuteTimer execTimer;
    private final transient boolean replaceSpacesWithUnderscores;
    private final EnumMap<KeywordType, Object> keywordCache = new EnumMap<KeywordType, Object>(KeywordType.class);

    public KeywordReplacer(final IText input, final CommandSource sender, final IEssentials ess) {
        this.input = input;
        this.replaced = new ArrayList<String>(this.input.getLines().size());
        this.ess = ess;
        this.includePrivate = true;
        this.replaceSpacesWithUnderscores = false;
        replaceKeywords(sender);
    }

    public KeywordReplacer(final IText input, final CommandSource sender, final IEssentials ess, final boolean showPrivate) {
        this.input = input;
        this.replaced = new ArrayList<String>(this.input.getLines().size());
        this.ess = ess;
        this.includePrivate = showPrivate;
        this.replaceSpacesWithUnderscores = false;
        replaceKeywords(sender);
    }

    public KeywordReplacer(final IText input, final CommandSource sender, final IEssentials ess, final boolean showPrivate,
                           boolean replaceSpacesWithUnderscores) {
        this.input = input;
        this.replaced = new ArrayList<String>(this.input.getLines().size());
        this.ess = ess;
        this.includePrivate = showPrivate;
        this.replaceSpacesWithUnderscores = replaceSpacesWithUnderscores;
        replaceKeywords(sender);
    }

    private void replaceKeywords(final CommandSource sender) {
        execTimer = new ExecuteTimer();
        execTimer.start();
        User user = null;
        if (sender.isPlayer()) {
            user = ess.getUser(sender.getPlayer());
        }
        execTimer.mark("User Grab");

        for (int i = 0; i < input.getLines().size(); i++) {
            String line = input.getLines().get(i);
            final Matcher matcher = KEYWORD.matcher(line);

            while (matcher.find()) {
                final String fullMatch = matcher.group(0);
                final String keywordMatch = matcher.group(1);
                final String[] matchTokens = KEYWORDSPLIT.split(keywordMatch);
                line = replaceLine(line, fullMatch, matchTokens, user);
            }
            replaced.add(line);
        }

        execTimer.mark("Text Replace");
        final String timeroutput = execTimer.end();
        if (ess.getSettings().isDebug()) {
            ess.getLogger().log(Level.INFO, "Keyword Replacer " + timeroutput);
        }
    }

    private String replaceLine(String line, final String fullMatch, final String[] matchTokens, final User user) {
        final String keyword = matchTokens[0];
        try {
            String replacer = null;
            KeywordType validKeyword = KeywordType.valueOf(keyword);
            if (validKeyword.getType().equals(KeywordCachable.CACHEABLE) && keywordCache.containsKey(validKeyword)) {
                replacer = keywordCache.get(validKeyword).toString();
            } else if (validKeyword.getType().equals(KeywordCachable.SUBVALUE)) {
                String subKeyword = "";
                if (matchTokens.length > 1) {
                    subKeyword = matchTokens[1].toLowerCase(Locale.ENGLISH);
                }

                if (keywordCache.containsKey(validKeyword)) {
                    Map<String, String> values = (Map<String, String>) keywordCache.get(validKeyword);
                    if (values.containsKey(subKeyword)) {
                        replacer = values.get(subKeyword);
                    }
                }
            }

            if (validKeyword.isPrivate() && !includePrivate) {
                replacer = "";
            }

            if (replacer == null) {
                replacer = "";
                Locale locale = user != null ? user.getApplicableLocale() : ess.getI18n().getCurrentLocale();
                switch (validKeyword) {
                    case PLAYER:
                    case DISPLAYNAME:
                        if (user != null) {
                            replacer = user.getDisplayName();
                        }
                        break;
                    case USERNAME:
                        if (user != null) {
                            replacer = user.getName();
                        }
                        break;
                    case BALANCE:
                        if (user != null) {
                            replacer = NumberUtil.displayCurrency(user.getMoney(), ess);
                        }
                        break;
                    case MAILS:
                        if (user != null) {
                            replacer = Integer.toString(user.getMails().size());
                        }
                        break;
                    case WORLD:
                    case WORLDNAME:
                        if (user != null) {
                            final Location location = user.getLocation();
                            replacer = location == null || location.getWorld() == null ? "" : location.getWorld().getName();
                        }
                        break;
                    case ONLINE:
                        int playerHidden = 0;
                        for (User u : ess.getOnlineUsers()) {
                            if (u.isHidden()) {
                                playerHidden++;
                            }
                        }
                        replacer = Integer.toString(ess.getOnlinePlayers().size() - playerHidden);
                        break;
                    case UNIQUE:
                        replacer = NumberFormat.getInstance().format(ess.getUserMap().getUniqueUsers());
                        break;
                    case WORLDS:
                        final StringBuilder worldsBuilder = new StringBuilder();
                        for (World w : ess.getServer().getWorlds()) {
                            if (worldsBuilder.length() > 0) {
                                worldsBuilder.append(", ");
                            }
                            worldsBuilder.append(w.getName());
                        }
                        replacer = worldsBuilder.toString();
                        break;
                    case PLAYERLIST:
                        final Map<String, String> outputList;
                        if (keywordCache.containsKey(validKeyword)) {
                            outputList = (Map<String, String>) keywordCache.get(validKeyword);
                        } else {
                            final boolean showHidden;
                            if (user == null) {
                                showHidden = true;
                            } else {
                                showHidden = user.isAuthorized("essentials.list.hidden") || user.canInteractVanished();
                            }

                            //First lets build the per group playerlist
                            final Map<String, List<User>> playerList = PlayerList.getPlayerLists(ess, user, showHidden);
                            outputList = new HashMap<String, String>();
                            for (String groupName : playerList.keySet()) {
                                final List<User> groupUsers = playerList.get(groupName);
                                if (groupUsers != null && !groupUsers.isEmpty()) {
                                    outputList.put(groupName, PlayerList.listUsers(ess, user != null ? user.getSource() : new CommandSource(Bukkit.getConsoleSender()), groupUsers, " "));
                                }
                            }

                            //Now lets build the all user playerlist
                            final StringBuilder playerlistBuilder = new StringBuilder();
                            for (Player p : ess.getOnlinePlayers()) {
                                if (ess.getUser(p).isHidden()) {
                                    continue;
                                }
                                if (playerlistBuilder.length() > 0) {
                                    playerlistBuilder.append(", ");
                                }
                                playerlistBuilder.append(p.getDisplayName());
                            }
                            outputList.put("", playerlistBuilder.toString());
                            keywordCache.put(validKeyword, outputList);
                        }

                        //Now thats all done, output the one we want and cache the rest.
                        if (matchTokens.length == 1) {
                            replacer = outputList.get("");
                        } else if (outputList.containsKey(matchTokens[1].toLowerCase(Locale.ENGLISH))) {
                            replacer = outputList.get(matchTokens[1].toLowerCase(Locale.ENGLISH));
                        } else if (matchTokens.length > 2) {
                            replacer = matchTokens[2];
                        }

                        keywordCache.put(validKeyword, outputList);
                        break;
                    case TIME:
                        replacer = DateFormat.getTimeInstance(DateFormat.MEDIUM, locale).format(new Date());
                        break;
                    case DATE:
                        replacer = DateFormat.getDateInstance(DateFormat.MEDIUM, locale).format(new Date());
                        break;
                    case WORLDTIME12:
                        if (user != null) {
                            replacer = DescParseTickFormat.format12(user.getWorld() == null ? 0 : user.getWorld().getTime());
                        }
                        break;
                    case WORLDTIME24:
                        if (user != null) {
                            replacer = DescParseTickFormat.format24(user.getWorld() == null ? 0 : user.getWorld().getTime());
                        }
                        break;
                    case WORLDDATE:
                        if (user != null) {
                            replacer = DateFormat.getDateInstance(DateFormat.MEDIUM, locale).format(DescParseTickFormat.ticksToDate(user.getWorld() == null ? 0 : user.getWorld().getFullTime()));
                        }
                        break;
                    case COORDS:
                        if (user != null) {
                            final Location location = user.getLocation();
                            replacer = user.tl("coordsKeyword", location.getBlockX(), location.getBlockY(), location.getBlockZ());
                        }
                        break;
                    case TPS:
                        replacer = NumberUtil.formatDouble(ess.getTimer().getAverageTPS());
                        break;
                    case UPTIME:
                        replacer = DateUtil.formatDateDiff(ManagementFactory.getRuntimeMXBean().getStartTime());
                        break;
                    case IP:
                        if (user != null) {
                            replacer = user.getBase().getAddress() == null || user.getBase().getAddress().getAddress() == null ? "" : user.getBase().getAddress().getAddress().toString();
                        }
                        break;
                    case ADDRESS:
                        if (user != null) {
                            replacer = user.getBase().getAddress() == null ? "" : user.getBase().getAddress().toString();
                        }
                        break;
                    case PLUGINS:
                        final StringBuilder pluginlistBuilder = new StringBuilder();
                        for (Plugin p : ess.getServer().getPluginManager().getPlugins()) {
                            if (pluginlistBuilder.length() > 0) {
                                pluginlistBuilder.append(", ");
                            }
                            pluginlistBuilder.append(p.getDescription().getName());
                        }
                        replacer = pluginlistBuilder.toString();
                        break;
                    case VERSION:
                        replacer = ess.getServer().getVersion();
                        break;
                    default:
                        replacer = "N/A";
                        break;
                }

                if (this.replaceSpacesWithUnderscores) {
                    replacer = replacer.replaceAll("\\s", "_");
                }

                //If this is just a regular keyword, lets throw it into the cache
                if (validKeyword.getType().equals(KeywordCachable.CACHEABLE)) {
                    keywordCache.put(validKeyword, replacer);
                }
            }

            line = line.replace(fullMatch, replacer);
        } catch (IllegalArgumentException ex) {
        }

        return line;
    }

    @Override
    public List<String> getLines() {
        return replaced;
    }

    @Override
    public List<String> getChapters() {
        return input.getChapters();
    }

    @Override
    public Map<String, Integer> getBookmarks() {
        return input.getBookmarks();
    }
}

//When adding a keyword here, you also need to add the implementation above
enum KeywordType {
    PLAYER(KeywordCachable.CACHEABLE),
    DISPLAYNAME(KeywordCachable.CACHEABLE),
    USERNAME(KeywordCachable.NOTCACHEABLE),
    BALANCE(KeywordCachable.CACHEABLE),
    MAILS(KeywordCachable.CACHEABLE),
    WORLD(KeywordCachable.CACHEABLE),
    WORLDNAME(KeywordCachable.CACHEABLE),
    ONLINE(KeywordCachable.CACHEABLE),
    UNIQUE(KeywordCachable.CACHEABLE),
    WORLDS(KeywordCachable.CACHEABLE),
    PLAYERLIST(KeywordCachable.SUBVALUE, true),
    TIME(KeywordCachable.CACHEABLE),
    DATE(KeywordCachable.CACHEABLE),
    WORLDTIME12(KeywordCachable.CACHEABLE),
    WORLDTIME24(KeywordCachable.CACHEABLE),
    WORLDDATE(KeywordCachable.CACHEABLE),
    COORDS(KeywordCachable.CACHEABLE),
    TPS(KeywordCachable.CACHEABLE),
    UPTIME(KeywordCachable.CACHEABLE),
    IP(KeywordCachable.CACHEABLE, true),
    ADDRESS(KeywordCachable.CACHEABLE, true),
    PLUGINS(KeywordCachable.CACHEABLE, true),
    VERSION(KeywordCachable.CACHEABLE, true);
    private final KeywordCachable type;
    private final boolean isPrivate;

    KeywordType(KeywordCachable type) {
        this.type = type;
        this.isPrivate = false;
    }

    KeywordType(KeywordCachable type, boolean isPrivate) {
        this.type = type;
        this.isPrivate = isPrivate;
    }

    public KeywordCachable getType() {
        return type;
    }

    public boolean isPrivate() {
        return isPrivate;
    }
}


enum KeywordCachable {
    CACHEABLE, // This keyword can be cached as a string
    SUBVALUE, // This keyword can be cached as a map
    NOTCACHEABLE // This keyword should never be cached
}
