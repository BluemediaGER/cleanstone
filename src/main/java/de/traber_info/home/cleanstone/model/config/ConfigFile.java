package de.traber_info.home.cleanstone.model.config;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.ArrayList;

/**
 * Model for the main configuration file of cleanstone.
 *
 * @author Oliver Traber
 */
public class ConfigFile {

    /** Port on which the proxy should be listening */
    @JsonProperty("listenPort")
    private int listenPort = 25565;

    /** List of available backend servers and what address should be mapped to them */
    @JsonProperty("backendServerMappings")
    private ArrayList<BackendServerMapping> backendServerMappings;

    /** Config object for setting PROXY protocol settings */
    @JsonProperty("proxyProtocol")
    private ProxyProtocolSettings proxyProtocolSettings = new ProxyProtocolSettings();

    /** Private constructor for instantiation by Jackson */
    private ConfigFile() {}

    /**
     * Get the port on which the proxy should be listening.
     * @return Port on which the proxy should be listening.
     */
    public int getListenPort() {
        return listenPort;
    }

    /**
     * Get the list of available backend servers and what address should be mapped to them.
     * @return List of available backend servers and what address should be mapped to them.
     */
    public ArrayList<BackendServerMapping> getBackendServerMappings() {
        return backendServerMappings;
    }

    /**
     * Get the PROXY protocol config object.
     * @return PROXY protocol config object.
     */
    public ProxyProtocolSettings getProxyProtocolSettings() {
        return proxyProtocolSettings;
    }

    /**
     * Class to hold information about PROXY protocol support options.
     */
    public class ProxyProtocolSettings {

        /** Enable PROXY protocol support */
        @JsonProperty("enable")
        private boolean enabled = false;

        /** Enable PROXY protocol pass through */
        @JsonProperty("passThrough")
        private boolean passThrough = false;

        /**
         * Check if PROXY protocol support is enabled.
         * @return true if PROXY protocol support is enabled, otherwise false.
         */
        public boolean isEnabled() {
            return enabled;
        }

        /**
         * Check if PROXY protocol pass through is enabled.
         * @return true if PROXY protocol pass through is enabled, otherwise false.
         */
        public boolean passThroughEnabled() {
            return passThrough;
        }
    }
}
