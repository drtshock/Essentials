package net.essentialsx.discord;

import club.minnced.discord.webhook.WebhookClient;
import club.minnced.discord.webhook.WebhookClientBuilder;
import club.minnced.discord.webhook.send.WebhookMessage;
import club.minnced.discord.webhook.send.WebhookMessageBuilder;
import com.earth2me.essentials.utils.FormatUtil;
import com.earth2me.essentials.utils.VersionUtil;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.entities.Webhook;
import net.dv8tion.jda.api.events.ShutdownEvent;
import net.dv8tion.jda.api.hooks.EventListener;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.ess3.nms.refl.providers.AchievementListenerProvider;
import net.ess3.nms.refl.providers.AdvancementListenerProvider;
import net.essentialsx.api.v2.events.discord.DiscordMessageEvent;
import net.essentialsx.api.v2.services.discord.DiscordService;
import net.essentialsx.api.v2.services.discord.InteractionController;
import net.essentialsx.api.v2.services.discord.InteractionException;
import net.essentialsx.api.v2.services.discord.InteractionMember;
import net.essentialsx.api.v2.services.discord.MessageType;
import net.essentialsx.api.v2.services.discord.Unsafe;
import net.essentialsx.discord.interactions.InteractionControllerImpl;
import net.essentialsx.discord.interactions.InteractionMemberImpl;
import net.essentialsx.discord.interactions.commands.ExecuteCommand;
import net.essentialsx.discord.interactions.commands.ListCommand;
import net.essentialsx.discord.interactions.commands.MessageCommand;
import net.essentialsx.discord.listeners.BukkitListener;
import net.essentialsx.discord.listeners.DiscordCommandDispatcher;
import net.essentialsx.discord.listeners.DiscordListener;
import net.essentialsx.discord.util.ConsoleInjector;
import net.essentialsx.discord.util.DiscordUtil;
import org.bukkit.Bukkit;
import org.bukkit.event.HandlerList;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.ServicePriority;
import org.jetbrains.annotations.NotNull;

import javax.security.auth.login.LoginException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;

import static com.earth2me.essentials.I18n.tl;

public class JDADiscordService implements DiscordService {
    private final static Logger logger = Logger.getLogger("EssentialsDiscord");
    private final EssentialsDiscord plugin;
    private final Unsafe unsafe = this::getJda;

    private JDA jda;
    private Guild guild;
    private TextChannel primaryChannel;
    private WebhookClient consoleWebhook;
    private String lastConsoleId;
    private final Map<String, MessageType> registeredTypes = new HashMap<>();
    private final Map<MessageType, String> typeToChannelId = new HashMap<>();
    private final Map<String, WebhookClient> channelIdToWebhook = new HashMap<>();
    private ConsoleInjector injector;
    private DiscordCommandDispatcher commandDispatcher;
    private InteractionControllerImpl interactionController;
    private boolean invalidStartup = false;

    public JDADiscordService(EssentialsDiscord plugin) {
        this.plugin = plugin;
        for (final MessageType type : MessageType.DefaultTypes.values()) {
            registerMessageType(plugin, type);
        }
    }

    public TextChannel getChannel(String key, boolean primaryFallback) {
        long resolvedId;
        try {
            resolvedId = Long.parseLong(key);
        } catch (NumberFormatException ignored) {
            resolvedId = getSettings().getChannelId(getSettings().getMessageChannel(key));
        }

        if (isDebug()) {
            logger.log(Level.INFO, "Channel definition " + key + " resolved as " + resolvedId);
        }
        TextChannel channel = guild.getTextChannelById(resolvedId);
        if (channel == null && primaryFallback) {
            if (isDebug()) {
                logger.log(Level.WARNING, "Resolved channel id " + resolvedId + " was not found! Falling back to primary channel.");
            }
            channel = primaryChannel;
        }
        return channel;
    }

    public WebhookMessage getWebhookMessage(String message) {
        return getWebhookMessage(message, jda.getSelfUser().getAvatarUrl(), getSettings().getConsoleWebhookName(), false);
    }

    public WebhookMessage getWebhookMessage(String message, String avatarUrl, String name, boolean groupMentions) {
        return new WebhookMessageBuilder()
                .setAvatarUrl(avatarUrl)
                .setAllowedMentions(groupMentions ? DiscordUtil.ALL_MENTIONS_WEBHOOK : DiscordUtil.NO_GROUP_MENTIONS_WEBHOOK)
                .setUsername(name)
                .setContent(message)
                .build();
    }

    public void sendMessage(DiscordMessageEvent event, String message, boolean groupMentions) {
        final TextChannel channel = getChannel(event.getType().getKey(), true);

        final String strippedContent = FormatUtil.stripFormat(message);

        final String webhookChannelId = typeToChannelId.get(event.getType());
        if (webhookChannelId != null) {
            final WebhookClient client = channelIdToWebhook.get(webhookChannelId);
            if (client != null) {
                final String avatarUrl = event.getAvatarUrl() != null ? event.getAvatarUrl() : jda.getSelfUser().getAvatarUrl();
                final String name = event.getName() != null ? event.getName() : guild.getSelfMember().getEffectiveName();
                client.send(getWebhookMessage(strippedContent, avatarUrl, name, groupMentions));
                return;
            }
        }

        if (!channel.canTalk()) {
            logger.warning(tl("discordNoSendPermission", channel.getName()));
            return;
        }
        channel.sendMessage(strippedContent)
                .allowedMentions(groupMentions ? null : DiscordUtil.NO_GROUP_MENTIONS)
                .queue();
    }

    public void startup() throws LoginException, InterruptedException {
        shutdown();

        logger.log(Level.INFO, tl("discordLoggingIn"));
        if (plugin.getSettings().getBotToken().replace("INSERT-TOKEN-HERE", "").trim().isEmpty()) {
            throw new IllegalArgumentException(tl("discordErrorNoToken"));
        }

        jda = JDABuilder.createDefault(plugin.getSettings().getBotToken())
                .addEventListeners(new DiscordListener(this))
                .setContextEnabled(false)
                .setRawEventsEnabled(true)
                .build()
                .awaitReady();
        updatePresence();
        logger.log(Level.INFO, tl("discordLoggingInDone", jda.getSelfUser().getAsTag()));

        if (jda.getGuilds().isEmpty()) {
            invalidStartup = true;
            throw new IllegalArgumentException(tl("discordErrorNoGuildSize"));
        }

        guild = jda.getGuildById(plugin.getSettings().getGuildId());
        if (guild == null) {
            invalidStartup = true;
            throw new IllegalArgumentException(tl("discordErrorNoGuild"));
        }

        interactionController = new InteractionControllerImpl(this);
        // Each will throw an exception if disabled
        try {
            interactionController.registerCommand(new ExecuteCommand(this));
        } catch (InteractionException ignored) {
        }
        try {
            interactionController.registerCommand(new MessageCommand(this));
        } catch (InteractionException ignored) {
        }
        try {
            interactionController.registerCommand(new ListCommand(this));
        } catch (InteractionException ignored) {
        }

        updatePrimaryChannel();

        updateConsoleRelay();

        updateTypesRelay();

        Bukkit.getPluginManager().registerEvents(new BukkitListener(this), plugin);

        try {
            if (VersionUtil.getServerBukkitVersion().isHigherThanOrEqualTo(VersionUtil.v1_12_0_R01)) {
                Bukkit.getPluginManager().registerEvents(new AdvancementListenerProvider(), plugin);
            } else {
                Bukkit.getPluginManager().registerEvents(new AchievementListenerProvider(), plugin);
            }
        } catch (final Throwable e) {
            logger.log(Level.WARNING, "Error while loading the achievement/advancement listener. You will not receive achievement/advancement notifications on Discord.", e);
        }

        getPlugin().getEss().scheduleSyncDelayedTask(() -> DiscordUtil.dispatchDiscordMessage(JDADiscordService.this, MessageType.DefaultTypes.SERVER_START, getSettings().getStartMessage(), true, null, null, null));

        Bukkit.getServicesManager().register(DiscordService.class, this, plugin, ServicePriority.Normal);
    }

    @Override
    public boolean isRegistered(String key) {
        return registeredTypes.containsKey(key);
    }

    @Override
    public void registerMessageType(Plugin plugin, MessageType type) {
        if (!type.getKey().matches("^[a-z0-9-]*$")) {
            throw new IllegalArgumentException("MessageType key must match \"^[a-z0-9-]*$\"");
        }

        if (registeredTypes.containsKey(type.getKey())) {
            throw new IllegalArgumentException("A MessageType with that key is already registered!");
        }

        registeredTypes.put(type.getKey(), type);
    }

    @Override
    public void sendMessage(MessageType type, String message, boolean allowGroupMentions) {
        if (!registeredTypes.containsKey(type.getKey())) {
            logger.warning("Sending message to channel \"" + type.getKey() + "\" which is an unregistered type! If you are a plugin author, you should be registering your MessageType before using them.");
        }
        final DiscordMessageEvent event = new DiscordMessageEvent(type, FormatUtil.stripFormat(message), allowGroupMentions);
        if (Bukkit.getServer().isPrimaryThread()) {
            Bukkit.getPluginManager().callEvent(event);
        } else {
            Bukkit.getScheduler().runTask(plugin, () -> Bukkit.getPluginManager().callEvent(event));
        }
    }

    @Override
    public InteractionController getInteractionController() {
        return interactionController;
    }

    public void updatePrimaryChannel() {
        TextChannel channel = guild.getTextChannelById(plugin.getSettings().getPrimaryChannelId());
        if (channel == null) {
            channel = guild.getDefaultChannel();
            if (channel == null || !channel.canTalk()) {
                throw new RuntimeException(tl("discordErrorNoPerms"));
            }
        }
        primaryChannel = channel;
    }

    public void updatePresence() {
        jda.getPresence().setPresence(plugin.getSettings().getStatus(), plugin.getSettings().getStatusActivity());
    }

    public void updateTypesRelay() {
        if (!getSettings().isShowAvatar() && !getSettings().isShowName() && !getSettings().isShowDisplayName()) {
            for (WebhookClient webhook : channelIdToWebhook.values()) {
                webhook.close();
            }
            typeToChannelId.clear();
            channelIdToWebhook.clear();
            return;
        }

        for (MessageType type : MessageType.DefaultTypes.values()) {
            if (!type.isPlayer()) {
                continue;
            }

            final TextChannel channel = getChannel(type.getKey(), true);
            if (channel.getId().equals(typeToChannelId.get(type))) {
                continue;
            }

            final String webhookName = "EssX Advanced Relay";
            Webhook webhook = DiscordUtil.getAndCleanWebhooks(channel, webhookName).join();
            webhook = webhook == null ? DiscordUtil.createWebhook(channel, webhookName).join() : webhook;
            if (webhook == null) {
                final WebhookClient current = channelIdToWebhook.get(channel.getId());
                if (current != null) {
                    current.close();
                }
                channelIdToWebhook.remove(channel.getId());
                continue;
            }
            typeToChannelId.put(type, channel.getId());
            channelIdToWebhook.put(channel.getId(), DiscordUtil.getWebhookClient(webhook.getIdLong(), webhook.getToken(), jda.getHttpClient()));
        }
    }

    public void updateConsoleRelay() {
        final String consoleDef = getSettings().getConsoleChannelDef();
        final Matcher matcher = WebhookClientBuilder.WEBHOOK_PATTERN.matcher(consoleDef);
        final long webhookId;
        final String webhookToken;
        if (matcher.matches()) {
            webhookId = Long.parseUnsignedLong(matcher.group(1));
            webhookToken = matcher.group(2);
            if (commandDispatcher != null) {
                jda.removeEventListener(commandDispatcher);
                commandDispatcher = null;
            }
        } else {
            final TextChannel channel = getChannel(consoleDef, false);
            if (channel != null) {
                if (getSettings().isConsoleCommandRelay()) {
                    if (commandDispatcher == null) {
                        commandDispatcher = new DiscordCommandDispatcher(this);
                        jda.addEventListener(commandDispatcher);
                    }
                    commandDispatcher.setChannelId(channel.getId());
                } else if (commandDispatcher != null) {
                    jda.removeEventListener(commandDispatcher);
                    commandDispatcher = null;
                }

                if (channel.getId().equals(lastConsoleId)) {
                    return;
                }

                final String webhookName = "EssX Console Relay";
                Webhook webhook = DiscordUtil.getAndCleanWebhooks(channel, webhookName).join();
                webhook = webhook == null ? DiscordUtil.createWebhook(channel, webhookName).join() : webhook;
                if (webhook == null) {
                    logger.info(tl("discordErrorLoggerNoPerms"));
                    return;
                }
                webhookId = webhook.getIdLong();
                webhookToken = webhook.getToken();
                lastConsoleId = channel.getId();
            } else if (!getSettings().getConsoleChannelDef().equals("none") && !getSettings().getConsoleChannelDef().startsWith("0")) {
                logger.info(tl("discordErrorLoggerInvalidChannel"));
                shutdownConsoleRelay(true);
                return;
            } else {
                // It's either not configured at all or knowingly disabled.
                shutdownConsoleRelay(true);
                return;
            }
        }

        shutdownConsoleRelay(false);
        consoleWebhook = DiscordUtil.getWebhookClient(webhookId, webhookToken, jda.getHttpClient());
        if (injector == null) {
            injector = new ConsoleInjector(this);
            injector.start();
        }
    }

    private void shutdownConsoleRelay(final boolean closeInjector) {
        if (consoleWebhook != null && !consoleWebhook.isShutdown()) {
            consoleWebhook.close();
        }
        consoleWebhook = null;

        if (closeInjector) {
            if (injector != null) {
                injector.remove();
                injector = null;
            }

            if (commandDispatcher != null) {
                jda.removeEventListener(commandDispatcher);
                commandDispatcher = null;
            }
        }
    }

    public void shutdown() {
        if (interactionController != null) {
            interactionController.shutdown();
        }

        if (jda != null) {
            if (!invalidStartup) {
                sendMessage(MessageType.DefaultTypes.SERVER_STOP, getSettings().getStopMessage(), true);
                DiscordUtil.dispatchDiscordMessage(JDADiscordService.this, MessageType.DefaultTypes.SERVER_STOP, getSettings().getStopMessage(), true, null, null, null);
            }

            shutdownConsoleRelay(true);

            // Unregister leftover jda listeners
            for (Object obj : jda.getRegisteredListeners()) {
                if (!(obj instanceof EventListener)) { // Yeah bro I wish I knew too :/
                    jda.removeEventListener(obj);
                }
            }

            // Unregister Bukkit Events
            HandlerList.unregisterAll(plugin);

            // Creates a future which will be completed when JDA fully shutdowns
            final CompletableFuture<Void> future = new CompletableFuture<>();
            jda.addEventListener(new ListenerAdapter() {
                @Override
                public void onShutdown(@NotNull ShutdownEvent event) {
                    future.complete(null);
                }
            });

            // Tell JDA to wrap it up
            jda.shutdown();
            try {
                // Wait for JDA to wrap it up
                future.get(5, TimeUnit.SECONDS);
            } catch (InterruptedException | ExecutionException | TimeoutException e) {
                logger.warning("JDA took longer than expected to shutdown, this may have caused some problems.");
            } finally {
                jda = null;
            }
        }
    }

    @Override
    public CompletableFuture<InteractionMember> getMemberById(final String id) {
        final CompletableFuture<InteractionMember> future = new CompletableFuture<>();
        getGuild().retrieveMemberById(id).queue(member -> {
            if (member != null) {
                future.complete(new InteractionMemberImpl(member));
                return;
            }
            future.complete(null);
        });
        return future;
    }

    public JDA getJda() {
        return jda;
    }

    @Override
    public Unsafe getUnsafe() {
        return unsafe;
    }

    public Guild getGuild() {
        return guild;
    }

    public EssentialsDiscord getPlugin() {
        return plugin;
    }

    public DiscordSettings getSettings() {
        return plugin.getSettings();
    }

    public WebhookClient getConsoleWebhook() {
        return consoleWebhook;
    }

    public boolean isInvalidStartup() {
        return invalidStartup;
    }

    public boolean isDebug() {
        return plugin.getEss().getSettings().isDebug();
    }
}
