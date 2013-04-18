package com.sandwich;

import com.sandwich.ui.DetailsDialog;
import com.sandwich.ui.Dialog;
import com.sandwich.ui.SpinnerDialog;

import android.app.Activity;
import android.app.TabActivity;
import android.content.Intent;
import android.os.Bundle;
import android.view.Window;
import android.view.inputmethod.InputMethodManager;
import android.widget.TabHost;
import android.widget.TabHost.OnTabChangeListener;
import android.widget.TabHost.TabSpec;

@SuppressWarnings("deprecation")
public class TabView extends TabActivity implements OnTabChangeListener {
	private ClientUiBinding client;
	private BackgroundUpdater updater;
	private String lastTab;
	
	private static final String SEARCH = "Search";
	private static final String PEER_LIST = "Peer List";
	private static final String SETTINGS = "Settings";
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		// Setup the global client object and register it with each activity
		client = new ClientUiBinding(getApplicationContext());
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
		TabSpec searchTab = tabHost.newTabSpec(SEARCH);
		searchTab.setIndicator(SEARCH);
		Intent searchIntent = new Intent(this, Search.class);
		searchTab.setContent(searchIntent);
		
		// Add peer list tab
		TabSpec peerListTab = tabHost.newTabSpec(PEER_LIST);
		peerListTab.setIndicator(PEER_LIST);
		Intent peerListIntent = new Intent(this, PeerList.class);
		peerListTab.setContent(peerListIntent);
		
		// Add settings tab
		TabSpec settingsTab = tabHost.newTabSpec(SETTINGS);
		settingsTab.setIndicator(SETTINGS);
		Intent settingsIntent = new Intent(this, Settings.class);
		settingsTab.setContent(settingsIntent);
		
		// Add tabs to host
		tabHost.addTab(searchTab);
		tabHost.addTab(peerListTab);
		tabHost.addTab(settingsTab);
		
		// Listen for tab changes
		tabHost.setOnTabChangedListener(this);
		
		// Create the background updater
		updater = new BackgroundUpdater(client, Settings.getRefreshInterval(this));
	}
	
	@Override
	protected void onResume() {
		super.onResume();
		
		// Start the background updater again
		updater.start();
	}
	
	@Override
	protected void onPause() {
		// Halt the background updater
		updater.stop();
		
		// End bootstrap before entering background
		client.endBootstrap();
		
		// End search before entering background
		client.endSearch();
		
		super.onPause();
	}
	
	@Override
	public void onDestroy() {
    	client.release();
    	
    	Dialog.closeDialogs();
    	SpinnerDialog.closeDialogs();
    	DetailsDialog.dismissDialogs();
    	
    	super.onDestroy();
	}

	@Override
	public void onTabChanged(String tag) {
		// Bootstrap if we're leaving the settings tab
		if (lastTab != null && lastTab.equals(SETTINGS)) {
			client.bootstrap();
		}
		
		// Hide the keyboard when switching tabs
		InputMethodManager imm = (InputMethodManager) getSystemService(Activity.INPUT_METHOD_SERVICE);
		imm.toggleSoftInput(InputMethodManager.SHOW_IMPLICIT, 0);
		
		// Store the last tab
		lastTab = tag;
	}
}
