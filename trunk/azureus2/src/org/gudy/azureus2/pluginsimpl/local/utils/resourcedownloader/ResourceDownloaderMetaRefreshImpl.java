/*
 * Created on 21-May-2004
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
import java.net.URL;

import org.gudy.azureus2.core3.util.*;
import org.gudy.azureus2.core3.html.*;
import org.gudy.azureus2.plugins.utils.resourcedownloader.*;

public class 
ResourceDownloaderMetaRefreshImpl 	
	extends 	ResourceDownloaderBaseImpl
	implements	ResourceDownloaderListener
{
	public static final int	MAX_FOLLOWS = 1;
	
	protected ResourceDownloaderBaseImpl		delegate;
	protected ResourceDownloaderBaseImpl		current_delegate;
	
	protected long						size	= -2;
	
	protected boolean					cancelled;
	protected ResourceDownloader		current_downloader;
	protected Object					result;
	protected int						done_count;
	protected AESemaphore				done_sem	= new AESemaphore("RDMetaRefresh");
			
	public
	ResourceDownloaderMetaRefreshImpl(
		ResourceDownloaderBaseImpl	_parent,
		ResourceDownloader			_delegate )
	{
		super( _parent );
		
		delegate		= (ResourceDownloaderBaseImpl)_delegate;
		
		delegate.setParent( this );
		
		current_delegate	= delegate;
	}
	
	public String
	getName()
	{
		return( delegate.getName() + ": meta-refresh" );
	}
	
	public long
	getSize()
	
		throws ResourceDownloaderException
	{	
		if ( size == -2 ){
			
			try{
				size = getSizeSupport();
				
			}finally{
				
				if ( size == -2 ){
					
					size = -1;
				}
			}
		}
		
		return( size );
	}
	
	protected long
	getSizeSupport()
	
		throws ResourceDownloaderException
	{
		try{
			ResourceDownloader	x = delegate.getClone( this );
			
			addReportListener( x );
			
			HTMLPage	page = HTMLPageFactory.loadPage( x.download());
			
			URL	redirect = page.getMetaRefreshURL();
	
			if ( redirect == null ){
				
				ResourceDownloader c = delegate.getClone( this );
				
				addReportListener( c );
				
				return( c.getSize());
			}else{
				
				ResourceDownloaderURLImpl c =  new ResourceDownloaderURLImpl( getParent(), redirect );
				
				addReportListener( c );
				
				return( c.getSize());
			}
		}catch( HTMLException e ){
			
			throw( new ResourceDownloaderException( "getSize failed", e ));
		}
	}	
	
	protected void
	setSize(
		long	l )
	{
		size	= l;

	}
	
	public ResourceDownloader
	getClone(
		ResourceDownloaderBaseImpl	parent )
	{
		ResourceDownloaderMetaRefreshImpl c = new ResourceDownloaderMetaRefreshImpl( parent, delegate.getClone( this ));
		
		c.setSize( size );
		
		return( c );
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
	
	public void
	asyncDownload()
	{
		try{
			this_mon.enter();
		
			if ( cancelled ){
				
				done_sem.release();
				
				informFailed((ResourceDownloaderException)result);
				
			}else{
			
				done_count++;
							
				current_downloader = current_delegate.getClone( this );
				
				informActivity( getLogIndent() + "Downloading: " + getName());
	
				current_downloader.addListener( this );
				
				current_downloader.asyncDownload();
			}
		}finally{
			
			this_mon.exit();
		}
	}
	
	public void
	cancel()
	{
		try{
			this_mon.enter();
		
			result	= new ResourceDownloaderException( "Download cancelled");
			
			cancelled	= true;
			
			informFailed((ResourceDownloaderException)result );
			
			done_sem.release();
			
			if ( current_downloader != null ){
				
				current_downloader.cancel();
			}
		}finally{
			
			this_mon.exit();
		}
	}	
	
	public boolean
	completed(
		ResourceDownloader	downloader,
		InputStream			data )
	{
		boolean	complete = false;
		
		try{
			if ( done_count == 1 ){
				
					// assumption is that there is a refresh tag
				
				boolean	marked = false;
				
				if ( data instanceof ByteArrayInputStream){
				
					data.mark(data.available());
					
					marked	= true;
				}
				
					// leave file open if marked so we can recover
				
				HTMLPage	page = HTMLPageFactory.loadPage( data, !marked );
				
				URL	redirect = page.getMetaRefreshURL();
				
				if ( redirect == null ){
					
					if ( !marked ){
						
						failed( downloader, new ResourceDownloaderException( "meta refresh tag not found and input stream not recoverable"));
						
					}else{
					
						data.reset();
					
						complete	= true;
					}
					
				}else{
				
					current_delegate = new ResourceDownloaderURLImpl( this, redirect );
					
					// informActivity( "meta-refresh -> " + current_delegate.getName());
					
					asyncDownload();
				}
				
				if ( marked && !complete){
					
					data.close();
				}
			}else{
				
				complete = true;
			}
			
			if ( complete ){
			
				if ( informComplete( data )){
					
					result	= data;
					
					done_sem.release();
				}
			}
		}catch( Throwable e ){
			
			failed( downloader, new ResourceDownloaderException("meta-refresh processing fails", e ));
		}
		
		return( true );
	}
	
	public void
	failed(
		ResourceDownloader			downloader,
		ResourceDownloaderException e )
	{
		result		= e;
		
		done_sem.release();

		informFailed(e);
	}
}
