package com.example.breadboard.model;

public class Pins {
    Coordinate src;
    Attribute ob;
    Pins next;

    Pins(Coordinate src) {
        this.src = src;
    }

    public void add(Pins ob) {
        Pins tmp = this;
        while(tmp.next != null) {
            tmp = tmp.next;
        }
        tmp.next = ob;
    }
}
