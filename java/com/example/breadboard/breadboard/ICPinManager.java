package com.example.breadboard;

import com.example.breadboard.logic.ICGate;
import com.example.breadboard.model.Attribute;
import com.example.breadboard.model.Coordinate;
import com.example.breadboard.MainActivity;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ICPinManager {

    MainActivity mainActivity;

    private static final int ROWS = 5;
    private static final int COLS = 64;
    private static final int SECTIONS = 2;

    private static Map<Coordinate, ICPinInfo> icPinRegistry = new HashMap<>();
    private Attribute[][][] pinAttributes;
    private List<Coordinate> vccPins;
    private List<Coordinate> gndPins;
    private List<Coordinate> inputs;

    public static class ICPinInfo {
        public String function; // "INPUT", "OUTPUT", "VCC", "GND"
        public ICGate icGate;   // Reference to the IC gate
        public int logicalPin;  // Logical pin number (1-14)

        public ICPinInfo(String function, ICGate icGate, int logicalPin) {
            this.function = function;
            this.icGate = icGate;
            this.logicalPin = logicalPin;
        }
    }

    public ICPinManager(MainActivity mainActivity, Attribute[][][] pinAttributes, List<Coordinate> vccPins,
                        List<Coordinate> gndPins, List<Coordinate> inputs) {
        this.mainActivity = mainActivity;
        this.pinAttributes = pinAttributes;
        this.vccPins = vccPins;
        this.gndPins = gndPins;
        this.inputs = inputs;
    }

    public void registerICPin(Coordinate pinCoord, String function, ICGate icGate) {
        // Validate coordinates
        if (pinCoord == null || pinCoord.s < 0 || pinCoord.s >= SECTIONS ||
                pinCoord.r < 0 || pinCoord.r >= ROWS ||
                pinCoord.c < 0 || pinCoord.c >= COLS) {
            return;
        }

        // Calculate logical pin number based on physical position
        int logicalPin = getLogicalPinNumber(pinCoord, icGate.position);

        // Create pin info and register it
        ICPinInfo pinInfo = new ICPinInfo(function, icGate, logicalPin);
        icPinRegistry.put(pinCoord, pinInfo);

        // Mark the pin as an IC pin in the attributes
        pinAttributes[pinCoord.s][pinCoord.r][pinCoord.c] = new Attribute(-3, -3);
    }

    private int getLogicalPinNumber(Coordinate pinCoord, Coordinate icPosition) {
        if (pinCoord.s == 1 && pinCoord.r == 0) {
            // Top row (F): pins 1-7
            return (pinCoord.c - icPosition.c) + 1;
        } else if (pinCoord.s == 0 && pinCoord.r == 4) {
            // Bottom row (E): pins 8-14 in reverse order
            return 14 - (pinCoord.c - icPosition.c);
        }
        return -1; // Invalid pin
    }

    public int getPinValue(Coordinate pinCoord) {
        // Check if this is a registered IC pin
        ICPinInfo pinInfo = icPinRegistry.get(pinCoord);
        if (pinInfo != null) {
            // FIXED: For IC OUTPUT pins, return their stored value directly
            if ("OUTPUT".equals(pinInfo.function)) {
                Attribute attr = pinAttributes[pinCoord.s][pinCoord.r][pinCoord.c];
                if (attr.value != -1 && attr.value != -3) {
                    return attr.value;
                }
                // If no value stored, default to 0
                return 0;
            }
            // For IC INPUT pins, check the column for connections
            return getColumnValue(pinCoord);
        }

        // For regular pins, use existing logic
        return mainActivity.getValue(pinCoord);
    }

    private int getColumnValue(Coordinate pinCoord) {
        // Check all rows in the same section and column
        for (int r = 0; r < ROWS; r++) {
            Coordinate checkCoord = new Coordinate(pinCoord.s, r, pinCoord.c);
            System.out.println("Checking " + checkCoord + ", attr.value=" + pinAttributes[pinCoord.s][r][pinCoord.c].value + ", isInput=" + inputs.contains(checkCoord));
            // Skip the IC pin itself
            if (icPinRegistry.containsKey(checkCoord)) {
                continue;
            }

            Attribute attr = pinAttributes[pinCoord.s][r][pinCoord.c];

            // Check for VCC connections
            if (vccPins.contains(checkCoord)) {
                return 1;
            }

            // Check for GND connections
            if (gndPins.contains(checkCoord)) {
                return 0; // FIXED: Return 0 instead of -2 for logic purposes
            }

            // Check for input connections
            if (inputs.contains(checkCoord)) {
                return attr.value != -1 ? attr.value : 0;
            }

            // Check for other connections
            if (attr.value != -1 && attr.value != -3) {
                return attr.value;
            }
        }

        return 0; // Default to 0 if no connection found
    }

    /**
     * FIXED: Improved IC pin value setting
     */
    public void setICPinValue(Coordinate pinCoord, int value) {
        System.out.println("Coordinate: " + pinCoord + ", Value: " + value);
        ICPinInfo pinInfo = icPinRegistry.get(pinCoord);
        if (pinInfo != null && "OUTPUT".equals(pinInfo.function)) {
            // FIXED: Set the IC pin's own attribute value properly
            Attribute icAttr = pinAttributes[pinCoord.s][pinCoord.r][pinCoord.c];
            icAttr.value = value;

            // FIXED: Also propagate to connected pins in the same column
            propagateToColumn(pinCoord, value);
        }
    }

    /**
     * FIXED: New method to properly propagate IC output values to column
     */
    private void propagateToColumn(Coordinate icPinCoord, int value) {
        for (int r = 0; r < ROWS; r++) {
            if (r == icPinCoord.r) continue; // Skip the IC pin itself
            
            Coordinate checkCoord = new Coordinate(icPinCoord.s, r, icPinCoord.c);
            
            // Skip other IC pins
            if (icPinRegistry.containsKey(checkCoord)) {
                continue;
            }
            
            Attribute attr = pinAttributes[icPinCoord.s][r][icPinCoord.c];
            
            // Set value for connected pins (link != -1) or output pins (value == 2)
            if (attr.link != -1 || attr.value == 2) {
                attr.value = value;
            }
        }
    }

    public ICPinInfo getICPinInfo(Coordinate pinCoord) {
        return icPinRegistry.get(pinCoord);
    }

    public boolean isICPin(Coordinate pinCoord) {
        return icPinRegistry.containsKey(pinCoord);
    }

    public void unregisterICPins(ICGate icGate) {
        icPinRegistry.entrySet().removeIf(entry -> entry.getValue().icGate == icGate);
    }
}