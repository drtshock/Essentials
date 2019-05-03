package net.ess3.nms.refl;

import net.ess3.nms.PlayerLocaleProvider;
import org.bukkit.entity.Player;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class ReflPlayerLocaleProvider extends PlayerLocaleProvider {

    private final Method getHandleMethod;
    private final Field localeField;

    public ReflPlayerLocaleProvider() {
        Class<?> craftPlayerClass = ReflUtil.getOBCClass("entity.CraftPlayer");
        Class<?> nmsEntityPlayerClass = ReflUtil.getNMSClass("EntityPlayer");
        if (craftPlayerClass != null && nmsEntityPlayerClass != null) {
            getHandleMethod = ReflUtil.getMethodCached(craftPlayerClass, "getHandle");
            localeField = ReflUtil.getFieldCached(nmsEntityPlayerClass, "locale");
        } else {
            getHandleMethod = null;
            localeField = null;
        }
    }

    @Override
    public String getLocale(Player player) {
        if (tryProvider()) {
            try {
                Object nmsEntityPlayer = getHandleMethod.invoke(player);
                if (nmsEntityPlayer != null) {
                    return (String) localeField.get(nmsEntityPlayer);
                }
            } catch (IllegalAccessException | InvocationTargetException ignored) {
            }
        }
        return null;
    }

    @Override
    public boolean tryProvider() {
        return getHandleMethod != null && localeField != null;
    }

    @Override
    public String getHumanName() {
        return "api player locale provider";
    }
}
