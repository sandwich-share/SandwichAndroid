package com.sandwich;

import com.sandwich.client.ResultListener;
import com.sandwich.client.ResultListener.Result;

import android.os.Bundle;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.KeyEvent;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.ListView;
import android.app.Activity;
import android.content.Context;

public class Search extends Activity implements TextView.OnEditorActionListener {
	private ClientThread client;
	private Thread thread;
	private ListView list;
	private EditText searchBox;

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
		
		// Setup search box
		searchBox = (EditText) findViewById(R.id.searchBar);
		searchBox.setOnEditorActionListener(this);

    	// Create the bootstrapper thread
        client = new ClientThread(this);
        thread = null;
        
        // Call UI initialization code from the UI thread
        client.initialize();
        
        // Bootstrap it
    	thread = new Thread(client);
    	thread.start();
    	
    	// Begin search
    	onSearchRequested();
	}
	
	@Override
	protected void onPause() {
		// End search before entering background
		client.endSearch();
		super.onPause();
	}
	
	@Override
	public boolean onSearchRequested() {
		// Give focus to the search box
		searchBox.requestFocus();
		
		// Display the keyboard
		InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
		imm.showSoftInput(searchBox, InputMethodManager.SHOW_FORCED);
		
		// We don't want the dialog to actually come up
		return false;
	}
    
    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        
        AdapterContextMenuInfo info = (AdapterContextMenuInfo) menuInfo;
        ResultListener.Result res = (Result) list.getAdapter().getItem(info.position);
        if (res == null)
        	return;
        
        // Inflate the context menu
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.context_menu, menu);
        
        // Remove the stream option if it's not streamable
        if (!client.isResultStreamable(res)) {
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
        case R.id.copy:
        	try {
        		client.copyUrl(result);
        	} catch (Exception e) {
        		Dialog.displayDialog(this, "URL Error", e.getMessage(), false);
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
	public boolean onEditorAction(TextView view, int actionId, KeyEvent event) {
		
		if (event != null) {
			String query = view.getText().toString();
			
	        // Start the bootstrapping process if it's not already running
	    	if (thread == null || thread.getState() == Thread.State.TERMINATED)
	    	{
	    		System.out.println("Starting new bootstrap thread");
	    		thread = new Thread(client);
	    		thread.start();
	    	}
			
			System.out.println("Searching: "+query);
			client.doSearch(query);
		}
		
		return false;
	}
}
