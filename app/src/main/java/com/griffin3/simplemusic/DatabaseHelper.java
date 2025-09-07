package com.griffin3.simplemusic;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.provider.MediaStore;
import java.util.ArrayList;

public class DatabaseHelper extends SQLiteOpenHelper {
    private static final String DATABASE_NAME = "music.db";
    private static final int DATABASE_VERSION = 2;

    // Media table
    private static final String TABLE_MEDIA = "media";
    private static final String COLUMN_MEDIA_ID = "id";
    private static final String COLUMN_ARTIST = "artist";
    private static final String COLUMN_TITLE = "title";
    private static final String COLUMN_DURATION = "duration";
    private static final String COLUMN_DATA = "data";

    // Queue table
    private static final String TABLE_QUEUE = "queue";
    private static final String COLUMN_QUEUE_ID = "id";
    private static final String COLUMN_MEDIA_ID_FK = "media_id";
    private static final String COLUMN_VOLUME = "volume";
    private static final String COLUMN_LIKES = "likes";

    // State table
    private static final String TABLE_STATE = "state";
    private static final String COLUMN_STATE_ID = "id";
    private static final String COLUMN_POSITION = "position";
    private static final String COLUMN_SHUFFLE = "shuffle";
    private static final String COLUMN_LOOP = "loop";

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String createMediaTable = "CREATE TABLE " + TABLE_MEDIA + " (" +
                COLUMN_MEDIA_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                COLUMN_ARTIST + " TEXT, " +
                COLUMN_TITLE + " TEXT, " +
                COLUMN_DURATION + " INTEGER, " +
                COLUMN_DATA + " TEXT, " +
                COLUMN_VOLUME + " INTEGER DEFAULT 128, " +
                COLUMN_LIKES + " INTEGER DEFAULT 0)";
        db.execSQL(createMediaTable);

        String createQueueTable = "CREATE TABLE " + TABLE_QUEUE + " (" +
                COLUMN_QUEUE_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                COLUMN_MEDIA_ID_FK + " INTEGER, " +
                "FOREIGN KEY(" + COLUMN_MEDIA_ID_FK + ") REFERENCES " + TABLE_MEDIA + "(" + COLUMN_MEDIA_ID + "))";
        db.execSQL(createQueueTable);

        String createStateTable = "CREATE TABLE " + TABLE_STATE + " (" +
                COLUMN_STATE_ID + " INTEGER PRIMARY KEY, " +
                COLUMN_POSITION + " INTEGER DEFAULT -1, " +
                COLUMN_SHUFFLE + " INTEGER DEFAULT 1, " +
                COLUMN_LOOP + " INTEGER DEFAULT 1)";
        db.execSQL(createStateTable);

        // Insert default state
        ContentValues values = new ContentValues();
        values.put(COLUMN_STATE_ID, 1);
        values.put(COLUMN_POSITION, -1);
        values.put(COLUMN_SHUFFLE, 1);
        values.put(COLUMN_LOOP, 1);
        db.insert(TABLE_STATE, null, values);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion < 2) {
            // Add volume and likes to media
            db.execSQL("ALTER TABLE " + TABLE_MEDIA + " ADD COLUMN " + COLUMN_VOLUME + " INTEGER DEFAULT 128");
            db.execSQL("ALTER TABLE " + TABLE_MEDIA + " ADD COLUMN " + COLUMN_LIKES + " INTEGER DEFAULT 0");
            // For queue, since removing columns, recreate queue table
            db.execSQL("DROP TABLE IF EXISTS " + TABLE_QUEUE);
            String createQueueTable = "CREATE TABLE " + TABLE_QUEUE + " (" +
                    COLUMN_QUEUE_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    COLUMN_MEDIA_ID_FK + " INTEGER, " +
                    "FOREIGN KEY(" + COLUMN_MEDIA_ID_FK + ") REFERENCES " + TABLE_MEDIA + "(" + COLUMN_MEDIA_ID + "))";
            db.execSQL(createQueueTable);
        }
    }

    public int getMediaCount() {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query(TABLE_MEDIA, new String[]{COLUMN_MEDIA_ID}, null, null, null, null, null);
        int count = cursor.getCount();
        cursor.close();
        db.close();
        return count;
    }

    public boolean isQueueEmpty() {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query(TABLE_QUEUE, new String[]{COLUMN_QUEUE_ID}, null, null, null, null, null);
        boolean empty = cursor.getCount() == 0;
        cursor.close();
        db.close();
        return empty;
    }

    public long loadMediaFromMediaStore(Context context) {
        long startTime = System.currentTimeMillis();
        SQLiteDatabase db = this.getWritableDatabase();
        // Clear existing
        db.delete(TABLE_MEDIA, null, null);

        Cursor cursor = context.getContentResolver().query(
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                new String[]{
                        MediaStore.Audio.Media.ARTIST,
                        MediaStore.Audio.Media.TITLE,
                        MediaStore.Audio.Media.DURATION,
                        MediaStore.Audio.Media.DATA
                },
                null, null, null);

        if (cursor != null) {
            while (cursor.moveToNext()) {
                String artist = cursor.getString(0);
                String title = cursor.getString(1);
                long duration = cursor.getLong(2);
                String data = cursor.getString(3);

                if (artist.equals("<unknown>") && title.contains(" - ")) {
                    String[] parts = title.split(" - ", 2);
                    if (parts.length == 2) {
                        artist = parts[0];
                        title = parts[1];
                    }
                }

                ContentValues values = new ContentValues();
                values.put(COLUMN_ARTIST, artist);
                values.put(COLUMN_TITLE, title);
                values.put(COLUMN_DURATION, duration);
                values.put(COLUMN_DATA, data);
                values.put(COLUMN_VOLUME, 128);
                values.put(COLUMN_LIKES, 0);
                db.insert(TABLE_MEDIA, null, values);
            }
            cursor.close();
        }
        db.close();
        long endTime = System.currentTimeMillis();
        return endTime - startTime;
    }

    public ArrayList<String> getQueueItems() {
        ArrayList<String> items = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT m." + COLUMN_ARTIST + ", m." + COLUMN_TITLE + ", m." + COLUMN_DURATION + ", m." + COLUMN_DATA +
                " FROM " + TABLE_QUEUE + " q INNER JOIN " + TABLE_MEDIA + " m ON q." + COLUMN_MEDIA_ID_FK + " = m." + COLUMN_MEDIA_ID, null);
        while (cursor.moveToNext()) {
            String artist = cursor.getString(0);
            String title = cursor.getString(1);
            long duration = cursor.getLong(2);
            String data = cursor.getString(3);
            items.add(artist + "|" + title + "|" + duration + "|" + data);
        }
        cursor.close();
        db.close();
        return items;
    }

    public void fillQueueWithShuffledMedia() {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(TABLE_QUEUE, null, null);
        Cursor cursor = db.query(TABLE_MEDIA, new String[]{COLUMN_MEDIA_ID}, null, null, null, null, null);
        ArrayList<Long> mediaIds = new ArrayList<>();
        while (cursor.moveToNext()) {
            mediaIds.add(cursor.getLong(0));
        }
        cursor.close();
        // Shuffle
        java.util.Collections.shuffle(mediaIds);
        for (Long id : mediaIds) {
            ContentValues values = new ContentValues();
            values.put(COLUMN_MEDIA_ID_FK, id);
            db.insert(TABLE_QUEUE, null, values);
        }
        db.close();
    }

    public int getQueuePosition() {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query(TABLE_STATE, new String[]{COLUMN_POSITION}, COLUMN_STATE_ID + "=1", null, null, null, null);
        int position = -1;
        if (cursor.moveToFirst()) {
            position = cursor.getInt(0);
        }
        cursor.close();
        db.close();
        return position;
    }

    public void setQueuePosition(int position) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_POSITION, position);
        db.update(TABLE_STATE, values, COLUMN_STATE_ID + "=1", null);
        db.close();
    }

    public int getShuffle() {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query(TABLE_STATE, new String[]{COLUMN_SHUFFLE}, COLUMN_STATE_ID + "=1", null, null, null, null);
        int shuffle = 1;
        if (cursor.moveToFirst()) {
            shuffle = cursor.getInt(0);
        }
        cursor.close();
        db.close();
        return shuffle;
    }

    public void setShuffle(int shuffle) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_SHUFFLE, shuffle);
        db.update(TABLE_STATE, values, COLUMN_STATE_ID + "=1", null);
        db.close();
    }

    public int getLoop() {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query(TABLE_STATE, new String[]{COLUMN_LOOP}, COLUMN_STATE_ID + "=1", null, null, null, null);
        int loop = 1;
        if (cursor.moveToFirst()) {
            loop = cursor.getInt(0);
        }
        cursor.close();
        db.close();
        return loop;
    }

    public String getCurrentSongInfo(int position) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT m." + COLUMN_TITLE + ", m." + COLUMN_ARTIST +
                " FROM " + TABLE_QUEUE + " q INNER JOIN " + TABLE_MEDIA + " m ON q." + COLUMN_MEDIA_ID_FK + " = m." + COLUMN_MEDIA_ID +
                " LIMIT 1 OFFSET " + position, null);
        String info = null;
        if (cursor.moveToFirst()) {
            String title = cursor.getString(0);
            String artist = cursor.getString(1);
            info = title + " - " + artist;
        }
        cursor.close();
        db.close();
        return info;
    }

    public void clearAllData() {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(TABLE_QUEUE, null, null);
        db.delete(TABLE_MEDIA, null, null);
        db.delete(TABLE_STATE, null, null);
        // Re-insert default state
        ContentValues values = new ContentValues();
        values.put(COLUMN_STATE_ID, 1);
        values.put(COLUMN_POSITION, -1);
        values.put(COLUMN_SHUFFLE, 1);
        values.put(COLUMN_LOOP, 1);
        db.insert(TABLE_STATE, null, values);
        db.close();
    }
}
