package com.sandwich;


import com.sandwich.R;
import com.sandwich.player.SandwichPlayer;

import android.net.Uri;
import android.os.Bundle;
import android.app.Activity;

public class AudioPlayer extends Activity {
	private SandwichPlayer player;
	
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_audio_player);
		getActionBar().setDisplayHomeAsUpEnabled(true);
		
		player = new com.sandwich.player.AudioPlayer(this);
		Bundle extras = getIntent().getExtras();
		try {
			player.initialize(Uri.parse(extras.getString("URL")));
			player.start();
		} catch (Exception e) {
			e.printStackTrace();
			Dialog.displayDialog(this, "Audio Playback Error", e.getMessage(), true);
		}
    }
    
    @Override
    protected void onDestroy()
    {
    	super.onDestroy();
    	
    	Dialog.closeDialogs();
    	
    	if (player != null)
    		player.release();

		getActionBar().setDisplayHomeAsUpEnabled(false);
    }
}
