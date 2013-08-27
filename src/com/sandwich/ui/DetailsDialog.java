package com.sandwich.ui;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;

import com.sandwich.client.Client;
import com.sandwich.client.PeerSet;
import com.sandwich.client.PeerSet.Peer;
import com.sandwich.client.ResultListener;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;

public class DetailsDialog implements OnClickListener, Runnable {
	private Activity activity;
	private AlertDialog dialog;
	
	private ResultListener.Result result;
	private PeerSet.Peer peer;
	
	private static ArrayList<AlertDialog> dialogs = new ArrayList<AlertDialog>();
	
	public DetailsDialog(Activity activity, ResultListener.Result result)
	{
		this.activity = activity;
		this.result = result;
	}
	
	public DetailsDialog(Activity activity, PeerSet.Peer peer)
	{
		this.activity = activity;
		this.peer = peer;
	}
	
    public void createDetailsDialog()
    {
    	activity.runOnUiThread(this);
    }
    
    public static void dismissDialogs()
    {
    	for (AlertDialog d : dialogs)
    	{
    		d.dismiss();
    	}
    	
    	dialogs.clear();
    }

	@Override
	public void onClick(DialogInterface dialog, int arg1) {
		dialog.dismiss();
		dialogs.remove(dialog);
	}
	
	@SuppressLint("DefaultLocale")
	private static String humanReadableByteCount(long bytes) {
	    if (bytes < 1024) {
	    	return bytes + " bytes";
	    }
	    
	    int exp = (int) (Math.log(bytes) / Math.log(1024));
	    char prefix = "KMGTPE".charAt(exp-1);

	    return String.format("%.2f %cB", bytes / Math.pow(1024, exp), prefix);
	}

	@Override
	public void run() {
		if (dialog == null) {
	    	AlertDialog.Builder builder = new AlertDialog.Builder(activity);
	    	String peerList;
	    	
	    	builder.setNeutralButton("OK", this);
	    	
	    	if (result != null) {
		    	builder.setTitle(result.result);

		    	String streamable;
		    	if (Client.isResultStreamable(result))
		    		streamable = "Yes";
		    	else
		    		streamable = "No";
		    	
		    	// Create the peer list
		    	peerList = null;
		    	for (Peer p : result.peers)
		    	{
		    		if (peerList == null)
		    			peerList = p.getIpAddress();
		    		else
		    			peerList += ", "+p.getIpAddress();
		    	}
		    	
		    	// Create the size string
		    	String size = humanReadableByteCount(result.size);
		    	
		    	builder.setMessage("Peers: "+peerList+"\nSize: "+size+"\nStreamable: "+streamable);
	    	} else {
	    		builder.setTitle(peer.getIpAddress());
	    		
	    		String state;
	    		switch (peer.getState())
	    		{
	    		case Peer.STATE_BLACKLISTED:
	    			state = "Blacklisted";
	    			break;
	    			
	    		case Peer.STATE_UP_TO_DATE:
	    			state = "Up-to-date";
	    			break;
	    			
	    		case Peer.STATE_UPDATE_FAILED:
	    			state = "Update Failed";
	    			break;
	    			
	    		case Peer.STATE_UPDATE_FORBIDDEN:
	    			state = "Update Forbidden";
	    			break;
	    			
	    		case Peer.STATE_UPDATING:
	    			state = "Updating";
	    			break;
	    			
	    		default:
	    		case Peer.STATE_UNKNOWN:
	    			state = "Unknown";
	    			break;
	    		}
	    		
	    		InetAddress ip;
	    		try {
	    			ip = InetAddress.getByName(peer.getIpAddress());
	    		} catch (UnknownHostException e) {
	    			ip = null;
	    		}
	    		
	    		builder.setMessage("State: "+state+
	    				           "\nIndex Hash: "+peer.getIndexHash()+
	    				           "\nLast Seen: "+peer.getTimestamp()+
	    				           "\nCurrent IP Address: "+peer.getIpAddress()+
	    				           "\nPort: "+Client.getPortNumberFromIPAddress(ip));
	    	}
	    	
	    	dialog = builder.create();
			dialog.show();
			dialogs.add(dialog);
		} else {
			dialog.dismiss();
			dialogs.remove(dialog);
		}
	}
}
