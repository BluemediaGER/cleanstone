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
    private int listenPort;

    /** List of available backend servers and what address should be mapped to them */
    @JsonProperty("backendServerMappings")
    private ArrayList<BackendServerMapping> backendServerMappings;

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
}
