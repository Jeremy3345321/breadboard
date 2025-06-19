package com.example.breadboard;

import android.app.AlertDialog;
import android.graphics.Color;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.widget.Button;
import android.widget.RelativeLayout;
import android.widget.Toast;

import com.example.breadboard.logic.AND;
import com.example.breadboard.logic.ICGate;
import com.example.breadboard.logic.ICGateInfo;
import com.example.breadboard.logic.NAND;
import com.example.breadboard.logic.NOR;
import com.example.breadboard.logic.NOT;
import com.example.breadboard.logic.OR;
import com.example.breadboard.logic.XOR;
import com.example.breadboard.model.Attribute;
import com.example.breadboard.model.Coordinate;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ICSetup {
    private MainActivity mainActivity;
    private AddConnection addConnection;
    private RelativeLayout icContainer;
    private Attribute[][][] pinAttributes;
    private List<Button> icGates;
    private List<Object> gates;
    private List<Coordinate> inputs;
    private List<Coordinate> outputs;
    private List<Coordinate> vccPins;
    private List<Coordinate> gndPins;
    private List<ICGateInfo> icGateObjects;
    private ICToDB icToDB;
    private RelativeLayout breadboardParentLayout;
    private static boolean canDisplay = false;

    private String currentUsername;
    private String currentCircuitName;
    private String previousUsername;
    private String previousCircuitName;
    private boolean isInitialized = false;
    private boolean forceNextClear = false;
    private boolean isLoadingFromDatabase = false;

    private boolean isCurrentlyLoading = false;
    private boolean hasLoadedForCurrentContext = false;

    // Constants
    private static final int ROWS = 5;
    private static final int COLS = 64;

    public ICSetup(MainActivity mainActivity, RelativeLayout icContainer,
                   Attribute[][][] pinAttributes, List<Button> icGates,
                   List<Object> gates, List<Coordinate> inputs,
                   List<Coordinate> outputs, List<Coordinate> vccPins,
                   List<Coordinate> gndPins, List<ICGateInfo> icGateObjects,
                   AddConnection addConnection) {
        this.mainActivity = mainActivity;
        this.icContainer = icContainer;
        this.pinAttributes = pinAttributes;
        this.icGates = icGates;
        this.gates = gates;
        this.inputs = inputs;
        this.outputs = outputs;
        this.vccPins = vccPins;
        this.gndPins = gndPins;
        this.icGateObjects = icGateObjects;
        this.addConnection = addConnection;
        this.icToDB = new ICToDB(mainActivity);

        clearInMemoryICData();
    }

    public void showICSelectionDialog(Coordinate coord) {
        String[] icTypes = {"AND", "OR", "NOT", "NAND", "NOR", "XOR"};

        AlertDialog.Builder builder = new AlertDialog.Builder(mainActivity);
        builder.setTitle("Select IC Type")
                .setItems(icTypes, (dialog, which) -> {
                    String icType = icTypes[which];
                    addICGate(coord, icType);
                })
                .show();
    }

    public void loadICGate(String icType, Coordinate coord) {
        Button icButton = new Button(mainActivity);
        icButton.setText(icType);
        icButton.setBackgroundResource(R.drawable.breadboard_ic);
        icButton.setTextColor(Color.WHITE);
        icButton.setTextSize(12);

        int pinSize = mainActivity.getResources().getDimensionPixelSize(R.dimen.pin_size);
        int icWidth = pinSize * 7 - 19;
        int icHeight = mainActivity.getResources().getDimensionPixelSize(R.dimen.ic_height);

        RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(icWidth, icHeight);

        int rowLabelWidth = pinSize;
        int pinMarginLeft = 1;
        int pinMarginLeftPx = Math.round(pinMarginLeft * mainActivity.getResources().getDisplayMetrics().density);

        int scrollPadding = Math.round(5 * mainActivity.getResources().getDisplayMetrics().density);
        int gridPadding = Math.round(4 * mainActivity.getResources().getDisplayMetrics().density);

        params.leftMargin = mainActivity.contMargin + rowLabelWidth + pinMarginLeftPx + (coord.c * (pinSize + (pinMarginLeftPx * 2))
                + gridPadding + scrollPadding);

        params.addRule(RelativeLayout.CENTER_VERTICAL);

        icButton.setLayoutParams(params);

        // Create the actual ICGate logic object based on type
        ICGate gateLogic = createICGateLogic(icType, coord);
        if (gateLogic != null) {
            gateLogic.init();
            gates.add(gateLogic);
        } else {
            Toast.makeText(mainActivity, "Failed to create " + icType + " gate", Toast.LENGTH_SHORT).show();
            return;
        }

        if (breadboardParentLayout == null) {
            breadboardParentLayout = (RelativeLayout) icContainer.getParent();
        }
        breadboardParentLayout.addView(icButton);
        System.out.println("Added IC button to stored parent layout");

        // Create ICGateInfo object for UI management
        ICGateInfo icGateInfo = new ICGateInfo(icType, coord, icButton, gateLogic);
        icGateObjects.add(icGateInfo);

        System.out.println("=== LOADED IC DEBUG INFO ===");
        System.out.println("Loaded IC Type: " + icType);
        System.out.println("Loaded IC Coordinate: " + coord);
        System.out.println("Coordinate details - s:" + coord.s + ", r:" + coord.r + ", c:" + coord.c);
        System.out.println("Total ICs in memory after loading: " + icGateObjects.size());
        System.out.println("============================");


        // Modified click listener to show IC details
        icButton.setOnClickListener(v -> showICConnectionDialog(icGateInfo));

        icGates.add(icButton);

        // Mark IC pins as occupied
        markICPins(coord, icType);

        System.out.println("Loaded IC " + icType + " at coordinate " + coord + " from database");

        int addedPadding = 0;
        if (mainActivity.extraPadding != 0) {
            addedPadding = 1;
        }
        if (mainActivity.ICnum == 0) {
            mainActivity.contMargin = -7;
            mainActivity.ICnum += 1;
        } else if (mainActivity.ICnum == 1) {
            mainActivity.contMargin = -20;
            mainActivity.ICnum += 1;
        } else if (mainActivity.ICnum == 2) {
            mainActivity.contMargin = -34;
            mainActivity.ICnum += 1;
        } else if (mainActivity.ICnum == 3) {
            mainActivity.contMargin = -48;
            mainActivity.ICnum += 1;
        } else if (mainActivity.ICnum == 4) {
            mainActivity.contMargin = -63;
            mainActivity.ICnum += 1;
        } else if (mainActivity.ICnum == 5) {
            mainActivity.contMargin = -76;
            mainActivity.ICnum += 1;
        } else if (mainActivity.ICnum == 6) {
            mainActivity.contMargin = -90;
            mainActivity.ICnum += 1;
        } else if (mainActivity.ICnum == 7) {
            mainActivity.contMargin = -103;
            mainActivity.ICnum += 1;
        }
    }

    public void addICGate(Coordinate coord, String icType) {
        Button icButton = new Button(mainActivity);
        icButton.setText(icType);
        icButton.setBackgroundResource(R.drawable.breadboard_ic);
        icButton.setTextColor(Color.WHITE);
        icButton.setTextSize(12);

        int pinSize = mainActivity.getResources().getDimensionPixelSize(R.dimen.pin_size);
        int icWidth = pinSize * 7 - 19;
        int icHeight = mainActivity.getResources().getDimensionPixelSize(R.dimen.ic_height);

        RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(icWidth, icHeight);

        int rowLabelWidth = pinSize;
        int pinMarginLeft = 1;
        int pinMarginLeftPx = Math.round(pinMarginLeft * mainActivity.getResources().getDisplayMetrics().density);

        int scrollPadding = Math.round(5 * mainActivity.getResources().getDisplayMetrics().density);
        int gridPadding = Math.round(4 * mainActivity.getResources().getDisplayMetrics().density);

        params.leftMargin = mainActivity.contMargin + rowLabelWidth + pinMarginLeftPx + (coord.c * (pinSize + (pinMarginLeftPx * 2))
                + gridPadding + scrollPadding);

        params.addRule(RelativeLayout.CENTER_VERTICAL);

        icButton.setLayoutParams(params);

        // Create the actual ICGate logic object based on type
        ICGate gateLogic = createICGateLogic(icType, coord);
        if (gateLogic != null) {
            gateLogic.init();
            gates.add(gateLogic);
        } else {
            Toast.makeText(mainActivity, "Failed to create " + icType + " gate", Toast.LENGTH_SHORT).show();
            return;
        }

        if (breadboardParentLayout == null) {
            breadboardParentLayout = (RelativeLayout) icContainer.getParent();
        }
        breadboardParentLayout.addView(icButton);
        System.out.println("Added IC button to stored parent layout");

        // Create ICGateInfo object for UI management
        ICGateInfo icGateInfo = new ICGateInfo(icType, coord, icButton, gateLogic);
        icGateObjects.add(icGateInfo);

        icButton.setOnClickListener(v -> showICConnectionDialog(icGateInfo));

        icGates.add(icButton);

        // Mark IC pins as occupied
        markICPins(coord, icType);

        int addedPadding = 0;
        if (mainActivity.extraPadding != 0) {
            addedPadding = 1;
        }
        if (mainActivity.ICnum == 0) {
            mainActivity.contMargin = -7;
            mainActivity.ICnum += 1;
        } else if (mainActivity.ICnum == 1) {
            mainActivity.contMargin = -20;
            mainActivity.ICnum += 1;
        } else if (mainActivity.ICnum == 2) {
            mainActivity.contMargin = -34;
            mainActivity.ICnum += 1;
        } else if (mainActivity.ICnum == 3) {
            mainActivity.contMargin = -48;
            mainActivity.ICnum += 1;
        } else if (mainActivity.ICnum == 4) {
            mainActivity.contMargin = -63;
            mainActivity.ICnum += 1;
        } else if (mainActivity.ICnum == 5) {
            mainActivity.contMargin = -76;
            mainActivity.ICnum += 1;
        } else if (mainActivity.ICnum == 6) {
            mainActivity.contMargin = -90;
            mainActivity.ICnum += 1;
        } else if (mainActivity.ICnum == 7) {
            mainActivity.contMargin = -103;
            mainActivity.ICnum += 1;
        }

        boolean saved = icToDB.insertIC(icType, mainActivity.currentUsername,
                mainActivity.currentCircuitName, coord);
        if (!saved) {
            Toast.makeText(mainActivity, "Failed to save " + icType + " to database",
                    Toast.LENGTH_SHORT).show();
        }
    }

    public void markICPins(Coordinate coord, String icType) {
        // Mark pins for a 14-pin DIP IC
        // Bottom row (section 1, row 0): pins 1-7
        System.out.println("Mark IC Pins complete");
        for (int i = 0; i < 7; i++) {
            if (coord.c + i < COLS) {
                Coordinate bottomPin = new Coordinate(1, 0, coord.c + i); // Section 1, Row 0 (F)
                pinAttributes[1][0][coord.c + i] = new Attribute(-3, -3); // Special IC marker
            }
        }
        // Top row (section 0, row 4): pins 8-14 (reversed order)
        for (int i = 0; i < 7; i++) {
            if (coord.c + (6 - i) < COLS) {
                Coordinate topPin = new Coordinate(0, 4, coord.c + (6 - i)); // Section 0, Row 4 (E)
                pinAttributes[0][4][coord.c + (6 - i)] = new Attribute(-3, -3); // Special IC marker
            }
        }
    }

    public void executeCircuit() {
        System.out.println("Circuit executed");
        for (Coordinate coord : inputs) {
            // Keep existing input values - they should be set by the InputManager
            // Don't override them here
        }

        // Execute gate logic first
        executeGates();

        // Then calculate output values after IC execution
        for (Coordinate coord : outputs) {
            // Let the OutputManager handle this during updateOutputDisplay()
            // Don't set output values here as it interferes with IC outputs
        }

        // Let MainActivity handle output updates
        mainActivity.updateOutputDisplay();
    }

    private void executeGates() {
        try {
            if (gates == null || gates.isEmpty()) {
                System.out.println("No gates to execute");
                return;
            }

            if (mainActivity == null || mainActivity.getICPinManager() == null) {
                System.out.println("MainActivity or ICPinManager is null");
                canDisplay = false;
                return;
            }
            for (Object gateObj : gates) {
                if (gateObj instanceof ICGate) {
                    ICGate gate = (ICGate) gateObj;

                    // Get input values for this gate based on its pin connections
                    int[][] gateInputs = getGateInputs(gate);

                    // Execute the gate logic based on its type
                    if (gate instanceof AND) {
                        int[] outputs = ((AND) gate).executeAllGates(gateInputs);
                        setGateOutputs(gate, outputs);
                    } else if (gate instanceof OR) {
                        int[] outputs = ((OR) gate).executeAllGates(gateInputs);
                        setGateOutputs(gate, outputs);
                    } else if (gate instanceof NOT) {
                        // NOT gates have single inputs
                        int[] singleInputs = new int[gateInputs.length];
                        for (int i = 0; i < gateInputs.length; i++) {
                            singleInputs[i] = gateInputs[i].length > 0 ? gateInputs[i][0] : 0;
                        }
                        int[] outputs = ((NOT) gate).executeAllGates(singleInputs);
                        setGateOutputs(gate, outputs);
                    } else if (gate instanceof NAND) {
                        int[] outputs = ((NAND) gate).executeAllGates(gateInputs);
                        setGateOutputs(gate, outputs);
                    } else if (gate instanceof NOR) {
                        int[] outputs = ((NOR) gate).executeAllGates(gateInputs);
                        setGateOutputs(gate, outputs);
                    } else if (gate instanceof XOR) {
                        int[] outputs = ((XOR) gate).executeAllGates(gateInputs);
                        setGateOutputs(gate, outputs);
                    }
                }
            }
            // REMOVED: updateOutputPins() call - handled by MainActivity

        } catch (Exception e) {
            // Use MainActivity's showToast method
            mainActivity.runOnUiThread(() ->
                    Toast.makeText(mainActivity, "Error executing circuit: " + e.getMessage(), Toast.LENGTH_SHORT).show()
            );
            System.out.println("Error executing circuit" + e);
            canDisplay = false;
        }
    }

    private int[][] getGateInputs(ICGate gate) {
        // Get the number of gates in this IC type
        int numGates = getNumGatesInIC(gate.type);
        int[][] inputs = new int[numGates][];

        // Handle different gate types
        if (gate.type.equals("NOT")) {
            // NOT gates have single inputs
            for (int i = 0; i < numGates; i++) {
                inputs[i] = new int[1];
                int[] gateInputs = getGateInputsForSpecificGate(gate, i);
                inputs[i][0] = gateInputs.length > 0 ? gateInputs[0] : 0;
            }
        } else {
            for (int i = 0; i < numGates; i++) {
                inputs[i] = getGateInputsForSpecificGate(gate, i);
            }
        }
        System.out.println("getGateInputs" + numGates + " " + Arrays.deepToString(inputs));
        return inputs;
    }

    private int[] getGateInputsForSpecificGate(ICGate gate, int gateNumber) {
        // Get input pins for the specific gate number within the IC
        int[] inputPins = getInputPinsForGate(gate.type, gateNumber);
        int[] inputs = new int[inputPins.length];

        for (int i = 0; i < inputPins.length; i++) {
            Coordinate pinCoord = getPhysicalPinCoordinate(inputPins[i], gate.position);
            if (pinCoord != null) {
                inputs[i] = mainActivity.getICPinManager().getPinValue(pinCoord);
                System.out.println("Gate " + gateNumber + ", Pin " + inputPins[i] + " -> Coord: " + pinCoord + " = " + inputs[i]);
            } else {
                inputs[i] = 0; // Default to 0 if pin coordinate is invalid
                System.out.println("Invalid pin coordinate for logical pin: " + inputPins[i]);
            }
        }
        System.out.println("getGateInputsForSpecificGate Gate#" + gateNumber + " inputPins: " + Arrays.toString(inputPins) + " inputs: " + Arrays.toString(inputs));
        return inputs;
    }

    private int[] getInputPinsForGate(String icType, int gateNumber) {
        switch (icType.toUpperCase()) {
            case "AND":
            case "NAND":
                switch (gateNumber) {
                    case 0: return new int[]{1, 2};   // Gate 1: pins 1,2 -> output 3
                    case 1: return new int[]{4, 5};   // Gate 2: pins 4,5 -> output 6
                    case 2: return new int[]{8, 9};  // Gate 3: pins 9,10 -> output 8
                    case 3: return new int[]{11, 12}; // Gate 4: pins 12,13 -> output 11
                    default: return new int[]{0, 0};
                }
            case "OR":
            case "NOR":
            case "XOR":
                switch (gateNumber) {
                    case 0: return new int[]{1, 2};   // Gate 1: pins 1,2 -> output 3
                    case 1: return new int[]{4, 5};   // Gate 2: pins 4,5 -> output 6
                    case 2: return new int[]{8, 9};  // Gate 3: pins 8,9 -> output 10
                    case 3: return new int[]{11, 12}; // Gate 4: pins 11,12 -> output 13
                    default: return new int[]{0, 0};
                }
            case "NOT":
                switch (gateNumber) {
                    case 0: return new int[]{1};   // Gate 1: pin 1 -> output 2
                    case 1: return new int[]{3};   // Gate 2: pin 3 -> output 4
                    case 2: return new int[]{5};   // Gate 3: pin 5 -> output 6
                    case 3: return new int[]{8};   // Gate 4: pin 8 -> output 9
                    case 4: return new int[]{10};  // Gate 5: pin 10 -> output 11
                    case 5: return new int[]{12};  // Gate 6: pin 12 -> output 13
                    default: return new int[]{0};
                }
            default:
                return new int[]{0, 0};
        }
    }

    private void setGateOutputs(ICGate gate, int[] outputs) {
        // Set the output values to the appropriate pins based on the gate's pin configuration
        for (int i = 0; i < outputs.length; i++) {
            int outputPin = getOutputPinForGate(gate.type, i);
            if (outputPin != -1) {
                Coordinate outputCoord = getPhysicalPinCoordinate(outputPin, gate.position);
                if (outputCoord != null) {
                    // Use ICPinManager to set the IC pin value
                    mainActivity.getICPinManager().setICPinValue(outputCoord, outputs[i]);

                    // Also update the pin attributes directly for consistency
                    setColumnValue(outputCoord, outputs[i]);
                    System.out.println("setGateOutputs, outputCoord: " + outputCoord);
                    System.out.println("setGateOutputs, int[] outputs: " + Arrays.toString(outputs));
                }
            }
        }
    }

    private void setColumnValue(Coordinate pinCoord, int value) {
        System.out.println("setColumnValue, value: " + value + " pinCoord: " + pinCoord.toString());
        for (int r = 0; r < ROWS; r++) {
            if (pinCoord.c < COLS) {
                Attribute attr = pinAttributes[pinCoord.s][r][pinCoord.c];
                // Set value for connected pins (link != -1) or output pins (value == 2)
                // Skip IC marker pins (value == -3)
                if ((attr.link != -1 || attr.value == 2) && attr.value != -3) {
                    attr.value = value;
                }
            }
        }
    }

    private int getOutputPinForGate(String icType, int gateNumber) {
        switch (icType.toUpperCase()) {
            case "AND":
            case "NAND":
            case "OR":
            case "NOR":
            case "XOR":
                int[] outputPins = {3, 6, 10, 13}; // Standard 4-gate IC output pins
                return (gateNumber >= 0 && gateNumber < outputPins.length) ? outputPins[gateNumber] : -1;
            case "NOT":
                int[] notOutputPins = {2, 4, 6, 9, 11, 13}; // 6 NOT gates output pins
                return (gateNumber >= 0 && gateNumber < notOutputPins.length) ? notOutputPins[gateNumber] : -1;
            default:
                return -1;
        }
    }

    private Coordinate getPhysicalPinCoordinate(int logicalPin, Coordinate icPosition) {
        // Convert logical pin number (1-14) to physical breadboard coordinate
        if (logicalPin >= 1 && logicalPin <= 7) {
            // Pins 1-7 are on bottom row (section 1, row 0)
            return new Coordinate(1, 0, icPosition.c + (logicalPin - 1));
        } else if (logicalPin >= 8 && logicalPin <= 14) {
            // Pins 8-14 are on top row (section 0, row 4) in reverse order
            return new Coordinate(0, 4, icPosition.c + (14 - logicalPin));
        }
        System.out.println("Invalid logical pin: " + logicalPin);
        return null;
    }

    private int getNumGatesInIC(String icType) {
        switch (icType.toUpperCase()) {
            case "NOT":
                return 6; // 7404 has 6 NOT gates
            case "AND":
            case "OR":
            case "NAND":
            case "NOR":
            case "XOR":
                return 4; // Most have 4 gates
            default:
                return 1;
        }
    }

    private ICGate createICGateLogic(String icType, Coordinate coord) {
        switch (icType.toUpperCase()) {
            case "AND":
                return new AND(coord, null, mainActivity);
            case "OR":
                return new OR(coord, null, mainActivity);
            case "NOT":
                return new NOT(coord, null, mainActivity);
            case "NAND":
                return new NAND(coord, null, mainActivity);
            case "NOR":
                return new NOR(coord, null, mainActivity);
            case "XOR":
                return new XOR(coord, null, mainActivity);
            default:
                return null;
        }
    }

    private void showICConnectionDialog(ICGateInfo icGate) {
        StringBuilder connectionInfo = new StringBuilder();
        connectionInfo.append(icGate.type).append(" GATE\n\n");

        // Get pin connections for this IC
        String[] pinConnections = getICPinConnections(icGate);

        // Format the display in two columns
        for (int i = 0; i < 7; i++) {
            int leftPin = i + 1;
            int rightPin = 14 - i;

            String leftConnection = pinConnections[i];
            String rightConnection = pinConnections[13 - i];

            connectionInfo.append(String.format("%-2d - %-8s    %2d - %s\n",
                    leftPin, leftConnection, rightPin, rightConnection));
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(mainActivity);
        builder.setTitle("IC Pin Connections")
                .setMessage(connectionInfo.toString())
                .setPositiveButton("Remove", (dialog, which) -> removeICByCoord(icGate.position))
                .setNegativeButton("Close", null)
                .show();

    }

    public void debugICGateObjects() {
        System.out.println("=== DEBUG: All ICs in icGateObjects ===");
        System.out.println("Total count: " + icGateObjects.size());
        for (int i = 0; i < icGateObjects.size(); i++) {
            ICGateInfo icInfo = icGateObjects.get(i);
            System.out.println(i + ". " + icInfo.type + " at {" +
                    icInfo.position.s + ", " + icInfo.position.r + ", " + icInfo.position.c + "}");
        }
        System.out.println("=====================================");
    }

    public void removeICByCoord(Coordinate coord) {
        System.out.println("=== REMOVE IC DEBUG INFO ===");
        System.out.println("Attempting to remove IC at coordinate: " + coord);
        System.out.println("Looking for - s:" + coord.s + ", r:" + coord.r + ", c:" + coord.c);
        System.out.println("Total ICs in memory: " + icGateObjects.size());

        // FIXED: Find ALL ICs at this coordinate (handle duplicates)
        List<ICGateInfo> icsToRemove = new ArrayList<>();

        for (ICGateInfo icInfo : icGateObjects) {
            System.out.println("Checking IC: " + icInfo.type +
                    " at s:" + icInfo.position.s +
                    ", r:" + icInfo.position.r +
                    ", c:" + icInfo.position.c);

            if (isCoordinateWithinIC(coord, icInfo.position)) {
                icsToRemove.add(icInfo);
                System.out.println("✅ MATCH FOUND! IC spans from column " + icInfo.position.c +
                        " to " + (icInfo.position.c + 6));
            }
        }

        System.out.println("Found " + icsToRemove.size() + " ICs to remove");
        System.out.println("============================");

        if (icsToRemove.isEmpty()) {
            System.err.println("❌ No IC found at coordinate: " + coord);
            debugICGateObjects();
            return;
        }

        // FIXED: Remove ALL duplicate ICs
        for (ICGateInfo icToRemove : icsToRemove) {
            System.out.println("Removing IC: " + icToRemove.type + " at " + icToRemove.position);

            // Remove from in-memory collections
            icGateObjects.remove(icToRemove);
            if (icToRemove.button != null) {
                icGates.remove(icToRemove.button);
            }
            if (icToRemove.gateLogic != null) {
                gates.remove(icToRemove.gateLogic);
            }

            // Remove visual button (only once)
            if (icToRemove.button != null && icsToRemove.indexOf(icToRemove) == 0) {
                try {
                    if (breadboardParentLayout == null) {
                        breadboardParentLayout = (RelativeLayout) icContainer.getParent();
                    }
                    if (breadboardParentLayout != null) {
                        breadboardParentLayout.removeView(icToRemove.button);
                        System.out.println("Removed button from breadboard parent layout");
                        breadboardParentLayout.invalidate();
                        breadboardParentLayout.requestLayout();
                    }
                } catch (Exception e) {
                    System.err.println("Error removing button: " + e.getMessage());
                }
            }
        }

        // Reset pin attributes and database (only once)
        ICGateInfo firstIC = icsToRemove.get(0);
        resetICPinAttributes(firstIC.position);

        // Remove from database
        boolean dbResult = icToDB.deleteICByCoordinate(currentUsername, currentCircuitName, firstIC.position);
        if (dbResult) {
            System.out.println("Removed IC from database at " + firstIC.position);
        } else {
            System.err.println("Failed to remove IC from database");
        }

        // Update IC counter
        mainActivity.ICnum = Math.max(0, mainActivity.ICnum - 1);

        System.out.println("Successfully removed " + icsToRemove.size() + " IC objects");
        debugICGateObjects();
    }

    // Helper method to check if a coordinate falls within an IC's pin range
    private boolean isCoordinateWithinIC(Coordinate clickedCoord, Coordinate icBasePosition) {
        // An IC spans 7 columns starting from its base position
        int icStartCol = icBasePosition.c;
        int icEndCol = icBasePosition.c + 6;

        // Check if the clicked coordinate is within the IC's column range
        boolean withinColumnRange = clickedCoord.c >= icStartCol && clickedCoord.c <= icEndCol;

        // Check if it's on the correct rows (top row: section 0, row 4 or bottom row: section 1, row 0)
        boolean onTopRow = (clickedCoord.s == 0 && clickedCoord.r == 4);
        boolean onBottomRow = (clickedCoord.s == 1 && clickedCoord.r == 0);

        return withinColumnRange && (onTopRow || onBottomRow);
    }

    private String[] getICPinConnections(ICGateInfo icGate) {
        String[] connections = new String[14];
        Coordinate icPos = icGate.position;

        // Check pins 1-7 (bottom row, section 1, row 0)
        for (int i = 0; i < 7; i++) {
            int col = icPos.c + i;
            if (col < COLS) {
                connections[i] = getPinConnectionType(1, 0, col);
            } else {
                connections[i] = "NC"; // Not Connected
            }
        }

        // Check pins 8-14 (top row, section 0, row 4) - reverse order
        for (int i = 0; i < 7; i++) {
            int col = icPos.c + (6 - i);
            if (col < COLS) {
                connections[7 + i] = getPinConnectionType(0, 4, col);
            } else {
                connections[7 + i] = "NC"; // Not Connected
            }
        }

        System.out.println("getICPinConnections: " + Arrays.toString(connections));
        return connections;
    }

    private String getConnectionString (String[] connections) {
        return connections[0];
    }
    private String getPinConnectionType(int section, int row, int col) {
        // Check the entire column for connections (breadboard columns are connected vertically)
        for (int r = 0; r < ROWS; r++) {
            Attribute attr = pinAttributes[section][r][col];

            // Skip IC marker pins (-3 value)
            if (attr.value == -3) continue;

            // Check if there's a GND connection in this column
            if (gndPins.contains(new Coordinate(section, r, col))) {
                return "GND";
            }

            // Check if there's an input connection in this column
            if (inputs.contains(new Coordinate(section, r, col))) {
                return String.valueOf(attr.value != -1 ? attr.value : 0);
            }

            // Check if there's an output connection in this column
            if (outputs.contains(new Coordinate(section, r, col))) {
                return "OUT";
            }

            // Check for wire connections (but not IC markers)
            if (attr.link != -1 && attr.value != -1 && attr.value != -3) {
                // CHANGED: Don't convert value 1 to "VCC" anymore - show actual numeric value
                if (attr.value == -2) return "GND";
                if (attr.value == 0) return "0";
                return String.valueOf(attr.value);
            }


            // Check if there's a VCC connection in this column
            if (vccPins.contains(new Coordinate(section, r, col))) {
                return "VCC";
            }
        }

        return "NC"; // Not Connected if no actual connection found
    }

    public void loadAllICsFromDatabase() {
        System.out.println("=== LOADING ICs FROM DATABASE START ===");
        System.out.println("Current state - isCurrentlyLoading: " + isCurrentlyLoading +
                ", hasLoadedForCurrentContext: " + hasLoadedForCurrentContext);
        System.out.println("Loading ICs for: " + currentUsername + " / " + currentCircuitName);
        System.out.println("Current ICs in memory: " + icGateObjects.size());

        // Prevent multiple simultaneous loads
        if (isCurrentlyLoading) {
            System.out.println("ICSetup: Already loading ICs, skipping duplicate load request");
            return;
        }

        // Check if we've already loaded for this context
        if (hasLoadedForCurrentContext) {
            System.out.println("ICSetup: Already loaded ICs for current context, skipping");
            return;
        }

        isCurrentlyLoading = true;

        try {
            // Get fresh data from database
            List<ICToDB.ICData> databaseICs = icToDB.getICsForCircuit(currentUsername, currentCircuitName);
            System.out.println("Found " + databaseICs.size() + " ICs in database");

            // Load each IC
            for (ICToDB.ICData icData : databaseICs) {
                Coordinate icCoordinate = icData.getCoordinate();
                System.out.println("Loading IC: " + icData.ic_type + " at " + icCoordinate);
                loadICGate(icData.ic_type, icCoordinate);
            }

            hasLoadedForCurrentContext = true;
            System.out.println("Successfully loaded " + databaseICs.size() + " ICs from database");
            System.out.println("Final ICs in memory: " + icGateObjects.size());

        } catch (Exception e) {
            System.err.println("Error loading ICs from database: " + e.getMessage());
            e.printStackTrace();
        } finally {
            isCurrentlyLoading = false;
            System.out.println("=== LOADING ICs FROM DATABASE END ===");
        }
    }

    public void removeIC() {
        clearInMemoryICData();
        clearICVisuals();
        clearICsFromDatabase();
        mainActivity.ICnum = 0; // Reset IC counter
        System.out.println("All ICs removed and ICnum reset to 0");
    }

    private boolean isICAlreadyLoaded(Coordinate coordinate) {
        for (ICGateInfo icGateInfo : icGateObjects) {
            if (icGateInfo.position.equals(coordinate)) {
                return true;
            }
        }
        return false;
    }

    private void clearInMemoryICData() {
        System.out.println("=== CLEARING IN-MEMORY IC DATA START ===");
        System.out.println("Before clearing:");
        System.out.println("- icGateObjects.size(): " + icGateObjects.size());
        System.out.println("- icGates.size(): " + icGates.size());
        System.out.println("- gates.size(): " + gates.size());

        // Print current ICs before clearing
        for (int i = 0; i < icGateObjects.size(); i++) {
            ICGateInfo icInfo = icGateObjects.get(i);
            System.out.println("  - IC[" + i + "]: " + icInfo.type + " at " + icInfo.position);
        }

        // FIXED: Clear lists in correct order
        icGateObjects.clear();
        icGates.clear();
        gates.clear();

        System.out.println("Cleared all in-memory IC data");
        System.out.println("After clearing:");
        System.out.println("- icGateObjects.size(): " + icGateObjects.size());
        System.out.println("- icGates.size(): " + icGates.size());
        System.out.println("- gates.size(): " + gates.size());
        System.out.println("=== CLEARING IN-MEMORY IC DATA END ===");
    }

    private void clearICVisuals() {
        System.out.println("=== CLEARING IC VISUALS START ===");

        // Remove all IC buttons from the parent layout, not just icContainer
        if (breadboardParentLayout == null) {
            breadboardParentLayout = (RelativeLayout) icContainer.getParent();
        }

        // Remove each IC button individually from the parent layout
        for (ICGateInfo icInfo : new ArrayList<>(icGateObjects)) {
            if (icInfo.button != null && breadboardParentLayout != null) {
                try {
                    breadboardParentLayout.removeView(icInfo.button);
                    System.out.println("Removed IC button: " + icInfo.type + " from parent layout");
                } catch (Exception e) {
                    System.err.println("Error removing IC button: " + e.getMessage());
                }
            }
            // Reset pin attributes for this IC
            resetICPinAttributes(icInfo.position);
        }

        // Also clear the icContainer as a safety measure
        icContainer.removeAllViews();

        // Force UI refresh
        if (breadboardParentLayout != null) {
            breadboardParentLayout.invalidate();
            breadboardParentLayout.requestLayout();
        }

        System.out.println("Cleared all IC visuals from breadboard");
        System.out.println("=== CLEARING IC VISUALS END ===");
    }

    private void resetICPinAttributes(Coordinate icPosition) {
        System.out.println("Resetting pin attributes for IC at: " + icPosition);

        // FIX: Use the correct coordinate mapping for your breadboard layout
        // Based on your markICPins method, ICs span across sections 0 and 1
        int startColumn = icPosition.c;

        // Safety check for column bounds
        if (startColumn < 0 || startColumn >= COLS) {
            System.err.println("IC column position out of bounds: " + startColumn);
            return;
        }

        try {
            // Reset bottom row pins (section 1, row 0): pins 1-7
            for (int i = 0; i < 7; i++) {
                int col = startColumn + i;
                if (col < COLS && col >= 0) {
                    if (pinAttributes[1][0][col] != null) {
                        pinAttributes[1][0][col] = new Attribute(-1, -1);
                        System.out.println("Reset bottom pin at column " + col);
                    }
                }
            }

            // Reset top row pins (section 0, row 4): pins 8-14 (reversed order)
            for (int i = 0; i < 7; i++) {
                int col = startColumn + (6 - i);
                if (col < COLS && col >= 0) {
                    if (pinAttributes[0][4][col] != null) {
                        pinAttributes[0][4][col] = new Attribute(-1, -1);
                        System.out.println("Reset top pin at column " + col);
                    }
                }
            }

            System.out.println("Successfully reset pin attributes for IC at " + icPosition);

        } catch (ArrayIndexOutOfBoundsException e) {
            System.err.println("Array bounds error while resetting IC pins: " + e.getMessage());
            System.err.println("IC position: " + icPosition + ", Array dimensions: " +
                    pinAttributes.length + "x" + pinAttributes[0].length + "x" + pinAttributes[0][0].length);
        }
    }

    public void forceUIRefresh() {
        mainActivity.runOnUiThread(() -> {
            // Force a layout refresh
            RelativeLayout parentLayout = (RelativeLayout) icContainer.getParent();
            if (parentLayout != null) {
                parentLayout.requestLayout();
                parentLayout.invalidate();
            }
            icContainer.requestLayout();
            icContainer.invalidate();

            System.out.println("Forced UI refresh after IC removal");
        });
    }


    public void clearICsFromDatabase() {
        boolean result = icToDB.clearICsForCircuit(currentUsername, currentCircuitName);
        if (result) {
            System.out.println("Cleared all ICs from database for circuit " + currentCircuitName);
        } else {
            System.err.println("Failed to clear ICs from database for circuit " + currentCircuitName);
        }
    }

    public void setLoadingFromDatabase(boolean loading) {
        this.isLoadingFromDatabase = loading;
    }

    public void updateCircuitContext(String username, String circuitName) {
        System.out.println("ICSetup: Updating circuit context from [" + currentUsername + ", " + currentCircuitName +
                "] to [" + username + ", " + circuitName + "]");

        // Skip clearing on the very first context update (initialization)
        if (!isInitialized) {
            System.out.println("First initialization - skipping data clearing");
            this.currentUsername = username;
            this.currentCircuitName = circuitName;
            this.isInitialized = true;
            this.hasLoadedForCurrentContext = false;
            return;
        }

        // FIXED: Always clear when switching to a different context
        boolean isContextChange = !username.equals(currentUsername) || !circuitName.equals(currentCircuitName);

        if (isContextChange || forceNextClear) {
            System.out.println("Context changed - clearing all IC data");

            // Store previous context before clearing
            previousUsername = currentUsername;
            previousCircuitName = currentCircuitName;

            // CRITICAL: Clear everything before context switch
            clearInMemoryICData();
            clearICVisuals();
            mainActivity.ICnum = 0;

            // FIXED: Reset ALL loading flags
            hasLoadedForCurrentContext = false;
            isCurrentlyLoading = false;

            forceNextClear = false;

            System.out.println("Cleared IC data for circuit context change");
        }

        // Update context
        this.currentUsername = username;
        this.currentCircuitName = circuitName;

        System.out.println("ICSetup context updated successfully - Username: " + username + ", Circuit: " + circuitName);
    }

    public void forceCleanState() {
        System.out.println("=== FORCING CLEAN STATE ===");
        isCurrentlyLoading = false;
        hasLoadedForCurrentContext = false;
        clearInMemoryICData();
        clearICVisuals();
        mainActivity.ICnum = 0;
        System.out.println("Clean state forced");
    }
}