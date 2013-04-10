package com.sandwich;

import java.util.ArrayList;

import com.sandwich.client.Client;
import com.sandwich.client.ResultListener;

import android.app.Activity;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;
import android.widget.ProgressBar;

public class SearchListener implements ResultListener,OnItemClickListener,Runnable  {
	private Client sandwichClient;
	private Activity activity;
	
	private ListView resultsView;
	private ProgressBar updateBar;
	
	private ArrayList<ResultListener.Result> results;
	private int searchesFinished;
		
	public SearchListener(Activity activity, Client client)
	{
		this.sandwichClient = client;
		this.activity = activity;

		// Fetch these here so we don't have to do it later
		this.resultsView = (ListView)activity.findViewById(R.id.resultsListView);
		this.updateBar = (ProgressBar)activity.findViewById(R.id.updateBar);
		
		// Initialize the results list
		results = new ArrayList<ResultListener.Result>();
	}
	
	// Called when the search button is clicked
	public boolean onQueryTextSubmit(String query)
	{
		ResultAdapter listAdapter;
		
		// End any pending search to prevent modification of these result lists
		// after we've cleared them, but before beginSearch() kills them.
		sandwichClient.endSearch();
		
		// Clear the results list
		listAdapter = (ResultAdapter)resultsView.getAdapter();
		listAdapter.clear();
		
		// Initialize the results list
		results.clear();
		searchesFinished = 0;
		
		// Start progress bar at 0
		updateBar.setProgress(0);
		
		// Execute the asynchronous search with the client
		try {
			updateBar.setMax(sandwichClient.beginSearch(query, this));
		} catch (Exception e) {
			e.printStackTrace();
			Dialog.displayDialog(activity, "Search Error", e.getMessage(), false);
		}
		
		// We handled the event so return true
		return true;
	}

	@Override
	// Called for each result found during the search
	public void foundResult(String query, ResultListener.Result result) {
		synchronized (results) {
			results.add(result);
		}
		
		activity.runOnUiThread(this);
	}
	
	@Override
	// Called in UI thread to add search result
	public void run() {
		ResultAdapter listAdapter = (ResultAdapter)resultsView.getAdapter();
		synchronized (results) {
			for (ResultListener.Result result : results) {
				listAdapter.add(result);
			}
			
			// Remove the results we just added
			results.clear();
			
			// Update the progressbar
			if (updateBar.getProgress() != searchesFinished)
			{
				updateBar.setProgress(searchesFinished);
			
				// If nothing was found, just add an entry to say nothing was found
				if ((listAdapter.getCount() == 0) && (searchesFinished == updateBar.getMax()))
					listAdapter.add(null);
			}
		}
	}
	
	@Override
	public void searchFailed(String query, String peer, Exception e) {
		Dialog.displayDialog(activity, "Search Error", e.getMessage(), false);
	}

	@Override
	public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {
		activity.openContextMenu(view);
	}

	@Override
	public void searchComplete(String query, String peer) {
		synchronized (results) {
			searchesFinished++;
		}
		
		activity.runOnUiThread(this);
	}
}
