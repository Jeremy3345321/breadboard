package com.example.breadboard.model;

import com.example.breadboard.model.Coordinate;

public class Pins {
    public Coordinate src;
    public Coordinate dest;

    public Pins(Coordinate src) {
        this.src = src;
    }

    public Pins(Coordinate src, Coordinate dest) {
        this.src = src;
        this.dest = dest;
    }

    // Getter methods
    public Coordinate getSrc() {
        return src;
    }

    public Coordinate getDst() {
        return dest;
    }

    // Setter methods (optional, for completeness)
    public void setSrc(Coordinate src) {
        this.src = src;
    }

    public void setDst(Coordinate dest) {
        this.dest = dest;
    }
}