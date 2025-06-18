package com.example.breadboard;

import android.widget.ImageButton;
import android.widget.Toast;

import com.example.breadboard.model.Attribute;
import com.example.breadboard.model.Coordinate;
import com.example.breadboard.ComponentToDB;
import com.example.breadboard.ComponentToDB.ComponentData;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ComponentManager {

    private MainActivity mainActivity;
    private ComponentToDB componentToDB;
    private String currentUsername;
    private String currentCircuitName;
    private String previousUsername = null;
    private String previousCircuitName = null;
    private boolean forceNextClear = false;
    private ImageButton[][][] pins;
    private Attribute[][][] pinAttributes;
    private List<Coordinate> vccPins;
    private List<Coordinate> gndPins;

    // Map to track component states
    private Map<Coordinate, Integer> componentStates = new HashMap<>();

    public ComponentManager(MainActivity mainActivity, ImageButton[][][] pins,
                           Attribute[][][] pinAttributes, List<Coordinate> vccPins, 
                           List<Coordinate> gndPins, String username, String circuitName) {
        this.mainActivity = mainActivity;
        this.pins = pins;
        this.pinAttributes = pinAttributes;
        this.vccPins = vccPins;
        this.gndPins = gndPins;
        this.currentUsername = username;
        this.currentCircuitName = circuitName;
        this.componentToDB = new ComponentToDB(mainActivity);
    }

    private void loadComponent(Coordinate coord, int componentType) {
        System.out.println("Loading component at coordinate: " + coord + " with type: " + componentType);

        if (componentType == ComponentToDB.VCC) {
            mainActivity.resizeSpecialPin(coord, R.drawable.breadboard_vcc);
            if (!vccPins.contains(coord)) {
                vccPins.add(coord);
                System.out.println("Added coordinate " + coord + " to vccPins list");
            }
            // Set pin attributes for VCC
            pinAttributes[coord.s][coord.r][coord.c] = new Attribute(-1, 1);
        } else if (componentType == ComponentToDB.GND) {
            mainActivity.resizeSpecialPin(coord, R.drawable.breadboard_gnd);
            if (!gndPins.contains(coord)) {
                gndPins.add(coord);
                System.out.println("Added coordinate " + coord + " to gndPins list");
            }
            // Set pin attributes for GND
            pinAttributes[coord.s][coord.r][coord.c] = new Attribute(-1, -2);
        }

        componentStates.put(coord, componentType);
        System.out.println("Loaded component at " + coord + " without database save");
    }

    public void addComponent(Coordinate coord, int componentType) {
        if (!isComponentPlacementValid(coord)) {
            showToast("Error! Another Connection already Exists!");
            return;
        }

        if (componentType == ComponentToDB.VCC) {
            // Resize pin and set VCC drawable
            mainActivity.resizeSpecialPin(coord, R.drawable.breadboard_vcc);
            
            // Add to vccPins list and set attributes
            vccPins.add(coord);
            pinAttributes[coord.s][coord.r][coord.c] = new Attribute(-1, 1);
            
            // Save to database
            boolean dbResult = componentToDB.insertVCC(currentUsername, currentCircuitName, coord);
            if (!dbResult) {
                System.err.println("Failed to save VCC to database at " + coord + " for circuit " + currentCircuitName);
                showToast("Warning: Failed to save VCC to database");
            } else {
                System.out.println("VCC saved to database successfully at " + coord + " for circuit " + currentCircuitName);
            }
            
        } else if (componentType == ComponentToDB.GND) {
            // Resize pin and set GND drawable
            mainActivity.resizeSpecialPin(coord, R.drawable.breadboard_gnd);
            
            // Add to gndPins list and set attributes
            gndPins.add(coord);
            pinAttributes[coord.s][coord.r][coord.c] = new Attribute(-1, -2);
            
            // Save to database
            boolean dbResult = componentToDB.insertGND(currentUsername, currentCircuitName, coord);
            if (!dbResult) {
                System.err.println("Failed to save GND to database at " + coord + " for circuit " + currentCircuitName);
                showToast("Warning: Failed to save GND to database");
            } else {
                System.out.println("GND saved to database successfully at " + coord + " for circuit " + currentCircuitName);
            }
        }

        // Track component state
        componentStates.put(coord, componentType);
    }

    private boolean isComponentPlacementValid(Coordinate coord) {
        debugComponentPlacement(coord);

        Attribute currentAttr = pinAttributes[coord.s][coord.r][coord.c];

        // Allow placement if it's already a VCC or GND pin
        if (currentAttr.value == 1 || currentAttr.value == -2) {
            return true;
        }

        // Check if pin is already connected
        if (currentAttr.link != -1) {
            return false;
        }

        // Check if pin is occupied by other components
        if (currentAttr.value != -1 && currentAttr.value != 0 &&
                currentAttr.value != 1 && currentAttr.value != -2 && currentAttr.value != 2) {
            return false; // Pin occupied by other component
        }
        return true;
    }

    public void debugComponentPlacement(Coordinate coord) {
        Attribute currentAttr = pinAttributes[coord.s][coord.r][coord.c];
        System.out.println("Current pin - link: " + currentAttr.link + ", value: " + currentAttr.value);

        System.out.println("Checking column connections:");
        for (int r = 0; r < 5; r++) {
            if (r == coord.r) continue;

            Attribute attr = pinAttributes[coord.s][r][coord.c];
            System.out.println("  Row " + r + " - link: " + attr.link + ", value: " + attr.value);
        }
        System.out.println("=== END DEBUG ===");
    }

    public void removeComponent(Coordinate coord) {
        Integer componentType = componentStates.get(coord);
        
        if (componentType != null) {
            if (componentType == ComponentToDB.VCC) {
                vccPins.remove(coord);
            } else if (componentType == ComponentToDB.GND) {
                gndPins.remove(coord);
            }
        }
        
        componentStates.remove(coord);

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
        boolean dbResult = componentToDB.deleteComponentByCoordinate(currentUsername, currentCircuitName, coord);
        if (!dbResult) {
            System.err.println("Failed to remove component from database at " + coord + " for circuit " + currentCircuitName);
        } else {
            System.out.println("Removed component from database at " + coord + " for circuit " + currentCircuitName);
        }
    }

    /**
     * Clear values from the entire column when removing a component
     */
    private void clearColumnValues(Coordinate coord) {
        for (int r = 0; r < 5; r++) { // ROWS = 5
            if (r == coord.r) continue; // Skip the component pin position
            
            Coordinate checkCoord = new Coordinate(coord.s, r, coord.c);
            Attribute attr = pinAttributes[coord.s][r][coord.c];
            
            // Only clear if it's not an IC pin or other special pin
            // Note: You may need to add ICPinManager check here if available
            if (attr.link == -1) {
                attr.value = -1;
            }
        }
    }

    public void loadComponentsFromDatabase() {
        try {
            System.out.println("=== LOADING COMPONENTS FROM DATABASE START ===");
            System.out.println("Loading for Username: " + currentUsername + ", Circuit: " + currentCircuitName);

            List<ComponentData> dbComponents = componentToDB.getComponentsForCircuit(currentUsername, currentCircuitName);
            System.out.println("Database returned " + dbComponents.size() + " components");

            // Ensure we start with clean state
            if (!vccPins.isEmpty() || !gndPins.isEmpty() || !componentStates.isEmpty()) {
                System.out.println("Warning: Loading components but memory isn't clean. Clearing first.");
                clearInMemoryComponentData();
            }

            for (ComponentData componentData : dbComponents) {
                // Additional safety check: only load components that match the current circuit name and username
                if (!componentData.circuitName.equals(currentCircuitName) || !componentData.username.equals(currentUsername)) {
                    System.out.println("Skipping component - belongs to different circuit/user: " +
                            componentData.circuitName + "/" + componentData.username);
                    continue;
                }

                // Translate database coordinates to Coordinate object
                Coordinate coord = new Coordinate(componentData.section, componentData.row_pos, componentData.column_pos);
                System.out.println("Processing component at coordinate " + coord + " with type " + componentData.value);

                // Load the component using the new loadComponent method (doesn't save to DB)
                loadComponent(coord, componentData.value);

                System.out.println("Loaded component from database at " + coord + " for circuit " + currentCircuitName);
            }

            System.out.println("Final vccPins list size: " + vccPins.size());
            System.out.println("Final gndPins list size: " + gndPins.size());
            System.out.println("Final componentStates map size: " + componentStates.size());

            System.out.println("Loaded " + dbComponents.size() + " components from database for circuit " + currentCircuitName);
            System.out.println("=== LOADING COMPONENTS FROM DATABASE END ===");

        } catch (Exception e) {
            System.err.println("Error loading components from database for circuit " + currentCircuitName + ": " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void clearComponentsFromDatabase() {
        boolean result = componentToDB.clearComponentsForCircuit(currentUsername, currentCircuitName);
        if (result) {
            System.out.println("Cleared all components from database for circuit " + currentCircuitName);
        } else {
            System.err.println("Failed to clear components from database for circuit " + currentCircuitName);
        }
    }

    public void updateCircuitContext(String username, String circuitName) {
        System.out.println("ComponentManager: Updating circuit context from [" + currentUsername + ", " + currentCircuitName +
                "] to [" + username + ", " + circuitName + "]");

        // Enhanced logic to detect when we need to clear data
        boolean isActualSwitch = !username.equals(currentUsername) || !circuitName.equals(currentCircuitName);
        boolean isReturningToDifferentCircuit = (previousUsername != null && previousCircuitName != null) &&
                (!username.equals(previousUsername) || !circuitName.equals(previousCircuitName));
        boolean shouldClearData = isActualSwitch || forceNextClear || isReturningToDifferentCircuit;

        System.out.println("Context switch analysis:");
        System.out.println("- isActualSwitch: " + isActualSwitch);
        System.out.println("- isReturningToDifferentCircuit: " + isReturningToDifferentCircuit);
        System.out.println("- forceNextClear: " + forceNextClear);
        System.out.println("- shouldClearData: " + shouldClearData);

        if (shouldClearData) {
            previousUsername = currentUsername;
            previousCircuitName = currentCircuitName;

            clearInMemoryComponentData();
            clearComponentVisuals();

            System.out.println("Cleared data for circuit context change");

            forceNextClear = false;
        } else {
            System.out.println("No data clearing needed for this context update");
        }

        // Update context
        this.currentUsername = username;
        this.currentCircuitName = circuitName;

        System.out.println("ComponentManager context updated successfully - Username: " + username + ", Circuit: " + circuitName);
    }

    public void clearInMemoryComponentData() {
        System.out.println("=== CLEARING IN-MEMORY COMPONENT DATA START ===");
        System.out.println("Before clearing:");
        System.out.println("- vccPins.size(): " + vccPins.size());
        System.out.println("- gndPins.size(): " + gndPins.size());
        System.out.println("- componentStates.size(): " + componentStates.size());

        // Clear the component coordinate lists
        vccPins.clear();
        gndPins.clear();

        // Clear the component state mappings
        componentStates.clear();

        System.out.println("After clearing:");
        System.out.println("- vccPins.size(): " + vccPins.size());
        System.out.println("- gndPins.size(): " + gndPins.size());
        System.out.println("- componentStates.size(): " + componentStates.size());

        System.out.println("In-memory component data cleared successfully");
        System.out.println("=== CLEARING IN-MEMORY COMPONENT DATA END ===");
    }

    public void clearComponentVisuals() {
        System.out.println("Clearing component visual elements from breadboard");

        // Reset VCC pins and their attributes
        for (Coordinate coord : new ArrayList<>(vccPins)) { // Create copy to avoid concurrent modification
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
                System.err.println("Error clearing visual for VCC at " + coord + ": " + e.getMessage());
            }
        }

        // Reset GND pins and their attributes
        for (Coordinate coord : new ArrayList<>(gndPins)) { // Create copy to avoid concurrent modification
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
                System.err.println("Error clearing visual for GND at " + coord + ": " + e.getMessage());
            }
        }

        System.out.println("Component visual elements cleared successfully");
    }

    /**
     * Check if a coordinate is a VCC pin
     */
    public boolean isVCC(Coordinate coord) {
        return vccPins.contains(coord);
    }

    /**
     * Check if a coordinate is a GND pin
     */
    public boolean isGND(Coordinate coord) {
        return gndPins.contains(coord);
    }

    /**
     * Check if a coordinate is any power component (VCC or GND)
     */
    public boolean isComponent(Coordinate coord) {
        return isVCC(coord) || isGND(coord);
    }

    /**
     * Get all VCC coordinates
     */
    public List<Coordinate> getAllVCC() {
        return vccPins;
    }

    /**
     * Get all GND coordinates
     */
    public List<Coordinate> getAllGND() {
        return gndPins;
    }

    /**
     * Get component type at coordinate (returns ComponentToDB.VCC, ComponentToDB.GND, or null)
     */
    public Integer getComponentType(Coordinate coord) {
        return componentStates.get(coord);
    }

    private void showToast(String message) {
        Toast.makeText(mainActivity, message, Toast.LENGTH_SHORT).show();
    }
}