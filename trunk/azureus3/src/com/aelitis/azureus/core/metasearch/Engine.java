package com.aelitis.azureus.core.metasearch;


public interface Engine {
	
	public Result[] search(SearchParameter[] searchParameters);
	
	public String getName();
	
	public long getId();
	
	public String getIcon();

}
