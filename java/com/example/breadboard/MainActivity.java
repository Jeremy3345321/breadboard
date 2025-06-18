package com.example.breadboard;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.GridLayout;
import android.widget.HorizontalScrollView;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.breadboard.ICPinManager;
import com.example.breadboard.ICPinManager.ICPinInfo;
import com.example.breadboard.InputManager.InputInfo;
import com.example.breadboard.OutputManager;
import com.example.breadboard.logic.ICGateInfo;
import com.example.breadboard.model.Attribute;
import com.example.breadboard.model.Coordinate;
import com.example.breadboard.model.Pins;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MainActivity extends AppCompatActivity implements BreadboardSetup.OnPinClickListener {

    // Implements:
    private BreadboardSetup breadboardSetup;
    private ICSetup icSetup;
    private ICPinManager icPinManager;
    private InputManager inputManager;
    private OutputManager outputManager;
    private AddConnection addConnection;
    private RemoveConnection removeConnection;


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
    String currentUsername;
    String currentCircuitName;

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
        // Get circuit context first before initializing other components.
        getCurrentCircuitContext();

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
                inputs, outputs, vccPins, gndPins, icGateObjects, addConnection);

        // Initialize ICPinManager
        icPinManager = new ICPinManager(this, pinAttributes, vccPins, gndPins, inputs);

        // Initialize InputManager - pass the current context directly
        inputManager = new InputManager(this, pins, pinAttributes, middleGrid, inputs,
                inputNames, inputLabels, inputDisplayContainer, currentUsername, currentCircuitName);

        // Initialize OutputManager - pass the current context directly
        outputManager = new OutputManager(this, pins, pinAttributes, outputs, icPinManager, currentUsername,
                currentCircuitName);

        // Initialize AddConnection
        addConnection = new AddConnection(this, pins, pinAttributes, icSetup,
                inputManager, outputManager, vccPins, gndPins);

        // Initialize RemoveConnection
        removeConnection = new RemoveConnection(this, pins, pinAttributes, middleGrid,
                inputs, vccPins, gndPins, inputNames, inputLabels, inputManager, outputManager);

        // IMPORTANT: Load data from database AFTER all managers are initialized
        // This ensures the context is properly set and no data gets cleared during initialization
        loadAllDataFromDatabase();

        debugDatabase();

        Button executeButton = findViewById(R.id.btnExecute);
        executeButton.setOnClickListener(v -> executeCircuit());

        Button clearButton = findViewById(R.id.btnClear);
        clearButton.setOnClickListener(v -> clearCircuitAndDatabase());
    }

    /**
     * Load all component data from database after initialization
     */
    private void loadAllDataFromDatabase() {
        System.out.println("=== LOADING ALL DATA FROM DATABASE ===");

        icSetup.setLoadingFromDatabase(true);
        getCurrentCircuitContext();

        // Load in the correct order to maintain dependencies
        if (icSetup != null) {
            icSetup.loadAllICsFromDatabase();
        }

        if (inputManager != null) {
            inputManager.loadInputsFromDatabase();
        }

        if (outputManager != null) {
            outputManager.loadOutputsFromDatabase();
        }

        icSetup.setLoadingFromDatabase(false);
        icSetup.updateCircuitContext(currentUsername, currentCircuitName);

        // Load connections last as they depend on other components
        // Add your connection loading here if you have it

        System.out.println("=== FINISHED LOADING ALL DATA FROM DATABASE ===");
    }

    private void debugDatabase() {
        System.out.println("🔍 Starting Database Debug...");

        DBHelper dbHelper = new DBHelper(this);

        // Full comprehensive debug
        dbHelper.fullDatabaseDebug();

        // Or individual checks:
        // dbHelper.listAllTables();
        // dbHelper.showTableStructure("inputs");
        // dbHelper.showTableData("inputs");
    }

    private void getCurrentCircuitContext() {
        // Get UserAuthentication instance
        UserAuthentication userAuth = UserAuthentication.getInstance(this);

        // First, try to get from Intent (this is the primary source for circuit context)
        String intentUsername = getIntent().getStringExtra("username");
        String intentCircuitName = getIntent().getStringExtra("circuit_name");

        // If username is provided in Intent, use it and update UserAuthentication
        if (intentUsername != null && !intentUsername.trim().isEmpty()) {
            currentUsername = intentUsername;
            // Update UserAuthentication with the current user
            userAuth.forceSetUser(currentUsername);
        } else {
            // Fallback to UserAuthentication if no Intent data
            currentUsername = userAuth.getCurrentUsername();
        }

        // Set circuit name from Intent
        if (intentCircuitName != null && !intentCircuitName.trim().isEmpty()) {
            currentCircuitName = intentCircuitName;
        } else {
            currentCircuitName = "defaultCircuit"; // Fallback
        }

        // Validate that we have valid context
        if (currentUsername == null || currentUsername.trim().isEmpty()) {
            System.err.println("ERROR: No valid username found!");
            currentUsername = "defaultUser"; // Emergency fallback
            Toast.makeText(this, "Authentication error - using default user", Toast.LENGTH_LONG).show();
        }

        // Print debug info
        System.out.println("MainActivity Circuit Context:");
        System.out.println("- Username: " + currentUsername);
        System.out.println("- Circuit Name: " + currentCircuitName);
        System.out.println("- Intent Username: " + intentUsername);
        System.out.println("- Intent Circuit: " + intentCircuitName);
        System.out.println("- UserAuth Username: " + userAuth.getCurrentUsername());

        // Print session info for debugging
        userAuth.printSessionInfo();
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

        // Load componenets from database
        if (inputManager != null) {
            inputManager.loadInputsFromDatabase();
        }
        if (outputManager != null) {
            outputManager.loadOutputsFromDatabase();
        }
        if (icSetup != null) {
            icSetup.loadAllICsFromDatabase();
            icSetup.updateCircuitContext(currentUsername, currentCircuitName);
        }
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
        inputDisplayScrollView.setVerticalScrollBarEnabled(false);

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
        if (addConnection.isEmptyPin(coord)) {
            addConnection.showPinConfigDialog(coord);
        } else {
            removeConnection.showPinRemovalDialog(coord);
        }
    }

    public void updateInputDisplay() {
        inputManager.updateInputDisplay();
    }

    // New method to handle input toggle from display
    public void onInputDisplayToggle(Coordinate coord) {
        inputManager.toggleInputValue(coord);
    }

    // Add this new method for updating outputs after circuit execution:
    public void updateOutputDisplay() {
        outputManager.updateAllOutputs();
    }

    // Add this method to get the OutputManager instance:
    public OutputManager getOutputManager() {
        return outputManager;
    }

    public void saveCircuitToDatabase() {
        try {
            inputManager.syncInputsToDatabase();
            showToast("Circuit saved to database successfully");
        } catch (Exception e) {
            showToast("Error saving circuit to database");
            System.err.println("Error saving circuit: " + e.getMessage());
        }
    }

    public void loadCircuitFromDatabase() {
        try {
            inputManager.loadInputsFromDatabase();
            showToast("Circuit loaded from database successfully");
        } catch (Exception e) {
            showToast("Error loading circuit from database");
            System.err.println("Error loading circuit: " + e.getMessage());
        }
    }
    public void clearCircuitAndDatabase() {
        try {
            // Clear database for current circuit (uses inputManager's internal context)
            inputManager.clearInputsFromDatabase();
            outputManager.clearOutputsFromDatabase();
            icSetup.removeIC();

            // Clear current circuit state
            clearCircuitState();

            showToast("Circuit '" + currentCircuitName + "' cleared successfully");

        } catch (Exception e) {
            showToast("Error clearing circuit and database");
            System.err.println("Error clearing circuit: " + e.getMessage());
            e.printStackTrace();
        }
    }
    public InputToDB getInputToDB() {
        return inputManager.getInputToDB();
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

    public AddConnection getAddConnection() {
        return addConnection;
    }

    private void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    public void setCurrentUser(String username) {
        this.currentUsername = username;
        if (inputManager != null) {
            inputManager.updateCircuitContext(username, currentCircuitName);
        }
    }

    public void setCurrentCircuit(String circuitName) {
        this.currentCircuitName = circuitName;
        if (inputManager != null) {
            inputManager.updateCircuitContext(currentUsername, circuitName);
        }
    }

    public void switchCircuit(String username, String circuitName) {
        // Clear current circuit state first
        clearCircuitState();

        // Update context
        this.currentUsername = username;
        this.currentCircuitName = circuitName;

        if (inputManager != null) {
            inputManager.updateCircuitContext(username, circuitName);
            // Load the new circuit's data
            inputManager.loadInputsFromDatabase();
        }
    }

    // Add method to clear current circuit state without affecting database
    private void clearCircuitState() {
        try {
            // Clear visual elements first
            List<Coordinate> inputsToRemove = new ArrayList<>(inputs);
            for (Coordinate coord : inputsToRemove) {
                removeConnection.removeConnection(coord);
            }

            // Clear in-memory data
            inputs.clear();
            inputNames.clear();
            inputLabels.clear();
            outputs.clear();

            // Add IC clearing:
            if (icSetup != null) {
                icSetup.removeIC();
            }

            // Update UI
            if (inputManager != null) {
                inputManager.updateInputDisplay();
            }

        } catch (Exception e) {
            System.err.println("Error clearing circuit state: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        // Refresh circuit context when returning to activity
        getCurrentCircuitContext();

        // Update all managers with current context
        if (inputManager != null) {
            inputManager.updateCircuitContext(currentUsername, currentCircuitName);
        }
        if (outputManager != null) {
            outputManager.updateCircuitContext(currentUsername, currentCircuitName);
        }
        if (icSetup != null) {
            icSetup.updateCircuitContext(currentUsername, currentCircuitName);
            // Only load if we haven't already loaded from initializeComponents()
            // The loadAllICsFromDatabase() method now has its own duplicate prevention
            icSetup.loadAllICsFromDatabase();
        }

        System.out.println("MainActivity onResume - Context refreshed: " + currentUsername + " / " + currentCircuitName);
    }

    public void debugCurrentContext() {
        System.out.println("=== CURRENT CIRCUIT CONTEXT ===");
        System.out.println("Username: " + currentUsername);
        System.out.println("Circuit Name: " + currentCircuitName);

        UserAuthentication userAuth = UserAuthentication.getInstance(this);
        System.out.println("UserAuth Username: " + userAuth.getCurrentUsername());
        System.out.println("UserAuth LoggedIn: " + userAuth.isUserLoggedIn());
        System.out.println("==============================");
    }
}