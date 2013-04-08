package com.sandwich.client;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public interface ResultListener {
	public void foundResult(String query, Result result);
	
	public void searchFailed(String query, String peer, Exception e);
	
	public class Result {
		public String result;
		public List<String> peers;
		
		public Result(String peer, String result)
		{
			this.peers = new ArrayList<String>();
			this.result = result;
			
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
			// Checks only for result equality. The peer doesn't matter for us.
			return ((o instanceof Result) && ((Result)o).result.equals(result));
		}
	}
}
