package com.sandwich;

import android.os.Bundle;
import android.view.Window;
import android.app.Activity;
import android.app.SearchManager;
import android.content.Intent;

public class Search extends Activity {
	BootstrapThread bootstrapper;
	Thread thread;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		// No title
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		
		setContentView(R.layout.activity_search);

    	// Create the bootstrapper thread
        bootstrapper = new BootstrapThread(this);
        thread = null;
        
        // Call UI initialization code from the UI thread
        bootstrapper.initialize();
	}
	
    @Override
    public void onStart()
    {
    	super.onStart();
        
        // Start the bootstrapping process if it's not already running
    	if (thread == null || thread.getState() == Thread.State.TERMINATED)
    	{
    		thread = new Thread(bootstrapper);
    		thread.start();
    	}
    	
    	this.onSearchRequested();
    }
    
    @Override
    public void onBackPressed()
    {
    	this.onSearchRequested();
    }
    
    @Override
    protected void onDestroy()
    {
    	super.onDestroy();
    	
    	Dialog.closeDialogs();
    	SpinnerDialog.closeDialogs();
    }
	
	@Override
	protected void onNewIntent(Intent intent)
	{
		// Verify the action and get the query
	    if (Intent.ACTION_SEARCH.equals(intent.getAction())) {
	      String query = intent.getStringExtra(SearchManager.QUERY);
	      System.out.println("Searching: "+query);
	      bootstrapper.doSearch(query);
	    }
	}
}
