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

package org.gudy.azureus2.pluginsimpl.update.sf.impl2;

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

public class 
SFPluginDetailsLoaderImpl 
	implements SFPluginDetailsLoader, ResourceDownloaderListener
{

	public static final String	site_prefix = "http://azureus.sourceforge.net/";
	public static final String	page_url 	= site_prefix + "update/pluginlist.php?type=release";

	protected static SFPluginDetailsLoaderImpl		singleton;
	
	
	public static synchronized SFPluginDetailsLoader
	getSingleton()
	{
		if ( singleton == null ){
			
			singleton	= new SFPluginDetailsLoaderImpl();
		}
		
		return( singleton );
	}
	
	protected boolean	plugin_names_loaded;
	
	protected List		plugin_names;
	protected Map		plugin_map;
	
	protected List		listeners			= new ArrayList();
	
	protected ResourceDownloaderFactory rd_factory = ResourceDownloaderFactoryImpl.getSingleton();

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
			
			Properties	details = new Properties();
			
			details.load( dl.download());
			
			Iterator it = details.keySet().iterator();
			
			while( it.hasNext()){
				
				String	plugin_name 	= (String)it.next();
				String	version			= (String)details.get(plugin_name);

				plugin_names.add( plugin_name );
				
				plugin_map.put(plugin_name.toLowerCase(), 
				               new SFPluginDetailsImpl( this, plugin_name, version ));
			}
			
			plugin_names_loaded	= true;
			
		}catch( Throwable e ){
			
			e.printStackTrace();
			
			throw( new SFPluginDetailsException( "Plugin list load failed", e ));
		}
	}
	
	protected void
	loadPluginDetails(
		SFPluginDetailsImpl		details )
	
		throws SFPluginDetailsException
	{
		try{
			ResourceDownloader p_dl = rd_factory.create( new URL( site_prefix + "plugin_details.php?plugin=" + details.getName() ));
		
			p_dl = rd_factory.getRetryDownloader( p_dl, 5 );
		
			p_dl.addListener( this );

			HTMLPage	plugin_page = HTMLPageFactory.loadPage( p_dl.download());
			
			if ( !processPluginPage( details, plugin_page )){
							
				throw( new SFPluginDetailsException( "Plugin details load fails for '" + details.getName() + "': data not found" ));
			}
					
		}catch( Throwable e ){
			
			e.printStackTrace();
			
			throw( new SFPluginDetailsException( "Plugin details load fails", e ));
		}
	}
	
	protected boolean
	processPluginPage(
		SFPluginDetailsImpl		details,
		HTMLPage				page )
	
		throws SFPluginDetailsException
	{
		HTMLTable[]	tables = page.getTables();
		
		// dumpTables("", tables );
		
		return( processPluginPage( details, tables ));
	}
	
	protected boolean
	processPluginPage(
		SFPluginDetailsImpl		details,
		HTMLTable[]				tables )
	
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
					
					details.setDetails(
									plugin_name,
									plugin_version,
									plugin_download,
									plugin_auth,
									plugin_cvs_version,
									plugin_cvs_download,
									rows[6].getCells()[0].getContent(),
									rows[9].getCells()[0].getContent());
					
					return( true );
				}
			}
			
			HTMLTable[]	sub_tables = table.getTables();
			
			boolean	res = processPluginPage( details, sub_tables );
			
			if( res ){
				
				return( res );
			}
		}
		
		return( false );
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
	
	public synchronized String[]
	getPluginNames()
		
		throws SFPluginDetailsException
	{
		if ( !plugin_names_loaded ){
			
			loadPluginList();
		}
		
		String[]	res = new String[plugin_names.size()];
		
		plugin_names.toArray( res );
		
		return( res );
	}
	
	public synchronized SFPluginDetails
	getPluginDetails(
		String		name )
	
		throws SFPluginDetailsException
	{
			// make sure details are loaded
		
		getPluginNames();
		
		SFPluginDetails details = (SFPluginDetails)plugin_map.get(name.toLowerCase()); 
		
		if ( details == null ){
			
			throw( new SFPluginDetailsException( "Plugin '" + name + "' not found" ));
		}
		
		return( details );
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
	
	public synchronized void
	reset()
	{
		plugin_names_loaded	= false;
		
		plugin_names		= new ArrayList();
		plugin_map			= new HashMap();
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
