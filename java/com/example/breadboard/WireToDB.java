package com.example.breadboard;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.example.breadboard.model.Coordinate;

import java.util.ArrayList;
import java.util.List;

public class WireToDB {
    private DBHelper dbHelper;
    private Context context;

    public WireToDB(Context context) {
        this.context = context;
        this.dbHelper = new DBHelper(context);
    }

    public static class WireData {
        public int id;
        public String username;
        public String circuitName;
        public int srcSection;
        public int srcRow;
        public int srcColumn;
        public int dstSection;
        public int dstRow;
        public int dstColumn;

        public WireData(int id, String username, String circuitName, 
                       int srcSection, int srcRow, int srcColumn,
                       int dstSection, int dstRow, int dstColumn) {
            this.id = id;
            this.username = username;
            this.circuitName = circuitName;
            this.srcSection = srcSection;
            this.srcRow = srcRow;
            this.srcColumn = srcColumn;
            this.dstSection = dstSection;
            this.dstRow = dstRow;
            this.dstColumn = dstColumn;
        }

        public WireData(String username, String circuitName, 
                       int srcSection, int srcRow, int srcColumn,
                       int dstSection, int dstRow, int dstColumn) {
            this.username = username;
            this.circuitName = circuitName;
            this.srcSection = srcSection;
            this.srcRow = srcRow;
            this.srcColumn = srcColumn;
            this.dstSection = dstSection;
            this.dstRow = dstRow;
            this.dstColumn = dstColumn;
        }

        public Coordinate getSourceCoordinate() {
            return new Coordinate(srcSection, srcRow, srcColumn);
        }

        public Coordinate getDestinationCoordinate() {
            return new Coordinate(dstSection, dstRow, dstColumn);
        }

        public String getWireDescription() {
            return String.format("Wire from (%d,%d,%d) to (%d,%d,%d)", 
                srcSection, srcRow, srcColumn, dstSection, dstRow, dstColumn);
        }

        public boolean connectsTo(Coordinate coord) {
            return (srcSection == coord.s && srcRow == coord.r && srcColumn == coord.c) ||
                   (dstSection == coord.s && dstRow == coord.r && dstColumn == coord.c);
        }
    }

    /**
     * Insert a new wire connection
     */
    public boolean insertWire(String username, String circuitName, Coordinate srcCoord, Coordinate dstCoord) {
        // Prevent self-connection
        if (srcCoord.equals(dstCoord)) {
            throw new IllegalArgumentException("Source and destination coordinates cannot be the same");
        }

        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();

        values.put("username", username);
        values.put("circuit_name", circuitName);
        values.put("src_section", srcCoord.s);
        values.put("src_row", srcCoord.r);
        values.put("src_column", srcCoord.c);
        values.put("dst_section", dstCoord.s);
        values.put("dst_row", dstCoord.r);
        values.put("dst_column", dstCoord.c);

        long result = db.insert("wires", null, values);
        db.close();

        return result != -1;
    }

    /**
     * Update an existing wire's source position for a specific circuit
     */
    public boolean updateWireSourcePosition(String username, String circuitName, 
                                          Coordinate oldSrcCoord, Coordinate oldDstCoord, 
                                          Coordinate newSrcCoord) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();

        values.put("src_section", newSrcCoord.s);
        values.put("src_row", newSrcCoord.r);
        values.put("src_column", newSrcCoord.c);

        int rowsAffected = db.update("wires", values,
                "username = ? AND circuit_name = ? AND src_section = ? AND src_row = ? AND src_column = ? " +
                "AND dst_section = ? AND dst_row = ? AND dst_column = ?",
                new String[]{username, circuitName, 
                           String.valueOf(oldSrcCoord.s), String.valueOf(oldSrcCoord.r), String.valueOf(oldSrcCoord.c),
                           String.valueOf(oldDstCoord.s), String.valueOf(oldDstCoord.r), String.valueOf(oldDstCoord.c)});
        db.close();

        return rowsAffected > 0;
    }

    /**
     * Update an existing wire's destination position for a specific circuit
     */
    public boolean updateWireDestinationPosition(String username, String circuitName, 
                                               Coordinate oldSrcCoord, Coordinate oldDstCoord, 
                                               Coordinate newDstCoord) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();

        values.put("dst_section", newDstCoord.s);
        values.put("dst_row", newDstCoord.r);
        values.put("dst_column", newDstCoord.c);

        int rowsAffected = db.update("wires", values,
                "username = ? AND circuit_name = ? AND src_section = ? AND src_row = ? AND src_column = ? " +
                "AND dst_section = ? AND dst_row = ? AND dst_column = ?",
                new String[]{username, circuitName, 
                           String.valueOf(oldSrcCoord.s), String.valueOf(oldSrcCoord.r), String.valueOf(oldSrcCoord.c),
                           String.valueOf(oldDstCoord.s), String.valueOf(oldDstCoord.r), String.valueOf(oldDstCoord.c)});
        db.close();

        return rowsAffected > 0;
    }

    /**
     * Update both endpoints of a wire
     */
    public boolean updateWire(String username, String circuitName, 
                            Coordinate oldSrcCoord, Coordinate oldDstCoord,
                            Coordinate newSrcCoord, Coordinate newDstCoord) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();

        values.put("src_section", newSrcCoord.s);
        values.put("src_row", newSrcCoord.r);
        values.put("src_column", newSrcCoord.c);
        values.put("dst_section", newDstCoord.s);
        values.put("dst_row", newDstCoord.r);
        values.put("dst_column", newDstCoord.c);

        int rowsAffected = db.update("wires", values,
                "username = ? AND circuit_name = ? AND src_section = ? AND src_row = ? AND src_column = ? " +
                "AND dst_section = ? AND dst_row = ? AND dst_column = ?",
                new String[]{username, circuitName, 
                           String.valueOf(oldSrcCoord.s), String.valueOf(oldSrcCoord.r), String.valueOf(oldSrcCoord.c),
                           String.valueOf(oldDstCoord.s), String.valueOf(oldDstCoord.r), String.valueOf(oldDstCoord.c)});
        db.close();

        return rowsAffected > 0;
    }

    /**
     * Delete a wire by its endpoints from a specific circuit
     */
    public boolean deleteWire(String username, String circuitName, Coordinate srcCoord, Coordinate dstCoord) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        int rowsDeleted = db.delete("wires",
                "username = ? AND circuit_name = ? AND src_section = ? AND src_row = ? AND src_column = ? " +
                "AND dst_section = ? AND dst_row = ? AND dst_column = ?",
                new String[]{username, circuitName, 
                           String.valueOf(srcCoord.s), String.valueOf(srcCoord.r), String.valueOf(srcCoord.c),
                           String.valueOf(dstCoord.s), String.valueOf(dstCoord.r), String.valueOf(dstCoord.c)});
        db.close();

        return rowsDeleted > 0;
    }

    /**
     * Delete all wires connected to a specific coordinate
     */
    public boolean deleteWiresConnectedTo(String username, String circuitName, Coordinate coord) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        int rowsDeleted = db.delete("wires",
                "username = ? AND circuit_name = ? AND ((src_section = ? AND src_row = ? AND src_column = ?) " +
                "OR (dst_section = ? AND dst_row = ? AND dst_column = ?))",
                new String[]{username, circuitName, 
                           String.valueOf(coord.s), String.valueOf(coord.r), String.valueOf(coord.c),
                           String.valueOf(coord.s), String.valueOf(coord.r), String.valueOf(coord.c)});
        db.close();

        return rowsDeleted > 0;
    }

    /**
     * Get all wires from a specific circuit
     */
    public List<WireData> getWiresForCircuit(String username, String circuitName) {
        List<WireData> wireList = new ArrayList<>();
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT * FROM wires WHERE username = ? AND circuit_name = ? " +
                "ORDER BY src_section, src_row, src_column",
                new String[]{username, circuitName});

        if (cursor.moveToFirst()) {
            do {
                WireData wireData = new WireData(
                        cursor.getInt(cursor.getColumnIndexOrThrow("id")),
                        cursor.getString(cursor.getColumnIndexOrThrow("username")),
                        cursor.getString(cursor.getColumnIndexOrThrow("circuit_name")),
                        cursor.getInt(cursor.getColumnIndexOrThrow("src_section")),
                        cursor.getInt(cursor.getColumnIndexOrThrow("src_row")),
                        cursor.getInt(cursor.getColumnIndexOrThrow("src_column")),
                        cursor.getInt(cursor.getColumnIndexOrThrow("dst_section")),
                        cursor.getInt(cursor.getColumnIndexOrThrow("dst_row")),
                        cursor.getInt(cursor.getColumnIndexOrThrow("dst_column"))
                );
                wireList.add(wireData);
            } while (cursor.moveToNext());
        }

        cursor.close();
        db.close();

        return wireList;
    }

    /**
     * Get all wires connected to a specific coordinate
     */
    public List<WireData> getWiresConnectedTo(String username, String circuitName, Coordinate coord) {
        List<WireData> wireList = new ArrayList<>();
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT * FROM wires WHERE username = ? AND circuit_name = ? AND " +
                "((src_section = ? AND src_row = ? AND src_column = ?) OR " +
                "(dst_section = ? AND dst_row = ? AND dst_column = ?))",
                new String[]{username, circuitName, 
                           String.valueOf(coord.s), String.valueOf(coord.r), String.valueOf(coord.c),
                           String.valueOf(coord.s), String.valueOf(coord.r), String.valueOf(coord.c)});

        if (cursor.moveToFirst()) {
            do {
                WireData wireData = new WireData(
                        cursor.getInt(cursor.getColumnIndexOrThrow("id")),
                        cursor.getString(cursor.getColumnIndexOrThrow("username")),
                        cursor.getString(cursor.getColumnIndexOrThrow("circuit_name")),
                        cursor.getInt(cursor.getColumnIndexOrThrow("src_section")),
                        cursor.getInt(cursor.getColumnIndexOrThrow("src_row")),
                        cursor.getInt(cursor.getColumnIndexOrThrow("src_column")),
                        cursor.getInt(cursor.getColumnIndexOrThrow("dst_section")),
                        cursor.getInt(cursor.getColumnIndexOrThrow("dst_row")),
                        cursor.getInt(cursor.getColumnIndexOrThrow("dst_column"))
                );
                wireList.add(wireData);
            } while (cursor.moveToNext());
        }

        cursor.close();
        db.close();

        return wireList;
    }

    /**
     * Clear all wires from a specific circuit
     */
    public boolean clearWiresForCircuit(String username, String circuitName) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        int rowsDeleted = db.delete("wires", "username = ? AND circuit_name = ?",
                new String[]{username, circuitName});
        db.close();

        return rowsDeleted >= 0;
    }

    /**
     * Load wires from database for a specific circuit
     */
    public List<WireData> loadWiresForCircuit(String username, String circuitName) {
        return getWiresForCircuit(username, circuitName);
    }

    /**
     * Save current circuit wires to database (replaces all existing wires)
     */
    public boolean syncWiresForCircuit(String username, String circuitName, List<WireData> currentWires) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        db.beginTransaction();

        try {
            // Clear existing wires for this circuit
            db.delete("wires", "username = ? AND circuit_name = ?",
                    new String[]{username, circuitName});

            // Insert current wires
            ContentValues values = new ContentValues();
            for (WireData wire : currentWires) {
                values.clear();
                values.put("username", username);
                values.put("circuit_name", circuitName);
                values.put("src_section", wire.srcSection);
                values.put("src_row", wire.srcRow);
                values.put("src_column", wire.srcColumn);
                values.put("dst_section", wire.dstSection);
                values.put("dst_row", wire.dstRow);
                values.put("dst_column", wire.dstColumn);

                long result = db.insert("wires", null, values);
                if (result == -1) {
                    throw new Exception("Failed to insert wire: " + wire.getWireDescription());
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
     * Check if a wire exists between two specific coordinates
     */
    public boolean wireExists(String username, String circuitName, Coordinate srcCoord, Coordinate dstCoord) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT COUNT(*) FROM wires WHERE username = ? AND circuit_name = ? AND " +
                "src_section = ? AND src_row = ? AND src_column = ? AND " +
                "dst_section = ? AND dst_row = ? AND dst_column = ?",
                new String[]{username, circuitName, 
                           String.valueOf(srcCoord.s), String.valueOf(srcCoord.r), String.valueOf(srcCoord.c),
                           String.valueOf(dstCoord.s), String.valueOf(dstCoord.r), String.valueOf(dstCoord.c)});

        boolean exists = false;
        if (cursor.moveToFirst()) {
            exists = cursor.getInt(0) > 0;
        }

        cursor.close();
        db.close();
        return exists;
    }

    /**
     * Check if any wire is connected to a specific coordinate
     */
    public boolean wireConnectedTo(String username, String circuitName, Coordinate coord) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT COUNT(*) FROM wires WHERE username = ? AND circuit_name = ? AND " +
                "((src_section = ? AND src_row = ? AND src_column = ?) OR " +
                "(dst_section = ? AND dst_row = ? AND dst_column = ?))",
                new String[]{username, circuitName, 
                           String.valueOf(coord.s), String.valueOf(coord.r), String.valueOf(coord.c),
                           String.valueOf(coord.s), String.valueOf(coord.r), String.valueOf(coord.c)});

        boolean exists = false;
        if (cursor.moveToFirst()) {
            exists = cursor.getInt(0) > 0;
        }

        cursor.close();
        db.close();
        return exists;
    }

    /**
     * Get wire connecting two specific coordinates (if it exists)
     */
    public WireData getWire(String username, String circuitName, Coordinate srcCoord, Coordinate dstCoord) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT * FROM wires WHERE username = ? AND circuit_name = ? AND " +
                "src_section = ? AND src_row = ? AND src_column = ? AND " +
                "dst_section = ? AND dst_row = ? AND dst_column = ?",
                new String[]{username, circuitName, 
                           String.valueOf(srcCoord.s), String.valueOf(srcCoord.r), String.valueOf(srcCoord.c),
                           String.valueOf(dstCoord.s), String.valueOf(dstCoord.r), String.valueOf(dstCoord.c)});

        WireData wire = null;
        if (cursor.moveToFirst()) {
            wire = new WireData(
                    cursor.getInt(cursor.getColumnIndexOrThrow("id")),
                    cursor.getString(cursor.getColumnIndexOrThrow("username")),
                    cursor.getString(cursor.getColumnIndexOrThrow("circuit_name")),
                    cursor.getInt(cursor.getColumnIndexOrThrow("src_section")),
                    cursor.getInt(cursor.getColumnIndexOrThrow("src_row")),
                    cursor.getInt(cursor.getColumnIndexOrThrow("src_column")),
                    cursor.getInt(cursor.getColumnIndexOrThrow("dst_section")),
                    cursor.getInt(cursor.getColumnIndexOrThrow("dst_row")),
                    cursor.getInt(cursor.getColumnIndexOrThrow("dst_column"))
            );
        }

        cursor.close();
        db.close();
        return wire;
    }

    /**
     * Get total wire count for a specific circuit
     */
    public int getWireCountForCircuit(String username, String circuitName) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT COUNT(*) FROM wires WHERE username = ? AND circuit_name = ?",
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
     * Get count of wires connected to a specific coordinate
     */
    public int getWireCountConnectedTo(String username, String circuitName, Coordinate coord) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT COUNT(*) FROM wires WHERE username = ? AND circuit_name = ? AND " +
                "((src_section = ? AND src_row = ? AND src_column = ?) OR " +
                "(dst_section = ? AND dst_row = ? AND dst_column = ?))",
                new String[]{username, circuitName, 
                           String.valueOf(coord.s), String.valueOf(coord.r), String.valueOf(coord.c),
                           String.valueOf(coord.s), String.valueOf(coord.r), String.valueOf(coord.c)});

        int count = 0;
        if (cursor.moveToFirst()) {
            count = cursor.getInt(0);
        }

        cursor.close();
        db.close();
        return count;
    }
}