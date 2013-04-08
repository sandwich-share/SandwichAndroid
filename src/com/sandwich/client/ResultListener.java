package com.sandwich.client;

public interface ResultListener {
	public void foundResult(String query, Result result);
	
	public void searchFailed(String query, String peer, Exception e);
	
	public class Result {
		public String peer;
		public String result;
		
		public Result(String peer, String result)
		{
			this.peer = peer;
			this.result = result;
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
