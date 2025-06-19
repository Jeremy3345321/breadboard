package com.example.breadboard;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class DBHelper extends SQLiteOpenHelper {

    public static final String DBNAME = "Breadboard.db";

    public DBHelper(Context context) {
        super(context, "Breadboard.db", null, 10); // Version 10 for wires table
    }

    @Override
    public void onCreate(SQLiteDatabase MyDB) {
        // Create users table
        MyDB.execSQL("create Table users(username TEXT primary key, password TEXT)");

        // Create inputs table
        MyDB.execSQL("create Table inputs(" +
                "id INTEGER primary key AUTOINCREMENT, " +
                "name TEXT, " +
                "username TEXT, " +
                "circuit_name TEXT, " +
                "section INTEGER, " +
                "row_pos INTEGER, " +
                "column_pos INTEGER, " +
                "UNIQUE(name, username, circuit_name), " +
                "FOREIGN KEY (username) REFERENCES users(username))");

        // Create outputs table
        MyDB.execSQL("create Table outputs(" +
                "id INTEGER primary key AUTOINCREMENT, " +
                "username TEXT, " +
                "circuit_name TEXT, " +
                "section INTEGER, " +
                "row_pos INTEGER, " +
                "column_pos INTEGER, " +
                "UNIQUE(username, circuit_name, section, row_pos, column_pos), " +
                "FOREIGN KEY (username) REFERENCES users(username))");

        // Create ics table
        MyDB.execSQL("create Table ics(" +
                "id INTEGER primary key AUTOINCREMENT, " +
                "ic_type TEXT, " +
                "username TEXT, " +
                "circuit_name TEXT, " +
                "section INTEGER, " +
                "row_pos INTEGER, " +
                "column_pos INTEGER, " +
                "UNIQUE(ic_type, username, circuit_name, section, row_pos, column_pos), " +
                "FOREIGN KEY (username) REFERENCES users(username))");

        // Create circuits table
        MyDB.execSQL("create Table circuits(" +
                "id INTEGER primary key AUTOINCREMENT, " +
                "circuit_name TEXT, " +
                "username TEXT, " +
                "created_date TEXT, " +
                "last_modified TEXT, " +
                "FOREIGN KEY (username) REFERENCES users(username))");

        // Create power_components table
        MyDB.execSQL("create Table power_components(" +
                "id INTEGER primary key AUTOINCREMENT, " +
                "value INTEGER NOT NULL CHECK (value IN (1, -2)), " +
                "username TEXT NOT NULL, " +
                "circuit_name TEXT NOT NULL, " +
                "section INTEGER NOT NULL, " +
                "row_pos INTEGER NOT NULL, " +
                "column_pos INTEGER NOT NULL, " +
                "UNIQUE(username, circuit_name, section, row_pos, column_pos), " +
                "FOREIGN KEY (username) REFERENCES users(username))");

        // Create wires table
        MyDB.execSQL("create Table wires(" +
                "id INTEGER primary key AUTOINCREMENT, " +
                "username TEXT NOT NULL, " +
                "circuit_name TEXT NOT NULL, " +
                "src_section INTEGER NOT NULL, " +
                "src_row INTEGER NOT NULL, " +
                "src_column INTEGER NOT NULL, " +
                "dst_section INTEGER NOT NULL, " +
                "dst_row INTEGER NOT NULL, " +
                "dst_column INTEGER NOT NULL, " +
                "UNIQUE(username, circuit_name, src_section, src_row, src_column, dst_section, dst_row, dst_column), " +
                "FOREIGN KEY (username) REFERENCES users(username))");

        System.out.println("Database created with version 10 - added wires table");
    }

    @Override
    public void onUpgrade(SQLiteDatabase MyDB, int oldVersion, int newVersion) {
        System.out.println("Upgrading database from version " + oldVersion + " to " + newVersion);

        if (oldVersion < 5) {
            try {
                // Drop and recreate inputs table with new structure
                MyDB.execSQL("DROP TABLE IF EXISTS inputs");

                MyDB.execSQL("create Table inputs(" +
                        "id INTEGER primary key AUTOINCREMENT, " +
                        "name TEXT, " +
                        "username TEXT, " +
                        "circuit_name TEXT, " +
                        "section INTEGER, " +
                        "row_pos INTEGER, " +
                        "column_pos INTEGER, " +
                        "UNIQUE(name, username, circuit_name), " +
                        "FOREIGN KEY (username) REFERENCES users(username))");

                // Add circuits table if it doesn't exist
                MyDB.execSQL("create Table IF NOT EXISTS circuits(" +
                        "id INTEGER primary key AUTOINCREMENT, " +
                        "circuit_name TEXT, " +
                        "username TEXT, " +
                        "created_date TEXT, " +
                        "last_modified TEXT, " +
                        "FOREIGN KEY (username) REFERENCES users(username))");

                System.out.println("Updated inputs table with circuit association during upgrade");
            } catch (Exception e) {
                System.err.println("Error upgrading database: " + e.getMessage());
                // If there's an error, recreate all tables
                MyDB.execSQL("DROP TABLE IF EXISTS users");
                MyDB.execSQL("DROP TABLE IF EXISTS inputs");
                MyDB.execSQL("DROP TABLE IF EXISTS circuits");
                onCreate(MyDB);
            }
        }

        // Handle upgrade to version 7 (outputs table)
        if (oldVersion < 7) {
            try {
                MyDB.execSQL("create Table IF NOT EXISTS outputs(" +
                        "id INTEGER primary key AUTOINCREMENT, " +
                        "username TEXT, " +
                        "circuit_name TEXT, " +
                        "section INTEGER, " +
                        "row_pos INTEGER, " +
                        "column_pos INTEGER, " +
                        "UNIQUE(username, circuit_name, section, row_pos, column_pos), " +
                        "FOREIGN KEY (username) REFERENCES users(username))");

                System.out.println("Added outputs table during upgrade to version 7");
            } catch (Exception e) {
                System.err.println("Error adding outputs table: " + e.getMessage());
            }
        }

        // Handle upgrade to version 8 (ics table)
        if (oldVersion < 8) {
            try {
                MyDB.execSQL("create Table IF NOT EXISTS ics(" +
                        "id INTEGER primary key AUTOINCREMENT, " +
                        "ic_type TEXT, " +
                        "username TEXT, " +
                        "circuit_name TEXT, " +
                        "section INTEGER, " +
                        "row_pos INTEGER, " +
                        "column_pos INTEGER, " +
                        "UNIQUE(ic_type, username, circuit_name, section, row_pos, column_pos), " +
                        "FOREIGN KEY (username) REFERENCES users(username))");

                System.out.println("Added ics table during upgrade to version 8");
            } catch (Exception e) {
                System.err.println("Error adding ics table: " + e.getMessage());
            }
        }

        // Handle upgrade to version 9 (power_components table)
        if (oldVersion < 9) {
            try {
                MyDB.execSQL("create Table IF NOT EXISTS power_components(" +
                        "id INTEGER primary key AUTOINCREMENT, " +
                        "value INTEGER NOT NULL CHECK (value IN (1, -2)), " + // 1 for VCC, -2 for GND
                        "username TEXT NOT NULL, " +
                        "circuit_name TEXT NOT NULL, " +
                        "section INTEGER NOT NULL, " +
                        "row_pos INTEGER NOT NULL, " +
                        "column_pos INTEGER NOT NULL, " +
                        "UNIQUE(username, circuit_name, section, row_pos, column_pos), " +
                        "FOREIGN KEY (username) REFERENCES users(username))");

                System.out.println("Added power_components table during upgrade to version 9");
            } catch (Exception e) {
                System.err.println("Error adding power_components table: " + e.getMessage());
            }
        }

        // Handle upgrade to version 10 (wires table)
        if (oldVersion < 10) {
            try {
                MyDB.execSQL("create Table IF NOT EXISTS wires(" +
                        "id INTEGER primary key AUTOINCREMENT, " +
                        "username TEXT NOT NULL, " +
                        "circuit_name TEXT NOT NULL, " +
                        "src_section INTEGER NOT NULL, " +
                        "src_row INTEGER NOT NULL, " +
                        "src_column INTEGER NOT NULL, " +
                        "dst_section INTEGER NOT NULL, " +
                        "dst_row INTEGER NOT NULL, " +
                        "dst_column INTEGER NOT NULL, " +
                        "UNIQUE(username, circuit_name, src_section, src_row, src_column, dst_section, dst_row, dst_column), " +
                        "FOREIGN KEY (username) REFERENCES users(username))");

                System.out.println("Added wires table during upgrade to version 10");
            } catch (Exception e) {
                System.err.println("Error adding wires table: " + e.getMessage());
            }
        }

        System.out.println("Database upgrade completed");
    }

    // User management methods
    public Boolean insertData(String username, String password) {
        SQLiteDatabase MyDB = this.getWritableDatabase();
        ContentValues contentValues = new ContentValues();
        contentValues.put("username", username);
        contentValues.put("password", password);
        long result = MyDB.insert("users", null, contentValues);
        MyDB.close();
        if (result == -1) return false;
        else return true;
    }

    // Alternative method name for compatibility with LoginActivity
    public Boolean insertUserData(String username, String password) {
        return insertData(username, password);
    }

    public Boolean checkusername(String username) {
        SQLiteDatabase MyDB = this.getReadableDatabase();
        Cursor cursor = MyDB.rawQuery("Select * from users where username = ?", new String[]{username});
        boolean exists = cursor.getCount() > 0;
        cursor.close();
        MyDB.close();
        return exists;
    }

    public Boolean checkusernamepassword(String username, String password) {
        SQLiteDatabase MyDB = this.getReadableDatabase();
        Cursor cursor = MyDB.rawQuery("Select * from users where username = ? and password = ?", new String[]{username, password});
        boolean exists = cursor.getCount() > 0;
        cursor.close();
        MyDB.close();
        return exists;
    }

    public Boolean updatePassword(String username, String password) {
        SQLiteDatabase db = null;
        try {
            db = this.getWritableDatabase();
            ContentValues contentValues = new ContentValues();
            contentValues.put("password", password);
            int rowsAffected = db.update("users", contentValues, "username = ?", new String[]{username});
            return rowsAffected > 0;
        } catch (Exception e) {
            System.err.println("Error updating password for user " + username + ": " + e.getMessage());
            return false;
        } finally {
            if (db != null) {
                try {
                    db.close();
                } catch (Exception e) {
                    System.err.println("Error closing database: " + e.getMessage());
                }
            }
        }
    }

    // Debug and utility methods
    public void checkTables() {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT name FROM sqlite_master WHERE type='table'", null);

        System.out.println("=== Database Tables ===");
        if (cursor.moveToFirst()) {
            do {
                String tableName = cursor.getString(0);
                System.out.println("Table: " + tableName);
            } while (cursor.moveToNext());
        }
        System.out.println("=====================");

        cursor.close();
        db.close();
    }

    public void listAllTables() {
        SQLiteDatabase db = null;
        Cursor cursor = null;

        try {
            db = this.getReadableDatabase();
            cursor = db.rawQuery("SELECT name FROM sqlite_master WHERE type='table'", null);

            System.out.println("=== ALL TABLES IN DATABASE ===");
            if (cursor.moveToFirst()) {
                do {
                    String tableName = cursor.getString(0);
                    System.out.println("Table found: " + tableName);
                } while (cursor.moveToNext());
            } else {
                System.out.println("No tables found in database!");
            }
            System.out.println("==============================");

        } catch (Exception e) {
            System.err.println("Error listing tables: " + e.getMessage());
            e.printStackTrace();
        } finally {
            // Always close cursor first, then database
            if (cursor != null) {
                try {
                    cursor.close();
                } catch (Exception e) {
                    System.err.println("Error closing cursor: " + e.getMessage());
                }
            }
            if (db != null) {
                try {
                    db.close();
                } catch (Exception e) {
                    System.err.println("Error closing database: " + e.getMessage());
                }
            }
        }
    }

    public void showTableStructure(String tableName) {
        SQLiteDatabase db = null;
        Cursor cursor = null;

        try {
            db = this.getReadableDatabase();
            cursor = db.rawQuery("PRAGMA table_info(" + tableName + ")", null);

            System.out.println("=== STRUCTURE OF TABLE: " + tableName + " ===");
            if (cursor.moveToFirst()) {
                do {
                    int cid = cursor.getInt(0);
                    String name = cursor.getString(1);
                    String type = cursor.getString(2);
                    int notNull = cursor.getInt(3);
                    String defaultValue = cursor.getString(4);
                    int pk = cursor.getInt(5);

                    System.out.println("Column " + cid + ": " + name + " (" + type + ")" +
                            (pk == 1 ? " PRIMARY KEY" : "") +
                            (notNull == 1 ? " NOT NULL" : ""));
                } while (cursor.moveToNext());
            } else {
                System.out.println("Table " + tableName + " does not exist!");
            }
            System.out.println("=====================================");

        } catch (Exception e) {
            System.err.println("Error showing table structure for " + tableName + ": " + e.getMessage());
            e.printStackTrace();
        } finally {
            // Always close cursor first, then database
            if (cursor != null) {
                try {
                    cursor.close();
                } catch (Exception e) {
                    System.err.println("Error closing cursor: " + e.getMessage());
                }
            }
            if (db != null) {
                try {
                    db.close();
                } catch (Exception e) {
                    System.err.println("Error closing database: " + e.getMessage());
                }
            }
        }
    }

    public void showTableData(String tableName) {
        SQLiteDatabase db = null;
        Cursor cursor = null;

        try {
            db = this.getReadableDatabase();
            cursor = db.rawQuery("SELECT * FROM " + tableName, null);

            System.out.println("=== DATA IN TABLE: " + tableName + " ===");
            System.out.println("Row count: " + cursor.getCount());

            if (cursor.moveToFirst()) {
                // Print column names
                String[] columnNames = cursor.getColumnNames();
                System.out.print("Columns: ");
                for (String col : columnNames) {
                    System.out.print(col + " | ");
                }
                System.out.println();

                // Print data
                do {
                    System.out.print("Row: ");
                    for (int i = 0; i < columnNames.length; i++) {
                        String value = cursor.getString(i);
                        System.out.print((value != null ? value : "NULL") + " | ");
                    }
                    System.out.println();
                } while (cursor.moveToNext());
            } else {
                System.out.println("No data in table " + tableName);
            }
            System.out.println("===============================");

        } catch (Exception e) {
            System.err.println("Error showing table data for " + tableName + ": " + e.getMessage());
            e.printStackTrace();
        } finally {
            // Always close cursor first, then database
            if (cursor != null) {
                try {
                    cursor.close();
                } catch (Exception e) {
                    System.err.println("Error closing cursor: " + e.getMessage());
                }
            }
            if (db != null) {
                try {
                    db.close();
                } catch (Exception e) {
                    System.err.println("Error closing database: " + e.getMessage());
                }
            }
        }
    }

    public void fullDatabaseDebug() {
        System.out.println("\n" + "=".repeat(50));
        System.out.println("FULL DATABASE DEBUG REPORT");
        System.out.println("Database: " + DBNAME);

        // Get database version safely
        SQLiteDatabase db = null;
        try {
            db = this.getReadableDatabase();
            System.out.println("Version: " + db.getVersion());
        } catch (Exception e) {
            System.err.println("Error getting database version: " + e.getMessage());
        } finally {
            if (db != null) db.close();
        }

        System.out.println("=".repeat(50));

        // List all tables (this method manages its own connection)
        listAllTables();

        // Check specific tables (each method manages its own connection)
        String[] expectedTables = {"users", "inputs", "outputs", "circuits", "ics", "power_components", "wires"};
        for (String table : expectedTables) {
            if (tableExists(table)) {
                showTableStructure(table);
                showTableData(table);
            } else {
                System.out.println("❌ Table '" + table + "' does not exist!");
            }
        }

        System.out.println("=".repeat(50) + "\n");
    }

    /**
     * Check if a specific table exists
     */
    public boolean tableExists(String tableName) {
        SQLiteDatabase db = null;
        Cursor cursor = null;

        try {
            db = this.getReadableDatabase();
            cursor = db.rawQuery("SELECT name FROM sqlite_master WHERE type='table' AND name=?",
                    new String[]{tableName});
            return cursor.getCount() > 0;
        } catch (Exception e) {
            System.err.println("Error checking if table " + tableName + " exists: " + e.getMessage());
            return false;
        } finally {
            // Always close cursor first, then database
            if (cursor != null) {
                try {
                    cursor.close();
                } catch (Exception e) {
                    System.err.println("Error closing cursor: " + e.getMessage());
                }
            }
            if (db != null) {
                try {
                    db.close();
                } catch (Exception e) {
                    System.err.println("Error closing database: " + e.getMessage());
                }
            }
        }
    }

    /**
     * Force database recreation (USE WITH CAUTION - DELETES ALL DATA)
     */
    public void recreateDatabase() {
        System.out.println("⚠️ RECREATING DATABASE - ALL DATA WILL BE LOST!");

        SQLiteDatabase db = this.getWritableDatabase();

        // Drop all existing tables
        db.execSQL("DROP TABLE IF EXISTS users");
        db.execSQL("DROP TABLE IF EXISTS inputs");
        db.execSQL("DROP TABLE IF EXISTS outputs");
        db.execSQL("DROP TABLE IF EXISTS circuits");
        db.execSQL("DROP TABLE IF EXISTS ics");
        db.execSQL("DROP TABLE IF EXISTS power_components");
        db.execSQL("DROP TABLE IF EXISTS wires");
        db.execSQL("DROP TABLE IF EXISTS user_circuit");

        // Recreate tables
        onCreate(db);

        db.close();
        System.out.println("✅ Database recreated successfully!");
    }
}