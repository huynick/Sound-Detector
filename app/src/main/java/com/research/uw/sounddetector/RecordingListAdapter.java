package com.research.uw.sounddetector;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
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

public class RecordingListAdapter extends ArrayAdapter<Recording> implements DeleteRecordingDialog.NoticeDialogListener {
    private ArrayList<Recording> recordings;
    private Context context;
    private FragmentManager fragmentManager;

    public RecordingListAdapter(Context context, int resource, ArrayList<Recording> recordings, FragmentManager fragmentManager) {
        super(context, resource);
        this.recordings = recordings;
        this.fragmentManager = fragmentManager;
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
        }
        Button edit = (Button)v.findViewById(R.id.edit);
        edit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

            }
        });
        Button remove = (Button)v.findViewById(R.id.remove);
        remove.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                DeleteRecordingDialog deleteDialog = new DeleteRecordingDialog();
                Bundle b = new Bundle();
                b.putInt("pos", pos);
                deleteDialog.show(fragmentManager, "delete");
            }
        });

        return v;
    }

    public void onDialogPositiveClick(DialogFragment dialog, int position) {
        recordings.remove(position);
    }
    public void onDialogNegativeClick(DialogFragment dialog) {

    }

}
