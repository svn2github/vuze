/*
 * Created on 03-May-2004
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

import java.net.URL;

import org.gudy.azureus2.plugins.utils.resourcedownloader.*;

public class 
ResourceDownloaderFactoryImpl
	implements ResourceDownloaderFactory
{
	protected static ResourceDownloaderFactoryImpl	singleton = new ResourceDownloaderFactoryImpl();
	
	public static ResourceDownloaderFactory
	getSingleton()
	{
		return( singleton );
	}
	
	public ResourceDownloader
	create(
		URL		url )
	{
		return( new ResourceDownloaderImpl( url ));
	}
	
	public ResourceDownloader
	create(
		ResourceDownloaderDelayedFactory		factory )
	{
		return( new ResourceDownloaderDelayedImpl( factory ));
	}
	
	public ResourceDownloader
	getRetryDownloader(
		ResourceDownloader		downloader,
		int						retry_count )
	{
		return( new ResourceDownloaderRetryImpl( downloader, retry_count ));
	}
	
	public ResourceDownloader
	getTimeoutDownloader(
		ResourceDownloader		downloader,
		int						timeout_millis )
	{
		return( new ResourceDownloaderTimeoutImpl( downloader, timeout_millis ));
	}
	
	public ResourceDownloader
	getAlternateDownloader(
		ResourceDownloader[]		downloaders )
	{
		return( new ResourceDownloaderAlternateImpl( downloaders, -1, false ));
	}
	
	public ResourceDownloader
	getAlternateDownloader(
		ResourceDownloader[]		downloaders,
		int							max_to_try )
	{
		return( new ResourceDownloaderAlternateImpl( downloaders, max_to_try, false ));
	}
	
	public ResourceDownloader
	getRandomDownloader(
		ResourceDownloader[]		downloaders )
	{
		return( new ResourceDownloaderAlternateImpl( downloaders, -1, true ));
	}
	
	public ResourceDownloader
	getRandomDownloader(
		ResourceDownloader[]		downloaders,
		int							max_to_try )
	{
		return( new ResourceDownloaderAlternateImpl( downloaders, max_to_try, true ));
	}
	
	public ResourceDownloader
	getMetaRefreshDownloader(
		ResourceDownloader			downloader )
	{
		return( new ResourceDownloaderMetaRefreshImpl( downloader ));
	}
}
