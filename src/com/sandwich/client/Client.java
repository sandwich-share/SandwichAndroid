package com.sandwich.client;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.ConnectException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Scanner;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.DownloadManager;
import android.content.Context;
import android.net.Uri;
import android.os.Environment;

public class Client {
	private Context context;
	private PeerSet peers;
	
	public Client(Context context)
	{
		this.context = context;
		this.peers = null;
	}
	
	private static HttpURLConnection createHttpConnection(URL url) throws IOException
	{
		HttpURLConnection conn;
		
		System.out.println("Connecting to "+url.toExternalForm());
		conn = (HttpURLConnection) url.openConnection();
		
		// Disable cache
		conn.setUseCaches(false);
		
		return conn;
	}
	
	private static int sendGetRequest(HttpURLConnection conn) throws IOException
	{		
		// Send the get request
		conn.setRequestMethod("GET");

		// Wait for the response
		return conn.getResponseCode();
	}
	
	static URL createQueryUrl(URL peerUrl, String querySuffix) throws MalformedURLException
	{
		String urlString = peerUrl.toExternalForm();
		
		// If it doesn't end with a slash, normalize it by adding a slash
		if (!urlString.endsWith("/"))
			urlString += "/";
		
		// Now append the query suffix
		urlString += querySuffix;
		
		return new URL(urlString);
	}
	
	static BufferedInputStream getInputStreamFromUrl(URL url) throws IOException
	{
		HttpURLConnection conn;
		int responseCode;

		// Create the HTTP connection to the peer list
		conn = createHttpConnection(url);

		// Send the GET request and get the response code
		responseCode = sendGetRequest(conn);
		if (responseCode != HttpURLConnection.HTTP_OK)
		{
			throw new ConnectException("Failed to get peer list from bootstrap peer");
		}

		return new BufferedInputStream(conn.getInputStream());
	}
	
	private PeerSet getPeerList(URL bootstrapUrl) throws IOException, JSONException
	{
		URL queryUrl;
		Scanner in;
		PeerSet peerSet;
		JSONArray jsonPeerList;
		String json;
		
		// Build the query peer list URL
		queryUrl = createQueryUrl(bootstrapUrl, "peerlist");
		
		// Get an input stream from the GET request on this URL
		in = new Scanner(getInputStreamFromUrl(queryUrl));
		
		// Read the JSON response
		json = "";
		while (in.hasNext())
		{
			json += in.next();
		}
		
		System.out.println("Peerlist response: "+json);
		
		jsonPeerList = new JSONArray(json);
		peerSet = new PeerSet();
		for (int i = 0; i < jsonPeerList.length(); i++)
		{
			JSONObject jsonPeer = jsonPeerList.getJSONObject(i);
			
			System.out.println("Parsing JSON object: "+jsonPeer.toString());
			
			// We only care about the IP field on Android
			peerSet.addPeerByString(jsonPeer.getString("IP"));
		}
		
		in.close();
		
		return peerSet;
	}
	
	private Thread startDownloadIndexThreadForPeer(URL bootstrapUrl, String peer)
	{
		Thread t = new Thread(new IndexDownloadThread(context, bootstrapUrl, peer));
		t.start();
		return t;
	}
	
	public void bootstrap(URL bootstrapUrl) throws IOException, JSONException, InterruptedException
	{
		Iterator<String> iterator;
		ArrayList<Thread> threadList;

		if (peers != null)
		{
			throw new IllegalStateException("Already bootstrapped");
		}

		System.out.println("Downloading the peer list");
		
		// Download the peer list
		peers = getPeerList(bootstrapUrl);
		
		// Iterate the peer list and download indexes for each
		iterator = peers.getPeerListIterator();
		threadList = new ArrayList<Thread>();
		while (iterator.hasNext())
		{
			String peer = iterator.next();
			
			threadList.add(startDownloadIndexThreadForPeer(bootstrapUrl, peer));
		}
		
		// Wait for downloads to finish
		for (Thread t : threadList)
			t.join();
		
		System.out.println("Bootstrapped");
	}
	
	public void doSearch(String query, ResultListener listener) throws IOException, InterruptedException
	{
		ArrayList<Thread> threads = new ArrayList<Thread>();
		
		if (peers == null)
		{
			throw new IllegalStateException("Not bootstrapped");
		}
		
		// Start search threads for each peer
		Iterator<String> peerIterator = peers.getPeerListIterator();
		while (peerIterator.hasNext())
		{
			String peer = peerIterator.next();

			// Start the search thread
			System.out.println("Spawning thread to search "+peer);
			Thread t = new Thread(new SearchThread(listener, context.openFileInput(peer), peer, query));
			threads.add(t);
			t.start();
		}
		
		// Wait for search threads to terminate
		for (Thread t : threads)
		{
			t.join();
		}
	}
	
	public void startFileDownloadFromPeer(String peer, String file) throws URISyntaxException
	{
		DownloadManager downloader = (DownloadManager) context.getSystemService(Context.DOWNLOAD_SERVICE);
		DownloadManager.Request request;
		String title;
		String url;
		
		// Make a title from the last component of the file path
		title = file;
		if (title.lastIndexOf("/") > 0)
			title = title.substring(title.lastIndexOf("/"));

		//url = new URI("http", null, peer, 8000, "/file", "path="+file, null).toString();
		url = "http://anandtech.com/img/logo2.png";
		
		System.out.println("Downloading URL: "+url+" ("+title+")");

		request = new DownloadManager.Request(Uri.parse(url));
		
		// Download to the external downloads folder
		request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, "logo2.png");
		
		// Allow the media scanner to pick this file up
		request.allowScanningByMediaScanner();
		
		// Continue showing the notification even after the download finishes
		request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
		
		// Give it our title
		request.setTitle(title);
		
		// Fire the download
		downloader.enqueue(request);
	}


}

class IndexDownloadThread implements Runnable {
	Context context;
	URL bootstrapUrl;
	String peer;
	
	public IndexDownloadThread(Context context, URL bootstrapUrl, String peer)
	{
		this.context = context;
		this.bootstrapUrl = bootstrapUrl;
		this.peer = peer;
	}

	@Override
	public void run() {
		URL queryUrl;
		BufferedInputStream in;
		DataOutputStream fileOut;
		
		try {
			// Build the query index URL
			queryUrl = Client.createQueryUrl(bootstrapUrl, "indexfor?ip="+peer);
			
			// Get an input stream from the GET request on this URL
			in = Client.getInputStreamFromUrl(queryUrl);
			
			// Open the target file
			fileOut = new DataOutputStream(new BufferedOutputStream(context.openFileOutput(peer, 0)));
			
			// Read the index and write the it to a file
			for (;;)
			{
				int size = in.available();
				
				if (size == 0) size = 1;
				
				byte buf[] = new byte[size];
				
				if (in.read(buf) == -1)
					break;
				
				fileOut.write(buf);
			}
			
			fileOut.close();
			in.close();
			
			System.out.println("Wrote index: "+peer);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}

class SearchThread implements Runnable {
	FileInputStream peerFile;
	String query, peer;
	ResultListener listener;
	
	public SearchThread(ResultListener listener, FileInputStream peerFile, String peer, String query)
	{
		this.peerFile = peerFile;
		this.listener = listener;
		this.query = query;
		this.peer = peer;
	}

	@Override
	public void run() {
		String json;
		Scanner in;
		
		json = "";
		in = new Scanner(new BufferedInputStream(peerFile));
		
		while (in.hasNext())
		{
			json += in.next();
		}
		
		try {
			JSONObject index = new JSONObject(json);
			JSONArray fileList = index.getJSONArray("List");
			
			for (int i = 0; i < fileList.length(); i++)
			{
				JSONObject fileDescriptor = fileList.getJSONObject(i);
				String file = fileDescriptor.getString("FileName");
				
				System.out.println("Searching "+file+" for query "+query);
				if (file.toUpperCase().contains(query.toUpperCase()))
				{
					listener.foundResult(query, peer, file);
				}
			}
		} catch (JSONException e) {
			listener.searchFailed(query, peer, e);
		}
	}
}
