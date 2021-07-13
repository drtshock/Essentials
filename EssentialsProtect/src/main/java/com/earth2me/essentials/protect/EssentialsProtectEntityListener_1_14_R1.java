package com.earth2me.essentials.protect;

import org.bukkit.entity.EntityType;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityChangeBlockEvent;

public class EssentialsProtectEntityListener_1_14_R1 implements Listener {
    private final IProtect prot;

    EssentialsProtectEntityListener_1_14_R1(final IProtect prot) {
        this.prot = prot;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onEntityChangeBlock(final EntityChangeBlockEvent event) {
        if (event.getEntityType() == EntityType.RAVAGER && prot.getSettingBool(ProtectConfig.prevent_ravager_thief)) {
            event.setCancelled(true);
        }
    }
   @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
   public void onLingeringPotionSplasEvent(final LingeringPotionSplashEvent event){
       if (prot.getSettingBool(ProtectConfig.lingering_potion_particles)) {
       event.setCancelled(true);
           }
   }
   @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
   public void onAreaEffectCloudEvent(final AreaEffectCloudApplyEvent event){
       if (prot.getSettingBool(ProtectConfig.lingering_potion_particles)) {
           event.setCancelled(true);
       }
   }
}
