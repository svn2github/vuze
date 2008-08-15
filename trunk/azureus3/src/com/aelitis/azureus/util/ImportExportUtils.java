package com.aelitis.azureus.util;

import java.io.IOException;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.gudy.azureus2.core3.util.UrlUtils;
import org.json.simple.JSONArray;

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
	
	public final static void
	exportJSONString(
		Map		map,
		String	key,
		String	value )
	
		throws IOException
	{
		if ( value != null ){
	
			map.put( key, value );
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
	
	public final static void
	exportJSONBoolean(
		Map		map,
		String	key,
		boolean	value )
	
		throws IOException
	{
		map.put( key, new Boolean( value ));
	}
	
	public static final String
	importURL(
		Map		map,
		String	key )
	
		throws IOException
	{
		String url = importString( map, key );
		
		if ( url != null ){
			
			url = url.trim();
			
			if ( url.length() == 0 ){
				
				url = null;
				
			}else{
				
				url = URLDecoder.decode( url, "UTF-8" );
			}
		}
		
		return( url );
	}
	
	public final static void
	exportURL(
		Map		map,
		String	key,
		String	value )
	
		throws IOException
	{
		exportString( map, key, value );
	}
	
	public final static void
	exportJSONURL(
		Map		map,
		String	key,
		String	value )
	
		throws IOException
	{
		exportJSONString( map, key, UrlUtils.encode( value ));
	}
	
	public static final String[]
	importStringArray(
		Map		map,
		String	key )
	
		throws IOException
	{
		List	list = (List)map.get( key );
		
		if ( list == null ){
			
			return( new String[0] );
		}
		
		String[]	res = new String[list.size()];
		
		for (int i=0;i<res.length;i++){
			
			Object obj = list.get(i);
			
			if ( obj instanceof String ){
				
				res[i] = (String)obj;
				
			}else if ( obj instanceof byte[] ){
				
				res[i] = new String((byte[])obj, "UTF-8" );
			}
		}
		
		return( res );
	}
	
	public static final void
	exportStringArray(
		Map			map,
		String		key,
		String[]	data )
	
		throws IOException
	{
		List	l = new ArrayList(data.length);
		
		map.put( key, l );
		
		for (int i=0;i<data.length;i++){
			
			l.add( data[i].getBytes( "UTF-8" ));
		}
	}
	
	public static final void
	exportJSONStringArray(
		Map			map,
		String		key,
		String[]	data )
	
		throws IOException
	{
		List	l = new JSONArray(data.length);
		
		map.put( key, l );
		
		for (int i=0;i<data.length;i++){
			
			l.add( data[i] );
		}
	}
}
