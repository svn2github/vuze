/*
 * Created on 27-Apr-2004
 * Created by Paul Gardner
 * Copyright (C) 2004 Aelitis, All Rights Reserved.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 * 
 * AELITIS, SARL au capital de 30,000 euros
 * 8 Allee Lenotre, La Grille Royale, 78600 Le Mesnil le Roi, France.
 *
 */

package org.gudy.azureus2.pluginsimpl.update.sf.impl;

/**
 * @author parg
 *
 */

import java.util.*;
import java.net.URL;
import java.io.InputStream;

import org.gudy.azureus2.plugins.utils.resourcedownloader.*;
import org.gudy.azureus2.pluginsimpl.update.sf.*;
import org.gudy.azureus2.pluginsimpl.local.utils.resourcedownloader.*;

import org.gudy.azureus2.core3.html.*;
import org.gudy.azureus2.core3.util.*;

public class 
SFPluginDetailsLoaderImpl 
	implements SFPluginDetailsLoader, ResourceDownloaderListener
{

	public static final String	site_prefix = "http://azureus.sourceforge.net/";
	
	public static final String	page_url 	= site_prefix + "plugin_list.php";

	protected static SFPluginDetailsLoaderImpl		singleton;
  	protected static AEMonitor		class_mon		= new AEMonitor( "SFPluginDetailsLoader:class" );

	
	public static SFPluginDetailsLoader
	getSingleton()
	{
		try{
			class_mon.enter();
		
			if ( singleton == null ){
			
				singleton	= new SFPluginDetailsLoaderImpl();
			}
		
			return( singleton );
			
		}finally{
			
			class_mon.exit();
		}
	}
	
	protected boolean	plugin_names_loaded;
	
	protected List		plugin_names;
	protected Map		plugin_map;
	
	protected List		listeners			= new ArrayList();
	
	protected ResourceDownloaderFactory rd_factory = ResourceDownloaderFactoryImpl.getSingleton();

  	protected AEMonitor		this_mon		= new AEMonitor( "SFPluginDetailsLoader" );

	protected
	SFPluginDetailsLoaderImpl()
	{
		reset();
	}
	
	protected void
	loadPluginList()
	
		throws SFPluginDetailsException
	{
		try{
			ResourceDownloader dl = rd_factory.create( new URL(page_url));
			
			dl = rd_factory.getRetryDownloader( dl, 5 );
			
			dl.addListener( this );
			
			HTMLPage	page = HTMLPageFactory.loadPage( dl.download());
			
			String[]	links = page.getLinks();
						
			for (int i=0;i<links.length;i++){
				
				String	link = links[i];
				
				if ( link.startsWith("plugin_details.php?plugin=" )){
	
					String	plugin_name = link.substring( 26 );

					plugin_names.add( plugin_name );
				}					
			}
			
			plugin_names_loaded	= true;
			
		}catch( Throwable e ){
			
			e.printStackTrace();
			
			throw( new SFPluginDetailsException( "Plugin list load failed", e ));
		}
	}
	
	protected SFPluginDetailsImpl
	loadPluginDetails(
		String		plugin_name )
	
		throws SFPluginDetailsException
	{
		try{
			ResourceDownloader p_dl = rd_factory.create( new URL( site_prefix + "plugin_details.php?plugin=" + plugin_name ));
		
			p_dl = rd_factory.getRetryDownloader( p_dl, 5 );
		
			p_dl.addListener( this );

			HTMLPage	plugin_page = HTMLPageFactory.loadPage( p_dl.download());
			
			SFPluginDetailsImpl res = processPluginPage( plugin_name, plugin_page );
			
			if ( res == null ){
				
				throw( new SFPluginDetailsException( "Plugin details load fails for '" + plugin_name + "': data not found" ));
			}
			
			return( res );
			
		}catch( Throwable e ){
			
			e.printStackTrace();
			
			throw( new SFPluginDetailsException( "Plugin details load fails", e ));
		}
	}
	
	protected SFPluginDetailsImpl
	processPluginPage(
		String			name,
		HTMLPage		page )
	
		throws SFPluginDetailsException
	{
		HTMLTable[]	tables = page.getTables();
		
		// dumpTables("", tables );
		
		return( processPluginPage( name, tables ));
	}
	
	protected SFPluginDetailsImpl
	processPluginPage(
		String			name,
		HTMLTable[]		tables )
	
		throws SFPluginDetailsException
	{
		for (int i=0;i<tables.length;i++){
			
			HTMLTable	table = tables[i];
			
			HTMLTableRow[]	rows = table.getRows();
		
			if ( rows.length == 10 ){
				
				HTMLTableCell[]	cells = rows[0].getCells();
				
				if ( cells.length == 6 &&
						cells[0].getContent().trim().equals("Name") &&
						cells[5].getContent().trim().equals("Contact")){
				
					
					// got the plugin details table
				
					HTMLTableCell[]	detail_cells = rows[2].getCells();
					
					String	plugin_name			= detail_cells[0].getContent();
					String	plugin_version		= detail_cells[1].getContent();
					String	plugin_auth			= detail_cells[4].getContent();
					
					String[]	dl_links = detail_cells[2].getLinks();
					
					String	plugin_download;
					
					if ( dl_links.length == 0 ){
						
						plugin_download	= "<unknown>";
						
					}else{
						
						plugin_download = site_prefix + dl_links[0];
					}
					
					HTMLTableCell[]	cvs_detail_cells = rows[3].getCells();

					String	plugin_cvs_version		= cvs_detail_cells[1].getContent();

					String[]	cvs_dl_links 		= cvs_detail_cells[2].getLinks();
					
					String	plugin_cvs_download;
					
					if ( cvs_dl_links.length == 0 ){
						
						plugin_cvs_download	= "<unknown>";
						
					}else{
						
						plugin_cvs_download = site_prefix + cvs_dl_links[0];
					}

					// System.out.println( "got plugin:" + plugin_name + "/" + plugin_version + "/" + plugin_download + "/" + plugin_auth );
					
					return(	new SFPluginDetailsImpl(
									plugin_name,
									plugin_version,
									plugin_download,
									plugin_auth,
									plugin_cvs_version,
									plugin_cvs_download,
									rows[6].getCells()[0].getContent(),
									rows[9].getCells()[0].getContent()));							
				}
			}
			
			HTMLTable[]	sub_tables = table.getTables();
			
			SFPluginDetailsImpl	res = processPluginPage( name, sub_tables );
			
			if( res != null ){
				
				return( res );
			}
		}
		
		return( null );
	}
	
	protected void
	dumpTables(
		String			indent,
		HTMLTable[]		tables )
	{
		for (int i=0;i<tables.length;i++){
			
			HTMLTable	tab = tables[i];
			
			System.out.println( indent + "tab:" + tab.getContent());
			
			HTMLTableRow[] rows = tab.getRows();
			
			for (int j=0;j<rows.length;j++){
				
				HTMLTableRow	row = rows[j];
				
				System.out.println( indent + "  row[" + j + "]: " + rows[j].getContent());
				
				HTMLTableCell[]	cells = row.getCells();
				
				for (int k=0;k<cells.length;k++){
					
					System.out.println( indent + "    cell[" + k + "]: " + cells[k].getContent());
					
				}
			}
			
			dumpTables( indent + "  ", tab.getTables());
		}
	}
	
	public String[]
	getPluginNames()
		
		throws SFPluginDetailsException
	{
		try{
			this_mon.enter();
		
			if ( !plugin_names_loaded ){
				
				loadPluginList();
			}
			
			String[]	res = new String[plugin_names.size()];
			
			plugin_names.toArray( res );
			
			return( res );
			
		}finally{
			
			this_mon.exit();
		}
	}
	
	public SFPluginDetails
	getPluginDetails(
		String		name )
	
		throws SFPluginDetailsException
	{
		try{
			this_mon.enter();
		
			SFPluginDetails details = (SFPluginDetails)plugin_map.get(name); 
			
			if ( details == null ){
				
				details = loadPluginDetails( name );
				
				plugin_map.put( name, details );
			}
			
			return( details );
			
		}finally{
			
			this_mon.exit();
		}
	}
	
	public SFPluginDetails[]
	getPluginDetails()
	
		throws SFPluginDetailsException	
	{
		String[]	plugin_names = getPluginNames();
		
		SFPluginDetails[]	res = new SFPluginDetails[plugin_names.length];
	
		for (int i=0;i<plugin_names.length;i++){
			
			res[i] = getPluginDetails(plugin_names[i]);
		}
		
		return( res );
	}
	
	public void
	reportPercentComplete(
		ResourceDownloader	downloader,
		int					percentage )
	{
	}
	
	public void
	reportActivity(
		ResourceDownloader	downloader,
		String				activity )
	{
		informListeners( activity );
	}
	
	public boolean
	completed(
		ResourceDownloader	downloader,
		InputStream			data )
	{
		return( true );
	}
	
	public void
	failed(
		ResourceDownloader			downloader,
		ResourceDownloaderException e )
	{
		informListeners( "Error: " + e.getMessage());
	}

	protected void
	informListeners(
		String		log )
	{
		for (int i=0;i<listeners.size();i++){
			
			((SFPluginDetailsLoaderListener)listeners.get(i)).log( log );
		}
	}
	
	public void
	reset()
	{
		try{
			this_mon.enter();
		
			plugin_names_loaded	= false;
			
			plugin_names		= new ArrayList();
			plugin_map			= new HashMap();
			
		}finally{
			
			this_mon.exit();
		}
	}
	
	public void
	addListener(
		SFPluginDetailsLoaderListener		l )
	{
		listeners.add( l );
	}
	
	public void
	removeListener(
		SFPluginDetailsLoaderListener		l )
	{
		listeners.remove(l);
	}
}
