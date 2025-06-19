package com.example.breadboard;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.view.View;
import android.widget.ImageButton;
import android.widget.RelativeLayout;

import com.example.breadboard.model.Coordinate;
import com.example.breadboard.WireToDB.WireData;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class WireVisualManager {
    private MainActivity mainActivity;
    private ImageButton[][][] pins;
    private RelativeLayout breadboardContainer;
    private WireOverlayView wireOverlay;
    private List<VisualWire> visualWires;
    private Map<String, VisualWire> wireMap; // For quick lookup and removal
    
    // Wire appearance constants
    private static final int WIRE_COLOR = Color.RED;
    private static final int WIRE_WIDTH = 6;
    private static final int WIRE_ALPHA = 200;
    
    public WireVisualManager(MainActivity mainActivity, ImageButton[][][] pins) {
        this.mainActivity = mainActivity;
        this.pins = pins;
        this.visualWires = new ArrayList<>();
        this.wireMap = new HashMap<>();
        
        setupWireOverlay();
    }
    
    private void setupWireOverlay() {
        // Get the breadboard container from the activity
        breadboardContainer = mainActivity.findViewById(R.id.breadboardContainer);
        
        if (breadboardContainer != null) {
            // Create and add the wire overlay view
            wireOverlay = new WireOverlayView();
            
            // Set layout params to match the breadboard container
            RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(
                RelativeLayout.LayoutParams.MATCH_PARENT,
                RelativeLayout.LayoutParams.MATCH_PARENT
            );
            
            wireOverlay.setLayoutParams(params);
            
            // Add the overlay to the breadboard container at index 0 so it doesn't interfere with existing views
            breadboardContainer.addView(wireOverlay, 0);
            
            System.out.println("Wire overlay view added to breadboard container");
        } else {
            System.err.println("Failed to find breadboard container for wire overlay");
        }
    }
    
    public void addVisualWire(Coordinate srcCoord, Coordinate dstCoord) {
        if (wireOverlay == null) {
            System.err.println("Wire overlay not initialized, cannot add visual wire");
            return;
        }
        
        // Calculate pin positions using actual pin locations (like WireManager does)
        float[] srcPos = calculateActualPinPosition(srcCoord);
        float[] dstPos = calculateActualPinPosition(dstCoord);
        
        if (srcPos == null || dstPos == null) {
            System.err.println("Failed to calculate pin positions for wire from " + srcCoord + " to " + dstCoord);
            return;
        }
        
        // Create visual wire
        VisualWire wire = new VisualWire(srcCoord, dstCoord, srcPos[0], srcPos[1], dstPos[0], dstPos[1]);
        
        // Add to collections
        visualWires.add(wire);
        wireMap.put(generateWireKey(srcCoord, dstCoord), wire);
        
        // Trigger redraw
        wireOverlay.invalidate();
        
        System.out.println("Added visual wire from " + srcCoord + " to " + dstCoord);
    }
    
    public void removeVisualWire(Coordinate srcCoord, Coordinate dstCoord) {
        String key = generateWireKey(srcCoord, dstCoord);
        String reverseKey = generateWireKey(dstCoord, srcCoord);
        
        VisualWire wire = wireMap.remove(key);
        if (wire == null) {
            wire = wireMap.remove(reverseKey);
        }
        
        if (wire != null) {
            visualWires.remove(wire);
            
            if (wireOverlay != null) {
                wireOverlay.invalidate();
            }
            
            System.out.println("Removed visual wire from " + srcCoord + " to " + dstCoord);
        } else {
            System.err.println("Visual wire not found for removal: " + srcCoord + " to " + dstCoord);
        }
    }
    
    public void removeAllVisualWires() {
        visualWires.clear();
        wireMap.clear();
        
        if (wireOverlay != null) {
            wireOverlay.invalidate();
        }
        
        System.out.println("Removed all visual wires");
    }
    
    public void loadVisualWiresFromData(List<WireData> wireDataList) {
        // Clear existing wires first
        removeAllVisualWires();
        
        // Add visual wires for each wire data
        for (WireData wireData : wireDataList) {
            Coordinate srcCoord = wireData.getSourceCoordinate();
            Coordinate dstCoord = wireData.getDestinationCoordinate();
            addVisualWire(srcCoord, dstCoord);
        }
        
        System.out.println("Loaded " + wireDataList.size() + " visual wires from data");
    }
    
    /**
     * Calculate actual pin position using the same approach as WireManager
     * This gets the real position of the pin in the layout
     */
    private float[] calculateActualPinPosition(Coordinate coord) {
        try {
            // Validate coordinate bounds
            if (coord.s < 0 || coord.s >= pins.length ||
                coord.r < 0 || coord.r >= pins[coord.s].length ||
                coord.c < 0 || coord.c >= pins[coord.s][coord.r].length) {
                System.err.println("Invalid coordinate bounds: " + coord);
                return null;
            }
            
            ImageButton pin = pins[coord.s][coord.r][coord.c];
            if (pin == null) {
                System.err.println("Pin is null at coordinate: " + coord);
                return null;
            }
            
            // Get pin positions using the same method as WireManager
            int[] pinLocation = new int[2];
            pin.getLocationInWindow(pinLocation);
            
            // Get container position for relative positioning
            int[] containerLocation = new int[2];
            breadboardContainer.getLocationInWindow(containerLocation);
            
            // Calculate relative positions (center of the pin)
            float x = pinLocation[0] - containerLocation[0] + pin.getWidth() / 2f;
            float y = pinLocation[1] - containerLocation[1] + pin.getHeight() / 2f;
            
            return new float[]{x, y};
            
        } catch (Exception e) {
            System.err.println("Error calculating actual pin position for " + coord + ": " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }
    
    /**
     * Fallback method for calculating pin positions when pins aren't laid out yet
     * This uses the same calculation logic as the original method
     */
    private float[] calculatePinPosition(Coordinate coord) {
        try {
            // Validate coordinate bounds
            if (coord.s < 0 || coord.s >= pins.length ||
                coord.r < 0 || coord.r >= pins[coord.s].length ||
                coord.c < 0 || coord.c >= pins[coord.s][coord.r].length) {
                System.err.println("Invalid coordinate bounds: " + coord);
                return null;
            }
            
            ImageButton pin = pins[coord.s][coord.r][coord.c];
            if (pin == null) {
                System.err.println("Pin is null at coordinate: " + coord);
                return null;
            }
            
            // Get pin dimensions and position
            int pinSize = 0;
            try {
                pinSize = mainActivity.getResources().getDimensionPixelSize(R.dimen.pin_size);
            } catch (Exception e) {
                pinSize = 40; // Default fallback
            }
            
            int pinMarginLeft = Math.round(1 * mainActivity.getResources().getDisplayMetrics().density);
            int scrollPadding = Math.round(5 * mainActivity.getResources().getDisplayMetrics().density);
            int gridPadding = Math.round(4 * mainActivity.getResources().getDisplayMetrics().density);
            
            // Calculate position based on coordinate and layout parameters
            float x = scrollPadding + gridPadding + pinMarginLeft + 
                     (coord.c * (pinSize + (pinMarginLeft * 2))) + (pinSize / 2.0f);
            
            // Calculate Y position based on section and row
            float y;
            if (coord.s == 0) {
                // Top section (rows A-E)
                y = gridPadding + (coord.r * (pinSize + (pinMarginLeft * 2))) + (pinSize / 2.0f);
            } else {
                // Bottom section (rows F-J)
                // Account for IC container height
                int icContainerHeight = 0;
                try {
                    icContainerHeight = mainActivity.getResources().getDimensionPixelSize(R.dimen.ic_height);
                } catch (Exception e) {
                    icContainerHeight = 50; // Default fallback
                }
                if (icContainerHeight == 0) {
                    icContainerHeight = 50; // Default fallback
                }
                
                y = gridPadding + (5 * (pinSize + (pinMarginLeft * 2))) + icContainerHeight + 
                    (coord.r * (pinSize + (pinMarginLeft * 2))) + (pinSize / 2.0f);
            }
            
            return new float[]{x, y};
            
        } catch (Exception e) {
            System.err.println("Error calculating pin position for " + coord + ": " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }
    
    private String generateWireKey(Coordinate coord1, Coordinate coord2) {
        return coord1.toString() + "->" + coord2.toString();
    }
    
    public void refreshVisualWires() {
        if (wireOverlay != null) {
            wireOverlay.invalidate();
        }
    }
    
    /**
     * Refresh wire positions after layout changes
     * This should be called after the layout is complete
     */
    public void refreshWirePositions() {
        if (wireOverlay == null || visualWires.isEmpty()) {
            return;
        }
        
        // Recalculate positions for all wires
        for (VisualWire wire : visualWires) {
            float[] srcPos = calculateActualPinPosition(wire.srcCoord);
            float[] dstPos = calculateActualPinPosition(wire.dstCoord);
            
            if (srcPos != null && dstPos != null) {
                // Update wire positions
                wire.startX = srcPos[0];
                wire.startY = srcPos[1];
                wire.endX = dstPos[0];
                wire.endY = dstPos[1];
            }
        }
        
        // Trigger redraw with updated positions
        wireOverlay.invalidate();
    }
    
    public int getVisualWireCount() {
        return visualWires.size();
    }
    
    // Inner class for visual wire representation
    private static class VisualWire {
        public final Coordinate srcCoord;
        public final Coordinate dstCoord;
        public float startX, startY;
        public float endX, endY;
        
        public VisualWire(Coordinate srcCoord, Coordinate dstCoord, 
                         float startX, float startY, float endX, float endY) {
            this.srcCoord = srcCoord;
            this.dstCoord = dstCoord;
            this.startX = startX;
            this.startY = startY;
            this.endX = endX;
            this.endY = endY;
        }
    }
    
    // Custom view for drawing wire overlays
    private class WireOverlayView extends View {
        private Paint wirePaint;
        
        public WireOverlayView() {
            super(mainActivity);
            setupPaint();
            // Make sure the overlay doesn't interfere with touch events
            setClickable(false);
            setFocusable(false);
        }
        
        private void setupPaint() {
            wirePaint = new Paint();
            wirePaint.setColor(WIRE_COLOR);
            wirePaint.setStrokeWidth(WIRE_WIDTH);
            wirePaint.setAlpha(WIRE_ALPHA);
            wirePaint.setAntiAlias(true);
            wirePaint.setStrokeCap(Paint.Cap.ROUND);
        }
        
        @Override
        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            
            // Draw all visual wires
            for (VisualWire wire : visualWires) {
                drawWire(canvas, wire);
            }
        }
        
        private void drawWire(Canvas canvas, VisualWire wire) {
            try {
                // Draw the main wire line
                canvas.drawLine(wire.startX, wire.startY, wire.endX, wire.endY, wirePaint);
                
                // Draw connection points (small circles at endpoints)
                float connectionRadius = WIRE_WIDTH / 2.0f;
                canvas.drawCircle(wire.startX, wire.startY, connectionRadius, wirePaint);
                canvas.drawCircle(wire.endX, wire.endY, connectionRadius, wirePaint);
                
            } catch (Exception e) {
                System.err.println("Error drawing wire from " + wire.srcCoord + " to " + wire.dstCoord + ": " + e.getMessage());
            }
        }
    }
}