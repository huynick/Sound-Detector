package com.research.uw.sounddetector;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentManager;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.view.LayoutInflater;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.HashMap;

public class RecordingListAdapter extends ArrayAdapter<Recording> {
    private ArrayList<Recording> recordings;
    private Context context;
    private FragmentManager fragmentManager;
    private RecordingTableOpenHelper db;

    public RecordingListAdapter(Context context, int resource, ArrayList<Recording> recordings, RecordingTableOpenHelper db, FragmentManager fragmentManager) {
        super(context, resource, recordings);
        this.recordings = recordings;
        this.fragmentManager = fragmentManager;
        this.db = db;
    }

    public View getView(int position, View convertView, ViewGroup parent) {
        final int pos = position;
        View v = convertView;
        if (v == null) {
            LayoutInflater inflater = (LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            v = inflater.inflate(R.layout.edit_recording_element, null);
        }

        final Recording r = recordings.get(position);
        if(r != null) {
            TextView recordingName = (TextView) v.findViewById(R.id.recordingName);
            recordingName.setText(r.getName());
        } else {
            TextView recordingName = (TextView) v.findViewById(R.id.recordingName);
            recordingName.setText("null");
        }
        Button edit = (Button)v.findViewById(R.id.edit);
        edit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                EditRecordingDialog editDialog = EditRecordingDialog.newInstance(r, db);
                editDialog.show(fragmentManager, "edit");
            }
        });
        Button remove = (Button)v.findViewById(R.id.remove);
        remove.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                DeleteRecordingDialog deleteDialog = new DeleteRecordingDialog();
                Bundle b = new Bundle();
                b.putInt("pos", pos);
                deleteDialog.setArguments(b);
                deleteDialog.show(fragmentManager, "delete");
            }
        });

        return v;
    }
}
