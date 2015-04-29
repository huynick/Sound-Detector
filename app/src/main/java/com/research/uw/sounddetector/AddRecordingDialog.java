package com.research.uw.sounddetector;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.media.MediaRecorder;
import android.os.*;
import android.support.v4.app.DialogFragment;
import android.text.format.Time;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.ToggleButton;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;

public class AddRecordingDialog extends DialogFragment {

    private MediaRecorder mRecorder;
    private LineWaveformView waveView;
    private RecordingThread recordingThread;
    private int bufferSize;
    private Recording recording;
    private EditText nameEditText;
    private String mFileName;
    private String soundType;
    private String autoName;
    private RecordingTableOpenHelper dbHelper;
    private ArrayList<String> soundTypeList;

    private short[] mAudioBuffer;
    private byte[] byteBuffer;

    private static final String LOG_TAG = "Recorder";

    public void setSoundType(String soundType) {
        this.soundType = soundType;
    }
    public void setDb(RecordingTableOpenHelper db) {
        this.dbHelper = db;
    }

    public interface AddRecordingDialogListener {
        public void onDialogStopClick(DialogFragment dialog, Recording recording);
    }

    // Use this instance of the interface to deliver action events
    AddRecordingDialogListener mListener;

    // Override the Fragment.onAttach() method to instantiate the NoticeDialogListener
    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        // Verify that the host activity implements the callback interface
        try {
            // Instantiate the NoticeDialogListener so we can send events to the host
            mListener = (AddRecordingDialogListener) activity;
        } catch (ClassCastException e) {
            // The activity doesn't implement the interface, throw exception
            throw new ClassCastException(activity.toString()
                    + " must implement NoticeDialogListener");
        }
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        LayoutInflater inflater = getActivity().getLayoutInflater();
        View view = inflater.inflate(R.layout.activity_dialog_add_recording, null);
        int sampleRate = AudioTrack.getNativeOutputSampleRate(AudioManager.STREAM_SYSTEM);
        int channelMode = AudioFormat.CHANNEL_IN_MONO;
        int encodingMode = AudioFormat.ENCODING_PCM_16BIT;
        bufferSize = AudioRecord.getMinBufferSize(sampleRate, channelMode, encodingMode);
        if (bufferSize > sampleRate) {
            bufferSize = sampleRate;
        }
        System.out.println(bufferSize);
        int detectAfterEvery = (int)((float)sampleRate * 1.0f);

        if (detectAfterEvery > bufferSize)
        {
            Log.w("Add Dialog", "Increasing buffer to hold enough samples " + detectAfterEvery + " was: " + bufferSize);
            bufferSize = detectAfterEvery;
        }
        mAudioBuffer = new short[bufferSize];
        waveView = (LineWaveformView) view.findViewById(R.id.waveView);
        nameEditText = (EditText) view.findViewById(R.id.nameEditText);
        nameEditText.setOnFocusChangeListener(new View.OnFocusChangeListener()
        {
            @Override
            public void onFocusChange(View v, boolean hasFocus)
            {
                if (hasFocus==true)
                {
                    if (nameEditText.getText().toString().compareTo(autoName)==0)
                    {
                        nameEditText.setText("");
                    }
                }
            }
        });
        nameEditText.setText(autoName);

        SQLiteDatabase db = dbHelper.getReadableDatabase();
        soundTypeList = new ArrayList<String>();

        // Define a projection that specifies which columns from the database
        // you will actually use after this query.
        String[] projection = {
                RecordingContract.RecordingEntry._ID,
                RecordingContract.RecordingEntry.COLUMN_NAME_SOUND_TYPE_NAME
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
            soundTypeList.add(s);
        }
        while(c.moveToNext()) {
            String s = c.getString(c.getColumnIndex(RecordingContract.RecordingEntry.COLUMN_NAME_SOUND_TYPE_NAME));
            soundTypeList.add(s);
        }
        Spinner soundTypes = (Spinner) view.findViewById(R.id.soundTypes);

        ArrayAdapter<String> soundTypeAdapter = new ArrayAdapter<String>(getActivity(), android.R.layout.simple_list_item_1, soundTypeList);
        soundTypes.setAdapter(soundTypeAdapter);
        soundTypes.setSelection(soundTypeList.indexOf(soundType));
        //TODO:Add Unrecognized IMMEDIATELY

        builder.setView(view).setTitle(R.string.add_recording);
        ToggleButton recordButton = (ToggleButton) view.findViewById(R.id.recordButton);
        recordButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(((ToggleButton) v.findViewById(R.id.recordButton)).isChecked()) {
                    mFileName = Environment.getExternalStorageDirectory().getAbsolutePath() + "/" + nameEditText.getText().toString() + ".wav";
                    if(recordingThread == null) {
                        recordingThread = new RecordingThread();
                        recordingThread.start();
                    }


                } else {
                    if(recordingThread != null) {
                        recordingThread.stopRunning();
                        recordingThread = null;
                        recording = new Recording(nameEditText.getText().toString(), mFileName, soundType);
                    }
                    mListener.onDialogStopClick(AddRecordingDialog.this, recording);
                    dismiss();
                }
            }
        });
        byteBuffer = new byte[bufferSize];
        mAudioBuffer = new short[bufferSize];
        return builder.create();
    }

    boolean m_isRun = true;
    byte[] buffer = new byte[44100];

    public void loopback() {
        AudioRecord m_record;
        AudioTrack m_track;
        int bufferSize = AudioRecord.getMinBufferSize(44100, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT);
        if (bufferSize > 44100) {
            bufferSize = 44100;
        }

        m_record = new AudioRecord(MediaRecorder.AudioSource.MIC,
                44100, AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT, bufferSize * 1);

        m_track = new AudioTrack(AudioManager.STREAM_ALARM,
                44100, AudioFormat.CHANNEL_OUT_MONO,
                AudioFormat.ENCODING_PCM_16BIT, bufferSize * 1,
                AudioTrack.MODE_STREAM);
        m_track.setPlaybackRate(44100);

        m_record.startRecording();
        Log.i(LOG_TAG,"Audio Recording started");
        m_track.play();
        Log.i(LOG_TAG,"Audio Playing started");

        while (m_isRun) {
            m_record.read(buffer, 0, 44100);
            m_track.write(buffer, 0, buffer.length);
        }
    }


    public Recording getRecording() {
        return recording;
    }

    public void setAutoName(String s) {
        this.autoName = s;
    }

    private class RecordingThread extends Thread {

        private boolean mShouldContinue = true;
        private int dataRecorded = 0;

        @Override
        public void run() {
            android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_URGENT_AUDIO);

            AudioRecord record = new AudioRecord(MediaRecorder.AudioSource.MIC, AudioTrack.getNativeOutputSampleRate(AudioManager.STREAM_SYSTEM),
            AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT, bufferSize);
            ArrayList<Byte> audioBytes = new ArrayList<Byte>();

            record.startRecording();
            Time time = new Time();   time.setToNow();
            Log.d("TIME TEST", Long.toString(time.toMillis(false)));
            FileOutputStream os = null;
            try {
                os = new FileOutputStream(mFileName);
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }

            while (shouldContinue()) {
                record.read(mAudioBuffer, 0, bufferSize);
                int read = record.read(byteBuffer, 0, bufferSize);
                System.out.println("read: " + read);
                waveView.updateAudioData(mAudioBuffer);
                try {
                    if (os != null) {
                        os.write(byteBuffer, 0, bufferSize);
                        dataRecorded += bufferSize;
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            //writeWavHeader();
            System.out.println(dataRecorded);
            time.setToNow();
            Log.d("TIME TEST", Long.toString(time.toMillis(false)));

            record.stop();
            record.release();
//            loopback();
        }

        /**
         * Gets a value indicating whether the thread should continue running.
         *
         * @return true if the thread should continue running or false if it should stop
         */
        private synchronized boolean shouldContinue() {
            return mShouldContinue;
        }

        private void writeWavHeader() {
            try {
                FileOutputStream out = new FileOutputStream(mFileName);
                int totalDataLen = dataRecorded + 44;
                int channels = 1;
                int longSampleRate = AudioTrack.getNativeOutputSampleRate(AudioManager.STREAM_SYSTEM);
                int byteRate = longSampleRate * 16 * 1 / 8;


                byte[] header = new byte[44];

                header[0] = 'R';  // RIFF/WAVE header
                header[1] = 'I';
                header[2] = 'F';
                header[3] = 'F';
                header[4] = (byte) (totalDataLen & 0xff);
                header[5] = (byte) ((totalDataLen >> 8) & 0xff);
                header[6] = (byte) ((totalDataLen >> 16) & 0xff);
                header[7] = (byte) ((totalDataLen >> 24) & 0xff);
                header[8] = 'W';
                header[9] = 'A';
                header[10] = 'V';
                header[11] = 'E';
                header[12] = 'f';  // 'fmt ' chunk
                header[13] = 'm';
                header[14] = 't';
                header[15] = ' ';
                header[16] = 16;  // 4 bytes: size of 'fmt ' chunk
                header[17] = 0;
                header[18] = 0;
                header[19] = 0;
                header[20] = 1;  // format = 1
                header[21] = 0;
                header[22] = (byte) channels;
                header[23] = 0;
                header[24] = (byte) (longSampleRate & 0xff);
                header[25] = (byte) ((longSampleRate >> 8) & 0xff);
                header[26] = (byte) ((longSampleRate >> 16) & 0xff);
                header[27] = (byte) ((longSampleRate >> 24) & 0xff);
                header[28] = (byte) (byteRate & 0xff);
                header[29] = (byte) ((byteRate >> 8) & 0xff);
                header[30] = (byte) ((byteRate >> 16) & 0xff);
                header[31] = (byte) ((byteRate >> 24) & 0xff);
                header[32] = (byte) (1 * 16 / 8);  // block align
                header[33] = 0;
                header[34] = 16;  // bits per sample
                header[35] = 0;
                header[36] = 'd';
                header[37] = 'a';
                header[38] = 't';
                header[39] = 'a';
                header[40] = (byte) (dataRecorded & 0xff);
                header[41] = (byte) ((dataRecorded >> 8) & 0xff);
                header[42] = (byte) ((dataRecorded >> 16) & 0xff);
                header[43] = (byte) ((dataRecorded >> 24) & 0xff);

                out.write(header, 0, 44);
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        /** Notifies the thread that it should stop running at the next opportunity. */
        public synchronized void stopRunning() {
            mShouldContinue = false;
        }
    }
}
