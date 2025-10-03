package dev.ked.quetzalmap.integration;

public interface Integration {

    /**
     * Initialize the integration
     * @return true if successful, false otherwise
     */
    boolean initialize();

    /**
     * Shutdown the integration and clean up resources
     */
    void shutdown();

    /**
     * Get the name of the integration
     */
    String getName();

    /**
     * Update markers from this integration
     */
    void updateMarkers();
}
