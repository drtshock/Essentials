package com.earth2me.essentials.commands;

import com.earth2me.essentials.CommandSource;
import com.earth2me.essentials.EssentialsUpgrade;
import com.earth2me.essentials.User;
import com.earth2me.essentials.UserMap;
import com.earth2me.essentials.utils.*;
import com.google.common.base.Charsets;
import com.google.common.collect.Lists;
import org.bukkit.Material;
import org.bukkit.Server;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginDescriptionFile;
import org.bukkit.plugin.PluginManager;

import java.util.*;

// This command has 4 undocumented behaviours #EasterEgg
public class Commandessentials extends EssentialsCommand {

    private final Sound NOTE_HARP = EnumUtil.valueOf(Sound.class, "BLOCK_NOTE_HARP", "NOTE_PIANO");

    public Commandessentials() {
        super("essentials");
    }

    private transient int taskid;
    private final transient Map<Player, Block> noteBlocks = new HashMap<Player, Block>();

    private final List<String> versionPlugins = Arrays.asList(
            "Vault",
            "LuckPerms",
            "PermissionsEx",
            "GroupManager"
    );

    private final List<String> officialPlugins = Arrays.asList(
            "EssentialsAntiBuild",
            "EssentialsChat",
            "EssentialsGeoIP",
            "EssentialsProtect",
            "EssentialsSpawn",
            "EssentialsXMPP"
    );

    private final List<String> warnPlugins = Arrays.asList(
            "PermissionsEx",
            "GroupManager"
    );

    @Override
    public void run(final Server server, final CommandSource sender, final String commandLabel, final String[] args) throws Exception {
        if (args.length == 0) {
            run_disabled(server, sender, commandLabel, args);
        } else if (args[0].equalsIgnoreCase("debug")) {
            run_debug(server, sender, commandLabel, args);
        } else if (args[0].equalsIgnoreCase("nya")) {
            run_nya(server, sender, commandLabel, args);
        } else if (args[0].equalsIgnoreCase("moo")) {
            run_moo(server, sender, commandLabel, args);
        } else if (args[0].equalsIgnoreCase("reset")) {
            run_reset(server, sender, commandLabel, args);
        } else if (args[0].equalsIgnoreCase("cleanup")) {
            run_cleanup(server, sender, commandLabel, args);
        } else if (args[0].equalsIgnoreCase("uuidconvert")) {
            run_uuidconvert(server, sender, commandLabel, args);
        } else if (args[0].equalsIgnoreCase("uuidtest")) {
            run_uuidtest(server, sender, commandLabel, args);
        } else if (args[0].equalsIgnoreCase("version")) {
            run_version(server, sender, commandLabel, args);
        } else {
            run_reload(server, sender, commandLabel, args);
        }
    }

    //If you do not supply an argument this command will list 'overridden' commands.
    private void run_disabled(final Server server, final CommandSource sender, final String commandLabel, final String[] args) throws Exception {
        sender.sendMessage("/<command> <reload/debug>");

        final StringBuilder disabledCommands = new StringBuilder();
        for (Map.Entry<String, String> entry : ess.getAlternativeCommandsHandler().disabledCommands().entrySet()) {
            if (disabledCommands.length() > 0) {
                disabledCommands.append(", ");
            }
            disabledCommands.append(entry.getKey()).append(" => ").append(entry.getValue());
        }
        if (disabledCommands.length() > 0) {
            sender.sendTl("blockList");
            sender.sendMessage(disabledCommands.toString());
        }
    }

    private void run_reset(final Server server, final CommandSource sender, final String commandLabel, final String[] args) throws Exception {
        if (args.length < 2) {
            throw new Exception("/<command> reset <player>");
        }
        final User user = getPlayer(server, args, 1, true, true);
        user.reset();
        sender.sendMessage("Reset Essentials userdata for player: " + user.getDisplayName());
    }

    private void run_debug(final Server server, final CommandSource sender, final String commandLabel, final String[] args) throws Exception {
        ess.getSettings().setDebug(!ess.getSettings().isDebug());
        sender.sendMessage("Essentials " + ess.getDescription().getVersion() + " debug mode " + (ess.getSettings().isDebug() ? "enabled" : "disabled"));
    }

    private void run_reload(final Server server, final CommandSource sender, final String commandLabel, final String[] args) throws Exception {
        ess.reload();
        sender.sendTl("essentialsReload", ess.getDescription().getVersion());
    }

    private void run_nya(final Server server, final CommandSource sender, final String commandLabel, final String[] args) throws Exception {
        final Map<String, Float> noteMap = new HashMap<String, Float>();
        noteMap.put("1F#", 0.5f);
        noteMap.put("1G", 0.53f);
        noteMap.put("1G#", 0.56f);
        noteMap.put("1A", 0.6f);
        noteMap.put("1A#", 0.63f);
        noteMap.put("1B", 0.67f);
        noteMap.put("1C", 0.7f);
        noteMap.put("1C#", 0.76f);
        noteMap.put("1D", 0.8f);
        noteMap.put("1D#", 0.84f);
        noteMap.put("1E", 0.9f);
        noteMap.put("1F", 0.94f);
        noteMap.put("2F#", 1.0f);
        noteMap.put("2G", 1.06f);
        noteMap.put("2G#", 1.12f);
        noteMap.put("2A", 1.18f);
        noteMap.put("2A#", 1.26f);
        noteMap.put("2B", 1.34f);
        noteMap.put("2C", 1.42f);
        noteMap.put("2C#", 1.5f);
        noteMap.put("2D", 1.6f);
        noteMap.put("2D#", 1.68f);
        noteMap.put("2E", 1.78f);
        noteMap.put("2F", 1.88f);
        final String tuneStr = "1D#,1E,2F#,,2A#,1E,1D#,1E,2F#,2B,2D#,2E,2D#,2A#,2B,,2F#,,1D#,1E,2F#,2B,2C#,2A#,2B,2C#,2E,2D#,2E,2C#,,2F#,,2G#,,1D,1D#,,1C#,1D,1C#,1B,,1B,,1C#,,1D,,1D,1C#,1B,1C#,1D#,2F#,2G#,1D#,2F#,1C#,1D#,1B,1C#,1B,1D#,,2F#,,2G#,1D#,2F#,1C#,1D#,1B,1D,1D#,1D,1C#,1B,1C#,1D,,1B,1C#,1D#,2F#,1C#,1D,1C#,1B,1C#,,1B,,1C#,,2F#,,2G#,,1D,1D#,,1C#,1D,1C#,1B,,1B,,1C#,,1D,,1D,1C#,1B,1C#,1D#,2F#,2G#,1D#,2F#,1C#,1D#,1B,1C#,1B,1D#,,2F#,,2G#,1D#,2F#,1C#,1D#,1B,1D,1D#,1D,1C#,1B,1C#,1D,,1B,1C#,1D#,2F#,1C#,1D,1C#,1B,1C#,,1B,,1B,,1B,,1F#,1G#,1B,,1F#,1G#,1B,1C#,1D#,1B,1E,1D#,1E,2F#,1B,,1B,,1F#,1G#,1B,1E,1D#,1C#,1B,,,,1F#,1B,,1F#,1G#,1B,,1F#,1G#,1B,1B,1C#,1D#,1B,1F#,1G#,1F#,1B,,1B,1A#,1B,1F#,1G#,1B,1E,1D#,1E,2F#,1B,,1A#,,1B,,1F#,1G#,1B,,1F#,1G#,1B,1C#,1D#,1B,1E,1D#,1E,2F#,1B,,1B,,1F#,1G#,1B,1F#,1E,1D#,1C#,1B,,,,1F#,1B,,1F#,1G#,1B,,1F#,1G#,1B,1B,1C#,1D#,1B,1F#,1G#,1F#,1B,,1B,1A#,1B,1F#,1G#,1B,1E,1D#,1E,2F#,1B,,1A#,,1B,,1F#,1G#,1B,,1F#,1G#,1B,1C#,1D#,1B,1E,1D#,1E,2F#,1B,,1B,,1F#,1G#,1B,1F#,1E,1D#,1C#,1B,,,,1F#,1B,,1F#,1G#,1B,,1F#,1G#,1B,1B,1C#,1D#,1B,1F#,1G#,1F#,1B,,1B,1A#,1B,1F#,1G#,1B,1E,1D#,1E,2F#,1B,,1A#,,1B,,1F#,1G#,1B,,1F#,1G#,1B,1C#,1D#,1B,1E,1D#,1E,2F#,1B,,1B,,1F#,1G#,1B,1F#,1E,1D#,1C#,1B,,,,1F#,1B,,1F#,1G#,1B,,1F#,1G#,1B,1B,1C#,1D#,1B,1F#,1G#,1F#,1B,,1B,1A#,1B,1F#,1G#,1B,1E,1D#,1E,2F#,1B,,1A#,,1B,,1F#,1G#,1B,,1F#,1G#,1B,1C#,1D#,1B,1E,1D#,1E,2F#,1B,,1B,,1F#,1G#,1B,1F#,1E,1D#,1C#,1B,,,,1F#,1B,,1F#,1G#,1B,,1F#,1G#,1B,1B,1C#,1D#,1B,1F#,1G#,1F#,1B,,1B,1A#,1B,1F#,1G#,1B,1E,1D#,1E,2F#,1B,,1B,,";
        final String[] tune = tuneStr.split(",");
        taskid = ess.scheduleSyncRepeatingTask(new Runnable() {
            int i = 0;

            @Override
            public void run() {
                final String note = tune[i];
                i++;
                if (i >= tune.length) {
                    Commandessentials.this.stopTune();
                }
                if (note == null || note.isEmpty()) {
                    return;
                }

                for (Player onlinePlayer : ess.getOnlinePlayers()) {
                    onlinePlayer.playSound(onlinePlayer.getLocation(), NOTE_HARP, 1, noteMap.get(note));
                }
            }
        }, 20, 2);
    }

    private void stopTune() {
        ess.getScheduler().cancelTask(taskid);
        for (Block block : noteBlocks.values()) {
            if (block.getType() == Material.NOTE_BLOCK) {
                block.setType(Material.AIR);
            }
        }
        noteBlocks.clear();
    }

    private final String[] consoleMoo = new String[]{"         (__)", "         (oo)", "   /------\\/", "  / |    ||", " *  /\\---/\\", "    ~~   ~~", "....\"Have you mooed today?\"..."};
    private final String[] playerMoo = new String[]{"            (__)", "            (oo)", "   /------\\/", "  /  |      | |", " *  /\\---/\\", "    ~~    ~~", "....\"Have you mooed today?\"..."};

    private void run_moo(final Server server, final CommandSource sender, final String command, final String args[]) {
        Sound moo = EnumUtil.valueOf(Sound.class, "COW_IDLE", "ENTITY_COW_MILK");

        if (args.length == 2 && args[1].equals("moo")) {
            for (String s : consoleMoo) {
                logger.info(s);
            }
            for (Player player : ess.getOnlinePlayers()) {
                player.sendMessage(playerMoo);
                player.playSound(player.getLocation(), moo, 1, 1.0f);
            }
        } else {
            if (sender.isPlayer()) {
                sender.getSender().sendMessage(playerMoo);
                final Player player = sender.getPlayer();
                player.playSound(player.getLocation(), moo, 1, 1.0f);

            } else {
                sender.getSender().sendMessage(consoleMoo);
            }
        }
    }

    private void run_cleanup(final Server server, final CommandSource sender, final String command, final String args[]) throws Exception {
        if (args.length < 2 || !NumberUtil.isInt(args[1])) {
            sender.sendMessage("This sub-command will delete users who havent logged in in the last <days> days.");
            sender.sendMessage("Optional parameters define the minium amount required to prevent deletion.");
            sender.sendMessage("Unless you define larger default values, this command wil ignore people who have more than 0 money/homes.");
            throw new Exception("/<command> cleanup <days> [money] [homes]");
        }
        sender.sendTl("cleaning");

        final long daysArg = Long.parseLong(args[1]);
        final double moneyArg = args.length >= 3 ? FloatUtil.parseDouble(args[2].replaceAll("[^0-9\\.]", "")) : 0;
        final int homesArg = args.length >= 4 && NumberUtil.isInt(args[3]) ? Integer.parseInt(args[3]) : 0;
        final UserMap userMap = ess.getUserMap();

        ess.runTaskAsynchronously(new Runnable() {
            @Override
            public void run() {
                Long currTime = System.currentTimeMillis();
                for (UUID u : userMap.getAllUniqueUsers()) {
                    final User user = ess.getUserMap().getUser(u);
                    if (user == null) {
                        continue;
                    }

                    long lastLog = user.getLastLogout();
                    if (lastLog == 0) {
                        lastLog = user.getLastLogin();
                    }
                    if (lastLog == 0) {
                        user.setLastLogin(currTime);
                    }

                    if (user.isNPC()) {
                        continue;
                    }

                    long timeDiff = currTime - lastLog;
                    long milliDays = daysArg * 24L * 60L * 60L * 1000L;
                    int homeCount = user.getHomes().size();
                    double moneyCount = user.getMoney().doubleValue();

                    if ((lastLog == 0) || (timeDiff < milliDays) || (homeCount > homesArg) || (moneyCount > moneyArg)) {
                        continue;
                    }

                    if (ess.getSettings().isDebug()) {
                        ess.getLogger().info("Deleting user: " + user.getName() + " Money: " + moneyCount + " Homes: " + homeCount + " Last seen: " + DateUtil.formatDateDiff(lastLog));
                    }

                    user.reset();
                }
                sender.sendTl("cleaned");
            }
        });

    }

    private void run_uuidconvert(final Server server, final CommandSource sender, final String commandLabel, final String[] args) throws Exception {
        sender.sendMessage("Starting Essentials UUID userdata conversion, this may lag the server.");

        Boolean ignoreUFCache = (args.length > 2 && args[1].toLowerCase(Locale.ENGLISH).contains("ignore"));
        EssentialsUpgrade.uuidFileConvert(ess, ignoreUFCache);

        sender.sendMessage("UUID conversion complete, check your server log for more information.");
    }

    private void run_uuidtest(final Server server, final CommandSource sender, final String commandLabel, final String[] args) throws Exception {
        if (args.length < 2) {
            throw new Exception("/<command> uuidtest <name>");
        }
        String name = args[1];
        sender.sendMessage("Looking up UUID for " + name);

        UUID onlineUUID = null;

        for (Player player : ess.getOnlinePlayers()) {
            if (player.getName().equalsIgnoreCase(name)) {
                onlineUUID = player.getUniqueId();
                break;
            }
        }

        UUID essUUID = ess.getUserMap().getUser(name).getConfigUUID();

        org.bukkit.OfflinePlayer player = ess.getServer().getOfflinePlayer(name);
        UUID bukkituuid = player.getUniqueId();
        sender.sendMessage("Bukkit Lookup: " + bukkituuid.toString());

        if (onlineUUID != null && onlineUUID != bukkituuid) {
            sender.sendMessage("Online player: " + onlineUUID.toString());
        }

        if (essUUID != null && essUUID != bukkituuid) {
            sender.sendMessage("Essentials config: " + essUUID.toString());
        }

        UUID npcuuid = UUID.nameUUIDFromBytes(("NPC:" + name).getBytes(Charsets.UTF_8));
        sender.sendMessage("NPC UUID: " + npcuuid.toString());

        UUID offlineuuid = UUID.nameUUIDFromBytes(("OfflinePlayer:" + name).getBytes(Charsets.UTF_8));
        sender.sendMessage("Offline Mode UUID: " + offlineuuid.toString());
    }

    private void run_version(final Server server, final CommandSource sender, final String commandLabel, final String[] args) throws Exception {
        if (sender.isPlayer() && !ess.getUser(sender.getPlayer()).isAuthorized("essentials.version")) return;

        boolean isMismatched = false;
        boolean isVaultInstalled = false;
        boolean isUnsupported = false;
        final boolean isServerSupported = VersionUtil.isServerSupported();
        final PluginManager pm = server.getPluginManager();
        final String essVer = pm.getPlugin("Essentials").getDescription().getVersion();

        sender.sendTl(isServerSupported ? "versionOutputFine" : "versionOutputWarn", "Server", server.getBukkitVersion() + " " + server.getVersion());
        sender.sendTl("versionOutputFine", "EssentialsX", essVer);

        for (Plugin plugin : pm.getPlugins()) {
            final PluginDescriptionFile desc = plugin.getDescription();
            String name = desc.getName();
            String version = desc.getVersion();

            if (name.startsWith("Essentials") && !name.equalsIgnoreCase("Essentials")) {
                if (officialPlugins.contains(name)) {
                    name = name.replace("Essentials", "EssentialsX");

                    if (!version.equalsIgnoreCase(essVer)) {
                        isMismatched = true;
                        sender.sendTl("versionOutputWarn", name, version);
                    } else {
                        sender.sendTl("versionOutputFine", name, version);
                    }
                } else {
                    sender.sendTl("versionOutputUnsupported", name, version);
                    isUnsupported = true;
                }
            }

            if (versionPlugins.contains(name)) {
                if (warnPlugins.contains(name)) {
                    sender.sendTl("versionOutputUnsupported", name, version);
                    isUnsupported = true;
                } else {
                    sender.sendTl("versionOutputFine", name, version);
                }
            }

            if (name.equals("Vault")) isVaultInstalled = true;
        }

        if (isMismatched) {
            sender.sendTl("versionMismatchAll");
        }

        if (!isVaultInstalled) {
            sender.sendTl("versionOutputVaultMissing");
        }

        if (isUnsupported) {
            sender.sendTl("versionOutputUnsupportedPlugins");
        }

        if (!VersionUtil.isServerSupported()) {
            sender.sendTl("serverUnsupported");
        }
    }

    @Override
    protected List<String> getTabCompleteOptions(final Server server, final CommandSource sender, final String commandLabel, final String[] args) {
        if (args.length == 1) {
            List<String> options = Lists.newArrayList();
            options.add("reload");
            options.add("debug");
            //options.add("nya");
            //options.add("moo");
            options.add("reset");
            options.add("cleanup");
            //options.add("uuidconvert");
            //options.add("uuidtest");
            options.add("version");
            return options;
        } else if (args[0].equalsIgnoreCase("debug")) {
            // No args
        } else if (args[0].equalsIgnoreCase("nya")) {
            // No args
        } else if (args[0].equalsIgnoreCase("moo")) {
            if (args.length == 2) {
                return Lists.newArrayList("moo");
            }
        } else if (args[0].equalsIgnoreCase("reset")) {
            if (args.length == 2) {
                return getPlayers(server, sender);
            }
        } else if (args[0].equalsIgnoreCase("cleanup")) {
            if (args.length == 2) {
                return COMMON_DURATIONS;
            } else if (args.length == 3 || args.length == 4) {
                return Lists.newArrayList("-1", "0");
            }
        } else if (args[0].equalsIgnoreCase("uuidconvert")) {
            if (args.length == 2) {
                return Lists.newArrayList("ignoreUFCache");
            }
        } else if (args[0].equalsIgnoreCase("uuidtest")) {
            if (args.length == 2) {
                return getPlayers(server, sender);
            }
        } else if (args[0].equalsIgnoreCase("version")) {
            // No args
        } else {
            // No args
        }
        return Collections.emptyList();
    }
}
