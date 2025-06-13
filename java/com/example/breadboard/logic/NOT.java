package com.example.breadboard.logic;

import android.widget.Button;

import com.example.breadboard.ICPinManager;
import com.example.breadboard.ICSetup;
import com.example.breadboard.MainActivity;
import com.example.breadboard.model.Coordinate;

public class NOT extends ICGate {
    // Pin positioning
    ICSetup icSetup;
    ICPinManager icPinManager;
    MainActivity mainActivity;

    private int[] inputPins = {1, 3, 5, 9, 11, 13}; // Input pins
    private int[] outputPins = {2, 4, 6, 8, 10, 12}; // Output pins
    private int vccPin = 14;
    private int gndPin = 7;

    public NOT(Coordinate position, Button button, MainActivity mainActivity) {
        super("NOT", position, button, mainActivity);
        this.mainActivity = mainActivity;
        // Initialize ICPinManager from MainActivity
        this.icPinManager = mainActivity.getICPinManager();
    }

    @Override
    public void init() {
        // Initialize NOT gate pin connections
        // Request MainActivity to mark the pins and get pin mappings
        if (mainActivity != null) {
            // Register pin functions with MainActivity
            registerPinFunctions();
        }
    }

    private void registerPinFunctions() {
        // Register which physical pins correspond to which logical functions
        for (int pin : inputPins) {
            Coordinate pinCoord = getPhysicalPinCoordinate(pin);
            if (pinCoord != null) {
                icPinManager.registerICPin(pinCoord, "INPUT", this);
            }
        }

        for (int pin : outputPins) {
            Coordinate pinCoord = getPhysicalPinCoordinate(pin);
            if (pinCoord != null) {
                icPinManager.registerICPin(pinCoord, "OUTPUT", this);
            }
        }

        // Register power pins
        Coordinate vccCoord = getPhysicalPinCoordinate(vccPin);
        Coordinate gndCoord = getPhysicalPinCoordinate(gndPin);
        if (vccCoord != null) icPinManager.registerICPin(vccCoord, "VCC", this);
        if (gndCoord != null) icPinManager.registerICPin(gndCoord, "GND", this);
    }

    private Coordinate getPhysicalPinCoordinate(int logicalPin) {
        // Convert logical pin number (1-14) to physical breadboard coordinate
        if (logicalPin >= 1 && logicalPin <= 7) {
            // Pins 1-7 are on top row (section 1, row 0)
            return new Coordinate(1, 0, position.c + (logicalPin - 1));
        } else if (logicalPin >= 8 && logicalPin <= 14) {
            // Pins 8-14 are on bottom row (section 0, row 4) in reverse order
            return new Coordinate(0, 4, position.c + (14 - logicalPin));
        }
        return null;
    }

    @Override
    public int execute(int[] inputs) {
        if (inputs.length >= 2) {
            return inputs[0] & inputs[1]; // AND operation
        }
        return 0;
    }

    // Execute all 4 AND gates in the IC
    public int[] executeAllGates(int[] allInputs) {
        int[] outputs = new int[6];
        for (int i = 0; i < 6 && i < allInputs.length; i++) {
            outputs[i] = execute(new int[]{allInputs[i]});
        }
        return outputs;
    }

    // Get input values for a specific gate within the IC
    public int[] getGateInputs(int gateNumber) {
        if (gateNumber < 0 || gateNumber >= 4) return new int[]{0, 0};

        // Map gate number to input pins
        int[] gatePins = getInputPinsForGate(gateNumber);
        int[] inputs = new int[2];

        for (int i = 0; i < gatePins.length && i < 2; i++) {
            Coordinate pinCoord = getPhysicalPinCoordinate(gatePins[i]);
            if (pinCoord != null && mainActivity != null) {
                inputs[i] = icPinManager.getPinValue(pinCoord);
            }
        }

        return inputs;
    }

    private int[] getInputPinsForGate(int gateNumber) {
        switch (gateNumber) {
            case 0: return new int[]{1};   // Gate 1: pins 1 -> output 2
            case 1: return new int[]{3};   // Gate 2: pins 3 -> output 4
            case 2: return new int[]{5};   // Gate 3: pins 5 -> output 6
            case 3: return new int[]{9};   // Gate 4: pins 9 -> output 8
            case 4: return new int[]{11};   // Gate 5: pins 11 -> output 10
            case 5: return new int[]{13};   // Gate 6: pins 13 -> output 12
            default: return new int[]{0, 0};
        }
    }

    public int getOutputPinForGate(int gateNumber) {
        if (gateNumber >= 0 && gateNumber < outputPins.length) {
            return outputPins[gateNumber];
        }
        return -1;
    }
}
