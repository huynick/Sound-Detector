package com.research.uw.sounddetector;

import android.app.AlertDialog;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SurfaceHolder;
import android.view.View;
import android.widget.AdapterView;
import android.widget.CompoundButton;
import android.widget.ListView;
import android.widget.Switch;

import com.parse.ParseInstallation;
import com.parse.ParsePush;

import java.util.ArrayList;
import java.util.Set;


public class MainScreen extends ActionBarActivity implements AddRecordingDialog.AddRecordingDialogListener, AddNewSoundTypeDialog.NoticeDialogListener, DeleteSoundTypeDialog.DeleteRecordingDialogListener {

    private AudioRecord recorder;
    private WaveformView waveView;
    private SurfaceHolder waveHolder;
    private RecordingTableOpenHelper mDbHelper;

    private ArrayList<SoundType> soundTypeList;
    private ArrayList<String> soundTypeNames;
    private SoundTypeAdapter soundTypeAdapter;
    private ListView listView;

    private boolean recording;
    private boolean listening;

    boolean newSoundType;
    private Recording tempRecording;

    private int bufferSize, sampleRate, channelMode, encodingMode;
    private ArrayList<short[]> bufferList;
    private int MAX_BUFFER_SIZE = 15;
    private short[] mAudioBuffer;

    private boolean writing;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        writing = false;
        setContentView(R.layout.activity_main_screen);
        Switch listener = (Switch)findViewById(R.id.listenerSwitch);
        listener.setChecked(false);
        listener.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                listenerOn(buttonView);
            }
        });
        sampleRate = AudioTrack.getNativeOutputSampleRate(AudioManager.STREAM_SYSTEM);
        channelMode = AudioFormat.CHANNEL_IN_MONO;
        encodingMode = AudioFormat.ENCODING_PCM_16BIT;
        bufferSize = AudioRecord.getMinBufferSize(sampleRate, channelMode, encodingMode);
        mAudioBuffer = new short[bufferSize];
        mDbHelper = new RecordingTableOpenHelper(getBaseContext());
        bufferList = new ArrayList<short[]>();
        listening = false;
        newSoundType = false;
        //Instantiate Action Bar
        // Set a toolbar to replace the action bar.
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        ParseInstallation.getCurrentInstallation().saveInBackground();
        ParsePush.subscribeInBackground("Test1");

        //Sound Type list initialization
        updateSoundType();

        soundTypeAdapter = new SoundTypeAdapter(this, android.R.layout.simple_list_item_1, soundTypeList, mDbHelper, getSupportFragmentManager());
        if(soundTypeAdapter.isEmpty()) {
            addSoundType("Uncategorized");
            addSoundType("Garbage Disposal");
            addSoundType("Microwave Beeping");
            addSoundType("Breaking Glass");
            addSoundType("Knocking on Door");
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
    }

    // You need to do the Play Services APK check here too.
    @Override
    protected void onResume() {
        super.onResume();
        Log.e("Main Screen", "Resuming");
        updateSoundType();
        if (listening) {
            startRecording();
        } else {
            stopRecording();
        }
    }

    private void stopRecording() {
        if (recorder != null) {
            recorder.stop();
            recorder = null;
        }
    }

    private void startRecording() {
        stopRecording();
        if (recorder == null) {
            recorder = new AudioRecord(MediaRecorder.AudioSource.MIC, sampleRate, channelMode, encodingMode, bufferSize);
            recorder.startRecording();
        }
    }

    private void updateSoundType() {
        if (soundTypeNames == null) {
            soundTypeNames = new ArrayList<String>();
        } else {
            soundTypeNames.clear();
        }
        if (soundTypeList == null) {
            soundTypeList = new ArrayList<SoundType>();
        } else {
            soundTypeList.clear();
        }
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
        if (soundTypeAdapter != null) {
            soundTypeAdapter.notifyDataSetChanged();
        }
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
        newSoundType = false;
        AddNewSoundTypeDialog dialog = new AddNewSoundTypeDialog();
        dialog.show(getSupportFragmentManager(), "AddSoundType");
    }
    @Override
    public void onFinish(DialogFragment dialog) {
        if (listening) {
            startRecording();
        }
    }

    @Override
    //Add the inputted sound type name to the list
    public void onDialogPositiveClick(DialogFragment dialog, String name) {

        addSoundType(name);
        dialog.dismiss();
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

        if (newSoundType) {
            newSoundType = false;
            Log.e("New sound type:", "name");
            tempRecording.setSoundType(name);
            // Gets the data repository in write mode

            // Create a new map of values, where column names are the keys
            ContentValues recordingValues = new ContentValues();
            recordingValues.put(RecordingContract.RecordingEntry.COLUMN_NAME_FILE_NAME, tempRecording.getFileName());
            recordingValues.put(RecordingContract.RecordingEntry.COLUMN_NAME_RECORDING_NAME, tempRecording.getName());
            recordingValues.put(RecordingContract.RecordingEntry.COLUMN_NAME_SOUND_TYPE, tempRecording.getSoundType());
            recordingValues.put(RecordingContract.RecordingEntry.COLUMN_NAME_FILE_BEGINNING, 0.0);
            recordingValues.put(RecordingContract.RecordingEntry.COLUMN_NAME_FILE_END, 100.0);

            db.insert(
                    RecordingContract.RecordingEntry.TABLE_NAME,
                    null,
                    recordingValues);
        }
    }

    @Override
    //Do nothing
    public void onDialogNegativeClick(DialogFragment dialog) {
        dialog.dismiss();
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

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main_screen, menu);
        return true;
    }

    public void listenerOn(View view) {
        Switch listenerSwitch = (Switch) findViewById(R.id.listenerSwitch);
        if (listenerSwitch.isChecked()) {
            listenerSwitch.setText("ON");
            listening = true;
            startRecording();
        } else {
            listenerSwitch.setText("OFF");
            listening = false;
            stopRecording();
        }
    }

    public boolean instructionsMenu(MenuItem item) {
        Intent i = new Intent(getApplicationContext(), InstructionsScreen.class);
        startActivity(i);
        return true;
    }

    public void display(View view) {
        Intent i = new Intent(getApplicationContext(), Display.class);
        startActivity(i);
    }

    public void addQuickRecording(View view) {
        newSoundType = false;
        stopRecording();
        AddRecordingDialog dialog = new AddRecordingDialog();
        dialog.setSoundType("Uncategorized");
        dialog.setAutoName(getAutoName());
        dialog.setDb(mDbHelper);
        dialog.show(getSupportFragmentManager(), "AddRecording");
    }

    private String getAutoName() {
        SQLiteDatabase db = mDbHelper.getReadableDatabase();
        String soundName = "Uncategorized";
        // Define a projection that specifies which columns from the database
        // you will actually use after this query.
        String[] projection = {
                RecordingContract.RecordingEntry.COLUMN_NAME_RECORDING_NAME
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
        int min = 0;
        if(c.moveToFirst()) {
            String currName = c.getString(c.getColumnIndex(RecordingContract.RecordingEntry.COLUMN_NAME_RECORDING_NAME));
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
        while(c.moveToNext()) {
            String currName = c.getString(c.getColumnIndex(RecordingContract.RecordingEntry.COLUMN_NAME_RECORDING_NAME));
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
        final Recording finalRec = recording;
        Log.e("Recording sound name", recording.getSoundType());
        if(recording.getSoundType().equals("Uncategorized")) {
            Log.e("Uncategorized recording", "");
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setMessage("Would you like to add a new sound type for your uncategorized recording?").setNegativeButton("No", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int id) {
                    // Gets the data repository in write mode
                    SQLiteDatabase db = mDbHelper.getWritableDatabase();

                    // Create a new map of values, where column names are the keys
                    ContentValues values = new ContentValues();
                    values.put(RecordingContract.RecordingEntry.COLUMN_NAME_FILE_NAME, finalRec.getFileName());
                    values.put(RecordingContract.RecordingEntry.COLUMN_NAME_RECORDING_NAME, finalRec.getName());
                    values.put(RecordingContract.RecordingEntry.COLUMN_NAME_SOUND_TYPE, finalRec.getSoundType());
                    values.put(RecordingContract.RecordingEntry.COLUMN_NAME_FILE_BEGINNING, 0.0);
                    values.put(RecordingContract.RecordingEntry.COLUMN_NAME_FILE_END, 100.0);

                    db.insert(
                            RecordingContract.RecordingEntry.TABLE_NAME,
                            null,
                            values);
                }
            }).setPositiveButton(("Yes"), new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int id) {
                    AddNewSoundTypeDialog soundTypeDialog = new AddNewSoundTypeDialog();
                    soundTypeDialog.show(getSupportFragmentManager(), "AddSoundType");
                    newSoundType = true;
                    tempRecording = finalRec;
                }
            });
            while (writing){

            }
            AlertDialog uncatDialog = builder.create();
            uncatDialog.show();
            dialog.dismiss();
        } else {
            // Gets the data repository in write mode
            SQLiteDatabase db = mDbHelper.getWritableDatabase();

            // Create a new map of values, where column names are the keys
            ContentValues values = new ContentValues();
            values.put(RecordingContract.RecordingEntry.COLUMN_NAME_FILE_NAME, recording.getFileName());
            values.put(RecordingContract.RecordingEntry.COLUMN_NAME_RECORDING_NAME, recording.getName());
            values.put(RecordingContract.RecordingEntry.COLUMN_NAME_SOUND_TYPE, recording.getSoundType());
            values.put(RecordingContract.RecordingEntry.COLUMN_NAME_FILE_BEGINNING, 0.0);
            values.put(RecordingContract.RecordingEntry.COLUMN_NAME_FILE_END, 100.0);

            db.insert(
                    RecordingContract.RecordingEntry.TABLE_NAME,
                    null,
                    values);
            while (writing) {
                Log.e("Writing", "still writing");
            }
            dialog.dismiss();
        }
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

