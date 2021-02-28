package de.traber_info.home.cleanstone;

import de.traber_info.home.cleanstone.model.config.BackendServerMapping;
import de.traber_info.home.cleanstone.proxy.CleanstoneProxy;
import de.traber_info.home.cleanstone.util.ConfigUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

/**
 * Main class for cleanstone. Performs the first initialisation of all components.
 *
 * @author Oliver Traber
 */
public class CleanStone {

    /** SLF4J logger for usage in this class */
    private static final Logger LOG = LoggerFactory.getLogger(CleanStone.class.getName());

    /** Map that assigns domain names to their corresponding backend servers */
    private static Map<String, BackendServerMapping> backendServerMappings = new HashMap<>();

    /**
     * Main function of cleanstone. Initializes everything that is needed to run cleanstone.
     * @param args Arguments passed in by the commandline.
     */
    public static void main(String[] args) {
        for (BackendServerMapping backendServerMapping : ConfigUtil.getConfig().getBackendServerMappings()) {
            backendServerMappings.put(backendServerMapping.getMappingDomain(), backendServerMapping);
        }

        // Warn if PROXY protocol pass-through is enabled
        if (ConfigUtil.getConfig().getProxyProtocolSettings().passThroughEnabled()) {
            LOG.warn("PROXY protocol v2 pass-through is enabled. " +
                    "Make sure that cleanstone is properly firewalled and only receives data from it's upstream. " +
                    "Otherwise, players might be able to spoof their IP address."
            );
        }

        LOG.info("Starting proxy server on port {}", ConfigUtil.getConfig().getListenPort());
        CleanstoneProxy cleanstoneProxy = new CleanstoneProxy(ConfigUtil.getConfig().getListenPort());
        cleanstoneProxy.listen();
    }

    /**
     * Get the map that assigns domain names to their corresponding backend servers.
     * @return Map that assigns domain names to their corresponding backend servers.
     */
    public static Map<String, BackendServerMapping> getBackendServerMappings() {
        return backendServerMappings;
    }

}
