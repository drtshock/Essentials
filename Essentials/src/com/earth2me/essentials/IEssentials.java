package com.earth2me.essentials;

import com.earth2me.essentials.api.IItemDb;
import com.earth2me.essentials.api.IJails;
import com.earth2me.essentials.api.IWarps;
import com.earth2me.essentials.metrics.Metrics;
import com.earth2me.essentials.perm.PermissionsHandler;
import com.earth2me.essentials.register.payment.Methods;
import net.ess3.nms.SpawnerProvider;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitScheduler;
import org.bukkit.scheduler.BukkitTask;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface IEssentials extends Plugin {
    void addReloadListener(IConf listener);

    void reload();

    List<String> onTabCompleteEssentials(CommandSender sender, Command command, String commandLabel, String[] args, ClassLoader classLoader, String commandPath, String permissionPrefix, IEssentialsModule module);

    boolean onCommandEssentials(CommandSender sender, Command command, String commandLabel, String[] args, ClassLoader classLoader, String commandPath, String permissionPrefix, IEssentialsModule module);

    @Deprecated
    User getUser(Object base);

    User getUser(UUID base);

    User getUser(String base);

    User getUser(Player base);

    I18n getI18n();

    User getOfflineUser(String name);

    World getWorld(String name);

    int broadcastMessage(String message);

    int broadcastMessage(IUser sender, String message);

    int broadcastMessage(String permission, String message);

    int broadcastTl(String string, Object... objects);

    int broadcastTl(IUser sender, String string, Object... objects);

    int broadcastTl(CommandSource sender, String string, Object... objects);

    int broadcastTl(String permission, String string, Object... objects);

    ISettings getSettings();

    BukkitScheduler getScheduler();

    IJails getJails();

    IWarps getWarps();

    Worth getWorth();

    Backup getBackup();

    Kits getKits();

    Methods getPaymentMethod();

    BukkitTask runTaskAsynchronously(Runnable run);

    BukkitTask runTaskLaterAsynchronously(Runnable run, long delay);

    BukkitTask runTaskTimerAsynchronously(Runnable run, long delay, long period);

    int scheduleSyncDelayedTask(Runnable run);

    int scheduleSyncDelayedTask(Runnable run, long delay);

    int scheduleSyncRepeatingTask(Runnable run, long delay, long period);

    TNTExplodeListener getTNTListener();

    PermissionsHandler getPermissionsHandler();

    AlternativeCommandsHandler getAlternativeCommandsHandler();

    void showError(CommandSource sender, Throwable exception, String commandLabel);

    IItemDb getItemDb();

    UserMap getUserMap();

    Metrics getMetrics();

    void setMetrics(Metrics metrics);

    EssentialsTimer getTimer();

    @Deprecated
    List<String> getVanishedPlayers();

    Collection<Player> getOnlinePlayers();

    Iterable<User> getOnlineUsers();

    SpawnerProvider getSpawnerProvider();
}
