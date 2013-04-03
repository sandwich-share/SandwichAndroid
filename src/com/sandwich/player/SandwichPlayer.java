package com.sandwich.player;

import java.io.IOException;

import android.net.Uri;

public interface SandwichPlayer {
	public void initialize(Uri path) throws IllegalArgumentException, SecurityException, IllegalStateException, IOException;
	
	public void start();
	
	public void stop();
	
	public void release();
}
