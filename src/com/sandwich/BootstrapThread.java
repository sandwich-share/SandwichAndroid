package com.sandwich;


import android.app.Activity;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import com.sandwich.client.Client;

public class BootstrapThread implements Runnable {
	private Activity activity;
	private Client client;
	private boolean bootstrapped;
	private SearchListener listener;
	
	public BootstrapThread(Activity activity)
	{
		this.activity = activity;
		this.client = null;
	}
	
	public void doSearch(String query)
	{
		// A bit of a hack
		listener.onQueryTextSubmit(query);
	}
	
	public void initialize()
	{    	
    	if (client != null)
    		throw new IllegalStateException("Bootstrap thread was already initialized");
    	
    	// Create the client
    	client = new Client(activity);
		
        // Create our search listener
        listener = new SearchListener(activity, client);
        
        // Add our array adapter to the list view
        ListView results = (ListView)activity.findViewById(R.id.resultsListView);
        results.setAdapter(new ArrayAdapter<String>(activity, R.layout.simplerow));
        results.setOnItemClickListener(listener);
        results.setOnItemLongClickListener(listener);
        
        // Bootstrap from the cache initially
        bootstrapped = client.bootstrapFromCache();
	}
	
	@Override
	public void run() {
		SpinnerDialog d;

		if (client == null)
			throw new IllegalStateException("Bootstrap thread was not initialized");
		
		// If we're not running on cache, we want to display the spinner and block the user, otherwise we just bootstrap in the background
		if (!bootstrapped)
		{
			d = SpinnerDialog.displayDialog(activity, "Please Wait", "Waiting for initial bootstrap", false);
		}
		else
		{
			d = null;
		}

		try {
			String initialHost = "isys-ubuntu.case.edu";

			// Bootstrap from network
			client.bootstrapFromNetwork(initialHost);
			
			// Bootstrapped
			bootstrapped = true;
			
			if (d != null) d.dismiss();
		} catch (Exception e) {
			if (d != null) d.dismiss();
			Dialog.displayDialog(activity, "Bootstrap Error", e.getMessage(), true);
			e.printStackTrace();
		}
	}
}
