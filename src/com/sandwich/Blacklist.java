package com.sandwich;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import android.content.Context;
import android.content.SharedPreferences;

public class Blacklist {
	private HashSet<String> list;
	private Context context;
	
	private static final String BLACKLIST_PREF = "SandwichBlacklist";
	private static final String BLACKLIST = "Blacklist";
	
	public Blacklist(Context context)
	{
		this.context = context;
		list = new HashSet<String>();
	}
	
	public void initialize()
	{
		SharedPreferences prefs = context.getSharedPreferences(BLACKLIST_PREF, 0);
		
		// Read in the persistent preferences
		String blacklist = prefs.getString(BLACKLIST, "");
		if (blacklist.length() != 0)
		{
			String entries[] = blacklist.split(",");
			
			for (String e : entries)
				list.add(e);
		}
	}
	
	public Set<String> getBlacklistSet()
	{
		return list;
	}
	
	public void addToBlacklist(String ip)
	{
		SharedPreferences prefs = context.getSharedPreferences(BLACKLIST_PREF, 0);

		String blacklist = prefs.getString(BLACKLIST, "");
		if (blacklist.length() != 0)
		{
			blacklist += ","+ip;
		}
		else
		{
			blacklist = ip;
		}
		
		// Add to the hash set
		list.add(ip);
		
		// Commit to persistent preferences
		SharedPreferences.Editor edit = prefs.edit();
		edit.putString(BLACKLIST, blacklist);
		edit.commit();
	}
	
	public void removeFromBlacklist(String ip)
	{
		if (!list.remove(ip))
			return;
		
		SharedPreferences prefs = context.getSharedPreferences(BLACKLIST_PREF, 0);
		SharedPreferences.Editor edit = prefs.edit();
		
		Iterator<String> i = list.iterator();
		String blacklist = "";
		while (i.hasNext())
		{
			if (blacklist.length() != 0)
			{
				blacklist += ","+i.next();
			}
			else
			{
				blacklist = i.next();
			}
		}
		
		edit.putString(BLACKLIST, blacklist);
		edit.commit();
	}
	
	public boolean isBlacklisted(String ip)
	{
		return list.contains(ip);
	}
}
