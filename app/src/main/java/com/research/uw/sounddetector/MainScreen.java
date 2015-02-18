package com.research.uw.sounddetector;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.media.AudioFormat;
import android.media.MediaRecorder;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SurfaceHolder;
import android.view.View;
import android.widget.ToggleButton;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.media.AudioManager;



public class MainScreen extends ActionBarActivity {

    private AudioRecord recorder;
    private WaveformView waveView;
    private SurfaceHolder waveHolder;
    private RecordingThread recordingThread;

    private int bufferSize;

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

    public void help(View view) {
        HelpDialog dialog = new HelpDialog();
        dialog.show(getSupportFragmentManager(), "HelpDialog");
    }

    public void manageRecordings(View view) {
        Intent i = new Intent(getApplicationContext(), SoundType.class);
        startActivity(i);
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

