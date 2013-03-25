package com.sandwich.client;

import java.util.HashSet;
import java.util.Iterator;

public class PeerSet {
	private HashSet<String> peerSet;
	
	public PeerSet()
	{
		peerSet = new HashSet<String>();
	}
	
	public boolean addPeerByString(String peer)
	{
		return peerSet.add(peer);
	}
	
	public int getPeerListLength()
	{
		return peerSet.size();
	}
	
	public Iterator<String> getPeerListIterator()
	{
		return peerSet.iterator();
	}
}
