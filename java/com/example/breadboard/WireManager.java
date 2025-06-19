package com.example.breadboard;

import android.app.AlertDialog;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.GridLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.Toast;

import com.example.breadboard.model.Attribute;
import com.example.breadboard.model.Coordinate;
import com.example.breadboard.model.Pins;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

public class WireManager {
    private MainActivity mainActivity;
    private ImageButton[][][] pins;
    private Attribute[][][] pinAttributes;
    private List<Pins> wires;
    private ICPinManager icPinManager;

    // Wire connection state
    private Coordinate firstWirePin = null;
    private boolean isWireMode = false;
    private Map<Coordinate, List<Coordinate>> wireConnections = new HashMap<>();

    // Visual feedback for wire mode
    private static final int WIRE_COLOR_SELECTED = Color.parseColor("#FF9800"); // Orange
    private static final int WIRE_COLOR_CONNECTED = Color.parseColor("#4CAF50"); // Green

    // Visual wire drawing
    private ViewGroup breadboardContainer; // The main container holding the breadboard
    private List<ImageView> wireViews = new ArrayList<>(); // Store visual wire elements
    private Random colorGenerator = new Random();

    private OutputManager outputManager;
    private ConnectionManager connectionManager; // Add reference to ConnectionManager

    // Wire colors array for variety
    private static final int[] WIRE_COLORS = {
            Color.parseColor("#FF5722"), // Red
            Color.parseColor("#2196F3"), // Blue
            Color.parseColor("#4CAF50"), // Green
            Color.parseColor("#FF9800"), // Orange
            Color.parseColor("#9C27B0"), // Purple
            Color.parseColor("#607D8B"), // Blue Grey
            Color.parseColor("#795548"), // Brown
            Color.parseColor("#000000"), // Black
            Color.parseColor("#FFEB3B"), // Yellow
            Color.parseColor("#E91E63")  // Pink
    };

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


    // Inner class to represent wire data for ConnectionManager
    public static class Wire {
        public final Coordinate start;
        public final Coordinate end;

        public Wire(Coordinate start, Coordinate end) {
            this.start = start;
            this.end = end;
        }
    }

    public WireManager(MainActivity mainActivity, ImageButton[][][] pins,
                       Attribute[][][] pinAttributes, List<Pins> wires,
                       ICPinManager icPinManager, ViewGroup breadboardContainer) {
        this.mainActivity = mainActivity;
        this.pins = pins;
        this.pinAttributes = pinAttributes;
        this.wires = wires;
        this.icPinManager = icPinManager;
        this.breadboardContainer = breadboardContainer;
    }

    public void setOutputManager(OutputManager outputManager) {
        this.outputManager = outputManager;
    }

    public void setConnectionManager(ConnectionManager connectionManager) {
        this.connectionManager = connectionManager;
    }

    /**
     * Get all wires in a format suitable for ConnectionManager
     */
    public List<Wire> getAllWires() {
        List<Wire> wireList = new ArrayList<>();
        for (Pins pin : wires) {
            wireList.add(new Wire(pin.getSrc(), pin.getDst()));
        }
        return wireList;
    }

    /**
     * Toggle wire connection mode
     */
    public void toggleWireMode() {
        isWireMode = !isWireMode;

        if (isWireMode) {
            showToast("Wire mode ON - Select two pins to connect");
            firstWirePin = null;
        } else {
            showToast("Wire mode OFF");
            resetWireSelection();
        }
    }

    /**
     * Handle pin click in wire mode
     */
    public boolean handleWirePinClick(Coordinate coord) {
        if (!isWireMode) {
            return false; // Not handled, let other managers handle it
        }

        // Check if pin can be used for wire connections
        if (!isValidWirePin(coord)) {
            showToast("Cannot connect wire to this pin type");
            return true; // Handled, but invalid
        }

        if (firstWirePin == null) {
            // First pin selection
            firstWirePin = coord;
            highlightPin(coord, WIRE_COLOR_SELECTED);
            showToast("First pin selected. Select second pin to connect.");
        } else {
            // Second pin selection - create wire
            if (coord.equals(firstWirePin)) {
                // Same pin clicked - deselect
                resetWireSelection();
                showToast("Wire selection cancelled");
            } else {
                // Create wire connection
                createWire(firstWirePin, coord);
                resetWireSelection();
            }
        }

        return true; // Handled
    }

    /**
     * Check if a pin can be used for wire connections
     */
    private boolean isValidWirePin(Coordinate coord) {
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
     * Create a wire connection between two pins with visual representation
     */
    private void createWire(Coordinate pin1, Coordinate pin2) {
        // Validate connection
        if (!canConnectPins(pin1, pin2)) {
            showToast("Cannot create wire connection between these pins");
            return;
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

        // Create visual wire representation
        createVisualWire(pin1, pin2);

        // Visual feedback
        highlightPin(pin1, WIRE_COLOR_CONNECTED);
        highlightPin(pin2, WIRE_COLOR_CONNECTED);

        // Notify ConnectionManager about new wire
        if (connectionManager != null) {
            connectionManager.onWireAdded(pin1, pin2);
        }

        showToast("Wire connected!");
        System.out.println("Wire created: " + pin1 + " <-> " + pin2 + " (link=" + linkId + ")");
    }

    private void updateConnectedOutputs(Coordinate pin1, Coordinate pin2) {
        if (outputManager == null) return;

        // Check if either pin is in a column with an output
        updateOutputsInColumn(pin1);
        updateOutputsInColumn(pin2);
    }

    private void updateOutputsInColumn(Coordinate coord) {
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
     * Create visual wire representation between two pins
     */
    private void createVisualWire(Coordinate pin1, Coordinate pin2) {
        ImageButton button1 = pins[pin1.s][pin1.r][pin1.c];
        ImageButton button2 = pins[pin2.s][pin2.r][pin2.c];

        if (button1 == null || button2 == null) return;

        // Get pin positions
        int[] location1 = new int[2];
        int[] location2 = new int[2];
        button1.getLocationInWindow(location1);
        button2.getLocationInWindow(location2);

        // Get container position for relative positioning
        int[] containerLocation = new int[2];
        breadboardContainer.getLocationInWindow(containerLocation);

        // Calculate relative positions
        int x1 = location1[0] - containerLocation[0] + button1.getWidth() / 2;
        int y1 = location1[1] - containerLocation[1] + button1.getHeight() / 2;
        int x2 = location2[0] - containerLocation[0] + button2.getWidth() / 2;
        int y2 = location2[1] - containerLocation[1] + button2.getHeight() / 2;

        // Create wire path (simple straight line for now)
        createStraightWire(x1, y1, x2, y2, pin1, pin2);
    }

    /**
     * Create a straight wire line between two points
     */
    private void createStraightWire(int x1, int y1, int x2, int y2, Coordinate pin1, Coordinate pin2) {
        // Create ImageView for the wire
        ImageView wireView = new ImageView(mainActivity);

        // Calculate wire dimensions and position
        int dx = x2 - x1;
        int dy = y2 - y1;
        double length = Math.sqrt(dx * dx + dy * dy);
        double angle = Math.atan2(dy, dx) * 180 / Math.PI;

        // Create wire drawable (rounded rectangle)
        GradientDrawable wireDrawable = new GradientDrawable();
        wireDrawable.setShape(GradientDrawable.RECTANGLE);
        wireDrawable.setCornerRadius(4f); // Rounded ends

        // Select wire color
        int wireColor = WIRE_COLORS[colorGenerator.nextInt(WIRE_COLORS.length)];
        wireDrawable.setColor(wireColor);

        wireView.setImageDrawable(wireDrawable);

        // Set wire dimensions
        int wireWidth = (int) length;
        int wireHeight = 8; // Wire thickness

        // Position the wire
        RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(wireWidth, wireHeight);
        params.leftMargin = Math.min(x1, x2);
        params.topMargin = Math.min(y1, y2) - wireHeight / 2;

        // Apply rotation
        wireView.setRotation((float) angle);
        wireView.setPivotX(0);
        wireView.setPivotY(wireHeight / 2f);

        // Adjust position for rotation
        if (x2 < x1) {
            params.leftMargin = x2;
            wireView.setRotation((float) (angle + 180));
        } else {
            params.leftMargin = x1;
        }

        wireView.setLayoutParams(params);

        // Store reference to wire view with coordinate info
        wireView.setTag(pin1.toString() + ":" + pin2.toString());

        // Add to container
        if (breadboardContainer instanceof RelativeLayout) {
            ((RelativeLayout) breadboardContainer).addView(wireView);
        } else {
            // If container is not RelativeLayout, wrap in one or handle differently
            // This is a fallback - you might need to adjust based on your layout
            ViewGroup.LayoutParams layoutParams = new ViewGroup.LayoutParams(wireWidth, wireHeight);
            wireView.setLayoutParams(layoutParams);
            breadboardContainer.addView(wireView);
        }

        wireViews.add(wireView);

        // Make wire clickable for removal
        wireView.setOnClickListener(v -> showWireRemovalDialog(pin1, pin2, wireView));
    }

    private boolean canConnectPins(Coordinate pin1, Coordinate pin2) {
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
    private void removeWireConnection(Coordinate from, Coordinate to) {
        List<Coordinate> connections = wireConnections.get(from);
        if (connections != null) {
            connections.remove(to);
            if (connections.isEmpty()) {
                wireConnections.remove(from);
            }
        }
    }

    /**
     * Remove a wire connection and its visual representation
     */
    public void removeWire(Coordinate coord) {
        List<Pins> wiresToRemove = new ArrayList<>();
        List<ImageView> wireViewsToRemove = new ArrayList<>();

        // Find all wires connected to this coordinate
        for (Pins wire : wires) {
            if (wire.getSrc().equals(coord) || wire.getDst().equals(coord)) {
                wiresToRemove.add(wire);

                // Find corresponding visual wire
                String wireTag = wire.getSrc().toString() + ":" + wire.getDst().toString();
                String reverseWireTag = wire.getDst().toString() + ":" + wire.getSrc().toString();

                for (ImageView wireView : wireViews) {
                    String viewTag = (String) wireView.getTag();
                    if (wireTag.equals(viewTag) || reverseWireTag.equals(viewTag)) {
                        wireViewsToRemove.add(wireView);
                        break;
                    }
                }
            }
        }

        // Remove found wires
        for (Pins wire : wiresToRemove) {
            removeWireConnection(wire.getSrc(), wire.getDst());
            removeWireConnection(wire.getDst(), wire.getSrc());

            // Reset pin attributes
            pinAttributes[wire.getSrc().s][wire.getSrc().r][wire.getSrc().c].link = -1;
            pinAttributes[wire.getDst().s][wire.getDst().r][wire.getDst().c].link = -1;

            // Reset visual highlighting
            resetPinHighlight(wire.getSrc());
            resetPinHighlight(wire.getDst());

            // Notify ConnectionManager about wire removal
            if (connectionManager != null) {
                connectionManager.onWireRemoved(wire.getSrc(), wire.getDst());
            }

            System.out.println("Wire removed: " + wire.getSrc() + " <-> " + wire.getDst());
        }

        // Remove visual wire representations
        for (ImageView wireView : wireViewsToRemove) {
            breadboardContainer.removeView(wireView);
            wireViews.remove(wireView);
        }

        wires.removeAll(wiresToRemove);

        if (!wiresToRemove.isEmpty()) {
            showToast(wiresToRemove.size() + " wire(s) removed");
        }
    }

    /**
     * Show wire removal dialog for specific wire
     */
    public void showWireRemovalDialog(Coordinate pin1, Coordinate pin2, ImageView wireView) {
        AlertDialog.Builder builder = new AlertDialog.Builder(mainActivity);
        builder.setTitle("Remove Wire");
        builder.setMessage("Remove this wire connection?");

        builder.setPositiveButton("Remove", (dialog, which) -> {
            removeSpecificWire(pin1, pin2, wireView);
        });

        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    /**
     * Remove a specific wire connection
     */
    private void removeSpecificWire(Coordinate pin1, Coordinate pin2, ImageView wireView) {
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
            removeWireConnection(wireToRemove.getSrc(), wireToRemove.getDst());
            removeWireConnection(wireToRemove.getDst(), wireToRemove.getSrc());

            // Reset pin attributes
            pinAttributes[wireToRemove.getSrc().s][wireToRemove.getSrc().r][wireToRemove.getSrc().c].link = -1;
            pinAttributes[wireToRemove.getDst().s][wireToRemove.getDst().r][wireToRemove.getDst().c].link = -1;

            // Reset visual highlighting
            resetPinHighlight(wireToRemove.getSrc());
            resetPinHighlight(wireToRemove.getDst());

            // Remove visual wire
            breadboardContainer.removeView(wireView);
            wireViews.remove(wireView);

            // Remove from wires list
            wires.remove(wireToRemove);

            // Notify ConnectionManager about wire removal
            if (connectionManager != null) {
                connectionManager.onWireRemoved(wireToRemove.getSrc(), wireToRemove.getDst());
            }

            showToast("Wire removed");
            System.out.println("Wire removed: " + wireToRemove.getSrc() + " <-> " + wireToRemove.getDst());
        }
    }

    public void showWireConnectionDialog(Coordinate coord) {
        String[] options = {"Start Wire Connection", "Remove Wire", "Cancel"};

        AlertDialog.Builder builder = new AlertDialog.Builder(mainActivity);
        builder.setTitle("Wire Connection Options")
                .setMessage("Choose an action for pin " + coord.toString())
                .setItems(options, (dialog, which) -> {
                    switch (which) {
                        case 0: // Start Wire Connection
                            startWireConnection(coord);
                            break;
                        case 1: // Remove Wire
                            showWireRemovalDialog(coord);
                            break;
                        case 2: // Cancel
                            // Do nothing
                            break;
                    }
                })
                .show();
    }

    /**
     * Start wire connection process from a specific coordinate
     */
    private void startWireConnection(Coordinate coord) {
        if (!isValidWirePin(coord)) {
            showToast("Cannot connect wire to this pin type");
            return;
        }

        // Enable wire mode if not already enabled
        if (!isWireMode) {
            isWireMode = true;
            showToast("Wire mode activated");
        }

        // Set this as the first pin
        firstWirePin = coord;
        highlightPin(coord, WIRE_COLOR_SELECTED);
        showToast("First pin selected. Click another pin to complete connection.");
    }

    /**
     * Show wire removal dialog for a coordinate
     */
    public void showWireRemovalDialog(Coordinate coord) {
        // Check if this coordinate has any wires
        boolean hasWires = false;
        for (Pins wire : wires) {
            if (wire.getSrc().equals(coord) || wire.getDst().equals(coord)) {
                hasWires = true;
                break;
            }
        }

        if (!hasWires) {
            showToast("No wires connected to this pin");
            return;
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(mainActivity);
        builder.setTitle("Remove Wire");
        builder.setMessage("Remove all wire connections from this pin?");

        builder.setPositiveButton("Remove", (dialog, which) -> {
            removeWire(coord);
        });

        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    /**
     * Highlight a pin with the specified color
     */
    private void highlightPin(Coordinate coord, int color) {
        ImageButton pin = pins[coord.s][coord.r][coord.c];
        if (pin != null) {
            pin.setColorFilter(color);
        }
    }

    /**
     * Reset pin highlighting
     */
    private void resetPinHighlight(Coordinate coord) {
        ImageButton pin = pins[coord.s][coord.r][coord.c];
        if (pin != null) {
            pin.clearColorFilter();
        }
    }

    /**
     * Reset wire selection state
     */
    private void resetWireSelection() {
        if (firstWirePin != null) {
            resetPinHighlight(firstWirePin);
            firstWirePin = null;
        }
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
     * Check if wire mode is currently active
     */
    public boolean isWireMode() {
        return isWireMode;
    }

    /**
     * Get debug information about wires
     */
    public String getWireDebugInfo() {
        StringBuilder debug = new StringBuilder();
        debug.append("Wires: ").append(wires.size()).append("\n");
        debug.append("Visual Wires: ").append(wireViews.size()).append("\n");

        for (int i = 0; i < wires.size(); i++) {
            Pins wire = wires.get(i);
            debug.append("Wire ").append(i + 1).append(": ")
                    .append(wire.getSrc()).append(" <-> ").append(wire.getDst()).append("\n");
        }

        return debug.toString();
    }

    /**
     * Clear all wires and their visual representations
     */
    public void clearAllWires() {
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

            resetPinHighlight(wire.getSrc());
            resetPinHighlight(wire.getDst());
        }

        // Remove all visual wire representations
        for (ImageView wireView : wireViews) {
            breadboardContainer.removeView(wireView);
        }

        // Clear data structures
        wires.clear();
        wireConnections.clear();
        wireViews.clear();

        // Reset wire mode
        isWireMode = false;
        resetWireSelection();

        showToast("All wires cleared");
    }

    /**
     * Refresh all visual wires (useful after layout changes)
     */
    public void refreshVisualWires() {
        // Remove all visual wires
        for (ImageView wireView : wireViews) {
            breadboardContainer.removeView(wireView);
        }
        wireViews.clear();

        // Recreate visual wires
        for (Pins wire : wires) {
            createVisualWire(wire.getSrc(), wire.getDst());
        }
    }

    /**
     * Display a toast message
     */
    private void showToast(String message) {
        Toast.makeText(mainActivity, message, Toast.LENGTH_SHORT).show();
    }
}