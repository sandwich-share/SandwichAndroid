package com.sandwich;


import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.UnknownHostException;
import java.security.NoSuchAlgorithmException;

import android.app.Activity;
import android.content.Intent;
import android.widget.ListView;

import com.sandwich.client.Client;
import com.sandwich.client.ResultListener;

public class ClientThread implements Runnable {
	private Activity activity;
	private Client client;
	private SearchListener listener;
	
	public ClientThread(Activity activity)
	{
		this.activity = activity;
		this.client = null;
	}
	
	public void doSearch(String query)
	{
		// A bit of a hack
		listener.onQueryTextSubmit(query);
	}
	
	public boolean isResultStreamable(ResultListener.Result result)
	{
		return client.isResultStreamable(result);
	}
	
	public void download(ResultListener.Result result) throws NoSuchAlgorithmException, MalformedURLException, URISyntaxException, IOException
	{
		client.startFileDownloadFromPeer(result.peer, result.result);
	}
	
	public void stream(ResultListener.Result result) throws NoSuchAlgorithmException, MalformedURLException, URISyntaxException, IOException
	{
		client.startFileStreamFromPeer(activity, result.peer, result.result);
	}
	
	public void share(ResultListener.Result result) throws UnknownHostException, NoSuchAlgorithmException, URISyntaxException
	{
		Intent shareIntent = new Intent();
		String url = client.getUriForResult(result);
		
		shareIntent.setAction(Intent.ACTION_SEND);
		shareIntent.putExtra(Intent.EXTRA_TEXT, url);
		shareIntent.setType("text/plain");

		activity.startActivity(Intent.createChooser(shareIntent, "Share to..."));
	}
	
	public void initialize()
	{    	
    	if (client != null)
    		throw new IllegalStateException("Bootstrap thread was already initialized");
    	
    	// Create the client
    	client = new Client(activity);
    	client.initialize();
		
        // Create our search listener
        listener = new SearchListener(activity, client);
        
        // Add our array adapter to the list view
        ListView results = (ListView)activity.findViewById(R.id.resultsListView);
        results.setAdapter(new ResultAdapter<ResultListener.Result>(activity, R.layout.simplerow));
        results.setOnItemClickListener(listener);
        
        // Bootstrap from the cache initially
        client.bootstrapFromCache();
	}
	
	public void release()
	{
		if (client != null)
		{
			client.release();
		}
	}
	
	@Override
	public void run() {
		if (client == null)
			throw new IllegalStateException("Bootstrap thread was not initialized");

		try {
			String initialHost = "isys-ubuntu.case.edu";

			// Bootstrap from network
			client.bootstrapFromNetwork(initialHost);
		} catch (Exception e) {
			Dialog.displayDialog(activity, "Bootstrap Error", e.getMessage(), true);
			e.printStackTrace();
		}
	}
}
