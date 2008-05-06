package com.aelitis.azureus.core.metasearch;

public interface MetaSearch {
	
	public void search(ResultListener listener,final SearchParameter[] searchParameters);
	
	public Engine[] getEngines();
	
	public void addEngine( Engine engine);
	
	public void removeEngine(Engine engine );
	
}
