package com.aelitis.azureus.core.metasearch;


public interface ResultListener {
	
	public void resultsReceived(Engine engine,Result[] results);
	
	public void resultsComplete(Engine engine);

}
