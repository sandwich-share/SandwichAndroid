package com.sandwich.player;

import java.io.IOException;

import com.sandwich.R;
import com.sandwich.SpinnerDialog;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Service;
import android.content.ComponentName;
import android.content.Intent;
import android.content.IntentFilter;
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
	MediaPlayer.OnPreparedListener,OnClickListener,OnSeekBarChangeListener,MediaPlayer.OnCompletionListener,
	AudioManager.OnAudioFocusChangeListener  {

	private Activity activity;
	private MediaPlayer player;
	private Button playpause;
	private SeekBar seeker;
	private TextView timeView;
	private SpinnerDialog waitDialog;
	
	private AudioManager am;

	boolean touchActive = false;
	
	private Handler handler;
	private IntentFilter noisyIntents = new IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY);
	
	private AudioEventReceiver audioEventReceiver = new AudioEventReceiver(this);
	
	final static int SHOW_PROGRESS = 12813;
	
	public AudioPlayer(Activity activity)
	{
		this.activity = activity;

		this.seeker = (SeekBar)activity.findViewById(R.id.seekBar);
		this.playpause = (Button)activity.findViewById(R.id.playButton);
		this.timeView = (TextView)activity.findViewById(R.id.timeView);
		
		handler = new Handler()
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
	}
	
	public void initialize(Uri filePath) throws IllegalArgumentException, SecurityException, IllegalStateException, IOException
	{
		player = new MediaPlayer();
		
		this.am = (AudioManager)activity.getSystemService(AUDIO_SERVICE);
		
		// Add listeners
		playpause.setOnClickListener(this);
		seeker.setOnSeekBarChangeListener(this);
		
		// Configure media player
		player.setDataSource(activity.getApplicationContext(), filePath);
		player.setAudioStreamType(AudioManager.STREAM_MUSIC);
		player.setOnErrorListener(this);
		player.setOnPreparedListener(this);
		player.setOnCompletionListener(this);

		// We want this activity's audio controls to change the music volume
		activity.setVolumeControlStream(AudioManager.STREAM_MUSIC);
	}
	
	public void start()
	{
		// Prepare and start asynchronously
		waitDialog = SpinnerDialog.displayDialog(activity, "Please Wait", "Loading Media", true);
		player.prepareAsync();
	}
	
	public void play()
	{
		if (!player.isPlaying())
		{
			// Request audio focus
			int res = am.requestAudioFocus(this, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN);
			onAudioFocusChange(res);
		}
	}
	
	public void pause()
	{
		// Abandon audio focus
		if (player.isPlaying())
		{
			am.abandonAudioFocus(this);
			onAudioFocusChange(AudioManager.AUDIOFOCUS_LOSS);
		}
	}
	
	public void stop()
	{
		// Unregister our media button receiver
		am.unregisterMediaButtonEventReceiver(new ComponentName(activity, AudioEventReceiver.class));
		
		// Dismiss the wait dialog
		if (waitDialog != null)
		{
			waitDialog.dismiss();
			waitDialog = null;
		}
		
		// Drop audio focus and stop
		if (player.isPlaying())
		{
			am.abandonAudioFocus(this);
			onAudioFocusChange(AudioManager.AUDIOFOCUS_LOSS);
			player.stop();
		}
		
		// Stop progress bar updates
		handler.removeMessages(SHOW_PROGRESS);
	}
	
	public void release()
	{
		// Stop the player first
		stop();

		// Release the player
		player.release();
		player = null;
	}

	@Override
	public boolean onError(MediaPlayer mp, int what, int extra) {
		// Dismiss the wait dialog
		if (waitDialog != null)
		{
			waitDialog.dismiss();
			waitDialog = null;
		}
		
		// Close the player
		activity.finish();
		return true;
	}

	@Override
	public IBinder onBind(Intent arg0) {
		return null;
	}

	@Override
	public void onPrepared(MediaPlayer mp)
	{
		// Dismiss the wait dialog
		if (waitDialog != null)
		{
			waitDialog.dismiss();
			waitDialog = null;
		}
		
		// Register for media button events
		am.registerMediaButtonEventReceiver(new ComponentName(activity, AudioEventReceiver.class));
		
		// Setup the progress bar with the duration of the media
		seeker.setMax(player.getDuration());
		seeker.setProgress(0);
		player.seekTo(0);
		
		// Request audio focus
		int res = am.requestAudioFocus(this, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN);
		onAudioFocusChange(res);
	}

	@Override
	// Called when play/pause button is clicked
	public void onClick(View target) {
		// If it's playing, pause it. If it's paused, play it.
		if (player.isPlaying())
		{
			am.abandonAudioFocus(this);
			onAudioFocusChange(AudioManager.AUDIOFOCUS_LOSS);
		}
		else
		{
			// Request audio focus
			int res = am.requestAudioFocus(this, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN);
			onAudioFocusChange(res);
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
		
		// Unregister audio events
		activity.unregisterReceiver(audioEventReceiver);
		
		// Set seeker to start
		seeker.setProgress(0);
		
		// Give up audio focus
		am.abandonAudioFocus(this);
	}

	@Override
	public void onAudioFocusChange(int focusChange) {
		// Check if we're losing focus
		switch (focusChange)
		{
		case AudioManager.AUDIOFOCUS_LOSS:
		case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT:
		case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK:
			// Stop playing
			if (player.isPlaying())
			{
				activity.unregisterReceiver(audioEventReceiver);
				player.pause();
				handler.removeMessages(SHOW_PROGRESS);
				playpause.setText("Play");
			}
			break;
			
		case AudioManager.AUDIOFOCUS_GAIN:
			if (!player.isPlaying())
			{
				// Start playing
				player.start();
				handler.sendEmptyMessage(SHOW_PROGRESS);
				playpause.setText("Pause");
				activity.registerReceiver(audioEventReceiver, noisyIntents);
			}
			break;
		}
	}
}
