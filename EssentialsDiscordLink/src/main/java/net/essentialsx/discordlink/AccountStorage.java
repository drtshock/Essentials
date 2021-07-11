package net.essentialsx.discordlink;

import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;

public class AccountStorage {
    private final static Logger logger = Logger.getLogger("EssentialsDiscordLink");
    private final Gson gson = new Gson();
    private final EssentialsDiscordLink plugin;
    private final File accountFile;
    private final ConcurrentHashMap<String, String> uuidToDiscordIdMap;
    private final AtomicBoolean mapDirty = new AtomicBoolean(false);
    private final ScheduledExecutorService executorService = Executors.newSingleThreadScheduledExecutor();

    public AccountStorage(final EssentialsDiscordLink plugin) throws IOException {
        this.plugin = plugin;
        this.accountFile = new File(plugin.getDataFolder(), "accounts.json");
        if (!plugin.getDataFolder().exists() && !plugin.getDataFolder().mkdirs()) {
            throw new IOException("Unable to create account file!");
        }
        if (!accountFile.exists() && !accountFile.createNewFile()) {
            throw new IOException("Unable to create account file!");
        }
        try (final Reader reader = new FileReader(accountFile)) {
            //noinspection UnstableApiUsage
            final Map<String, String> map = gson.fromJson(reader, new TypeToken<Map<String, String>>() {}.getType());
            uuidToDiscordIdMap = map == null ? new ConcurrentHashMap<>() : new ConcurrentHashMap<>(map);
        }

        executorService.scheduleAtFixedRate(() -> {
            if (!mapDirty.compareAndSet(true, false)) {
                return;
            }

            if (plugin.getEss().getSettings().isDebug()) {
                logger.log(Level.INFO, "Saving linked discord accounts to disk...");
            }

            final Map<String, String> clone;
            synchronized (uuidToDiscordIdMap) {
                clone = new HashMap<>(uuidToDiscordIdMap);
            }
            try (final Writer writer = new FileWriter(accountFile)) {
                gson.toJson(clone, writer);
            } catch (IOException e) {
                logger.log(Level.SEVERE, "Failed to save link accounts!", e);
                mapDirty.set(true); // mark the map as dirty and pray it fixes itself :D
            }
        }, 0, 10, TimeUnit.SECONDS);
    }

    public void add(final UUID uuid, final String discordId) {
        synchronized (uuidToDiscordIdMap) {
            uuidToDiscordIdMap.values().removeIf(discordId::equals);
            uuidToDiscordIdMap.put(uuid.toString(), discordId);
            queueSave();
        }
    }

    public boolean remove(final UUID uuid) {
        synchronized (uuidToDiscordIdMap) {
            final boolean success = uuidToDiscordIdMap.remove(uuid.toString()) != null;
            queueSave();
            return success;
        }
    }

    public boolean remove(final String discordId) {
        synchronized (uuidToDiscordIdMap) {
            final boolean success = uuidToDiscordIdMap.values().removeIf(discordId::equals);
            queueSave();
            return success;
        }
    }

    public UUID getUUID(final String discordId) {
        synchronized (uuidToDiscordIdMap) {
            for (Map.Entry<String, String> entry : uuidToDiscordIdMap.entrySet()) {
                if (entry.getValue().equals(discordId)) {
                    return UUID.fromString(entry.getKey());
                }
            }
        }
        return null;
    }

    public String getDiscordId(final UUID uuid) {
        synchronized (uuidToDiscordIdMap) {
            return uuidToDiscordIdMap.get(uuid.toString());
        }
    }

    public void queueSave() {
        mapDirty.set(true);
    }

    public void shutdown() {
        synchronized (uuidToDiscordIdMap) {
            try {
                executorService.shutdown();
                if (!executorService.awaitTermination(10, TimeUnit.SECONDS)) {
                    logger.log(Level.SEVERE, "Timed out while saving!");
                    executorService.shutdownNow();
                }
                if (mapDirty.get()) {
                    try (final Writer writer = new FileWriter(accountFile)) {
                        gson.toJson(uuidToDiscordIdMap, writer);
                    }
                }
            } catch (InterruptedException | IOException e) {
                logger.log(Level.SEVERE, "Failed to shutdown link accounts save!", e);
                executorService.shutdownNow();
            }
        }
    }
}
