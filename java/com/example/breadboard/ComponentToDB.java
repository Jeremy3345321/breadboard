package com.example.breadboard;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.example.breadboard.model.Coordinate;

import java.util.ArrayList;
import java.util.List;

public class ComponentToDB {
    private DBHelper dbHelper;
    private Context context;

    // Constants for component types based on your internal markings
    public static final int VCC = 1;  // VCC (High input)
    public static final int GND = -2; // Ground

    public ComponentToDB(Context context) {
        this.context = context;
        this.dbHelper = new DBHelper(context);
    }

    public static class ComponentData {
        public int id;
        public int value; // 1 for VCC, -2 for GND
        public String username;
        public String circuitName;
        public int section;
        public int row_pos;
        public int column_pos;

        public ComponentData(int id, int value, String username, String circuitName, int section, int row, int column) {
            this.id = id;
            this.value = value;
            this.username = username;
            this.circuitName = circuitName;
            this.section = section;
            this.row_pos = row;
            this.column_pos = column;
        }

        public ComponentData(int value, String username, String circuitName, int section, int row, int column) {
            this.value = value;
            this.username = username;
            this.circuitName = circuitName;
            this.section = section;
            this.row_pos = row;
            this.column_pos = column;
        }

        public Coordinate getCoordinate() {
            return new Coordinate(section, row_pos, column_pos);
        }

        public boolean isVCC() {
            return value == VCC;
        }

        public boolean isGND() {
            return value == GND;
        }

        public String getComponentType() {
            return value == VCC ? "VCC" : "GND";
        }
    }

    /**
     * Insert a new power component (VCC or GND)
     */
    public boolean insertComponent(String username, String circuitName, Coordinate coord, int componentValue) {
        if (componentValue != VCC && componentValue != GND) {
            throw new IllegalArgumentException("Component value must be VCC (1) or GND (-2)");
        }

        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();

        values.put("value", componentValue);
        values.put("username", username);
        values.put("circuit_name", circuitName);
        values.put("section", coord.s);
        values.put("row_pos", coord.r);
        values.put("column_pos", coord.c);

        long result = db.insert("power_components", null, values);
        db.close();

        return result != -1;
    }

    /**
     * Insert VCC component
     */
    public boolean insertVCC(String username, String circuitName, Coordinate coord) {
        return insertComponent(username, circuitName, coord, VCC);
    }

    /**
     * Insert GND component
     */
    public boolean insertGND(String username, String circuitName, Coordinate coord) {
        return insertComponent(username, circuitName, coord, GND);
    }

    /**
     * Update an existing component's position for a specific circuit
     */
    public boolean updateComponentPosition(String username, String circuitName, Coordinate oldCoord, Coordinate newCoord) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();

        values.put("section", newCoord.s);
        values.put("row_pos", newCoord.r);
        values.put("column_pos", newCoord.c);

        int rowsAffected = db.update("power_components", values,
                "username = ? AND circuit_name = ? AND section = ? AND row_pos = ? AND column_pos = ?",
                new String[]{username, circuitName, String.valueOf(oldCoord.s),
                        String.valueOf(oldCoord.r), String.valueOf(oldCoord.c)});
        db.close();

        return rowsAffected > 0;
    }

    /**
     * Update component type (VCC to GND or vice versa) at specific coordinate
     */
    public boolean updateComponentType(String username, String circuitName, Coordinate coord, int newComponentValue) {
        if (newComponentValue != VCC && newComponentValue != GND) {
            throw new IllegalArgumentException("Component value must be VCC (1) or GND (-2)");
        }

        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("value", newComponentValue);

        int rowsAffected = db.update("power_components", values,
                "username = ? AND circuit_name = ? AND section = ? AND row_pos = ? AND column_pos = ?",
                new String[]{username, circuitName, String.valueOf(coord.s),
                        String.valueOf(coord.r), String.valueOf(coord.c)});
        db.close();

        return rowsAffected > 0;
    }

    /**
     * Delete a component by coordinate from a specific circuit
     */
    public boolean deleteComponentByCoordinate(String username, String circuitName, Coordinate coord) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        int rowsDeleted = db.delete("power_components",
                "username = ? AND circuit_name = ? AND section = ? AND row_pos = ? AND column_pos = ?",
                new String[]{username, circuitName, String.valueOf(coord.s),
                        String.valueOf(coord.r), String.valueOf(coord.c)});
        db.close();

        return rowsDeleted > 0;
    }

    /**
     * Get all power components from a specific circuit
     */
    public List<ComponentData> getComponentsForCircuit(String username, String circuitName) {
        List<ComponentData> componentList = new ArrayList<>();
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT * FROM power_components WHERE username = ? AND circuit_name = ? ORDER BY section, row_pos, column_pos",
                new String[]{username, circuitName});

        if (cursor.moveToFirst()) {
            do {
                ComponentData componentData = new ComponentData(
                        cursor.getInt(cursor.getColumnIndexOrThrow("id")),
                        cursor.getInt(cursor.getColumnIndexOrThrow("value")),
                        cursor.getString(cursor.getColumnIndexOrThrow("username")),
                        cursor.getString(cursor.getColumnIndexOrThrow("circuit_name")),
                        cursor.getInt(cursor.getColumnIndexOrThrow("section")),
                        cursor.getInt(cursor.getColumnIndexOrThrow("row_pos")),
                        cursor.getInt(cursor.getColumnIndexOrThrow("column_pos"))
                );
                componentList.add(componentData);
            } while (cursor.moveToNext());
        }

        cursor.close();
        db.close();

        return componentList;
    }

    /**
     * Get only VCC components from a specific circuit
     */
    public List<ComponentData> getVCCComponentsForCircuit(String username, String circuitName) {
        List<ComponentData> vccList = new ArrayList<>();
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT * FROM power_components WHERE username = ? AND circuit_name = ? AND value = ? ORDER BY section, row_pos, column_pos",
                new String[]{username, circuitName, String.valueOf(VCC)});

        if (cursor.moveToFirst()) {
            do {
                ComponentData componentData = new ComponentData(
                        cursor.getInt(cursor.getColumnIndexOrThrow("id")),
                        cursor.getInt(cursor.getColumnIndexOrThrow("value")),
                        cursor.getString(cursor.getColumnIndexOrThrow("username")),
                        cursor.getString(cursor.getColumnIndexOrThrow("circuit_name")),
                        cursor.getInt(cursor.getColumnIndexOrThrow("section")),
                        cursor.getInt(cursor.getColumnIndexOrThrow("row_pos")),
                        cursor.getInt(cursor.getColumnIndexOrThrow("column_pos"))
                );
                vccList.add(componentData);
            } while (cursor.moveToNext());
        }

        cursor.close();
        db.close();

        return vccList;
    }

    /**
     * Get only GND components from a specific circuit
     */
    public List<ComponentData> getGNDComponentsForCircuit(String username, String circuitName) {
        List<ComponentData> gndList = new ArrayList<>();
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT * FROM power_components WHERE username = ? AND circuit_name = ? AND value = ? ORDER BY section, row_pos, column_pos",
                new String[]{username, circuitName, String.valueOf(GND)});

        if (cursor.moveToFirst()) {
            do {
                ComponentData componentData = new ComponentData(
                        cursor.getInt(cursor.getColumnIndexOrThrow("id")),
                        cursor.getInt(cursor.getColumnIndexOrThrow("value")),
                        cursor.getString(cursor.getColumnIndexOrThrow("username")),
                        cursor.getString(cursor.getColumnIndexOrThrow("circuit_name")),
                        cursor.getInt(cursor.getColumnIndexOrThrow("section")),
                        cursor.getInt(cursor.getColumnIndexOrThrow("row_pos")),
                        cursor.getInt(cursor.getColumnIndexOrThrow("column_pos"))
                );
                gndList.add(componentData);
            } while (cursor.moveToNext());
        }

        cursor.close();
        db.close();

        return gndList;
    }

    /**
     * Clear all power components from a specific circuit
     */
    public boolean clearComponentsForCircuit(String username, String circuitName) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        int rowsDeleted = db.delete("power_components", "username = ? AND circuit_name = ?",
                new String[]{username, circuitName});
        db.close();

        return rowsDeleted >= 0;
    }

    /**
     * Load components from database for a specific circuit
     */
    public List<ComponentData> loadComponentsForCircuit(String username, String circuitName) {
        return getComponentsForCircuit(username, circuitName);
    }

    /**
     * Save current circuit components to database (replaces all existing components)
     */
    public boolean syncComponentsForCircuit(String username, String circuitName, List<ComponentData> currentComponents) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        db.beginTransaction();

        try {
            // Clear existing components for this circuit
            db.delete("power_components", "username = ? AND circuit_name = ?",
                    new String[]{username, circuitName});

            // Insert current components
            ContentValues values = new ContentValues();
            for (ComponentData component : currentComponents) {
                values.clear();
                values.put("value", component.value);
                values.put("username", username);
                values.put("circuit_name", circuitName);
                values.put("section", component.section);
                values.put("row_pos", component.row_pos);
                values.put("column_pos", component.column_pos);

                long result = db.insert("power_components", null, values);
                if (result == -1) {
                    throw new Exception("Failed to insert component at: " + component.getCoordinate());
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
     * Check if a component exists at a specific coordinate
     */
    public boolean componentExistsAt(String username, String circuitName, Coordinate coord) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT COUNT(*) FROM power_components WHERE username = ? AND circuit_name = ? AND section = ? AND row_pos = ? AND column_pos = ?",
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
     * Get component type at specific coordinate (returns VCC, GND, or null if none exists)
     */
    public ComponentData getComponentAt(String username, String circuitName, Coordinate coord) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT * FROM power_components WHERE username = ? AND circuit_name = ? AND section = ? AND row_pos = ? AND column_pos = ?",
                new String[]{username, circuitName, String.valueOf(coord.s),
                        String.valueOf(coord.r), String.valueOf(coord.c)});

        ComponentData component = null;
        if (cursor.moveToFirst()) {
            component = new ComponentData(
                    cursor.getInt(cursor.getColumnIndexOrThrow("id")),
                    cursor.getInt(cursor.getColumnIndexOrThrow("value")),
                    cursor.getString(cursor.getColumnIndexOrThrow("username")),
                    cursor.getString(cursor.getColumnIndexOrThrow("circuit_name")),
                    cursor.getInt(cursor.getColumnIndexOrThrow("section")),
                    cursor.getInt(cursor.getColumnIndexOrThrow("row_pos")),
                    cursor.getInt(cursor.getColumnIndexOrThrow("column_pos"))
            );
        }

        cursor.close();
        db.close();
        return component;
    }

    /**
     * Get total component count for a specific circuit
     */
    public int getComponentCountForCircuit(String username, String circuitName) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT COUNT(*) FROM power_components WHERE username = ? AND circuit_name = ?",
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
     * Get VCC component count for a specific circuit
     */
    public int getVCCCountForCircuit(String username, String circuitName) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT COUNT(*) FROM power_components WHERE username = ? AND circuit_name = ? AND value = ?",
                new String[]{username, circuitName, String.valueOf(VCC)});

        int count = 0;
        if (cursor.moveToFirst()) {
            count = cursor.getInt(0);
        }

        cursor.close();
        db.close();
        return count;
    }

    /**
     * Get GND component count for a specific circuit
     */
    public int getGNDCountForCircuit(String username, String circuitName) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT COUNT(*) FROM power_components WHERE username = ? AND circuit_name = ? AND value = ?",
                new String[]{username, circuitName, String.valueOf(GND)});

        int count = 0;
        if (cursor.moveToFirst()) {
            count = cursor.getInt(0);
        }

        cursor.close();
        db.close();
        return count;
    }
}