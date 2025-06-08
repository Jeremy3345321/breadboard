package com.example.breadboard.logic;

import android.widget.Button;
import com.example.breadboard.MainActivity;
import com.example.breadboard.model.Coordinate;

public class XOR extends ICGate {

    public XOR(Coordinate position, Button button, MainActivity mainActivity) {
        super("XOR", position, button, mainActivity);
    }

    @Override
    public void init() {
        // Initialize XOR gate pin connections
        // 7486 quad 2-input XOR gate
    }

    @Override
    public int execute(int[] inputs) {
        if (inputs.length >= 2) {
            return inputs[0] ^ inputs[1]; // XOR operation
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
