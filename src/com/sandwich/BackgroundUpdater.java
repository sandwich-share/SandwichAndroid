package com.sandwich;

import java.util.Timer;
import java.util.TimerTask;

public class BackgroundUpdater {
	private ClientUiBinding client;
	private int interval;
	private Timer timer;
	
	public BackgroundUpdater(ClientUiBinding client, int interval) {
		this.client = client;
		this.interval = interval;
	}
	
	public void start() {
		if (timer != null) {
			stop();
		}

		timer = new Timer();
		
		// Schedule the task for now and every interval milliseconds
		timer.schedule(new TimerTask() {
			@Override
			public void run() {
				client.bootstrap();
			}
		}, 0, interval);
	}
	
	public void stop() {
		timer.cancel();
		timer.purge();
		timer = null;
	}
}
