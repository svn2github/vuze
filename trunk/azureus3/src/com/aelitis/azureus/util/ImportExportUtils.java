package com.aelitis.azureus.util;

import java.io.IOException;
import java.util.Map;

public final class ImportExportUtils {
	
	public final static void
	exportString(
		Map		map,
		String	key,
		String	value )
	
		throws IOException
	{
		if ( value != null ){
	
			map.put( key, value.getBytes( "UTF-8" ));
		}
	}
	
	public final static String
	importString(
		Map		map,
		String	key )
	
		throws IOException
	{
		if ( map == null ){
			
			return( null );
		}
		
		Object	obj = map.get( key );
		
		if ( obj instanceof String ){
			
			return((String)obj);
			
		}else if ( obj instanceof byte[]){
			
			return( new String((byte[])obj, "UTF-8" ));
		}
		
		return( null );
	}
	
	public final static long
	importLong(
		Map		map,
		String	key )
	
		throws IOException
	{
		return( importLong( map, key, 0 ));
	}
	
	public final static long
	importLong(
		Map		map,
		String	key,
		long	def )
	
		throws IOException
	{
		if ( map == null ){
			
			return( def );
		}
		
		Object	obj = map.get( key );
		
		if ( obj instanceof Long){
			
			return(((Long)obj).longValue());
			
		}else if ( obj instanceof String ){
			
			return( Long.parseLong((String)obj));
		}
		
		return( def );
	}

	public final static void
	exportBoolean(
		Map		map,
		String	key,
		boolean	value )
	
		throws IOException
	{
		map.put( key, new Long( value?1:0 ));
	}
	
	public final static boolean
	importBoolean(
		Map		map,
		String	key )
	
		throws IOException
	{
		return( importBoolean( map, key, false ));
	}
	
	public final static boolean
	importBoolean(
		Map		map,
		String	key,
		boolean	def )
	
		throws IOException
	{
		if ( map == null ){
			
			return( def );
		}
		
		Object	obj = map.get( key );
		
		if ( obj instanceof Long){
			
			return(((Long)obj).longValue() == 1 );
			
		}else if ( obj instanceof Boolean ){
			
			return(((Boolean)obj).booleanValue());
		}
		
		return( def );
	}
}
