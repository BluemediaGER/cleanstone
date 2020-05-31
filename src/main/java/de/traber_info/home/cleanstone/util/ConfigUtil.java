package de.traber_info.home.cleanstone.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.traber_info.home.cleanstone.CleanStone;
import de.traber_info.home.cleanstone.model.config.ConfigFile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.URISyntaxException;

/**
 * Util to read and serialize the json configuration file.
 *
 * @author Oliver Traber
 */
public class ConfigUtil {

    /** SLF4J logger for usage in this class */
    private static final Logger LOG = LoggerFactory.getLogger(ConfigUtil.class.getName());

    /** Global instance of the loaded and deserialized config */
    public static ConfigFile config;

    /**
     * Get the current loaded config or load the config if it isn't loaded already.
     * @return The global instance of the current config
     */
    public static ConfigFile getConfig() {
        // Return config if it was already loaded, or try to load the config file and deserialize it.
        if (config != null) {
            return config;
        } else {
            // Check if configuration path is set in environmental variable. If not, fall back to default location.
            if (System.getenv("CLEANSTONE_CONFIG") != null) {
                readConfig(System.getenv("CLEANSTONE_CONFIG"));
                return config;
            } else {
                LOG.info("Environmental variable CLEANSTONE_CONFIG is not present. Falling back to default config location.");
                try {
                    readConfig(getJarPath());
                    return config;
                } catch (URISyntaxException ex) {
                    LOG.error("Failed to load cleanstone's config file. Fallback to default config location failed.", ex);
                    System.exit(1);
                }
            }
        }
        return null;
    }

    /**
     * Try to deserialize the config file into the config model object
     * @param path Path where the file named config.json is saved
     */
    private static void readConfig(String path) {
        try {
            InputStream configInputStream = new FileInputStream(path + "/config.json");
            config = new ObjectMapper().readValue(configInputStream, ConfigFile.class);
        } catch (FileNotFoundException ex) {
            LOG.error("Config file config.json not found in path {}", path, ex);
            System.exit(2);
        } catch (IOException ex) {
            LOG.error("Failed to parse config file config.json in path {}. Please check your config syntax and try again.", path, ex);
            System.exit(3);
        }
    }

    /**
     * Get the path of the JAR file this class is packaged in.
     * @return Path of the current JAR file
     * @throws URISyntaxException If the path could not be parsed
     */
    public static String getJarPath() throws URISyntaxException {
        return new File(CleanStone.class.getProtectionDomain().getCodeSource().getLocation()
                .toURI()).getParentFile().getPath();
    }

}
