package com.earth2me.essentials;

import com.earth2me.essentials.config.ConfigurateUtil;
import com.earth2me.essentials.config.EssentialsConfiguration;
import com.earth2me.essentials.config.entities.LazyLocation;
import com.earth2me.essentials.utils.LocationUtil;
import com.earth2me.essentials.utils.VersionUtil;
import io.papermc.lib.PaperLib;
import net.ess3.api.InvalidWorldException;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Biome;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentLinkedQueue;

public class RandomTeleport implements IConf {
    private static final Random RANDOM = new Random();
    private static final int HIGHEST_BLOCK_Y_OFFSET = VersionUtil.getServerBukkitVersion().isHigherThanOrEqualTo(VersionUtil.v1_15_R01) ? 1 : 0;
    private final IEssentials essentials;
    private final EssentialsConfiguration config;
    private final Map<String, ConcurrentLinkedQueue<Location>> cachedLocations = new HashMap<>();

    public RandomTeleport(final IEssentials essentials) {
        this.essentials = essentials;
        config = new EssentialsConfiguration(new File(essentials.getDataFolder(), "tpr.yml"), "/tpr.yml",
                "Configuration for the random teleport command.\nUse the /settpr command in-game to set random teleport locations.");
        reloadConfig();
    }

    @Override
    public void reloadConfig() {
        config.load();
        cachedLocations.clear();
    }

    public void updateConfig() {
        try {
            final LazyLocation center = config.getLocation("center");
            if (center != null && center.location() != null) {
                final double minRange = config.getDouble("min-range", Double.MIN_VALUE);
                final double maxRange = config.getDouble("max-range", Double.MIN_VALUE);
                for (World world : essentials.getServer().getWorlds()) {
                    setCenter(world.getName(), center.location());
                    if (minRange != Double.MIN_VALUE) {
                        setMinRange(world.getName(), minRange);
                    }
                    if (maxRange != Double.MIN_VALUE) {
                        setMaxRange(world.getName(), maxRange);
                    }
                }
            }
            config.removeProperty("center");
        } catch (InvalidWorldException ignored) {
        }
    }

    public Location getCenter(final String name) {
        try {
            final LazyLocation center = config.getLocation(locationKey(name, "center"));
            if (center != null && center.location() != null) {
                return center.location();
            }
        } catch (final InvalidWorldException ignored) {
        }
        final Location center = essentials.getServer().getWorlds().get(0).getWorldBorder().getCenter();
        center.setY(center.getWorld().getHighestBlockYAt(center) + HIGHEST_BLOCK_Y_OFFSET);
        setCenter(name, center);
        return center;
    }

    public void setCenter(final String name, final Location center) {
        config.setProperty(locationKey(name, "center"), center);
        config.save();
    }

    public double getMinRange(final String name) {
        return config.getDouble(locationKey(name, "min-range"), 0d);
    }

    public void setMinRange(final String name, final double minRange) {
        config.setProperty(locationKey(name, "min-range"), minRange);
        config.save();
    }

    public double getMaxRange(final String name) {
        return config.getDouble(locationKey(name, "max-range"), getCenter(name).getWorld().getWorldBorder().getSize() / 2);
    }

    public void setMaxRange(final String name, final double maxRange) {
        config.setProperty(locationKey(name, "max-range"), maxRange);
        config.save();
    }

    public String getDefaultLocation() {
        return config.getString("default-location", "{world}");
    }

    public boolean isPerLocationPermission() {
        return config.getBoolean("per-location-permission", false);
    }

    public Set<Biome> getExcludedBiomes() {
        final List<String> biomeNames = config.getList("excluded-biomes", String.class);
        final Set<Biome> excludedBiomes = new HashSet<>();
        for (final String biomeName : biomeNames) {
            try {
                excludedBiomes.add(Biome.valueOf(biomeName.toUpperCase()));
            } catch (final IllegalArgumentException ignored) {
            }
        }
        return excludedBiomes;
    }

    public int getFindAttempts() {
        return config.getInt("find-attempts", 10);
    }

    public int getCacheThreshold() {
        return config.getInt("cache-threshold", 10);
    }

    public List<String> listLocations() {
        return new ArrayList<>(ConfigurateUtil.getKeys(config.getRootNode().node("locations")));
    }

    public Queue<Location> getCachedLocations(final String name) {
        this.cachedLocations.computeIfAbsent(name, x -> new ConcurrentLinkedQueue<>());
        return cachedLocations.get(name);
    }

    // Get a named random teleport location; cached if possible, otherwise on demand.
    public CompletableFuture<Location> getRandomLocation(final String name) {
        final Queue<Location> cached = this.getCachedLocations(name);
        // Try to build up the cache if it is below the threshold
        if (cached.size() < this.getCacheThreshold()) {
            cacheRandomLocations(name);
        }
        final CompletableFuture<Location> future = new CompletableFuture<>();
        // Return a random location immediately if one is available, otherwise try to find one now
        if (cached.isEmpty()) {
            final int findAttempts = this.getFindAttempts();
            final Location center = this.getCenter(name);
            final double minRange = this.getMinRange(name);
            final double maxRange = this.getMaxRange(name);
            attemptRandomLocation(findAttempts, center, minRange, maxRange).thenAccept(future::complete);
        } else {
            future.complete(cached.poll());
        }
        return future;
    }

    // Get a random location with specific parameters (note: not cached).
    public CompletableFuture<Location> getRandomLocation(final Location center, final double minRange, final double maxRange) {
        return attemptRandomLocation(this.getFindAttempts(), center, minRange, maxRange);
    }

    // Prompts caching random valid locations, up to a maximum number of attempts.
    public void cacheRandomLocations(final String name) {
        essentials.getServer().getScheduler().scheduleSyncDelayedTask(essentials, () -> {
            for (int i = 0; i < this.getFindAttempts(); ++i) {
                calculateRandomLocation(getCenter(name), getMinRange(name), getMaxRange(name)).thenAccept(location -> {
                    if (isValidRandomLocation(location)) {
                        this.getCachedLocations(name).add(location);
                    }
                });
            }
        });
    }

    // Recursively attempt to find a random location. After a maximum number of attempts, the center is returned.
    private CompletableFuture<Location> attemptRandomLocation(final int attempts, final Location center, final double minRange, final double maxRange) {
        final CompletableFuture<Location> future = new CompletableFuture<>();
        if (attempts > 0) {
            calculateRandomLocation(center, minRange, maxRange).thenAccept(location -> {
                if (isValidRandomLocation(location)) {
                    future.complete(location);
                } else {
                    attemptRandomLocation(attempts - 1, center, minRange, maxRange).thenAccept(future::complete);
                }
            });
        } else {
            future.complete(center);
        }
        return future;
    }

    // Calculates a random location asynchronously.
    private CompletableFuture<Location> calculateRandomLocation(final Location center, final double minRange, final double maxRange) {
        final CompletableFuture<Location> future = new CompletableFuture<>();
        // Find an equally distributed offset by randomly rotating a point inside a rectangle about the origin
        final double rectX = RANDOM.nextDouble() * (maxRange - minRange) + minRange;
        final double rectZ = RANDOM.nextDouble() * (maxRange + minRange) - minRange;
        final double offsetX;
        final double offsetZ;
        final int transform = RANDOM.nextInt(4);
        if (transform == 0) {
            offsetX = rectX;
            offsetZ = rectZ;
        } else if (transform == 1) {
            offsetX = -rectZ;
            offsetZ = rectX;
        } else if (transform == 2) {
            offsetX = -rectX;
            offsetZ = -rectZ;
        } else {
            offsetX = rectZ;
            offsetZ = -rectX;
        }
        final Location location = new Location(
                center.getWorld(),
                center.getX() + offsetX,
                center.getWorld().getMaxHeight(),
                center.getZ() + offsetZ,
                360 * RANDOM.nextFloat() - 180,
                0
        );
        PaperLib.getChunkAtAsync(location).thenAccept(chunk -> {
            if (World.Environment.NETHER.equals(center.getWorld().getEnvironment())) {
                location.setY(getNetherYAt(location));
            } else {
                location.setY(center.getWorld().getHighestBlockYAt(location) + HIGHEST_BLOCK_Y_OFFSET);
            }
            future.complete(location);
        });
        return future;
    }

    // Returns an appropriate elevation for a given location in the nether, or MIN_VALUE if none is found
    private double getNetherYAt(final Location location) {
        for (int y = 32; y < location.getWorld().getMaxHeight() / 2; ++y) {
            if (!LocationUtil.isBlockUnsafe(location.getWorld(), location.getBlockX(), y, location.getBlockZ())) {
                return y;
            }
        }
        return Double.MIN_VALUE;
    }

    private boolean isValidRandomLocation(final Location location) {
        final int minHeight = VersionUtil.getServerBukkitVersion().isHigherThanOrEqualTo(VersionUtil.v1_17_R01) ? location.getWorld().getMinHeight() : 0;
        return location.getBlockY() >= minHeight && !this.getExcludedBiomes().contains(location.getBlock().getBiome());
    }

    private String locationKey(String name, String key) {
        return "locations." + name + "." + key;
    }
}
