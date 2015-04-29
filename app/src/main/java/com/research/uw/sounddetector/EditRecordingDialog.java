package com.research.uw.sounddetector;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.media.MediaPlayer;
import android.media.SoundPool;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.v4.app.DialogFragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.SurfaceHolder;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.io.DataInputStream;
import java.util.Scanner;

public class EditRecordingDialog extends DialogFragment {
    private Recording recording;
    private ArrayList<String> soundTypeList;
    private Spinner soundTypes;
    private EditText recordingNameEditText;
    private Button playButton, saveButton, cancelButton;
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
        builder.setView(view);
        soundTypeList = new ArrayList<String>();
        waveform = (StaticLineWaveformView)view.findViewById(R.id.waveView);

        try {
            FileInputStream fis = new FileInputStream (new File(recording.getFileName()));
            byte[] buf = new byte[80000];
            short[] shortArr = new short[buf.length/2];
            fis.read(buf);
            for (int i = 0; i <buf.length/2 ; i++)
            {
                shortArr[i] = ( (short)( ( buf[i*2] & 0xff )|( buf[i*2 + 1] << 8 ) ) );
            }

            fis.close();
            Log.e("Array", Arrays.toString(shortArr));
            waveform.updateAudioData(shortArr);
        }  catch(IOException e) {
            Log.e("Exception", "IO Exception");
        }

        SQLiteDatabase db = dbHelper.getReadableDatabase();

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
        recordingNameEditText = (EditText) view.findViewById(R.id.recordingNameEditText);
        playButton = (Button) view.findViewById(R.id.playButton);
        saveButton = (Button) view.findViewById(R.id.saveButton);
        cancelButton = (Button) view.findViewById(R.id.cancelButton);

        ArrayAdapter<String> soundTypeAdapter = new ArrayAdapter<String>(getActivity(), android.R.layout.simple_list_item_1, soundTypeList);
        soundTypes.setAdapter(soundTypeAdapter);

        playButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.e("File name", recording.getFileName());
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

                SQLiteDatabase writeDb = dbHelper.getWritableDatabase();

                // Which row to update, based on the ID
                String selection = RecordingContract.RecordingEntry.COLUMN_NAME_RECORDING_NAME + " = ? AND " +
                        RecordingContract.RecordingEntry.COLUMN_NAME_FILE_NAME + " = ?";
                String[] selectionArgs = { recording.getName(), recording.getFileName() };

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
        try {
            FileInputStream fis = new FileInputStream (new File(recording.getFileName()));
            byte[] buf = new byte[80000];
            short[] shortArr = new short[buf.length/2];
            fis.read(buf);
            for (int i = 0; i <buf.length/2 ; i++)
            {
                shortArr[i] = ( (short)( ( buf[i*2] & 0xff )|( buf[i*2 + 1] << 8 ) ) );
            }

            fis.close();
            Log.e("Array", Arrays.toString(shortArr));

            waveform.updateAudioData(shortArr);
        }  catch(IOException e) {
            Log.e("Exception", "IO Exception");
        }



        int minBufferSize = AudioTrack.getMinBufferSize(44100, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT);
        if (minBufferSize < 44100) {
            minBufferSize = 44100;
        }
        AudioTrack at = new AudioTrack(AudioManager.STREAM_MUSIC, 44100, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT, minBufferSize, AudioTrack.MODE_STREAM);
        String filepath = recording.getFileName();

        int i = 0;
        byte[] s = new byte[minBufferSize];
        try {
            FileInputStream fin = new FileInputStream(filepath);
            DataInputStream dis = new DataInputStream(fin);

            at.play();
            int dataPlayed = 0;
            while((i = dis.read(s, 0, minBufferSize)) > -1){
                at.write(s, 0, i);
                dataPlayed += i;
            }
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

}
