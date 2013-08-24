package com.sandwich.client;

import android.content.Context;

import com.sandwich.ClientUiBinding;

public final class ClientManager {
	
	private static ClientUiBinding binding;
	private static int refCount;
	
	public static synchronized ClientUiBinding getClientBinding(Context appContext)
	{
		if (binding == null)
		{
			// Setup the global client object and register it with each activity
			binding = new ClientUiBinding(appContext);
	        binding.initialize();
	        refCount = 1;
		}
		else
		{
			refCount++;
		}
		
		return binding;
	}
	
	public static synchronized void freeClientBinding()
	{
		refCount--;
		
		if (refCount == 0)
		{
			binding.release();
			binding = null;
		}
	}
}
