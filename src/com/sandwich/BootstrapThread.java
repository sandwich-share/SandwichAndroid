package com.sandwich;

import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.URI;

import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.SearchView;

import com.sandwich.client.Client;

public class BootstrapThread implements Runnable {
	private Search activity;
	private Client client;
	
	public BootstrapThread(Search activity)
	{
		this.activity = activity;
		this.client = null;
	}
	
	public void initialize()
	{
    	SearchListener listener;
    	
    	if (client != null)
    		throw new IllegalStateException("Bootstrap thread was already initialized");
    	
    	// Create the client
    	client = new Client(activity);
		
        // Create our search listener
        listener = new SearchListener(activity, client);
		
        // Add SearchListener to our file search view
        SearchView search = (SearchView)activity.findViewById(R.id.fileSearchView);
        search.setOnQueryTextListener(listener);
        
        // Add our array adapter to the list view
        ListView results = (ListView)activity.findViewById(R.id.resultsListView);
        results.setAdapter(new ArrayAdapter<String>(activity, R.layout.simplerow));
        results.setOnItemClickListener(listener);
	}
	
	@Override
	public void run() {
		if (client == null)
			throw new IllegalStateException("Bootstrap thread was not initialized");
		
		SpinnerDialog d = SpinnerDialog.displayDialog(activity, "Please Wait", "Waiting for bootstrap");
		try {
			String host = "isys-ubuntu.case.edu";
			InetAddress hostaddr = Inet6Address.getByName(host);

			client.bootstrap(new URI("http", null, hostaddr.getHostAddress(), Client.getPortNumberFromIPAddress(hostaddr), null, null, null).toURL());
			d.dismiss();
		} catch (Exception e) {
			d.dismiss();
			Dialog.displayDialog(activity, "Bootstrap Error", e.getMessage(), true);
			e.printStackTrace();
		}
	}
}
