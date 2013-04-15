package com.sandwich.player;

import com.sandwich.R;
import com.sandwich.ui.Dialog;
import com.sandwich.ui.SpinnerDialog;

import android.app.Activity;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnErrorListener;
import android.media.MediaPlayer.OnPreparedListener;
import android.net.Uri;
import android.widget.MediaController;
import android.widget.VideoView;

public class VideoPlayer implements SandwichPlayer,OnErrorListener,OnPreparedListener {
	private Activity activity;
	private VideoView player;
	private SpinnerDialog waitDialog;
	
	private int savedPosition;
	
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
		player.setOnPreparedListener(this);
	}
	
	public void pause()
	{
		savedPosition = player.getCurrentPosition();
		player.pause();
	}
	
	public void resume()
	{
		player.seekTo(savedPosition);
		player.resume();
	}
	
	public void start()
	{
		// Enable wakelock
		player.setKeepScreenOn(true);
		
		// Start the media
		waitDialog = SpinnerDialog.displayDialog(activity, "Please Wait", "Loading Media", true);
		player.start();
	}
	
	public void stop()
	{
		// Dismiss the wait dialog
		if (waitDialog != null)
		{
			waitDialog.dismiss();
			waitDialog = null;
		}
		
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
	public boolean onError(MediaPlayer player, int what, int extra)
	{
		// Dismiss the wait dialog
		if (waitDialog != null)
		{
			waitDialog.dismiss();
			waitDialog = null;
		}
		
		String error;
		
		// Decode the error
		switch (extra)
		{
		case MediaPlayer.MEDIA_ERROR_IO:
		case MediaPlayer.MEDIA_ERROR_MALFORMED:
		case MediaPlayer.MEDIA_ERROR_TIMED_OUT:
			error = "Connection failed";
			break;

		case MediaPlayer.MEDIA_ERROR_UNSUPPORTED:
		case MediaPlayer.MEDIA_ERROR_NOT_VALID_FOR_PROGRESSIVE_PLAYBACK:
			error = "Media format unsupported";
			break;

		default:
			error = "Unknown error";
			break;
		}
		
		// Display an error dialog
		Dialog.displayDialog(activity, "Streaming Error", error, true);
		return true;
	}
	
	@Override
	public boolean isPlaying() {
		return (player != null && player.isPlaying());
	}

	@Override
	public void onPrepared(MediaPlayer arg0)
	{
		// Dismiss the wait dialog
		if (waitDialog != null)
		{
			waitDialog.dismiss();
			waitDialog = null;
		}
	}
}
