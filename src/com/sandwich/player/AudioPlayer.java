package com.sandwich.player;

import java.io.IOException;

import com.sandwich.R;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Service;
import android.content.Intent;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;

@SuppressLint("HandlerLeak")
public class AudioPlayer extends Service implements SandwichPlayer,MediaPlayer.OnErrorListener,
	MediaPlayer.OnPreparedListener,OnClickListener,OnSeekBarChangeListener,MediaPlayer.OnCompletionListener {

	private Activity activity;
	private MediaPlayer player;
	private Button playpause;
	private SeekBar seeker;
	private TextView timeView;
	
	boolean touchActive = false;
	
	private Handler handler = new Handler()
	{
	    @Override
	    public void handleMessage(Message msg)
	    {
	        int pos;
	        switch (msg.what)
	        {
	            case SHOW_PROGRESS:
	            	if (player != null)
	            	{
	            		pos = player.getCurrentPosition();
	            		seeker.setProgress(pos);
	                
	            		if (!touchActive && player.isPlaying())
	            		{
	            			msg = obtainMessage(SHOW_PROGRESS);
	            			sendMessageDelayed(msg, 1000);
	            		}
	            	}
	                break;
	                
	            default:
	            	super.handleMessage(msg);
	            	break;
	        }
	    }
	};
	
	final static int SHOW_PROGRESS = 12813;
	
	public AudioPlayer(Activity activity)
	{
		this.activity = activity;

		this.seeker = (SeekBar)activity.findViewById(R.id.seekBar);
		this.playpause = (Button)activity.findViewById(R.id.playButton);
		this.timeView = (TextView)activity.findViewById(R.id.timeView);
	}
	
	public void initialize(Uri filePath) throws IllegalArgumentException, SecurityException, IllegalStateException, IOException
	{
		player = new MediaPlayer();
		
		// Add listeners
		playpause.setOnClickListener(this);
		seeker.setOnSeekBarChangeListener(this);
		
		// Configure media player
		player.setDataSource(activity.getApplicationContext(), filePath);
		player.setAudioStreamType(AudioManager.STREAM_MUSIC);
		player.setOnErrorListener(this);
		player.setOnPreparedListener(this);
		player.setOnCompletionListener(this);
	}
	
	public void start()
	{
		// Prepare and start asynchronously
		player.prepareAsync();
	}
	
	public void stop()
	{
		// Stop the player
		player.stop();
		
		// Stop progress bar updates
		handler.removeMessages(SHOW_PROGRESS);
	}
	
	public void release()
	{
		// Release the player
		player.release();
		player = null;
	}

	@Override
	public boolean onError(MediaPlayer mp, int what, int extra) {
		return false;
	}

	@Override
	public IBinder onBind(Intent arg0) {
		return null;
	}

	@Override
	public void onPrepared(MediaPlayer mp)
	{	
		// Setup the progress bar with the duration of the media
		seeker.setMax(player.getDuration());
		seeker.setProgress(0);
		
		// Start playback
		player.seekTo(0);
		player.start();
		handler.sendEmptyMessage(SHOW_PROGRESS);
		playpause.setText("Pause");
	}

	@Override
	// Called when play/pause button is clicked
	public void onClick(View target) {
		// If it's playing, pause it. If it's paused, play it.
		if (player.isPlaying())
		{
			player.pause();
			handler.removeMessages(SHOW_PROGRESS);
			playpause.setText("Play");
		}
		else
		{
			player.start();
			handler.sendEmptyMessage(SHOW_PROGRESS);
			playpause.setText("Pause");
		}
	}

	@Override
	public void onProgressChanged(SeekBar seeker, int newProgress, boolean fromUser) {
		int newMinutes, newSeconds;
		int totalMinutes, totalSeconds;
		String time = "";
		
		// Compute the new time
		newSeconds = newProgress / 1000;
		newMinutes = newSeconds / 60;
		newSeconds %= 60;
		
		// Compute the total time
		totalSeconds = player.getDuration() / 1000;
		totalMinutes = totalSeconds / 60;
		totalSeconds %= 60;

		// Generate the new time
		if (newMinutes < 10)
			time += "0";
		time += newMinutes+":";
		if (newSeconds < 10)
			time += "0";
		time += newSeconds+" / ";
		if (totalMinutes < 10)
			time += "0";
		time += totalMinutes+":";
		if (totalSeconds < 10)
			time += "0";
		time += totalSeconds;

		// Update the text in the text view
		timeView.setText(time);
	}

	@Override
	public void onStartTrackingTouch(SeekBar seeker) {
		// Don't update while the user is touching the bar
		touchActive = true;
		handler.removeMessages(SHOW_PROGRESS);
	}

	@Override
	public void onStopTrackingTouch(SeekBar seeker) {		
		// Seek to the new location
		player.seekTo(seeker.getProgress());
		
		// Touch is not active anymore
		touchActive = false;
		handler.sendEmptyMessage(SHOW_PROGRESS);
	}

	@Override
	public void onCompletion(MediaPlayer arg0) {
		// Stop updating seeker
		handler.removeMessages(SHOW_PROGRESS);
		playpause.setText("Play");
		
		// Set seeker to start
		seeker.setProgress(0);
	}
}
