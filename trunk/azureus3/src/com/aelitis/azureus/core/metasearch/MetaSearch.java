package com.aelitis.azureus.core.metasearch;

public interface 
MetaSearch 
{	
	public void 
	search(
		ResultListener 		listener,
		SearchParameter[] 	searchParameters );
	
	public Engine[] 
	getEngines(
		boolean		active_only );
	
	public Engine
	getEngine(
		long		id );
	
	public void 
	addEngine( 
		Engine 		engine );
	
	public void 
	removeEngine(
		Engine 		engine );
	
}
