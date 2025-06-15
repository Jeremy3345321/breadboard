package com.example.breadboard;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.breadboard.CircuitToDB;
import com.example.breadboard.CircuitToDB.CircuitData;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.List;

public class HomeActivity extends AppCompatActivity {

    FloatingActionButton newCircuit;
    LinearLayout circuitContainer;
    private int circuitCounter = 1;
    private CircuitToDB circuitToDB;
    private String currentUsername;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        // Initialize components
        initializeComponents();

        // Get current username from SharedPreferences or Intent
        getCurrentUsername();

        // Load existing circuits for the user
        loadUserCircuits();

        newCircuit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Create a new circuit in database and UI
                createNewCircuit();
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Reload circuits when returning to this activity
        // This ensures the UI is updated if circuits were modified in MainActivity
        loadUserCircuits();
    }

    private void initializeComponents() {
        newCircuit = (FloatingActionButton) findViewById(R.id.floatingBtn);
        circuitContainer = (LinearLayout) findViewById(R.id.circuitContainer);
        circuitToDB = new CircuitToDB(this);
    }

    private void getCurrentUsername() {
        // Use UserAuthentication to get current user
        UserAuthentication userAuth = UserAuthentication.getInstance(this);
        currentUsername = userAuth.getCurrentUsername();

        // If no user is logged in through UserAuthentication, try to get from Intent as fallback
        if (currentUsername == null) {
            currentUsername = getIntent().getStringExtra("username");

            // If found in Intent, update UserAuthentication
            if (currentUsername != null) {
                userAuth.loginUser(currentUsername);
            }
        }

        // If still null, handle as no user logged in
        if (currentUsername == null) {
            System.err.println("ERROR: No user is currently logged in!");
            // You might want to redirect to login screen here
            Toast.makeText(this, "Please log in again", Toast.LENGTH_SHORT).show();

            // Redirect to login activity
            Intent intent = new Intent(this, LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
            return;
        }

        // Print session info for debugging
        userAuth.printSessionInfo();
        System.out.println("HomeActivity: Current username confirmed: " + currentUsername);
    }

    private void createNewCircuit() {
        try {
            // Verify current user before creating circuit
            UserAuthentication userAuth = UserAuthentication.getInstance(this);
            String verifiedUsername = userAuth.getCurrentUsername();

            if (verifiedUsername == null) {
                showToast("Please log in again");
                logoutUser();
                return;
            }

            // Update currentUsername to verified username
            currentUsername = verifiedUsername;

            // Generate a unique circuit name for this user
            String circuitName = circuitToDB.generateUniqueCircuitName(currentUsername);

            // Insert circuit into database
            boolean dbResult = circuitToDB.insertCircuit(circuitName, currentUsername);

            if (dbResult) {
                // Reload circuits to update the UI with the new circuit
                loadUserCircuits();

                // Navigate to MainActivity for circuit creation
                Intent intent = new Intent(HomeActivity.this, MainActivity.class);
                intent.putExtra("circuit_name", circuitName);
                intent.putExtra("username", currentUsername);
                startActivity(intent);

                showToast("Circuit '" + circuitName + "' created successfully!");
                System.out.println("Created new circuit: " + circuitName + " for user: " + currentUsername);
            } else {
                showToast("Failed to create circuit in database");
                System.err.println("Failed to create circuit in database");
            }

        } catch (Exception e) {
            showToast("Error creating circuit");
            System.err.println("Error creating circuit: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void loadUserCircuits() {
        try {
            List<CircuitData> userCircuits = circuitToDB.getCircuitsForUser(currentUsername);

            // Clear existing circuit items
            circuitContainer.removeAllViews();

            // Create UI items for each circuit
            for (CircuitData circuit : userCircuits) {
                createCircuitItemFromDatabase(circuit);
            }

            // Update circuit counter for next circuit
            circuitCounter = circuitToDB.getNextCircuitNumber(currentUsername);

            System.out.println("Loaded " + userCircuits.size() + " circuits for user: " + currentUsername);

        } catch (Exception e) {
            showToast("Error loading circuits");
            System.err.println("Error loading circuits: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Create a circuit item in the UI from database data
     */
    private void createCircuitItemFromDatabase(CircuitData circuitData) {
        // Create the main container for the circuit item
        LinearLayout circuitItem = new LinearLayout(this);
        circuitItem.setOrientation(LinearLayout.VERTICAL);
        
        // Set layout parameters with margins
        LinearLayout.LayoutParams itemParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        itemParams.setMargins(0, 0, 0, 50); // Bottom margin between items
        circuitItem.setLayoutParams(itemParams);
        
        // Create and configure the circuit name TextView
        TextView circuitName = new TextView(this);
        circuitName.setText(circuitData.circuitName);
        circuitName.setTextSize(18);
        circuitName.setTextColor(Color.parseColor("#000000"));
        circuitName.setPadding(50, 70, 50, 37);
        
        // Create the background drawable with border and fill
        GradientDrawable background = new GradientDrawable();
        background.setShape(GradientDrawable.RECTANGLE);
        background.setColor(Color.parseColor("#FFFAF0")); // Light cream color
        background.setStroke(6, Color.parseColor("#000000")); // Black border
        background.setCornerRadius(8); // Rounded corners
        
        // Set the background to the TextView
        circuitName.setBackground(background);
        
        // Set layout parameters for the TextView
        LinearLayout.LayoutParams textParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        circuitName.setLayoutParams(textParams);
        
        // Add click listener to open MainActivity with specific circuit data
        circuitItem.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(HomeActivity.this, MainActivity.class);
                intent.putExtra("circuit_name", circuitData.circuitName);
                intent.putExtra("circuit_id", circuitData.id);
                intent.putExtra("username", currentUsername);
                startActivity(intent);
            }
        });
        
        // Add long click listener for additional options (delete, rename, etc.)
        circuitItem.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                showCircuitOptions(circuitData);
                return true; // Consume the long click event
            }
        });
        
        // Add the TextView to the circuit item container
        circuitItem.addView(circuitName);
        
        // Add the circuit item to the main container
        circuitContainer.addView(circuitItem);
    }

    /**
     * Show options for a circuit (delete, rename, etc.)
     */
    private void showCircuitOptions(CircuitData circuitData) {
        // For now, just show a simple delete confirmation
        // You can expand this to show a dialog with multiple options
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
        builder.setTitle("Circuit Options")
                .setMessage("What would you like to do with '" + circuitData.circuitName + "'?")
                .setPositiveButton("Delete", (dialog, which) -> deleteCircuit(circuitData))
                .setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss())
                .setNeutralButton("Open", (dialog, which) -> {
                    Intent intent = new Intent(HomeActivity.this, MainActivity.class);
                    intent.putExtra("circuit_name", circuitData.circuitName);
                    intent.putExtra("circuit_id", circuitData.id);
                    intent.putExtra("username", currentUsername);
                    startActivity(intent);
                })
                .show();
    }

    /**
     * Delete a circuit from database and update UI
     */
    private void deleteCircuit(CircuitData circuitData) {
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
        builder.setTitle("Delete Circuit")
                .setMessage("Are you sure you want to delete '" + circuitData.circuitName + "'? This action cannot be undone.")
                .setPositiveButton("Delete", (dialog, which) -> {
                    boolean success = circuitToDB.deleteCircuit(circuitData.id);
                    if (success) {
                        showToast("Circuit '" + circuitData.circuitName + "' deleted successfully");
                        loadUserCircuits(); // Refresh the UI
                    } else {
                        showToast("Failed to delete circuit");
                    }
                })
                .setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss())
                .show();
    }

    /**
     * Create a circuit item in the UI (generic version for backward compatibility)
     * This method is kept for any legacy calls, but createCircuitItemFromDatabase should be used instead
     */
    private void createCircuitItem(String circuitName) {
        // Create a temporary CircuitData object for display
        CircuitData tempCircuitData = new CircuitData(0, circuitName, currentUsername, "", "");
        createCircuitItemFromDatabase(tempCircuitData);
    }

    /**
     * Show a toast message to the user
     */
    private void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    /**
     * Debug method to clear all circuits for current user (for testing)
     */
    private void clearAllUserCircuits() {
        boolean success = circuitToDB.clearCircuitsForUser(currentUsername);
        if (success) {
            showToast("All circuits cleared for user: " + currentUsername);
            loadUserCircuits(); // Refresh UI
        } else {
            showToast("Failed to clear circuits");
        }
    }

    /**
     * Debug method to show database info (for testing)
     */
    private void debugDatabaseInfo() {
        int circuitCount = circuitToDB.getCircuitCountForUser(currentUsername);
        System.out.println("Current user has " + circuitCount + " circuits");
        
        List<CircuitData> allCircuits = circuitToDB.getAllCircuits();
        System.out.println("Total circuits in database: " + allCircuits.size());
        
        for (CircuitData circuit : allCircuits) {
            System.out.println("Circuit: " + circuit.circuitName + " (User: " + circuit.username + ")");
        }
    }

    private void logoutUser() {
        UserAuthentication userAuth = UserAuthentication.getInstance(this);
        userAuth.logoutUser();

        // Redirect to login screen
        Intent intent = new Intent(this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}