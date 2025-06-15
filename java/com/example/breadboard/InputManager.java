package com.example.breadboard;

import android.app.AlertDialog;
import android.graphics.Color;
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
        this.inputToDB = getInputToDB();

        loadInputsFromDatabase();
    }

    public void showInputNameDialog(Coordinate coord) {
        AlertDialog.Builder builder = new AlertDialog.Builder(mainActivity);
        builder.setTitle("Name Input");

        // Create EditText for input
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

    public boolean isInputNameUsed(String name) {
        // Check both in-memory and database for this specific circuit
        boolean inMemory = false;
        for (Coordinate coord : inputs) {
            InputInfo info = getInputInfo(coord);
            if (info != null && name.equals(info.name)) {
                inMemory = true;
                break;
            }
        }

        boolean inDatabase = inputToDB.inputNameExists(name, currentUsername, currentCircuitName);

        System.out.println("Name check for circuit " + currentCircuitName + " - In memory: " + inMemory + ", In database: " + inDatabase);
        return inMemory || inDatabase;
    }

    public void createNamedInput(Coordinate coord, String name) {
        mainActivity.resizeSpecialPin(coord, R.drawable.breadboard_inpt);
        inputs.add(coord);
        pinAttributes[coord.s][coord.r][coord.c] = new Attribute(-2, 0);

        // Store the input name in memory
        setInputName(coord, name);

        // Save to database with circuit information
        boolean dbResult = inputToDB.insertInput(name, currentUsername, currentCircuitName, coord, 0);
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
        // Clear existing display
        inputDisplayContainer.removeAllViews();

        // Add each input to the display
        for (Coordinate coord : inputs) {
            InputInfo info = getInputInfo(coord);
            if (info != null) {
                createInputDisplayButton(coord, info);
            }
        }
    }

    private void createInputDisplayButton(Coordinate coord, InputInfo info) {
        // Create a button for the input
        Button inputButton = new Button(mainActivity);

        // Set button text and appearance based on current value
        updateInputButtonAppearance(inputButton, info);

        // Set button size and margins
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        params.setMargins(8, 4, 8, 4);
        inputButton.setLayoutParams(params);

        // Set click listener to toggle input value
        inputButton.setOnClickListener(v -> {
            toggleInputValue(coord);
            updateInputButtonAppearance(inputButton, getInputInfo(coord));
        });

        // Add button to container
        inputDisplayContainer.addView(inputButton);
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

            // Update database with circuit information
            boolean dbResult = inputToDB.updateInputValue(info.name, currentUsername, currentCircuitName, info.value);
            if (!dbResult) {
                System.err.println("Failed to update input " + info.name + " value in database for circuit " + currentCircuitName);
            } else {
                System.out.println("Updated input " + info.name + " value to " + info.value + " in database for circuit " + currentCircuitName);
            }

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

        // Add label to the FrameLayout (it will overlay the pin)
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
            List<InputData> dbInputs = inputToDB.loadInputsForCircuit(currentUsername, currentCircuitName);

            for (InputData inputData : dbInputs) {
                Coordinate coord = inputData.getCoordinate();

                // Restore the input in memory
                if (!inputs.contains(coord)) {
                    inputs.add(coord);
                }

                // Set pin attributes
                pinAttributes[coord.s][coord.r][coord.c] = new Attribute(-2, inputData.value);

                // Store input info in memory
                inputNames.put(coord, new InputInfo(inputData.name, inputData.value));

                // Restore visual representation
                mainActivity.resizeSpecialPin(coord, R.drawable.breadboard_inpt);
                createInputLabel(coord, inputData.name);

                System.out.println("Loaded input " + inputData.name + " from database at " + coord + " for circuit " + currentCircuitName);
            }

            // Update display after loading all inputs
            updateInputDisplay();

            System.out.println("Loaded " + dbInputs.size() + " inputs from database for circuit " + currentCircuitName);

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
                    currentInputs.add(new InputData(info.name, currentUsername, currentCircuitName,
                            coord.s, coord.r, coord.c, info.value));
                }
            }

            boolean result = inputToDB.syncInputsForCircuit(currentUsername, currentCircuitName, currentInputs);
            if (result) {
                System.out.println("Successfully synced " + currentInputs.size() + " inputs to database for circuit " + currentCircuitName);
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

    public void updateCircuitContext(String username, String circuitName) {
        this.currentUsername = username;
        this.currentCircuitName = circuitName;
        System.out.println("InputManager context updated - Username: " + username + ", Circuit: " + circuitName);
    }

    public String getCurrentCircuitName() {
        return currentCircuitName;
    }

    public String getCurrentUsername() {
        return currentUsername;
    }


    // New method to get database instance for external use
    public InputToDB getInputToDB() {
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
}