package com.example.breadboard;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.example.breadboard.model.Coordinate;

import java.util.ArrayList;
import java.util.List;

public class ICToDB {
    private DBHelper dbHelper;
    private Context context;

    private Coordinate savedICs;

    public ICToDB(Context context) {
        this.context = context;
        this.dbHelper = new DBHelper(context);
    }

    public static class ICData {
        public int id;
        public String ic_type;
        public String username;
        public String circuitName;
        public int section;
        public int row_pos;
        public int column_pos;

        public ICData(int id, String icType, String username, String circuitName, int section, int row, int column) {
            this.id = id;
            this.ic_type = icType;
            this.username = username;
            this.circuitName = circuitName;
            this.section = section;
            this.row_pos = row;
            this.column_pos = column;
        }

        public ICData(String icType, String username, String circuitName, int section, int row, int column) {
            this.ic_type = icType;
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
     * Insert a new IC into the database for a specific circuit
     */
    public boolean insertIC(String icType, String username, String circuitName, Coordinate coord) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();

        values.put("ic_type", icType);
        values.put("username", username);
        values.put("circuit_name", circuitName);
        values.put("section", coord.s);
        values.put("row_pos", coord.r);
        values.put("column_pos", coord.c);

        long result = db.insert("ics", null, values);
        db.close();

        System.out.println("Inserted " + icType + " at " + coord);

        return result != -1;
    }

    /**
     * Update an existing IC's position for a specific circuit
     */
    public boolean updateICPosition(String icType, String username, String circuitName, Coordinate oldCoord, Coordinate newCoord) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();

        values.put("section", newCoord.s);
        values.put("row_pos", newCoord.r);
        values.put("column_pos", newCoord.c);

        int rowsAffected = db.update("ics", values,
                "ic_type = ? AND username = ? AND circuit_name = ? AND section = ? AND row_pos = ? AND column_pos = ?",
                new String[]{icType, username, circuitName, String.valueOf(oldCoord.s), String.valueOf(oldCoord.r), String.valueOf(oldCoord.c)});
        db.close();

        return rowsAffected > 0;
    }

    /**
     * Delete an IC from a specific circuit by coordinate
     */
    // Add this method to ICToDB.java if it doesn't exist
    public boolean deleteICByCoordinate(String username, String circuitName, Coordinate coord) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        int rowsDeleted = db.delete("ics",
                "username = ? AND circuit_name = ? AND section = ? AND row_pos = ? AND column_pos = ?",
                new String[]{username, circuitName, String.valueOf(coord.s),
                        String.valueOf(coord.r), String.valueOf(coord.c)});
        db.close();

        return rowsDeleted > 0;
    }

    public boolean deleteIC(String icType, String username, String circuitName, Coordinate coord) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        int rowsDeleted = db.delete("ics",
                "ic_type = ? AND username = ? AND circuit_name = ? AND section = ? AND row_pos = ? AND column_pos = ?",
                new String[]{icType, username, circuitName, String.valueOf(coord.s), String.valueOf(coord.r), String.valueOf(coord.c)});
        db.close();

        return rowsDeleted > 0;
    }

    /**
     * Get all ICs from a specific circuit
     */
    public List<ICData> getICsForCircuit(String username, String circuitName) {
        List<ICData> icList = new ArrayList<>();
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT * FROM ics WHERE username = ? AND circuit_name = ? ORDER BY ic_type",
                new String[]{username, circuitName});

        if (cursor.moveToFirst()) {
            do {
                ICData icData = new ICData(
                        cursor.getInt(cursor.getColumnIndexOrThrow("id")),
                        cursor.getString(cursor.getColumnIndexOrThrow("ic_type")),
                        cursor.getString(cursor.getColumnIndexOrThrow("username")),
                        cursor.getString(cursor.getColumnIndexOrThrow("circuit_name")),
                        cursor.getInt(cursor.getColumnIndexOrThrow("section")),
                        cursor.getInt(cursor.getColumnIndexOrThrow("row_pos")),
                        cursor.getInt(cursor.getColumnIndexOrThrow("column_pos"))
                );
                icList.add(icData);
            } while (cursor.moveToNext());
        }

        cursor.close();
        db.close();

        return icList;
    }

    /**
     * Clear all ICs from a specific circuit
     */
    public boolean clearICsForCircuit(String username, String circuitName) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        int rowsDeleted = db.delete("ics", "username = ? AND circuit_name = ?",
                new String[]{username, circuitName});
        db.close();

        return rowsDeleted >= 0;
    }

    /**
     * Load ICs from database for a specific circuit
     */
    public List<ICData> loadICsForCircuit(String username, String circuitName) {
        return getICsForCircuit(username, circuitName);
    }

    /**
     * Save current circuit ICs to database
     */
    public boolean syncICsForCircuit(String username, String circuitName, List<ICData> currentICs) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        db.beginTransaction();

        try {
            // Clear existing ICs for this circuit
            db.delete("ics", "username = ? AND circuit_name = ?",
                    new String[]{username, circuitName});

            // Insert current ICs
            ContentValues values = new ContentValues();
            for (ICData ic : currentICs) {
                values.clear();
                values.put("ic_type", ic.ic_type);
                values.put("username", username);
                values.put("circuit_name", circuitName);
                values.put("section", ic.section);
                values.put("row_pos", ic.row_pos);
                values.put("column_pos", ic.column_pos);

                long result = db.insert("ics", null, values);
                if (result == -1) {
                    throw new Exception("Failed to insert IC: " + ic.ic_type);
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