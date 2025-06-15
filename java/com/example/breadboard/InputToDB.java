package com.example.breadboard;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.example.breadboard.model.Coordinate;

import java.util.ArrayList;
import java.util.List;

public class InputToDB {
    private DBHelper dbHelper;
    private Context context;

    // InputToDB.java - Updated InputData class and all methods to support circuit association

    public InputToDB(Context context) {
        this.context = context;
        this.dbHelper = new DBHelper(context);
    }

    public static class InputData {
        public int id;
        public String name;
        public String username;
        public String circuitName;
        public int section;
        public int row_pos;
        public int column_pos;


        public InputData(int id, String name, String username, String circuitName, int section, int row, int column) {
            this.id = id;
            this.name = name;
            this.username = username;
            this.circuitName = circuitName;
            this.section = section;
            this.row_pos = row;
            this.column_pos = column;
        }

        public InputData(String name, String username, String circuitName, int section, int row, int column) {
            this.name = name;
            this.username = username;
            this.circuitName = circuitName;
            this.section = section;
            this.row_pos = row;
            this.column_pos = column;
        }

        public Coordinate getCoordinate() {
            return new Coordinate(section, row_pos, column_pos);
        }
    }

    /**
     * Insert a new input into the database for a specific circuit
     */
    public boolean insertInput(String name, String username, String circuitName, Coordinate coord) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();

        values.put("name", name);
        values.put("username", username);
        values.put("circuit_name", circuitName);
        values.put("section", coord.s);
        values.put("row_pos", coord.r);
        values.put("column_pos", coord.c);

        long result = db.insert("inputs", null, values);
        db.close();

        return result != -1;
    }

    /**
     * Update an existing input's position for a specific circuit
     */
    public boolean updateInputPosition(String name, String username, String circuitName, Coordinate newCoord) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();

        values.put("section", newCoord.s);
        values.put("row_pos", newCoord.r);
        values.put("column_pos", newCoord.c);

        int rowsAffected = db.update("inputs", values,
                "name = ? AND username = ? AND circuit_name = ?",
                new String[]{name, username, circuitName});
        db.close();

        return rowsAffected > 0;
    }

    /**
     * Delete an input from a specific circuit
     */
    public boolean deleteInput(String name, String username, String circuitName) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        int rowsDeleted = db.delete("inputs",
                "name = ? AND username = ? AND circuit_name = ?",
                new String[]{name, username, circuitName});
        db.close();

        return rowsDeleted > 0;
    }

    /**
     * Delete an input by coordinate from a specific circuit
     */
    public boolean deleteInputByCoordinate(String username, String circuitName, Coordinate coord) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        int rowsDeleted = db.delete("inputs",
                "username = ? AND circuit_name = ? AND section = ? AND row_pos = ? AND column_pos = ?",
                new String[]{username, circuitName, String.valueOf(coord.s), String.valueOf(coord.r), String.valueOf(coord.c)});
        db.close();

        return rowsDeleted > 0;
    }

    /**
     * Check if an input name already exists in a specific circuit
     */
    public boolean inputNameExists(String name, String username, String circuitName) {
        SQLiteDatabase db = null;
        Cursor cursor = null;
        try {
            db = dbHelper.getReadableDatabase();
            cursor = db.rawQuery("SELECT * FROM inputs WHERE name = ? AND username = ? AND circuit_name = ?",
                    new String[]{name, username, circuitName});

            boolean exists = cursor.getCount() > 0;
            return exists;

        } catch (Exception e) {
            System.err.println("Error checking if input name exists: " + e.getMessage());
            return false;
        } finally {
            if (cursor != null) {
                cursor.close();
            }
            if (db != null) {
                db.close();
            }
        }
    }

    /**
     * Check if an input exists at the given coordinate in a specific circuit
     */
    public boolean inputExistsAtCoordinate(String username, String circuitName, Coordinate coord) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT * FROM inputs WHERE username = ? AND circuit_name = ? AND section = ? AND row_pos = ? AND column_pos = ?",
                new String[]{username, circuitName, String.valueOf(coord.s), String.valueOf(coord.r), String.valueOf(coord.c)});

        boolean exists = cursor.getCount() > 0;
        cursor.close();
        db.close();

        return exists;
    }

    /**
     * Get input data by name from a specific circuit
     */
    public InputData getInputByName(String name, String username, String circuitName) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT * FROM inputs WHERE name = ? AND username = ? AND circuit_name = ?",
                new String[]{name, username, circuitName});

        InputData inputData = null;
        if (cursor.moveToFirst()) {
            inputData = new InputData(
                    cursor.getInt(cursor.getColumnIndexOrThrow("id")),
                    cursor.getString(cursor.getColumnIndexOrThrow("name")),
                    cursor.getString(cursor.getColumnIndexOrThrow("username")),
                    cursor.getString(cursor.getColumnIndexOrThrow("circuit_name")),
                    cursor.getInt(cursor.getColumnIndexOrThrow("section")),
                    cursor.getInt(cursor.getColumnIndexOrThrow("row_pos")),
                    cursor.getInt(cursor.getColumnIndexOrThrow("column_pos"))
            );
        }

        cursor.close();
        db.close();

        return inputData;
    }

    /**
     * Get input data by coordinate from a specific circuit
     */
    public InputData getInputByCoordinate(String username, String circuitName, Coordinate coord) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT * FROM inputs WHERE username = ? AND circuit_name = ? AND section = ? AND row_pos = ? AND column_pos = ?",
                new String[]{username, circuitName, String.valueOf(coord.s), String.valueOf(coord.r), String.valueOf(coord.c)});

        InputData inputData = null;
        if (cursor.moveToFirst()) {
            inputData = new InputData(
                    cursor.getInt(cursor.getColumnIndexOrThrow("id")),
                    cursor.getString(cursor.getColumnIndexOrThrow("name")),
                    cursor.getString(cursor.getColumnIndexOrThrow("username")),
                    cursor.getString(cursor.getColumnIndexOrThrow("circuit_name")),
                    cursor.getInt(cursor.getColumnIndexOrThrow("section")),
                    cursor.getInt(cursor.getColumnIndexOrThrow("row_pos")),
                    cursor.getInt(cursor.getColumnIndexOrThrow("column_pos"))
            );
        }

        cursor.close();
        db.close();

        return inputData;
    }

    /**
     * Get all inputs from a specific circuit
     */
    public List<InputData> getInputsForCircuit(String username, String circuitName) {
        List<InputData> inputList = new ArrayList<>();
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT * FROM inputs WHERE username = ? AND circuit_name = ? ORDER BY name",
                new String[]{username, circuitName});

        if (cursor.moveToFirst()) {
            do {
                InputData inputData = new InputData(
                        cursor.getInt(cursor.getColumnIndexOrThrow("id")),
                        cursor.getString(cursor.getColumnIndexOrThrow("name")),
                        cursor.getString(cursor.getColumnIndexOrThrow("username")),
                        cursor.getString(cursor.getColumnIndexOrThrow("circuit_name")),
                        cursor.getInt(cursor.getColumnIndexOrThrow("section")),
                        cursor.getInt(cursor.getColumnIndexOrThrow("row_pos")),
                        cursor.getInt(cursor.getColumnIndexOrThrow("column_pos"))

                );
                inputList.add(inputData);
            } while (cursor.moveToNext());
        }

        cursor.close();
        db.close();

        return inputList;
    }

    /**
     * Clear all inputs from a specific circuit
     */
    public boolean clearInputsForCircuit(String username, String circuitName) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        int rowsDeleted = db.delete("inputs", "username = ? AND circuit_name = ?",
                new String[]{username, circuitName});
        db.close();

        return rowsDeleted >= 0;
    }

    /**
     * Get the count of inputs in a specific circuit
     */
    public int getInputCountForCircuit(String username, String circuitName) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT COUNT(*) FROM inputs WHERE username = ? AND circuit_name = ?",
                new String[]{username, circuitName});

        int count = 0;
        if (cursor.moveToFirst()) {
            count = cursor.getInt(0);
        }

        cursor.close();
        db.close();

        return count;
    }

    /**
     * Load inputs from database for a specific circuit
     */
    public List<InputData> loadInputsForCircuit(String username, String circuitName) {
        return getInputsForCircuit(username, circuitName);
    }

    /**
     * Save current circuit inputs to database
     */
    public boolean syncInputsForCircuit(String username, String circuitName, List<InputData> currentInputs) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        db.beginTransaction();

        try {
            // Clear existing inputs for this circuit
            db.delete("inputs", "username = ? AND circuit_name = ?",
                    new String[]{username, circuitName});

            // Insert current inputs
            ContentValues values = new ContentValues();
            for (InputData input : currentInputs) {
                values.clear();
                values.put("name", input.name);
                values.put("username", username);
                values.put("circuit_name", circuitName);
                values.put("section", input.section);
                values.put("row_pos", input.row_pos);
                values.put("column_pos", input.column_pos);

                long result = db.insert("inputs", null, values);
                if (result == -1) {
                    throw new Exception("Failed to insert input: " + input.name);
                }
            }

            db.setTransactionSuccessful();
            return true;

        } catch (Exception e) {
            e.printStackTrace();
            return false;
        } finally {
            db.endTransaction();
            db.close();
        }
    }
}