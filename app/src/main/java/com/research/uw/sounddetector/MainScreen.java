package com.research.uw.sounddetector;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.media.AudioFormat;
import android.media.MediaRecorder;
import android.os.AsyncTask;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.TaskStackBuilder;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SurfaceHolder;
import android.view.View;
import android.widget.ToggleButton;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.media.AudioManager;
import android.os.Vibrator;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.gcm.GoogleCloudMessaging;
import com.parse.Parse;
import com.parse.ParseInstallation;

import java.io.IOException;
import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicInteger;


public class MainScreen extends ActionBarActivity implements AddRecordingDialog.AddRecordingDialogListener {

    private AudioRecord recorder;
    private WaveformView waveView;
    private SurfaceHolder waveHolder;
    private RecordingThread recordingThread;
    private RecordingTableOpenHelper mDbHelper;

    private int bufferSize;
    private ArrayList<short[]> bufferList;
    private int MAX_BUFFER_SIZE = 15;
    private short[] mAudioBuffer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_screen);
        ToggleButton off = (ToggleButton)findViewById(R.id.ServiceOff);
        off.setChecked(true);
        int sampleRate = AudioTrack.getNativeOutputSampleRate(AudioManager.STREAM_SYSTEM);
        int channelMode = AudioFormat.CHANNEL_IN_MONO;
        int encodingMode = AudioFormat.ENCODING_PCM_16BIT;
        bufferSize = AudioRecord.getMinBufferSize(sampleRate, channelMode, encodingMode);
        recorder = new AudioRecord(MediaRecorder.AudioSource.MIC, sampleRate, channelMode, encodingMode, bufferSize);
        waveView = (WaveformView) findViewById(R.id.waveView);
        waveHolder = waveView.getHolder();
        mAudioBuffer = new short[bufferSize];
        mDbHelper = new RecordingTableOpenHelper(getBaseContext());
        bufferList = new ArrayList<short[]>();

        //Instantiate Action Bar
        // Set a toolbar to replace the action bar.
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        ParseInstallation.getCurrentInstallation().saveInBackground();
    }

    // You need to do the Play Services APK check here too.
    @Override
    protected void onResume() {
        super.onResume();
    }



    @Override
    protected void onDestroy() {
        super.onDestroy();
        if(recordingThread != null) {
            recordingThread.stopRunning();
            recordingThread = null;
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main_screen, menu);
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

    public void serviceOn(View view) {
        ToggleButton on = (ToggleButton)findViewById(R.id.ServiceOn);
        ToggleButton off = (ToggleButton)findViewById(R.id.ServiceOff);
        on.setChecked(true);
        off.setChecked(false);
        recorder.startRecording();

    }

    public void serviceOff(View view) {
        ToggleButton on = (ToggleButton)findViewById(R.id.ServiceOn);
        ToggleButton off = (ToggleButton)findViewById(R.id.ServiceOff);
        on.setChecked(false);
        off.setChecked(true);
        recorder.stop();
    }

    public void meterOn(View view) {
        ToggleButton on = (ToggleButton)findViewById(R.id.MeterOn);
        ToggleButton off = (ToggleButton)findViewById(R.id.MeterOff);
        on.setChecked(true);
        off.setChecked(false);
        if(recordingThread == null) {
            recordingThread = new RecordingThread();
            recordingThread.start();
        }
    }

    public void meterOff(View view) {
        ToggleButton on = (ToggleButton)findViewById(R.id.MeterOn);
        ToggleButton off = (ToggleButton)findViewById(R.id.MeterOff);
        on.setChecked(false);
        off.setChecked(true);
        if(recordingThread != null) {
            recordingThread.stopRunning();
            recordingThread = null;
        }
    }

    public void meterOff() {
        ToggleButton on = (ToggleButton)findViewById(R.id.MeterOn);
        ToggleButton off = (ToggleButton)findViewById(R.id.MeterOff);
        on.setChecked(false);
        off.setChecked(true);
        if(recordingThread != null) {
            recordingThread.stopRunning();
            recordingThread = null;
        }
    }

    public void help(View view) {
        HelpDialog dialog = new HelpDialog();
        dialog.show(getSupportFragmentManager(), "HelpDialog");
        RecordingTableOpenHelper mDbHelper = new RecordingTableOpenHelper(getBaseContext());
        SQLiteDatabase db = mDbHelper.getWritableDatabase();
        db.delete(RecordingContract.RecordingEntry.TABLE_NAME, null, null);
        db.delete(RecordingContract.RecordingEntry.SOUND_TABLE_NAME, null, null);
    }

    public boolean help(MenuItem item) {
        HelpDialog dialog = new HelpDialog();
        dialog.show(getSupportFragmentManager(), "HelpDialog");
        return true;
    }

    public boolean settingsMenu(MenuItem item) {
        meterOff();
        Intent i = new Intent(getApplicationContext(), SettingsScreen.class);
        startActivity(i);
        return true;
    }

    public void manageRecordings(View view) {
        meterOff();
        Intent i = new Intent(getApplicationContext(), SoundTypeList.class);
        startActivity(i);
    }

    public void addQuickRecording(View view) {
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
    }


    private class RecordingThread extends Thread {

        private boolean mShouldContinue = true;

        @Override
        public void run() {
            android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_AUDIO);

            AudioRecord record = new AudioRecord(MediaRecorder.AudioSource.MIC, 44100,
            AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT, bufferSize);
            record.startRecording();

            while (shouldContinue()) {
                record.read(mAudioBuffer, 0, bufferSize);
                if(bufferList.size() >= MAX_BUFFER_SIZE) {
                    bufferList.remove(0);
                }
                bufferList.add(mAudioBuffer);
                analyzeAndNotify(mAudioBuffer);
                waveView.updateAudioData(mAudioBuffer);
            }

            record.stop();
            record.release();
        }

        /**
         * Gets a value indicating whether the thread should continue running.
         *
         * @return true if the thread should continue running or false if it should stop
         */
        private synchronized boolean shouldContinue() {
            return mShouldContinue;
        }

        /** Notifies the thread that it should stop running at the next opportunity. */
        public synchronized void stopRunning() {
            mShouldContinue = false;
        }
    }

    private void analyzeAndNotify(short[] mAudioBuffer) {
        int sum = 0;
        for (int j = 0; j < mAudioBuffer.length; j++) {
            sum += Math.abs(mAudioBuffer[j]);
        }
        sum = sum / mAudioBuffer.length;
        int mId = 0;
        if (sum > 2000.0f) {
            notify("Too loud!");
        }
    }

    private void notify(String text) {
        int mId = 0;
        Vibrator v = (Vibrator) this.getSystemService(Context.VIBRATOR_SERVICE);
        // Vibrate for 500 milliseconds
        v.vibrate(500);
        NotificationCompat.Builder mBuilder =
                new NotificationCompat.Builder(this)
                        .setSmallIcon(R.drawable.ic_launcher)
                        .setContentTitle("Sound Detector")
                        .setContentText("Too loud!");
        // Creates an explicit intent for an Activity in your app
        Intent resultIntent = new Intent(this, MainScreen.class);

        // The stack builder object will contain an artificial back stack for the
        // started Activity.
        // This ensures that navigating backward from the Activity leads out of
        // your application to the Home screen.
        TaskStackBuilder stackBuilder = TaskStackBuilder.create(this);
        // Adds the back stack for the Intent (but not the Intent itself)
        stackBuilder.addParentStack(MainScreen.class);
        // Adds the Intent that starts the Activity to the top of the stack
        stackBuilder.addNextIntent(resultIntent);
        PendingIntent resultPendingIntent =
                stackBuilder.getPendingIntent(
                        0,
                        PendingIntent.FLAG_UPDATE_CURRENT
                );
        mBuilder.setContentIntent(resultPendingIntent);
        NotificationManager mNotificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        // mId allows you to update the notification later on.
        mNotificationManager.notify(mId, mBuilder.build());
    }
}

