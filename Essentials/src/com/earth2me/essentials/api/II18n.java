package com.earth2me.essentials.api;

import java.util.Locale;


public interface II18n {
    /**
     * Gets the current default locale setting
     *
     * @return the current locale, if not set it will return the default locale
     */
    Locale getCurrentLocale();
}
