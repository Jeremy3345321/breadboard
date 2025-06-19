package com.example.breadboard;

import android.widget.ImageButton;
import android.widget.Toast;

import com.example.breadboard.model.Attribute;
import com.example.breadboard.model.Coordinate;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

/**
 * ConnectionManager acts as a bridge between InputManager, OutputManager, and WireManager
 * to coordinate signal propagation and manage connections between inputs, outputs, and wires.
 */
public class ConnectionManager {
    private MainActivity mainActivity;
    private InputManager inputManager;
    private OutputManager outputManager;
    private WireManager wireManager;
    private ICPinManager icPinManager;

    private ImageButton[][][] pins;
    private Attribute[][][] pinAttributes;

    // Connection tracking
    private Map<Coordinate, Set<Coordinate>> connectionMap; // Maps each pin to its connected pins
    private Map<Coordinate, Integer> propagatedValues; // Tracks propagated signal values

    // Circuit context
    private String currentUsername;
    private String currentCircuitName;

    public ConnectionManager(MainActivity mainActivity,
                             InputManager inputManager,
                             OutputManager outputManager,
                             WireManager wireManager,
                             ICPinManager icPinManager,
                             ImageButton[][][] pins,
                             Attribute[][][] pinAttributes,
                             String username,
                             String circuitName) {
        this.mainActivity = mainActivity;
        this.inputManager = inputManager;
        this.outputManager = outputManager;
        this.wireManager = wireManager;
        this.icPinManager = icPinManager;
        this.pins = pins;
        this.pinAttributes = pinAttributes;
        this.currentUsername = username;
        this.currentCircuitName = circuitName;

        this.connectionMap = new HashMap<>();
        this.propagatedValues = new HashMap<>();

        // Set this ConnectionManager in the WireManager
        if (wireManager != null) {
            wireManager.setConnectionManager(this);
        }
    }

    /**
     * Initialize or rebuild all connections in the circuit
     */
    public void buildConnectionMap() {
        System.out.println("=== BUILDING CONNECTION MAP START ===");
        connectionMap.clear();
        propagatedValues.clear();

        // Build connections from wire data
        if (wireManager != null) {
            List<WireManager.Wire> allWires = wireManager.getAllWires();

            for (WireManager.Wire wire : allWires) {
                addConnection(wire.start, wire.end);
                System.out.println("Added wire connection: " + wire.start + " <-> " + wire.end);
            }
        }

        // Add column-based connections (breadboard internal connections)
        addBreadboardInternalConnections();

        System.out.println("Connection map built with " + connectionMap.size() + " nodes");
        System.out.println("=== BUILDING CONNECTION MAP END ===");
    }

    /**
     * Add breadboard internal connections (same column connections)
     */
    private void addBreadboardInternalConnections() {
        // Check if pinAttributes is properly initialized
        if (pinAttributes == null) {
            System.out.println("PinAttributes not initialized yet, skipping breadboard connections");
            return;
        }

        // For each column, connect all pins in the same section
        for (int s = 0; s < pinAttributes.length; s++) {
            if (pinAttributes[s] == null) continue;

            for (int c = 0; c < 64 && c < pinAttributes[s][0].length; c++) {
                List<Coordinate> columnPins = new ArrayList<>();

                // Collect all pins in this column
                for (int r = 0; r < pinAttributes[s].length; r++) {
                    if (pinAttributes[s][r] == null || c >= pinAttributes[s][r].length) continue;

                    Coordinate coord = new Coordinate(s, r, c);
                    Attribute attr = pinAttributes[s][r][c];

                    // Check if attribute is not null before accessing its fields
                    if (attr != null && (attr.value != -1 || attr.link != -1)) {
                        columnPins.add(coord);
                    }
                }

                // Connect all pins in the same column to each other
                for (int i = 0; i < columnPins.size(); i++) {
                    for (int j = i + 1; j < columnPins.size(); j++) {
                        addConnection(columnPins.get(i), columnPins.get(j));
                    }
                }
            }
        }
    }

    /**
     * Add a bidirectional connection between two coordinates
     */
    public void addConnection(Coordinate coord1, Coordinate coord2) {
        connectionMap.computeIfAbsent(coord1, k -> new HashSet<>()).add(coord2);
        connectionMap.computeIfAbsent(coord2, k -> new HashSet<>()).add(coord1);
    }

    /**
     * Remove a connection between two coordinates
     */
    public void removeConnection(Coordinate coord1, Coordinate coord2) {
        Set<Coordinate> connections1 = connectionMap.get(coord1);
        if (connections1 != null) {
            connections1.remove(coord2);
            if (connections1.isEmpty()) {
                connectionMap.remove(coord1);
            }
        }

        Set<Coordinate> connections2 = connectionMap.get(coord2);
        if (connections2 != null) {
            connections2.remove(coord1);
            if (connections2.isEmpty()) {
                connectionMap.remove(coord2);
            }
        }

        // Remove propagated values for disconnected pins
        propagatedValues.remove(coord1);
        propagatedValues.remove(coord2);
    }

    /**
     * Called by WireManager when a wire is added
     */
    public void onWireAdded(Coordinate coord1, Coordinate coord2) {
        System.out.println("ConnectionManager: Wire added " + coord1 + " <-> " + coord2);
        addConnection(coord1, coord2);

        // Propagate signals immediately after adding connection
        propagateSignalsFromPin(coord1);
        propagateSignalsFromPin(coord2);
    }

    /**
     * Called by WireManager when a wire is removed
     */
    public void onWireRemoved(Coordinate coord1, Coordinate coord2) {
        System.out.println("ConnectionManager: Wire removed " + coord1 + " <-> " + coord2);
        removeConnection(coord1, coord2);

        // Update outputs after removing connection
        updateConnectedOutputs();
    }

    /**
     * Propagate signal from a specific pin to all connected pins
     */
    public void propagateSignalsFromPin(Coordinate sourcePin) {
        System.out.println("=== PROPAGATING SIGNALS FROM " + sourcePin + " ===");

        // Validate coordinates before accessing pinAttributes
        if (!isValidCoordinate(sourcePin)) {
            System.out.println("Invalid coordinate for signal propagation: " + sourcePin);
            return;
        }

        Attribute sourceAttr = pinAttributes[sourcePin.s][sourcePin.r][sourcePin.c];
        if (sourceAttr == null) {
            System.out.println("Source attribute is null for: " + sourcePin);
            return;
        }

        int signalValue = getSignalValue(sourceAttr);

        if (signalValue == -1) {
            System.out.println("No signal to propagate from " + sourcePin);
            return;
        }

        Set<Coordinate> visited = new HashSet<>();
        Queue<Coordinate> queue = new LinkedList<>();
        queue.offer(sourcePin);
        visited.add(sourcePin);

        while (!queue.isEmpty()) {
            Coordinate current = queue.poll();
            Set<Coordinate> connections = connectionMap.get(current);

            if (connections != null) {
                for (Coordinate connected : connections) {
                    if (!visited.contains(connected)) {
                        visited.add(connected);
                        queue.offer(connected);

                        // Set the signal value
                        propagatedValues.put(connected, signalValue);
                        System.out.println("Propagated signal " + signalValue + " to " + connected);

                        // Update visual representation if it's an output
                        if (outputManager != null && outputManager.isOutput(connected)) {
                            outputManager.updateOutputVisual(connected);
                        }
                    }
                }
            }
        }

        System.out.println("=== PROPAGATION COMPLETE ===");
    }

    /**
     * Check if coordinates are valid for the pinAttributes array
     */
    private boolean isValidCoordinate(Coordinate coord) {
        return pinAttributes != null &&
                coord.s >= 0 && coord.s < pinAttributes.length &&
                pinAttributes[coord.s] != null &&
                coord.r >= 0 && coord.r < pinAttributes[coord.s].length &&
                pinAttributes[coord.s][coord.r] != null &&
                coord.c >= 0 && coord.c < pinAttributes[coord.s][coord.r].length;
    }

    /**
     * Get the signal value from a pin attribute
     */
    private int getSignalValue(Attribute attr) {
        if (attr == null) return -1;
        if (attr.value == 1) return 1; // HIGH
        if (attr.value == 0) return 0; // LOW
        if (attr.value == -2) return 0; // GND
        if (attr.value == 2) return 1; // VCC
        return -1; // NO SIGNAL
    }

    /**
     * Get the effective signal value for a coordinate (including propagated values)
     */
    public int getEffectiveSignalValue(Coordinate coord) {
        if (!isValidCoordinate(coord)) {
            return -1;
        }

        Attribute attr = pinAttributes[coord.s][coord.r][coord.c];

        // Check direct value first
        int directValue = getSignalValue(attr);
        if (directValue != -1) {
            return directValue;
        }

        // Check propagated value
        Integer propagatedValue = propagatedValues.get(coord);
        if (propagatedValue != null) {
            return propagatedValue;
        }

        return -1; // No signal
    }

    /**
     * Check if two coordinates are connected (directly or indirectly)
     */
    public boolean areConnected(Coordinate coord1, Coordinate coord2) {
        if (coord1.equals(coord2)) return true;

        Set<Coordinate> visited = new HashSet<>();
        Queue<Coordinate> queue = new LinkedList<>();
        queue.offer(coord1);
        visited.add(coord1);

        while (!queue.isEmpty()) {
            Coordinate current = queue.poll();

            if (current.equals(coord2)) {
                return true;
            }

            Set<Coordinate> connections = connectionMap.get(current);
            if (connections != null) {
                for (Coordinate connected : connections) {
                    if (!visited.contains(connected)) {
                        visited.add(connected);
                        queue.offer(connected);
                    }
                }
            }
        }

        return false;
    }

    /**
     * Get all coordinates connected to a given coordinate
     */
    public Set<Coordinate> getAllConnectedPins(Coordinate coord) {
        Set<Coordinate> allConnected = new HashSet<>();
        Set<Coordinate> visited = new HashSet<>();
        Queue<Coordinate> queue = new LinkedList<>();

        queue.offer(coord);
        visited.add(coord);

        while (!queue.isEmpty()) {
            Coordinate current = queue.poll();
            allConnected.add(current);

            Set<Coordinate> connections = connectionMap.get(current);
            if (connections != null) {
                for (Coordinate connected : connections) {
                    if (!visited.contains(connected)) {
                        visited.add(connected);
                        queue.offer(connected);
                    }
                }
            }
        }

        allConnected.remove(coord); // Remove the original coordinate
        return allConnected;
    }

    /**
     * Update all connected outputs based on current connections
     */
    public void updateConnectedOutputs() {
        if (outputManager == null) return;

        System.out.println("=== UPDATING ALL CONNECTED OUTPUTS ===");

        // Clear all propagated values first
        propagatedValues.clear();

        // Find all input sources and propagate from them
        if (pinAttributes != null) {
            for (int s = 0; s < pinAttributes.length; s++) {
                if (pinAttributes[s] == null) continue;
                for (int r = 0; r < pinAttributes[s].length; r++) {
                    if (pinAttributes[s][r] == null) continue;
                    for (int c = 0; c < pinAttributes[s][r].length; c++) {
                        Coordinate coord = new Coordinate(s, r, c);
                        Attribute attr = pinAttributes[s][r][c];

                        // Check if this is a signal source
                        if (attr != null && (attr.value == 1 || attr.value == 0 || attr.value == -2 || attr.value == 2)) {
                            propagateSignalsFromPin(coord);
                        }
                    }
                }
            }
        }

        // Update all output visuals
        outputManager.updateAllOutputVisuals();

        System.out.println("=== OUTPUT UPDATE COMPLETE ===");
    }

    /**
     * Handle input state change - propagate the new signal
     */
    public void onInputStateChanged(Coordinate inputCoord) {
        System.out.println("Input state changed at: " + inputCoord);
        propagateSignalsFromPin(inputCoord);
    }

    /**
     * Get debug information about connections
     */
    public String getConnectionDebugInfo() {
        StringBuilder debug = new StringBuilder();
        debug.append("=== CONNECTION DEBUG INFO ===\n");
        debug.append("Total connection nodes: ").append(connectionMap.size()).append("\n");
        debug.append("Propagated values: ").append(propagatedValues.size()).append("\n\n");

        for (Map.Entry<Coordinate, Set<Coordinate>> entry : connectionMap.entrySet()) {
            Coordinate coord = entry.getKey();
            Set<Coordinate> connections = entry.getValue();

            debug.append(coord).append(" -> ");
            if (connections.isEmpty()) {
                debug.append("(no connections)");
            } else {
                boolean first = true;
                for (Coordinate connected : connections) {
                    if (!first) debug.append(", ");
                    debug.append(connected);
                    first = false;
                }
            }
            debug.append("\n");
        }

        debug.append("\n=== PROPAGATED VALUES ===\n");
        for (Map.Entry<Coordinate, Integer> entry : propagatedValues.entrySet()) {
            debug.append(entry.getKey()).append(" = ").append(entry.getValue()).append("\n");
        }

        return debug.toString();
    }

    /**
     * Clear all connections and propagated values
     */
    public void clearAllConnections() {
        System.out.println("Clearing all connections...");
        connectionMap.clear();
        propagatedValues.clear();

        // Update outputs to reflect cleared state
        if (outputManager != null) {
            outputManager.updateAllOutputVisuals();
        }
    }

    /**
     * Validate the integrity of the connection map
     */
    public boolean validateConnections() {
        for (Map.Entry<Coordinate, Set<Coordinate>> entry : connectionMap.entrySet()) {
            Coordinate coord1 = entry.getKey();
            Set<Coordinate> connections = entry.getValue();

            for (Coordinate coord2 : connections) {
                // Check if the reverse connection exists
                Set<Coordinate> reverseConnections = connectionMap.get(coord2);
                if (reverseConnections == null || !reverseConnections.contains(coord1)) {
                    System.err.println("Connection integrity error: " + coord1 + " -> " + coord2 + " but not reverse");
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * Get connection statistics
     */
    public Map<String, Integer> getConnectionStats() {
        Map<String, Integer> stats = new HashMap<>();
        stats.put("total_nodes", connectionMap.size());
        stats.put("propagated_values", propagatedValues.size());

        int totalConnections = 0;
        for (Set<Coordinate> connections : connectionMap.values()) {
            totalConnections += connections.size();
        }
        stats.put("total_connections", totalConnections / 2); // Divide by 2 because connections are bidirectional

        return stats;
    }

    // Getters and setters
    public Map<Coordinate, Set<Coordinate>> getConnectionMap() {
        return new HashMap<>(connectionMap);
    }

    public Map<Coordinate, Integer> getPropagatedValues() {
        return new HashMap<>(propagatedValues);
    }

    public void setCurrentCircuit(String username, String circuitName) {
        this.currentUsername = username;
        this.currentCircuitName = circuitName;
    }

    public String getCurrentUsername() {
        return currentUsername;
    }

    public String getCurrentCircuitName() {
        return currentCircuitName;
    }

    public void onInputValueChanged(Coordinate inputCoord) {
        System.out.println("=== INPUT VALUE CHANGED: " + inputCoord + " ===");

        // Validate coordinate before accessing
        if (!isValidCoordinate(inputCoord)) {
            System.out.println("Invalid coordinate for input value change: " + inputCoord);
            return;
        }

        // Get the new value from the pin attributes
        Attribute inputAttr = pinAttributes[inputCoord.s][inputCoord.r][inputCoord.c];
        if (inputAttr == null) {
            System.out.println("Input attribute is null for: " + inputCoord);
            return;
        }

        int newValue = getSignalValue(inputAttr);

        System.out.println("New input value: " + newValue + " at coordinate: " + inputCoord);

        // Clear any existing propagated values for this input's network
        // to ensure clean propagation with the new value
        clearPropagatedValuesForNetwork(inputCoord);

        // Propagate the new signal from this input to all connected pins
        propagateSignalsFromPin(inputCoord);

        // Update all connected outputs to reflect the change
        updateConnectedOutputs();

        System.out.println("=== INPUT VALUE CHANGE PROPAGATION COMPLETE ===");
    }

    /**
     * Clear propagated values for all pins in the same network as the given coordinate
     * This ensures clean signal propagation when an input value changes
     */
    private void clearPropagatedValuesForNetwork(Coordinate sourceCoord) {
        Set<Coordinate> networkPins = getAllConnectedPins(sourceCoord);
        networkPins.add(sourceCoord); // Include the source coordinate itself

        for (Coordinate coord : networkPins) {
            propagatedValues.remove(coord);
        }

        System.out.println("Cleared propagated values for " + networkPins.size() + " pins in network");
    }
}