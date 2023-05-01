package io.snice.testing.runtime.config;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Configuration object for a single simulation.
 *
 * @author borjesson.jonas@gmail.com
 */
public class SimulationConfig {

    /**
     * The fully qualified class name of the simulation to run.
     */
    @JsonProperty
    private String simulation;

    public String getSimulation() {
        return simulation;
    }

    public void setSimulation(final String simulation) {
        this.simulation = simulation;
    }
}
