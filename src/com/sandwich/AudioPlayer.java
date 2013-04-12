package com.sandwich;


import com.sandwich.R;
import com.sandwich.player.SandwichPlayer;

import android.net.Uri;
import android.os.Bundle;
import android.app.Activity;
import android.content.Intent;

public class AudioPlayer extends Activity {
	private static SandwichPlayer player;
	
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_audio_player);
        
        // Start the player
        onNewIntent(null);
    }
    
    @Override
    protected void onNewIntent(Intent intent)
    {
    	// Distinguish between a call from onCreate() and a normal call
    	if (intent == null) {
    		intent = getIntent();
    	} else {
        	super.onNewIntent(intent);
    	}
    	
    	Bundle extras = intent.getExtras();
    	
    	// If there are no extras, this isn't the intent we're looking for
    	if (extras == null)
    		return;
    	
    	// Close an existing player
    	killPlayer();
    	
    	// Create a new player
		player = new com.sandwich.player.AudioPlayer(this);
		try {
			player.initialize(Uri.parse(extras.getString("URL")));
			player.start();
		} catch (Exception e) {
			e.printStackTrace();
			Dialog.displayDialog(this, "Audio Playback Error", e.getMessage(), true);
		}
    }
    
    @Override
    public void onBackPressed()
    {
		Intent i = new Intent(this, com.sandwich.Search.class);
		startActivity(i);
		
		if (!player.isPlaying()) {
			super.onBackPressed();
		}
    }
    
    private void killPlayer()
    {
    	Dialog.closeDialogs();
    	
    	if (player != null) {
    		player.release();
    		player = null;
    	}
    }
    
    @Override
    protected void onDestroy()
    {
    	super.onDestroy();
    	
    	killPlayer();
    }
}
