package com.aelitis.azureus.core.metasearch;


public interface ResultListener {
	
	public void contentReceived(Engine engine, String content );
	
	public void matchFound( Engine engine, String[] fields );
	
	public void resultsReceived(Engine engine,Result[] results);
	
	public void resultsComplete(Engine engine);
	
	public void engineFailed(Engine engine, Throwable cause );

}
