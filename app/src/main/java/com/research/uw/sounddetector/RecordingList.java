package com.research.uw.sounddetector;

import android.content.Intent;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.HashMap;


public class RecordingList extends ActionBarActivity {
    private ListView listView;
    private ArrayList<Recording> currRecordings;
    private HashMap<String, ArrayList<Recording>> recordingMap;
    private RecordingListAdapter recordingListAdapter
    private String soundName;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_recording_list);

        Intent i = getIntent();
        soundName = i.getStringExtra("sound name");
        TextView soundNameTextView = (TextView)findViewById(R.id.soundName);
        soundNameTextView.setText(soundName + "Recordings");

        currRecordings = recordingMap.get(soundName);
        if(currRecordings == null) {
            currRecordings = new ArrayList<Recording>();
        }

        Spinner spinner = (Spinner) findViewById(R.id.sortbyspinner);
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this, R.array.sort_by_array, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);

        listView = (ListView) findViewById(R.id.recordingList);
        recordingListAdapter = new RecordingListAdapter(this, android.R.layout.simple_list_item_1, currRecordings, getSupportFragmentManager());
        listView.setAdapter(recordingListAdapter);

    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_recording_list, menu);
        return true;
    }

    public void addNewRecording() {

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
}
