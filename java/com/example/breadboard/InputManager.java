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
import com.example.breadboard.model.Attribute;
import com.example.breadboard.model.Coordinate;
import java.util.HashMap;
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
    private LinearLayout inputDisplayContainer; // New field

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
                        LinearLayout inputDisplayContainer) {
        this.mainActivity = mainActivity;
        this.pins = pins;
        this.pinAttributes = pinAttributes;
        this.middleGrid = middleGrid;
        this.inputs = inputs;
        this.inputNames = inputNames;
        this.inputLabels = inputLabels;
        this.inputDisplayContainer = inputDisplayContainer;
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
        System.out.println("Name is already used");
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

        // Store the input name
        setInputName(coord, name);

        // Create and display the input name label
        createInputLabel(coord, name);

        // Update the input display
        updateInputDisplay();

        showToast("Input '" + name + "' created successfully!");
        System.out.println("Input " + name + " created");
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
            System.out.println("Toggled input at " + coord + " to value=" + info.value);
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
}