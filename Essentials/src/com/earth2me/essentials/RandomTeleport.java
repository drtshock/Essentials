package com.earth2me.essentials;

import com.earth2me.essentials.utils.LocationUtil;
import com.earth2me.essentials.utils.VersionUtil;
import io.papermc.lib.PaperLib;
import net.ess3.api.InvalidWorldException;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Biome;

import java.io.File;
import java.util.EnumSet;
import java.util.Queue;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

public class RandomTeleport implements IConf {
    private final IEssentials essentials;
    private final EssentialsConf config;
    private final ConcurrentHashMap<String, ConcurrentLinkedQueue<Location>> cache = new ConcurrentHashMap<>();
    private static final Random RANDOM = new Random();
    private static final int HIGHEST_BLOCK_Y_OFFSET = VersionUtil.getServerBukkitVersion().isHigherThanOrEqualTo(VersionUtil.v1_15_R01) ? 1 : 0;

    public RandomTeleport(final IEssentials essentials) {
        this.essentials = essentials;
        File file = new File(essentials.getDataFolder(), "tpr.yml");
        config = new EssentialsConf(file);
        config.setTemplateName("/tpr.yml");
        config.options().copyHeader(true);
        reloadConfig();
    }

    @Override
    public void reloadConfig() {
        config.load();
        cache.clear();
    }

    public boolean getPerWorld() {
        return config.getBoolean("per-world", false);
    }

    public void setPerWorld(boolean perWorld) {
        config.setProperty("per-world", perWorld);
        config.save();
    }

    private String getWorldConfigKey(World world, String key) {
        return (world == null || !getPerWorld() ? "" : "world." + world.getName() + ".") + key;
    }

    public Location getCenter(World world) {
        try {
            Location center = config.getLocation(getWorldConfigKey(world, "center"), essentials.getServer());
            if (center != null) {
                return center;
            }
        } catch (InvalidWorldException ignored) {
        }
        World defaultWorld = world == null || !getPerWorld() ? essentials.getServer().getWorlds().get(0) : world;
        Location center = defaultWorld.getWorldBorder().getCenter();
        center.setY(center.getWorld().getHighestBlockYAt(center) + HIGHEST_BLOCK_Y_OFFSET);
        setCenter(world, center);
        return center;
    }

    public void setCenter(World world, Location center) {
        config.setProperty(getWorldConfigKey(world, "center"), center);
        config.save();
    }

    public double getMinRange(World world) {
        return config.getDouble(getWorldConfigKey(world, "min-range"), 0d);
    }

    public void setMinRange(World world, double minRange) {
        config.setProperty(getWorldConfigKey(world, "min-range"), minRange);
        config.save();
    }

    public double getMaxRange(World world) {
        return config.getDouble(getWorldConfigKey(world, "max-range"), getCenter(world).getWorld().getWorldBorder().getSize() / 2);
    }

    public void setMaxRange(World world, double maxRange) {
        config.setProperty(getWorldConfigKey(world, "max-range"), maxRange);
        config.save();
    }

    public Set<Biome> getExcludedBiomes() {
        final EnumSet<Biome> excludedBiomes = EnumSet.noneOf(Biome.class);
        config.getStringList("excluded-biomes").forEach(biome -> {
            try {
                excludedBiomes.add(Biome.valueOf(biome.toUpperCase()));
            } catch (IllegalArgumentException ignored) {
            }
        });
        return excludedBiomes;
    }

    public int getFindAttempts() {
        return config.getInt("find-attempts", 10);
    }

    public int getCacheThreshold() {
        return config.getInt("cache-threshold", 10);
    }

    public boolean getPreCache() {
        return config.getBoolean("pre-cache", false);
    }

    public Queue<Location> getCachedLocations(Location center, double minRange, double maxRange) {
        String cacheKey = String.valueOf(center.getWorld()) + minRange + maxRange;
        cache.putIfAbsent(cacheKey, new ConcurrentLinkedQueue<>());
        return cache.get(cacheKey);
    }

    // Get a random location; cached if possible. Otherwise on demand.
    public CompletableFuture<Location> getRandomLocation(Location center, double minRange, double maxRange) {
        int findAttempts = this.getFindAttempts();
        Queue<Location> cachedLocations = this.getCachedLocations(center, minRange, maxRange);
        // Try to build up the cache if it is below the threshold
        if (cachedLocations.size() < this.getCacheThreshold()) {
            cacheRandomLocations(center, minRange, maxRange);
        }
        CompletableFuture<Location> future = new CompletableFuture<>();
        // Return a random location immediately if one is available, otherwise try to find one now
        if (cachedLocations.isEmpty()) {
            attemptRandomLocation(findAttempts, center, minRange, maxRange).thenAccept(future::complete);
        } else {
            future.complete(cachedLocations.poll());
        }
        return future;
    }

    // Prompts caching random valid locations, up to a maximum number of attempts
    public void cacheRandomLocations(Location center, double minRange, double maxRange) {
        essentials.getServer().getScheduler().scheduleSyncDelayedTask(essentials, () -> {
            for (int i = 0; i < this.getFindAttempts(); ++i) {
                calculateRandomLocation(center, minRange, maxRange).thenAccept(location -> {
                    if (isValidRandomLocation(location)) {
                        this.getCachedLocations(center, minRange, maxRange).add(location);
                    }
                });
            }
        });
    }

    // Recursively attempt to find a random location. After a maximum number of attempts, the center is returned.
    private CompletableFuture<Location> attemptRandomLocation(int attempts, Location center, double minRange, double maxRange) {
        CompletableFuture<Location> future = new CompletableFuture<>();
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
    private CompletableFuture<Location> calculateRandomLocation(Location center, double minRange, double maxRange) {
        CompletableFuture<Location> future = new CompletableFuture<>();
        // Find an equally distributed offset by randomly rotating a point inside a rectangle about the origin
        double rectX = RANDOM.nextDouble() * (maxRange - minRange) + minRange;
        double rectZ = RANDOM.nextDouble() * (maxRange + minRange) - minRange;
        double offsetX, offsetZ;
        int transform = RANDOM.nextInt(4);
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
        Location location = new Location(
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

    // Returns an appropriate elevation for a given location in the nether, or -1 if none is found
    private double getNetherYAt(Location location) {
        for (int y = 32; y < location.getWorld().getMaxHeight() / 2; ++y) {
            if (!LocationUtil.isBlockUnsafe(location.getWorld(), location.getBlockX(), y, location.getBlockZ())) {
                return y;
            }
        }
        return -1;
    }

    private boolean isValidRandomLocation(Location location) {
        return location.getBlockY() > 0 && !this.getExcludedBiomes().contains(location.getBlock().getBiome());
    }
}
