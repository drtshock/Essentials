package net.essentialsx.discordlink;

import com.earth2me.essentials.IEssentialsModule;
import net.ess3.api.IUser;
import net.essentialsx.api.v2.services.discord.InteractionMember;

import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

public class AccountLinkManager implements IEssentialsModule {
    private static final char[] CODE_CHARACTERS = "abcdefghijklmnopqrstuvwxyz0123456789".toCharArray();

    private final EssentialsDiscordLink ess;
    private final AccountStorage storage;

    private final Map<String, UUID> codeToUuidMap = new ConcurrentHashMap<>();

    public AccountLinkManager(EssentialsDiscordLink ess, AccountStorage storage) {
        this.ess = ess;
        this.storage = storage;
    }

    public boolean isLinked(final UUID uuid) {
        return getDiscordId(uuid) != null;
    }

    public boolean isLinked(final String discordId) {
        return getUUID(discordId) != null;
    }

    public String createCode(final UUID uuid) throws IllegalArgumentException {
        synchronized (codeToUuidMap) {
            final Optional<Map.Entry<String, UUID>> prevCode = codeToUuidMap.entrySet().stream().filter(stringUUIDEntry -> stringUUIDEntry.getValue().equals(uuid)).findFirst();
            if (prevCode.isPresent()) {
                throw new IllegalArgumentException(prevCode.get().getKey());
            }

            final String code = generateCode();

            codeToUuidMap.put(code, uuid);
            return code;
        }
    }

    public UUID getPendingUUID(final String code) {
        synchronized (codeToUuidMap) {
            return codeToUuidMap.remove(code);
        }
    }

    public String getDiscordId(final UUID uuid) {
        return storage.getDiscordId(uuid);
    }

    public IUser getUser(final String discordId) {
        final UUID uuid = getUUID(discordId);
        if (uuid == null) {
            return null;
        }
        return ess.getEss().getUser(uuid);
    }

    public UUID getUUID(final String discordId) {
        return storage.getUUID(discordId);
    }

    public boolean removeAccount(final InteractionMember member) {
        final UUID uuid = getUUID(member.getId());
        if (storage.remove(member.getId())) {
            ess.getServer().getPluginManager().callEvent(new UserLinkStatusChangeEvent(ess.getEss().getUser(uuid), member, false));
            return true;
        }
        return false;
    }

    public boolean removeAccount(final IUser user) {
        final String id = getDiscordId(user.getBase().getUniqueId());
        if (storage.remove(user.getBase().getUniqueId())) {
            ess.getApi().getMemberById(id).thenAccept(member -> ess.getServer().getPluginManager().callEvent(new UserLinkStatusChangeEvent(user, member, false)));
            return true;
        }
        return false;
    }

    public void registerAccount(final UUID uuid, final InteractionMember member) {
        storage.add(uuid, member.getId());
        ess.getServer().getPluginManager().callEvent(new UserLinkStatusChangeEvent(ess.getEss().getUser(uuid), member, true));
    }

    private String generateCode() {
        final char[] code = new char[8];
        final Random random = ThreadLocalRandom.current();

        for (int i = 0; i < 8; i++) {
            code[i] = CODE_CHARACTERS[random.nextInt(CODE_CHARACTERS.length)];
        }
        final String result = new String(code);

        if (codeToUuidMap.containsKey(result)) {
            // If this happens, buy a lottery ticket.
            return generateCode();
        }
        return result;
    }
}
