package com.example.breadboard;

import android.app.AlertDialog;
import android.graphics.Color;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.GridLayout;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.example.breadboard.InputToDB;
import com.example.breadboard.InputToDB.InputData;
import com.example.breadboard.model.Attribute;
import com.example.breadboard.model.Coordinate;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class InputManager {
    private MainActivity mainActivity;
    private ImageButton[][][] pins;
    private Attribute[][][] pinAttributes;
    private GridLayout middleGrid;
    private List<Coordinate> inputs;
    private Map<Coordinate, InputInfo> inputNames;
    private Map<Coordinate, TextView> inputLabels;
    private LinearLayout inputDisplayContainer;
    private InputToDB inputToDB;
    private String currentUsername;
    private String currentCircuitName;


    public static class InputInfo {
        public String name;
        public int value;

        public InputInfo(String name, int value) {
            this.name = name;
            this.value = value;
        }
    }

    public InputManager(MainActivity mainActivity, ImageButton[][][] pins, Attribute[][][] pinAttributes,
                        GridLayout middleGrid, List<Coordinate> inputs,
                        Map<Coordinate, InputInfo> inputNames, Map<Coordinate, TextView> inputLabels,
                        LinearLayout inputDisplayContainer, String username, String circuitName) {
        this.mainActivity = mainActivity;
        this.pins = pins;
        this.pinAttributes = pinAttributes;
        this.middleGrid = middleGrid;
        this.inputs = inputs;
        this.inputNames = inputNames;
        this.inputLabels = inputLabels;
        this.inputDisplayContainer = inputDisplayContainer;
        this.currentUsername = username;
        this.currentCircuitName = circuitName;

        this.inputToDB = new InputToDB(mainActivity);
    }

    public void showInputNameDialog(Coordinate coord) {
        AlertDialog.Builder builder = new AlertDialog.Builder(mainActivity);
        builder.setTitle("Name Input");

        final EditText input = new EditText(mainActivity);
        input.setHint("Enter single letter (A-Z)");
        input.setInputType(android.text.InputType.TYPE_CLASS_TEXT | android.text.InputType.TYPE_TEXT_FLAG_CAP_CHARACTERS);
        input.setFilters(new android.text.InputFilter[]{new android.text.InputFilter.LengthFilter(1)});

        builder.setView(input);

        builder.setPositiveButton("OK", (dialog, which) -> {
            String inputName = input.getText().toString().trim().toUpperCase();

            // Validate input
            if (inputName.isEmpty()) {
                showToast("Please enter a letter!");
                showInputNameDialog(coord); // Show dialog again
                return;
            }

            if (!inputName.matches("[A-Z]")) {
                showToast("Only single letters (A-Z) are allowed!");
                showInputNameDialog(coord); // Show dialog again
                return;
            }

            // Check if name is already used
            if (isInputNameUsed(inputName)) {
                showToast("Input name '" + inputName + "' is already used!");
                showInputNameDialog(coord); // Show dialog again
                return;
            }

            // Create the input with the given name
            createNamedInput(coord, inputName);
        });

        builder.setNegativeButton("Cancel", (dialog, which) -> {
            // Reset the coordinate since we're canceling
            pinAttributes[coord.s][coord.r][coord.c] = new Attribute(-1, -1);
            mainActivity.removeValue(coord);
        });

        builder.show();
    }

    // Check if input name is already used.
    public boolean isInputNameUsed(String name) {
        // Only check in-memory inputs for the current circuit/breadboard
        for (Coordinate coord : inputs) {
            InputInfo info = getInputInfo(coord);
            if (info != null && name.equals(info.name)) {
                return true;
            }
        }
        return false;
    }

    public void createNamedInput(Coordinate coord, String name) {
        mainActivity.resizeSpecialPin(coord, R.drawable.breadboard_inpt);
        inputs.add(coord);
        pinAttributes[coord.s][coord.r][coord.c] = new Attribute(-2, 0);

        // Store the input name in memory with default LOW value
        setInputName(coord, name);

        // Save to database WITHOUT value (remove the value parameter)
        boolean dbResult = inputToDB.insertInput(name, currentUsername, currentCircuitName, coord);
        if (!dbResult) {
            System.err.println("Failed to save input " + name + " to database for circuit " + currentCircuitName);
            showToast("Warning: Failed to save input to database");
        } else {
            System.out.println("Input " + name + " saved to database successfully for circuit " + currentCircuitName);
        }

        // Create and display the input name label
        createInputLabel(coord, name);

        // Update the input display
        updateInputDisplay();

        showToast("Input '" + name + "' created successfully!");
        System.out.println("Input " + name + " created for circuit " + currentCircuitName);
    }

    public void updateInputDisplay() {
        System.out.println("=== updateInputDisplay START ===");
        System.out.println("Current context - Username: " + currentUsername + ", Circuit: " + currentCircuitName);
        System.out.println("inputs.size(): " + inputs.size());
        System.out.println("inputNames.size(): " + inputNames.size());
        System.out.println("inputDisplayContainer null check: " + (inputDisplayContainer == null ? "NULL" : "NOT NULL"));

        // ADDED: Debug container properties
        if (inputDisplayContainer != null) {
            System.out.println("Container visibility: " + inputDisplayContainer.getVisibility());
            System.out.println("Container dimensions: " + inputDisplayContainer.getWidth() + "x" + inputDisplayContainer.getHeight());
            System.out.println("Container parent: " + (inputDisplayContainer.getParent() != null ? inputDisplayContainer.getParent().getClass().getSimpleName() : "null"));
        }

        // Clear existing display
        if (inputDisplayContainer != null) {
            int childCountBefore = inputDisplayContainer.getChildCount();
            inputDisplayContainer.removeAllViews();
            System.out.println("Cleared " + childCountBefore + " existing views from inputDisplayContainer");
        } else {
            System.err.println("ERROR: inputDisplayContainer is null!");
            return;
        }

        // Add each input to the display with context validation
        int buttonsCreated = 0;
        for (Coordinate coord : inputs) {
            System.out.println("Processing coordinate: " + coord);
            InputInfo info = getInputInfo(coord);
            if (info != null) {
                System.out.println("Creating display button for input: " + info.name + " (value: " + info.value + ") at " + coord);

                createInputDisplayButton(coord, info);
                buttonsCreated++;
            } else {
                System.err.println("ERROR: No InputInfo found for coordinate " + coord + " in circuit " + currentCircuitName);
            }
        }

        System.out.println("Created " + buttonsCreated + " input display buttons");

        // ADDED: Force layout update on the container with delay to allow for proper rendering
        if (inputDisplayContainer != null) {
            inputDisplayContainer.requestLayout();

            // Post a runnable to ensure layout completion
            inputDisplayContainer.post(() -> {
                inputDisplayContainer.invalidate();

                // ADDED: Also update parent layouts
                if (inputDisplayContainer.getParent() instanceof View) {
                    ((View) inputDisplayContainer.getParent()).requestLayout();
                }

                // ADDED: Debug final dimensions
                System.out.println("Final container dimensions: " + inputDisplayContainer.getWidth() + "x" + inputDisplayContainer.getHeight());
                System.out.println("Final container child count: " + inputDisplayContainer.getChildCount());
            });
        }
        System.out.println("=== updateInputDisplay END ===");
    }

    private void refreshInputDisplay() {
        System.out.println("Refreshing input display...");

        // Force clear and recreate
        if (inputDisplayContainer != null) {
            inputDisplayContainer.removeAllViews();

            // Recreate all buttons
            for (Coordinate coord : inputs) {
                InputInfo info = getInputInfo(coord);
                if (info != null) {
                    createInputDisplayButton(coord, info);
                    System.out.println("Refreshed display button for " + info.name);
                }
            }

            // Force layout update
            inputDisplayContainer.requestLayout();
            inputDisplayContainer.invalidate();
        }
    }

    private void createInputDisplayButton(Coordinate coord, InputInfo info) {
        try {
            System.out.println("Creating button for input: " + info.name + " at " + coord);

            // Create a button for the input
            Button inputButton = new Button(mainActivity);

            // Set button text and appearance based on current value
            updateInputButtonAppearance(inputButton, info);

            // FIXED: Use wrap_content for width to prevent 0x0 dimensions
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
            );
            params.setMargins(8, 4, 8, 4);

            // ADDED: Set minimum and preferred dimensions
            inputButton.setMinWidth(dpToPx(100));
            inputButton.setMinHeight(dpToPx(60));
            inputButton.setPadding(dpToPx(16), dpToPx(8), dpToPx(16), dpToPx(8));

            inputButton.setLayoutParams(params);

            // ADDED: Ensure visibility and force dimensions
            inputButton.setVisibility(View.VISIBLE);

            // ADDED: Set specific width/height to ensure button is measurable
            inputButton.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED);

            // Set click listener to toggle input value
            inputButton.setOnClickListener(v -> {
                toggleInputValue(coord);
                updateInputButtonAppearance(inputButton, getInputInfo(coord));
            });

            // Add button to container
            if (inputDisplayContainer != null) {
                inputDisplayContainer.addView(inputButton);
                System.out.println("Successfully added button for " + info.name + " to container");

                // ADDED: Force layout refresh with proper sequence
                inputDisplayContainer.requestLayout();

                // ADDED: Post a runnable to ensure layout happens after view is added
                inputDisplayContainer.post(() -> {
                    inputDisplayContainer.invalidate();
                    // Force parent to relayout as well
                    if (inputDisplayContainer.getParent() instanceof View) {
                        ((View) inputDisplayContainer.getParent()).requestLayout();
                    }
                });

                // Verify the button was added
                int childCount = inputDisplayContainer.getChildCount();
                System.out.println("Container now has " + childCount + " children");

                // ADDED: Debug the actual button properties after layout
                inputButton.post(() -> {
                    System.out.println("Button post-layout visibility: " + inputButton.getVisibility());
                    System.out.println("Button post-layout dimensions: " + inputButton.getWidth() + "x" + inputButton.getHeight());
                    System.out.println("Container post-layout dimensions: " + inputDisplayContainer.getWidth() + "x" + inputDisplayContainer.getHeight());
                });
            } else {
                System.err.println("ERROR: inputDisplayContainer is null when trying to add button for " + info.name);
            }

        } catch (Exception e) {
            System.err.println("Error creating display button for " + info.name + ": " + e.getMessage());
            e.printStackTrace();
        }
    }

    private int dpToPx(int dp) {
        float density = mainActivity.getResources().getDisplayMetrics().density;
        return Math.round(dp * density);
    }

    private void updateInputButtonAppearance(Button button, InputInfo info) {
        String valueText = info.value == 1 ? "HIGH" : "LOW";
        button.setText(info.name + ": " + valueText);

        // Set color based on value
        if (info.value == 1) {
            button.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.parseColor("#4CAF50"))); // Green for HIGH
            button.setTextColor(Color.WHITE);
        } else {
            button.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.parseColor("#757575"))); // Gray for LOW
            button.setTextColor(Color.WHITE);
        }
    }

    public void toggleInputValue(Coordinate coord) {
        InputInfo info = inputNames.get(coord);
        if (info != null) {
            info.value = info.value == 0 ? 1 : 0;
            pinAttributes[coord.s][coord.r][coord.c].value = info.value;

            System.out.println("Toggled input at " + coord + " to value=" + info.value + " for circuit " + currentCircuitName);
            updateInputDisplay();
        }
    }

    public void createInputLabel(Coordinate coord, String name) {
        // Create a TextView for the input name
        TextView inputLabel = new TextView(mainActivity);
        inputLabel.setText(name);
        inputLabel.setTextColor(Color.WHITE);
        inputLabel.setTextSize(14);
        inputLabel.setTypeface(null, android.graphics.Typeface.BOLD);
        inputLabel.setGravity(android.view.Gravity.CENTER);
        inputLabel.setShadowLayer(2, 1, 1, Color.BLACK); // Add shadow for better visibility

        // Get the input pin button
        ImageButton pin = pins[coord.s][coord.r][coord.c];

        // Create a FrameLayout to overlay the text on the pin
        FrameLayout frameLayout = new FrameLayout(mainActivity);

        // Get pin's current layout parameters
        GridLayout.LayoutParams pinParams = (GridLayout.LayoutParams) pin.getLayoutParams();

        // Remove pin from its current parent
        middleGrid.removeView(pin);

        // Add pin to the FrameLayout
        FrameLayout.LayoutParams pinFrameParams = new FrameLayout.LayoutParams(
                pinParams.width, pinParams.height);
        pin.setLayoutParams(pinFrameParams);
        frameLayout.addView(pin);

        FrameLayout.LayoutParams labelParams = new FrameLayout.LayoutParams(
                pinParams.width, pinParams.height);
        labelParams.gravity = android.view.Gravity.CENTER;
        inputLabel.setLayoutParams(labelParams);
        frameLayout.addView(inputLabel);

        // Set the FrameLayout's layout parameters to match the original pin
        frameLayout.setLayoutParams(pinParams);

        // Add the FrameLayout (containing pin + label) back to the grid
        middleGrid.addView(frameLayout);

        // Store the label reference for later removal if needed
        inputLabels.put(coord, inputLabel);

        // Make sure the pin click listener still works
        frameLayout.setOnClickListener(v -> mainActivity.onPinClicked(coord));
    }

    public void removeInputFromDatabase(Coordinate coord) {
        InputInfo info = getInputInfo(coord);
        if (info != null) {
            boolean dbResult = inputToDB.deleteInput(info.name, currentUsername, currentCircuitName);
            if (!dbResult) {
                System.err.println("Failed to remove input " + info.name + " from database for circuit " + currentCircuitName);
            } else {
                System.out.println("Removed input " + info.name + " from database for circuit " + currentCircuitName);
            }
        } else {
            // Try to remove by coordinate if name is not found
            boolean dbResult = inputToDB.deleteInputByCoordinate(currentUsername, currentCircuitName, coord);
            if (dbResult) {
                System.out.println("Removed input at coordinate " + coord + " from database for circuit " + currentCircuitName);
            }
        }
    }

    public void loadInputsFromDatabase() {
        try {
            System.out.println("=== LOADING INPUTS FROM DATABASE START ===");
            System.out.println("Loading for Username: " + currentUsername + ", Circuit: " + currentCircuitName);

            List<InputData> dbInputs = inputToDB.loadInputsForCircuit(currentUsername, currentCircuitName);
            System.out.println("Database returned " + dbInputs.size() + " inputs");

            for (InputData inputData : dbInputs) {
                // Additional safety check: only load inputs that match the current circuit name and username
                if (!inputData.circuitName.equals(currentCircuitName) || !inputData.username.equals(currentUsername)) {
                    System.out.println("Skipping input " + inputData.name + " - belongs to different circuit/user: " + inputData.circuitName + "/" + inputData.username);
                    continue; // Skip this input as it doesn't belong to the current circuit or user
                }

                Coordinate coord = inputData.getCoordinate();
                System.out.println("Processing input " + inputData.name + " at coordinate " + coord);

                // Restore the input in memory
                if (!inputs.contains(coord)) {
                    inputs.add(coord);
                    System.out.println("Added coordinate " + coord + " to inputs list");
                }

                // Set pin attributes with default LOW value (0)
                pinAttributes[coord.s][coord.r][coord.c] = new Attribute(-2, 0);

                // Store input info in memory with default LOW value
                inputNames.put(coord, new InputInfo(inputData.name, 0)); // Always start with LOW
                System.out.println("Stored InputInfo for " + inputData.name + " at " + coord);

                // Restore visual representation
                mainActivity.resizeSpecialPin(coord, R.drawable.breadboard_inpt);
                createInputLabel(coord, inputData.name);

                System.out.println("Loaded input " + inputData.name + " from database at " + coord + " for circuit " + currentCircuitName + " with default LOW value");
            }

            System.out.println("Final inputs list size: " + inputs.size());
            System.out.println("Final inputNames map size: " + inputNames.size());

            // Update display immediately after loading all inputs
            updateInputDisplay();

            // ADDED: Ensure display sync after a delay to handle context switching
            inputDisplayContainer.postDelayed(() -> {
                ensureInputDisplaySync();
            }, 100);

            System.out.println("Loaded " + dbInputs.size() + " inputs from database for circuit " + currentCircuitName + " - all with default LOW values");
            System.out.println("=== LOADING INPUTS FROM DATABASE END ===");

        } catch (Exception e) {
            System.err.println("Error loading inputs from database for circuit " + currentCircuitName + ": " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void syncInputsToDatabase() {
        try {
            List<InputData> currentInputs = new ArrayList<>();

            for (Coordinate coord : inputs) {
                InputInfo info = getInputInfo(coord);
                if (info != null) {
                    // Create InputData without value (constructor will need to be updated)
                    currentInputs.add(new InputData(info.name, currentUsername, currentCircuitName,
                            coord.s, coord.r, coord.c));
                }
            }

            boolean result = inputToDB.syncInputsForCircuit(currentUsername, currentCircuitName, currentInputs);
            if (result) {
                System.out.println("Successfully synced " + currentInputs.size() + " inputs to database for circuit " + currentCircuitName + " (names only)");
            } else {
                System.err.println("Failed to sync inputs to database for circuit " + currentCircuitName);
            }

        } catch (Exception e) {
            System.err.println("Error syncing inputs to database for circuit " + currentCircuitName + ": " + e.getMessage());
            e.printStackTrace();
        }
    }


    public void clearInputsFromDatabase() {
        boolean result = inputToDB.clearInputsForCircuit(currentUsername, currentCircuitName);
        if (result) {
            System.out.println("Cleared all inputs from database for circuit " + currentCircuitName);
        } else {
            System.err.println("Failed to clear inputs from database for circuit " + currentCircuitName);
        }
    }

    public void ensureInputDisplaySync() {
        if (inputDisplayContainer != null && !inputs.isEmpty()) {
            int containerChildCount = inputDisplayContainer.getChildCount();
            int inputsCount = inputs.size();

            System.out.println("Display sync check - Container children: " + containerChildCount + ", Inputs: " + inputsCount);

            if (containerChildCount == 0 && inputsCount > 0) {
                System.out.println("Input display out of sync - recreating buttons");
                updateInputDisplay();
            }
        }
    }

    public void updateCircuitContext(String username, String circuitName) {
        System.out.println("InputManager: Updating circuit context from [" + currentUsername + ", " + currentCircuitName +
                "] to [" + username + ", " + circuitName + "]");

        // CHANGED: Only clear if we're actually switching to a different circuit
        boolean isActualSwitch = !username.equals(currentUsername) || !circuitName.equals(currentCircuitName);

        if (isActualSwitch) {
            // Clear all in-memory data before switching context
            clearInMemoryInputData();
            clearInputVisuals();

            System.out.println("Cleared data for actual circuit switch");
        } else {
            System.out.println("No actual circuit switch detected - skipping data clear");
        }

        // Update context
        this.currentUsername = username;
        this.currentCircuitName = circuitName;

        System.out.println("InputManager context updated successfully - Username: " + username + ", Circuit: " + circuitName);

        // ADDED: If this was not an actual switch, ensure display is updated
        if (!isActualSwitch && inputDisplayContainer != null && inputDisplayContainer.getChildCount() == 0 && !inputs.isEmpty()) {
            System.out.println("Recreating input display after context refresh");
            updateInputDisplay();
        }
    }

    public String getCurrentCircuitName() {
        return currentCircuitName;
    }

    public String getCurrentUsername() {
        return currentUsername;
    }


    // New method to get database instance for external use
    public InputToDB getInputToDB() {
        if (inputToDB == null) {
            inputToDB = new InputToDB(mainActivity);
        }
        return inputToDB;
    }
    public void setInputName(Coordinate coord, String name) {
        inputNames.put(coord, new InputInfo(name, 0)); // Default value is 0 (LOW)
    }

    public InputInfo getInputInfo(Coordinate coord) {
        return inputNames.get(coord);
    }

    public String getInputName(Coordinate coord) {
        InputInfo info = inputNames.get(coord);
        return info != null ? info.name : null;
    }

    private void showToast(String message) {
        Toast.makeText(mainActivity, message, Toast.LENGTH_SHORT).show();
    }

    public void debugDatabase() {
        System.out.println("=== DATABASE DEBUG ===");

        // Check what tables exist
        if (inputToDB != null) {
            DBHelper dbHelper = new DBHelper(mainActivity);
            dbHelper.checkTables(); // This calls the new method in DBHelper

            // Try to get a database connection
            try {
                android.database.sqlite.SQLiteDatabase db = dbHelper.getReadableDatabase();
                System.out.println("Database connection successful");

                // Check specifically for inputs table
                android.database.Cursor cursor = db.rawQuery("SELECT name FROM sqlite_master WHERE type='table' AND name='inputs'", null);
                if (cursor.moveToFirst()) {
                    System.out.println("inputs table EXISTS in database");
                } else {
                    System.out.println("inputs table DOES NOT EXIST in database");
                }
                cursor.close();
                db.close();

            } catch (Exception e) {
                System.err.println("Database error: " + e.getMessage());
                e.printStackTrace();
            }
        }

        System.out.println("=== END DEBUG ===");
    }

    // Clear all in-memory input data
    public void clearInMemoryInputData() {
        System.out.println("Clearing in-memory input data for context switch");

        // Clear the input coordinate list
        inputs.clear();

        // Clear the input name/value mappings
        inputNames.clear();

        // Clear the input label references
        inputLabels.clear();

        // Clear the input display container
        if (inputDisplayContainer != null) {
            inputDisplayContainer.removeAllViews();
        }

        System.out.println("In-memory input data cleared successfully");
    }

    public void clearInputVisuals() {
        System.out.println("Clearing input visual elements from breadboard");

        // Remove input labels from the grid and reset pins
        for (Coordinate coord : new ArrayList<>(inputs)) { // Create copy to avoid concurrent modification
            try {
                // Reset pin attributes
                if (pinAttributes != null && coord.s < pinAttributes.length &&
                        coord.r < pinAttributes[coord.s].length && coord.c < pinAttributes[coord.s][coord.r].length) {
                    pinAttributes[coord.s][coord.r][coord.c] = new Attribute(-1, -1);
                }

                // Remove the input label if it exists
                TextView label = inputLabels.get(coord);
                if (label != null && label.getParent() != null) {
                    // The label is inside a FrameLayout, so we need to handle this properly
                    // This will be handled by MainActivity's pin reset functionality
                    mainActivity.removeValue(coord);
                }

            } catch (Exception e) {
                System.err.println("Error clearing visual for input at " + coord + ": " + e.getMessage());
            }
        }

        System.out.println("Input visual elements cleared successfully");
    }
}