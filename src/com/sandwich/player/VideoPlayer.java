package com.sandwich.player;

import com.sandwich.R;

import android.app.Activity;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnErrorListener;
import android.net.Uri;
import android.widget.MediaController;
import android.widget.VideoView;

public class VideoPlayer implements SandwichPlayer,OnErrorListener {
	private Activity activity;
	private VideoView player;
	
	public VideoPlayer(Activity activity)
	{
		this.activity = activity;
		this.player = (VideoView)activity.findViewById(R.id.videoView);
	}
	
	public void initialize(Uri mediaPath)
	{ 
		MediaController controller = new MediaController(activity);

		// Attach the controller to the video view
		controller.setAnchorView(player);
		
		// Setup the video player
		player.setMediaController(controller);
		player.setVideoURI(mediaPath);
		player.setOnErrorListener(this);
	}
	
	public void start()
	{
		// Enable wakelock
		player.setKeepScreenOn(true);
		
		// Start the media
		player.start();
	}
	
	public void stop()
	{
		// Disable wakelock
		player.setKeepScreenOn(false);
		
		// Stop the media
		player.stopPlayback();
	}
	
	public void release()
	{
		// Release the player
		player = null;
	}
	
	@Override
	public boolean onError(MediaPlayer player, int what, int extra) {
		return false;
	}
}
