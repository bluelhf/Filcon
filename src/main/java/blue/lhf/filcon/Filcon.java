package blue.lhf.filcon;

import org.apache.logging.log4j.core.Logger;
import org.bukkit.plugin.java.JavaPlugin;

import java.nio.file.Path;
import java.util.*;
import java.util.logging.Level;
import java.util.regex.*;

import static blue.lhf.filcon.PollingFileWatcher.check;
import static java.time.temporal.ChronoUnit.SECONDS;
import static org.apache.logging.log4j.LogManager.getRootLogger;

public class Filcon extends JavaPlugin {

    private RegexBlacklistFilter filter;
    private PollingFileWatcher watcher;

    @Override
    public void onLoad() {
        final Logger rootLogger = (Logger) getRootLogger();
        this.filter = new RegexBlacklistFilter();
        rootLogger.addFilter(this.filter);

        watcher = check(getConfigPath()).every(2, SECONDS).ifChanged(this::applyConfig);
    }

    @Override
    public void onDisable() {
        this.filter.stop();
        this.watcher.close();
    }

    @Override
    public void onEnable() {
        this.saveDefaultConfig();
        this.filter.start();
        this.watcher.start();

    }

    private Path getConfigPath() {
        return getDataFolder().toPath().resolve("config.yml");
    }

    private void applyConfig() {
        reloadConfig();

        final List<Pattern> patterns = new ArrayList<>();
        for (final String pattern : getConfig().getStringList("patterns")) {
            try {
                patterns.add(Pattern.compile(pattern));
            } catch (PatternSyntaxException e) {
                getLogger().log(Level.WARNING, () -> "The pattern syntax for '" + pattern + "' is invalid. Skipping.");
                e.getMessage().indent(4).lines().forEachOrdered(getLogger()::warning);
            }
        }

        getLogger().info(() -> switch (patterns.size()) {
            case 0 -> "Not blocking any messages. Try adding some patterns!";
            case 1 -> "Now blocking messages containing the pattern '" + patterns.get(0) + "'.";
            default -> "Now blocking messages containing any of the " + patterns.size() + " patterns.";
        });

        this.filter.getPatterns().clear();
        this.filter.getPatterns().addAll(patterns);
    }
}
