package com.sandwich.client;

public interface ResultListener {
	public void foundResult(String query, String peer, String result);
	
	public void searchFailed(String query, String peer, Exception e);
}
