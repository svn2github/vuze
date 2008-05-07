package com.aelitis.azureus.core.metasearch;

import java.io.IOException;
import java.util.Map;


public interface 
Engine 
{
	public static final int ENGINE_TYPE_REGEX		= 1;
	public static final int ENGINE_TYPE_JSON		= 2;
	
	
	public int getType();
	
	public Result[] search(SearchParameter[] searchParameters) throws SearchException;
	
	public String getName();
	
	public long getId();
	
	public long getLastUpdated();
	
	public String getIcon();

	public Map exportToBencodedMap() throws IOException;
}
