package com.example.mobilewhmapp.data;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.content.ContentValues;
import android.database.Cursor;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class LocalDatabaseHelper extends SQLiteOpenHelper {
    private static final String DATABASE_NAME = "mobilewhmapp.db";
    private static final int DATABASE_VERSION = 1;

    // Table and columns for cached products
    public static final String TABLE_PRODUCTS = "products";
    public static final String COLUMN_PRODUCT_ID = "product_id";
    public static final String COLUMN_PRODUCT_DATA = "product_data"; // JSON string

    // Table and columns for queued stock movements
    public static final String TABLE_STOCK_MOVEMENTS = "stock_movements";
    public static final String COLUMN_MOVEMENT_ID = "movement_id";
    public static final String COLUMN_MOVEMENT_DATA = "movement_data"; // JSON string

    public LocalDatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String createProductsTable = "CREATE TABLE " + TABLE_PRODUCTS + " (" +
                COLUMN_PRODUCT_ID + " TEXT PRIMARY KEY, " +
                COLUMN_PRODUCT_DATA + " TEXT" +
                ")";
        db.execSQL(createProductsTable);

        String createStockMovementsTable = "CREATE TABLE " + TABLE_STOCK_MOVEMENTS + " (" +
                COLUMN_MOVEMENT_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                COLUMN_MOVEMENT_DATA + " TEXT" +
                ")";
        db.execSQL(createStockMovementsTable);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_PRODUCTS);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_STOCK_MOVEMENTS);
        onCreate(db);
    }

    // Cache product data locally
    public void cacheProduct(String productId, JSONObject productData) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_PRODUCT_ID, productId);
        values.put(COLUMN_PRODUCT_DATA, productData.toString());
        db.insertWithOnConflict(TABLE_PRODUCTS, null, values, SQLiteDatabase.CONFLICT_REPLACE);
        db.close();
    }

    // Get cached product data
    public JSONObject getCachedProduct(String productId) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query(TABLE_PRODUCTS, new String[]{COLUMN_PRODUCT_DATA},
                COLUMN_PRODUCT_ID + " = ?", new String[]{productId},
                null, null, null);
        if (cursor != null && cursor.moveToFirst()) {
            String jsonString = cursor.getString(cursor.getColumnIndex(COLUMN_PRODUCT_DATA));
            cursor.close();
            db.close();
            try {
                return new JSONObject(jsonString);
            } catch (Exception e) {
                e.printStackTrace();
                return null;
            }
        }
        if(cursor != null) cursor.close();
        db.close();
        return null;
    }

    // Add a stock movement to the offline queue
    public void addStockMovement(JSONObject movementData) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_MOVEMENT_DATA, movementData.toString());
        db.insert(TABLE_STOCK_MOVEMENTS, null, values);
        db.close();
    }

    // Get all queued stock movements
    public List<JSONObject> getQueuedStockMovements() {
        List<JSONObject> movements = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query(TABLE_STOCK_MOVEMENTS, new String[]{COLUMN_MOVEMENT_DATA},
                null, null, null, null, COLUMN_MOVEMENT_ID + " ASC");
        if (cursor != null && cursor.moveToFirst()) {
            do {
                String jsonString = cursor.getString(cursor.getColumnIndex(COLUMN_MOVEMENT_DATA));
                try {
                    movements.add(new JSONObject(jsonString));
                } catch (Exception e) {
                    e.printStackTrace();
                }
            } while (cursor.moveToNext());
            cursor.close();
        }
        db.close();
        return movements;
    }

    // Remove a stock movement from the queue after successful sync
    public void removeStockMovement(long movementId) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(TABLE_STOCK_MOVEMENTS, COLUMN_MOVEMENT_ID + " = ?", new String[]{String.valueOf(movementId)});
        db.close();
    }

    // Get ID for queued stock movement from Cursor row
    public long getMovementId(Cursor cursor) {
        return cursor.getLong(cursor.getColumnIndex(COLUMN_MOVEMENT_ID));
    }
}
