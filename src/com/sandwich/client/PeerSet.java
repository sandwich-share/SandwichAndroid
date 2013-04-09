package com.sandwich.client;

import java.util.HashMap;
import java.util.Iterator;

public class PeerSet {
	private HashMap<Peer, Peer> peerSet;
	
	public PeerSet()
	{
		peerSet = new HashMap<Peer, Peer>();
	}
	
	public synchronized void updatePeerSet(PeerSet peers)
	{
		// Clear peer set first
		peerSet.clear();
		
		// Add a copy of each peer into our peer set
		for (Peer p : peers.peerSet.values())
		{
			updatePeer(new Peer(p));
		}
	}
	
	public synchronized void updatePeer(Peer p)
	{
		Peer oldPeer = peerSet.get(p);
		if (oldPeer != null)
		{
			// Update an existing peer
			oldPeer.indexHash = p.indexHash;
			oldPeer.timestamp = p.timestamp;
		}
		else
		{
			// Otherwise add a new one
			peerSet.put(p, p);
			p.setPeerSet(this);
		}
	}
	
	public synchronized void updatePeer(String ip, String timestamp, long indexHash)
	{
		updatePeer(new Peer(ip, timestamp, indexHash));
	}
	
	public synchronized boolean removePeer(String ip)
	{
		for (Peer p : peerSet.values())
			if (p.getIpAddress().equals(ip))
				return removePeer(p);
		
		return false;
	}
	
	public synchronized boolean removePeer(Peer p)
	{
		if (peerSet.remove(p) != null)
		{
			p.setPeerSet(null);
			return true;
		}

		return false;
	}
	
	public int getPeerListLength()
	{
		return peerSet.size();
	}
	
	public Iterator<Peer> getPeerListIterator()
	{
		return peerSet.values().iterator();
	}
	
	public class Peer {
		private String ip;
		private long indexHash;
		private PeerSet peerSet;
		private String timestamp;
		
		public Peer(String ip, String timestamp, long indexHash)
		{
			this.ip = ip;
			this.indexHash = indexHash;
			this.peerSet = null;
			this.timestamp = timestamp;
		}
		
		public Peer(Peer p)
		{
			this.ip = p.ip;
			this.indexHash = p.indexHash;
			this.peerSet = null;
		}
		
		private void setPeerSet(PeerSet peerSet)
		{
			this.peerSet = peerSet;
		}
		
		public PeerSet getPeerSet()
		{
			return peerSet;
		}
		
		public boolean remove()
		{
			if (peerSet != null)
				return peerSet.removePeer(this);
			else
				return false;
		}
		
		public String getIpAddress()
		{
			return ip;
		}
		
		public long getIndexHash()
		{
			return indexHash;
		}
		
		public String getTimestamp()
		{
			return timestamp;
		}
		
		public void updateIndexHash(long newHash)
		{
			indexHash = newHash;
		}
		
		@Override
		public boolean equals(Object o)
		{
			if (!(o instanceof Peer))
				return false;
			
			Peer p = (Peer)o;
			
			// The IP address should be unique
			return p.getIpAddress().equals(getIpAddress());
		}
		
		@Override
		public int hashCode()
		{
			// The IP address is the unique portion
			return getIpAddress().hashCode();
		}
	}
}
