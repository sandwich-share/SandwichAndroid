package com.sandwich.client;

import java.util.HashSet;
import java.util.Iterator;

public class PeerSet {
	private HashSet<Peer> peerSet;
	
	public PeerSet()
	{
		peerSet = new HashSet<Peer>();
	}
	
	public synchronized void updatePeerSet(PeerSet peers)
	{
		peerSet.clear();
		peerSet.addAll(peers.peerSet);
	}
	
	public synchronized boolean addPeer(String ip, String timestamp, long indexHash)
	{
		Peer p = new Peer(ip, timestamp, indexHash);

		if (peerSet.add(p))
		{
			p.setPeerSet(this);
			return true;
		}
		
		return false;
	}
	
	public synchronized boolean removePeer(String ip)
	{
		for (Peer p : peerSet)
			if (p.getIpAddress().equals(ip))
				return removePeer(p);
		
		return false;
	}
	
	public synchronized boolean removePeer(Peer p)
	{
		if (peerSet.remove(p))
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
		return peerSet.iterator();
	}
	
	public class Peer {
		private String ip;
		private long indexHash;
		private PeerSet peerSet;
		
		public Peer(String ip, String timestamp, long indexHash)
		{
			this.ip = ip;
			this.indexHash = indexHash;
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
