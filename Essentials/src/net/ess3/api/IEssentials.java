package net.ess3.api;

import net.ess3.nms.PlayerLocaleProvider;
import net.ess3.nms.PotionMetaProvider;
import net.ess3.nms.SpawnEggProvider;

import java.util.Collection;

public interface IEssentials extends com.earth2me.essentials.IEssentials {

    Collection<String> getVanishedPlayersNew();

    SpawnEggProvider getSpawnEggProvider();

    PotionMetaProvider getPotionMetaProvider();

    PlayerLocaleProvider getPlayerLocaleProvider();
}
