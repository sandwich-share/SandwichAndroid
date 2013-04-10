package com.sandwich.client;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public interface ResultListener {
	public void foundResult(String query, Result result);
	
	public void searchFailed(String query, String peer, Exception e);
	
	public void searchComplete(String query, String peer);
	
	public class Result {
		public String result;
		public List<String> peers;
		public int checksum;
		
		public Result(String peer, String result, int checksum)
		{
			this.peers = new ArrayList<String>();
			this.result = result;
			this.checksum = checksum;
			
			// We have just one peer now
			peers.add(peer);
		}
		
		public void addPeer(String peer)
		{
			peers.add(peer);
		}
		
		public void addPeers(List<String> peers)
		{
			this.peers.addAll(peers);
		}
		
		public Iterator<String> getPeerIterator()
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
				
				// Check the file name first
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
