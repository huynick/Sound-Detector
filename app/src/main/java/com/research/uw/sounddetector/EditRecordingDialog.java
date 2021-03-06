package com.research.uw.sounddetector;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.media.MediaPlayer;
import android.media.SoundPool;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.v4.app.DialogFragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.SurfaceHolder;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;

import com.dropbox.client2.DropboxAPI;
import com.dropbox.client2.android.AndroidAuthSession;
import com.dropbox.client2.exception.DropboxException;
import com.dropbox.client2.session.AppKeyPair;

import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.io.DataInputStream;
import java.util.Scanner;

public class EditRecordingDialog extends DialogFragment {
    // For Dropbox
    final static private String APP_KEY = "owz8xcak9sdvvsw";
    final static private String APP_SECRET = "343f1aa8mk2cpn5";

    private DropboxAPI<AndroidAuthSession> mDBApi;
    private Recording recording;
    private ArrayList<String> soundTypeList;
    private Spinner soundTypes;
    private EditText recordingNameEditText;
    private Button playButton, saveButton, cancelButton;
    private int sampleRate;
    private double begin, end;
    private StaticLineWaveformView waveform;
    private AudioTrack audioTrack;
    private RecordingTableOpenHelper dbHelper;

    public interface EditRecordingDialogListener {
        public void onDialogStopClick(DialogFragment dialog);
    }

    // Use this instance of the interface to deliver action events
    EditRecordingDialogListener mListener;

    // Override the Fragment.onAttach() method to instantiate the NoticeDialogListener
    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        // Verify that the host activity implements the callback interface
        try {
            // Instantiate the NoticeDialogListener so we can send events to the host
            mListener = (EditRecordingDialogListener) activity;
        } catch (ClassCastException e) {
            // The activity doesn't implement the interface, throw exception
            throw new ClassCastException(activity.toString()
                    + " must implement NoticeDialogListener");
        }
    }

    public void setRecording(Recording recording) {
        this.recording = recording;
    }

    public void setDb(RecordingTableOpenHelper db) {
        this.dbHelper = db;
    }

    static EditRecordingDialog newInstance(Recording recording, RecordingTableOpenHelper db) {
        EditRecordingDialog f = new EditRecordingDialog();


        f.setRecording(recording);
        f.setDb(db);

        return f;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        LayoutInflater inflater = getActivity().getLayoutInflater();
        View view = inflater.inflate(R.layout.activity_dialog_edit_recording, null);
        sampleRate = AudioTrack.getNativeOutputSampleRate(AudioManager.STREAM_SYSTEM);
        builder.setView(view);
        soundTypeList = new ArrayList<String>();
        waveform = (StaticLineWaveformView)view.findViewById(R.id.waveView);
        int minBufferSize = AudioTrack.getMinBufferSize(sampleRate, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT);
        String filepath = recording.getFileName();
        waveform.updateAudioData(filepath);
        waveform.updateBeginAndEnd(recording.getStart(), recording.getEnd());
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        SharedPreferences settings = getActivity().getSharedPreferences("Access Token", Activity.MODE_PRIVATE);
        String accessToken = settings.getString("Access Token", "Not found");
        AppKeyPair appKeys = new AppKeyPair(APP_KEY, APP_SECRET);
        AndroidAuthSession session = new AndroidAuthSession(appKeys, accessToken);
        mDBApi = new DropboxAPI<AndroidAuthSession>(session);

        //Instantiate Seekbar
        RangeSeekBar<Double> seekBar = new RangeSeekBar<Double>(0.0, 100.0, this.getActivity().getBaseContext());
        seekBar.setSelectedMaxValue(recording.getEnd());
        seekBar.setSelectedMinValue(recording.getStart());
        begin = recording.getStart();
        end = recording.getEnd();

        seekBar.setNotifyWhileDragging(true);
        seekBar.setOnRangeSeekBarChangeListener(new RangeSeekBar.OnRangeSeekBarChangeListener<Double>() {
            @Override
            public void onRangeSeekBarValuesChanged(RangeSeekBar<?> bar, Double minValue, Double maxValue) {
                begin = minValue;
                end = maxValue;
                waveform.updateBeginAndEnd(begin, end);
            }
        });
        ViewGroup layout = (ViewGroup) view.findViewById(R.id.seekBar);
        layout.addView(seekBar);

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
        soundTypes = (Spinner) view.findViewById(R.id.soundTypes);
        recordingNameEditText = (EditText) view.findViewById(R.id.nameEditText);
        playButton = (Button) view.findViewById(R.id.playButton);
        saveButton = (Button) view.findViewById(R.id.saveButton);
        cancelButton = (Button) view.findViewById(R.id.cancelButton);

        ArrayAdapter<String> soundTypeAdapter = new ArrayAdapter<String>(getActivity(), android.R.layout.simple_list_item_1, soundTypeList);
        soundTypes.setAdapter(soundTypeAdapter);
        int pos = 0;
        for (int i = 0; i < soundTypeList.size(); i++) {
            if (soundTypeList.get(i).equals(recording.getSoundType())) {
                pos = i;
            }
        }
        soundTypes.setSelection(pos);

        recordingNameEditText.setText(recording.getName());

        playButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.e("File name", recording.getFileName());
                //waveform.startPlaying();
                playWav();
            }
        });

        saveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // New value for one column
                ContentValues values = new ContentValues();
                values.put(RecordingContract.RecordingEntry.COLUMN_NAME_SOUND_TYPE, (String)soundTypes.getSelectedItem());
                Log.e("Selected item", (String)soundTypes.getSelectedItem());
                values.put(RecordingContract.RecordingEntry.COLUMN_NAME_RECORDING_NAME, recordingNameEditText.getText().toString());
                values.put(RecordingContract.RecordingEntry.COLUMN_NAME_FILE_BEGINNING, begin);
                values.put(RecordingContract.RecordingEntry.COLUMN_NAME_FILE_END, end);

                SQLiteDatabase writeDb = dbHelper.getWritableDatabase();

                // Which row to update, based on the ID
                String selection = RecordingContract.RecordingEntry.COLUMN_NAME_RECORDING_NAME + " = ? AND " +
                        RecordingContract.RecordingEntry.COLUMN_NAME_FILE_NAME + " = ?";
                String[] selectionArgs = { recording.getName(), recording.getFileName() };

                final String filename = Environment.getExternalStorageDirectory().getAbsolutePath() + "/log.txt";
                RandomAccessFile raf = null;
                String content = "Recording named: " + recording.getName() + " under Sound Type: "
                            + recording.getSoundType() + " Begin: " + begin + "End: " + end + "\r\n";
                try {
                    File f = new File(filename);
                    raf = new RandomAccessFile(new File(filename), "rw");
                    if (f.length() > 0) {
                        raf.seek(f.length());
                    }
                    raf.writeBytes(content);
                } catch (Exception e) {
                    e.printStackTrace();
                }

                int count = writeDb.update(
                        RecordingContract.RecordingEntry.TABLE_NAME,
                        values,
                        selection,
                        selectionArgs);
                mListener.onDialogStopClick(EditRecordingDialog.this);
                dismiss();
            }
        });

        cancelButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dismiss();
            }
        });

        return builder.create();
    }

    public void playWav(){
        PlayBackThread playback = new PlayBackThread();
        waveform.startPlaying();
        playback.start();
    }

    private class PlayBackThread extends Thread {

        private boolean mShouldContinue = true;

        @Override
        public void run() {
            int minBufferSize = AudioTrack.getMinBufferSize(sampleRate, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT);
            if (minBufferSize < sampleRate) {
                minBufferSize = sampleRate;
            }
            AudioTrack at = new AudioTrack(AudioManager.STREAM_SYSTEM, sampleRate, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT, minBufferSize, AudioTrack.MODE_STREAM);
            String filepath = recording.getFileName();
            File f = new File(recording.getFileName());
            long length = f.length();

            int i = 0;
            byte[] s = new byte[minBufferSize * 2];
            int dataPlayed = 0;
            try {
                FileInputStream fin = new FileInputStream(filepath);
                DataInputStream dis = new DataInputStream(fin);
                int skip = (int)(length * begin / 100.0);
                if (skip % 2 == 1) {
                    skip = skip - 1;
                }
                dis.skipBytes(skip);
                int dataEnd = (int)(length * (end - begin) / 100.0);
                System.out.println(dataEnd);
                at.play();
                mShouldContinue = true;
                while(i > -1 && mShouldContinue){
                    if (dataEnd < dataPlayed + minBufferSize) {
                        i = dis.read(s, 0, dataEnd - dataPlayed);
                        for (int j = i; j < minBufferSize; j++ ) {
                            s[j] = 0;
                        }
                        mShouldContinue = false;
                    } else {
                        i = dis.read(s, 0, minBufferSize);
                    }
                    if (i > -1) {
                        at.write(s, 0, minBufferSize);
                        dataPlayed += i;
                    }
                }
                at.write(s, 0, minBufferSize);

                System.out.println(dataPlayed);
                at.stop();
                at.release();
                dis.close();
                fin.close();

            } catch (FileNotFoundException e) {
                // TODO
                e.printStackTrace();
            } catch (IOException e) {
                // TODO
                e.printStackTrace();
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
}
