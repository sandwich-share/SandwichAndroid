package com.sandwich;


import java.io.IOException;
import java.net.URISyntaxException;
import java.net.UnknownHostException;
import java.security.NoSuchAlgorithmException;
import java.util.Iterator;
import java.util.Set;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.text.ClipboardManager;

import com.sandwich.client.Client;
import com.sandwich.client.PeerSet.Peer;
import com.sandwich.client.ResultListener;
import com.sandwich.player.MediaMimeInfo;

@SuppressWarnings("deprecation") // Needed to avoid stupid warnings for older ClipboardManager class
public class ClientThread implements Runnable {
	private Context appContext;
	private Search searchActivity;
	private Client client;
	private boolean loadedCache;
	private Thread thread;
	public Blacklist blacklist;
	
	public ClientThread(Context appContext)
	{
		this.appContext = appContext;
		this.blacklist = new Blacklist(appContext);
	}
	
	public void bootstrap()
	{
        // Start the bootstrapping process if it's not already running
    	if (thread == null || thread.getState() == Thread.State.TERMINATED)
    	{
    		System.out.println("Starting new bootstrap thread");
    		thread = new Thread(this);
    		thread.start();
    	}
	}
	
	public void registerSearchActivity(Search searchActivity)
	{        
		this.searchActivity = searchActivity;
	}
	
	public SearchListener getSearchListener(Activity activity)
	{
		return new SearchListener(activity, client);
	}
	
	public void doSearch(String query)
	{
		// A bit of a hack
		if (searchActivity != null)
			searchActivity.listener.onQueryTextSubmit(query);
	}
	
	public boolean getPeerIndex(String peer, ResultListener listener)
	{
		return client.getPeerIndex(peer, listener);
	}
	
	public static boolean isResultStreamable(ResultListener.Result result)
	{
		return Client.isResultStreamable(result);
	}
	
	public Set<Peer> getPeerSet()
	{
		return client.getPeerSet();
	}
	
	public void copyUrl(ResultListener.Result result) throws UnknownHostException, NoSuchAlgorithmException, URISyntaxException
	{
		String url = null;
		Iterator<Peer> peers = result.getPeerIterator();
		
		// This class was deprecated in API level 11, but we need compatibility for API level 9
		ClipboardManager clipboard = (ClipboardManager) appContext.getSystemService(Context.CLIPBOARD_SERVICE);
		
		while (peers.hasNext())
		{
			try {
				 url = Client.getUriForResult(peers.next(), result);
				 break;
			} catch (NoSuchAlgorithmException e) {
				if (!peers.hasNext())
					throw e;
			} catch (URISyntaxException e) {
				if (!peers.hasNext())
					throw e;
			} catch (UnknownHostException e) {
				if (!peers.hasNext())
					throw e;
			}
		}
		
		// Write to the clipboard
		clipboard.setText(url);
	}
	
	public void download(ResultListener.Result result) throws NoSuchAlgorithmException, URISyntaxException, IOException
	{
		Iterator<Peer> peers = result.getPeerIterator();
		
		while (peers.hasNext())
		{
			try {
				client.startFileDownloadFromPeer(peers.next(), result.result);
				break;
			} catch (NoSuchAlgorithmException e) {
				if (!peers.hasNext())
					throw e;
			} catch (URISyntaxException e) {
				if (!peers.hasNext())
					throw e;
			} catch (IOException e) {
				if (!peers.hasNext())
					throw e;
			}
		}
	}
	
	public void stream(ResultListener.Result result) throws NoSuchAlgorithmException, URISyntaxException, IOException
	{
		Iterator<Peer> peers = result.getPeerIterator();
		
		while (peers.hasNext())
		{
			try {
				client.startFileStreamFromPeer(appContext, peers.next(), result.result);
				break;
			} catch (NoSuchAlgorithmException e) {
				if (!peers.hasNext())
					throw e;
			} catch (URISyntaxException e) {
				if (!peers.hasNext())
					throw e;
			} catch (IOException e) {
				if (!peers.hasNext())
					throw e;
			}
		}
	}
	
	public void share(Activity activity, ResultListener.Result result) throws UnknownHostException, NoSuchAlgorithmException, URISyntaxException
	{
		Intent shareIntent = new Intent();
		String url = null;
		Iterator<Peer> peers = result.getPeerIterator();
		
		while (peers.hasNext())
		{
			try {
				 url = Client.getUriForResult(peers.next(), result);
				 break;
			} catch (NoSuchAlgorithmException e) {
				if (!peers.hasNext())
					throw e;
			} catch (URISyntaxException e) {
				if (!peers.hasNext())
					throw e;
			} catch (UnknownHostException e) {
				if (!peers.hasNext())
					throw e;
			}
		}
		
		shareIntent.setAction(Intent.ACTION_SEND);
		shareIntent.putExtra(Intent.EXTRA_TEXT, url);
		shareIntent.setType("text/plain");

		activity.startActivity(Intent.createChooser(shareIntent, "Share to..."));
	}
	
	public void endSearch() {
		client.endSearch();
	}
	
	public void endBootstrap() {
		client.endBootstrap();
	}
	
	public void initialize()
	{
    	if (client != null)
    		throw new IllegalStateException("Bootstrap thread was already initialized");
    	
    	// Create the client
    	client = new Client(appContext);
    	client.initialize();
    	
    	// Initialize the blacklist
    	blacklist.initialize();
    	
    	// Register our package manager with the MIME class
    	MediaMimeInfo.registerPackageManager(appContext.getPackageManager());
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

        if (!loadedCache) {
        	// Bootstrap from the cache initially
        	client.bootstrapFromCache();
        	loadedCache = true;
        }
		
		try {
			client.bootstrapFromNetwork(Settings.getBootstrapNode(appContext),
					null, blacklist.getBlacklistSet());
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
