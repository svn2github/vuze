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

import org.gudy.azureus2.core3.util.*;
import org.gudy.azureus2.plugins.utils.resourcedownloader.*;

public class 
ResourceDownloaderRetryImpl 	
	extends 	ResourceDownloaderBaseImpl
	implements	ResourceDownloaderListener
{
	protected ResourceDownloaderBaseImpl		delegate;
	protected int								retry_count;
	
	protected boolean					cancelled;
	protected ResourceDownloader		current_downloader;
	protected int						done_count;
	protected Object					result;
	protected Semaphore					done_sem	= new Semaphore();
		
	public
	ResourceDownloaderRetryImpl(
		ResourceDownloader	_delegate,
		int					_retry_count )
	{
		delegate		= (ResourceDownloaderBaseImpl)_delegate;
		retry_count		= _retry_count;
	}
	
	public String
	getName()
	{
		return( delegate.getName() + ", retry=" + retry_count );
	}
	
	public ResourceDownloader
	getClone()
	{
		return( new ResourceDownloaderRetryImpl( delegate.getClone(), retry_count ));
	}
	
	public InputStream
	download()
	
		throws ResourceDownloaderException
	{
		asyncDownload();
		
		done_sem.reserve();
		
		if ( result instanceof InputStream ){
			
			return((InputStream)result);
		}
		
		throw((ResourceDownloaderException)result);
	}
	
	public synchronized void
	asyncDownload()
	{
		if ( done_count == retry_count || cancelled ){
			
			done_sem.release();
			
			informFailed((ResourceDownloaderException)result);
			
		}else{
		
			done_count++;
			
			informActivity( "download attempt " + done_count + " of " + retry_count );
			
			current_downloader = delegate.getClone();
			
			current_downloader.addListener( this );
			
			current_downloader.asyncDownload();
		}
	}
	
	public synchronized void
	cancel()
	{
		result	= new ResourceDownloaderException( "Download cancelled");
		
		cancelled	= true;
		
		informFailed((ResourceDownloaderException)result );
		
		done_sem.release();
		
		if ( current_downloader != null ){
			
			current_downloader.cancel();
		}
	}	
	
	public boolean
	completed(
		ResourceDownloader	downloader,
		InputStream			data )
	{
		if ( informComplete( data )){
			
			result	= data;
			
			done_sem.release();
			
			return( true );
		}
		
		return( false );
	}
	
	public void
	failed(
		ResourceDownloader			downloader,
		ResourceDownloaderException e )
	{
		result		= e;
		
		asyncDownload();
	}
}
