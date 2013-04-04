package com.sandwich.client;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataOutputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
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
import java.net.URLConnection;
import java.net.UnknownHostException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Scanner;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.DownloadManager;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;

public class Client {
	private Context context;
	private PeerSet peers;
	
	private static final String CREATE_TABLE = "CREATE TABLE peers (IP TEXT PRIMARY KEY, IndexHash INT);";
	
	static final String PEER_TABLE = "peers";
	static final String PEER_DB = "peers.db";
	
	public Client(Context context)
	{
		this.context = context;
		this.peers = new PeerSet();
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
	
	private Thread startDownloadIndexThreadForPeer(SQLiteDatabase database, PeerSet.Peer peer)
	{
		Thread t = new Thread(new IndexDownloadThread(database, context, peer));
		t.start();
		return t;
	}
	
	private PeerSet getPeerSetFromDatabase(SQLiteDatabase database) throws SQLiteException
	{
		PeerSet peers = new PeerSet();
		Cursor c = database.query(PEER_TABLE, new String[] {"IP", "IndexHash"}, null, null, null, null, null, null);
		
		c.moveToFirst();
		while (!c.isAfterLast())
		{
			peers.addPeer(c.getString(0), "FIXME", Long.parseLong(c.getString(1), 10));
			c.moveToNext();
		}
		
		c.close();
		
		return peers;
	}
	
	static long getOldHashOfPeerIndex(SQLiteDatabase database, String peer)
	{
		Cursor c = database.query(PEER_TABLE, new String[] {"IP", "IndexHash"}, null, null, null, null, null, null);
		
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
	
	private boolean doesDatabaseExist(String dbname)
	{
		// FIXME: See if there's a better way to do this
		
		for (String db : context.databaseList())
			if (db.equals(dbname))
				return true;
		
		return false;
	}
	
	private SQLiteDatabase getDatabase(String dbname)
	{
		SQLiteDatabase database;
		
		// Open our database
		database = context.openOrCreateDatabase(dbname, Context.MODE_PRIVATE, null);
		
		return database;
	}
	
	private boolean readCachedBootstrapData()
	{
		SQLiteDatabase database;
		boolean success;
		
		// If it doesn't exist, we have no cache to read
		if (!doesDatabaseExist(PEER_DB))
			return false;
		
		System.out.println("Loading cached bootstrap data");
		
		try {
			database = getDatabase(PEER_DB);
		} catch (SQLiteException e) {
			// Couldn't read the DB, so cached bootstrap fails
			return false;
		}
		
		try {
			// Try to read the peer set from the database
			peers.updatePeerSet(getPeerSetFromDatabase(database));
			success = true;
		} catch (SQLiteException e) {
			// Failed to read peer set
			success = false;
		} finally {
			// Close the database
			database.close();
		}
		
		return success;
	}
	
	public boolean bootstrapFromCache()
	{
		// Just read the bootstrap data in from the database
		return readCachedBootstrapData();
	}
	
	private boolean downloadPeerList(String initialPeer) throws NoSuchAlgorithmException, URISyntaxException, IOException, JSONException
	{
		URL bootstrapUrl;
		Iterator<PeerSet.Peer> iterator;
		
		System.out.println("Downloading the peer list");

		// Try the initial peer first
		if (initialPeer != null)
		{			
			try {
				// Resolve address and create a URL
				bootstrapUrl = new URL(createPeerUrlString(initialPeer, null, null));
				
				// Download the peer list
				peers.updatePeerSet(getPeerList(bootstrapUrl));
				
				// It worked
				return true;
			} catch (Exception e) {
				// Failed to connect to the initial peer, so let's try another one
			}
		}
		
		// Try to bootstrap from another peer
		iterator = peers.getPeerListIterator();
		while (iterator.hasNext())
		{
			try {
				// Resolve address and create a URL
				bootstrapUrl = new URL(createPeerUrlString(iterator.next().getIpAddress(), null, null));
				
				// Download the peer list
				peers.updatePeerSet(getPeerList(bootstrapUrl));
				
				// If we get here, bootstrapping was successful
				return true;
			} catch (Exception e) {
				// Failed to connect to this one
			}
		}
		
		// We're out of people to bootstrap from
		return false;
	}
	
	public void bootstrapFromNetwork(String initialPeer) throws IOException, JSONException, InterruptedException, NoSuchAlgorithmException, URISyntaxException
	{
		Iterator<PeerSet.Peer> iterator;
		ArrayList<Thread> threadList;
		SQLiteDatabase database;
		boolean databaseCreated;

		// Download the peer list
		if (!downloadPeerList(initialPeer))
		{
			throw new IllegalStateException("No peers available");
		}
		
		databaseCreated = !doesDatabaseExist(PEER_DB);
		database = getDatabase(PEER_DB);
		
		try {
			// Create our table if it's a new database
			if (databaseCreated)
			{
				database.execSQL(CREATE_TABLE);
			}
			
			// Iterate the peer list and download indexes for each
			iterator = peers.getPeerListIterator();
			threadList = new ArrayList<Thread>();
			while (iterator.hasNext())
			{
				PeerSet.Peer peer = iterator.next();
				long oldHash;
				
				// Check if the old index hash is valid
				oldHash = getOldHashOfPeerIndex(database, peer.getIpAddress());
				if (oldHash == peer.getIndexHash())
				{
					System.out.println(peer.getIpAddress()+" index is up to date (hash: "+peer.getIndexHash()+")");
					
					// Don't download anything
					continue;
				}
				
				// We need to download the updated index
				threadList.add(startDownloadIndexThreadForPeer(database, peer));
			}
			
			// Wait for downloads to finish
			for (Thread t : threadList)
				t.join();
			
			System.out.println("Bootstrapped");
		} finally {
			// Close the database
			database.close();
		}

	}
	
	public void beginSearch(String query, ResultListener listener) throws IOException, InterruptedException
	{
		ArrayList<Thread> threads = new ArrayList<Thread>();
		ArrayList<PeerSet.Peer> peersToReap = new ArrayList<PeerSet.Peer>();
		
		if (peers == null)
		{
			throw new IllegalStateException("Not bootstrapped");
		}
		
		// Start search threads for each peer
		Iterator<PeerSet.Peer> peerIterator = peers.getPeerListIterator();
		while (peerIterator.hasNext())
		{
			PeerSet.Peer peer = peerIterator.next();
			FileInputStream file;
			
			try {
				// Try to open the peer index
				file = context.openFileInput(peer.getIpAddress());
			} catch (FileNotFoundException e) {
				// If it doesn't exist, remove the peer from the peer list
				peersToReap.add(peer);
				continue;
			}

			// Start the search thread
			System.out.println("Spawning thread to search "+peer.getIpAddress());
			Thread t = new Thread(new SearchThread(context, listener, file, peer, query));
			threads.add(t);
			t.start();
		}
		
		// Reap all dead peers
		for (PeerSet.Peer p : peersToReap)
			p.remove();
	}

	@TargetApi(11)
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
		
		// DownloadManager was enhanced with Honeycomb with useful features we want to activate
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB)
		{
			// Allow the media scanner to pick this file up
			request.allowScanningByMediaScanner();
		
			// Continue showing the notification even after the download finishes
			request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
		}
		
		// Give it our title
		request.setTitle(title);
		
		// Fire the download
		downloader.enqueue(request);
	}
	
	public boolean startFileStreamFromPeer(Activity activity, String peer, String file) throws NoSuchAlgorithmException, URISyntaxException, MalformedURLException, IOException
	{
		String url;
		String mimeType;

		mimeType = URLConnection.guessContentTypeFromName(file);
		url = createPeerUrlString(peer, "/file", "path="+file);
		if (mimeType == null)
		{
			// Undetermined MIME type
			return false;
		}
		else if (mimeType.startsWith("audio/"))
		{
			// Create the audio player activity
			Intent i = new Intent(activity, com.sandwich.AudioPlayer.class);
			i.putExtra("URL", url);
			activity.startActivity(i);
		}
		else if (mimeType.startsWith("video/"))
		{
			// Create the video player activity
			Intent i = new Intent(activity, com.sandwich.VideoPlayer.class);
			i.putExtra("URL", url);
			activity.startActivity(i);
		}
		else
		{
			// No player for this
			return false;
		}
		
		return true;
	}
}

class IndexDownloadThread implements Runnable {
	Context context;
	PeerSet.Peer peer;
	SQLiteDatabase database;
	
	public IndexDownloadThread(SQLiteDatabase database, Context context, PeerSet.Peer peer)
	{
		this.context = context;
		this.peer = peer;
		this.database = database;
	}

	@Override
	public void run() {
		URL queryUrl;
		BufferedInputStream in;
		DataOutputStream fileOut;
		long oldHash;
		
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
				
				if (size <= 0)
					size = 1;
				
				byte buf[] = new byte[size];
				
				if (in.read(buf) == -1)
					break;
				
				fileOut.write(buf);
			}
			
			fileOut.close();
			in.close();
			
			// Create the values to be stored in the SQL database
			ContentValues vals = new ContentValues();
			vals.put("IP", peer.getIpAddress());
			vals.put("IndexHash", ""+peer.getIndexHash());
			
			database.beginTransaction();
			try {
				oldHash = Client.getOldHashOfPeerIndex(database, peer.getIpAddress());
				if (oldHash == -1)
				{
					System.out.println(peer.getIpAddress()+" has never been seen before (new hash: "+peer.getIndexHash()+")");

					// We need to insert this into the list
					database.insert(Client.PEER_TABLE, null, vals);
				}
				else
				{
					System.out.println(peer.getIpAddress()+" has a newer index (old hash: "+oldHash+" | new hash: "+peer.getIndexHash()+")");

					// We need to replace this peer's existing values
					database.replace(Client.PEER_TABLE, null, vals);
				}
				
				database.setTransactionSuccessful();
			} finally {
				database.endTransaction();
			}
			
			System.out.println("Index for "+peer.getIpAddress()+" downloaded (hash: "+peer.getIndexHash()+")");
		} catch (Exception e) {
			System.out.println(e.getMessage());
			
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
			System.out.println(e.getMessage());
			
			// Delete the peer's index file (which is likely corrupt)
			context.deleteFile(peer.getIpAddress());
			
			// Remove them from the peer set
			peer.remove();
		}
	}
}
