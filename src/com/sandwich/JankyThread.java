package com.sandwich;

import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.URI;

import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.SearchView;

import com.sandwich.client.Client;

public class JankyThread implements Runnable {
	private Search activity;
	
	public JankyThread(Search activity)
	{
		this.activity = activity;
	}
	
	@Override
	public void run() {
		Client c = new Client(activity);
    	SearchListener listener;
		
        // Create our search listener
        listener = new SearchListener(activity, c);
		
        // Add SearchListener to our file search view
        SearchView search = (SearchView)activity.findViewById(R.id.fileSearchView);
        search.setOnQueryTextListener(listener);
        
        // Add our array adapter to the list view
        ListView results = (ListView)activity.findViewById(R.id.resultsListView);
        results.setAdapter(new ArrayAdapter<String>(activity, R.layout.simplerow));
        results.setOnItemClickListener(listener);
        
		SpinnerDialog d = SpinnerDialog.displayDialog(activity, "Please Wait", "Waiting for bootstrap");
		try {
			String host = "isys-ubuntu.case.edu";
			InetAddress hostaddr = Inet6Address.getByName(host);

			c.bootstrap(new URI("http", null, hostaddr.getHostAddress(), Client.getPortNumberFromIPAddress(hostaddr), null, null, null).toURL());
			d.dismiss();
		} catch (Exception e) {
			d.dismiss();
			Dialog.displayDialog(activity, "Bootstrap Error", e.getMessage(), true);
			e.printStackTrace();
		}
	}

}
