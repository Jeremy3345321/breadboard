package com.example.breadboard;

import android.widget.ImageButton;
import android.widget.Toast;

import com.example.breadboard.model.Attribute;
import com.example.breadboard.model.Coordinate;
import com.example.breadboard.OutputToDB;
import com.example.breadboard.OutputToDB.OutputData;
import java.util.ArrayList;


import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class OutputManager {

    private MainActivity mainActivity;
    private OutputToDB outputToDB;
    private String currentUsername;
    private String currentCircuitName;
    private String previousUsername = null;
    private String previousCircuitName = null;
    private boolean forceNextClear = false;
    private ImageButton[][][] pins;
    private Attribute[][][] pinAttributes;
    private List<Coordinate> outputs;
    private ICPinManager icPinManager;

    // Map to track output states
    private Map<Coordinate, Boolean> outputStates = new HashMap<>();

    public OutputManager(MainActivity mainActivity, ImageButton[][][] pins,
                         Attribute[][][] pinAttributes, List<Coordinate> outputs,
                         ICPinManager icPinManager, String username, String circuitName) {
        this.mainActivity = mainActivity;
        this.pins = pins;
        this.pinAttributes = pinAttributes;
        this.outputs = outputs;
        this.icPinManager = icPinManager;
        this.currentUsername = username;
        this.currentCircuitName = circuitName;
        this.outputToDB = new OutputToDB(mainActivity);
    }

    private void loadOutput(Coordinate coord) {
        System.out.println("Loading output at coordinate: " + coord);

        mainActivity.resizeSpecialPin(coord, R.drawable.breadboard_otpt);

        if (!outputs.contains(coord)) {
            outputs.add(coord);
            System.out.println("Added coordinate " + coord + " to outputs list");
        }

        // Set pin attributes - Use value 2 to mark as output pin, link -1 means no wire connection
        pinAttributes[coord.s][coord.r][coord.c] = new Attribute(-1, 2);

        outputStates.put(coord, false);

        System.out.println("Loaded output at " + coord + " without database save");
    }


    public void addOutput(Coordinate coord) {
        if (!isOutputPlacementValid(coord)) {
            showToast("Error! Another Connection already Exists!");
            return;
        }

        // Resize pin and set initial drawable (off state)
        mainActivity.resizeSpecialPin(coord, R.drawable.breadboard_otpt);

        // Add to outputs list and set attributes
        outputs.add(coord);
        // FIXED: Use value 2 to mark as output pin, link -1 means no wire connection
        pinAttributes[coord.s][coord.r][coord.c] = new Attribute(-1, 2);

        // Initialize output state as off
        outputStates.put(coord, false);

        // Save to database
        boolean dbResult = outputToDB.insertOutput(currentUsername, currentCircuitName, coord);
        if (!dbResult) {
            System.err.println("Failed to save output to database at " + coord + " for circuit " + currentCircuitName);
            showToast("Warning: Failed to save output to database");
        } else {
            System.out.println("Output saved to database successfully at " + coord + " for circuit " + currentCircuitName);
        }
    }

    private boolean isOutputPlacementValid(Coordinate coord) {
        debugOutputPlacement(coord);

        Attribute currentAttr = pinAttributes[coord.s][coord.r][coord.c];

        if (currentAttr.value == 2) {
            return true;
        }

        if (currentAttr.link != -1) {
            return false;
        }

        if (currentAttr.value != -1 && currentAttr.value != 0 &&
                currentAttr.value != -2 && currentAttr.value != 1 &&
                currentAttr.value != 2) {
            return false; // Pin occupied by other component
        }
        return true;
    }

    public void debugOutputPlacement(Coordinate coord) {
        Attribute currentAttr = pinAttributes[coord.s][coord.r][coord.c];
        System.out.println("Current pin - link: " + currentAttr.link + ", value: " + currentAttr.value);

        System.out.println("Checking column connections:");
        for (int r = 0; r < 5; r++) {
            if (r == coord.r) continue;

            Coordinate checkCoord = new Coordinate(coord.s, r, coord.c);
            Attribute attr = pinAttributes[coord.s][r][coord.c];
            boolean isIC = icPinManager.isICPin(checkCoord);

            System.out.println("  Row " + r + " - link: " + attr.link +
                    ", value: " + attr.value + ", isIC: " + isIC);
        }
        System.out.println("=== END DEBUG ===");
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
        System.out.println("Getting output value for " + coord.toString());

        for (int r = 0; r < 5; r++) {
            if (r == coord.r) continue; // Skip the output pin itself

            Coordinate checkCoord = new Coordinate(coord.s, r, coord.c);
            System.out.println("Checking " + checkCoord.toString());

            // FIXED: Check IC pins first and handle them properly
            if (icPinManager.isICPin(checkCoord)) {
                ICPinManager.ICPinInfo pinInfo = icPinManager.getICPinInfo(checkCoord);
                if (pinInfo != null && "OUTPUT".equals(pinInfo.function)) {
                    // FIXED: Use ICPinManager's getPinValue method which handles IC outputs correctly
                    int icValue = icPinManager.getPinValue(checkCoord);
                    System.out.println("Found IC output pin at " + checkCoord + " with value " + icValue);
                    return icValue;
                }
                // For IC input pins, continue checking other pins in column
                continue;
            }

            // Check regular pins
            Attribute attr = pinAttributes[coord.s][r][coord.c];

            // Check for VCC (value = 1)
            if (attr.value == 1) {
                System.out.println("Found VCC at " + checkCoord.toString());
                return 1;
            }

            // Check for GND (value = -2)
            if (attr.value == -2) {
                System.out.println("Found GND at " + checkCoord.toString());
                return 0;
            }

            // Check for input pins (value = 0, but not -1, -3, or 2)
            if (attr.value == 0) {
                System.out.println("Found input pin at " + checkCoord.toString() + " with value 0");
                return 0;
            }

            // Check for other connected pins (exclude output pins with value 2)
            if (attr.link != -1 && attr.value != -1 && attr.value != -3 && attr.value != 2) {
                System.out.println("Found connected pin at " + checkCoord.toString() + " with value " + attr.value);
                return attr.value;
            }
        }

        System.out.println("getOutputValue: No connection found for " + coord.toString() + ", defaulting to 0");
        return 0;
    }

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

        // Remove from database
        boolean dbResult = outputToDB.deleteOutputByCoordinate(currentUsername, currentCircuitName, coord);
        if (!dbResult) {
            System.err.println("Failed to remove output from database at " + coord + " for circuit " + currentCircuitName);
        } else {
            System.out.println("Removed output from database at " + coord + " for circuit " + currentCircuitName);
        }
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
                        Attribute icAttr = pinAttributes[checkCoord.s][checkCoord.r][checkCoord.c];
                        int icOutputValue;

                        // FIXED: Proper value retrieval from IC output
                        if (icAttr.value != -1 && icAttr.value != -3) {
                            icOutputValue = icAttr.value;
                        } else {
                            icOutputValue = icPinManager.getPinValue(checkCoord);
                        }

                        break;
                    }
                }
            }
        }
    }

    public void loadOutputsFromDatabase() {
        try {
//            System.out.println("=== LOADING OUTPUTS FROM DATABASE START ===");
//            System.out.println("Loading for Username: " + currentUsername + ", Circuit: " + currentCircuitName);
//            System.out.println("Previous context: " + getPreviousCircuitContext());

            List<OutputData> dbOutputs = outputToDB.getOutputsForCircuit(currentUsername, currentCircuitName);
            System.out.println("Database returned " + dbOutputs.size() + " outputs");

            // Ensure we start with clean state
            if (!outputs.isEmpty() || !outputStates.isEmpty()) {
                System.out.println("Warning: Loading outputs but memory isn't clean. Clearing first.");
                clearInMemoryOutputData();
            }

            for (OutputData outputData : dbOutputs) {
                // Additional safety check: only load outputs that match the current circuit name and username
                if (!outputData.circuitName.equals(currentCircuitName) || !outputData.username.equals(currentUsername)) {
                    System.out.println("Skipping output - belongs to different circuit/user: " +
                            outputData.circuitName + "/" + outputData.username);
                    continue;
                }

                // Translate database coordinates to Coordinate object
                Coordinate coord = new Coordinate(outputData.section, outputData.row_pos, outputData.column_pos);
                System.out.println("Processing output at coordinate " + coord);

                // Load the output using the new loadOutput method (doesn't save to DB)
                loadOutput(coord);

                System.out.println("Loaded output from database at " + coord + " for circuit " + currentCircuitName);
            }

            System.out.println("Final outputs list size: " + outputs.size());
            System.out.println("Final outputStates map size: " + outputStates.size());

            // Update all output visuals after loading
            updateAllOutputs();

            System.out.println("Loaded " + dbOutputs.size() + " outputs from database for circuit " + currentCircuitName);
            System.out.println("=== LOADING OUTPUTS FROM DATABASE END ===");

        } catch (Exception e) {
            System.err.println("Error loading outputs from database for circuit " + currentCircuitName + ": " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void clearOutputsFromDatabase() {
        boolean result = outputToDB.clearOutputsForCircuit(currentUsername, currentCircuitName);
        if (result) {
            System.out.println("Cleared all outputs from database for circuit " + currentCircuitName);
        } else {
            System.err.println("Failed to clear outputs from database for circuit " + currentCircuitName);
        }
    }

    public void updateCircuitContext(String username, String circuitName) {
        System.out.println("OutputManager: Updating circuit context from [" + currentUsername + ", " + currentCircuitName +
                "] to [" + username + ", " + circuitName + "]");

        // Enhanced logic to detect when we need to clear data
        boolean isActualSwitch = !username.equals(currentUsername) || !circuitName.equals(currentCircuitName);
        boolean isReturningToDifferentCircuit = (previousUsername != null && previousCircuitName != null) &&
                (!username.equals(previousUsername) || !circuitName.equals(previousCircuitName));
        boolean shouldClearData = isActualSwitch || forceNextClear || isReturningToDifferentCircuit;

//        System.out.println("Context switch analysis:");
//        System.out.println("- isActualSwitch: " + isActualSwitch);
//        System.out.println("- isReturningToDifferentCircuit: " + isReturningToDifferentCircuit);
//        System.out.println("- forceNextClear: " + forceNextClear);
//        System.out.println("- shouldClearData: " + shouldClearData);

        if (shouldClearData) {
            previousUsername = currentUsername;
            previousCircuitName = currentCircuitName;

            clearInMemoryOutputData();
            clearOutputVisuals();

            System.out.println("Cleared data for circuit context change");

            forceNextClear = false;
        } else {
            System.out.println("No data clearing needed for this context update");
        }

        // Update context
        this.currentUsername = username;
        this.currentCircuitName = circuitName;

        System.out.println("OutputManager context updated successfully - Username: " + username + ", Circuit: " + circuitName);
    }

    public void clearInMemoryOutputData() {
//        System.out.println("=== CLEARING IN-MEMORY OUTPUT DATA START ===");
//        System.out.println("Before clearing:");
//        System.out.println("- outputs.size(): " + outputs.size());
//        System.out.println("- outputStates.size(): " + outputStates.size());

        // Clear the output coordinate list
        outputs.clear();

        // Clear the output state mappings
        outputStates.clear();

//        System.out.println("After clearing:");
//        System.out.println("- outputs.size(): " + outputs.size());
//        System.out.println("- outputStates.size(): " + outputStates.size());
//
//        System.out.println("In-memory output data cleared successfully");
//        System.out.println("=== CLEARING IN-MEMORY OUTPUT DATA END ===");
    }

    public void clearOutputVisuals() {
        System.out.println("Clearing output visual elements from breadboard");

        // Reset pins and their attributes
        for (Coordinate coord : new ArrayList<>(outputs)) { // Create copy to avoid concurrent modification
            try {
                // Reset pin attributes
                if (pinAttributes != null && coord.s < pinAttributes.length &&
                        coord.r < pinAttributes[coord.s].length && coord.c < pinAttributes[coord.s][coord.r].length) {
                    pinAttributes[coord.s][coord.r][coord.c] = new Attribute(-1, -1);
                }

                // Reset pin visual
                ImageButton pin = pins[coord.s][coord.r][coord.c];
                if (pin != null) {
                    pin.setImageResource(R.drawable.breadboard_pin);
                }

            } catch (Exception e) {
                System.err.println("Error clearing visual for output at " + coord + ": " + e.getMessage());
            }
        }

        System.out.println("Output visual elements cleared successfully");
    }

    private void showToast(String message) {
        Toast.makeText(mainActivity, message, Toast.LENGTH_SHORT).show();
    }
}