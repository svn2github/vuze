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

import org.gudy.azureus2.pluginsimpl.update.sf.*;
import org.gudy.azureus2.core3.resourcedownloader.*;
import org.gudy.azureus2.core3.html.*;

public class 
SFPluginDetailsLoaderImpl 
	implements SFPluginDetailsLoader
{

	public static final String	site_prefix = "http://azureus.sourceforge.net/";
	
	public static final String	page_url 	= site_prefix + "plugin_list.php";

	protected SFPluginDetailsImpl[]		plugin_details;
	
	public
	SFPluginDetailsLoaderImpl()
	{
	}
	
	public void
	load()
	{
		ResourceDownloader dl = ResourceDownloaderFactory.create( page_url );
		
		dl = ResourceDownloaderFactory.getRetryDownloader( dl, 5 );
		
		try{
			HTMLPage	page = HTMLPageFactory.loadPage( dl.download());
			
			String[]	links = page.getLinks();
			
			List	details = new ArrayList();
			
			for (int i=0;i<links.length;i++){
				
				String	link = links[i];
				
				if ( link.startsWith("plugin_details.php?" )){
	
					String	plugin_name = link.substring( 19 );

					try{
						ResourceDownloader p_dl = ResourceDownloaderFactory.create( site_prefix + link );
					
						p_dl = ResourceDownloaderFactory.getRetryDownloader( p_dl, 5 );
					
						HTMLPage	plugin_page = HTMLPageFactory.loadPage( p_dl.download());
						
						System.out.println( plugin_page.getContent());
						
						dumpTables( "", plugin_page.getTables());
						
						// details.add( new SFPluginDetailsImpl( link.substring(8) ));
						
					}catch( Throwable e ){
						
						e.printStackTrace();
					}
				}
			}
			
		}catch( Throwable e ){
			
			e.printStackTrace();
		}
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
	
	public SFPluginDetails[]
	getPluginDetails()
	{
		return( plugin_details );
	}
}
