package com.research.uw.sounddetector;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.os.Vibrator;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.TaskStackBuilder;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.View;

import java.util.ArrayList;

public class Display extends ActionBarActivity implements AddRecordingDialog.AddRecordingDialogListener {

    private WaveformView waveView;
    private AudioRecord recorder;
    private RecordingThread recordingThread;
    private final int MAX_BUFFER_SIZE = 15;
    private short[] mAudioBuffer;
    private int bufferSize, sampleRate, channelMode, encodingMode;
    private RecordingTableOpenHelper mDbHelper;
    private ArrayList<short[]> bufferList;
    private boolean writing;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        writing = false;
        setContentView(R.layout.activity_display);

        waveView = (WaveformView) findViewById(R.id.waveView);
        sampleRate = AudioTrack.getNativeOutputSampleRate(AudioManager.STREAM_SYSTEM);
        channelMode = AudioFormat.CHANNEL_IN_MONO;
        encodingMode = AudioFormat.ENCODING_PCM_16BIT;
        bufferSize = AudioRecord.getMinBufferSize(sampleRate, channelMode, encodingMode);
        waveView = (WaveformView) findViewById(R.id.waveView);
        mAudioBuffer = new short[bufferSize];
        bufferList = new ArrayList<short[]>();
        mDbHelper = new RecordingTableOpenHelper(getBaseContext());

        startRecording();
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.e("pause", "Pausing now");
        recordingThread.stopRunning();
        recordingThread = null;
        if (recorder != null) {
            recorder.stop();
            recorder = null;
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.e("resume", "resuming");
        startRecording();
    }

    private void stopRecording() {
        if (recordingThread != null) {
            recordingThread.stopRunning();
            recordingThread = null;
        }
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

        if (recordingThread == null) {
            Log.e("resume", "res");
            recordingThread = new RecordingThread();
            recordingThread.start();
        }
    }

    @Override
    public void onFinish(DialogFragment dialog) {
        startRecording();
        Log.e("Hi", "Hi");
    }

    @Override
    public void onDialogStopClick(DialogFragment dialog, Recording recording) {
        Log.e("resume", "stop");
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
        startRecording();
    }

    public void quickAddRecording(View view) {
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

    private class RecordingThread extends Thread {

        private boolean mShouldContinue = true;

        @Override
        public void run() {
            android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_AUDIO);
            while (shouldContinue()) {
                recorder.read(mAudioBuffer, 0, bufferSize);
                if(bufferList.size() >= MAX_BUFFER_SIZE) {
                    bufferList.remove(0);
                }
                bufferList.add(mAudioBuffer);
                analyzeAndNotify(mAudioBuffer);
                waveView.updateAudioData(mAudioBuffer);
            }
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
            //notify("Too loud!");
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
