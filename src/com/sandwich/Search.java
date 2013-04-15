package com.sandwich;

import com.sandwich.client.Client;
import com.sandwich.client.ResultListener;
import com.sandwich.client.ResultListener.Result;
import com.sandwich.ui.DetailsDialog;
import com.sandwich.ui.Dialog;

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
	private static ClientThread client;
	private ListView list;
	private EditText searchBox;
	SearchListener listener;

	public static void addClient(ClientThread t) {
		client = t;
	}
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		// No title
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		
		// Set the layout
		setContentView(R.layout.activity_search);
    	
    	// Get the search listener
    	listener = client.getSearchListener(this);
    	
		// Register for context menu on search results
		list = (ListView) findViewById(R.id.resultsListView);
        list.setAdapter(new ResultAdapter(this, R.layout.simplerow));
        list.setOnItemClickListener(listener);
		registerForContextMenu(list);
		
		// Setup search box
		searchBox = (EditText) findViewById(R.id.searchBar);
		searchBox.setOnEditorActionListener(this);
		
		// Register ourselves with the client thread
		client.registerSearchActivity(this);

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
		// Bootstrap
		client.bootstrap();
		
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
        if (!Client.isResultStreamable(res)) {
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
        case R.id.details:
        	try {
        		new DetailsDialog(this, result).createDetailsDialog();
        	} catch (Exception e) {
        		Dialog.displayDialog(this, "Error", e.getMessage(), false);
        	}
        	return true;
        case R.id.share:
        	try {
        		client.share(this, result);
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
    	client.registerSearchActivity(null);
    	unregisterForContextMenu(list);
    	
    	super.onDestroy();
    }

	@Override
	public boolean onEditorAction(TextView view, int actionId, KeyEvent event) {
		
		if (event != null) {
			String query = view.getText().toString();
			client.bootstrap();
			System.out.println("Searching: "+query);
			client.doSearch(query);
		}
		
		return false;
	}
}
