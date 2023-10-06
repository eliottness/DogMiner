package com.softalys;

import com.softalys.metrics.listeners.MetricListener;
import com.softalys.metrics.listeners.MetricListeners;
import com.timgroup.statsd.Event;
import com.timgroup.statsd.NonBlockingStatsDClient;
import com.timgroup.statsd.NonBlockingStatsDClientBuilder;
import com.timgroup.statsd.StatsDClient;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.logging.Level;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.event.HandlerList;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Main DogMiner class.
 */
@Getter @Setter(AccessLevel.PRIVATE) public class DogMinerPlugin extends JavaPlugin {
    @Getter @Setter(AccessLevel.PRIVATE) private static DogMinerPlugin instance;

    private String instanceName;

    private StatsDClient statsd;

    private SendMetricsTask task;

    private SendMetricsTask syncTask;

    private Properties buildInfo;

    private boolean debug;

    /**
     * Sets singleton instance.
     */
    public DogMinerPlugin() {
        DogMinerPlugin.setInstance(this);
    }

    /**
     * Register command and vote listener.
     */
    @Override public void onEnable() {
        this.setBuildInfo(new Properties());
        try {
            this.getBuildInfo().load(this.getClass().getClassLoader().getResourceAsStream("git.properties"));
        } catch (IOException ex) {
            this.getLogger().log(Level.WARNING, "Couldn't load build info.", ex);
        }

        this.setDebug(this.getConfig().getBoolean("debug", false));

        //this.getCommand("dogminer").setExecutor(new DogMinerCommand());
    }

    /**
     * Cancels metric collection task, unregisters listeners.
     */
    @Override public void onDisable() {
        this.getTask().cancel();

        this.getSyncTask().cancel();

        HandlerList.unregisterAll(this);
    }

    /**
     * Begins metric collection.
     */
    void initialize() {
        this.setInstanceName(this.findInstanceName());

        List<String> configTags = new ArrayList<>();
        if (this.getConfig().isList("tags")) {
            configTags = this.getConfig().getStringList("tags");
        }

        configTags.add("instance:" + this.getInstanceName());
        configTags.add("environment:" + this.getConfig().getString("environment", "production"));
        String[] tags = configTags.toArray(new String[0]);

        statsd = new NonBlockingStatsDClientBuilder().prefix(this.getConfig().getString("prefix", "minecraft"))
                                                     .hostname(this.getConfig().getString("host", "localhost"))
                                                     .port(this.getConfig().getInt("port", 8125))
                                                     .constantTags(tags)
                                                     .build();

        this.setTask(new SendMetricsTask(false));
        if (this.getConfig().getBoolean("async", true)) {
            this.getTask().runTaskTimerAsynchronously(this, this.getConfig().getInt("delay", 300), this.getConfig().getInt("interval", 100));
        } else {
            this.getTask().runTaskTimer(this, this.getConfig().getInt("delay", 300), this.getConfig().getInt("interval", 100));
        }

        this.setSyncTask(new SendMetricsTask(true));
        this.getSyncTask().runTaskTimer(this, this.getConfig().getInt("sync.delay", 350), this.getConfig().getInt("sync.interval", 600));

        for (MetricListeners type : MetricListeners.values()) {
            MetricListener listener;
            try {
                listener = type.getCls().newInstance();
            } catch (InstantiationException | IllegalAccessException ex) {
                this.getLogger().log(Level.SEVERE, "Couldn't initialize MetricListener:" + type.name(), ex);
                continue;
            }

            if (listener.shouldRegister()) {
                this.getServer().getPluginManager().registerEvents(listener, this);
            }
        }

        if (this.getConfig().getBoolean("events.startup", true)) {
            this.getStatsd()
                .recordEvent(Event.builder()
                                  .withTitle(this.getInstanceName() + "'s DogMiner plugin has initialized")
                                  .withText("See "
                                            + this.getInstanceName()
                                            + "'s [dashboard]"
                                            + "(https://app"
                                            + ".datadoghq"
                                            + ".com/dash/dash/"
                                            + this.getConfig().getInt("dashboard", 0)
                                            + "?live=true"
                                            + "&tile_size=m"
                                            + "&tpl_var_scope"
                                            + "=instance:"
                                            + this.getInstanceName()
                                            + ")")
                                  .build());
        }

        this.getLogger().log(Level.INFO, "DogMiner initialization completed.");
    }

    /**
     * Gets the name to identify this instance.
     *
     * @return The name of this instance.
     */
    private String findInstanceName() {
        if (!this.getConfig().getString("instance", "").isEmpty()) {
            return this.getConfig().getString("instance");
        }

        if (!Bukkit.getServer().getName().isEmpty()) {
            return Bukkit.getServer().getName();
        }

        return Bukkit.getIp() + ":" + Bukkit.getPort();
    }

    /**
     * Gets a language string from the config.
     *
     * @param path
     *         The path (under lang) to the string.
     * @param def
     *         The default string.
     *
     * @return The requested language string.
     */
    public String getLangString(String path, String def) {
        return ChatColor.translateAlternateColorCodes('&', this.getConfig().getString("lang." + path, def));
    }
}
