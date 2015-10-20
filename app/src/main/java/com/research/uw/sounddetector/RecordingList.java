package com.research.uw.sounddetector;

import android.app.LoaderManager;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.Loader;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;

import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;


public class RecordingList extends ActionBarActivity implements AddRecordingDialog.AddRecordingDialogListener, DeleteRecordingDialog.DeleteRecordingDialogListener, EditRecordingDialog.EditRecordingDialogListener {
    private ListView listView;
    private ArrayList<Recording> currRecordings;
    private RecordingListAdapter recordingListAdapter;
    private RecordingTableOpenHelper mDbHelper;
    private String soundName;
    private boolean writing;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        writing = false;
        setContentView(R.layout.activity_recording_list);
        currRecordings = new ArrayList<Recording>();

        mDbHelper = new RecordingTableOpenHelper(getBaseContext());

        Intent i = getIntent();
        soundName = i.getStringExtra("sound name");

        Spinner spinner = (Spinner) findViewById(R.id.sortbyspinner);
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this, R.array.sort_by_array, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);
        //Set Sort by to invisible:
        spinner.setVisibility(View.GONE);
        findViewById(R.id.sortbytext).setVisibility(View.GONE);

        listView = (ListView) findViewById(R.id.recordingList);
        recordingListAdapter = new RecordingListAdapter(this, android.R.layout.simple_list_item_1, currRecordings, mDbHelper, getSupportFragmentManager());
        listView.setAdapter(recordingListAdapter);
        updateCurrRecordings();

        //Instantiate Action Bar
        // Set a toolbar to replace the action bar.
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        toolbar.setTitle(soundName + " Recordings");
        setSupportActionBar(toolbar);
    }

    @Override
    public void onResume() {
        super.onResume();
        updateCurrRecordings();
    }

    @Override
    public void onFinish(DialogFragment dialog) {

    }

    private void updateCurrRecordings() {
        SQLiteDatabase db = mDbHelper.getReadableDatabase();
        recordingListAdapter.clear();

        // Define a projection that specifies which columns from the database
        // you will actually use after this query.
        String[] projection = {
                RecordingContract.RecordingEntry._ID,
                RecordingContract.RecordingEntry.COLUMN_NAME_RECORDING_NAME,
                RecordingContract.RecordingEntry.COLUMN_NAME_FILE_NAME,
                RecordingContract.RecordingEntry.COLUMN_NAME_SOUND_TYPE,
                RecordingContract.RecordingEntry.COLUMN_NAME_FILE_BEGINNING,
                RecordingContract.RecordingEntry.COLUMN_NAME_FILE_END
        };

        String selection = RecordingContract.RecordingEntry.COLUMN_NAME_SOUND_TYPE + " = ?";

        String[] selectionArgs = {soundName};

        Cursor c = db.query(
                RecordingContract.RecordingEntry.TABLE_NAME,  // The table to query
                projection,                               // The columns to return
                selection,                                // The columns for the WHERE clause
                selectionArgs,                            // The values for the WHERE clause
                null,                                     // don't group the rows
                null,                                     // don't filter by row groups
                null                                 // The sort order
        );
        if(c.moveToFirst()) {
            Recording r = new Recording(c.getString(c.getColumnIndex(RecordingContract.RecordingEntry.COLUMN_NAME_RECORDING_NAME)),
                    c.getString(c.getColumnIndex(RecordingContract.RecordingEntry.COLUMN_NAME_FILE_NAME)),
                    c.getString(c.getColumnIndex(RecordingContract.RecordingEntry.COLUMN_NAME_SOUND_TYPE)),
                    c.getDouble(c.getColumnIndex(RecordingContract.RecordingEntry.COLUMN_NAME_FILE_BEGINNING)),
                    c.getDouble(c.getColumnIndex(RecordingContract.RecordingEntry.COLUMN_NAME_FILE_END)));
            recordingListAdapter.add(r);
        }
        while(c.moveToNext()) {
            Recording r = new Recording(c.getString(c.getColumnIndex(RecordingContract.RecordingEntry.COLUMN_NAME_RECORDING_NAME)),
                    c.getString(c.getColumnIndex(RecordingContract.RecordingEntry.COLUMN_NAME_FILE_NAME)),
                    c.getString(c.getColumnIndex(RecordingContract.RecordingEntry.COLUMN_NAME_SOUND_TYPE)),
                    c.getDouble(c.getColumnIndex(RecordingContract.RecordingEntry.COLUMN_NAME_FILE_BEGINNING)),
                    c.getDouble(c.getColumnIndex(RecordingContract.RecordingEntry.COLUMN_NAME_FILE_END)));
            System.out.println("begin" + r.getStart());
            System.out.println("end" + r.getEnd());
            recordingListAdapter.add(r);
        }
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_recording_list, menu);
        return true;
    }

    public void addNewRecording(View v) {
        AddRecordingDialog dialog = new AddRecordingDialog();
        dialog.setSoundType(soundName);
        dialog.setDb(mDbHelper);
        dialog.setAutoName(getAutoName());
        dialog.show(getSupportFragmentManager(), "AddRecording");
    }

    private String getAutoName() {
        int min = 0;
        for(int i = 0; i < currRecordings.size(); i++) {
            String currName = currRecordings.get(i).getName();
            if(currName.startsWith(soundName)) {
                try {
                    int currNum = Integer.parseInt(currName.substring(soundName.length() + 1));
                    if(currNum > min) {
                        min = currNum;
                    }
                } catch (NumberFormatException e) {

                } catch (IndexOutOfBoundsException e) {

                }
            }
        }

        return soundName + " " + (min + 1);
    }

    public void onDialogStopClick(DialogFragment dialog, Recording recording) {
        // Gets the data repository in write mode
        SQLiteDatabase db = mDbHelper.getWritableDatabase();

        // Create a new map of values, where column names are the keys
        ContentValues values = new ContentValues();
        values.put(RecordingContract.RecordingEntry.COLUMN_NAME_FILE_NAME, recording.getFileName());
        values.put(RecordingContract.RecordingEntry.COLUMN_NAME_RECORDING_NAME, recording.getName());
        values.put(RecordingContract.RecordingEntry.COLUMN_NAME_SOUND_TYPE, recording.getSoundType());

        db.insert(
                RecordingContract.RecordingEntry.TABLE_NAME,
                null,
                values);
        recordingListAdapter.add(recording);
        while (writing) {
            Log.e("Writing", "still writing");
        }
        dialog.dismiss();
    }

    public void onDialogPositiveClick(DialogFragment dialog, int position) {
        SQLiteDatabase db = mDbHelper.getWritableDatabase();
        Recording r = currRecordings.get(position);
        // Define 'where' part of query.
        String selection = RecordingContract.RecordingEntry.COLUMN_NAME_RECORDING_NAME + " = ?" +
                "AND " + RecordingContract.RecordingEntry.COLUMN_NAME_SOUND_TYPE + " = ?";
        // Specify arguments in placeholder order.
        String[] selectionArgs = {r.getName(), r.getSoundType() };
        db.delete(RecordingContract.RecordingEntry.TABLE_NAME, selection, selectionArgs);
        recordingListAdapter.remove(currRecordings.get(position));
    }
    public void onDialogNegativeClick(DialogFragment dialog) {

    }

    @Override
    public void onDialogStopClick(DialogFragment dialog) {
        updateCurrRecordings();
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

    /*
    Start writing to a file for a recording dialog
     */
    public void startWriting() {
        writing = true;
    }

    /*
    Ends writing to a file for a recording dialog
     */
    public void endWriting() {
        writing = false;
    }
}
