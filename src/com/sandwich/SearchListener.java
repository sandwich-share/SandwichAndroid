package com.sandwich;

import java.io.IOException;

import com.sandwich.client.Client;
import com.sandwich.client.ResultListener;

import android.app.Activity;
import android.content.Context;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.SearchView;
import android.widget.SearchView.OnQueryTextListener;
import android.widget.TextView;

public class SearchListener implements OnQueryTextListener,ResultListener,OnItemClickListener  {
	Client sandwichClient;
	Activity activity;
	
	SearchView searchView;
	ListView resultsView;
	
	public SearchListener(Activity activity, Client client)
	{
		this.sandwichClient = client;
		this.activity = activity;

		// Fetch these here so we don't have to do it later
		this.searchView = (SearchView)activity.findViewById(R.id.fileSearchView);
		this.resultsView = (ListView)activity.findViewById(R.id.resultsListView);
	}
	
	@SuppressWarnings("unchecked")
	@Override
	// Called when the search button is clicked
	public boolean onQueryTextSubmit(String query)
	{
		ArrayAdapter<String> listAdapter;
		
		// Clear the results list
		listAdapter = (ArrayAdapter<String>)resultsView.getAdapter();
		listAdapter.clear();
		
		// Collapse the keyboard
		InputMethodManager inputManager = (InputMethodManager)
                activity.getSystemService(Context.INPUT_METHOD_SERVICE);
		inputManager.hideSoftInputFromWindow(searchView.getWindowToken(),
                   InputMethodManager.HIDE_NOT_ALWAYS);

		// Remove focus from the search view
		searchView.clearFocus();
		
		// Execute the search with the client
		try {
			sandwichClient.doSearch(searchView.getQuery().toString(), this);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		// We handled the event so return true
		return true;
	}

	@Override
	// Called when the query text changes in the search box
	public boolean onQueryTextChange(String arg0) {
		// Don't care about this event
		return false;
	}

	@SuppressWarnings("unchecked")
	@Override
	// Called for each result found during the search
	public synchronized void foundResult(String query, String result) {
		ArrayAdapter<String> listAdapter = (ArrayAdapter<String>)resultsView.getAdapter();
		
		listAdapter.add(result);
	}

	@Override
	// Called when an item within the list view is clicked
	public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
		TextView row = (TextView) view;
		
		sandwichClient.startFileDownloadFromPeer("anandtech.com", "");
	}
}
