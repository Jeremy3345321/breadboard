package com.example.breadboard.logic;

import android.widget.Button;
import com.example.breadboard.MainActivity;
import com.example.breadboard.model.Coordinate;

public class NAND extends ICGate {

    public NAND(Coordinate position, Button button, MainActivity mainActivity) {
        super("NAND", position, button, mainActivity);
    }

    @Override
    public void init() {
        // Initialize NAND gate pin connections
        // 7400 quad 2-input NAND gate
    }

    @Override
    public int execute(int[] inputs) {
        if (inputs.length >= 2) {
            return (inputs[0] & inputs[1]) == 0 ? 1 : 0; // NAND operation
        }
        return 1; // Default high for NAND
    }

    public int[] executeAllGates(int[][] allInputs) {
        int[] outputs = new int[4];
        for (int i = 0; i < 4 && i < allInputs.length; i++) {
            outputs[i] = execute(allInputs[i]);
        }
        return outputs;
    }
}