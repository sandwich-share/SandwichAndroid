package com.sandwich.client;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.ConnectException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Scanner;

import android.app.DownloadManager;
import android.content.Context;
import android.net.Uri;

public class Client {
	Context context;
	PeerSet peers;
	
	public Client(Context context)
	{
		this.context = context;
		this.peers = null;
	}
	
	private HttpURLConnection createHttpConnection(URL url) throws IOException
	{
		HttpURLConnection conn = (HttpURLConnection) url.openConnection();
		
		// Disable cache
		conn.setUseCaches(false);
		
		return conn;
	}
	
	private int sendGetRequest(HttpURLConnection conn) throws IOException
	{		
		// Send the get request
		conn.setRequestMethod("GET");

		// Wait for the response
		return conn.getResponseCode();
	}
	
	private URL createQueryUrl(URL peerUrl, String querySuffix) throws MalformedURLException
	{
		String urlString = peerUrl.toExternalForm();
		
		// If it doesn't end with a slash, normalize it by adding a slash
		if (!urlString.endsWith("\\"))
			urlString += "\\";
		
		// Now append the query suffix
		urlString += querySuffix;
		
		return new URL(urlString);
	}
	
	private BufferedInputStream getInputStreamFromUrl(URL url) throws IOException
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
	
	private PeerSet getPeerList(URL bootstrapUrl) throws IOException
	{
		URL queryUrl;
		Scanner in;
		PeerSet peerSet;
		
		// Build the query peer list URL
		queryUrl = createQueryUrl(bootstrapUrl, "peerlist");
		
		// Get an input stream from the GET request on this URL
		in = new Scanner(getInputStreamFromUrl(queryUrl));
		
		// Read each line and add it to the peer set
		peerSet = new PeerSet();
		while (in.hasNextLine())
		{
			String nextPeer = in.nextLine();

			if (!peerSet.addPeerByString(nextPeer))
			{
				System.out.println("WARNING: Duplicate peer - "+nextPeer);
			}
		}
		
		in.close();
		
		return peerSet;
	}
	
	private void downloadIndexForPeer(URL bootstrapUrl, String peer) throws IOException
	{
		URL queryUrl;
		BufferedInputStream in;
		DataOutputStream fileOut;
		
		// Build the query index URL
		queryUrl = createQueryUrl(bootstrapUrl, "indexfor?ip="+peer);
		
		// Get an input stream from the GET request on this URL
		in = getInputStreamFromUrl(queryUrl);
		
		// Open the target file
		fileOut = new DataOutputStream(new BufferedOutputStream(context.openFileOutput(peer, 0)));
		
		// Read the index and write the it to a file
		while (in.available() > 0)
		{
			byte buf[] = new byte[in.available()];
			
			in.read(buf);
			
			fileOut.write(buf);
		}
		
		fileOut.close();
		in.close();
	}
	
	public void bootstrap(URL bootstrapUrl) throws IOException
	{
		Iterator<String> iterator;

		if (peers != null)
		{
			throw new IllegalStateException("Already bootstrapped");
		}

		System.out.println("Downloading the peer list");
		
		// Download the peer list
		peers = getPeerList(bootstrapUrl);
		
		// Iterate the peer list and download indexes for each
		iterator = peers.getPeerListIterator();
		while (iterator.hasNext())
		{
			String peer = iterator.next();
			
			System.out.println("Downloading index for peer: "+peer);
			
			downloadIndexForPeer(bootstrapUrl, peer);
		}
		
		System.out.println("Bootstrapped");
	}
	
	public void doSearch(String query, ResultListener listener) throws IOException, InterruptedException
	{
		ArrayList<Thread> threads = new ArrayList<Thread>();
		
		if (peers == null)
		{
			//throw new IllegalStateException("Not bootstrapped");
			
			listener.foundResult(query, query+" eat");
			listener.foundResult(query, query+" my");
			listener.foundResult(query, query+" sandwich");

			return;
		}
		
		// Start search threads for each peer
		Iterator<String> peerIterator = peers.getPeerListIterator();
		while (peerIterator.hasNext())
		{
			String peer = peerIterator.next();

			// Start the search thread
			Thread t = new Thread(new SearchThread(listener, context.openFileInput(peer), query));
			threads.add(t);
			t.start();
		}
		
		// Wait for search threads to terminate
		for (Thread t : threads)
		{
			t.join();
		}
	}
	
	public void startFileDownloadFromPeer(String peer, String file)
	{
		DownloadManager downloader = (DownloadManager) context.getSystemService(Context.DOWNLOAD_SERVICE);
		DownloadManager.Request request;
		String title;
		
		// Make a title from the last component of the file path
		title = file;
		if (title.lastIndexOf("/") > 0)
			title = title.substring(title.lastIndexOf("/"));
		
		request = new DownloadManager.Request(Uri.parse("http://"+peer+"/files/"+file));
		
		// Allow the media scanner to pick this file up
		request.allowScanningByMediaScanner();
		
		// Give it our title
		request.setTitle(title);
		
		// Fire the download
		downloader.enqueue(request);
	}
}

class SearchThread implements Runnable {
	FileInputStream peerFile;
	String query;
	ResultListener listener;
	
	public SearchThread(ResultListener listener, FileInputStream peerFile, String query)
	{
		this.peerFile = peerFile;
		this.listener = listener;
		this.query = query;
	}

	@Override
	public void run() {
		Scanner scanner = new Scanner(new BufferedInputStream(peerFile));
		
		while (scanner.hasNextLine())
		{
			String file = scanner.nextLine();
			
			if (file.contains(query))
			{
				listener.foundResult(query, file);
			}
		}
	}
}
