package com.sandwich;

import com.sandwich.R;

import android.os.Bundle;
import android.app.Activity;

public class Search extends Activity {
	BootstrapThread bootstrapper;
	Thread thread;
	
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
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
    }

    @Override
    protected void onDestroy()
    {
    	super.onDestroy();
    	
    	Dialog.closeDialogs();
    	SpinnerDialog.closeDialogs();
    }
}
