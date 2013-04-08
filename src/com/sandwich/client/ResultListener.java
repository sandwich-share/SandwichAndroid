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
	}
}
