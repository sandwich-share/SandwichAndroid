package com.sandwich;

import java.util.ArrayList;

import com.sandwich.client.Client;
import com.sandwich.client.ResultListener;

import android.app.Activity;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

public class SearchListener implements ResultListener,OnItemClickListener,Runnable,OnItemLongClickListener  {
	private Client sandwichClient;
	private Activity activity;
	
	private ListView resultsView;
	
	private ArrayList<String> results;
		
	public SearchListener(Activity activity, Client client)
	{
		this.sandwichClient = client;
		this.activity = activity;

		// Fetch these here so we don't have to do it later
		this.resultsView = (ListView)activity.findViewById(R.id.resultsListView);
		
		// Initialize the results list
		results = new ArrayList<String>();
	}
	
	@SuppressWarnings("unchecked")
	// Called when the search button is clicked
	public boolean onQueryTextSubmit(String query)
	{
		ArrayAdapter<String> listAdapter;
		
		// Clear the results list
		listAdapter = (ArrayAdapter<String>)resultsView.getAdapter();
		listAdapter.clear();
		
		// Initialize the results list
		results.clear();
		
		// Execute the asynchronous search with the client
		try {
			sandwichClient.beginSearch(query, this);
		} catch (Exception e) {
			Dialog.displayDialog(activity, "Search Error", e.getMessage(), false);
		}
		
		// We handled the event so return true
		return true;
	}

	@Override
	// Called for each result found during the search
	public void foundResult(String query, String peer, String result) {
		synchronized (results) {
			results.add(peer+" - "+result);
		}
		
		activity.runOnUiThread(this);
	}

	@Override
	// Called when an item within the list view is clicked
	public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
		TextView row = (TextView) view;
		String resultTuple[] = row.getText().toString().split(" - ");
		
		// Result tuple is in the format: peer - file
		try {
			sandwichClient.startFileDownloadFromPeer(resultTuple[0], resultTuple[1]);
		} catch (Exception e) {
			Dialog.displayDialog(activity, "Download Error", e.getMessage(), false);
			return true;
		}
		
		// Already handled
		return true;
	}

	
	@Override
	// Called in UI thread to add search result
	public void run() {
		@SuppressWarnings("unchecked")
		ArrayAdapter<String> listAdapter = (ArrayAdapter<String>)resultsView.getAdapter();
		
		synchronized (results) {
			for (String result : results)
			{
				listAdapter.add(result);
			}
			
			// Remove the results we just added
			results.clear();
		}
	}

	
	@Override
	public void searchFailed(String query, String peer, Exception e) {
		Dialog.displayDialog(activity, "Search Error", e.getMessage(), false);
	}

	@Override
	public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {
		TextView row = (TextView) view;
		String resultTuple[] = row.getText().toString().split(" - ");
		
		// Result tuple is in the format: peer - file
		try {
			sandwichClient.startFileStreamFromPeer(activity, resultTuple[0], resultTuple[1]);
		} catch (Exception e) {
			e.printStackTrace();
			Dialog.displayDialog(activity, "Streaming Error", e.getMessage(), false);
		}
	}
}
