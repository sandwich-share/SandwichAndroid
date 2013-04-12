package com.sandwich.client;

import java.util.ArrayList;
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
		// Mark old peers
		for (Peer p : peerSet.values())
		{
			p.tagged = true;
		}
		
		// Add a copy of each peer into our peer set
		for (Peer p : peers.peerSet.values())
		{
			updatePeer(new Peer(p));
		}
		
		// Add peers that weren't updated to reaper list
		ArrayList<Peer> peersToReap = new ArrayList<Peer>();
		for (Peer p : peerSet.values())
		{
			if (p.tagged)
				peersToReap.add(p);
		}
		
		// Reap peers that weren't updated
		for (Peer p : peersToReap)
		{
			removePeer(p);
		}
	}

	private synchronized void updatePeer(Peer p)
	{
		Peer oldPeer = peerSet.get(p);
		if (oldPeer != null)
		{
			// Update an existing peer
			oldPeer.indexHash = p.indexHash;
			oldPeer.timestamp = p.timestamp;
			if (p.version != Peer.VERSION_UNSPECIFIED)
				oldPeer.version = p.version;
		}
		else
		{
			// Otherwise add a new one
			peerSet.put(p, p);
			p.setPeerSet(this);
		}
	}
	
	public synchronized void updatePeer(String ip, String timestamp, long indexHash, int version)
	{
		updatePeer(new Peer(ip, timestamp, indexHash, version));
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
		private int version;
		private boolean tagged;
		
		public final static int VERSION_UNSPECIFIED = 0;
		public final static int VERSION_1_0 = 1;
		public final static int VERSION_1_1 = 2;
		
		public Peer(String ip, String timestamp, long indexHash, int version)
		{
			this.ip = ip;
			this.indexHash = indexHash;
			this.peerSet = null;
			this.timestamp = timestamp;
			this.version = version;
		}
		
		public Peer(Peer p)
		{
			this.ip = p.ip;
			this.indexHash = p.indexHash;
			this.peerSet = null;
			this.version = p.version;
		}
		
		public int getVersion()
		{
			return version;
		}
		
		public void setVersion(int version)
		{
			this.version = version;
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
