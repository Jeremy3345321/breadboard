package com.example.breadboard.logic;

import android.widget.Button;
import com.example.breadboard.MainActivity;
import com.example.breadboard.model.Coordinate;

public class NOR extends ICGate {

    public NOR(Coordinate position, Button button, MainActivity mainActivity) {
        super("NOR", position, button, mainActivity);
    }

    @Override
    public void init() {
        // Initialize NOR gate pin connections
        // 7402 quad 2-input NOR gate
    }

    @Override
    public int execute(int[] inputs) {
        if (inputs.length >= 2) {
            return (inputs[0] | inputs[1]) == 0 ? 1 : 0; // NOR operation
        }
        return 1; // Default high for NOR
    }

    public int[] executeAllGates(int[][] allInputs) {
        int[] outputs = new int[4];
        for (int i = 0; i < 4 && i < allInputs.length; i++) {
            outputs[i] = execute(allInputs[i]);
        }
        return outputs;
    }
}
