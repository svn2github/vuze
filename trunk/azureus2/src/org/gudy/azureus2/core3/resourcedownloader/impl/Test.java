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

package org.gudy.azureus2.core3.resourcedownloader.impl;

/**
 * @author parg
 *
 */

import java.io.*;

import org.gudy.azureus2.core3.resourcedownloader.*;

public class 
Test
	implements ResourceDownloaderListener
{
	protected
	Test()
	{
		ResourceDownloader rd1 = ResourceDownloaderFactory.create( "http://localhost:6967/");
		ResourceDownloader rd2 = ResourceDownloaderFactory.create( "http://localhost:6968/");
		
		ResourceDownloader[]	rds = { rd1, rd2 };
		
		ResourceDownloader rd = ResourceDownloaderFactory.getAlternateDownloader( rds );
		
		rd = ResourceDownloaderFactory.getRetryDownloader( rd, 5 );
		
		rd = ResourceDownloaderFactory.getTimeoutDownloader( rd, 20000 );
		
		rd.addListener( this );
		
		try{
			rd.asyncDownload();
			
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
	
	public void
	completed(
		ResourceDownloader	downloader,
		InputStream			data )
	{
		System.out.println( "Completed" );
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
		new Test();
	}
}
