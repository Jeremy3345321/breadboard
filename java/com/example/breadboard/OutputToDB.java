package com.example.breadboard;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.example.breadboard.model.Coordinate;

import java.util.ArrayList;
import java.util.List;

public class OutputToDB {
    private DBHelper dbHelper;
    private Context context;

    public OutputToDB(Context context) {
        this.context = context;
        this.dbHelper = new DBHelper(context);
    }

    public static class OutputData {
        public int id;
        public String username;
        public String circuitName;
        public int section;
        public int row_pos;
        public int column_pos;

        public OutputData(int id, String username, String circuitName, int section, int row, int column) {
            this.id = id;
            this.username = username;
            this.circuitName = circuitName;
            this.section = section;
            this.row_pos = row;
            this.column_pos = column;
        }

        public OutputData(String username, String circuitName, int section, int row, int column) {
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

    public boolean insertOutput(String username, String circuitName, Coordinate coord) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();

        values.put("username", username);
        values.put("circuit_name", circuitName);
        values.put("section", coord.s);
        values.put("row_pos", coord.r);
        values.put("column_pos", coord.c);

        long result = db.insert("outputs", null, values);
        db.close();

        return result != -1;
    }

    /**
     * Update an existing output's position for a specific circuit
     */
    public boolean updateOutputPosition(String username, String circuitName, Coordinate oldCoord, Coordinate newCoord) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();

        values.put("section", newCoord.s);
        values.put("row_pos", newCoord.r);
        values.put("column_pos", newCoord.c);

        int rowsAffected = db.update("outputs", values,
                "username = ? AND circuit_name = ? AND section = ? AND row_pos = ? AND column_pos = ?",
                new String[]{username, circuitName, String.valueOf(oldCoord.s),
                        String.valueOf(oldCoord.r), String.valueOf(oldCoord.c)});
        db.close();

        return rowsAffected > 0;
    }

    /**
     * Delete an output by coordinate from a specific circuit
     */
    public boolean deleteOutputByCoordinate(String username, String circuitName, Coordinate coord) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        int rowsDeleted = db.delete("outputs",
                "username = ? AND circuit_name = ? AND section = ? AND row_pos = ? AND column_pos = ?",
                new String[]{username, circuitName, String.valueOf(coord.s),
                        String.valueOf(coord.r), String.valueOf(coord.c)});
        db.close();

        return rowsDeleted > 0;
    }

    /**
     * Get all outputs from a specific circuit
     */
    public List<OutputData> getOutputsForCircuit(String username, String circuitName) {
        List<OutputData> outputList = new ArrayList<>();
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT * FROM outputs WHERE username = ? AND circuit_name = ? ORDER BY section, row_pos, column_pos",
                new String[]{username, circuitName});

        if (cursor.moveToFirst()) {
            do {
                OutputData outputData = new OutputData(
                        cursor.getInt(cursor.getColumnIndexOrThrow("id")),
                        cursor.getString(cursor.getColumnIndexOrThrow("username")),
                        cursor.getString(cursor.getColumnIndexOrThrow("circuit_name")),
                        cursor.getInt(cursor.getColumnIndexOrThrow("section")),
                        cursor.getInt(cursor.getColumnIndexOrThrow("row_pos")),
                        cursor.getInt(cursor.getColumnIndexOrThrow("column_pos"))
                );
                outputList.add(outputData);
            } while (cursor.moveToNext());
        }

        cursor.close();
        db.close();

        return outputList;
    }

    /**
     * Clear all outputs from a specific circuit
     */
    public boolean clearOutputsForCircuit(String username, String circuitName) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        int rowsDeleted = db.delete("outputs", "username = ? AND circuit_name = ?",
                new String[]{username, circuitName});
        db.close();

        return rowsDeleted >= 0;
    }

    /**
     * Load outputs from database for a specific circuit
     */
    public List<OutputData> loadOutputsForCircuit(String username, String circuitName) {
        return getOutputsForCircuit(username, circuitName);
    }

    /**
     * Save current circuit outputs to database
     */
    public boolean syncOutputsForCircuit(String username, String circuitName, List<OutputData> currentOutputs) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        db.beginTransaction();

        try {
            // Clear existing outputs for this circuit
            db.delete("outputs", "username = ? AND circuit_name = ?",
                    new String[]{username, circuitName});

            // Insert current outputs
            ContentValues values = new ContentValues();
            for (OutputData output : currentOutputs) {
                values.clear();
                values.put("username", username);
                values.put("circuit_name", circuitName);
                values.put("section", output.section);
                values.put("row_pos", output.row_pos);
                values.put("column_pos", output.column_pos);

                long result = db.insert("outputs", null, values);
                if (result == -1) {
                    throw new Exception("Failed to insert output at: " + output.getCoordinate());
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

    /**
     * Check if an output exists at a specific coordinate
     */
    public boolean outputExistsAt(String username, String circuitName, Coordinate coord) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT COUNT(*) FROM outputs WHERE username = ? AND circuit_name = ? AND section = ? AND row_pos = ? AND column_pos = ?",
                new String[]{username, circuitName, String.valueOf(coord.s),
                        String.valueOf(coord.r), String.valueOf(coord.c)});

        boolean exists = false;
        if (cursor.moveToFirst()) {
            exists = cursor.getInt(0) > 0;
        }

        cursor.close();
        db.close();
        return exists;
    }

    /**
     * Get output count for a specific circuit
     */
    public int getOutputCountForCircuit(String username, String circuitName) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT COUNT(*) FROM outputs WHERE username = ? AND circuit_name = ?",
                new String[]{username, circuitName});

        int count = 0;
        if (cursor.moveToFirst()) {
            count = cursor.getInt(0);
        }

        cursor.close();
        db.close();
        return count;
    }
}