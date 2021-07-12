package com.earth2me.essentials.metrics;

import com.earth2me.essentials.Essentials;
import com.earth2me.essentials.economy.EconomyLayer;
import com.earth2me.essentials.economy.EconomyLayers;
import org.bstats.bukkit.Metrics;
import org.bstats.charts.AdvancedBarChart;
import org.bstats.charts.CustomChart;
import org.bstats.charts.DrilldownPie;
import org.bstats.charts.MultiLineChart;
import org.bstats.charts.SimplePie;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashMap;
import java.util.Map;

public class MetricsWrapper {

    private final Essentials ess;
    private final Metrics metrics;
    private final JavaPlugin plugin;
    private final Map<String, Boolean> commands = new HashMap<>();

    public MetricsWrapper(final JavaPlugin plugin, final int pluginId, final boolean includeCommands) {
        this.plugin = plugin;
        this.ess = (Essentials) Bukkit.getPluginManager().getPlugin("Essentials");
        this.metrics = new Metrics(plugin, pluginId);

        plugin.getLogger().info("Starting Metrics. Opt-out using the global bStats config.");

        addPermsChart();
        addEconomyChart();
        addReleaseBranchChart();

        // bStats' backend currently doesn't support multi-line charts or advanced bar charts
        // These are included for when bStats is ready to accept this data
        addVersionHistoryChart();
        if (includeCommands) addCommandsChart();
    }

    public void markCommand(final String command, final boolean state) {
        commands.put(command, state);
    }

    public void addCustomChart(final CustomChart chart) {
        metrics.addCustomChart(chart);
    }

    private void addPermsChart() {
        metrics.addCustomChart(new DrilldownPie("permsPlugin", () -> {
            final Map<String, Map<String, Integer>> result = new HashMap<>();
            final String handler = ess.getPermissionsHandler().getName();
            final Map<String, Integer> backend = new HashMap<>();
            backend.put(ess.getPermissionsHandler().getBackendName(), 1);
            result.put(handler, backend);
            return result;
        }));
    }

    private void addEconomyChart() {
        metrics.addCustomChart(new DrilldownPie("econPlugin", () -> {
            final Map<String, Map<String, Integer>> result = new HashMap<>();
            final Map<String, Integer> backend = new HashMap<>();
            final EconomyLayer layer = EconomyLayers.getSelectedLayer();
            if (layer != null) {
                backend.put(layer.getBackendName(), 1);
                result.put(layer.getPluginName(), backend);
            } else {
                backend.put("Essentials", 1);
                result.put("Essentials", backend);
            }
            return result;
        }));
    }

    private void addVersionHistoryChart() {
        metrics.addCustomChart(new MultiLineChart("versionHistory", () -> {
            final HashMap<String, Integer> result = new HashMap<>();
            result.put(plugin.getDescription().getVersion(), 1);
            return result;
        }));
    }

    private void addReleaseBranchChart() {
        metrics.addCustomChart(new SimplePie("releaseBranch", ess.getUpdateChecker()::getVersionBranch));
    }

    private void addCommandsChart() {
        for (final String command : plugin.getDescription().getCommands().keySet()) {
            markCommand(command, false);
        }

        metrics.addCustomChart(new AdvancedBarChart("commands", () -> {
            final Map<String, int[]> result = new HashMap<>();
            for (final Map.Entry<String, Boolean> entry : commands.entrySet()) {
                if (entry.getValue()) {
                    result.put(entry.getKey(), new int[]{1, 0});
                } else {
                    result.put(entry.getKey(), new int[]{0, 1});
                }
            }
            return result;
        }));
    }

}
