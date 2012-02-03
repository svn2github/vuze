/*
 * Created on Feb 3, 2012
 * Created by Paul Gardner
 * 
 * Copyright 2012 Vuze, Inc.  All rights reserved.
 * 
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; version 2 of the License only.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307, USA.
 */


package com.aelitis.azureus.core.speedmanager;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.gudy.azureus2.core3.category.Category;
import org.gudy.azureus2.core3.category.CategoryManager;
import org.gudy.azureus2.core3.config.COConfigurationManager;
import org.gudy.azureus2.core3.config.impl.TransferSpeedValidator;
import org.gudy.azureus2.core3.download.DownloadManager;
import org.gudy.azureus2.core3.global.GlobalManager;
import org.gudy.azureus2.core3.torrent.TOTorrent;
import org.gudy.azureus2.core3.util.BEncoder;
import org.gudy.azureus2.core3.util.Base32;
import org.gudy.azureus2.core3.util.DisplayFormatters;
import org.gudy.azureus2.core3.util.FileUtil;
import org.gudy.azureus2.core3.util.HashWrapper;

import com.aelitis.azureus.core.AzureusCore;

public class 
SpeedLimitHandler 
{
	private static SpeedLimitHandler		singleton;
	
	public static SpeedLimitHandler
	getSingleton(
		AzureusCore		core )
	{
		synchronized( SpeedLimitHandler.class ){
			
			if ( singleton == null ){
				
				singleton = new SpeedLimitHandler( core );
			}
			
			return( singleton );
		}
	}
	
	private AzureusCore		core;
	
	private
	SpeedLimitHandler(
		AzureusCore		_core )
	{
		core 	= _core;
	}
	
	private synchronized Map
	loadConfig()
	{
		return( BEncoder.cloneMap( COConfigurationManager.getMapParameter( "speed.limit.handler.state", new HashMap())));
	}
	
	private synchronized void
	saveConfig(
		Map		map )
	{
		COConfigurationManager.setParameter( "speed.limit.handler.state", map );
		
		COConfigurationManager.save();
	}
	
	public List<String>
	reset()
	{
		LimitDetails details = new LimitDetails();
		
		details.loadForReset();
		
		details.apply();
		
		return( details.getString());
	}
	
	public List<String>
	getCurrent()
	{
		LimitDetails details = new LimitDetails();
		
		details.loadCurrent();
		
		return( details.getString());
	}
	
	public List<String>
	getProfileNames()
	{
		Map	map = loadConfig();
				
		List<String> profiles = new ArrayList<String>();
		
		List<Map> list = (List<Map>)map.get( "profiles" );

		if ( list != null ){
			
			for ( Map m: list ){
				
				String	name = importString( m, "n" );
				
				if ( name != null ){
					
					profiles.add( name );
				}
			}
		}

		return( profiles );
	}
	
	public List<String>
	loadProfile(
		String		name )
	{		
		Map	map = loadConfig();
				
		List<Map> list = (List<Map>)map.get( "profiles" );

		if ( list != null ){
			
			for ( Map m: list ){
				
				String	p_name = importString( m, "n" );
				
				if ( p_name != null && name.equals( p_name )){
					
					Map profile = (Map)m.get( "p" );
					
					LimitDetails ld = new LimitDetails( profile );
					
					ld.apply();
					
					return( ld.getString());
				}
			}
		}
		
		List<String> result = new ArrayList<String>();
		
		result.add( "Profile not found" );

		return( result );
	}
	
	public List<String>
	getProfile(
		String		name )
	{
		Map	map = loadConfig();
		
		List<Map> list = (List<Map>)map.get( "profiles" );

		if ( list != null ){
			
			for ( Map m: list ){
				
				String	p_name = importString( m, "n" );
				
				if ( p_name != null && name.equals( p_name )){
					
					Map profile = (Map)m.get( "p" );
					
					LimitDetails ld = new LimitDetails( profile );
										
					return( ld.getString());
				}
			}
		}
		
		List<String> result = new ArrayList<String>();
		
		result.add( "Profile not found" );

		return( result );
	}
	
	public void
	deleteProfile(
		String		name )
	{
		Map	map = loadConfig();
		
		List<Map> list = (List<Map>)map.get( "profiles" );

		if ( list != null ){
			
			for ( Map m: list ){
				
				String	p_name = importString( m, "n" );
				
				if ( p_name != null && name.equals( p_name )){
					
					list.remove( m );
					
					saveConfig( map );
					
					return;
				}
			}
		}
	}
	
	public List<String>
	saveProfile(
		String		name )
	{
		LimitDetails details = new LimitDetails();
		
		details.loadCurrent();
		
		Map	map = loadConfig();
		
		List<Map> list = (List<Map>)map.get( "profiles" );

		if ( list == null ){
			
			list = new ArrayList<Map>();
			
			map.put( "profiles", list );
		}
		
		for ( Map m: list ){
			
			String	p_name = importString( m, "n" );
			
			if ( p_name != null && name.equals( p_name )){
				
				list.remove( m );
				
				break;
			}
		}
		
		Map m = new HashMap();
		
		list.add( m );
		
		m.put( "n", name );
		m.put( "p", details.export());
		
		saveConfig( map );
		
		return( details.getString());
	}
	
	private String
	formatUp(
		int	rate )
	{
		return( "Up=" + format( rate ));
	}
	
	private String
	formatDown(
		int	rate )
	{
		return( "Down=" + format( rate ));
	}
	
	private String
	format(
		int		rate )
	{
		if ( rate < 0 ){
			
			return( "Disabled" );
			
		}else if ( rate == 0 ){
			
			return( "Unlimited" );
			
		}else{
			
			return( DisplayFormatters.formatByteCountToKiBEtcPerSec( rate ));
		}
	}
	
    private void
    exportBoolean(
    	Map<String,Object>	map,
    	String				key,
    	boolean				b )
    {
    	map.put( key, new Long(b?1:0));
    }
    
    private boolean
    importBoolean(
    	Map<String,Object>	map,
    	String				key )
    {
    	Long	l = (Long)map.get( key );
    	
    	if ( l != null ){
    		
    		return( l == 1 );
    	}
    	
    	return( false );
    }
    
    private void
    exportInt(
    	Map<String,Object>	map,
    	String				key,
    	int					i )
    {
    	map.put( key, new Long( i ));
    }
    
    private int
    importInt(
    	Map<String,Object>	map,
    	String				key )
    {
    	Long	l = (Long)map.get( key );
    	
    	if ( l != null ){
    		
    		return( l.intValue());
    	}
    	
    	return( 0 );
    }
    
    private void
    exportString(
    	Map<String,Object>	map,
    	String				key,
    	String				s )
    {
    	try{
    		map.put( key, s.getBytes( "UTF-8" ));
    		
    	}catch( Throwable e ){
    	}
    }
    
    private String
    importString(
    	Map<String,Object>	map,
    	String				key )
    {
       	Object obj= map.get( key );
       	
       	if ( obj instanceof String ){
       		
       		return((String)obj);
       		
       	}else if ( obj instanceof byte[] ){
       	
    		try{
    			return( new String((byte[])obj, "UTF-8" ));
    			
    		}catch( Throwable e ){
	    	}
       	}
       	
    	return( null );
    }
    
	private class
	LimitDetails
	{
	    private boolean		auto_up_enabled;
	    private boolean		auto_up_seeding_enabled;
	    private boolean		seeding_limits_enabled;
	    private int			up_limit;
	    private int			up_seeding_limit;
	    private int			down_limit;
	    
	    private Map<String,int[]>	download_limits = new HashMap<String, int[]>();
	    private Map<String,int[]>	category_limits = new HashMap<String, int[]>();
	    
	    private 
	    LimitDetails()
	    {	
	    }
	    
	    private 
	    LimitDetails(
	    	Map<String,Object>		map )
	    {
	    	auto_up_enabled 		= importBoolean( map, "aue" );
	    	auto_up_seeding_enabled	= importBoolean( map, "ause" );
	    	seeding_limits_enabled	= importBoolean( map, "sle" );
	    	
	    	up_limit			= importInt( map, "ul" );
	    	up_seeding_limit	= importInt( map, "usl" );
	    	down_limit			= importInt( map, "dl" );
	    	
	    	List<Map<String,Object>>	d_list = (List<Map<String,Object>>)map.get( "dms" );
	    	
	    	if ( d_list != null ){
	    		
	    		for ( Map<String,Object> m: d_list ){
	    			
	    			String	k = importString( m, "k" );
	    			
	    			if ( k != null ){
	    				
	    				int	ul = importInt( m, "u" );
	    				int	dl = importInt( m, "d" );
	    				
	    				download_limits.put( k, new int[]{ ul, dl });
	    			}
	    		}
	    	}
	    	
	    	List<Map<String,Object>>	c_list = (List<Map<String,Object>>)map.get( "cts" );
	    	
	    	if ( c_list != null ){
	    		
	    		for ( Map<String,Object> m: c_list ){
	    			
	    			String	k = importString( m, "k" );
	    			
	    			if ( k != null ){
	    				
	    				int	ul = importInt( m, "u" );
	    				int	dl = importInt( m, "d" );
	    				
	    				category_limits.put( k, new int[]{ ul, dl });
	    			}
	    		}
	    	}
	    }
	    
	    private Map<String,Object>
	    export()
	    {
	    	Map<String,Object>	map = new HashMap<String, Object>();
	    	
	    	exportBoolean( map, "aue", auto_up_enabled );
	    	exportBoolean( map, "ause", auto_up_seeding_enabled );
	    	exportBoolean( map, "sle", seeding_limits_enabled );
	    	
	    	exportInt( map, "ul", up_limit );
	    	exportInt( map, "usl", up_seeding_limit );
	    	exportInt( map, "dl", down_limit );
	    	
	    	List<Map<String,Object>>	d_list = new ArrayList<Map<String,Object>>();
	    	
	    	map.put( "dms", d_list );
	    	
	    	for ( Map.Entry<String,int[]> entry: download_limits.entrySet()){
	    		
	    		Map<String,Object> m = new HashMap<String,Object>();
	    		
	    		d_list.add( m );
	    		
	    		exportString( m, "k", entry.getKey());
	    		exportInt( m, "u", entry.getValue()[0]);
	    		exportInt( m, "d", entry.getValue()[1]);
	    	}
	    	
	    	List<Map<String,Object>>	c_list = new ArrayList<Map<String,Object>>();
	    	
	    	map.put( "cts", c_list );
	    	
	    	for ( Map.Entry<String,int[]> entry: category_limits.entrySet()){
	    		
	    		Map<String,Object> m = new HashMap<String,Object>();
	    		
	    		c_list.add( m );
	    		
	    		exportString( m, "k", entry.getKey());
	    		exportInt( m, "u", entry.getValue()[0]);
	    		exportInt( m, "d", entry.getValue()[1]);
	    	}
	    	
	    	return( map );
	    }
	    
	    private void
	    loadForReset()
	    {
	    		// just maintain the auto upload setting over a reset
	    	
		    auto_up_enabled 		= COConfigurationManager.getBooleanParameter( TransferSpeedValidator.AUTO_UPLOAD_ENABLED_CONFIGKEY );
	    }
	    
	    private void
	    loadCurrent()
	    {
		    auto_up_enabled 		= COConfigurationManager.getBooleanParameter( TransferSpeedValidator.AUTO_UPLOAD_ENABLED_CONFIGKEY );
		    auto_up_seeding_enabled = COConfigurationManager.getBooleanParameter( TransferSpeedValidator.AUTO_UPLOAD_SEEDING_ENABLED_CONFIGKEY );
		    seeding_limits_enabled 	= COConfigurationManager.getBooleanParameter( TransferSpeedValidator.UPLOAD_SEEDING_ENABLED_CONFIGKEY );
		    up_limit 				= COConfigurationManager.getIntParameter( TransferSpeedValidator.UPLOAD_CONFIGKEY );
		    up_seeding_limit 		= COConfigurationManager.getIntParameter( TransferSpeedValidator.UPLOAD_SEEDING_CONFIGKEY );
		    down_limit				= COConfigurationManager.getIntParameter( TransferSpeedValidator.DOWNLOAD_CONFIGKEY );
		
		    download_limits.clear();
		    
			GlobalManager gm = core.getGlobalManager();

			List<DownloadManager>	downloads = gm.getDownloadManagers();
			
			for ( DownloadManager download: downloads ){
				
				TOTorrent torrent = download.getTorrent();
				
				byte[]	hash = null;
				
				if ( torrent!= null ){
					
					try{
						hash = torrent.getHash();
						
					}catch( Throwable e ){
						
					}
				}
				
				if ( hash != null ){
					int	download_up_limit 	= download.getStats().getUploadRateLimitBytesPerSecond();
					int	download_down_limit = download.getStats().getDownloadRateLimitBytesPerSecond();
					
			    	if ( download_up_limit > 0 || download_down_limit > 0 ){
			    		
			    		download_limits.put( Base32.encode( hash ), new int[]{ download_up_limit, download_down_limit });
			    	}
				}
			}
		    
			Category[] categories = CategoryManager.getCategories();
		 
			category_limits.clear();
			
		    for ( Category category: categories ){
		    	
		    	int	cat_up_limit	 	= category.getUploadSpeed();
		    	int	cat_down_limit 		= category.getDownloadSpeed();
		    	
		    	if ( cat_up_limit > 0 || cat_down_limit > 0 ){
		    	
		    		category_limits.put( category.getName(), new int[]{ cat_up_limit, cat_down_limit });
		    	}
		    }
	    }
	    
	    private void
	    apply()
	    {			    		
	    		// don't manage this properly because the speedmanager has a 'memory' of 
    			// the last upload limit in force before it became active and we're
    			// not persisting this... rare use case methinks anyway

    		COConfigurationManager.setParameter( TransferSpeedValidator.AUTO_UPLOAD_ENABLED_CONFIGKEY, auto_up_enabled );
	    	COConfigurationManager.setParameter( TransferSpeedValidator.AUTO_UPLOAD_SEEDING_ENABLED_CONFIGKEY, auto_up_seeding_enabled );

    		if ( !( auto_up_enabled || auto_up_seeding_enabled )){
      				
 		     	COConfigurationManager.setParameter( TransferSpeedValidator.UPLOAD_CONFIGKEY, up_limit );
    		}
    		
		    COConfigurationManager.setParameter( TransferSpeedValidator.UPLOAD_SEEDING_ENABLED_CONFIGKEY, seeding_limits_enabled );
		    COConfigurationManager.setParameter( TransferSpeedValidator.UPLOAD_SEEDING_CONFIGKEY, up_seeding_limit );

		    COConfigurationManager.setParameter( TransferSpeedValidator.DOWNLOAD_CONFIGKEY, down_limit );

			GlobalManager gm = core.getGlobalManager();

			Set<DownloadManager>	all_managers = new HashSet<DownloadManager>( gm.getDownloadManagers());
			
			for ( Map.Entry<String,int[]> entry: download_limits.entrySet()){
				
				byte[] hash = Base32.decode( entry.getKey());
				
				DownloadManager dm = gm.getDownloadManager( new HashWrapper( hash ));
			
				if ( dm != null ){
						
					int[]	limits = entry.getValue();
					
					dm.getStats().setUploadRateLimitBytesPerSecond( limits[0] );
					dm.getStats().setDownloadRateLimitBytesPerSecond( limits[1] );
					
					all_managers.remove( dm );
				}
			}

			for ( DownloadManager dm: all_managers ){
				
				dm.getStats().setUploadRateLimitBytesPerSecond( 0 );
				dm.getStats().setDownloadRateLimitBytesPerSecond( 0 );
			}
			
			Set<Category> all_categories = new HashSet<Category>( Arrays.asList(CategoryManager.getCategories()));
			 
			Map<String, Category> cat_map = new HashMap<String, Category>();
			
			for ( Category c: all_categories ){
				
				cat_map.put( c.getName(), c );
			}
								
			for ( Map.Entry<String,int[]> entry: category_limits.entrySet()){
		    	
		    	String cat_name = entry.getKey();
		    	
		    	Category category = cat_map.get( cat_name );
		    	
		    	if ( category != null ){
		    		
		    		int[]	limits = entry.getValue();
		    		
		    		category.setUploadSpeed( limits[0] );
		    		category.setDownloadSpeed( limits[1] );
		    		
		    		all_categories.remove( category );
		    	}
			}
			
			for ( Category category: all_categories ){
				
	    		category.setUploadSpeed( 0 );
	    		category.setDownloadSpeed( 0 );
			}
	    }
	    
	    private List<String>
	    getString()
	    {
			List<String> result = new ArrayList<String>();
			
			result.add( "Global Limits" );
				    	    
		    if ( auto_up_enabled ){
		    	
			    result.add( "    Auto upload limit enabled" );
			    
		    }else if ( auto_up_seeding_enabled ){
		    				    	
		    	result.add( "    Auto upload seeding limit enabled" );

		    }else{
		    	
		    	result.add( "    " + formatUp( up_limit*1024 ));

	    		if ( seeding_limits_enabled ){

	    			result.add( "    Seeding only limit enabled" );
		    		
		    		result.add( "    Seeding only: " + format( up_seeding_limit*1024 ));
		    	}
		    }

		    
		    result.add( "    " + formatDown( down_limit*1024 ));

		    result.add( "Download Limits" );
		    
		    int	total_download_limits = 0;
		    
			GlobalManager gm = core.getGlobalManager();

			for ( Map.Entry<String,int[]> entry: download_limits.entrySet()){
				
				byte[] hash = Base32.decode( entry.getKey());
				
				DownloadManager dm = gm.getDownloadManager( new HashWrapper( hash ));
			
				if ( dm != null ){
						
					int[]	limits = entry.getValue();
					
		    		total_download_limits++;
		    		
		    		result.add( "    " + dm.getDisplayName() + ": " + formatUp( limits[0] ) + ", " + formatDown( limits[1] ));
		    	}
			}
			
		    if ( total_download_limits == 0 ){
		    	result.add( "    None" );
		    }
		    
			Category[] categories = CategoryManager.getCategories();
		 
			Map<String, Category> cat_map = new HashMap<String, Category>();
			
			for ( Category c: categories ){
				
				cat_map.put( c.getName(), c );
			}
			
			result.add( "Category Limits" );
			
			int	total_cat_limits = 0;
			
			for ( Map.Entry<String,int[]> entry: category_limits.entrySet()){
		    	
		    	String cat_name = entry.getKey();
		    	
		    	Category category = cat_map.get( cat_name );
		    	
		    	if ( category != null ){
		    		
		    		if ( category.getType() == Category.TYPE_UNCATEGORIZED ){
		    			
		    			cat_name = "Uncategorised";
		    		}
		    		
					int[]	limits = entry.getValue();
		    		total_cat_limits++;
		    		
		    		result.add( "    " + cat_name + ": " + formatUp( limits[0] ) + ", " + formatDown( limits[1] ));
		    	}
		    }
		    
		    if ( total_cat_limits == 0 ){
		    	result.add( "    None" );
		    }
		    
			return( result );
	    }
	    
	}
}
