package de.traber_info.home.cleanstone.model.config;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Model for an backend server definition.
 *
 * @author Oliver Traber
 */
public class BackendServerMapping {

    /** Domain that should be forwarded to the given backend server */
    @JsonProperty("mappingDomain")
    private String mappingDomain;

    /** Address of the backend server the given domain should be mapped to */
    @JsonProperty("backendServerAddress")
    private String backendServerAddress;

    /** Port of the backend server the given domain should be mapped to */
    @JsonProperty("backendServerPort")
    private int backendServerPort;

    /** Private constructor for instantiation by Jackson */
    private BackendServerMapping() {}

    /**
     * Get the domain that should be forwarded to the given backend server.
     * @return Domain that should be forwarded to the given backend server.
     */
    public String getMappingDomain() {
        return mappingDomain;
    }

    /**
     * Get the address of the backend server the given domain should be mapped to.
     * @return Address of the backend server the given domain should be mapped to.
     */
    public String getBackendServerAddress() {
        return backendServerAddress;
    }

    /**
     * Get the port of the backend server the given domain should be mapped to.
     * @return Port of the backend server the given domain should be mapped to.
     */
    public int getBackendServerPort() {
        return backendServerPort;
    }

}
