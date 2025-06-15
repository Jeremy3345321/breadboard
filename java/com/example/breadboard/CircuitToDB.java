package com.example.breadboard;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import java.util.ArrayList;
import java.util.List;

public class CircuitToDB {
    private DBHelper dbHelper;
    private Context context;

    public static class CircuitData {
        public int id;
        public String circuitName;
        public String username;
        public String createdDate;
        public String lastModified;

        public CircuitData(int id, String circuitName, String username, String createdDate, String lastModified) {
            this.id = id;
            this.circuitName = circuitName;
            this.username = username;
            this.createdDate = createdDate;
            this.lastModified = lastModified;
        }

        public CircuitData(String circuitName, String username, String createdDate, String lastModified) {
            this.circuitName = circuitName;
            this.username = username;
            this.createdDate = createdDate;
            this.lastModified = lastModified;
        }

        public CircuitData(String circuitName, String username) {
            this.circuitName = circuitName;
            this.username = username;
            // Set current timestamp for both created and modified dates
            long currentTime = System.currentTimeMillis();
            this.createdDate = String.valueOf(currentTime);
            this.lastModified = String.valueOf(currentTime);
        }
    }

    public CircuitToDB(Context context) {
        this.context = context;
        this.dbHelper = new DBHelper(context);
    }

    /**
     * Insert a new circuit into the database
     */
    public boolean insertCircuit(String circuitName, String username) {
        SQLiteDatabase db = null;
        boolean success = false;
        
        try {
            db = dbHelper.getWritableDatabase();
            ContentValues values = new ContentValues();
            
            long currentTime = System.currentTimeMillis();
            String timestamp = String.valueOf(currentTime);
            
            values.put("circuit_name", circuitName);
            values.put("username", username);
            values.put("created_date", timestamp);
            values.put("last_modified", timestamp);
            
            long result = db.insert("circuits", null, values);
            success = result != -1;
            
            if (success) {
                System.out.println("Circuit '" + circuitName + "' inserted successfully for user '" + username + "'");
            } else {
                System.err.println("Failed to insert circuit '" + circuitName + "' for user '" + username + "'");
            }
            
        } catch (Exception e) {
            System.err.println("Error inserting circuit: " + e.getMessage());
            e.printStackTrace();
        } finally {
            if (db != null) {
                db.close();
            }
        }
        
        return success;
    }

    /**
     * Update circuit's last modified timestamp
     */
    public boolean updateCircuitModified(int circuitId) {
        SQLiteDatabase db = null;
        boolean success = false;
        
        try {
            db = dbHelper.getWritableDatabase();
            ContentValues values = new ContentValues();
            
            long currentTime = System.currentTimeMillis();
            values.put("last_modified", String.valueOf(currentTime));
            
            int rowsAffected = db.update("circuits", values, "id = ?", new String[]{String.valueOf(circuitId)});
            success = rowsAffected > 0;
            
        } catch (Exception e) {
            System.err.println("Error updating circuit modified time: " + e.getMessage());
            e.printStackTrace();
        } finally {
            if (db != null) {
                db.close();
            }
        }
        
        return success;
    }

    /**
     * Update circuit name
     */
    public boolean updateCircuitName(int circuitId, String newName) {
        SQLiteDatabase db = null;
        boolean success = false;
        
        try {
            db = dbHelper.getWritableDatabase();
            ContentValues values = new ContentValues();
            
            values.put("circuit_name", newName);
            values.put("last_modified", String.valueOf(System.currentTimeMillis()));
            
            int rowsAffected = db.update("circuits", values, "id = ?", new String[]{String.valueOf(circuitId)});
            success = rowsAffected > 0;
            
        } catch (Exception e) {
            System.err.println("Error updating circuit name: " + e.getMessage());
            e.printStackTrace();
        } finally {
            if (db != null) {
                db.close();
            }
        }
        
        return success;
    }

    /**
     * Delete a circuit from the database
     */
    public boolean deleteCircuit(int circuitId) {
        SQLiteDatabase db = null;
        boolean success = false;
        
        try {
            db = dbHelper.getWritableDatabase();
            int rowsDeleted = db.delete("circuits", "id = ?", new String[]{String.valueOf(circuitId)});
            success = rowsDeleted > 0;
            
            if (success) {
                System.out.println("Circuit with ID " + circuitId + " deleted successfully");
            }
            
        } catch (Exception e) {
            System.err.println("Error deleting circuit: " + e.getMessage());
            e.printStackTrace();
        } finally {
            if (db != null) {
                db.close();
            }
        }
        
        return success;
    }

    /**
     * Delete a circuit by name and username
     */
    public boolean deleteCircuitByName(String circuitName, String username) {
        SQLiteDatabase db = null;
        boolean success = false;
        
        try {
            db = dbHelper.getWritableDatabase();
            int rowsDeleted = db.delete("circuits", "circuit_name = ? AND username = ?", 
                new String[]{circuitName, username});
            success = rowsDeleted > 0;
            
        } catch (Exception e) {
            System.err.println("Error deleting circuit by name: " + e.getMessage());
            e.printStackTrace();
        } finally {
            if (db != null) {
                db.close();
            }
        }
        
        return success;
    }

    /**
     * Check if a circuit name already exists for a specific user
     */
    public boolean circuitNameExistsForUser(String circuitName, String username) {
        SQLiteDatabase db = null;
        Cursor cursor = null;
        boolean exists = false;
        
        try {
            db = dbHelper.getReadableDatabase();
            cursor = db.rawQuery("SELECT * FROM circuits WHERE circuit_name = ? AND username = ?", 
                new String[]{circuitName, username});
            exists = cursor.getCount() > 0;
            
        } catch (Exception e) {
            System.err.println("Error checking if circuit name exists: " + e.getMessage());
            // If there's an error (like table doesn't exist), return false
            exists = false;
        } finally {
            if (cursor != null) {
                cursor.close();
            }
            if (db != null) {
                db.close();
            }
        }
        
        return exists;
    }

    /**
     * Get circuit data by ID
     */
    public CircuitData getCircuitById(int circuitId) {
        SQLiteDatabase db = null;
        Cursor cursor = null;
        CircuitData circuitData = null;
        
        try {
            db = dbHelper.getReadableDatabase();
            cursor = db.rawQuery("SELECT * FROM circuits WHERE id = ?", new String[]{String.valueOf(circuitId)});
            
            if (cursor.moveToFirst()) {
                circuitData = new CircuitData(
                    cursor.getInt(cursor.getColumnIndexOrThrow("id")),
                    cursor.getString(cursor.getColumnIndexOrThrow("circuit_name")),
                    cursor.getString(cursor.getColumnIndexOrThrow("username")),
                    cursor.getString(cursor.getColumnIndexOrThrow("created_date")),
                    cursor.getString(cursor.getColumnIndexOrThrow("last_modified"))
                );
            }
            
        } catch (Exception e) {
            System.err.println("Error getting circuit by ID: " + e.getMessage());
            e.printStackTrace();
        } finally {
            if (cursor != null) {
                cursor.close();
            }
            if (db != null) {
                db.close();
            }
        }
        
        return circuitData;
    }

    /**
     * Get all circuits for a specific user
     */
    public List<CircuitData> getCircuitsForUser(String username) {
        List<CircuitData> circuitList = new ArrayList<>();
        SQLiteDatabase db = null;
        Cursor cursor = null;
        
        try {
            db = dbHelper.getReadableDatabase();
            cursor = db.rawQuery("SELECT * FROM circuits WHERE username = ? ORDER BY last_modified DESC", 
                new String[]{username});
            
            if (cursor.moveToFirst()) {
                do {
                    CircuitData circuitData = new CircuitData(
                        cursor.getInt(cursor.getColumnIndexOrThrow("id")),
                        cursor.getString(cursor.getColumnIndexOrThrow("circuit_name")),
                        cursor.getString(cursor.getColumnIndexOrThrow("username")),
                        cursor.getString(cursor.getColumnIndexOrThrow("created_date")),
                        cursor.getString(cursor.getColumnIndexOrThrow("last_modified"))
                    );
                    circuitList.add(circuitData);
                } while (cursor.moveToNext());
            }
            
        } catch (Exception e) {
            System.err.println("Error getting circuits for user: " + e.getMessage());
            e.printStackTrace();
        } finally {
            if (cursor != null) {
                cursor.close();
            }
            if (db != null) {
                db.close();
            }
        }
        
        return circuitList;
    }

    /**
     * Get all circuits from the database
     */
    public List<CircuitData> getAllCircuits() {
        List<CircuitData> circuitList = new ArrayList<>();
        SQLiteDatabase db = null;
        Cursor cursor = null;
        
        try {
            db = dbHelper.getReadableDatabase();
            cursor = db.rawQuery("SELECT * FROM circuits ORDER BY username, last_modified DESC", null);
            
            if (cursor.moveToFirst()) {
                do {
                    CircuitData circuitData = new CircuitData(
                        cursor.getInt(cursor.getColumnIndexOrThrow("id")),
                        cursor.getString(cursor.getColumnIndexOrThrow("circuit_name")),
                        cursor.getString(cursor.getColumnIndexOrThrow("username")),
                        cursor.getString(cursor.getColumnIndexOrThrow("created_date")),
                        cursor.getString(cursor.getColumnIndexOrThrow("last_modified"))
                    );
                    circuitList.add(circuitData);
                } while (cursor.moveToNext());
            }
            
        } catch (Exception e) {
            System.err.println("Error getting all circuits: " + e.getMessage());
            e.printStackTrace();
        } finally {
            if (cursor != null) {
                cursor.close();
            }
            if (db != null) {
                db.close();
            }
        }
        
        return circuitList;
    }

    /**
     * Get the count of circuits for a specific user
     */
    public int getCircuitCountForUser(String username) {
        SQLiteDatabase db = null;
        Cursor cursor = null;
        int count = 0;
        
        try {
            db = dbHelper.getReadableDatabase();
            cursor = db.rawQuery("SELECT COUNT(*) FROM circuits WHERE username = ?", new String[]{username});
            
            if (cursor.moveToFirst()) {
                count = cursor.getInt(0);
            }
            
        } catch (Exception e) {
            System.err.println("Error getting circuit count: " + e.getMessage());
            e.printStackTrace();
        } finally {
            if (cursor != null) {
                cursor.close();
            }
            if (db != null) {
                db.close();
            }
        }
        
        return count;
    }

    /**
     * Clear all circuits for a specific user
     */
    public boolean clearCircuitsForUser(String username) {
        SQLiteDatabase db = null;
        boolean success = false;
        
        try {
            db = dbHelper.getWritableDatabase();
            int rowsDeleted = db.delete("circuits", "username = ?", new String[]{username});
            success = rowsDeleted >= 0;
            
            System.out.println("Cleared " + rowsDeleted + " circuits for user: " + username);
            
        } catch (Exception e) {
            System.err.println("Error clearing circuits for user: " + e.getMessage());
            e.printStackTrace();
        } finally {
            if (db != null) {
                db.close();
            }
        }
        
        return success;
    }

    /**
     * Clear all circuits from the database (useful for complete reset)
     */
    public boolean clearAllCircuits() {
        SQLiteDatabase db = null;
        boolean success = false;
        
        try {
            db = dbHelper.getWritableDatabase();
            int rowsDeleted = db.delete("circuits", null, null);
            success = rowsDeleted >= 0;
            
            System.out.println("Cleared all circuits from database");
            
        } catch (Exception e) {
            System.err.println("Error clearing all circuits: " + e.getMessage());
            e.printStackTrace();
        } finally {
            if (db != null) {
                db.close();
            }
        }
        
        return success;
    }

    /**
     * Load circuits from database for a specific user
     * Returns a list of CircuitData for display
     */
    public List<CircuitData> loadCircuitsFromDatabase(String username) {
        return getCircuitsForUser(username);
    }

    /**
     * Get next circuit number for a user (for auto-naming)
     */
    public int getNextCircuitNumber(String username) {
        int count = getCircuitCountForUser(username);
        return count + 1;
    }

    /**
     * Generate a unique circuit name for a user
     */
    public String generateUniqueCircuitName(String username) {
        int circuitNumber = getNextCircuitNumber(username);
        String baseName = "Circuit " + circuitNumber;
        
        // Check if this name already exists, increment if it does
        while (circuitNameExistsForUser(baseName, username)) {
            circuitNumber++;
            baseName = "Circuit " + circuitNumber;
        }
        
        return baseName;
    }
}