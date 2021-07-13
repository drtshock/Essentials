package com.earth2me.essentials.protect;

public enum ProtectConfig {
    disable_contactdmg("protect.disable.contactdmg", false),
    disable_lavadmg("protect.disable.lavadmg", false),
    disable_lava_item_dmg("protect.prevent.lava-itemdamage", false),
    disable_pvp("protect.disable.pvp", false),
    disable_projectiles("protect.disable.projectiles", false),
    disable_fall("protect.disable.fall", false),
    disable_suffocate("protect.disable.suffocate", false),
    disable_firedmg("protect.disable.firedmg", false),
    disable_lightning("protect.disable.lightning", false),
    disable_drown("protect.disable.drown", false),
    disable_wither("protect.disable.wither", false),
    disable_weather_storm("protect.disable.weather.storm", false),
    disable_weather_lightning("protect.disable.weather.lightning", false),
    disable_weather_thunder("protect.disable.weather.thunder", false),
    prevent_fire_spread("protect.prevent.fire-spread", true),
    prevent_flint_fire("protect.prevent.flint-fire", false),
    prevent_lava_fire_spread("protect.prevent.lava-fire-spread", true),
    prevent_lightning_fire_spread("protect.prevent.lightning-fire-spread", true),
    prevent_water_flow("protect.prevent.water-flow", false),
    prevent_lava_flow("protect.prevent.lava-flow", false),
    prevent_water_bucket_flow("protect.prevent.water-bucket-flow", false),
    prevent_portal_creation("protect.prevent.portal-creation", false),
    prevent_block_on_rail("protect.protect.prevent-block-on-rails", false),
    prevent_tnt_explosion("protect.prevent.tnt-explosion", false),
    prevent_tnt_playerdmg("protect.prevent.tnt-playerdamage", false),
    prevent_tnt_itemdmg("protect.prevent.tnt-itemdamage", false),
    prevent_tntminecart_explosion("protect.prevent.tnt-minecart-explosion", false),
    prevent_tntminecart_playerdmg("protect.prevent.tnt-minecart-playerdamage", false),
    prevent_tntminecart_itemdmg("protect.prevent.tnt-minecart-itemdamage", false),
    prevent_fireball_explosion("protect.prevent.fireball-explosion", false),
    prevent_fireball_fire("protect.prevent.fireball-fire", false),
    prevent_fireball_playerdmg("protect.prevent.fireball-playerdamage", false),
    prevent_fireball_itemdmg("protect.prevent.fireball-itemdamage", false),
    prevent_witherskull_explosion("protect.prevent.witherskull-explosion", false),
    prevent_witherskull_playerdmg("protect.prevent.witherskull-playerdamage", false),
    prevent_witherskull_itemdmg("protect.prevent.witherskull-itemdamage", false),
    prevent_wither_spawnexplosion("protect.prevent.wither-spawnexplosion", false),
    prevent_wither_blockreplace("protect.prevent.wither-blockreplace", false),
    prevent_creeper_explosion("protect.prevent.creeper-explosion", true),
    prevent_creeper_playerdmg("protect.prevent.creeper-playerdamage", false),
    prevent_creeper_itemdmg("protect.prevent.creeper-itemdamage", false),
    prevent_creeper_blockdmg("protect.prevent.creeper-blockdamage", false),
    prevent_ender_crystal_explosion("protect.prevent.ender-crystal-explosion", false),
    prevent_enderman_pickup("protect.prevent.enderman-pickup", false),
    prevent_villager_death("protect.prevent.villager-death", false),
    prevent_bed_explosion("protect.prevent.bed-explosion", false),
    prevent_respawn_anchor_explosion("protect.prevent.respawn-anchor-explosion", false),
    prevent_enderdragon_blockdmg("protect.prevent.enderdragon-blockdamage", true),
    prevent_entitytarget("protect.prevent.entitytarget", false),
    enderdragon_fakeexplosions("protect.enderdragon-fakeexplosions", false),
    prevent_zombie_door_break("protect.prevent.zombie-door-break", false),
    prevent_ravager_thief("protect.prevent.ravager-thief", false),
    prevent_sheep_eat_grass("protect.prevent.sheep-eat-grass", false),
    prevent_creeper_charge("protect.prevent.transformation.charged-creeper", false),
    prevent_villager_infection("protect.prevent.transformation.zombie-villager", false),
    prevent_villager_cure("protect.prevent.transformation.villager", false),
    prevent_villager_to_witch("protect.prevent.transformation.witch", false),
    prevent_pig_transformation("protect.prevent.transformation.zombie-pigman", false),
    prevent_zombie_drowning("protect.prevent.transformation.drowned", false),
    prevent_mooshroom_switching("protect.prevent.transformation.mooshroom", false);
    lingering_potion_particles("protect.prevent.lingering.effects",false);

    private final String configName;
    private final String defValueString;
    private final boolean defValueBoolean;
    private final boolean isList;
    private final boolean isString;

    ProtectConfig(final String configName, final boolean defValueBoolean) {
        this(configName, null, defValueBoolean, false, false);
    }

    ProtectConfig(final String configName, final String defValueString, final boolean defValueBoolean, final boolean isList, final boolean isString) {
        this.configName = configName;
        this.defValueString = defValueString;
        this.defValueBoolean = defValueBoolean;
        this.isList = isList;
        this.isString = isString;
    }

    /**
     * @return the configName
     */
    public String getConfigName() {
        return configName;
    }

    /**
     * @return the default value String
     */
    public String getDefaultValueString() {
        return defValueString;
    }

    /**
     * @return the default value boolean
     */
    public boolean getDefaultValueBoolean() {
        return defValueBoolean;
    }

    public boolean isString() {
        return isString;
    }

    public boolean isList() {
        return isList;
    }
}
