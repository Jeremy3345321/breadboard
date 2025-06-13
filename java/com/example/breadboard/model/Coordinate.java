package com.example.breadboard.model;

import java.util.Objects;

public class Coordinate {
    public int s; // section
    public int r; // row  
    public int c; // column

    public Coordinate(int s, int r, int c) {
        this.s = s;
        this.r = r;
        this.c = c;
    }

    // CRITICAL: These methods are needed for HashMap operations
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        Coordinate that = (Coordinate) obj;
        return s == that.s && r == that.r && c == that.c;
    }

    @Override
    public int hashCode() {
        return Objects.hash(s, r, c);
    }

    @Override
    public String toString() {
        return "coords {" + s + ", " + r + ", " + c + "}";
    }
}