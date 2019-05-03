package com.earth2me.essentials.geoip;

import com.earth2me.essentials.EssentialsConf;
import com.earth2me.essentials.IConf;
import com.earth2me.essentials.User;
import com.maxmind.geoip2.DatabaseReader;
import com.maxmind.geoip2.model.CityResponse;
import com.maxmind.geoip2.model.CountryResponse;
import com.maxmind.geoip2.exception.*;
import net.ess3.api.IEssentials;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

import java.io.*;
import java.net.*;
import java.util.logging.Level;
import java.util.Date;
import java.util.logging.Logger;
import java.util.zip.GZIPInputStream;
import java.util.Arrays;
import com.ice.tar.TarInputStream;
import com.ice.tar.TarEntry;

import static com.earth2me.essentials.I18n.tl;


public class EssentialsGeoIPPlayerListener implements Listener, IConf {
    private DatabaseReader mmreader = null; // initialize maxmind geoip2 reader
    private static final Logger logger = Logger.getLogger("EssentialsGeoIP");
    private File databaseFile;
    private File dataFolder;
    private final EssentialsConf config;
    private final transient IEssentials ess;

    EssentialsGeoIPPlayerListener(File dataFolder, IEssentials ess) {
        this.ess = ess;
        this.dataFolder = dataFolder;
        this.config = new EssentialsConf(new File(dataFolder, "config.yml"));
        config.setTemplateName("/config.yml", EssentialsGeoIP.class);
        reloadConfig();
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerJoin(final PlayerJoinEvent event) {
        ess.runTaskAsynchronously(() -> delayedJoin(event.getPlayer()));
    }

    private void delayedJoin(Player player) {
        User u = ess.getUser(player);
        if (u.isAuthorized("essentials.geoip.hide") || player.getAddress() == null) {
            return;
        }
        InetAddress address = player.getAddress().getAddress();
        StringBuilder sb = new StringBuilder();


        try {
            if (config.getBoolean("database.show-cities", false)) {
                CityResponse response = mmreader.city(address);
                if (response == null) {
                    return;
                }
                String city;
                String region;
                String country;
                city = response.getCity().getName();
                region = response.getMostSpecificSubdivision().getName();
                country = response.getCountry().getName();
                if (city != null) {
                    sb.append(city).append(", ");
                }
                if (region != null) {
                    sb.append(region).append(", ");
                }
                sb.append(country);
            } else {
                CountryResponse response = mmreader.country(address);
                sb.append(response.getCountry().getName());
            }
        } catch (AddressNotFoundException ex) {

            if (checkIfLocal(address)) {
                for (Player online : player.getServer().getOnlinePlayers()) {
                    User user = ess.getUser(online);
                    if (user.isAuthorized("essentials.geoip.show")) {
                        user.sendMessage(tl("geoipCantFind", u.getDisplayName()));
                    }
                }
                return;
            }
            // GeoIP2 API forced this when address not found in their DB. jar will not complied without this.
            // TODO: Maybe, we can set a new custom msg about addr-not-found in messages.properties.
            logger.log(Level.INFO, tl("cantReadGeoIpDB") + " " + ex.getLocalizedMessage());
        } catch (IOException | GeoIp2Exception ex) {
            // GeoIP2 API forced this when address not found in their DB. jar will not complied without this.
            logger.log(Level.SEVERE, tl("cantReadGeoIpDB") + " " + ex.getLocalizedMessage());
        }
        if (config.getBoolean("show-on-whois", true)) {
            u.setGeoLocation(sb.toString());
        }
        if (config.getBoolean("show-on-login", true) && !u.isHidden()) {
            for (Player onlinePlayer : player.getServer().getOnlinePlayers()) {
                User user = ess.getUser(onlinePlayer);
                if (user.isAuthorized("essentials.geoip.show")) {
                    user.sendTl("geoipJoinFormat", u.getDisplayName(), sb.toString());
                }
            }
        }
    }

    @Override
    public final void reloadConfig() {
        config.load();

        // detect and update the old config.yml. migrate from legacy GeoIP to GeoIP2.
        if (!config.isSet("enable-locale")) {
            config.set("database.download-url", "https://geolite.maxmind.com/download/geoip/database/GeoLite2-Country.tar.gz");
            config.set("database.download-url-city", "https://geolite.maxmind.com/download/geoip/database/GeoLite2-City.tar.gz");
            config.set("database.update.enable", true);
            config.set("database.update.by-every-x-days", 30);
            config.set("enable-locale", true);
            config.save();
            // delete old GeoIP.dat fiiles
            File oldDatFile = new File(dataFolder, "GeoIP.dat");
            File oldDatFileCity = new File(dataFolder, "GeoIP-City.dat");
            oldDatFile.delete();
            oldDatFileCity.delete();
        }

        if (config.getBoolean("database.show-cities", false)) {
            databaseFile = new File(dataFolder, "GeoIP2-City.mmdb");
        } else {
            databaseFile = new File(dataFolder, "GeoIP2-Country.mmdb");
        }
        if (!databaseFile.exists()) {
            if (config.getBoolean("database.download-if-missing", true)) {
                downloadDatabase();
            } else {
                logger.log(Level.SEVERE, tl("cantFindGeoIpDB"));
                return;
            }
        } else if (config.getBoolean("database.update.enable", true)) {
            // try to update expired mmdb files
            long diff = new Date().getTime() - databaseFile.lastModified();
            if (diff/24/3600/1000>config.getLong("database.update.by-every-x-days", 30)) {
                downloadDatabase();
            }
        }
        try {
            // locale setting
            if (config.getBoolean("enable-locale")) {
                // Get geolocation based on Essentials' locale. If the locale is not avaliable, use "en".
                String locale = ess.getI18n().getCurrentLocale().toString().replace('_', '-');
                mmreader = new DatabaseReader.Builder(databaseFile).locales(Arrays.asList(locale,"en")).build();
            } else {
                mmreader = new DatabaseReader.Builder(databaseFile).build();
            }
        } catch (IOException ex) {
            logger.log(Level.SEVERE, tl("cantReadGeoIpDB"), ex);
        }
    }

    private void downloadDatabase() {
        try {
            String url;
            if (config.getBoolean("database.show-cities", false)) {
                url = config.getString("database.download-url-city");
            } else {
                url = config.getString("database.download-url");
            }
            if (url == null || url.isEmpty()) {
                logger.log(Level.SEVERE, tl("geoIpUrlEmpty"));
                return;
            }
            logger.log(Level.INFO, tl("downloadingGeoIp"));
            URL downloadUrl = new URL(url);
            URLConnection conn = downloadUrl.openConnection();
            conn.setConnectTimeout(10000);
            conn.connect();
            InputStream input = conn.getInputStream();
            OutputStream output = new FileOutputStream(databaseFile);
            byte[] buffer = new byte[2048];
            if (url.endsWith(".gz")) {
                input = new GZIPInputStream(input);
                if (url.endsWith(".tar.gz")) {
                    // The new GeoIP2 uses tar.gz to pack the db file along with some other txt. So it makes things a bit complicated here.
                    String filename;
                    TarInputStream tarInputStream = new TarInputStream(input);
                    TarEntry entry;
                    while ((entry = tarInputStream.getNextEntry()) != null) {
                        if (!entry.isDirectory()) {
                            filename = entry.getName();
                            if (filename.substring(filename.length() - 5).equalsIgnoreCase(".mmdb")) {
                                input = tarInputStream;
                                break;
                            }
                        }
                    }
                }
            }
            int length = input.read(buffer);
            while (length >= 0) {
                output.write(buffer, 0, length);
                length = input.read(buffer);
            }
            output.close();
            input.close();
        } catch (MalformedURLException ex) {
            logger.log(Level.SEVERE, tl("geoIpUrlInvalid"), ex);
        } catch (IOException ex) {
            logger.log(Level.SEVERE, tl("connectionFailed"), ex);
        }
    }

    private boolean checkIfLocal(InetAddress address) {
        if (address.isAnyLocalAddress() || address.isLoopbackAddress()) {
            return true;
        }

        // Double checks if address is defined on any interface
        try {
            return NetworkInterface.getByInetAddress(address) != null;
        } catch (SocketException e) {
            return false;
        }
    }
}