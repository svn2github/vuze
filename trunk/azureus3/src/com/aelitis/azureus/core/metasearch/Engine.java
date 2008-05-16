package com.aelitis.azureus.core.metasearch;

import java.io.IOException;
import java.util.Map;


public interface 
Engine 
{
	public static final int ENGINE_TYPE_REGEX		= 1;
	public static final int ENGINE_TYPE_JSON		= 2;
	
	public static final int	ENGINE_SOURCE_UNKNOWN				= 0;
	public static final int	ENGINE_SOURCE_VUZE					= 1;
	public static final int	ENGINE_SOURCE_LOCAL					= 2;
	
	public static final int	SEL_STATE_DESELECTED			= 0;
	public static final int	SEL_STATE_AUTO_SELECTED			= 1;
	public static final int	SEL_STATE_MANUAL_SELECTED		= 2;
	
		/**
		 * don't change these as they are externalised
		 */
	public static final String[] ENGINE_SOURCE_STRS = { "unknown","vuze","local","unused","unused" };
	public static final String[] SEL_STATE_STRINGS	= { "no", "auto", "manual" };
	
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
	
	public int
	getSelectionState();
	
	public void
	setSelectionState(
		int			state );
	
	public void
	checkSelectionStateRecorded();
		
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
