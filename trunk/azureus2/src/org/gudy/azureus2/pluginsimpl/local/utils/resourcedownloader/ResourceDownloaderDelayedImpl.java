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

import org.gudy.azureus2.plugins.utils.resourcedownloader.*;

public class 
ResourceDownloaderDelayedImpl
	extends ResourceDownloaderBaseImpl
{
	protected ResourceDownloaderDelayedFactory		factory;
	
	protected ResourceDownloaderBaseImpl		delegate;
		
	protected
	ResourceDownloaderDelayedImpl(
		ResourceDownloaderBaseImpl				_parent,
		ResourceDownloaderDelayedFactory		_factory )
	{
		super( _parent );
		
		factory	= _factory;
	}
	
	protected synchronized void
	getDelegate()
	{
		if ( delegate == null ){
			
			try{
				delegate	= (ResourceDownloaderBaseImpl)factory.create();
				
				delegate.setParent( this );
				
			}catch(  ResourceDownloaderException e ){
				
				delegate = new ResourceDownloaderErrorImpl( this, e );
			}
		}
	}
	
	public String
	getName()
	{
		if ( delegate == null ){
			
			return( "<...>" );
		}
		
		return( delegate.getName());
	}
	
	public ResourceDownloader
	getClone(
		ResourceDownloaderBaseImpl	parent )
	{		
		return( new ResourceDownloaderDelayedImpl( parent, factory ));
	}
	
	public InputStream
	download()
	
		throws ResourceDownloaderException
	{
		getDelegate();
		
		return( delegate.download());	
	}
	
	
	public void
	asyncDownload()
	{
		getDelegate();
		
		delegate.asyncDownload();	
	}
	
	public long
	getSize()
	
		throws ResourceDownloaderException
	{	
		getDelegate();
		
		return( delegate.getSize());	
	}
	
	public void
	cancel()
	{
		getDelegate();
		
		delegate.cancel();		
	}
	
	public void
	reportActivity(
		String				activity )
	{
		getDelegate();
		
		delegate.reportActivity( activity );		
	}
	
	public void
	addListener(
		ResourceDownloaderListener	l )
	{
		getDelegate();
		
		delegate.addListener(l);	
	}
	
	public void
	removeListener(
		ResourceDownloaderListener	l )
	{
		getDelegate();
		
		delegate.removeListener(l);		
	}
}
