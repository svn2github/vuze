package com.aelitis.azureus.core.metasearch;

public interface MetaSearch {
	
	public void search(ResultListener listener,final SearchParameter[] searchParameters);
	
	public Engine[] getEngines();
	
	public void addEngine(int id);
	
	public void removeEngine(int id);
	
}
