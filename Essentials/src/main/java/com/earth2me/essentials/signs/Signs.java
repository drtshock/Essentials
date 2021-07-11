package com.earth2me.essentials.signs;

//This enum is used when checking to see what signs are enabled
public enum Signs {
    ANVIL(new SignAnvil()),
    BALANCE(new SignBalance()),
    BUY(new SignBuy()),
    CARTOGRAPHY(new SignCartography()),
    DISPOSAL(new SignDisposal()),
    ENCHANT(new SignEnchant()),
    FREE(new SignFree()),
    GAMEMODE(new SignGameMode()),
    GRINDSTONE(new SignGrindstone()),
    HEAL(new SignHeal()),
    INFO(new SignInfo()),
    KIT(new SignKit()),
    LOOM(new SignLoom()),
    MAIL(new SignMail()),
    PROTECTION(new SignProtection()),
    REPAIR(new SignRepair()),
    SELL(new SignSell()),
    SMITHING(new SignSmithing()),
    SPAWNMOB(new SignSpawnmob()),
    TIME(new SignTime()),
    TRADE(new SignTrade()),
    WARP(new SignWarp()),
    WEATHER(new SignWeather()),
    WORKBENCH(new SignWorkbench()),
    RANDOMTELEPORT(new SignRandomTeleport());
    private final EssentialsSign sign;

    Signs(final EssentialsSign sign) {
        this.sign = sign;
    }

    public EssentialsSign getSign() {
        return sign;
    }
}
