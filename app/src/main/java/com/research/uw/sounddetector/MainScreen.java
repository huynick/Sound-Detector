package com.research.uw.sounddetector;

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

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.gcm.GoogleCloudMessaging;
import com.parse.Parse;
import com.parse.ParseInstallation;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicInteger;


public class MainScreen extends ActionBarActivity implements AddRecordingDialog.AddRecordingDialogListener {

    private AudioRecord recorder;
    private WaveformView waveView;
    private SurfaceHolder waveHolder;
    private RecordingThread recordingThread;
    private RecordingTableOpenHelper mDbHelper;

    private int bufferSize;

    private short[] mAudioBuffer;

//    // Variables for registering to GCM
//
//    public static final String EXTRA_MESSAGE = "message";
//    public static final String PROPERTY_REG_ID = "registration_id";
//    private static final String PROPERTY_APP_VERSION = "appVersion";
//    private final static int PLAY_SERVICES_RESOLUTION_REQUEST = 9000;
//
//    /**
//     * Substitute you own sender ID here. This is the project number you got
//     * from the API Console, as described in "Getting Started."
//     */
//    String SENDER_ID = "AIzaSyCUIX66b7oXKbEW0lGH9-K6wsDTGJBCQR4";
//
//    /**
//     * Tag used on log messages.
//     */
//    static final String TAG = "GCMDemo";
//
//    GoogleCloudMessaging gcm;
//    AtomicInteger msgId = new AtomicInteger();
//    SharedPreferences prefs;
//    Context context;
//
//    String regid;

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

        //Instantiate Action Bar
        // Set a toolbar to replace the action bar.
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        Parse.initialize(this.getBaseContext(), "8Dnotr5GlTj7YebZstzzrxcbCSzmHcF1sOVGredV", "yqljM1Je0Bg42YZwiqQeRaLlbHzt5uTlnYXECuI4");
        ParseInstallation.getCurrentInstallation().saveInBackground();
    }

    // You need to do the Play Services APK check here too.
    @Override
    protected void onResume() {
        super.onResume();
    }

//    /**
//     * Check the device to make sure it has the Google Play Services APK. If
//     * it doesn't, display a dialog that allows users to download the APK from
//     * the Google Play Store or enable it in the device's system settings.
//     */
//    private boolean checkPlayServices() {
//        int resultCode = GooglePlayServicesUtil.isGooglePlayServicesAvailable(this);
//        if (resultCode != ConnectionResult.SUCCESS) {
//            if (GooglePlayServicesUtil.isUserRecoverableError(resultCode)) {
//                GooglePlayServicesUtil.getErrorDialog(resultCode, this,
//                        PLAY_SERVICES_RESOLUTION_REQUEST).show();
//            } else {
//                Log.i(TAG, "This device is not supported.");
//                finish();
//            }
//            return false;
//        }
//        return true;
//    }
//
//    /**
//     * Gets the current registration ID for application on GCM service.
//     * <p>
//     * If result is empty, the app needs to register.
//     *
//     * @return registration ID, or empty string if there is no existing
//     *         registration ID.
//     */
//    private String getRegistrationId(Context context) {
//        final SharedPreferences prefs = getGCMPreferences(context);
//        String registrationId = prefs.getString(PROPERTY_REG_ID, "");
//        if (registrationId.isEmpty()) {
//            Log.i(TAG, "Registration not found.");
//            return "";
//        }
//        // Check if app was updated; if so, it must clear the registration ID
//        // since the existing registration ID is not guaranteed to work with
//        // the new app version.
//        int registeredVersion = prefs.getInt(PROPERTY_APP_VERSION, Integer.MIN_VALUE);
//        int currentVersion = getAppVersion(context);
//        if (registeredVersion != currentVersion) {
//            Log.i(TAG, "App version changed.");
//            return "";
//        }
//        return registrationId;
//    }
//
//    /**
//     * @return Application's {@code SharedPreferences}.
//     */
//    private SharedPreferences getGCMPreferences(Context context) {
//        // This sample app persists the registration ID in shared preferences, but
//        // how you store the registration ID in your app is up to you.
//        return getSharedPreferences(MainScreen.class.getSimpleName(),
//                Context.MODE_PRIVATE);
//    }
//
//    /**
//     * @return Application's version code from the {@code PackageManager}.
//     */
//    private static int getAppVersion(Context context) {
//        try {
//            PackageInfo packageInfo = context.getPackageManager()
//                    .getPackageInfo(context.getPackageName(), 0);
//            return packageInfo.versionCode;
//        } catch (PackageManager.NameNotFoundException e) {
//            // should never happen
//            throw new RuntimeException("Could not get package name: " + e);
//        }
//    }
//
//    /**
//     * Registers the application with GCM servers asynchronously.
//     * <p>
//     * Stores the registration ID and app versionCode in the application's
//     * shared preferences.
//     */
//    private void registerInBackground() {
//
//    }
//
//    /**
//     * Sends the registration ID to your server over HTTP, so it can use GCM/HTTP
//     * or CCS to send messages to your app. Not needed for this demo since the
//     * device sends upstream messages to a server that echoes back the message
//     * using the 'from' address in the message.
//     */
//    private void sendRegistrationIdToBackend() {
//        //TODO: If this is actually needed
//    }
//
//    /**
//     * Stores the registration ID and app versionCode in the application's
//     * {@code SharedPreferences}.
//     *
//     * @param context application's context.
//     * @param regId registration ID
//     */
//    private void storeRegistrationId(Context context, String regId) {
//        final SharedPreferences prefs = getGCMPreferences(context);
//        int appVersion = getAppVersion(context);
//        Log.i(TAG, "Saving regId on app version " + appVersion);
//        SharedPreferences.Editor editor = prefs.edit();
//        editor.putString(PROPERTY_REG_ID, regId);
//        editor.putInt(PROPERTY_APP_VERSION, appVersion);
//        editor.commit();
//    } /**
//     * Check the device to make sure it has the Google Play Services APK. If
//     * it doesn't, display a dialog that allows users to download the APK from
//     * the Google Play Store or enable it in the device's system settings.
//     */
//    private boolean checkPlayServices() {
//        int resultCode = GooglePlayServicesUtil.isGooglePlayServicesAvailable(this);
//        if (resultCode != ConnectionResult.SUCCESS) {
//            if (GooglePlayServicesUtil.isUserRecoverableError(resultCode)) {
//                GooglePlayServicesUtil.getErrorDialog(resultCode, this,
//                        PLAY_SERVICES_RESOLUTION_REQUEST).show();
//            } else {
//                Log.i(TAG, "This device is not supported.");
//                finish();
//            }
//            return false;
//        }
//        return true;
//    }
//
//    /**
//     * Gets the current registration ID for application on GCM service.
//     * <p>
//     * If result is empty, the app needs to register.
//     *
//     * @return registration ID, or empty string if there is no existing
//     *         registration ID.
//     */
//    private String getRegistrationId(Context context) {
//        final SharedPreferences prefs = getGCMPreferences(context);
//        String registrationId = prefs.getString(PROPERTY_REG_ID, "");
//        if (registrationId.isEmpty()) {
//            Log.i(TAG, "Registration not found.");
//            return "";
//        }
//        // Check if app was updated; if so, it must clear the registration ID
//        // since the existing registration ID is not guaranteed to work with
//        // the new app version.
//        int registeredVersion = prefs.getInt(PROPERTY_APP_VERSION, Integer.MIN_VALUE);
//        int currentVersion = getAppVersion(context);
//        if (registeredVersion != currentVersion) {
//            Log.i(TAG, "App version changed.");
//            return "";
//        }
//        return registrationId;
//    }
//
//    /**
//     * @return Application's {@code SharedPreferences}.
//     */
//    private SharedPreferences getGCMPreferences(Context context) {
//        // This sample app persists the registration ID in shared preferences, but
//        // how you store the registration ID in your app is up to you.
//        return getSharedPreferences(MainScreen.class.getSimpleName(),
//                Context.MODE_PRIVATE);
//    }
//
//    /**
//     * @return Application's version code from the {@code PackageManager}.
//     */
//    private static int getAppVersion(Context context) {
//        try {
//            PackageInfo packageInfo = context.getPackageManager()
//                    .getPackageInfo(context.getPackageName(), 0);
//            return packageInfo.versionCode;
//        } catch (PackageManager.NameNotFoundException e) {
//            // should never happen
//            throw new RuntimeException("Could not get package name: " + e);
//        }
//    }
//
//    /**
//     * Registers the application with GCM servers asynchronously.
//     * <p>
//     * Stores the registration ID and app versionCode in the application's
//     * shared preferences.
//     */
//    private void registerInBackground() {
//
//    }
//
//    /**
//     * Sends the registration ID to your server over HTTP, so it can use GCM/HTTP
//     * or CCS to send messages to your app. Not needed for this demo since the
//     * device sends upstream messages to a server that echoes back the message
//     * using the 'from' address in the message.
//     */
//    private void sendRegistrationIdToBackend() {
//        //TODO: If this is actually needed
//    }
//
//    /**
//     * Stores the registration ID and app versionCode in the application's
//     * {@code SharedPreferences}.
//     *
//     * @param context application's context.
//     * @param regId registration ID
//     */
//    private void storeRegistrationId(Context context, String regId) {
//        final SharedPreferences prefs = getGCMPreferences(context);
//        int appVersion = getAppVersion(context);
//        Log.i(TAG, "Saving regId on app version " + appVersion);
//        SharedPreferences.Editor editor = prefs.edit();
//        editor.putString(PROPERTY_REG_ID, regId);
//        editor.putInt(PROPERTY_APP_VERSION, appVersion);
//        editor.commit();
//    }




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
}

