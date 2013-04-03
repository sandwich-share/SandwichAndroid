package com.sandwich;


import com.sandwich.R;
import com.sandwich.player.SandwichPlayer;

import android.net.Uri;
import android.os.Bundle;
import android.app.Activity;

public class VideoPlayer extends Activity {
	private SandwichPlayer player;
	
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_video_player);
		getActionBar().setDisplayHomeAsUpEnabled(true);
		
		// HACK: VideoView does not resize properly so disable rotation
		

		player = new com.sandwich.player.VideoPlayer(this);
		Bundle extras = getIntent().getExtras();
		try {
			player.initialize(Uri.parse(extras.getString("URL")));
			player.start();
		} catch (Exception e) {
			Dialog.displayDialog(this, "Video Playback Error", e.getMessage(), true);
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
