package com.example.breadboard;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * UserAuthentication class to manage user session and authentication state
 * This ensures proper user tracking across the application
 */
public class UserAuthentication {

    private static final String PREFS_NAME = "UserAuthPrefs";
    private static final String KEY_USERNAME = "current_username";
    private static final String KEY_IS_LOGGED_IN = "is_logged_in";
    private static final String KEY_LOGIN_TIMESTAMP = "login_timestamp";

    private Context context;
    private SharedPreferences sharedPreferences;
    private static UserAuthentication instance;

    // Private constructor for singleton pattern
    private UserAuthentication(Context context) {
        this.context = context.getApplicationContext();
        this.sharedPreferences = this.context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    /**
     * Get singleton instance of UserAuthentication
     */
    public static synchronized UserAuthentication getInstance(Context context) {
        if (instance == null) {
            instance = new UserAuthentication(context);
        }
        return instance;
    }

    /**
     * Login user and save session
     */
    public void loginUser(String username) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(KEY_USERNAME, username);
        editor.putBoolean(KEY_IS_LOGGED_IN, true);
        editor.putLong(KEY_LOGIN_TIMESTAMP, System.currentTimeMillis());
        editor.apply();

        System.out.println("UserAuthentication: User '" + username + "' logged in successfully");
    }

    /**
     * Logout current user and clear session
     */
    public void logoutUser() {
        String currentUser = getCurrentUsername();
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.clear();
        editor.apply();

        System.out.println("UserAuthentication: User '" + currentUser + "' logged out successfully");
    }

    /**
     * Get current logged in username
     */
    public String getCurrentUsername() {
        return sharedPreferences.getString(KEY_USERNAME, null);
    }

    /**
     * Check if any user is currently logged in
     */
    public boolean isUserLoggedIn() {
        return sharedPreferences.getBoolean(KEY_IS_LOGGED_IN, false) && getCurrentUsername() != null;
    }

    /**
     * Get login timestamp
     */
    public long getLoginTimestamp() {
        return sharedPreferences.getLong(KEY_LOGIN_TIMESTAMP, 0);
    }

    /**
     * Update current user (useful for account switching)
     */
    public void updateCurrentUser(String username) {
        if (username != null && !username.trim().isEmpty()) {
            loginUser(username);
        }
    }

    /**
     * Clear all authentication data (for debugging)
     */
    public void clearAuthData() {
        logoutUser();
    }

    /**
     * Get session info for debugging
     */
    public void printSessionInfo() {
        System.out.println("=== USER SESSION INFO ===");
        System.out.println("Current User: " + getCurrentUsername());
        System.out.println("Is Logged In: " + isUserLoggedIn());
        System.out.println("Login Timestamp: " + getLoginTimestamp());
        System.out.println("========================");
    }

    /**
     * Validate if username matches current session
     */
    public boolean validateCurrentUser(String username) {
        String currentUser = getCurrentUsername();
        return currentUser != null && currentUser.equals(username);
    }

    /**
     * Force set user (for migration or testing purposes)
     */
    public void forceSetUser(String username) {
        if (username != null) {
            loginUser(username);
            System.out.println("UserAuthentication: Force set user to '" + username + "'");
        }
    }
}
