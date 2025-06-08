package com.example.breadboard.logic;

import android.widget.Button;
import com.example.breadboard.MainActivity;
import com.example.breadboard.model.Coordinate;

public class OR extends ICGate {

    public OR(Coordinate position, Button button, MainActivity mainActivity) {
        super("OR", position, button, mainActivity);
    }

    @Override
    public void init() {
        // Initialize OR gate pin connections
        // Similar to AND gate but for OR logic (7432 quad 2-input OR gate)
    }

    @Override
    public int execute(int[] inputs) {
        if (inputs.length >= 2) {
            return inputs[0] | inputs[1]; // OR operation
        }
        return 0;
    }

    public int[] executeAllGates(int[][] allInputs) {
        int[] outputs = new int[4];
        for (int i = 0; i < 4 && i < allInputs.length; i++) {
            outputs[i] = execute(allInputs[i]);
        }
        return outputs;
    }
}
