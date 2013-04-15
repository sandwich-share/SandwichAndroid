package com.sandwich;

import java.util.concurrent.ConcurrentLinkedQueue;

import com.sandwich.client.PeerSet.Peer;
import com.sandwich.client.ResultListener;

import android.os.Bundle;
import android.app.Activity;
import android.widget.ArrayAdapter;
import android.widget.ListView;

public class PeerFiles extends Activity implements ResultListener, Runnable {
	private static ClientThread client;
	private String peer;
	private ListView filesView;
	private ArrayAdapter<ResultListener.Result> adapter;
	private ConcurrentLinkedQueue<ResultListener.Result> results;

	public static final String PEER = "Peer";	
	
	public static void addClient(ClientThread t) {
		client = t;
	}
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_peer_files);
		
		peer = getIntent().getExtras().getString(PEER);
		setTitle("File List for "+peer);
		
		results = new ConcurrentLinkedQueue<ResultListener.Result>();
		adapter = new ArrayAdapter<ResultListener.Result>(this, R.layout.simplerow);
		filesView = (ListView) findViewById(R.id.peerFilesView);
		filesView.setAdapter(adapter);

		client.getPeerIndex(peer, this);
	}

	@Override
	public void foundResult(String query, Result result) {
		results.add(result);
		runOnUiThread(this);
	}

	@Override
	public void searchComplete(String query, Peer peer) {
		// Done getting index
	}

	@Override
	public void run() {
		for (;;)
		{
			ResultListener.Result r = results.poll();
			if (r == null)
				break;
			
			adapter.add(r);
		}
	}
}
