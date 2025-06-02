package com.example.breadboard.model;

import com.example.breadboard.MainActivity;

public class Coordinate {
    public int s;
    public int r;
    public int c;
    public Coordinate(int s, int r, int c) {
        this.s = s;
        this.r = r;
        this.c = c;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        Coordinate that = (Coordinate) obj;
        return s == that.s && r == that.r && c == that.c;
    }
}
