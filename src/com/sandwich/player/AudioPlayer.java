package com.sandwich.player;

import java.io.IOException;

import com.sandwich.Dialog;

import android.app.Activity;
import android.app.Service;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.net.Uri;
import android.os.IBinder;

public class AudioPlayer implements SandwichPlayer,ServiceConnection {

	private Activity activity;
	private AudioPlayerService.AudioBinder binder;
	private Intent service;
	private Uri filePath;
	
	public AudioPlayer(Activity activity)
	{
		this.activity = activity;
	}
	
	public void initialize(Uri filePath) throws IllegalArgumentException, SecurityException, IllegalStateException, IOException
	{
		this.filePath = filePath;

		service = new Intent(activity, AudioPlayerService.class);
		activity.getApplicationContext().startService(service);
		activity.getApplicationContext().bindService(service, this, Service.BIND_AUTO_CREATE);
	}

	@Override
	public void onServiceConnected(ComponentName arg0, IBinder arg1) {
		binder = (AudioPlayerService.AudioBinder)arg1;
		try {
			binder.initialize(activity, filePath);
			binder.start();
		} catch (Exception e) {
			Dialog.displayDialog(activity, "Audio Player Error", "Failed to initialize audio player", true);
		}
	}

	@Override
	public void onServiceDisconnected(ComponentName arg0) {
		binder.release();
		binder = null;
	}

	@Override
	public void start() {
		if (binder != null) {
			binder.start();
		}
	}
	
	@Override
	public boolean isPlaying() {
		if (binder != null) {
			return binder.isPlaying();
		} else {
			return false;
		}
	}

	@Override
	public void stop() {
		if (binder != null) {
			binder.stop();
		}
	}

	@Override
	public void release() {
		if (binder != null) {
			binder.release();
			binder = null;
		}
		
		if (service != null) {
			activity.getApplicationContext().stopService(service);
			activity.getApplicationContext().unbindService(this);
		}
	}
}
