package com.sandwich;

import com.sandwich.client.ResultListener;

import android.os.Bundle;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.ListView;
import android.app.Activity;
import android.app.SearchManager;
import android.content.Intent;

public class Search extends Activity {
	ClientThread client;
	Thread thread;
	ListView list;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		// No title
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		
		// Set the layout
		setContentView(R.layout.activity_search);
		
		// Register for context menu on search results
		list = (ListView) findViewById(R.id.resultsListView);
		registerForContextMenu(list);

    	// Create the bootstrapper thread
        client = new ClientThread(this);
        thread = null;
        
        // Call UI initialization code from the UI thread
        client.initialize();
	}
	
	@Override
	public boolean onSearchRequested()
	{
        // Start the bootstrapping process if it's not already running
    	if (thread == null || thread.getState() == Thread.State.TERMINATED)
    	{
    		thread = new Thread(client);
    		thread.start();
    	}
		
		return super.onSearchRequested();
	}
	
    @Override
    public void onStart()
    {
    	super.onStart();
    	this.onSearchRequested();
    }
    
    @Override
    public void onBackPressed()
    {
    	this.onSearchRequested();
    }
    
    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        
        // Inflate the context menu
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.context_menu, menu);
        
        // Remove the stream option if it's not streamable
        AdapterContextMenuInfo info = (AdapterContextMenuInfo) menuInfo;
        if (!client.isResultStreamable((ResultListener.Result)list.getAdapter().getItem(info.position)))
        {
        	menu.removeItem(R.id.stream);
        }
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        AdapterContextMenuInfo info = (AdapterContextMenuInfo) item.getMenuInfo();
        ResultListener.Result result = (ResultListener.Result)list.getAdapter().getItem(info.position);
        switch (item.getItemId())
        {
        case R.id.stream:
        	try {
        		client.stream(result);
        	} catch (Exception e) {
        		Dialog.displayDialog(this, "Streaming Error", e.getMessage(), false);
        	}
            return true;
        case R.id.download:
        	try {
        		client.download(result);
        	} catch (Exception e) {
        		Dialog.displayDialog(this, "Download Error", e.getMessage(), false);
        	}
        	return true;
        case R.id.share:
        	try {
        		client.share(result);
        	} catch (Exception e) {
        		Dialog.displayDialog(this, "Sharing Error", e.getMessage(), false);
        	}
        	return true;
        default:
          return super.onContextItemSelected(item);
        }
    }
    
    @Override
    protected void onDestroy()
    {    	
    	unregisterForContextMenu(list);
    	client.release();
    	Dialog.closeDialogs();
    	SpinnerDialog.closeDialogs();
    	
    	super.onDestroy();
    }
	
	@Override
	protected void onNewIntent(Intent intent)
	{
		// Verify the action and get the query
	    if (Intent.ACTION_SEARCH.equals(intent.getAction())) {
	      String query = intent.getStringExtra(SearchManager.QUERY);
	      System.out.println("Searching: "+query);
	      client.doSearch(query);
	    }
	}
}
