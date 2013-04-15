package com.sandwich;

import java.util.Set;

import com.sandwich.client.PeerSet;
import com.sandwich.client.PeerSet.Peer;

import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.app.Activity;
import android.content.Intent;

public class PeerList extends Activity implements OnItemClickListener {
	private static ClientThread client;
	private ListView peerList;
	private ArrayAdapter<Peer> adapter;
	
	public static void addClient(ClientThread t) {
		client = t;
	}
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_peer_list);
		
		// Setup the list view
		peerList = (ListView)findViewById(R.id.peersListView);
		adapter = new ArrayAdapter<Peer>(this, R.layout.simplerow);
        peerList.setAdapter(adapter);
        peerList.setOnItemClickListener(this);
        
        // Perform initial update
        updateListView();
	}

	private void updateListView()
	{
		Set<Peer> peerSet = client.getPeerSet();
		adapter.clear();
		for (Peer p : peerSet)
		{
			adapter.add(p);
		}
	}
	
	@Override
	public void onItemClick(AdapterView<?> arg0, View arg1, int pos, long id) {
        PeerSet.Peer peer = adapter.getItem(pos);
        Intent filesPage = new Intent(this, PeerFiles.class);
        filesPage.putExtra(PeerFiles.PEER, peer.getIpAddress());
        startActivity(filesPage);
	}
}
