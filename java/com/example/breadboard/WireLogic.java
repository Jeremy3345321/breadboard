package com.example.breadboard;

import com.example.breadboard.model.Attribute;
import com.example.breadboard.model.Coordinate;
import com.example.breadboard.model.Pins;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * WireLogic handles the computational aspects of wire connections
 * including signal propagation, value determination, and communication
 * with external components like OutputManager and ConnectionManager
 */
public class WireLogic {
    private MainActivity mainActivity;
    private Attribute[][][] pinAttributes;
    private List<Pins> wires;
    private ICPinManager icPinManager;
    private OutputManager outputManager;
    private ConnectionManager connectionManager;

    // Wire connection state mapping
    private Map<Coordinate, List<Coordinate>> wireConnections = new HashMap<>();

    public WireLogic(MainActivity mainActivity, Attribute[][][] pinAttributes, 
                    List<Pins> wires, ICPinManager icPinManager) {
        this.mainActivity = mainActivity;
        this.pinAttributes = pinAttributes;
        this.wires = wires;
        this.icPinManager = icPinManager;
    }

    public void setOutputManager(OutputManager outputManager) {
        this.outputManager = outputManager;
    }

    public void setConnectionManager(ConnectionManager connectionManager) {
        this.connectionManager = connectionManager;
    }

    /**
     * Update wire values by propagating signals through wire connections
     * This method is called during circuit execution to ensure all connected pins
     * have consistent values based on their wire connections
     */
    public void updateWireValues() {
        System.out.println("🔌 Starting wire value propagation...");

        // Track which pins have been processed to avoid infinite loops
        Map<Coordinate, Boolean> processedPins = new HashMap<>();

        // Process each wire connection
        for (Pins wire : wires) {
            Coordinate src = wire.getSrc();
            Coordinate dst = wire.getDst();

            // Skip if both pins have already been processed in this update cycle
            if (processedPins.containsKey(src) && processedPins.containsKey(dst)) {
                continue;
            }

            // Get current values
            Attribute srcAttr = pinAttributes[src.s][src.r][src.c];
            Attribute dstAttr = pinAttributes[dst.s][dst.r][dst.c];

            int srcValue = srcAttr.value;
            int dstValue = dstAttr.value;

            // Determine which value should propagate
            int propagatedValue = determinePropagatedValue(srcValue, dstValue, src, dst);

            // Apply the propagated value to both pins
            if (propagatedValue != -1) {
                srcAttr.value = propagatedValue;
                dstAttr.value = propagatedValue;

                // Propagate to all connected pins in the same column/row group
                propagateToConnectedPins(src, propagatedValue);
                propagateToConnectedPins(dst, propagatedValue);

                System.out.println("Wire propagation: " + src + " <-> " + dst + " = " + propagatedValue);
            }

            // Mark pins as processed
            processedPins.put(src, true);
            processedPins.put(dst, true);
        }

        // After propagation, update any connected outputs
        updateConnectedOutputsAfterPropagation();

        System.out.println("🔌 Wire value propagation completed");
    }

    /**
     * Determine which value should propagate through a wire connection
     */
    private int determinePropagatedValue(int value1, int value2, Coordinate coord1, Coordinate coord2) {
        // Check for power supply pins (VCC=1, GND=0)
        if (isPowerPin(coord1)) {
            return value1;
        }
        if (isPowerPin(coord2)) {
            return value2;
        }

        // Check for active input pins
        if (isActiveInput(coord1, value1)) {
            return value1;
        }
        if (isActiveInput(coord2, value2)) {
            return value2;
        }

        // Check for IC output pins
        if (isICOutputPin(coord1) && (value1 == 0 || value1 == 1)) {
            return value1;
        }
        if (isICOutputPin(coord2) && (value2 == 0 || value2 == 1)) {
            return value2;
        }

        // If both pins have valid logic values, there might be a conflict
        if ((value1 == 0 || value1 == 1) && (value2 == 0 || value2 == 1)) {
            if (value1 != value2) {
                System.out.println("⚠️ Wire conflict detected: " + coord1 + "(" + value1 + ") vs " + coord2 + "(" + value2 + ")");
                // In case of conflict, prefer the higher value (could be customized)
                return Math.max(value1, value2);
            }
            return value1; // Both are the same
        }

        // If one has a valid logic value and the other doesn't
        if (value1 == 0 || value1 == 1) {
            return value1;
        }
        if (value2 == 0 || value2 == 1) {
            return value2;
        }

        // If neither has a valid logic value, maintain disconnected state
        return -1;
    }

    /**
     * Check if a coordinate represents a power supply pin
     */
    private boolean isPowerPin(Coordinate coord) {
        Attribute attr = pinAttributes[coord.s][coord.r][coord.c];
        return attr.value == 1 || attr.value == -2; // VCC or GND
    }

    /**
     * Check if a coordinate is an active input pin
     */
    private boolean isActiveInput(Coordinate coord, int value) {
        // Check if it's in the inputs list and has an active value
        for (Coordinate input : mainActivity.inputs) {
            if (input.equals(coord)) {
                return value == 0 || value == 1;
            }
        }
        return false;
    }

    /**
     * Check if a coordinate is an IC output pin
     */
    private boolean isICOutputPin(Coordinate coord) {
        if (icPinManager != null) {
            return icPinManager.isICOutputPin(coord);
        }
        return false;
    }

    /**
     * Propagate value to all pins connected in the same breadboard group
     * (same column in breadboard typically connects pins together)
     */
    private void propagateToConnectedPins(Coordinate coord, int value) {
        // Get the link ID for this coordinate
        int linkId = pinAttributes[coord.s][coord.r][coord.c].link;

        if (linkId == -1) {
            return; // No connections
        }

        // Find all pins with the same link ID and update their values
        for (int s = 0; s < pinAttributes.length; s++) {
            for (int r = 0; r < pinAttributes[s].length; r++) {
                for (int c = 0; c < pinAttributes[s][r].length; c++) {
                    if (pinAttributes[s][r][c].link == linkId) {
                        Coordinate linkedCoord = new Coordinate(s, r, c);

                        // Don't override power pins or IC pins with fixed values
                        if (!isPowerPin(linkedCoord) && !shouldPreserveValue(linkedCoord)) {
                            pinAttributes[s][r][c].value = value;
                        }
                    }
                }
            }
        }
    }

    /**
     * Check if a pin's value should be preserved (not overwritten by wire propagation)
     */
    private boolean shouldPreserveValue(Coordinate coord) {
        // Preserve values for:
        // 1. Power supply pins (VCC, GND)
        if (isPowerPin(coord)) {
            return true;
        }

        // 2. IC output pins (they drive the signal)
        if (icPinManager != null && icPinManager.isICOutputPin(coord)) {
            return true;
        }

        // 3. Active input pins (they provide the input signal)
        for (Coordinate input : MainActivity.inputs) {
            if (input.equals(coord)) {
                return true;
            }
        }

        return false;
    }

    /**
     * Update connected outputs after wire value propagation
     */
    private void updateConnectedOutputsAfterPropagation() {
        if (outputManager == null) {
            return;
        }

        // Check all wires and update any outputs they might be connected to
        for (Pins wire : wires) {
            updateOutputsConnectedToWire(wire.getSrc());
            updateOutputsConnectedToWire(wire.getDst());
        }
    }

    /**
     * Update outputs that might be connected to a specific coordinate through wires
     */
    private void updateOutputsConnectedToWire(Coordinate coord) {
        if (outputManager == null) {
            return;
        }

        // Get the link ID for this coordinate
        int linkId = pinAttributes[coord.s][coord.r][coord.c].link;

        if (linkId == -1) {
            return;
        }

        // Find all pins with the same link ID and check if any are outputs
        for (int s = 0; s < pinAttributes.length; s++) {
            for (int r = 0; r < pinAttributes[s].length; r++) {
                for (int c = 0; c < pinAttributes[s][r].length; c++) {
                    if (pinAttributes[s][r][c].link == linkId) {
                        Coordinate linkedCoord = new Coordinate(s, r, c);

                        // Check if this coordinate is an output
                        if (outputManager.isOutput(linkedCoord)) {
                            outputManager.updateOutputVisual(linkedCoord);
                            System.out.println("Updated output at " + linkedCoord + " via wire connection");
                        }
                    }
                }
            }
        }
    }

    /**
     * Update outputs in a specific column after new wire connection
     */
    public void updateOutputsInColumn(Coordinate coord) {
        if (outputManager == null) return;

        // Check all rows in the same column and section for outputs
        for (int r = 0; r < 5; r++) { // Assuming 5 rows
            Coordinate checkCoord = new Coordinate(coord.s, r, coord.c);
            if (outputManager.isOutput(checkCoord)) {
                outputManager.updateOutputVisual(checkCoord);
                System.out.println("Updated output at " + checkCoord + " due to wire connection");
            }
        }
    }

    /**
     * Create logical wire connection between two pins
     */
    public boolean createWireConnection(Coordinate pin1, Coordinate pin2) {
        // Validate connection
        if (!canConnectPins(pin1, pin2)) {
            return false;
        }

        // Create wire object
        Pins wire = new Pins(pin1, pin2);
        wires.add(wire);

        // Update pin attributes to link them
        int linkId = generateLinkId();
        pinAttributes[pin1.s][pin1.r][pin1.c].link = linkId;
        pinAttributes[pin2.s][pin2.r][pin2.c].link = linkId;

        // Store bidirectional connection mapping
        addWireConnection(pin1, pin2);
        addWireConnection(pin2, pin1);

        // Update connected outputs
        updateOutputsInColumn(pin1);
        updateOutputsInColumn(pin2);

        // Notify ConnectionManager about new wire
        if (connectionManager != null) {
            connectionManager.onWireAdded(pin1, pin2);
        }

        System.out.println("Wire created: " + pin1 + " <-> " + pin2 + " (link=" + linkId + ")");
        return true;
    }

    /**
     * Remove logical wire connection
     */
    public boolean removeWireConnection(Coordinate pin1, Coordinate pin2) {
        // Find and remove the specific wire
        Pins wireToRemove = null;
        for (Pins wire : wires) {
            if ((wire.getSrc().equals(pin1) && wire.getDst().equals(pin2)) ||
                    (wire.getSrc().equals(pin2) && wire.getDst().equals(pin1))) {
                wireToRemove = wire;
                break;
            }
        }

        if (wireToRemove != null) {
            // Remove connections
            removeWireConnectionMapping(wireToRemove.getSrc(), wireToRemove.getDst());
            removeWireConnectionMapping(wireToRemove.getDst(), wireToRemove.getSrc());

            // Reset pin attributes
            pinAttributes[wireToRemove.getSrc().s][wireToRemove.getSrc().r][wireToRemove.getSrc().c].link = -1;
            pinAttributes[wireToRemove.getDst().s][wireToRemove.getDst().r][wireToRemove.getDst().c].link = -1;

            // Remove from wires list
            wires.remove(wireToRemove);

            // Notify ConnectionManager about wire removal
            if (connectionManager != null) {
                connectionManager.onWireRemoved(wireToRemove.getSrc(), wireToRemove.getDst());
            }

            System.out.println("Wire removed: " + wireToRemove.getSrc() + " <-> " + wireToRemove.getDst());
            return true;
        }

        return false;
    }

    /**
     * Remove all wire connections for a specific coordinate
     */
    public List<Pins> removeAllWireConnections(Coordinate coord) {
        List<Pins> removedWires = new ArrayList<>();
        List<Pins> wiresToRemove = new ArrayList<>();

        // Find all wires connected to this coordinate
        for (Pins wire : wires) {
            if (wire.getSrc().equals(coord) || wire.getDst().equals(coord)) {
                wiresToRemove.add(wire);
            }
        }

        // Remove found wires
        for (Pins wire : wiresToRemove) {
            removeWireConnectionMapping(wire.getSrc(), wire.getDst());
            removeWireConnectionMapping(wire.getDst(), wire.getSrc());

            // Reset pin attributes
            pinAttributes[wire.getSrc().s][wire.getSrc().r][wire.getSrc().c].link = -1;
            pinAttributes[wire.getDst().s][wire.getDst().r][wire.getDst().c].link = -1;

            // Notify ConnectionManager about wire removal
            if (connectionManager != null) {
                connectionManager.onWireRemoved(wire.getSrc(), wire.getDst());
            }

            removedWires.add(wire);
            System.out.println("Wire removed: " + wire.getSrc() + " <-> " + wire.getDst());
        }

        wires.removeAll(wiresToRemove);
        return removedWires;
    }

    /**
     * Clear all wire connections
     */
    public void clearAllWireConnections() {
        // Notify ConnectionManager about all wire removals
        if (connectionManager != null) {
            for (Pins wire : wires) {
                connectionManager.onWireRemoved(wire.getSrc(), wire.getDst());
            }
        }

        // Reset all wire-related pin attributes
        for (Pins wire : wires) {
            pinAttributes[wire.getSrc().s][wire.getSrc().r][wire.getSrc().c].link = -1;
            pinAttributes[wire.getDst().s][wire.getDst().r][wire.getDst().c].link = -1;
        }

        // Clear data structures
        wires.clear();
        wireConnections.clear();
    }

    /**
     * Check if two pins can be connected with a wire
     */
    public boolean canConnectPins(Coordinate pin1, Coordinate pin2) {
        // Cannot connect pins in the same hole/column if they're in the same section
        if (pin1.s == pin2.s && pin1.c == pin2.c) {
            return false;
        }

        // Check if pins are already connected
        if (areAlreadyConnected(pin1, pin2)) {
            return false;
        }

        // Check if either pin is already part of a different connection type
        Attribute attr1 = pinAttributes[pin1.s][pin1.r][pin1.c];
        Attribute attr2 = pinAttributes[pin2.s][pin2.r][pin2.c];

        // Allow connection if pins are free or only have wire connections
        return (attr1.link == -1 || isWireConnection(pin1)) &&
                (attr2.link == -1 || isWireConnection(pin2));
    }

    /**
     * Check if a pin can be used for wire connections (validation)
     */
    public boolean isValidWirePin(Coordinate coord) {
        Attribute attr = pinAttributes[coord.s][coord.r][coord.c];

        // Cannot connect to VCC, GND, or special pins
        if (attr.value == 1 || attr.value == -2 || attr.value == 2) {
            return false;
        }

        // Cannot connect to IC pins directly (they should use their own connection system)
        if (icPinManager != null && icPinManager.isICPin(coord)) {
            return false;
        }

        return true;
    }

    /**
     * Check if two pins are already connected
     */
    private boolean areAlreadyConnected(Coordinate pin1, Coordinate pin2) {
        for (Pins wire : wires) {
            if ((wire.getSrc().equals(pin1) && wire.getDst().equals(pin2)) ||
                    (wire.getSrc().equals(pin2) && wire.getDst().equals(pin1))) {
                return true;
            }
        }
        return false;
    }

    /**
     * Check if a pin is part of a wire connection
     */
    private boolean isWireConnection(Coordinate coord) {
        List<Coordinate> connections = wireConnections.get(coord);
        return connections != null && !connections.isEmpty();
    }

    /**
     * Generate a unique link ID for wire connections
     */
    private int generateLinkId() {
        int maxId = 0;
        for (int s = 0; s < pinAttributes.length; s++) {
            for (int r = 0; r < pinAttributes[s].length; r++) {
                for (int c = 0; c < pinAttributes[s][r].length; c++) {
                    if (pinAttributes[s][r][c].link > maxId) {
                        maxId = pinAttributes[s][r][c].link;
                    }
                }
            }
        }
        return maxId + 1;
    }

    /**
     * Add wire connection to mapping
     */
    private void addWireConnection(Coordinate from, Coordinate to) {
        wireConnections.computeIfAbsent(from, k -> new ArrayList<>()).add(to);
    }

    /**
     * Remove wire connection from mapping
     */
    private void removeWireConnectionMapping(Coordinate from, Coordinate to) {
        List<Coordinate> connections = wireConnections.get(from);
        if (connections != null) {
            connections.remove(to);
            if (connections.isEmpty()) {
                wireConnections.remove(from);
            }
        }
    }

    /**
     * Get the current value that should be present on a wire connection
     * This is useful for debugging and validation
     */
    public int getWireValue(Coordinate coord1, Coordinate coord2) {
        // Find the wire connection
        for (Pins wire : wires) {
            if ((wire.getSrc().equals(coord1) && wire.getDst().equals(coord2)) ||
                    (wire.getSrc().equals(coord2) && wire.getDst().equals(coord1))) {

                // Return the value from either end (should be the same after propagation)
                int value1 = pinAttributes[coord1.s][coord1.r][coord1.c].value;
                int value2 = pinAttributes[coord2.s][coord2.r][coord2.c].value;

                // Return the first valid value found
                if (value1 == 0 || value1 == 1) return value1;
                if (value2 == 0 || value2 == 1) return value2;

                return -1; // No valid signal
            }
        }

        return -1; // Wire not found
    }

    /**
     * Get all wires connected to a specific coordinate
     */
    public List<Pins> getWiresForPin(Coordinate coord) {
        List<Pins> connectedWires = new ArrayList<>();
        for (Pins wire : wires) {
            if (wire.getSrc().equals(coord) || wire.getDst().equals(coord)) {
                connectedWires.add(wire);
            }
        }
        return connectedWires;
    }

    /**
     * Get all wires in a format suitable for ConnectionManager
     */
    public List<WireManager.Wire> getAllWires() {
        List<WireManager.Wire> wireList = new ArrayList<>();
        for (Pins pin : wires) {
            wireList.add(new WireManager.Wire(pin.getSrc(), pin.getDst()));
        }
        return wireList;
    }

    /**
     * Debug method to print all wire values
     */
    public void debugWireValues() {
        System.out.println("=== WIRE VALUES DEBUG ===");
        for (int i = 0; i < wires.size(); i++) {
            Pins wire = wires.get(i);
            Coordinate src = wire.getSrc();
            Coordinate dst = wire.getDst();

            int srcValue = pinAttributes[src.s][src.r][src.c].value;
            int dstValue = pinAttributes[dst.s][dst.r][dst.c].value;

            System.out.println("Wire " + (i + 1) + ": " + src + "(" + srcValue + ") <-> " + dst + "(" + dstValue + ")");
        }
        System.out.println("========================");
    }

    /**
     * Get debug information about wires
     */
    public String getWireDebugInfo() {
        StringBuilder debug = new StringBuilder();
        debug.append("Wires: ").append(wires.size()).append("\n");

        for (int i = 0; i < wires.size(); i++) {
            Pins wire = wires.get(i);
            debug.append("Wire ").append(i + 1).append(": ")
                    .append(wire.getSrc()).append(" <-> ").append(wire.getDst()).append("\n");
        }

        return debug.toString();
    }
}