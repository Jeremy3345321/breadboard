package com.example.breadboard.logic;

import android.widget.Button;

import com.example.breadboard.model.Coordinate;

public class ICGateInfo {
    public String type;
    public Coordinate position;
    Button button;
    ICGate gateLogic; 

    public ICGateInfo(String type, Coordinate position, Button button, ICGate gateLogic) {
        this.type = type;
        this.position = position;
        this.button = button;
        this.gateLogic = gateLogic;
    }
}
