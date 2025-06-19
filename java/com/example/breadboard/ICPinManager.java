package com.example.breadboard;

import com.example.breadboard.logic.ICGate;
import com.example.breadboard.model.Attribute;
import com.example.breadboard.model.Coordinate;

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

    public void setPinValue(Coordinate icCoord, Integer propagatedValue) {

    }

    public boolean isICOutputPin(Coordinate coord) {
        // Check if this coordinate is registered as an IC pin
        ICPinInfo pinInfo = getICPinInfo(coord);

        if (pinInfo != null) {
            // Check if the pin function is OUTPUT
            return "OUTPUT".equals(pinInfo.function);
        }

        return false; // Not an IC pin or not an output pin
    }

    public boolean isICOutputPinOptimized(Coordinate coord) {
        // Direct lookup in the registry
        for (Map.Entry<Coordinate, ICPinInfo> entry : icPinRegistry.entrySet()) {
            Coordinate regCoord = entry.getKey();

            // Check if coordinates match
            if (regCoord.s == coord.s && regCoord.r == coord.r && regCoord.c == coord.c) {
                ICPinInfo pinInfo = entry.getValue();
                return "OUTPUT".equals(pinInfo.function);
            }
        }

        return false; // Not found or not an output pin
    }


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

        // FIXED: Initialize IC pin with proper attribute
        // For OUTPUT pins, we'll update the value later during execution
        // For INPUT pins, we set to -3 as marker
        if ("OUTPUT".equals(function)) {
            pinAttributes[pinCoord.s][pinCoord.r][pinCoord.c] = new Attribute(-3, 0); // Start with 0, will be updated
        } else {
            pinAttributes[pinCoord.s][pinCoord.r][pinCoord.c] = new Attribute(-3, -3);
        }

        System.out.println("Registered IC pin at " + pinCoord + " as " + function + " with logical pin " + logicalPin);
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
            if ("OUTPUT".equals(pinInfo.function)) {
                // FIXED: For IC OUTPUT pins, return their stored value directly
                Attribute attr = pinAttributes[pinCoord.s][pinCoord.r][pinCoord.c];
                System.out.println("IC OUTPUT pin " + pinCoord + " has attribute value: " + attr.value);

                // Return the actual computed value, not -3
                if (attr.value != -3 && attr.value != -1) {
                    return attr.value;
                }
                // If still -3, it means the value hasn't been set yet, default to 0
                return 0;
            }
            // For IC INPUT pins, check the column for connections
            return getColumnValue(pinCoord);
        }

        // For regular pins, use existing logic
        return getValue(pinCoord);
    }

    public int getValue(Coordinate src) {
        Attribute tmp;

        for (int i = 0; i < ROWS; i++) {
            tmp = pinAttributes[src.s][i][src.c];
            if (src.r != i && tmp.link != -1 && tmp.value != 2) {
                return tmp.value;
            }
        }
        return 0;
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
                return 0; // Return 0 for logic purposes
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
     * FIXED: Improved IC pin value setting with better coordinate matching
     */
    public void setICPinValue(Coordinate pinCoord, int value) {
        System.out.println("setICPinValue: Coordinate: " + pinCoord + ", Value: " + value);

        // FIXED: Check if the coordinate exists in the registry using proper comparison
        ICPinInfo pinInfo = null;
        for (Map.Entry<Coordinate, ICPinInfo> entry : icPinRegistry.entrySet()) {
            Coordinate regCoord = entry.getKey();
            if (regCoord.s == pinCoord.s && regCoord.r == pinCoord.r && regCoord.c == pinCoord.c) {
                pinInfo = entry.getValue();
                break;
            }
        }

        if (pinInfo != null && "OUTPUT".equals(pinInfo.function)) {
            // FIXED: Set the IC pin's own attribute value properly
            Attribute icAttr = pinAttributes[pinCoord.s][pinCoord.r][pinCoord.c];
            icAttr.value = value;
            System.out.println("setICPinValue: Set IC output pin " + pinCoord + " to value " + value);

            // FIXED: Also propagate to connected pins in the same column
            propagateToColumn(pinCoord, value);
        } else {
            System.out.println("setICPinValue: Pin " + pinCoord + " is not a registered IC OUTPUT pin");
            // DEBUG: Print all registered pins to help diagnose
            System.out.println("DEBUG: All registered IC pins:");
            for (Map.Entry<Coordinate, ICPinInfo> entry : icPinRegistry.entrySet()) {
                Coordinate coord = entry.getKey();
                ICPinInfo info = entry.getValue();
                System.out.println("  " + coord + " -> " + info.function);
            }
        }
    }

    /**
     * FIXED: New method to properly propagate IC output values to column
     */
    private void propagateToColumn(Coordinate icPinCoord, int value) {
        System.out.println("propagateToColumn: IC pin " + icPinCoord + " propagating value " + value);

        for (int r = 0; r < ROWS; r++) {
            if (r == icPinCoord.r) continue; // Skip the IC pin itself

            Coordinate checkCoord = new Coordinate(icPinCoord.s, r, icPinCoord.c);

            // Skip other IC pins
            if (icPinRegistry.containsKey(checkCoord)) {
                System.out.println("propagateToColumn: Skipping IC pin at " + checkCoord);
                continue;
            }

            Attribute attr = pinAttributes[icPinCoord.s][r][icPinCoord.c];

            // Set value for connected pins (link != -1) or output pins (value == 2)
            if (attr.link != -1 || attr.value == 2) {
                attr.value = value;
                System.out.println("propagateToColumn: Set " + checkCoord + " to value " + value + " (link=" + attr.link + ")");
            }
        }
    }

    public ICPinInfo getICPinInfo(Coordinate pinCoord) {
        return icPinRegistry.get(pinCoord);
    }

    public boolean isICPin(Coordinate pinCoord) {
        // FIXED: Use proper coordinate comparison
        for (Coordinate regCoord : icPinRegistry.keySet()) {
            if (regCoord.s == pinCoord.s && regCoord.r == pinCoord.r && regCoord.c == pinCoord.c) {
                return true;
            }
        }
        return false;
    }

    public void unregisterICPins(ICGate icGate) {
        icPinRegistry.entrySet().removeIf(entry -> entry.getValue().icGate == icGate);
    }

    public void debugPrintICPins() {
        System.out.println("=== IC Pin Registry Debug ===");
        for (Map.Entry<Coordinate, ICPinInfo> entry : icPinRegistry.entrySet()) {
            Coordinate coord = entry.getKey();
            ICPinInfo info = entry.getValue();
            Attribute attr = pinAttributes[coord.s][coord.r][coord.c];
            System.out.println("IC Pin: " + coord + " -> " + info.function +
                    " (logical pin " + info.logicalPin + "), attr.value=" + attr.value);
        }
        System.out.println("=== End IC Pin Registry Debug ===");
    }
}