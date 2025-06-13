package com.example.breadboard;

import com.example.breadboard.logic.ICGate;
import com.example.breadboard.logic.ICGateInfo;
import com.example.breadboard.model.Attribute;
import com.example.breadboard.model.Coordinate;
import com.example.breadboard.model.Pins;
import com.example.breadboard.ICPinManager;
import com.example.breadboard.ICPinManager.ICPinInfo;
import com.example.breadboard.InputManager.InputInfo;
import com.example.breadboard.OutputManager;

import java.util.HashMap;
import java.util.Map;
import android.app.AlertDialog;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.GridLayout;
import android.widget.HorizontalScrollView;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.FrameLayout;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity implements BreadboardSetup.OnPinClickListener {

    // Implements:
    private BreadboardSetup breadboardSetup;
    private ICSetup icSetup;
    private ICPinManager icPinManager;
    private InputManager inputManager;
    private OutputManager outputManager;

    // UI Components
    private GridLayout topGrid, middleGrid, bottomGrid;
    private HorizontalScrollView topScrollView;
    private HorizontalScrollView middleScrollView;
    private HorizontalScrollView bottomScrollView;
    private HorizontalScrollView inputDisplayScrollView;
    private LinearLayout inputDisplayContainer;
    private RelativeLayout icContainer;
    private ImageButton[][][] pins;
    private TextView[] topLabels, bottomLabels;
    private TextView[] rowLabels;
    private static Map<Coordinate, ICPinInfo> icPinRegistry = new HashMap<>();
    private Map<Coordinate, TextView> inputLabels = new HashMap<>();
    Button executeButton;


    // Data structures
    private static Attribute[][][] pinAttributes;
    private static List<Button> icGates = new ArrayList<>();
    private static List<Object> gates = new ArrayList<>();
    private static List<Coordinate> inputs = new ArrayList<>();
    private static List<Coordinate> outputs = new ArrayList<>();
    private static List<Coordinate> vccPins = new ArrayList<>();
    private static List<Coordinate> gndPins = new ArrayList<>();
    private static List<Pins> wires = new ArrayList<>();
    private static List<ICGateInfo> icGateObjects = new ArrayList<>();

    private static Map<Coordinate, InputInfo> inputNames = new HashMap<>();



    // Constants
    private static final int ROWS = 5;
    private static final int COLS = 64;
    private static final int SECTIONS = 2;
    private static final char[] ROW_LABELS = {' ', 'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I', 'J', ' '};

    // Public fields
    public int ICnum = 0;
    public int contMargin = 6;
    public int extraPadding = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initializeComponents();
        setupBreadboard();
    }

    private void initializeComponents() {
        //  Initializing UI Elements
        topGrid = findViewById(R.id.topGrid);
        middleGrid = findViewById(R.id.middleGrid);
        bottomGrid = findViewById(R.id.bottomGrid);
        icContainer = findViewById(R.id.icContainer);
        inputDisplayScrollView = findViewById(R.id.inputDisplayScrollView);
        inputDisplayContainer = findViewById(R.id.inputDisplayContainer);

        // Initialize data structures
        pins = new ImageButton[SECTIONS][ROWS][COLS];
        pinAttributes = new Attribute[SECTIONS][ROWS][COLS];
        topLabels = new TextView[COLS];
        bottomLabels = new TextView[COLS];
        rowLabels = new TextView[12];

        // Initialize BreadboardSetup
        breadboardSetup = new BreadboardSetup(this, topGrid, middleGrid, bottomGrid,
                pins, pinAttributes, topLabels, bottomLabels, rowLabels);
        breadboardSetup.setPinClickListener(this);

        // Initialize ICSetup
        icSetup = new ICSetup(this, icContainer, pinAttributes, icGates, gates,
                inputs, outputs, vccPins, gndPins, icGateObjects);
        // Initialize ICPinManager
        icPinManager = new ICPinManager(this, pinAttributes, vccPins, gndPins, inputs);

        // Initialize InputManager
        inputManager = new InputManager(this, pins, pinAttributes, middleGrid, inputs, inputNames, inputLabels, inputDisplayContainer);

        // Intialize OutputManager
        outputManager = new OutputManager(this, pins, pinAttributes, outputs, icPinManager);

        Button executeButton = findViewById(R.id.btnExecute);
        executeButton.setOnClickListener(v -> executeCircuit());
    }
    private void executeCircuit() {
        if (icGateObjects.isEmpty()) {
            showToast("No IC gates found. Add some ICs to execute the circuit.");
            return;
        }

        // Debug: Print IC pins before execution
        icPinManager.debugPrintICPins();

        // Execute the circuit through ICSetup
        icSetup.executeCircuit();

        // Debug: Print IC pins after execution
        icPinManager.debugPrintICPins();

        // Update output displays after execution
        updateOutputDisplay();
    }

    private void setupBreadboard() {
        breadboardSetup.setupGrids();
        breadboardSetup.setupLabels();
        breadboardSetup.setupPins();
        setupScrollViews();
        synchronizeScrollViews();
    }

    private void setupScrollViews() {
        // Create HorizontalScrollViews of the label and breadboard body
        topScrollView = findViewById(R.id.topScrollView);
        middleScrollView = findViewById(R.id.middleScrollView);
        bottomScrollView = findViewById(R.id.bottomScrollView);

        if (topScrollView == null) {
            topScrollView = new HorizontalScrollView(this);
            middleScrollView = new HorizontalScrollView(this);
            bottomScrollView = new HorizontalScrollView(this);
        }
    }

    private void synchronizeScrollViews() {
        // Synchronize all scroll views so they scroll together
        // Top label scroll view, bottom label scroll view, and breadboard scroll view.
        HorizontalScrollView.OnScrollChangeListener scrollListener = new HorizontalScrollView.OnScrollChangeListener() {
            @Override
            public void onScrollChange(View v, int scrollX, int scrollY, int oldScrollX, int oldScrollY) {
                // Synchronize all scroll views to the same horizontal position
                if (v == topScrollView) {
                    if (middleScrollView.getScrollX() != scrollX) {
                        middleScrollView.scrollTo(scrollX, 0);
                    }
                    if (bottomScrollView.getScrollX() != scrollX) {
                        bottomScrollView.scrollTo(scrollX, 0);
                    }
                } else if (v == middleScrollView) {
                    if (topScrollView.getScrollX() != scrollX) {
                        topScrollView.scrollTo(scrollX, 0);
                    }
                    if (bottomScrollView.getScrollX() != scrollX) {
                        bottomScrollView.scrollTo(scrollX, 0);
                    }
                } else if (v == bottomScrollView) {
                    if (topScrollView.getScrollX() != scrollX) {
                        topScrollView.scrollTo(scrollX, 0);
                    }
                    if (middleScrollView.getScrollX() != scrollX) {
                        middleScrollView.scrollTo(scrollX, 0);
                    }
                }
            }
        };

        topScrollView.setVerticalScrollBarEnabled(false);
        middleScrollView.setVerticalScrollBarEnabled(false);
        bottomScrollView.setVerticalScrollBarEnabled(false);

        topScrollView.setOnScrollChangeListener(scrollListener);
        middleScrollView.setOnScrollChangeListener(scrollListener);
        bottomScrollView.setOnScrollChangeListener(scrollListener);
    }
    public void resizeSpecialPin(Coordinate coord, int drawableResource) {
        ImageButton pin = pins[coord.s][coord.r][coord.c];

        // Set the new drawable
        pin.setImageResource(drawableResource);

        // Get the current layout parameters
        GridLayout.LayoutParams params = (GridLayout.LayoutParams) pin.getLayoutParams();

        int originalSize = getResources().getDimensionPixelSize(R.dimen.pin_size);
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
        //extraPadding += 6;
    }

    @Override
    public void onPinClicked(Coordinate coord) {
        ImageButton pin = pins[coord.s][coord.r][coord.c];

        // Check current pin state and show appropriate dialog
        if (isEmptyPin(coord)) {
            showPinConfigDialog(coord);
        } else {
            showPinRemovalDialog(coord);
        }
    }

    private void showPinConfigDialog(Coordinate coord) {
        String[] options = {"Add IC", "Add Input", "Add Output", "Add Vcc", "Add Ground", "Connect Pin"};

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Pin Configuration")
                .setItems(options, (dialog, which) -> {
                    switch (which) {
                        case 0: addIC(coord); break;
                        case 1: addInput(coord); break;
                        case 2: addOutput(coord); break;
                        case 3: addVcc(coord); break;
                        case 4: addGround(coord); break;
                        case 5: connectPin(coord); break;
                    }
                })
                .show();
    }

    private void showPinRemovalDialog(Coordinate coord) {
        String[] options = {"Remove Connection", "Change Connection", "Cancel"};

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Pin Connection")
                .setItems(options, (dialog, which) -> {
                    switch (which) {
                        case 0: removeConnection(coord); break;
                        case 1:
                            removeConnection(coord);
                            showPinConfigDialog(coord);
                            break;
                        case 2: break; // Cancel
                    }
                })
                .show();
    }

    private boolean isEmptyPin(Coordinate coord) {
        ImageButton pin = pins[coord.s][coord.r][coord.c];
        Attribute attr = pinAttributes[coord.s][coord.r][coord.c];

        // Check if pin has original drawable and no special attributes
        boolean hasOriginalDrawable = pin.getDrawable().getConstantState().equals(
                ContextCompat.getDrawable(this, R.drawable.breadboard_pin).getConstantState());

        // Pin is empty if it has original drawable AND no attributes set (including IC markers)
        return hasOriginalDrawable && attr.link == -1 && attr.value == -1;
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

    private void addInput(Coordinate coord) {
        if (checkValue(coord, 0)) {
            inputManager.showInputNameDialog(coord);
        } else {
            showToast("Error! Another Connection already Exists!");
        }
    }

    public void updateInputDisplay() {
        inputManager.updateInputDisplay();
    }

    // New method to handle input toggle from display
    public void onInputDisplayToggle(Coordinate coord) {
        inputManager.toggleInputValue(coord);
    }

    private void addOutput(Coordinate coord) {
        outputManager.addOutput(coord);
    }

    // Add this new method for updating outputs after circuit execution:
    public void updateOutputDisplay() {
        outputManager.updateAllOutputs();
    }

    // Add this method to get the OutputManager instance:
    public OutputManager getOutputManager() {
        return outputManager;
    }

    private void addVcc(Coordinate coord) {
        if (checkValue(coord, 1)) {
            resizeSpecialPin(coord, R.drawable.breadboard_vcc);
            vccPins.add(coord);
            pinAttributes[coord.s][coord.r][coord.c] = new Attribute(-2, 1);
        } else {
            showToast("Error! Another Connection already Exists!");
        }
    }

    private void addGround(Coordinate coord) {
        if (checkValue(coord, -2)) {
            resizeSpecialPin(coord, R.drawable.breadboard_gnd);
            gndPins.add(coord);
            pinAttributes[coord.s][coord.r][coord.c] = new Attribute(-2, -2);
        } else {
            showToast("Error! Another Connection already Exists!");
        }
    }

    private void connectPin(Coordinate coord) {
        // TODO: Implementation for connecting pins with wires
        showToast("Pin connection feature - implementation needed");
    }

    private void removeConnection(Coordinate coord) {
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
                    ImageButton newPin = new ImageButton(this);
                    newPin.setImageResource(R.drawable.breadboard_pin);
                    newPin.setBackground(null);
                    newPin.setScaleType(ImageButton.ScaleType.CENTER_INSIDE);

                    // Set up the click listener
                    final int s = coord.s, r = coord.r, c = coord.c;
                    newPin.setOnClickListener(v -> onPinClicked(new Coordinate(s, r, c)));

                    // Create fresh layout parameters for the new pin
                    GridLayout.LayoutParams newParams = new GridLayout.LayoutParams();
                    int originalSize = getResources().getDimensionPixelSize(R.dimen.pin_size);
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
        if (extraPadding > 0) {
            extraPadding -= 7;
        }

        // Update input display after removal
        updateInputDisplay();
    }

    // Helper method to reset pin to original state
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
                    ImageButton newPin = new ImageButton(this);
                    newPin.setImageResource(R.drawable.breadboard_pin);
                    newPin.setBackground(null);
                    newPin.setScaleType(ImageButton.ScaleType.CENTER_INSIDE);

                    // Set up the click listener
                    final int s = coord.s, r = coord.r, c = coord.c;
                    newPin.setOnClickListener(v -> onPinClicked(new Coordinate(s, r, c)));

                    // Create fresh GridLayout parameters
                    GridLayout.LayoutParams newParams = new GridLayout.LayoutParams();
                    int originalSize = getResources().getDimensionPixelSize(R.dimen.pin_size);
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
                        int originalSize = getResources().getDimensionPixelSize(R.dimen.pin_size);
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

    // Helper method to recreate a pin in the grid with proper parameters
    private void recreatePinInGrid(Coordinate coord) {
        ImageButton oldPin = pins[coord.s][coord.r][coord.c];
        if (oldPin != null && oldPin.getParent() != null) {
            // Remove old pin from its parent
            ((android.view.ViewGroup) oldPin.getParent()).removeView(oldPin);
        }

        // Create new pin
        ImageButton newPin = new ImageButton(this);
        newPin.setImageResource(R.drawable.breadboard_pin);
        newPin.setBackground(null);
        newPin.setScaleType(ImageButton.ScaleType.CENTER_INSIDE);

        // Set up click listener
        final int s = coord.s, r = coord.r, c = coord.c;
        newPin.setOnClickListener(v -> onPinClicked(new Coordinate(s, r, c)));

        // Create proper GridLayout parameters
        GridLayout.LayoutParams params = new GridLayout.LayoutParams();
        int originalSize = getResources().getDimensionPixelSize(R.dimen.pin_size);
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
                    if (attr.link != -1 || attr.value != -1 || icPinManager.isICPin(new Coordinate(src.s, i, src.c))) {
                        hasConnection = true;
                        break;
                    }
                }
            }

            if (!hasConnection) {
                return false; // No connection to drive the output
            }

            // Set the output pin attributes
            pinAttributes[src.s][src.r][src.c] = new Attribute(-2, 2);
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

    public int getValue(Coordinate src) {
        Attribute tmp;

        for (int i = 0; i < ROWS; i++) {
            tmp = pinAttributes[src.s][i][src.c];
            if (src.r != i && tmp.link != -1 && tmp.value != 2) {
                return tmp.value;
            }
        }
        return 0;
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

    public ICPinManager getICPinManager() {
        return icPinManager;
    }

    private void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }
}