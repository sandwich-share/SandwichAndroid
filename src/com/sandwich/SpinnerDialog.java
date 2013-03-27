package com.sandwich;

import java.util.ArrayList;

import android.app.Activity;
import android.app.ProgressDialog;

public class SpinnerDialog implements Runnable {
	private String title, message;
	private Activity activity;
	private ProgressDialog progress;
	
	private static ArrayList<SpinnerDialog> rundownDialogs = new ArrayList<SpinnerDialog>();
	
	public SpinnerDialog(Activity activity, String title, String message)
	{
		this.activity = activity;
		this.title = title;
		this.message = message;
		this.progress = null;
	}
	
	public static SpinnerDialog displayDialog(Activity activity, String title, String message)
	{
		SpinnerDialog spinner = new SpinnerDialog(activity, title, message);
		activity.runOnUiThread(spinner);
		return spinner;
	}
	
	public static void closeDialogs()
	{
		for (SpinnerDialog d : rundownDialogs)
			d.progress.dismiss();
	}
	
	public void dismiss()
	{
		// Running again with progress != null will destroy it
		activity.runOnUiThread(this);
	}
	
	@Override
	public void run() {
		
		if (progress == null)
		{
	    	progress = new ProgressDialog(activity);
	    	
	    	progress.setTitle(title);
	    	progress.setMessage(message);
	    	progress.setProgressStyle(ProgressDialog.STYLE_SPINNER);
	    	progress.setCancelable(false);
	    	progress.setCanceledOnTouchOutside(false);
	    	
	    	progress.show();
		}
		else
		{
			progress.dismiss();
		}
	}
}
