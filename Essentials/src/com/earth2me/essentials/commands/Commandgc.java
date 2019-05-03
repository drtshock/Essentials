package com.earth2me.essentials.commands;

import com.earth2me.essentials.CommandSource;
import com.earth2me.essentials.utils.DateUtil;
import com.earth2me.essentials.utils.NumberUtil;
import org.bukkit.*;

import java.lang.management.ManagementFactory;
import java.util.List;
import java.util.logging.Level;


public class Commandgc extends EssentialsCommand {
    public Commandgc() {
        super("gc");
    }

    @Override
    protected void run(final Server server, final CommandSource sender, final String commandLabel, final String[] args) throws Exception {
        double tps = ess.getTimer().getAverageTPS();
        ChatColor color;
        if (tps >= 18.0) {
            color = ChatColor.GREEN;
        } else if (tps >= 15.0) {
            color = ChatColor.YELLOW;
        } else {
            color = ChatColor.RED;
        }

        sender.sendTl("uptime", DateUtil.formatDateDiff(sender, ManagementFactory.getRuntimeMXBean().getStartTime()));
        sender.sendTl("tps", "" + color + NumberUtil.formatDouble(tps));
        sender.sendTl("gcmax", (Runtime.getRuntime().maxMemory() / 1024 / 1024));
        sender.sendTl("gctotal", (Runtime.getRuntime().totalMemory() / 1024 / 1024));
        sender.sendTl("gcfree", (Runtime.getRuntime().freeMemory() / 1024 / 1024));

        List<World> worlds = server.getWorlds();
        for (World w : worlds) {
            String worldType = "World";
            switch (w.getEnvironment()) {
                case NETHER:
                    worldType = "Nether";
                    break;
                case THE_END:
                    worldType = "The End";
                    break;
            }

            int tileEntities = 0;

            try {
                for (Chunk chunk : w.getLoadedChunks()) {
                    tileEntities += chunk.getTileEntities().length;
                }
            } catch (java.lang.ClassCastException ex) {
                Bukkit.getLogger().log(Level.SEVERE, "Corrupted chunk data on world " + w, ex);
            }

            sender.sendTl("gcWorld", worldType, w.getName(), w.getLoadedChunks().length, w.getEntities().size(), tileEntities);
        }
    }
}
