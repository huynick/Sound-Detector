package com.research.uw.sounddetector;

import android.content.Intent;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import java.util.ArrayList;
import java.util.Collections;


public class SoundType extends ActionBarActivity implements AddNewSoundTypeDialog.NoticeDialogListener {
    private ArrayList<String> soundTypeList;
    private ArrayAdapter<String> soundTypeAdapter;
    private ListView listView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sound_type);

        soundTypeList = new ArrayList<String>();
        soundTypeAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, soundTypeList);

        listView = (ListView) findViewById(R.id.soundTypeList);
        listView.setAdapter(soundTypeAdapter);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Intent i = new Intent(getApplicationContext(), RecordingList.class);
                i.putExtra("sound name", soundTypeAdapter.getItem(position));
                startActivity(i);
            }
        });
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_sound_type, menu);
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

    //prompt the user to add a new sound type
    public void addNew(View view) {
        AddNewSoundTypeDialog dialog = new AddNewSoundTypeDialog();
        dialog.show(getSupportFragmentManager(), "HelpDialog");
    }

    @Override
    //Add the inputted sound type name to the list
    public void onDialogPositiveClick(DialogFragment dialog, String name) {
        soundTypeAdapter.add(name);
        Collections.sort(soundTypeList);
    }

    @Override
    //Do nothing
    public void onDialogNegativeClick(DialogFragment dialog) {

    }
}
