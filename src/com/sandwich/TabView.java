package com.sandwich;

import com.sandwich.ui.DetailsDialog;
import com.sandwich.ui.Dialog;
import com.sandwich.ui.SpinnerDialog;

import android.app.TabActivity;
import android.content.Intent;
import android.os.Bundle;
import android.view.Window;
import android.widget.TabHost;
import android.widget.TabHost.TabSpec;

@SuppressWarnings("deprecation")
public class TabView extends TabActivity {
	private ClientThread client;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		// Setup the global client object and register it with each activity
		client = new ClientThread(this, getApplicationContext());
        client.initialize();
		Search.addClient(client);
		PeerList.addClient(client);
		PeerFiles.addClient(client);
		
		super.onCreate(savedInstanceState);
		
		// No title
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		
		// Set the layout
		setContentView(R.layout.activity_tabs);
		
		TabHost tabHost = getTabHost();
		
		// Add search tab
		TabSpec searchTab = tabHost.newTabSpec("Search");
		searchTab.setIndicator("Search");
		Intent searchIntent = new Intent(this, Search.class);
		searchTab.setContent(searchIntent);
		
		// Add peer list tab
		TabSpec peerListTab = tabHost.newTabSpec("Peer List");
		peerListTab.setIndicator("Peer List");
		Intent peerListIntent = new Intent(this, PeerList.class);
		peerListTab.setContent(peerListIntent);
		
		// Add settings tab
		TabSpec settingsTab = tabHost.newTabSpec("Settings");
		settingsTab.setIndicator("Settings");
		Intent settingsIntent = new Intent(this, Settings.class);
		settingsTab.setContent(settingsIntent);
		
		// Add tabs to host
		tabHost.addTab(searchTab);
		tabHost.addTab(peerListTab);
		tabHost.addTab(settingsTab);
	}
	
	@Override
	public void onDestroy() {
    	client.release();
    	
    	Dialog.closeDialogs();
    	SpinnerDialog.closeDialogs();
    	DetailsDialog.dismissDialogs();
    	
    	super.onDestroy();
	}
}
