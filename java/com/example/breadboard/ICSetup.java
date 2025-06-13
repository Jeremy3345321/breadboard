package com.example.breadboard;

import android.app.AlertDialog;
import android.graphics.Color;
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
import com.example.breadboard.ICPinManager;
import com.example.breadboard.ICPinManager.ICPinInfo;

import java.util.Arrays;
import java.util.List;

public class ICSetup {
    private MainActivity mainActivity;
    private RelativeLayout icContainer;
    private Attribute[][][] pinAttributes;
    private List<Button> icGates;
    private List<Object> gates;
    private List<Coordinate> inputs;
    private List<Coordinate> outputs;
    private List<Coordinate> vccPins;
    private List<Coordinate> gndPins;
    private List<ICGateInfo> icGateObjects;
    private static boolean canDisplay = false;

    // Constants
    private static final int ROWS = 5;
    private static final int COLS = 64;

    public ICSetup(MainActivity mainActivity, RelativeLayout icContainer,
                   Attribute[][][] pinAttributes, List<Button> icGates,
                   List<Object> gates, List<Coordinate> inputs,
                   List<Coordinate> outputs, List<Coordinate> vccPins,
                   List<Coordinate> gndPins, List<ICGateInfo> icGateObjects) {
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
    }

    public void showICSelectionDialog(Coordinate coord) {
        String[] icTypes = {"AND", "OR", "NOT", "NAND", "NOR", "XOR"};

        AlertDialog.Builder builder = new AlertDialog.Builder(mainActivity);
        builder.setTitle("Select IC Type")
                .setItems(icTypes, (dialog, which) -> {
                    String icType = icTypes[which];
                    addICGate(coord, icType); // CHANGED: Delegate to ICSetup
                })
                .show();
    }

    public void addICGate(Coordinate coord, String icType) {
        Button icButton = new Button(mainActivity);
        icButton.setText(icType);
        icButton.setBackgroundResource(R.drawable.breadboard_ic);
        icButton.setTextColor(Color.WHITE);
        icButton.setTextSize(12);

        int pinSize = mainActivity.getResources().getDimensionPixelSize(R.dimen.pin_size);
        int icWidth = pinSize * 7 - 19; // IC spans 7 columns
        int icHeight = mainActivity.getResources().getDimensionPixelSize(R.dimen.ic_height);

        RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(icWidth, icHeight);

        int rowLabelWidth = pinSize;
        int pinMarginLeft = 1; // Pin margins from setupPins()
        int pinMarginLeftPx = Math.round(pinMarginLeft * mainActivity.getResources().getDisplayMetrics().density);

        // Account for scroll view and grid padding
        int scrollPadding = Math.round(5 * mainActivity.getResources().getDisplayMetrics().density);
        int gridPadding = Math.round(4 * mainActivity.getResources().getDisplayMetrics().density);

        // Calculate absolute position
        params.leftMargin = mainActivity.contMargin + rowLabelWidth + pinMarginLeftPx + (coord.c * (pinSize + (pinMarginLeftPx * 2))
                + gridPadding + scrollPadding);

        // Center vertically in the gap
        params.addRule(RelativeLayout.CENTER_VERTICAL);

        icButton.setLayoutParams(params);

        // Create the actual ICGate logic object based on type
        ICGate gateLogic = createICGateLogic(icType, coord);
        if (gateLogic != null) {
            gateLogic.init();
            gates.add(gateLogic);
        } else {
            Toast.makeText(mainActivity, "Failed to create " + icType + " gate", Toast.LENGTH_SHORT).show();
            return; // Don't continue if gate creation failed
        }

        // Create ICGateInfo object for UI management
        ICGateInfo icGateInfo = new ICGateInfo(icType, coord, icButton, gateLogic);
        icGateObjects.add(icGateInfo);

        // Modified click listener to show IC details
        icButton.setOnClickListener(v -> showICConnectionDialog(icGateInfo));

        // Add to the RelativeLayout parent instead of icContainer
        RelativeLayout parentLayout = (RelativeLayout) icContainer.getParent();
        parentLayout.addView(icButton);

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
        // Set VCC pins to high
        for (Coordinate coord : vccPins) {
            mainActivity.addValue(coord, 1);
        }

        // Set input pins - use their current values instead of always setting to 0
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

        if (canDisplay) {
            showGeneralResultDialog();
        }
        // Let MainActivity handle output updates
        mainActivity.updateOutputDisplay();
    }

    private void showGeneralResultDialog() {
        StringBuilder result = new StringBuilder();
        result.append("Circuit Overview:\n\n");

        result.append("Components:\n");
        result.append("• ICs: ").append(icGateObjects.size()).append("\n");
        result.append("• Inputs: ").append(inputs.size()).append("\n");
        result.append("• Outputs: ").append(outputs.size()).append("\n");
        result.append("• VCC pins: ").append(vccPins.size()).append("\n");
        result.append("• GND pins: ").append(gndPins.size()).append("\n\n");

        if (!icGateObjects.isEmpty()) {
            result.append("IC Gates:\n");
            for (int i = 0; i < icGateObjects.size(); i++) {
                ICGateInfo ic = icGateObjects.get(i);
                result.append("• ").append(ic.type).append(" at column ").append(ic.position.c).append("\n");
            }
            result.append("\nTap on any IC to see its pin connections.");
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(mainActivity);
        builder.setTitle("Circuit Simulation")
                .setMessage(result.toString())
                .setPositiveButton("OK", null)
                .show();
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
            // Most other gates have 2 inputs (AND, OR, NAND, NOR, XOR)
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
        System.out.println("getGateInputsForSpecificGate Gate#" + gateNumber + " inputPins: " + java.util.Arrays.toString(inputPins) + " inputs: " + java.util.Arrays.toString(inputs));
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
        // Set the value for all connected pins in the same column
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
                .setPositiveButton("Show General Result", (dialog, which) -> showGeneralResultDialog())
                .setNegativeButton("Close", null)
                .show();

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

        System.out.println("getICPinConnections: " + java.util.Arrays.toString(connections));
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

            // Check if there's a VCC connection in this column
            if (vccPins.contains(new Coordinate(section, r, col))) {
                return "VCC";
            }

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
        }

        return "NC"; // Not Connected if no actual connection found
    }
}