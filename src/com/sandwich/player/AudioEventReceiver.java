package com.sandwich.player;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.view.KeyEvent;

public class AudioEventReceiver extends BroadcastReceiver
{
	static private AudioPlayerService.AudioBinder player;
	
	public AudioEventReceiver(AudioPlayerService.AudioBinder player)
	{
		// FIXME: Janky
		AudioEventReceiver.player = player;
	}
	
	public AudioEventReceiver()
	{
		// Must have been initialized already
	}
	
	@Override
	public void onReceive(Context context, Intent intent) {
		if (intent.getAction().equals(AudioManager.ACTION_AUDIO_BECOMING_NOISY))
		{
			// Pause playback
			player.pause();
		}
		else if (Intent.ACTION_MEDIA_BUTTON.equals(intent.getAction())) {
            KeyEvent event = (KeyEvent)intent.getParcelableExtra(Intent.EXTRA_KEY_EVENT);
            switch (event.getKeyCode())
            {
            case KeyEvent.KEYCODE_MEDIA_PLAY:
            	player.play();
            	break;
            	
            case KeyEvent.KEYCODE_MEDIA_REWIND:
            	player.rewind();
            	break;
            	
            case KeyEvent.KEYCODE_MEDIA_FAST_FORWARD:
            	player.fastForward();
            	break;
            	
            case KeyEvent.KEYCODE_MEDIA_PAUSE:
            	player.pause();
            	break;
            	
            case KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE:
                player.playpause();
                break;

            default:
            	System.out.println("Unhandled media keycode: "+event.getKeyCode());
            	break;
            }
        } else {
        	System.out.println("Unhandled intent action: "+intent.getAction());
        }
	}
}
