package com.example.breadboard.logic;

import android.widget.Button;

import com.example.breadboard.MainActivity;
import com.example.breadboard.model.Coordinate;

public class NOT extends ICGate {

    public NOT(Coordinate position, Button button, MainActivity mainActivity) {
        super("NOT", position, button, mainActivity);
    }

    @Override
    public void init() {
        // Initialize NOT gate pin connections
        // 7404 hex inverter: 6 NOT gates
        // Pin 1 -> Gate 1 input, Pin 2 -> Gate 1 output
        // Pin 3 -> Gate 2 input, Pin 4 -> Gate 2 output, etc.
    }

    @Override
    public int execute(int[] inputs) {
        if (inputs.length >= 1) {
            return inputs[0] == 0 ? 1 : 0; // NOT operation
        }
        return 0;
    }

    public int[] executeAllGates(int[] allInputs) {
        int[] outputs = new int[6];
        for (int i = 0; i < 6 && i < allInputs.length; i++) {
            outputs[i] = execute(new int[]{allInputs[i]});
        }
        return outputs;
    }
}
