package com.sandwich.client;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.ConnectException;
import java.net.HttpURLConnection;
import java.net.Inet6Address;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.UnknownHostException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Scanner;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.DownloadManager;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.net.Uri;
import android.os.Environment;

public class Client {
	private Context context;
	private PeerSet peers;
	
	private static final String CREATE_TABLE = "CREATE TABLE peers (IP TEXT PRIMARY KEY, IndexHash INT);";
	
	public Client(Context context)
	{
		this.context = context;
		this.peers = null;
	}
	
	// Holy fucking shit
	private static int unsign(byte signed)
	{
		if (signed >= 0)
			return signed;
		else
			return signed+256;
	}
	
	public static int getPortNumberFromIPAddress(InetAddress address) throws NoSuchAlgorithmException
	{
		MessageDigest md = MessageDigest.getInstance("MD5");
		byte[] hash;
		byte[] addr;
		int port;
		
		// We need an IPv6 address in all cases
		if (address instanceof Inet4Address)
		{
			// If it's an IPv4 address, we need to add 2 octets of 255
			addr = new byte[address.getAddress().length+12];
			
			// Copy the IPv4 portion over at the 12th octet
			System.arraycopy(address.getAddress(), 0, addr, 12, address.getAddress().length);
			
			// Copy the FF octets from the 10th octet
			addr[10] = (byte) 0xFF;
			addr[11] = (byte) 0xFF;
		}
		else if (address instanceof Inet6Address)
		{
			// It's fine the way it is
			addr = address.getAddress();
		}
		else
		{
			throw new IllegalArgumentException("Not an IPv4 or IPv6 address");
		}
		
		// Compute the MD5 of the address in byte form
		hash = md.digest(addr);
		
		// Create the port number by bit-shifting the hash
		port = (unsign(hash[0]) + unsign(hash[3])) << 8;
		port += unsign(hash[1]) + unsign(hash[2]);
		port &= 0xFFFF;
		
		// Keep the port number of out the sub-1024 range
		if (port < 1024)
			port += 1024;
		
		return port;
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
	
	static String createPeerUrlString(String peer, String path, String parameters) throws URISyntaxException, UnknownHostException, NoSuchAlgorithmException
	{
		return new URI("http", null, peer, getPortNumberFromIPAddress(Inet6Address.getByName(peer)), path, parameters, null).toString();
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
		
		jsonPeerList = new JSONArray(json);
		peerSet = new PeerSet();
		for (int i = 0; i < jsonPeerList.length(); i++)
		{
			JSONObject jsonPeer = jsonPeerList.getJSONObject(i);

			peerSet.addPeer(jsonPeer.getString("IP"), jsonPeer.getString("LastSeen"), jsonPeer.getLong("IndexHash"));
		}
		
		in.close();
		
		return peerSet;
	}
	
	private Thread startDownloadIndexThreadForPeer(URL bootstrapUrl, PeerSet.Peer peer)
	{
		Thread t = new Thread(new IndexDownloadThread(context, bootstrapUrl, peer));
		t.start();
		return t;
	}
	
	private long getOldHashOfPeerIndex(SQLiteDatabase database, String peer)
	{
		Cursor c = database.query("peers", new String[] {"IP", "IndexHash"}, null, null, null, null, null, null);
		
		c.moveToFirst();
		while (!c.isAfterLast())
		{
			if (c.getString(0).equals(peer))
			{
				String hash = c.getString(1);

				c.close();
				return Long.parseLong(hash, 10);
			}

			c.moveToNext();
		}

		c.close();
		return -1;
	}
	
	public void bootstrap(URL bootstrapUrl) throws IOException, JSONException, InterruptedException
	{
		Iterator<PeerSet.Peer> iterator;
		ArrayList<Thread> threadList;
		SQLiteDatabase database;

		System.out.println("Downloading the peer list");
		
		// Open our peer list database
		database = context.openOrCreateDatabase("peers.db", Context.MODE_PRIVATE, null);
		try {
			// This is janky but I don't know how to query for a table's existence
			database.execSQL(CREATE_TABLE);
		} catch (SQLiteException e) {}
		
		// Start an exclusive transaction
		database.beginTransaction();
		
		try {
			// Download the peer list
			peers = getPeerList(bootstrapUrl);
			
			// Iterate the peer list and download indexes for each
			iterator = peers.getPeerListIterator();
			threadList = new ArrayList<Thread>();
			while (iterator.hasNext())
			{
				PeerSet.Peer peer = iterator.next();
				ContentValues vals = new ContentValues();
				long oldHash;
				
				// Create the values to be stored in the SQL database
				vals.put("IP", peer.getIpAddress());
				vals.put("IndexHash", ""+peer.getIndexHash());
				
				// Check if the old index hash is valid
				oldHash = getOldHashOfPeerIndex(database, peer.getIpAddress());
				if (oldHash == -1)
				{
					System.out.println(peer.getIpAddress()+" has never been seen before (new hash: "+peer.getIndexHash()+")");

					// We need to insert this into the list
					database.insert("peers", null, vals);
				}
				else if (oldHash != peer.getIndexHash())
				{
					System.out.println(peer.getIpAddress()+" has a newer index (old hash: "+oldHash+" | new hash: "+peer.getIndexHash()+")");

					// We need to replace this peer's existing values
					database.replace("peers", null, vals);
				}
				else
				{
					System.out.println(peer.getIpAddress()+" index is up to date (hash: "+peer.getIndexHash()+")");
					
					// Don't download anything
					continue;
				}
				
				// We need to download the updated index
				threadList.add(startDownloadIndexThreadForPeer(bootstrapUrl, peer));
			}
			
			// Wait for downloads to finish
			for (Thread t : threadList)
				t.join();
			
			System.out.println("Bootstrapped");
			
			// Transaction was successful
			database.setTransactionSuccessful();
			
		} finally {
			// End the transaction
			database.endTransaction();
			
			// Close the database
			database.close();
		}

	}
	
	public void beginSearch(String query, ResultListener listener) throws IOException, InterruptedException
	{
		ArrayList<Thread> threads = new ArrayList<Thread>();
		
		if (peers == null)
		{
			throw new IllegalStateException("Not bootstrapped");
		}
		
		// Start search threads for each peer
		Iterator<PeerSet.Peer> peerIterator = peers.getPeerListIterator();
		while (peerIterator.hasNext())
		{
			PeerSet.Peer peer = peerIterator.next();

			// Start the search thread
			System.out.println("Spawning thread to search "+peer.getIpAddress());
			Thread t = new Thread(new SearchThread(context, listener, context.openFileInput(peer.getIpAddress()), peer, query));
			threads.add(t);
			t.start();
		}
	}

	public void startFileDownloadFromPeer(String peer, String file) throws URISyntaxException, UnknownHostException, NoSuchAlgorithmException
	{
		DownloadManager downloader = (DownloadManager) context.getSystemService(Context.DOWNLOAD_SERVICE);
		DownloadManager.Request request;
		String title;
		String url;
		
		// Make a title from the last component of the file path
		title = file;
		if (title.lastIndexOf("/") > 0)
			title = title.substring(title.lastIndexOf("/")+1);

		url = createPeerUrlString(peer, "/file", "path="+file);
		
		System.out.println("Downloading URL: "+url+" ("+title+")");

		request = new DownloadManager.Request(Uri.parse(url));
		
		// Download to the external downloads folder
		request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, title);
		
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
	PeerSet.Peer peer;
	
	public IndexDownloadThread(Context context, URL bootstrapUrl, PeerSet.Peer peer)
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
			queryUrl = new URL(Client.createPeerUrlString(peer.getIpAddress(), "/indexfor", null));
			
			// Get an input stream from the GET request on this URL
			in = Client.getInputStreamFromUrl(queryUrl);
			
			// Open the target file
			fileOut = new DataOutputStream(new BufferedOutputStream(context.openFileOutput(peer.getIpAddress(), 0)));
			
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
			
			System.out.println("Index for "+peer.getIpAddress()+" downloaded (hash: "+peer.getIndexHash()+")");
		} catch (Exception e) {
			e.printStackTrace();
			
			// Remove this peer from the peer set
			peer.remove();
		}
	}
}

class SearchThread implements Runnable {
	FileInputStream peerFile;
	String query;
	PeerSet.Peer peer;
	ResultListener listener;
	Context context;
	
	public SearchThread(Context context, ResultListener listener, FileInputStream peerFile, PeerSet.Peer peer, String query)
	{
		this.context = context;
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
				
				if (file.toUpperCase().contains(query.toUpperCase()))
				{
					listener.foundResult(query, peer.getIpAddress(), file);
				}
			}
		} catch (JSONException e) {
			e.printStackTrace();
			
			// Notify the listener of the failed search
			listener.searchFailed(query, peer.getIpAddress(), e);
			
			// Delete the peer's index file (which is likely corrupt)
			context.deleteFile(peer.getIpAddress());
			
			// Remove them from the peer set
			peer.remove();
		}
	}
}
