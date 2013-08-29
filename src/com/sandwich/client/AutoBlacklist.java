package com.sandwich.client;

import java.util.Calendar;
import java.util.LinkedList;
import java.util.List;

public class AutoBlacklist {
	private List<Peer> blacklist;
	
	final static int BLACKLIST_SECONDS = 300;
	
	public AutoBlacklist()
	{
		blacklist = new LinkedList<Peer>();
	}
	
	public void addBlacklistedPeer(PeerSet.Peer peer)
	{	
		Calendar dueDate = Calendar.getInstance();
		dueDate.add(Calendar.SECOND, AutoBlacklist.BLACKLIST_SECONDS);
		System.out.println("Blacklisting peer: "+peer);
		blacklist.add(new Peer(peer, dueDate));
	}
	
	public void clear()
	{
		blacklist.clear();
	}
	
	public boolean isPeerBlacklisted(PeerSet.Peer peer)
	{
		removeTimedOutEntries();
		
		for (Peer p : blacklist)
		{
			if (p.peer.equals(peer))
				return true;
		}
		return false;
	}
	
	private void removeTimedOutEntries()
	{
		Calendar currentDate = Calendar.getInstance();
		LinkedList<Peer> peersToRemove = new LinkedList<Peer>();
		
		for (Peer p : blacklist)
		{
			if (p.getDueTime().before(currentDate))
			{
				System.out.println("Peer's blacklisting timed out: "+p.peer);
				peersToRemove.add(p);
			}
		}
		
		blacklist.removeAll(peersToRemove);
	}
	
	class Peer implements Comparable<Peer> {
		private PeerSet.Peer peer;
		private Calendar dueTime;
		
		public Peer(PeerSet.Peer peer, Calendar dueTime)
		{
			this.peer = peer;
			this.dueTime = dueTime;
		}
		
		public Calendar getDueTime()
		{
			return dueTime;
		}
		
		@Override
		public int compareTo(Peer otherPeer)
		{
			return otherPeer.peer.compareTo(peer);
		}
		
		@Override
		public boolean equals(Object obj)
		{
			if (!(obj instanceof Peer))
				return false;
			
			return ((Peer)obj).peer.equals(peer);
		}
	}
}
