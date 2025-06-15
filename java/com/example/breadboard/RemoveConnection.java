package com.example.breadboard;

import android.app.AlertDialog;
import android.widget.FrameLayout;
import android.widget.GridLayout;
import android.widget.ImageButton;
import android.widget.TextView;

import com.example.breadboard.InputManager.InputInfo;
import com.example.breadboard.model.Attribute;
import com.example.breadboard.model.Coordinate;

import java.util.List;
import java.util.Map;

public class RemoveConnection {
    
    private MainActivity mainActivity;
    private ImageButton[][][] pins;
    private Attribute[][][] pinAttributes;
    private GridLayout middleGrid;
    private List<Coordinate> inputs;
    private List<Coordinate> vccPins;
    private List<Coordinate> gndPins;
    private Map<Coordinate, InputInfo> inputNames;
    private Map<Coordinate, TextView> inputLabels;
    private InputManager inputManager;
    private OutputManager outputManager;
    
    // Constants
    private static final int ROWS = 5;
    
    public RemoveConnection(MainActivity mainActivity, ImageButton[][][] pins,
                            Attribute[][][] pinAttributes, GridLayout middleGrid,
                            List<Coordinate> inputs, List<Coordinate> vccPins,
                            List<Coordinate> gndPins, Map<Coordinate, InputInfo> inputNames,
                            Map<Coordinate, TextView> inputLabels, InputManager inputManager,
                            OutputManager outputManager) {
        this.mainActivity = mainActivity;
        this.pins = pins;
        this.pinAttributes = pinAttributes;
        this.middleGrid = middleGrid;
        this.inputs = inputs;
        this.vccPins = vccPins;
        this.gndPins = gndPins;
        this.inputNames = inputNames;
        this.inputLabels = inputLabels;
        this.inputManager = inputManager;
        this.outputManager = outputManager;
    }
    
    public void showPinRemovalDialog(Coordinate coord) {
        String[] options = {"Remove Connection", "Change Connection", "Cancel"};

        AlertDialog.Builder builder = new AlertDialog.Builder(mainActivity);
        builder.setTitle("Pin Connection")
                .setItems(options, (dialog, which) -> {
                    switch (which) {
                        case 0: removeConnection(coord); break;
                        case 1:
                            removeConnection(coord);
                            mainActivity.getAddConnection().showPinConfigDialog(coord);
                            break;
                        case 2: break; // Cancel
                    }
                })
                .show();
    }
    
    public void removeConnection(Coordinate coord) {
        // Remove from database first if it's an input
        if (inputNames.containsKey(coord)) {
            inputManager.removeInputFromDatabase(coord);
        }

        // Remove from appropriate lists
        inputs.remove(coord);
        vccPins.remove(coord);
        gndPins.remove(coord);

        // Special handling for output removal
        if (outputManager.isOutput(coord)) {
            outputManager.removeOutput(coord);
        }

        // Remove input name if it exists
        inputNames.remove(coord);

        // Handle input label removal (FrameLayout case)
        TextView inputLabel = inputLabels.get(coord);
        if (inputLabel != null) {
            try {
                // Get the FrameLayout parent
                FrameLayout frameLayout = (FrameLayout) inputLabel.getParent();
                if (frameLayout != null && frameLayout.getParent() == middleGrid) {
                    // Get the FrameLayout's layout parameters before removal
                    GridLayout.LayoutParams frameParams = (GridLayout.LayoutParams) frameLayout.getLayoutParams();

                    // Remove FrameLayout from grid
                    middleGrid.removeView(frameLayout);

                    // Create a completely new pin button
                    ImageButton newPin = new ImageButton(mainActivity);
                    newPin.setImageResource(R.drawable.breadboard_pin);
                    newPin.setBackground(null);
                    newPin.setScaleType(ImageButton.ScaleType.CENTER_INSIDE);

                    // Set up the click listener
                    final int s = coord.s, r = coord.r, c = coord.c;
                    newPin.setOnClickListener(v -> mainActivity.onPinClicked(new Coordinate(s, r, c)));

                    // Create fresh layout parameters for the new pin
                    GridLayout.LayoutParams newParams = new GridLayout.LayoutParams();
                    int originalSize = mainActivity.getResources().getDimensionPixelSize(R.dimen.pin_size);
                    newParams.width = originalSize;
                    newParams.height = originalSize;
                    newParams.setMargins(2, -2, 2, -2); // Use original pin margins
                    newParams.rowSpec = frameParams.rowSpec;
                    newParams.columnSpec = frameParams.columnSpec;

                    newPin.setLayoutParams(newParams);

                    // Update the pins array reference
                    pins[coord.s][coord.r][coord.c] = newPin;

                    // Add new pin to grid
                    middleGrid.addView(newPin);
                }

                // Remove from labels map
                inputLabels.remove(coord);

            } catch (ClassCastException | NullPointerException e) {
                // Handle case where FrameLayout structure is not as expected
                // Fall back to normal pin reset
                resetPinToOriginal(coord);
            }
        } else {
            // For non-input pins, reset normally
            resetPinToOriginal(coord);
        }

        // Reset attributes - IMPORTANT: Only reset if it wasn't an output (output manager handles its own cleanup)
        if (!outputManager.isOutput(coord)) {
            pinAttributes[coord.s][coord.r][coord.c] = new Attribute(-1, -1);
            removeValue(coord);
        }

        // Adjust padding counter
        if (mainActivity.extraPadding > 0) {
            mainActivity.extraPadding -= 7;
        }

        // Update input display after removal
        mainActivity.updateInputDisplay();
    }
    
    private void resetPinToOriginal(Coordinate coord) {
        ImageButton pin = pins[coord.s][coord.r][coord.c];
        if (pin != null) {
            // Reset pin image
            pin.setImageResource(R.drawable.breadboard_pin);

            // Check if pin is currently in a FrameLayout (input case)
            if (pin.getParent() instanceof FrameLayout) {
                FrameLayout frameLayout = (FrameLayout) pin.getParent();
                if (frameLayout.getParent() == middleGrid) {
                    // Get the FrameLayout's GridLayout parameters
                    GridLayout.LayoutParams frameParams = (GridLayout.LayoutParams) frameLayout.getLayoutParams();

                    // Remove FrameLayout from grid
                    middleGrid.removeView(frameLayout);

                    // Create new pin with proper GridLayout parameters
                    ImageButton newPin = new ImageButton(mainActivity);
                    newPin.setImageResource(R.drawable.breadboard_pin);
                    newPin.setBackground(null);
                    newPin.setScaleType(ImageButton.ScaleType.CENTER_INSIDE);

                    // Set up the click listener
                    final int s = coord.s, r = coord.r, c = coord.c;
                    newPin.setOnClickListener(v -> mainActivity.onPinClicked(new Coordinate(s, r, c)));

                    // Create fresh GridLayout parameters
                    GridLayout.LayoutParams newParams = new GridLayout.LayoutParams();
                    int originalSize = mainActivity.getResources().getDimensionPixelSize(R.dimen.pin_size);
                    newParams.width = originalSize;
                    newParams.height = originalSize;
                    newParams.setMargins(2, -2, 2, -2);
                    newParams.rowSpec = frameParams.rowSpec;
                    newParams.columnSpec = frameParams.columnSpec;

                    newPin.setLayoutParams(newParams);

                    // Update pins array reference
                    pins[coord.s][coord.r][coord.c] = newPin;

                    // Add to grid
                    middleGrid.addView(newPin);
                }
            } else {
                // Pin is directly in GridLayout, safe to cast
                try {
                    GridLayout.LayoutParams params = (GridLayout.LayoutParams) pin.getLayoutParams();
                    if (params != null) {
                        int originalSize = mainActivity.getResources().getDimensionPixelSize(R.dimen.pin_size);
                        params.width = originalSize;
                        params.height = originalSize;
                        params.setMargins(2, -2, 2, -2);
                        pin.setLayoutParams(params);
                    }
                } catch (ClassCastException e) {
                    // If cast fails, recreate the pin properly
                    recreatePinInGrid(coord);
                }
            }
        }
    }
    
    private void recreatePinInGrid(Coordinate coord) {
        ImageButton oldPin = pins[coord.s][coord.r][coord.c];
        if (oldPin != null && oldPin.getParent() != null) {
            // Remove old pin from its parent
            ((android.view.ViewGroup) oldPin.getParent()).removeView(oldPin);
        }

        // Create new pin
        ImageButton newPin = new ImageButton(mainActivity);
        newPin.setImageResource(R.drawable.breadboard_pin);
        newPin.setBackground(null);
        newPin.setScaleType(ImageButton.ScaleType.CENTER_INSIDE);

        // Set up click listener
        final int s = coord.s, r = coord.r, c = coord.c;
        newPin.setOnClickListener(v -> mainActivity.onPinClicked(new Coordinate(s, r, c)));

        // Create proper GridLayout parameters
        GridLayout.LayoutParams params = new GridLayout.LayoutParams();
        int originalSize = mainActivity.getResources().getDimensionPixelSize(R.dimen.pin_size);
        params.width = originalSize;
        params.height = originalSize;
        params.setMargins(2, -2, 2, -2);

        // Calculate grid position
        int gridRow = coord.s == 0 ? coord.r : coord.r + 6;
        int gridCol = coord.c + 1;

        params.rowSpec = GridLayout.spec(gridRow);
        params.columnSpec = GridLayout.spec(gridCol);

        newPin.setLayoutParams(params);

        // Update pins array reference
        pins[coord.s][coord.r][coord.c] = newPin;

        // Add to grid
        middleGrid.addView(newPin);
    }
    
    public void removeValue(Coordinate src) {
        int tmp;

        for (int i = 0; i < ROWS; i++) {
            tmp = pinAttributes[src.s][i][src.c].link;
            if (src.r != i && tmp != -1) {
                pinAttributes[src.s][i][src.c].value = -1;
                break;
            }
        }
    }
}