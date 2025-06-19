package com.example.breadboard;

import android.app.AlertDialog;
import android.widget.GridLayout;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.core.content.ContextCompat;

import com.example.breadboard.model.Attribute;
import com.example.breadboard.model.Coordinate;

import java.util.List;

public class AddConnection {

    private MainActivity mainActivity;
    private ImageButton[][][] pins;
    private Attribute[][][] pinAttributes;
    private ICSetup icSetup;
    private InputManager inputManager;
    private OutputManager outputManager;
    private ComponentManager componentManager;
    private List<Coordinate> vccPins;
    private List<Coordinate> gndPins;

    // Constants
    private static final int ROWS = 5;
    private static final int COLS = 64;

    public AddConnection(MainActivity mainActivity,
                         ImageButton[][][] pins,
                         Attribute[][][] pinAttributes,
                         ICSetup icSetup,
                         InputManager inputManager,
                         OutputManager outputManager,
                         List<Coordinate> vccPins,
                         List<Coordinate> gndPins) {
        this.mainActivity = mainActivity;
        this.pins = pins;
        this.pinAttributes = pinAttributes;
        this.icSetup = icSetup;
        this.inputManager = inputManager;
        this.outputManager = outputManager;
        this.componentManager = componentManager;
        this.vccPins = vccPins;
        this.gndPins = gndPins;
    }

    public void showPinConfigDialog(Coordinate coord) {
        String[] options = {"Add IC", "Add Input", "Add Output", "Add Vcc", "Add Ground", "Add Connection"};

        AlertDialog.Builder builder = new AlertDialog.Builder(mainActivity);
        builder.setTitle("Pin Configuration")
                .setItems(options, (dialog, which) -> {
                    switch (which) {
                        case 0: addIC(coord); break;
                        case 1: addInput(coord); break;
                        case 2: addOutput(coord); break;
                        case 3: addVcc(coord); break;
                        case 4: addGround(coord); break;
                        case 5: addConnection(coord); break;
                    }
                })
                .show();
    }

    private void addConnection(Coordinate coord) {
        mainActivity.getWireManager().showWireConnectionDialog(coord);
    }

    private void addIC(Coordinate coord) {
        if (coord.s != 0 || coord.r != 4) {
            showToast("An IC can only be added on the E Row.");
            return;
        }
        if (coord.c > 57) {
            showToast("An IC has 7 pins. Select a column less than 58.");
            return;
        }

        Coordinate freePin = isFree(coord);
        if (freePin != null) {
            char ch = (char)(65 + freePin.r + 5 * freePin.s);
            showToast("Cannot place IC. Connection found at " + ch + "-" + freePin.c + ".");
            return;
        }

        icSetup.showICSelectionDialog(coord);
    }

    private Coordinate isFree(Coordinate src) {
        // Check all 14 pins of the IC
        // Pins 1-7 go on row F (section 1, row 0)
        for (int pin = 0; pin < 7; pin++) {
            int c = src.c + pin;
            if (c < COLS) {
                Coordinate checkCoord = new Coordinate(1, 0, c);
                if (!isEmptyPin(checkCoord)) {
                    return checkCoord;
                }
            }
        }

        // Pins 8-14 go on row E (section 0, row 4) in reverse order
        for (int pin = 0; pin < 7; pin++) {
            int c = src.c + (6 - pin);
            if (c < COLS) {
                Coordinate checkCoord = new Coordinate(0, 4, c);
                if (!isEmptyPin(checkCoord)) {
                    return checkCoord;
                }
            }
        }

        return null; // All pins are free
    }

    boolean isEmptyPin(Coordinate coord) {
        ImageButton pin = pins[coord.s][coord.r][coord.c];
        Attribute attr = pinAttributes[coord.s][coord.r][coord.c];

        // Check if pin has original drawable and no special attributes
        boolean hasOriginalDrawable = pin.getDrawable().getConstantState().equals(
                ContextCompat.getDrawable(mainActivity, R.drawable.breadboard_pin).getConstantState());

        // Pin is empty if it has original drawable AND no attributes set (including IC markers)
        return hasOriginalDrawable && attr.link == -1 && attr.value == -1;
    }

    private void addInput(Coordinate coord) {
        if (checkValue(coord, 0)) {
            inputManager.showInputNameDialog(coord);
        } else {
            showToast("Error! Another Connection already exists!");
        }
    }

    private void addOutput(Coordinate coord) {
        System.out.println("AddConnection: addOutput method called for coordinate: " + coord);
        outputManager.addOutput(coord);
    }

    private void addVcc(Coordinate coord) {
        // Use ComponentManager if available, otherwise fallback to original method
        if (componentManager != null) {
            componentManager.addComponent(coord, ComponentToDB.VCC);
        } else {
            // Fallback to original implementation
            if (checkValue(coord, 1)) {
                resizeSpecialPin(coord, R.drawable.breadboard_vcc);
                vccPins.add(coord);
                pinAttributes[coord.s][coord.r][coord.c] = new Attribute(-2, 1);
            } else {
                showToast("Error! Another Connection already Exists!");
            }
        }
    }

    private void addGround(Coordinate coord) {
        // Use ComponentManager if available, otherwise fallback to original method
        if (componentManager != null) {
            componentManager.addComponent(coord, ComponentToDB.GND);
        } else {
            // Fallback to original implementation
            if (checkValue(coord, -2)) {
                resizeSpecialPin(coord, R.drawable.breadboard_gnd);
                gndPins.add(coord);
                pinAttributes[coord.s][coord.r][coord.c] = new Attribute(-2, -2);
            } else {
                showToast("Error! Another Connection already Exists!");
            }
        }
    }

    public void resizeSpecialPin(Coordinate coord, int drawableResource) {
        ImageButton pin = pins[coord.s][coord.r][coord.c];

        // Set the new drawable
        pin.setImageResource(drawableResource);

        // Get the current layout parameters
        GridLayout.LayoutParams params = (GridLayout.LayoutParams) pin.getLayoutParams();

        int originalSize = mainActivity.getResources().getDimensionPixelSize(R.dimen.pin_size);
        int newSize = originalSize + 36;

        // Update the size
        params.width = newSize;
        params.height = newSize;

        // Adjust margins to center the larger pin in its grid cell
        int extraSize = newSize - originalSize;
        int marginAdjustment = -extraSize / 2;

        params.setMargins(
                2 + marginAdjustment, // left
                -2 + marginAdjustment, // top
                2 + marginAdjustment, // right
                -2 + marginAdjustment  // bottom
        );

        // Apply the new layout parameters
        pin.setLayoutParams(params);

        // Ensure the pin is brought to front so it's visible over other elements
        pin.bringToFront();
    }

    public boolean checkValue(Coordinate src, int value) {
        int i, tmp = -1;

        // Special handling for output pins (value == 2)
        if (value == 2) {
            // For output pins, we need to check if there's something to connect to
            // but we don't need the pin itself to be connected

            // Check if the pin itself is free
            Attribute currentAttr = pinAttributes[src.s][src.r][src.c];
            if (currentAttr.link != -1 || currentAttr.value != -1) {
                return false; // Pin is already occupied
            }

            // Check if there's at least one connection in the same column
            boolean hasConnection = false;
            for (i = 0; i < ROWS; i++) {
                if (src.r != i) {
                    Attribute attr = pinAttributes[src.s][i][src.c];
                    if (attr.link != -1 || attr.value != -1 || mainActivity.getICPinManager().isICPin(new Coordinate(src.s, i, src.c))) {
                        hasConnection = true;
                        break;
                    }
                }
            }

            if (!hasConnection) {
                return false; // No connection to drive the output
            }

            // FIXED: Don't set the output pin attributes here - let OutputManager handle it
            // pinAttributes[src.s][src.r][src.c] = new Attribute(-2, 2);
            return true;
        }

        // Original logic for other pin types
        for (i = 0; i < ROWS; i++) {
            if (src.r != i) {
                tmp = pinAttributes[src.s][i][src.c].value;
                if (tmp != -1) {
                    break;
                }
            }
        }

        if (tmp == 1 || tmp == 0 || tmp == -2) {
            return false;
        } else {
            addValue(src, value);
            return true;
        }
    }

    public void addValue(Coordinate src, int value) {
        Attribute tmp;

        for (int i = 0; i < ROWS; i++) {
            tmp = pinAttributes[src.s][i][src.c];
            if (src.r != i && tmp.link != -1) {
                tmp.value = value;
                break;
            }
        }
    }

    private void showToast(String message) {
        Toast.makeText(mainActivity, message, Toast.LENGTH_SHORT).show();
    }
}