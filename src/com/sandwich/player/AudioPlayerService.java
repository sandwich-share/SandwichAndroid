package com.sandwich.player;

import java.io.IOException;
import java.util.HashMap;

import com.sandwich.Dialog;
import com.sandwich.R;
import com.sandwich.SpinnerDialog;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.BitmapFactory;
import android.media.AudioManager;
import android.media.MediaMetadataRetriever;
import android.media.MediaPlayer;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.net.wifi.WifiManager;
import android.os.Binder;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.PowerManager;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;

@SuppressLint("HandlerLeak")
public class AudioPlayerService extends Service implements MediaPlayer.OnErrorListener,
	MediaPlayer.OnPreparedListener,OnClickListener,OnSeekBarChangeListener,MediaPlayer.OnCompletionListener,
	AudioManager.OnAudioFocusChangeListener,Runnable  {

	private Activity activity;
	private MediaPlayer player;
	private Button playpause;
	private SeekBar seeker;
	private TextView timeView;
	private SpinnerDialog waitDialog;
	
	private AudioManager am;
	
	private Uri filePath;

	boolean touchActive = false;
	
	private Thread metadataThread;
	
	private PendingIntent pi;
	
	private WifiManager.WifiLock wlanLock;
	
	private String metastring = "";
	private byte[] albumart = null;
	
	private Handler handler;
	private IntentFilter noisyIntents = new IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY);
	
	final static int SHOW_PROGRESS = 12813;
	final static int NOTIFICATION_ID = 12131;
	
	final static int RW_FF_INTERVAL = 750;
	
	final AudioBinder binder = new AudioBinder(this);
	
	final private AudioEventReceiver audioEventReceiver = new AudioEventReceiver();

	public class AudioBinder extends Binder {
		private AudioPlayerService service;
		
		public AudioBinder(AudioPlayerService service)
		{
			this.service = service;
		}
		
		public void initialize(Activity activity, Uri filePath) throws IllegalArgumentException, SecurityException, IllegalStateException, IOException
		{
			service.activity = activity;
			service.filePath = filePath;

			service.seeker = (SeekBar)activity.findViewById(R.id.seekBar);
			service.playpause = (Button)activity.findViewById(R.id.playButton);
			service.timeView = (TextView)activity.findViewById(R.id.timeView);
			
			TextView metaText = (TextView)activity.findViewById(R.id.songText);
			metaText.setText("");
			ImageView artView = (ImageView)activity.findViewById(R.id.albumArt);
			artView.setImageBitmap(null);
			
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
			
			player = new MediaPlayer();
			
			service.am = (AudioManager)activity.getSystemService(Context.AUDIO_SERVICE);
			
			// Initialize the audio event receiver
			AudioEventReceiver.setPlayer(this);
			
			// Add listeners
			service.playpause.setOnClickListener(service);
			service.seeker.setOnSeekBarChangeListener(service);
			
			// Configure media player
			service.player.setDataSource(activity.getApplicationContext(), filePath);
			service.player.setAudioStreamType(AudioManager.STREAM_MUSIC);
			service.player.setOnErrorListener(service);
			service.player.setOnPreparedListener(service);
			service.player.setOnCompletionListener(service);
			service.player.setWakeMode(activity, PowerManager.PARTIAL_WAKE_LOCK);

			// We want this activity's audio controls to change the music volume
			activity.setVolumeControlStream(AudioManager.STREAM_MUSIC);
		}
		
		public void fastForward()
		{
			player.seekTo(player.getCurrentPosition() + RW_FF_INTERVAL);
		}
		
		public void rewind()
		{
			player.seekTo(player.getCurrentPosition() - RW_FF_INTERVAL);
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
				int res = am.requestAudioFocus(service, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN);
				if (res == AudioManager.AUDIOFOCUS_REQUEST_GRANTED)
					onAudioFocusChange(AudioManager.AUDIOFOCUS_GAIN);
			}
		}
		
		public boolean isPlaying()
		{
			return player.isPlaying();
		}
		
		public void pause()
		{
			// Abandon audio focus
			if (player.isPlaying())
			{
				am.abandonAudioFocus(service);
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
			
			// Drop audio focus and WLAN lock then stop
			am.abandonAudioFocus(service);
			if (player.isPlaying())
			{
				onAudioFocusChange(AudioManager.AUDIOFOCUS_LOSS);
				player.stop();
			}
			
			// Stop progress bar updates
			handler.removeMessages(SHOW_PROGRESS);
		}
		
		public void playpause()
		{
			// Emulate click of play/pause button
			service.onClick(null);
		}
		
		public void release()
		{
			// Stop the player first
			stop();
			
			// Delete the notification
			service.stopForeground(true);
			
			// Stop the metadata thread
			if (metadataThread != null)
			{
				try {
					metadataThread.join();
				} catch (InterruptedException e) { }
				metadataThread = null;
			}
			
			// Release the audio event receiver
			AudioEventReceiver.clearPlayer();

			// Release the player
			player.release();
			player = null;
		}
    }

	@Override
	public boolean onError(MediaPlayer mp, int what, int extra) {
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
	
	@TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
	@SuppressWarnings("deprecation")
	@Override
	public void run()
	{
		String artist;
		String title;
		
		// Pull metadata for this file
		MediaMetadataRetriever metagetter = new MediaMetadataRetriever();
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH)
			metagetter.setDataSource(filePath.toString(), new HashMap<String, String>());
		else
			metagetter.setDataSource(filePath.toString());

		artist = metagetter.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST);
		title = metagetter.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE);
		albumart = metagetter.getEmbeddedPicture();
		if (artist == null || title == null)
			return;
		
		Notification notification = new Notification();
		notification.tickerText = "Sandwich Audio Player";
		notification.icon = R.drawable.ic_launcher;
		notification.flags |= Notification.FLAG_ONGOING_EVENT | Notification.FLAG_ONLY_ALERT_ONCE;
		notification.setLatestEventInfo(activity.getApplicationContext(), "Sandwich Audio Player",
                artist + " - " + title, pi);
		startForeground(NOTIFICATION_ID, notification);
		
		metastring = artist + " - " + title;
	}

	@TargetApi(Build.VERSION_CODES.HONEYCOMB_MR1)
	@Override
	@SuppressWarnings("deprecation")
	public void onPrepared(MediaPlayer mp)
	{
		// Dismiss the wait dialog
		if (waitDialog != null)
		{
			waitDialog.dismiss();
			waitDialog = null;
		}
		
		WifiManager wm = (WifiManager) activity.getSystemService(Context.WIFI_SERVICE);
		
		// Create a WLAN lock to maintain wireless capability during playback (not acquired yet)
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR1)
			wlanLock = wm.createWifiLock(WifiManager.WIFI_MODE_FULL_HIGH_PERF, "Sandwich Audio Player");
		else
			wlanLock = wm.createWifiLock(WifiManager.WIFI_MODE_FULL, "Sandwich Audio Player");
		
		pi = PendingIntent.getActivity(activity.getApplicationContext(), 0,
                new Intent(activity.getApplicationContext(), com.sandwich.AudioPlayer.class),
                PendingIntent.FLAG_UPDATE_CURRENT);
		Notification notification = new Notification();
		notification.tickerText = "Sandwich Audio Player";
		notification.icon = R.drawable.ic_launcher;
		notification.flags |= Notification.FLAG_ONGOING_EVENT | Notification.FLAG_ONLY_ALERT_ONCE;
		notification.setLatestEventInfo(activity.getApplicationContext(), "Sandwich Audio Player",
                "", pi);
		startForeground(NOTIFICATION_ID, notification);

		// Register for media button events
		am.registerMediaButtonEventReceiver(new ComponentName(activity, AudioEventReceiver.class));
		
		// Setup the progress bar with the duration of the media
		seeker.setMax(player.getDuration());
		seeker.setProgress(0);
		player.seekTo(0);
		
		// Request audio focus
		int res = am.requestAudioFocus(this, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN);
		if (res == AudioManager.AUDIOFOCUS_REQUEST_GRANTED)
			onAudioFocusChange(AudioManager.AUDIOFOCUS_GAIN);
		
		// If we're API level 10+, we have a metadata retreiver that we'll spin off to pull down
		// album art, artist, and song title. It will also update our notification using the pending
		// intent to include the new metadata.
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.GINGERBREAD_MR1)
		{
			metadataThread = new Thread(this);
			metadataThread.start();
		}
	}

	@Override
	public void onClick(View target) {
		// We get multiple button events here so we need to classify it
		if (target == null || target == playpause)
		{
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
		
		// Update the metadata
		if (metastring != null) {
			TextView metaText = (TextView)activity.findViewById(R.id.songText);
			metaText.setText(metastring);
			metastring = null;
		}
		if (albumart != null) {
			ImageView artView = (ImageView)activity.findViewById(R.id.albumArt);
			artView.setImageBitmap(BitmapFactory.decodeByteArray(albumart, 0, albumart.length));
			albumart = null;
		}
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
		
		// Release the network lock
		releaseNetworkUseLock();
		
		// Set seeker to start
		seeker.setProgress(0);
		
		// Give up audio focus
		am.abandonAudioFocus(this);
	}
	
	private void acquireNetworkUseLock()
	{
		ConnectivityManager cm = (ConnectivityManager) activity.getSystemService(Context.CONNECTIVITY_SERVICE); 
		NetworkInfo activeNetwork;
		
		// Check if our active network is WLAN. If so, we want to get a lock so the device doesn't kill it on us.
		activeNetwork = cm.getActiveNetworkInfo();
		if (activeNetwork != null && activeNetwork.getType() == ConnectivityManager.TYPE_WIFI)
		{	
			// Acquire the lock
			System.out.println("Acquiring WLAN lock");
			wlanLock.acquire();
		}
	}
	
	private void releaseNetworkUseLock()
	{
		// Release the WLAN lock if we have it
		if (wlanLock.isHeld())
		{
			System.out.println("Releasing WLAN lock");
			wlanLock.release();
		}
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
				releaseNetworkUseLock();
				handler.removeMessages(SHOW_PROGRESS);
				playpause.setText("Play");
			}
			break;
			
		case AudioManager.AUDIOFOCUS_GAIN:
			if (!player.isPlaying())
			{
				// Start playing
				acquireNetworkUseLock();
				player.start();
				handler.sendEmptyMessage(SHOW_PROGRESS);
				playpause.setText("Pause");
				activity.registerReceiver(audioEventReceiver, noisyIntents);
			}
			break;
		}
	}

	@Override
	public IBinder onBind(Intent arg0) {
		return binder;
	}
}