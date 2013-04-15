package com.sandwich;

import java.util.concurrent.ConcurrentLinkedQueue;

import com.sandwich.client.PeerSet.Peer;
import com.sandwich.client.Client;
import com.sandwich.client.ResultListener;
import com.sandwich.ui.DetailsDialog;
import com.sandwich.ui.Dialog;

import android.os.Bundle;
import android.app.Activity;
import android.view.ContextMenu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ContextMenu.ContextMenuInfo;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.AdapterView.AdapterContextMenuInfo;

public class PeerFiles extends Activity implements ResultListener, Runnable, OnItemClickListener {
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
		filesView.setOnItemClickListener(this);
		registerForContextMenu(filesView);

		client.endSearch();
		client.getPeerIndex(peer, this);
	}
	
	@Override
	protected void onDestroy() {
		client.endSearch();
		super.onDestroy();
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
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        
        AdapterContextMenuInfo info = (AdapterContextMenuInfo) menuInfo;
        ResultListener.Result res = (Result) filesView.getAdapter().getItem(info.position);
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
        ResultListener.Result result = (ResultListener.Result)filesView.getAdapter().getItem(info.position);
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
	public void run() {
		for (;;)
		{
			ResultListener.Result r = results.poll();
			if (r == null)
				break;
			
			adapter.add(r);
		}
	}

	@Override
	public void onItemClick(AdapterView<?> adapter, View view, int pos, long id) {
		openContextMenu(view);
	}
}
