/*
 * Created on 25-Apr-2004
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

package org.gudy.azureus2.pluginsimpl.local.utils.resourcedownloader;

/**
 * @author parg
 *
 */

import java.io.*;
import java.net.*;
import java.util.Properties;

import org.gudy.azureus2.plugins.*;
import org.gudy.azureus2.plugins.utils.resourcedownloader.*;

public class 
Test
	implements ResourceDownloaderListener, Plugin
{
	public void
	initialize(
		PluginInterface pi )
	{
		try{
			ResourceDownloaderFactory	factory = pi.getUtilities().getResourceDownloaderFactory();
			
			//ResourceDownloader rd1 = factory.create( new URL("http://localhost:6967/"));
			//ResourceDownloader rd2 = factory.create( new URL("http://www.microsoft.com/sdsdsd"));
			
			//ResourceDownloader[]	rds = { rd1, rd2 };
			
			//ResourceDownloader rd = factory.getAlternateDownloader( rds );
			
			ResourceDownloader rd = factory.create( new URL("http://66.90.75.92/suprnova//torrents/1822/DivX511-exe.torrent" ));
			
			rd = factory.getTorrentDownloader( rd, false );
						
			rd.addListener( this );
			
			try{
				rd.asyncDownload();
				
			}catch( Throwable e ){
				
				e.printStackTrace();
			}
						
		}catch( Throwable e ){
			
			e.printStackTrace();
		}
	}
	
	public void
	reportPercentComplete(
		ResourceDownloader	downloader,
		int					percentage )
	{
		System.out.println( "percent = " + percentage );
	}
	
	public void
	reportActivity(
		ResourceDownloader	downloader,
		String				activity )
	{
		System.out.println( "activity = " + activity );
	}
	
	public boolean
	completed(
		ResourceDownloader	downloader,
		InputStream			data )
	{
		System.out.println( "Completed" );
		
		return( true );
	}
	
	public void
	failed(
		ResourceDownloader			downloader,
		ResourceDownloaderException e )
	{
		System.out.println( "Failed");
		
		e.printStackTrace();
	}
	
	public static void
	main(
		String[]	args )
	{
		try{
			PluginManager.registerPlugin( Test.class );
										
			PluginManager.startAzureus( PluginManager.UI_SWT, new Properties() );
			
		}catch( Throwable e ){
			
			e.printStackTrace();
		}
	}
}
