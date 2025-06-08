package com.example.breadboard;

import com.example.breadboard.logic.AND;
import com.example.breadboard.logic.ICGate;
import com.example.breadboard.logic.ICGateInfo;
import com.example.breadboard.logic.NAND;
import com.example.breadboard.logic.NOR;
import com.example.breadboard.logic.NOT;
import com.example.breadboard.logic.OR;
import com.example.breadboard.logic.XOR;
import com.example.breadboard.model.Attribute;
import com.example.breadboard.model.Coordinate;
import com.example.breadboard.model.Pins;

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
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    // UI Components
    private GridLayout topGrid, middleGrid, bottomGrid;
    private HorizontalScrollView topScrollView;
    private HorizontalScrollView middleScrollView;
    private HorizontalScrollView bottomScrollView;
    private RelativeLayout icContainer;
    private ImageButton[][][] pins;
    private TextView[] topLabels, bottomLabels;
    private TextView[] rowLabels;
    private static Map<Coordinate, ICPinInfo> icPinRegistry = new HashMap<>();


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
    private static boolean canDisplay = false;

    // Constants
    private static final int ROWS = 5;
    private static final int COLS = 64;
    private static final int SECTIONS = 2;
    private static final char[] ROW_LABELS = {' ', 'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I', 'J', ' '};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initializeComponents();
        setupBreadboard();
    }

    private void initializeComponents() {
        topGrid = findViewById(R.id.topGrid);
        middleGrid = findViewById(R.id.middleGrid);
        bottomGrid = findViewById(R.id.bottomGrid);
        icContainer = findViewById(R.id.icContainer);

        // Initialize data structures
        pins = new ImageButton[SECTIONS][ROWS][COLS];
        pinAttributes = new Attribute[SECTIONS][ROWS][COLS];
        topLabels = new TextView[COLS];
        bottomLabels = new TextView[COLS];
        rowLabels = new TextView[12];
    }

    private void setupBreadboard() {
        setupScrollViews();
        setupGrids();
        setupLabels();
        setupPins();
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

            // Add to your main layout container
        }
    }

    private void setupGrids() {
        // Set up the grids where the pins will be placed
        topGrid.setColumnCount(COLS + 1);
        topGrid.setRowCount(1);

        middleGrid.setColumnCount(COLS + 1);
        middleGrid.setRowCount(ROWS * 2 + 1);

        bottomGrid.setColumnCount(COLS + 1);
        bottomGrid.setRowCount(1);
    }

    private void setupLabels() {
        // Set up the labels that will display the column of the breadboard
        TextView topCorner = new TextView(this);
        topCorner.setText(" ");
        topCorner.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
        topCorner.setTextSize(10);
        GridLayout.LayoutParams topCornerParams = new GridLayout.LayoutParams();
        topCornerParams.columnSpec = GridLayout.spec(0);
        topCornerParams.width = getResources().getDimensionPixelSize(R.dimen.pin_size);
        topCorner.setLayoutParams(topCornerParams);
        topGrid.addView(topCorner);

        // Setup column numbers for top grid (0, 1, 2, 3, ..., 63)
        for (int i = 0; i < COLS; i++) {
            TextView topLabel = new TextView(this);
            topLabel.setText(String.valueOf(i));
            topLabel.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
            topLabel.setTextSize(10);
            topLabels[i] = topLabel;

            GridLayout.LayoutParams topParams = new GridLayout.LayoutParams();
            topParams.columnSpec = GridLayout.spec(i + 1); // +1 to account for row label column
            topParams.width = getResources().getDimensionPixelSize(R.dimen.pin_size);
            topParams.setMargins(2, 2, 2, 2); // Same margins as pins
            topLabel.setLayoutParams(topParams);
            topGrid.addView(topLabel);
        }

        // Add corner label for bottom grid (empty space below row labels)
        TextView bottomCorner = new TextView(this);
        bottomCorner.setText(" ");
        bottomCorner.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
        bottomCorner.setTextSize(10);
        GridLayout.LayoutParams bottomCornerParams = new GridLayout.LayoutParams();
        bottomCornerParams.columnSpec = GridLayout.spec(0);
        bottomCornerParams.width = getResources().getDimensionPixelSize(R.dimen.pin_size);
        bottomCorner.setLayoutParams(bottomCornerParams);
        bottomGrid.addView(bottomCorner);

        // Setup column numbers for bottom grid - these will align with pin columns
        for (int i = 0; i < COLS; i++) {
            TextView bottomLabel = new TextView(this);
            bottomLabel.setText(String.valueOf(i));
            bottomLabel.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
            bottomLabel.setTextSize(10);
            bottomLabels[i] = bottomLabel;

            GridLayout.LayoutParams bottomParams = new GridLayout.LayoutParams();
            bottomParams.columnSpec = GridLayout.spec(i + 1); // +1 to account for row label column
            bottomParams.width = getResources().getDimensionPixelSize(R.dimen.pin_size);
            bottomParams.setMargins(2, 2, 2, 2); // Same margins as pins
            bottomLabel.setLayoutParams(bottomParams);
            bottomGrid.addView(bottomLabel);
        }
    }

    private void setupPins() {
        // Set up the pins to be used in the breadboard
        LayoutInflater inflater = LayoutInflater.from(this);

        // First, add row labels to the first column of middleGrid
        // Section 1 row labels (A-E)
        for (int i = 1; i <= 5; i++) {
            TextView rowLabel = new TextView(this);
            rowLabel.setText(String.valueOf(ROW_LABELS[i]));
            rowLabel.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
            rowLabel.setTextSize(12);

            GridLayout.LayoutParams params = new GridLayout.LayoutParams();
            params.rowSpec = GridLayout.spec(i - 1);
            params.columnSpec = GridLayout.spec(0);
            params.width = getResources().getDimensionPixelSize(R.dimen.pin_size);
            params.height = getResources().getDimensionPixelSize(R.dimen.pin_size);
            rowLabel.setLayoutParams(params);
            middleGrid.addView(rowLabel);
        }

        // Add gap row
        // This empty space is used for placement of ICs
        View gapView = new View(this);
        GridLayout.LayoutParams gapParams = new GridLayout.LayoutParams();
        gapParams.rowSpec = GridLayout.spec(5); // Row 5 is the gap
        gapParams.columnSpec = GridLayout.spec(0, COLS + 1); // Span all columns
        gapParams.width = GridLayout.LayoutParams.MATCH_PARENT;
        gapParams.height = getResources().getDimensionPixelSize(R.dimen.ic_height); // Use IC height for gap
        gapView.setLayoutParams(gapParams);
        middleGrid.addView(gapView);

        // Section 2 row labels (F-J)
        for (int i = 6; i <= 10; i++) {
            TextView rowLabel = new TextView(this);
            rowLabel.setText(String.valueOf(ROW_LABELS[i]));
            rowLabel.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
            rowLabel.setTextSize(12);

            GridLayout.LayoutParams params = new GridLayout.LayoutParams();
            params.rowSpec = GridLayout.spec(i); // i instead of ROWS + i - 6 because gap is at row 5
            params.columnSpec = GridLayout.spec(0);
            params.width = getResources().getDimensionPixelSize(R.dimen.pin_size);
            params.height = getResources().getDimensionPixelSize(R.dimen.pin_size);
            rowLabel.setLayoutParams(params);
            middleGrid.addView(rowLabel);
        }

        // Now add pins with proper GridLayout positioning
        for (int section = 0; section < SECTIONS; section++) {
            for (int row = 0; row < ROWS; row++) {
                for (int col = 0; col < COLS; col++) {
                    // Create pin button
                    ImageButton pin = new ImageButton(this);
                    pin.setImageResource(R.drawable.breadboard_pin);
                    pin.setBackground(null);
                    pin.setScaleType(ImageButton.ScaleType.CENTER_INSIDE);

                    // Set layout parameters for GridLayout
                    GridLayout.LayoutParams params = new GridLayout.LayoutParams();
                    params.width = getResources().getDimensionPixelSize(R.dimen.pin_size);
                    params.height = getResources().getDimensionPixelSize(R.dimen.pin_size);
                    params.setMargins(2, 1, 2, 1);

                    // Calculate grid row position accounting for the gap
                    int gridRow;
                    if (section == 0) {
                        gridRow = row; // First section: rows 0-4
                    } else {
                        gridRow = row + 6; // Second section: rows 6-10 (skipping gap at row 5)
                    }
                    int gridCol = col + 1; // +1 because column 0 is for row labels

                    params.rowSpec = GridLayout.spec(gridRow);
                    params.columnSpec = GridLayout.spec(gridCol);
                    pin.setLayoutParams(params);

                    // Set click listener
                    final int s = section, r = row, c = col;
                    pin.setOnClickListener(v -> onPinClicked(new Coordinate(s, r, c)));

                    // Add to arrays
                    pins[section][row][col] = pin;
                    pinAttributes[section][row][col] = new Attribute(-1, -1);

                    // Add to grid
                    middleGrid.addView(pin);
                }
            }
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

    private void onPinClicked(Coordinate coord) {
        ImageButton pin = pins[coord.s][coord.r][coord.c];

        // Check current pin state and show appropriate dialog
        if (isEmptyPin(coord)) {
            showPinConfigDialog(coord);
        } else {
            showPinRemovalDialog(coord);
        }
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


    // Dialog options
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

        showICSelectionDialog(coord);
    }

    private void showICSelectionDialog(Coordinate coord) {
        String[] icTypes = {"AND", "OR", "NOT", "NAND", "NOR", "XOR"};

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Select IC Type")
                .setItems(icTypes, (dialog, which) -> {
                    String icType = icTypes[which];
                    addICGate(coord, icType);
                })
                .show();
    }

    private void addInput(Coordinate coord) {
        if (checkValue(coord, 0)) {
            pins[coord.s][coord.r][coord.c].setImageResource(R.drawable.breadboard_inpt);
            inputs.add(coord);
            pinAttributes[coord.s][coord.r][coord.c] = new Attribute(-2, 0);
        } else {
            showToast("Error! Another Connection already Exists!");
        }
    }

    private void addOutput(Coordinate coord) {
        checkValue(coord, 2);
        pins[coord.s][coord.r][coord.c].setImageResource(R.drawable.breadboard_otpt);
        outputs.add(coord);
        pinAttributes[coord.s][coord.r][coord.c] = new Attribute(-2, 2);
    }

    private void addVcc(Coordinate coord) {
        if (checkValue(coord, 1)) {
            pins[coord.s][coord.r][coord.c].setImageResource(R.drawable.breadboard_vcc);
            vccPins.add(coord);
            pinAttributes[coord.s][coord.r][coord.c] = new Attribute(-2, 1);
        } else {
            showToast("Error! Another Connection already Exists!");
        }
    }

    private void addGround(Coordinate coord) {
        if (checkValue(coord, -2)) {
            pins[coord.s][coord.r][coord.c].setImageResource(R.drawable.breadboard_gnd);
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
        outputs.remove(coord);
        vccPins.remove(coord);
        gndPins.remove(coord);

        // Reset pin appearance and attributes
        pins[coord.s][coord.r][coord.c].setImageResource(R.drawable.breadboard_pin);
        pinAttributes[coord.s][coord.r][coord.c] = new Attribute(-1, -1);
        removeValue(coord);
    }



    public int ICnum = 0;
    public int contMargin = 0;
    private void addICGate(Coordinate coord, String icType) {
        Button icButton = new Button(this);
        icButton.setText(icType);
        icButton.setBackgroundResource(R.drawable.breadboard_ic);
        icButton.setTextColor(Color.WHITE);
        icButton.setTextSize(12);

        int pinSize = getResources().getDimensionPixelSize(R.dimen.pin_size);
        int icWidth = pinSize * 7 - 19; // IC spans 7 columns
        int icHeight = getResources().getDimensionPixelSize(R.dimen.ic_height);

        // Use RelativeLayout.LayoutParams since icContainer is inside a RelativeLayout
        RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(icWidth, icHeight);

        int rowLabelWidth = pinSize;
        int pinMarginLeft = 2; // Pin margins from setupPins()
        if (ICnum != 0) {
            pinMarginLeft = 1;
        }
        int pinMarginLeftPx = Math.round(pinMarginLeft * getResources().getDisplayMetrics().density);

        // Account for scroll view and grid padding
        int scrollPadding = Math.round(5 * getResources().getDisplayMetrics().density);
        int gridPadding = Math.round(4 * getResources().getDisplayMetrics().density);

        // Calculate absolute position
        params.leftMargin = contMargin + rowLabelWidth + pinMarginLeftPx + (coord.c * (pinSize + (pinMarginLeftPx * 2))
                + gridPadding + scrollPadding);

        // Center vertically in the gap
        params.addRule(RelativeLayout.CENTER_VERTICAL);

        icButton.setLayoutParams(params);

        // Create the actual ICGate logic object based on type
        ICGate gateLogic = createICGateLogic(icType, coord);
        if (gateLogic != null) {
            gateLogic.init(); // Initialize the gate's pin connections
            gates.add(gateLogic); // Add to your existing gates list
        }

        // Create ICGateInfo object for UI management
        ICGateInfo icGateInfo = new ICGateInfo(icType, coord, icButton, gateLogic);
        icGateObjects.add(icGateInfo);

        // Modified click listener to show IC details
        icButton.setOnClickListener(v -> showICConnectionDialog(icGateInfo));

        // Add to the RelativeLayout parent instead of icContainer
        RelativeLayout parentLayout = (RelativeLayout) icContainer.getParent();
        parentLayout.addView(icButton);

        icGates.add(icButton);

        // Mark IC pins as occupied
        markICPins(coord, icType);

        int addedPadding = 0;
        if (ICnum == 0) {
            contMargin -= 7;
            ICnum += 1;
        } else if (ICnum == 1) {
            contMargin -= 7 + 1;
            ICnum += 1;
        } else if (ICnum == 2) {
            contMargin -= 7 + 4;
            ICnum += 1;
        } else if (ICnum == 3) {
            contMargin -= 7 + 8;
            ICnum += 1;
        } else if (ICnum == 4){
            contMargin -= 7 + 12;
            ICnum += 1;
        } else if (ICnum == 5){
            addedPadding += 5;
            contMargin -= 7 + addedPadding;
            ICnum += 1;
        } else if (ICnum == 6){
            addedPadding += 5;
            contMargin -= 7 + addedPadding;
            ICnum += 1;
        } else if (ICnum == 7){
            addedPadding += 5;
            contMargin -= 7 + addedPadding;
            ICnum += 1;
        } else if (ICnum == 8){
            addedPadding += 5;
            contMargin -= 7 + addedPadding;
            ICnum += 1;
        }
    }

    private ICGate createICGateLogic(String icType, Coordinate coord) {
        switch (icType.toUpperCase()) {
            case "AND":
                return new AND(coord, null, this);
            case "OR":
                return new OR(coord, null, this);
            case "NOT":
                return new NOT(coord, null, this);
            case "NAND":
                return new NAND(coord, null, this);
            case "NOR":
                return new NOR(coord, null, this);
            case "XOR":
                return new XOR(coord, null, this);
            default:
                return null;
        }
    }

    private void showICConnectionDialog(ICGateInfo icGate) {
        StringBuilder connectionInfo = new StringBuilder();
        connectionInfo.append(icGate.type).append(" GATE\n\n");

        // Get pin connections for this IC
        String[] pinConnections = getICPinConnections(icGate);

        // Format the display in two columns
        for (int i = 0; i < 7; i++) {
            int leftPin = i + 1;
            int rightPin = 14 - i;

            String leftConnection = pinConnections[i];
            String rightConnection = pinConnections[13 - i];

            connectionInfo.append(String.format("%-2d - %-8s    %2d - %s\n",
                    leftPin, leftConnection, rightPin, rightConnection));
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("IC Pin Connections")
                .setMessage(connectionInfo.toString())
                .setPositiveButton("Execute Circuit", (dialog, which) -> executeCircuit())
                .setNegativeButton("Close", null)
                .show();
    }

    private String[] getICPinConnections(ICGateInfo icGate) {
        String[] connections = new String[14];
        Coordinate icPos = icGate.position;

        // Check pins 1-7 (top row F, section 1, row 0)
        for (int i = 0; i < 7; i++) {
            int col = icPos.c + i;
            if (col < COLS) {
                connections[i] = getPinConnectionType(1, 0, col);
            } else {
                connections[i] = "NC"; // Not Connected
            }
        }

        // Check pins 8-14 (bottom row E, section 0, row 4) - reverse order
        for (int i = 0; i < 7; i++) {
            int col = icPos.c + (6 - i);
            if (col < COLS) {
                connections[7 + i] = getPinConnectionType(0, 4, col);
            } else {
                connections[7 + i] = "NC"; // Not Connected
            }
        }

        return connections;
    }

    private String getPinConnectionType(int section, int row, int col) {
        // Check the entire column for connections (breadboard columns are connected vertically)
        for (int r = 0; r < ROWS; r++) {
            Attribute attr = pinAttributes[section][r][col];

            // Skip IC marker pins (-3 value)
            if (attr.value == -3) continue;

            // Check if there's a VCC connection in this column
            if (vccPins.contains(new Coordinate(section, r, col))) {
                return "VCC";
            }

            // Check if there's a GND connection in this column
            if (gndPins.contains(new Coordinate(section, r, col))) {
                return "GND";
            }

            // Check if there's an input connection in this column
            if (inputs.contains(new Coordinate(section, r, col))) {
                return String.valueOf(attr.value != -1 ? attr.value : 0);
            }

            // Check if there's an output connection in this column
            if (outputs.contains(new Coordinate(section, r, col))) {
                return "OUT";
            }

            // Check for wire connections (but not IC markers)
            if (attr.link != -1 && attr.value != -1 && attr.value != -3) {
                if (attr.value == 1) return "VCC";
                if (attr.value == -2) return "GND";
                if (attr.value == 0) return "0";
                return String.valueOf(attr.value);
            }
        }

        return "NC"; // Not Connected if no actual connection found
    }




    public void markICPins(Coordinate coord, String icType) {
        // Mark pins for a 14-pin DIP IC
        // Top row (F0-F6): pins 1-7
        for (int i = 0; i < 7; i++) {
            if (coord.c + i < COLS) {
                Coordinate topPin = new Coordinate(1, 0, coord.c + i); // Section 1, Row F
                pinAttributes[1][0][coord.c + i] = new Attribute(-3, -3); // Special IC marker
            }
        }

        // Bottom row (E6-E0): pins 8-14 (reversed order)
        for (int i = 0; i < 7; i++) {
            if (coord.c + (6 - i) < COLS) {
                Coordinate bottomPin = new Coordinate(0, 4, coord.c + (6 - i)); // Section 0, Row E
                pinAttributes[0][4][coord.c + (6 - i)] = new Attribute(-3, -3); // Special IC marker
            }
        }
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

    private boolean checkValue(Coordinate src, int value) {
        int i, tmp = -1;

        for (i = 0; i < ROWS; i++) {
            if (src.r != i) {
                tmp = pinAttributes[src.s][i][src.c].value;
                if (tmp != -1) {
                    break;
                }
            }
        }

        if (value == 2) {
            if (i == ROWS) {
                return false;
            }
            pinAttributes[src.s][src.r][src.c] = pinAttributes[src.s][i][src.c];
            return true;
        } else if (tmp == 1 || tmp == 0 || tmp == -2) {
            return false;
        } else {
            addValue(src, value);
            return true;
        }
    }

    private void addValue(Coordinate src, int value) {
        Attribute tmp;

        for (int i = 0; i < ROWS; i++) {
            tmp = pinAttributes[src.s][i][src.c];
            if (src.r != i && tmp.link != -1) {
                tmp.value = value;
                break;
            }
        }
    }

    private int getValue(Coordinate src) {
        Attribute tmp;

        for (int i = 0; i < ROWS; i++) {
            tmp = pinAttributes[src.s][i][src.c];
            if (src.r != i && tmp.link != -1 && tmp.value != 2) {
                return tmp.value;
            }
        }
        return 0;
    }

    private void removeValue(Coordinate src) {
        int tmp;

        for (int i = 0; i < ROWS; i++) {
            tmp = pinAttributes[src.s][i][src.c].link;
            if (src.r != i && tmp != -1) {
                pinAttributes[src.s][i][src.c].value = -1;
                break;
            }
        }
    }

    private void executeCircuit() {
        canDisplay = true;

        // Set VCC pins to high
        for (Coordinate coord : vccPins) {
            addValue(coord, 1);
        }

        // Set input pins to low initially
        for (Coordinate coord : inputs) {
            pinAttributes[coord.s][coord.r][coord.c].value = 0;
        }

        // Calculate output values
        for (Coordinate coord : outputs) {
            pinAttributes[coord.s][coord.r][coord.c].value = getValue(coord);
        }

        // Execute gate logic
        executeGates();

        if (canDisplay) {
            showGeneralResultDialog();
        }
    }

    private void showGeneralResultDialog() {
        StringBuilder result = new StringBuilder();
        result.append("Circuit Overview:\n\n");

        result.append("Components:\n");
        result.append("• ICs: ").append(icGateObjects.size()).append("\n");
        result.append("• Inputs: ").append(inputs.size()).append("\n");
        result.append("• Outputs: ").append(outputs.size()).append("\n");
        result.append("• VCC pins: ").append(vccPins.size()).append("\n");
        result.append("• GND pins: ").append(gndPins.size()).append("\n\n");

        if (!icGateObjects.isEmpty()) {
            result.append("IC Gates:\n");
            for (int i = 0; i < icGateObjects.size(); i++) {
                ICGateInfo ic = icGateObjects.get(i);
                result.append("• ").append(ic.type).append(" at column ").append(ic.position.c).append("\n");
            }
            result.append("\nTap on any IC to see its pin connections.");
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Circuit Simulation")
                .setMessage(result.toString())
                .setPositiveButton("OK", null)
                .show();
    }

    private void executeGates() {
        try {
            for (Object gateObj : gates) {
                if (gateObj instanceof ICGate) {
                    ICGate gate = (ICGate) gateObj;

                    // Get input values for this gate based on its pin connections
                    int[][] gateInputs = getGateInputs(gate);

                    // Execute the gate logic based on its type
                    if (gate instanceof AND) {
                        int[] outputs = ((AND) gate).executeAllGates(gateInputs);
                        setGateOutputs(gate, outputs);
                    } else if (gate instanceof OR) {
                        int[] outputs = ((OR) gate).executeAllGates(gateInputs);
                        setGateOutputs(gate, outputs);
                    } else if (gate instanceof NOT) {
                        // NOT gates have single inputs
                        int[] singleInputs = new int[gateInputs.length];
                        for (int i = 0; i < gateInputs.length; i++) {
                            singleInputs[i] = gateInputs[i].length > 0 ? gateInputs[i][0] : 0;
                        }
                        int[] outputs = ((NOT) gate).executeAllGates(singleInputs);
                        setGateOutputs(gate, outputs);
                    } else if (gate instanceof NAND) {
                        int[] outputs = ((NAND) gate).executeAllGates(gateInputs);
                        setGateOutputs(gate, outputs);
                    } else if (gate instanceof NOR) {
                        int[] outputs = ((NOR) gate).executeAllGates(gateInputs);
                        setGateOutputs(gate, outputs);
                    } else if (gate instanceof XOR) {
                        int[] outputs = ((XOR) gate).executeAllGates(gateInputs);
                        setGateOutputs(gate, outputs);
                    }
                }
            }

            // Update output pin values after all gates have been executed
            updateOutputPins();

        } catch (Exception e) {
            showToast("Error executing circuit: " + e.getMessage());
            canDisplay = false;
        }
    }

    private static class ICPinInfo {
        public String function; // "INPUT", "OUTPUT", "VCC", "GND"
        public ICGate icGate;   // Reference to the IC gate
        public int logicalPin;  // Logical pin number (1-14)

        public ICPinInfo(String function, ICGate icGate, int logicalPin) {
            this.function = function;
            this.icGate = icGate;
            this.logicalPin = logicalPin;
        }
    }

    public void registerICPin(Coordinate pinCoord, String function, ICGate icGate) {
        // Validate coordinates
        if (pinCoord == null || pinCoord.s < 0 || pinCoord.s >= SECTIONS ||
                pinCoord.r < 0 || pinCoord.r >= ROWS ||
                pinCoord.c < 0 || pinCoord.c >= COLS) {
            return;
        }

        // Calculate logical pin number based on physical position
        int logicalPin = getLogicalPinNumber(pinCoord, icGate.position);

        // Create pin info and register it
        ICPinInfo pinInfo = new ICPinInfo(function, icGate, logicalPin);
        icPinRegistry.put(pinCoord, pinInfo);

        // Mark the pin as an IC pin in the attributes
        pinAttributes[pinCoord.s][pinCoord.r][pinCoord.c] = new Attribute(-3, -3);
    }

    private int getLogicalPinNumber(Coordinate pinCoord, Coordinate icPosition) {
        if (pinCoord.s == 1 && pinCoord.r == 0) {
            // Top row (F): pins 1-7
            return (pinCoord.c - icPosition.c) + 1;
        } else if (pinCoord.s == 0 && pinCoord.r == 4) {
            // Bottom row (E): pins 8-14 in reverse order
            return 14 - (pinCoord.c - icPosition.c);
        }
        return -1; // Invalid pin
    }

    public int getPinValue(Coordinate pinCoord) {
        // Check if this is a registered IC pin
        ICPinInfo pinInfo = icPinRegistry.get(pinCoord);
        if (pinInfo != null) {
            // For IC pins, check the column for connections
            return getColumnValue(pinCoord);
        }

        // For regular pins, use existing logic
        return getValue(pinCoord);
    }

    private int getColumnValue(Coordinate pinCoord) {
        // Check all rows in the same section and column
        for (int r = 0; r < ROWS; r++) {
            Coordinate checkCoord = new Coordinate(pinCoord.s, r, pinCoord.c);

            // Skip the IC pin itself
            if (icPinRegistry.containsKey(checkCoord)) {
                continue;
            }

            Attribute attr = pinAttributes[pinCoord.s][r][pinCoord.c];

            // Check for VCC connections
            if (vccPins.contains(checkCoord)) {
                return 1;
            }

            // Check for GND connections
            if (gndPins.contains(checkCoord)) {
                return -2;
            }

            // Check for input connections
            if (inputs.contains(checkCoord)) {
                return attr.value != -1 ? attr.value : 0;
            }

            // Check for other connections
            if (attr.value != -1 && attr.value != -3) {
                return attr.value;
            }
        }

        return 0; // Default to 0 if no connection found
    }

    public void setICPinValue(Coordinate pinCoord, int value) {
        ICPinInfo pinInfo = icPinRegistry.get(pinCoord);
        if (pinInfo != null && "OUTPUT".equals(pinInfo.function)) {
            // Set the value for the entire column
            for (int r = 0; r < ROWS; r++) {
                Coordinate checkCoord = new Coordinate(pinCoord.s, r, pinCoord.c);
                if (!icPinRegistry.containsKey(checkCoord)) {
                    Attribute attr = pinAttributes[pinCoord.s][r][pinCoord.c];
                    if (attr.link != -1) {
                        attr.value = value;
                    }
                }
            }
        }
    }

    public ICPinInfo getICPinInfo(Coordinate pinCoord) {
        return icPinRegistry.get(pinCoord);
    }

    public boolean isICPin(Coordinate pinCoord) {
        return icPinRegistry.containsKey(pinCoord);
    }

    public void unregisterICPins(ICGate icGate) {
        icPinRegistry.entrySet().removeIf(entry -> entry.getValue().icGate == icGate);
    }


    private int[][] getGateInputs(ICGate gate) {
        // This method should analyze the pin connections of the IC
        // and return the input values for each gate within the IC
        int numGates = getNumGatesInIC(gate.type);
        int[][] inputs = new int[numGates][2]; // Most gates have 2 inputs
        // Need to implement the logic to read actual pin values here
        return inputs;
    }

    private void setGateOutputs(ICGate gate, int[] outputs) {
        // This method should set the output values to the appropriate pins
        // based on the gate's pin configuration

        // You'll need to implement the logic to write to actual pin values here
        // based on your pinAttributes array and connection logic
    }

    private int getNumGatesInIC(String icType) {
        switch (icType.toUpperCase()) {
            case "NOT":
                return 6; // 7404 has 6 NOT gates
            case "AND":
            case "OR":
            case "NAND":
            case "NOR":
            case "XOR":
                return 4; // Most have 4 gates
            default:
                return 1;
        }
    }

    private void updateOutputPins() {
        for (Coordinate coord : outputs) {
            int value = getValue(coord);
            // Update the visual representation if needed
        }
    }

    private void showResultDialog() {
        // TODO: Unfinished
        StringBuilder result = new StringBuilder();
        result.append("Circuit Results:\n\n");

        result.append("Inputs: ").append(inputs.size()).append("\n");
        result.append("Outputs: ").append(outputs.size()).append("\n");

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Simulation Results")
                .setMessage(result.toString())
                .setPositiveButton("OK", null)
                .show();
    }

    private void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }
}
