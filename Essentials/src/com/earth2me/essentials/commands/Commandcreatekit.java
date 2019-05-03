package com.earth2me.essentials.commands;

import com.google.common.base.Charsets;
import com.google.common.io.CharStreams;
import com.google.gson.Gson;
import com.google.gson.JsonObject;

import com.earth2me.essentials.CommandSource;
import com.earth2me.essentials.User;
import com.earth2me.essentials.utils.DateUtil;

import org.bukkit.Material;
import org.bukkit.Server;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.MemoryConfiguration;
import org.bukkit.configuration.file.YamlConstructor;
import org.bukkit.configuration.file.YamlRepresenter;
import org.bukkit.inventory.ItemStack;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.DumperOptions.FlowStyle;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.representer.Representer;

import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Commandcreatekit extends EssentialsCommand {

    private static final String PASTE_URL = "https://hastebin.com/";
    private static final String PASTE_UPLOAD_URL = PASTE_URL + "documents";
    private static final Gson GSON = new Gson();

    private final ExecutorService executorService = Executors.newSingleThreadExecutor();
    private final DumperOptions yamlOptions = new DumperOptions();
    private final Representer yamlRepresenter = new YamlRepresenter();
    private final YamlConstructor yamlConstructor = new YamlConstructor();

    public Commandcreatekit() {
        super("createkit");
        yamlOptions.setDefaultFlowStyle(FlowStyle.BLOCK);
        yamlRepresenter.setDefaultFlowStyle(FlowStyle.BLOCK);
    }

    // /createkit <name> <delay>
    @Override
    public void run(final Server server, final User user, final String commandLabel, final String[] args) throws Exception {
        if (args.length != 2) {
            throw new NotEnoughArgumentsException();
        }

        // Command handler will auto fail if this fails.
        long delay = Long.valueOf(args[1]);
        String kitname = args[0];
        ItemStack[] items = user.getBase().getInventory().getContents();
        List<String> list = new ArrayList<>();
        for (ItemStack is : items) {
            if (is != null && is.getType() != null && is.getType() != Material.AIR) {
                String serialized = ess.getItemDb().serialize(is);
                list.add(serialized);
            }
        }
        // Some users might want to directly write to config knowing the consequences. *shrug*
        if (!ess.getSettings().isPastebinCreateKit()) {
            ess.getKits().addKit(kitname, list, delay);
            user.sendTl("createdKit", kitname, list.size(), delay);
        } else {
            ConfigurationSection config = new MemoryConfiguration();
            config.set("kits." + kitname + ".delay", delay);
            config.set("kits." + kitname + ".items", list);

            final Yaml yaml = new Yaml(yamlConstructor, yamlRepresenter, yamlOptions);
            String fileContents = "# Copy the kit code below into the kits section in your config.yml file\n";
            fileContents += yaml.dump(config.getValues(false));

            uploadPaste(user.getSource(), kitname, delay, fileContents);
        }
    }

    private void uploadPaste(final CommandSource sender, final String kitName, final long delay, final String contents) {
        executorService.submit(new Runnable() {
            @Override
            public void run() {
                try {
                    HttpURLConnection connection = (HttpURLConnection) new URL(PASTE_UPLOAD_URL).openConnection();
                    connection.setRequestMethod("POST");
                    connection.setDoInput(true);
                    connection.setDoOutput(true);
                    connection.setRequestProperty("User-Agent", "EssentialsX plugin");
                    try (OutputStream os = connection.getOutputStream()) {
                        os.write(contents.getBytes(Charsets.UTF_8));
                    }
                    // Error
                    if (connection.getResponseCode() >= 400) {
                        sender.sendTl("createKitFailed", kitName);
                        String message = CharStreams.toString(new InputStreamReader(connection.getErrorStream(), Charsets.UTF_8));
                        ess.getLogger().severe("Error creating kit: " + message);
                        return;
                    }

                    // Read URL
                    JsonObject object = GSON.fromJson(new InputStreamReader(connection.getInputStream(), Charsets.UTF_8), JsonObject.class);
                    String pasteUrl = PASTE_URL + object.get("key").getAsString();
                    connection.disconnect();

                    String separator = sender.tl("createKitSeparator");
                    String delayFormat = "0";
                    if (delay > 0) {
                        delayFormat = DateUtil.formatDateDiff(sender, System.currentTimeMillis() + (delay * 1000));
                    }
                    sender.sendMessage(separator);
                    sender.sendTl("createKitSuccess", kitName, delayFormat, pasteUrl);
                    sender.sendMessage(separator);
                    if (ess.getSettings().isDebug()) {
                        ess.getLogger().info(sender.getSender().getName() + " created a kit: " + pasteUrl);
                    }
                } catch (Exception e) {
                    sender.sendTl("createKitFailed", kitName);
                    e.printStackTrace();
                }
            }
        });
    }
}
