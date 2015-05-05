package com.research.uw.sounddetector;

import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Spinner;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;


public class SoundTypeList extends ActionBarActivity implements AddNewSoundTypeDialog.NoticeDialogListener, DeleteSoundTypeDialog.DeleteRecordingDialogListener {
    private ArrayList<SoundType> soundTypeList;
    private ArrayList<String> soundTypeNames;
    private SoundTypeAdapter soundTypeAdapter;
    private RecordingTableOpenHelper mDbHelper;
    private ListView listView;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sound_type);
        mDbHelper = new RecordingTableOpenHelper(this.getBaseContext());

        updateSoundType();

        soundTypeAdapter = new SoundTypeAdapter(this, android.R.layout.simple_list_item_1, soundTypeList, mDbHelper, getSupportFragmentManager());
        if(soundTypeAdapter.isEmpty()) {
            addSoundType("Uncategorized");
        }

        listView = (ListView) findViewById(R.id.soundTypeList);
        listView.setAdapter(soundTypeAdapter);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Intent i = new Intent(getApplicationContext(), RecordingList.class);
                i.putExtra("sound name", soundTypeAdapter.getItem(position).getName());
                i.putExtra("sound list", soundTypeNames);
                startActivity(i);
            }
        });
        //Instantiate Action Bar
        // Set a toolbar to replace the action bar.
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
    }

    @Override
    public void onResume() {
        super.onResume();
        updateSoundType();
    }

    private void updateSoundType() {
        soundTypeNames = new ArrayList<String>();
        soundTypeList = new ArrayList<SoundType>();
        SQLiteDatabase db = mDbHelper.getReadableDatabase();

        // Define a projection that specifies which columns from the database
        // you will actually use after this query.
        String[] projection = {
                RecordingContract.RecordingEntry._ID,
                RecordingContract.RecordingEntry.COLUMN_NAME_SOUND_TYPE_NAME,
                RecordingContract.RecordingEntry.COLUMN_NAME_SOUND_TYPE_IN_USE
        };

        Cursor c = db.query(
                RecordingContract.RecordingEntry.SOUND_TABLE_NAME,  // The table to query
                projection,                               // The columns to return
                null,                                // The columns for the WHERE clause
                null,                            // The values for the WHERE clause
                null,                                     // don't group the rows
                null,                                     // don't filter by row groups
                null                                 // The sort order
        );
        if(c.moveToFirst()) {
            String s = c.getString(c.getColumnIndex(RecordingContract.RecordingEntry.COLUMN_NAME_SOUND_TYPE_NAME));
            soundTypeNames.add(s);
            int i = c.getInt(c.getColumnIndex(RecordingContract.RecordingEntry.COLUMN_NAME_SOUND_TYPE_IN_USE));
            SoundType sound;
            if(i == 0) {
                sound = new SoundType(s, false);
            } else {
                sound = new SoundType(s, true);
            }
            soundTypeList.add(sound);
        }
        while(c.moveToNext()) {
            String s = c.getString(c.getColumnIndex(RecordingContract.RecordingEntry.COLUMN_NAME_SOUND_TYPE_NAME));
            soundTypeNames.add(s);
            int i = c.getInt(c.getColumnIndex(RecordingContract.RecordingEntry.COLUMN_NAME_SOUND_TYPE_IN_USE));
            SoundType sound;
            if(i == 0) {
                sound = new SoundType(s, false);
            } else {
                sound = new SoundType(s, true);
            }
            soundTypeList.add(sound);
        }
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_sound_type, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    //prompt the user to add a new sound type
    public void addNew(View view) {
        AddNewSoundTypeDialog dialog = new AddNewSoundTypeDialog();
        dialog.show(getSupportFragmentManager(), "AddSoundType");
    }

    @Override
    //Add the inputted sound type name to the list
    public void onDialogPositiveClick(DialogFragment dialog, String name) {
        addSoundType(name);
    }

    public void addSoundType(String name) {
        // Gets the data repository in write mode
        SQLiteDatabase db = mDbHelper.getWritableDatabase();

        // Create a new map of values, where column names are the keys
        ContentValues values = new ContentValues();

        values.put(RecordingContract.RecordingEntry.COLUMN_NAME_SOUND_TYPE_NAME, name);
        values.put(RecordingContract.RecordingEntry.COLUMN_NAME_SOUND_TYPE_IN_USE, 1);

        long ret = db.insert(
                RecordingContract.RecordingEntry.SOUND_TABLE_NAME,
                null,
                values);
        if(ret != -1) {
            soundTypeAdapter.add(new SoundType(name, true));
        }
        //TODO: sort this
    }

    @Override
    //Do nothing
    public void onDialogNegativeClick(DialogFragment dialog) {

    }

    @Override
    public void onDeletePositiveClick(DialogFragment dialog, int position) {
        SQLiteDatabase db = mDbHelper.getWritableDatabase();
        String s = soundTypeAdapter.getItem(position).getName();
        // Define 'where' part of query.
        String selection = RecordingContract.RecordingEntry.COLUMN_NAME_SOUND_TYPE_NAME + " = ?";
        // Specify arguments in placeholder order.
        String[] selectionArgs = {s};
        db.delete(RecordingContract.RecordingEntry.SOUND_TABLE_NAME, selection, selectionArgs);
        soundTypeNames.remove(s);
        soundTypeAdapter.remove(soundTypeAdapter.getItem(position));
    }

    @Override
    public void onDeleteNegativeClick(DialogFragment dialog) {
        //Do Nothing
    }

    public boolean help(MenuItem item) {
        HelpDialog dialog = new HelpDialog();
        dialog.show(getSupportFragmentManager(), "HelpDialog");
        return true;
    }

    public boolean settingsMenu(MenuItem item) {
        Intent i = new Intent(getApplicationContext(), SettingsScreen.class);
        startActivity(i);
        return true;
    }

    public boolean mainMenu(MenuItem item) {
        Intent i = new Intent(getApplicationContext(), MainScreen.class);
        startActivity(i);
        return true;
    }
}
