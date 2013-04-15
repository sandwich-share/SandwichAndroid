package com.sandwich.ui;

import java.util.ArrayList;

import com.sandwich.client.Client;
import com.sandwich.client.PeerSet.Peer;
import com.sandwich.client.ResultListener;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;

public class DetailsDialog implements OnClickListener, Runnable {
	private Activity activity;
	private ResultListener.Result result;
	private AlertDialog dialog;
	
	private static ArrayList<AlertDialog> dialogs = new ArrayList<AlertDialog>();
	
	public DetailsDialog(Activity activity, ResultListener.Result result)
	{
		this.activity = activity;
		this.result = result;
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
	    	
	    	builder.setTitle(result.result);
	    	builder.setNeutralButton("OK", this);
	    	
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
	    	
	    	dialog = builder.create();
			dialog.show();
			dialogs.add(dialog);
		} else {
			dialog.dismiss();
			dialogs.remove(dialog);
		}
	}
}
