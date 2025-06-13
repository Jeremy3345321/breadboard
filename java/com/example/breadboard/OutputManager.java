package com.example.breadboard;

import android.widget.ImageButton;
import android.widget.Toast;

import com.example.breadboard.model.Attribute;
import com.example.breadboard.model.Coordinate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class OutputManager {

    private MainActivity mainActivity;
    private ImageButton[][][] pins;
    private Attribute[][][] pinAttributes;
    private List<Coordinate> outputs;
    private ICPinManager icPinManager;

    // Map to track output states
    private Map<Coordinate, Boolean> outputStates = new HashMap<>();

    public OutputManager(MainActivity mainActivity, ImageButton[][][] pins,
                         Attribute[][][] pinAttributes, List<Coordinate> outputs,
                         ICPinManager icPinManager) {
        this.mainActivity = mainActivity;
        this.pins = pins;
        this.pinAttributes = pinAttributes;
        this.outputs = outputs;
        this.icPinManager = icPinManager;
    }

    /**
     * Add an output pin at the specified coordinate
     */
    public void addOutput(Coordinate coord) {
        // Check if pin is available for output placement
        if (!isOutputPlacementValid(coord)) {
            showToast("Error! Another Connection already Exists!");
            return;
        }

        // Resize pin and set initial drawable (off state)
        mainActivity.resizeSpecialPin(coord, R.drawable.breadboard_otpt);

        // Add to outputs list and set attributes
        outputs.add(coord);
        pinAttributes[coord.s][coord.r][coord.c] = new Attribute(-2, 2);

        // Initialize output state as off
        outputStates.put(coord, false);
    }

    /**
     * Check if output can be placed at this coordinate
     */
    private boolean isOutputPlacementValid(Coordinate coord) {
        // Check if the pin itself is free
        Attribute currentAttr = pinAttributes[coord.s][coord.r][coord.c];
        if (currentAttr.link != -1 || currentAttr.value != -1) {
            return false;
        }

        // Check if there's at least one connection in the same column that can drive the output
        boolean hasConnection = false;
        for (int r = 0; r < 5; r++) { // ROWS = 5
            if (r == coord.r) continue; // Skip the output pin position itself

            Coordinate checkCoord = new Coordinate(coord.s, r, coord.c);
            Attribute attr = pinAttributes[coord.s][r][coord.c];

            // Check for any existing connections or IC pins
            if (icPinManager.isICPin(checkCoord) || 
                attr.link != -1 || 
                attr.value != -1) {
                hasConnection = true;
                break;
            }
        }

        return hasConnection;
    }

    /**
     * Update all output pin visuals based on their current values
     */
    public void updateAllOutputs() {
        // First propagate IC outputs to connected pins
        propagateICOutputs();

        // Then update all output visuals
        for (Coordinate outputCoord : outputs) {
            updateOutputVisual(outputCoord);
        }
    }

    /**
     * Update a specific output pin's visual representation
     */
    public void updateOutputVisual(Coordinate coord) {
        if (!outputs.contains(coord)) {
            return;
        }
        int currentValue = getOutputValue(coord);
        boolean isOn = (currentValue == 1);
        System.out.println("Output at " + coord + " has value " + currentValue + ", isOn=" + isOn);
        Boolean previousState = outputStates.get(coord);
        if (previousState == null || previousState != isOn) {
            ImageButton pin = pins[coord.s][coord.r][coord.c];
            if (pin != null) {
                if (isOn) {
                    pin.setImageResource(R.drawable.breadboard_otpt_on);
                } else {
                    pin.setImageResource(R.drawable.breadboard_otpt);
                }
            }
            outputStates.put(coord, isOn);
        }
    }

    /**
     * Get the current value of an output pin by checking its column
     * FIXED: Improved logic to properly read IC output values
     */
    private int getOutputValue(Coordinate coord) {
        System.out.println("Getting output value for " + coord);
        for (int r = 0; r < 5; r++) {
            if (r == coord.r) continue;
            Coordinate checkCoord = new Coordinate(coord.s, r, coord.c);
            System.out.println("Checking " + checkCoord);
            Attribute attr = pinAttributes[coord.s][r][coord.c];
            if (icPinManager.isICPin(checkCoord)) {
                ICPinManager.ICPinInfo pinInfo = icPinManager.getICPinInfo(checkCoord);
                if (pinInfo != null && "OUTPUT".equals(pinInfo.function)) {
                    int icValue = attr.value;
                    System.out.println("Found IC output pin at " + checkCoord + " with value " + icValue);
                    if (icValue != -1 && icValue != -3) {
                        return icValue;
                    }
                    int fallbackValue = icPinManager.getPinValue(checkCoord);
                    System.out.println("Fallback value from ICPinManager: " + fallbackValue);
                    return fallbackValue;
                }
                continue;
            }
            if (attr.value == 1) {
                System.out.println("Found VCC at " + checkCoord);
                return 1;
            }
            if (attr.value == -2) {
                System.out.println("Found GND at " + checkCoord);
                return 0;
            }
            if (attr.link != -1 && attr.value != -1 && attr.value != -3) {
                System.out.println("Found connected pin at " + checkCoord + " with value " + attr.value);
                return attr.value;
            }
            if (attr.value == 0 || attr.value == 1) {
                System.out.println("Found input pin at " + checkCoord + " with value " + attr.value);
                return attr.value;
            }
        }
        System.out.println("No connection found for " + coord + ", defaulting to 0");
        return 0;
    }

    /**
     * Force set an output value and update visual
     */
    public void setOutputValue(Coordinate coord, int value) {
        if (!outputs.contains(coord)) {
            return;
        }

        // Update the pin's attribute value
        pinAttributes[coord.s][coord.r][coord.c].value = value;

        // Update visual representation
        updateOutputVisual(coord);
    }

    /**
     * Get the current state of an output pin
     */
    public boolean getOutputState(Coordinate coord) {
        Boolean state = outputStates.get(coord);
        return state != null ? state : false;
    }

    /**
     * Remove an output pin
     */
    public void removeOutput(Coordinate coord) {
        outputs.remove(coord);
        outputStates.remove(coord);

        // Reset pin to original state
        ImageButton pin = pins[coord.s][coord.r][coord.c];
        if (pin != null) {
            pin.setImageResource(R.drawable.breadboard_pin);
        }

        // Reset attributes - IMPORTANT: Set both link and value to -1
        pinAttributes[coord.s][coord.r][coord.c] = new Attribute(-1, -1);
        
        // Also clear any column values that might be lingering
        clearColumnValues(coord);
    }

    /**
     * Clear values from the entire column when removing an output
     */
    private void clearColumnValues(Coordinate coord) {
        for (int r = 0; r < 5; r++) { // ROWS = 5
            if (r == coord.r) continue; // Skip the output pin position
            
            Coordinate checkCoord = new Coordinate(coord.s, r, coord.c);
            Attribute attr = pinAttributes[coord.s][r][coord.c];
            
            // Only clear if it's not an IC pin or other special pin
            if (!icPinManager.isICPin(checkCoord) && attr.link == -1) {
                attr.value = -1;
            }
        }
    }

    /**
     * Check if a coordinate is an output pin
     */
    public boolean isOutput(Coordinate coord) {
        return outputs.contains(coord);
    }

    /**
     * Get all output coordinates
     */
    public List<Coordinate> getAllOutputs() {
        return outputs;
    }

    /**
     * FIXED: Improved IC output propagation
     * Propagate IC output values to connected output pins
     * This method should be called after IC execution
     */
    public void propagateICOutputs() {
        // First, make sure IC output pins have their values properly set
        for (Coordinate outputCoord : outputs) {
            // Check if this output is connected to an IC output in the same column
            for (int r = 0; r < 5; r++) { // ROWS = 5
                if (r == outputCoord.r) continue;

                Coordinate checkCoord = new Coordinate(outputCoord.s, r, outputCoord.c);

                if (icPinManager.isICPin(checkCoord)) {
                    ICPinManager.ICPinInfo pinInfo = icPinManager.getICPinInfo(checkCoord);
                    if (pinInfo != null && "OUTPUT".equals(pinInfo.function)) {
                        // FIXED: Get the computed value from the IC pin's attribute
                        Attribute icAttr = pinAttributes[checkCoord.s][checkCoord.r][checkCoord.c];
                        int icOutputValue;
                        
                        // FIXED: Proper value retrieval from IC output
                        if (icAttr.value != -1 && icAttr.value != -3) {
                            icOutputValue = icAttr.value;
                        } else {
                            // Fallback: ask the IC pin manager
                            icOutputValue = icPinManager.getPinValue(checkCoord);
                        }

                        // FIXED: Propagate this value to the entire column properly
                        propagateValueToColumn(outputCoord, icOutputValue);
                        break;
                    }
                }
            }
        }
    }

    /**
     * FIXED: Improved column value propagation
     */
    private void propagateValueToColumn(Coordinate coord, int value) {
        // Set value for all connected pins in the same column and section
        for (int r = 0; r < 5; r++) {
            if (coord.c < 64) { // COLS = 64
                Coordinate checkCoord = new Coordinate(coord.s, r, coord.c);
                Attribute attr = pinAttributes[coord.s][r][coord.c];
                
                // FIXED: Don't overwrite IC pins, but do set output pins and connected pins
                if (!icPinManager.isICPin(checkCoord)) {
                    // Set value for output pins (value 2) or connected pins (link != -1)
                    if (attr.value == 2 || attr.link != -1) {
                        attr.value = value;
                    }
                }
            }
        }
    }

    /**
     * Display a toast message
     */
    private void showToast(String message) {
        Toast.makeText(mainActivity, message, Toast.LENGTH_SHORT).show();
    }

    /**
     * Get debug information about outputs
     */
    public String getOutputDebugInfo() {
        StringBuilder debug = new StringBuilder();
        debug.append("Outputs: ").append(outputs.size()).append("\n");

        for (Coordinate coord : outputs) {
            boolean state = getOutputState(coord);
            int value = getOutputValue(coord);
            char rowLabel = (char)(65 + coord.r + 5 * coord.s);

            debug.append("Pin ").append(rowLabel).append("-").append(coord.c)
                    .append(": State=").append(state ? "ON" : "OFF")
                    .append(", Value=").append(value);
            
            // FIXED: Add more debug info to help troubleshoot
            Attribute attr = pinAttributes[coord.s][coord.r][coord.c];
            debug.append(", Attr.value=").append(attr.value)
                    .append(", Attr.link=").append(attr.link).append("\n");
        }

        return debug.toString();
    }
}