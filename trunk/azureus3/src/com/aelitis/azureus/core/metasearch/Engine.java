package com.aelitis.azureus.core.metasearch;

import java.io.IOException;
import java.util.Map;


public interface 
Engine 
{
	public static final int ENGINE_TYPE_REGEX		= 1;
	public static final int ENGINE_TYPE_JSON		= 2;
	
	public static final int	ENGINE_SOURCE_UNKNOWN	= 0;
	public static final int	ENGINE_SOURCE_FEATURED	= 1;
	public static final int	ENGINE_SOURCE_POPULAR	= 2;
	public static final int	ENGINE_SOURCE_MANUAL	= 3;
	
		/**
		 * don't change these as they are externalised
		 */
	public static final String[] ENGINE_SOURCE_STRS = {"unknown","feat","pop","local" };
	
	public int getType();
	
	public Result[] 
	search(
		SearchParameter[] searchParameters ) 
	
		throws SearchException;
	
	public String 
	getName();
	
	public long 
	getId();
	
	public long 
	getLastUpdated();
	
	public String 
	getIcon();

	public boolean
	isActive();
	
	public boolean
	isSelected();
	
	public void
	setSelected(
		boolean		selected );
	
	public boolean
	isSelectionStateRecorded();
	
	public void
	setSelectionStateRecorded();
	
	public int
	getSource();
	
	public void
	setSource(
		int		source );
	
	public Map 
	exportToBencodedMap() 
	
		throws IOException;
	
	public String
	getString();
}
