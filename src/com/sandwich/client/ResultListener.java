package com.sandwich.client;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import com.sandwich.client.PeerSet.Peer;

public interface ResultListener {
	public void foundResult(String query, Result result);
	
	public void searchComplete(String query, Peer peer);
	
	public class Result {
		public String result;
		public List<Peer> peers;
		public int checksum;
		public long size;
		
		public Result(Peer peer, String result, long size, int checksum)
		{
			this.peers = new ArrayList<Peer>();
			this.result = result;
			this.checksum = checksum;
			this.size = size;
			
			// We have just one peer now
			peers.add(peer);
		}
		
		public void addPeer(Peer peer)
		{
			peers.add(peer);
		}
		
		public void addPeers(List<Peer> peers)
		{
			this.peers.addAll(peers);
		}
		
		public Iterator<Peer> getPeerIterator()
		{
			return peers.iterator();
		}
		
		public String toString()
		{
			return result;
		}
		
		@Override
		public int hashCode()
		{
			return result.hashCode();
		}
		
		@Override
		public boolean equals(Object o)
		{
			if (o instanceof Result)
			{
				Result other = (Result)o;
				
				// Check the checksum first because it's less expensive
				if (other.checksum != checksum)
					return false;
				
				// Now try the size
				if (other.size != size)
					return false;
				
				// Finally the file name if all else fails
				if (!other.result.equalsIgnoreCase(result))
					return false;
				
				// It's the same result
				return true;
			}
			else
			{
				// This isn't even a result...
				return false;
			}
		}
	}
}
