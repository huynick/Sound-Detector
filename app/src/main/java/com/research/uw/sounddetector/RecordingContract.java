package com.research.uw.sounddetector;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.provider.BaseColumns;

/**
 * Created by Nicholas on 3/10/2015.
 */
public class RecordingContract {
    public RecordingContract() {}

    public static abstract class RecordingEntry implements BaseColumns {
        public static final String TABLE_NAME = "recording";
        public static final String COLUMN_NAME_RECORDING_NAME = "recording";
        public static final String COLUMN_NAME_FILE_NAME = "fileName";
        public static final String COLUMN_NAME_SOUND_TYPE = "soundType";
        public static final String COLUMN_NAME_FILE_BEGINNING = "begin";
        public static final String COLUMN_NAME_FILE_END = "end";
        public static final String COLUMN_NAME_EDITED_FILE = "editedFile";

        public static final String SOUND_TABLE_NAME = "soundName";
        public static final String COLUMN_NAME_SOUND_TYPE_NAME = "soundTypeName";
        public static final String COLUMN_NAME_SOUND_TYPE_IN_USE = "inUse";
    }

    private static final String TEXT_TYPE = " TEXT";
    private static final String COMMA_SEP = ",";

    public static final String SQL_CREATE_ENTRIES =
            "CREATE TABLE " + RecordingEntry.TABLE_NAME + " (" +
                    RecordingEntry._ID + " INTEGER PRIMARY KEY," +
                    RecordingEntry.COLUMN_NAME_RECORDING_NAME + TEXT_TYPE + COMMA_SEP +
                    RecordingEntry.COLUMN_NAME_FILE_NAME + TEXT_TYPE + COMMA_SEP +
                    RecordingEntry.COLUMN_NAME_SOUND_TYPE + TEXT_TYPE + COMMA_SEP +
                    RecordingEntry.COLUMN_NAME_FILE_BEGINNING + " REAL" + COMMA_SEP +
                    RecordingEntry.COLUMN_NAME_FILE_END + " REAL" + COMMA_SEP +
                    RecordingEntry.COLUMN_NAME_EDITED_FILE + TEXT_TYPE +
                    " )";

    public static final String SQL_CREATE_SOUND_ENTRIES =
            "CREATE TABLE " + RecordingEntry.SOUND_TABLE_NAME + " (" +
                    RecordingEntry._ID + " INTEGER PRIMARY KEY" + COMMA_SEP +
                    RecordingEntry.COLUMN_NAME_SOUND_TYPE_NAME + TEXT_TYPE + " UNIQUE" + COMMA_SEP +
                    RecordingEntry.COLUMN_NAME_SOUND_TYPE_IN_USE + " INTEGER" +
                    " )";

    public static final String SQL_DELETE_ENTRIES =
            "DROP TABLE IF EXISTS " + RecordingEntry.TABLE_NAME;
}
