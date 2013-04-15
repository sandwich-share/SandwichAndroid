package com.sandwich;


import com.sandwich.R;
import com.sandwich.player.SandwichPlayer;
import com.sandwich.ui.Dialog;

import android.net.Uri;
import android.os.Bundle;
import android.view.Window;
import android.view.WindowManager;
import android.app.Activity;

public class VideoPlayer extends Activity {
	private SandwichPlayer player;
	
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        
        // Full screen, no title
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
            WindowManager.LayoutParams.FLAG_FULLSCREEN);
        
        setContentView(R.layout.activity_video_player);

		player = new com.sandwich.player.VideoPlayer(this);
		Bundle extras = getIntent().getExtras();
		try {
			player.initialize(Uri.parse(extras.getString("URL")));
			player.start();
		} catch (Exception e) {
			Dialog.displayDialog(this, "Video Playback Error", e.getMessage(), true);
			
			if (player != null) {
				player.release();
				player = null;
			}
		}
    }
    
    @Override
    protected void onPause()
    {
    	super.onPause();
    	if (player != null) {
    		player.pause();
    	}
    }
    
    @Override
    protected void onResume()
    {
    	super.onResume();
    	if (player != null) {
    		player.resume();
    	}
    }
    
    @Override
    protected void onDestroy()
    {
    	super.onDestroy();
    	
    	Dialog.closeDialogs();
    	
    	if (player != null)
    		player.release();
     }
}
