package com.example.breadboard.model;

public class Pins {
    public Coordinate src;
    public Coordinate dest;
    Pins(Coordinate src) {
        this.src = src;
    }
    public Pins(Coordinate src, Coordinate dest) {
        this.src = src;
        this.dest = dest;
    }
}

