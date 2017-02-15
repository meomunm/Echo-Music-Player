package com.lunchareas.divertio;


import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.view.ContextThemeWrapper;

import java.util.ArrayList;
import java.util.List;

public class SongDBHandler extends SQLiteOpenHelper {

    // database info
    private static final int DATABASE_VERSION = 1;
    private static final String DATABASE_NAME = "SongInfoDatabase";

    // database attributes
    private static final String TABLE_SONGS = "songs";
    private static final String KEY_NAME = "name";
    private static final String KEY_PATH = "path";

    public SongDBHandler(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String CREATE_SONG_DATABASE = "CREATE TABLE " + TABLE_SONGS + "(" + KEY_NAME + " TEXT," + KEY_PATH + " TEXT" + ")";
        db.execSQL(CREATE_SONG_DATABASE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldDb, int newDb) {

        // replace old table
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_SONGS);
        this.onCreate(db);
    }

    public void addSongData(SongData songData) {

        // get table data
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();

        // insert new data from song data
        values.put(KEY_NAME, songData.getSongName());
        values.put(KEY_PATH, songData.getSongPath());
        db.insert(TABLE_SONGS, null, values);
        db.close();
    }

    public SongData getSongData(String name) {

        // get table data
        SQLiteDatabase db = this.getWritableDatabase();
        Cursor cursor = db.query(TABLE_SONGS, new String[] { KEY_NAME, KEY_PATH }, KEY_NAME + "=?", new String[] { String.valueOf(name) }, null, null, null, null);

        // search through database
        if (cursor != null) {
            cursor.moveToFirst();
            SongData songData = new SongData(cursor.getString(0), cursor.getString(1));
            db.close();
            return songData;
        } else {
            System.out.println("Failed to create database cursor.");
        }

        System.out.println("Failed to find song data with that name in database.");
        return null;
    }

    public List<SongData> getSongDataList() {

        // create list and get table data
        List<SongData> songDataList = new ArrayList<>();
        SQLiteDatabase db = this.getWritableDatabase();
        String dbQuery = "SELECT * FROM " + TABLE_SONGS;
        Cursor cursor = db.rawQuery(dbQuery, null);

        // go through database and all to list
        if (cursor.moveToFirst()) {
            do {
                SongData songData = new SongData(cursor.getString(0), cursor.getString(1));
                songDataList.add(songData);
            } while (cursor.moveToNext());
        }

        db.close();
        return songDataList;
    }
}
