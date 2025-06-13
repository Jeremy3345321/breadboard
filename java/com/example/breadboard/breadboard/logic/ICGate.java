package com.example.breadboard.logic;

import android.widget.Button;

import com.example.breadboard.ICPinManager;
import com.example.breadboard.ICPinManager.ICPinInfo;
import com.example.breadboard.MainActivity;
import com.example.breadboard.model.Coordinate;

public abstract class ICGate {
    public String type;
    public Coordinate position;
    Button button;

    public ICGate(String type, Coordinate position, Button button, MainActivity mainActivity) {
        this.type = type;
        this.position = position;
        this.button = button;
    }

    public abstract void init();
    public abstract int execute(int[] inputs);
}
