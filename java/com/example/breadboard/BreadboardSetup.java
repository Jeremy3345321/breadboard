package com.example.breadboard;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.GridLayout;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.core.content.ContextCompat;

import com.example.breadboard.model.Attribute;
import com.example.breadboard.model.Coordinate;

public class BreadboardSetup {
    private Context context;
    private GridLayout topGrid, middleGrid, bottomGrid;
    private ImageButton[][][] pins;
    private Attribute[][][] pinAttributes;
    private TextView[] topLabels, bottomLabels;
    private TextView[] rowLabels;

    // Constantsaaa
    private static final int ROWS = 5;
    private static final int COLS = 64;
    private static final int SECTIONS = 2;
    private static final char[] ROW_LABELS = {' ', 'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I', 'J', ' '};

    // Interface for pin click handling
    public interface OnPinClickListener {
        void onPinClicked(Coordinate coord);
    }

    private OnPinClickListener pinClickListener;

    public BreadboardSetup(Context context, GridLayout topGrid, GridLayout middleGrid, GridLayout bottomGrid,
                           ImageButton[][][] pins, Attribute[][][] pinAttributes,
                           TextView[] topLabels, TextView[] bottomLabels, TextView[] rowLabels) {
        this.context = context;
        this.topGrid = topGrid;
        this.middleGrid = middleGrid;
        this.bottomGrid = bottomGrid;
        this.pins = pins;
        this.pinAttributes = pinAttributes;
        this.topLabels = topLabels;
        this.bottomLabels = bottomLabels;
        this.rowLabels = rowLabels;
    }

    public void setPinClickListener(OnPinClickListener listener) {
        this.pinClickListener = listener;
    }

    public void setupGrids() {
        // Set up the grids where the pins will be placed
        topGrid.setColumnCount(COLS + 1);
        topGrid.setRowCount(1);

        middleGrid.setColumnCount(COLS + 1);
        middleGrid.setRowCount(11);

        bottomGrid.setColumnCount(COLS + 1);
        bottomGrid.setRowCount(1);
    }

    public void setupLabels() {
        // Set up the labels that will display the column of the breadboard
        TextView topCorner = new TextView(context);
        topCorner.setText(" ");
        topCorner.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
        topCorner.setTextSize(10);
        GridLayout.LayoutParams topCornerParams = new GridLayout.LayoutParams();
        topCornerParams.columnSpec = GridLayout.spec(0);
        topCornerParams.width = context.getResources().getDimensionPixelSize(R.dimen.pin_size);
        topCorner.setLayoutParams(topCornerParams);
        topGrid.addView(topCorner);

        // Setup column numbers for top grid (0, 1, 2, 3, ..., 63)
        for (int i = 0; i < COLS; i++) {
            TextView topLabel = new TextView(context);
            topLabel.setText(String.valueOf(i));
            topLabel.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
            topLabel.setTextSize(10);
            topLabels[i] = topLabel;

            GridLayout.LayoutParams topParams = new GridLayout.LayoutParams();
            topParams.columnSpec = GridLayout.spec(i + 1); // +1 to account for row label column
            topParams.width = context.getResources().getDimensionPixelSize(R.dimen.pin_size);
            topParams.setMargins(2, 2, 2, 2); // Same margins as pins
            topLabel.setLayoutParams(topParams);
            topGrid.addView(topLabel);
        }

        // Add corner label for bottom grid (empty space below row labels)
        TextView bottomCorner = new TextView(context);
        bottomCorner.setText(" ");
        bottomCorner.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
        bottomCorner.setTextSize(10);
        GridLayout.LayoutParams bottomCornerParams = new GridLayout.LayoutParams();
        bottomCornerParams.columnSpec = GridLayout.spec(0);
        bottomCornerParams.width = context.getResources().getDimensionPixelSize(R.dimen.pin_size);
        bottomCorner.setLayoutParams(bottomCornerParams);
        bottomGrid.addView(bottomCorner);

        // Setup column numbers for bottom grid - these will align with pin columns
        for (int i = 0; i < COLS; i++) {
            TextView bottomLabel = new TextView(context);
            bottomLabel.setText(String.valueOf(i));
            bottomLabel.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
            bottomLabel.setTextSize(10);
            bottomLabels[i] = bottomLabel;

            GridLayout.LayoutParams bottomParams = new GridLayout.LayoutParams();
            bottomParams.columnSpec = GridLayout.spec(i + 1); // +1 to account for row label column
            bottomParams.width = context.getResources().getDimensionPixelSize(R.dimen.pin_size);
            bottomParams.setMargins(2, 2, 2, 2); // Same margins as pins
            bottomLabel.setLayoutParams(bottomParams);
            bottomGrid.addView(bottomLabel);
        }
    }

    public void setupPins() {
        // Set up the pins to be used in the breadboard
        LayoutInflater inflater = LayoutInflater.from(context);

        // First, add row labels to the first column of middleGrid
        // Section 1 row labels (A-E)
        for (int i = 1; i <= 5; i++) {
            TextView rowLabel = new TextView(context);
            rowLabel.setText(String.valueOf(ROW_LABELS[i]));
            rowLabel.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
            rowLabel.setTextSize(12);

            GridLayout.LayoutParams params = new GridLayout.LayoutParams();
            params.rowSpec = GridLayout.spec(i - 1);
            params.columnSpec = GridLayout.spec(0);
            params.width = context.getResources().getDimensionPixelSize(R.dimen.pin_size);
            params.height = context.getResources().getDimensionPixelSize(R.dimen.pin_size);
            rowLabel.setLayoutParams(params);
            middleGrid.addView(rowLabel);
        }

        // Add gap row
        // This empty space is used for placement of ICs
        View gapView = new View(context);
        GridLayout.LayoutParams gapParams = new GridLayout.LayoutParams();
        gapParams.rowSpec = GridLayout.spec(5); // Row 5 is the gap
        gapParams.columnSpec = GridLayout.spec(0, COLS + 1); // Span all columns
        gapParams.width = GridLayout.LayoutParams.MATCH_PARENT;
        gapParams.height = context.getResources().getDimensionPixelSize(R.dimen.middle_height); // Use IC height for gap
        gapView.setLayoutParams(gapParams);
        middleGrid.addView(gapView);

        // Section 2 row labels (F-J)
        for (int i = 6; i <= 10; i++) {
            TextView rowLabel = new TextView(context);
            rowLabel.setText(String.valueOf(ROW_LABELS[i]));
            rowLabel.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
            rowLabel.setTextSize(12);

            GridLayout.LayoutParams params = new GridLayout.LayoutParams();
            params.rowSpec = GridLayout.spec(i); // i instead of ROWS + i - 6 because gap is at row 5
            params.columnSpec = GridLayout.spec(0);
            params.width = context.getResources().getDimensionPixelSize(R.dimen.pin_size);
            params.height = context.getResources().getDimensionPixelSize(R.dimen.pin_size);
            rowLabel.setLayoutParams(params);
            middleGrid.addView(rowLabel);
        }

        // Now add pins with proper GridLayout positioning
        for (int section = 0; section < SECTIONS; section++) {
            for (int row = 0; row < ROWS; row++) {
                for (int col = 0; col < COLS; col++) {
                    // Create pin button
                    ImageButton pin = new ImageButton(context);
                    pin.setImageResource(R.drawable.breadboard_pin);
                    pin.setBackground(null);
                    pin.setScaleType(ImageButton.ScaleType.CENTER_INSIDE);

                    // Set layout parameters for GridLayout
                    GridLayout.LayoutParams params = new GridLayout.LayoutParams();
                    params.width = context.getResources().getDimensionPixelSize(R.dimen.pin_size);
                    params.height = context.getResources().getDimensionPixelSize(R.dimen.pin_size);
                    params.setMargins(2, -2, 2, -2);

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
                    pin.setOnClickListener(v -> {
                        if (pinClickListener != null) {
                            pinClickListener.onPinClicked(new Coordinate(s, r, c));
                        }
                    });

                    // Add to arrays
                    pins[section][row][col] = pin;
                    pinAttributes[section][row][col] = new Attribute(-1, -1);

                    // Add to grid
                    middleGrid.addView(pin);
                }
            }
        }
    }
}
