package com.research.uw.sounddetector;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ContentValues;
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
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

public class SoundTypeAdapter extends ArrayAdapter<SoundType> {
    private ArrayList<SoundType> soundTypes;
    private Context context;
    private FragmentManager fragmentManager;
    private RecordingTableOpenHelper db;
    private Set<String> defaultSounds;

    public SoundTypeAdapter(Context context, int resource, ArrayList<SoundType> soundTypes, RecordingTableOpenHelper db, FragmentManager fragmentManager) {
        super(context, resource, soundTypes);
        intializeDefaultSoundSet();
        this.soundTypes = soundTypes;
        this.fragmentManager = fragmentManager;
        this.db = db;
    }

    private  void intializeDefaultSoundSet() {
        defaultSounds = new HashSet<String>();
        defaultSounds.add("Uncategorized");
        defaultSounds.add("Garbage Disposal");
        defaultSounds.add("Microwave Beeping");
        defaultSounds.add("Breaking Glass");
        defaultSounds.add("Knocking on Door");
    }

    public View getView(int position, View convertView, ViewGroup parent) {
        final int pos = position;
        View v = convertView;
        if (v == null) {
            LayoutInflater inflater = (LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            v = inflater.inflate(R.layout.sound_type_element, null);
        }

        final SoundType soundType = soundTypes.get(position);
        final String s = soundTypes.get(position).getName();
        if(soundType != null) {
            TextView soundTypeName = (TextView) v.findViewById(R.id.soundTypeName);
            soundTypeName.setText(s);
        } else {
            TextView soundTypeName = (TextView) v.findViewById(R.id.soundTypeName);
            soundTypeName.setText(s);
        }

        Button delete = (Button)v.findViewById(R.id.delete);
        delete.setFocusable(false);
        delete.setFocusableInTouchMode(false);

        if(s.equals("Uncategorized")) {
            CheckBox checkbox = (CheckBox) v.findViewById(R.id.checkBox);
            checkbox.setVisibility(View.INVISIBLE);
        } else {
            CheckBox checkbox = ( CheckBox ) v.findViewById(R.id.checkBox);
            checkbox.setVisibility(View.VISIBLE);
            checkbox.setFocusableInTouchMode(false);
            checkbox.setFocusable(false);
            checkbox.setChecked(soundTypes.get(position).getInUse());
            checkbox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener()
            {
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked)
                {
                    if ( isChecked )
                    {
                        SQLiteDatabase writeDb = db.getWritableDatabase();
                        // Define 'where' part of query.
                        String selection = RecordingContract.RecordingEntry.COLUMN_NAME_SOUND_TYPE_NAME + " = ?";
                        // Specify arguments in placeholder order.
                        String[] selectionArgs = {s};
                        soundType.setInUse(true);
                        ContentValues cv = new ContentValues();
                        cv.put(RecordingContract.RecordingEntry.COLUMN_NAME_SOUND_TYPE_IN_USE, 1);
                        writeDb.update(RecordingContract.RecordingEntry.SOUND_TABLE_NAME, cv, selection, selectionArgs);
                    } else {
                        SQLiteDatabase writeDb = db.getWritableDatabase();
                        // Define 'where' part of query.
                        String selection = RecordingContract.RecordingEntry.COLUMN_NAME_SOUND_TYPE_NAME + " = ?";
                        // Specify arguments in placeholder order.
                        String[] selectionArgs = {s};
                        soundType.setInUse(false);
                        ContentValues cv = new ContentValues();
                        cv.put(RecordingContract.RecordingEntry.COLUMN_NAME_SOUND_TYPE_IN_USE, 0);
                        writeDb.update(RecordingContract.RecordingEntry.SOUND_TABLE_NAME, cv, selection, selectionArgs);
                    }

                }
            });
        }
        if(defaultSounds.contains(s)) {
            delete.setVisibility(View.INVISIBLE);
        } else {
            delete.setVisibility(View.VISIBLE);
        }
        delete.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                DeleteSoundTypeDialog deleteDialog = new DeleteSoundTypeDialog();
                Bundle b = new Bundle();
                b.putInt("pos", pos);
                deleteDialog.setArguments(b);
                deleteDialog.show(fragmentManager, "delete");
            }
        });

        return v;
    }
}
