package com.sandwich;

import java.util.ArrayList;

import com.sandwich.client.Client;
import com.sandwich.client.PeerSet.Peer;
import com.sandwich.client.ResultListener;

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
	    	long size = result.size;
	    	String units = "bytes";
	    	if (size >= 1024)
	    	{
	    		size /= 1024;
	    		units = "KB";
	    		if (size >= 1024)
	    		{
	    			size /= 1024;
	    			units = "MB";
	    			if (size >= 1024)
	    			{
	    				size /= 1024;
	    				units = "GB";
	    				if (size >= 1024)
	    				{
	    					size /= 1024;
	    					units = "TB";
	    				}
	    			}
	    		}
	    	}
	    	
	    	builder.setMessage("Peers: "+peerList+"\nSize: "+size+" "+units+"\nStreamable: "+streamable);
	    	
	    	dialog = builder.create();
			dialog.show();
			dialogs.add(dialog);
		} else {
			dialog.dismiss();
			dialogs.remove(dialog);
		}
	}
}
