package com.sandwich;

import com.sandwich.R;
import com.sandwich.client.Client;

import android.os.Bundle;
import android.app.Activity;
import android.view.Menu;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.SearchView;

public class Search extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
    	SearchListener listener;
    	
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search);
        
        // Create our search listener
        listener = new SearchListener(this, new Client(getApplicationContext()));

        // Add SearchListener to our file search view
        SearchView search = (SearchView)findViewById(R.id.fileSearchView);
        search.setOnQueryTextListener(listener);
        
        // Add our array adapter to the list view
        ListView results = (ListView)findViewById(R.id.resultsListView);
        results.setAdapter(new ArrayAdapter<String>(getApplicationContext(), R.layout.simplerow));
        results.setOnItemClickListener(listener);
        
        //new Thread(new JankyThread(getApplicationContext())).start();
    }

    

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.search, menu);
        return true;
    }
    
}
