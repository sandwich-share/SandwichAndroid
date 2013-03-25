package com.sandwich;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;

import android.content.Context;

import com.sandwich.client.Client;

public class JankyThread implements Runnable {
	Context context;
	
	public JankyThread(Context context)
	{
		this.context = context;
	}
	
	
	@Override
	public void run() {
		Client c = new Client(context);
		
		try {
			c.bootstrap(new URL("http://anandtech.com/"));
		} catch (MalformedURLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

}
