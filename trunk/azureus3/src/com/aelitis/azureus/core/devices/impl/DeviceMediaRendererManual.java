/*
 * Created on Jul 10, 2009
 * Created by Paul Gardner
 * 
 * Copyright 2009 Vuze, Inc.  All rights reserved.
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


package com.aelitis.azureus.core.devices.impl;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.gudy.azureus2.core3.util.AERunnable;
import org.gudy.azureus2.core3.util.AESemaphore;
import org.gudy.azureus2.core3.util.AEThread2;
import org.gudy.azureus2.core3.util.AsyncDispatcher;
import org.gudy.azureus2.core3.util.FileUtil;
import org.gudy.azureus2.core3.util.IndentWriter;

import com.aelitis.azureus.core.devices.DeviceManagerException;
import com.aelitis.azureus.core.devices.TranscodeException;
import com.aelitis.azureus.core.devices.TranscodeFile;
import com.aelitis.azureus.core.devices.TranscodeTargetListener;


public class 
DeviceMediaRendererManual 
	extends DeviceMediaRendererImpl
{
	private static final Object	COPY_ERROR_KEY 		= new Object();
	private static final Object	COPY_PENDING_KEY 	= new Object();

	private boolean				copy_outstanding;
	private boolean				copy_outstanding_set;
	private AEThread2			copy_thread;
	private AESemaphore			copy_sem = new AESemaphore( "Device:copy" );
	private AsyncDispatcher		async_dispatcher = new AsyncDispatcher( 5000 );

	protected
	DeviceMediaRendererManual(
		DeviceManagerImpl	_manager,
		String				_classification,
		boolean				_manual,
		String				_name )
	{
		super( _manager,_classification, _manual, _name );
	}
	
	protected
	DeviceMediaRendererManual(
		DeviceManagerImpl	_manager,
		Map					_map )
	
		throws IOException
	{
		super(_manager, _map );
	}
	
	protected void
	initialise()
	{
		super.initialise();
		
		if ( getPersistentBooleanProperty( PP_COPY_OUTSTANDING, false )){
		
			setCopyOutstanding();
		}
		
		addListener( 
			new TranscodeTargetListener()
			{
				public void
				fileAdded(
					TranscodeFile		file )
				{
					if ( file.isComplete() && !file.isCopiedToDevice()){
						
						setCopyOutstanding();
					}
				}
				
				public void
				fileChanged(
					TranscodeFile		file,
					int					type,
					Object				data )
				{
					if ( file.isComplete() && !file.isCopiedToDevice()){
						
						setCopyOutstanding();
					}
				}
				
				public void
				fileRemoved(
					TranscodeFile		file )
				{
					copy_sem.release();
				}
			});
	}
	
	public boolean 
	canAssociate() 
	{
		return( false );
	}
	
	public boolean
	canFilterFilesView() 
	{
		return( false );
	}
	
	public boolean
	isBrowsable()
	{
		return( false );
	}
	
	public boolean
	canCopyToFolder()
	{
		return( true );
	}
	
	public File
	getCopyToFolder()
	{
		String str = getPersistentStringProperty( PP_COPY_TO_FOLDER, null );
		
		if ( str == null ){
			
			return( null );
		}
		
		return( new File( str ));
	}
	
	public void
	setCopyToFolder(
		File		file )
	{
		setPersistentStringProperty( PP_COPY_TO_FOLDER, file.getAbsolutePath());
		
		if ( getAutoCopyToFolder()){
			
			setCopyOutstanding();
		}
	}
	
	public int
	getCopyToFolderPending()
	{
		synchronized( this ){
			
			if ( !copy_outstanding ){
				
				return( 0 );
			}
		}

		TranscodeFileImpl[] files = getFiles();
		
		int result = 0;
			
		for ( TranscodeFileImpl file: files ){

			if ( file.isComplete() && !file.isCopiedToDevice()){
				
				result++;
			}
		}
		
		return( result );
	}
	
	public boolean
	getAutoCopyToFolder()
	{
		return( getPersistentBooleanProperty( PP_AUTO_COPY, PP_AUTO_COPY_DEFAULT  ));
	}
		
	public void
	setAutoCopyToFolder(
		boolean		auto )
	{
		setPersistentBooleanProperty( PP_AUTO_COPY, auto );
			
		setCopyOutstanding();
	}
	
	public void 
	manualCopy() 
	
		throws DeviceManagerException 
	{
		if ( getAutoCopyToFolder()){
			
			throw( new DeviceManagerException( "Operation prohibited - auto copy enabled" ));
		}
		
		doCopy();
	}
	
	protected void
	setCopyOutstanding()
	{
		synchronized( this ){
			
			copy_outstanding_set = true;
			
			if ( copy_thread == null ){
				
				copy_thread = 
					new AEThread2( "Device:copier", true )
					{
						public void
						run()
						{
							performCopy();
						}
					};
									
				copy_thread.start();
			}
			
			copy_sem.release();
		}
	}
	
	protected void
	performCopy()
	{
		synchronized( this ){

			copy_outstanding = true;
		
			async_dispatcher.dispatch(
				new AERunnable()
				{
					public void
					runSupport()
					{
						setPersistentBooleanProperty( PP_COPY_OUTSTANDING, true );
					}
				});
		}
		
		while( true ){
			
			if ( copy_sem.reserve( 60*1000 )){
				
				while( copy_sem.reserveIfAvailable());
			}
						
			boolean	auto_copy = getAutoCopyToFolder();
			
			boolean	nothing_to_do = false;
			
			synchronized( this ){

				if ( !auto_copy ){
											
					copy_thread = null;
						
					nothing_to_do = true;
					
				}else{

					copy_outstanding_set = false;
				}
			}
			
			if ( nothing_to_do ){
				
				setError( COPY_ERROR_KEY, null );
				
				int pending = getCopyToFolderPending();
				
				if ( pending == 0 ){
					
					setInfo( COPY_PENDING_KEY, null );
					
				}else{
					
					setInfo( COPY_PENDING_KEY, pending + " files pending copy" );
				}
				return;
			}
			

			if ( doCopy()){
				
				break;
			}
		}
	}
	
	protected boolean
	doCopy()
	{
		setInfo( COPY_PENDING_KEY, null );
		
		File	copy_to = getCopyToFolder();
		
		List<TranscodeFileImpl>	to_copy = new ArrayList<TranscodeFileImpl>();

		boolean	borked = true;

		if ( copy_to == null ){
			
			setError( COPY_ERROR_KEY, "Copy to folder not set" );
			
		}else if ( !copy_to.exists()){
			
			setError( COPY_ERROR_KEY, "Copy to folder not found" );
			
		}else if ( !copy_to.canWrite()){
			
			setError( COPY_ERROR_KEY, "Copy to folder in not writable" );
			
		}else{
			
			setError( COPY_ERROR_KEY, null );
			
			borked = false;
		
			TranscodeFileImpl[] files = getFiles();
										
			for ( TranscodeFileImpl file: files ){
					
				if ( file.isComplete() && !file.isCopiedToDevice()){
					
					if ( file.getCopyToDeviceFails() < 3 ){
					
						to_copy.add( file );
						
					}else{
						
						borked = true;
					}
				}
			}
		
			if ( borked ){
			
				setError( COPY_ERROR_KEY, "Copy to folder failed" );
			}
		}
		
		synchronized( this ){

			if ( to_copy.size() == 0 && !copy_outstanding_set && !borked ){
					
				copy_outstanding = false;
				
				async_dispatcher.dispatch(
					new AERunnable()
					{
						public void
						runSupport()
						{
							setError( COPY_ERROR_KEY, null );

							setPersistentBooleanProperty( PP_COPY_OUTSTANDING, false );
						}
					});
				
				copy_thread = null;
				
				return( true );
			}
		}
		
		for ( TranscodeFileImpl transcode_file: to_copy ){
			
			try{
				File	file = transcode_file.getTargetFile().getFile();
				
				try{
					// copy the file!
					
					FileUtil.copyFile( file, new File( copy_to, file.getName()));
					
					log( "Copied file '" + file + ": to " + copy_to );
					
					transcode_file.setCopiedToDevice( true );
					
				}catch( Throwable e ){
					
					transcode_file.setCopyToDeviceFailed();
					
					log( "Failed to copy file " + file, e );
				}
			}catch( TranscodeException e ){

				// file has been deleted
			}
		}
		
		return( false );
	}
	
	protected void
	getDisplayProperties(
		List<String[]>	dp )
	{
		super.getDisplayProperties( dp );
		
		addDP( dp, "devices.copy.pending", copy_outstanding );
	}
	
	public void
	generate(
		IndentWriter		writer )
	{
		super.generate( writer );
		
		try{
			writer.indent();
	
			writer.println( "auto_copy=" + getAutoCopyToFolder() + ", copy_to=" + getCopyToFolder() + ", copy_os=" + copy_outstanding );
			
		}finally{
			
			writer.exdent();
		}
	}
}
