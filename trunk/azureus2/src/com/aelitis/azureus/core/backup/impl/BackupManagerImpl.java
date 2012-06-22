/*
 * Created on Jun 22, 2012
 * Created by Paul Gardner
 * 
 * Copyright 2012 Vuze, Inc.  All rights reserved.
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


package com.aelitis.azureus.core.backup.impl;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;

import org.gudy.azureus2.core3.config.COConfigurationManager;
import org.gudy.azureus2.core3.util.AERunnable;
import org.gudy.azureus2.core3.util.AEThread2;
import org.gudy.azureus2.core3.util.AsyncDispatcher;
import org.gudy.azureus2.core3.util.BDecoder;
import org.gudy.azureus2.core3.util.DisplayFormatters;
import org.gudy.azureus2.core3.util.FileUtil;
import org.gudy.azureus2.core3.util.SystemProperties;

import sun.awt.GlobalCursorManager;

import com.aelitis.azureus.core.AzureusCoreFactory;
import com.aelitis.azureus.core.backup.BackupManager;

public class 
BackupManagerImpl
	implements BackupManager
{
	private static BackupManagerImpl	singleton = new BackupManagerImpl();
	
	private AsyncDispatcher		dispatcher = new AsyncDispatcher();
	
	public static BackupManager
	getSingleton()
	{
		return( singleton );
	}
	
	public void
	backup(
		final File				parent_folder,
		final BackupListener	listener )
	{
		dispatcher.dispatch(
			new AERunnable()
			{
				public void
				runSupport()
				{
					backupSupport( parent_folder, listener );
				}
			});
	}
	
	private long[]
	copyFiles(
		File		from_file,
		File		to_file )
	
		throws Exception
	{
		long	total_files		= 0;
		long	total_copied 	= 0;

		if ( from_file.isDirectory()){
			
			if ( !to_file.mkdirs()){
			
				throw( new Exception( "Failed to create '" + to_file.getAbsolutePath() + "'" ));
			}
			
			File[] files = from_file.listFiles();
			
			for ( File f: files ){
				
				long[] temp = copyFiles( f, new File( to_file, f.getName()));
				
				total_files 	+= temp[0];
				total_copied	+= temp[1];
			}
		}else{
			
			if ( !FileUtil.copyFile( from_file, to_file )){
				
				throw( new Exception( "Failed to copy file '" + from_file + "'" ));
			}
			
			total_files++;
			
			total_copied = from_file.length();
		}
		
		return( new long[]{ total_files, total_copied });
	}
	
	private void
	backupSupport(
		File				parent_folder,
		BackupListener		listener )
	{
		try{
			String date_dir = new SimpleDateFormat( "yyyy-MM-dd" ).format( new Date());
			
			File backup_folder = null;
			
			for ( int i=0;i<100;i++){
				
				String test_dir = date_dir;
				
				if ( i > 0 ){
					
					test_dir = test_dir + "." + i;
				}
				
				File test_file = new File( parent_folder, test_dir );
				
				if ( !test_file.exists()){
					
					backup_folder = test_file;
					
					backup_folder.mkdirs();
					
					break;
				}
			}
			
			if ( backup_folder == null ){
				
				backup_folder = new File( parent_folder, date_dir );
			}
			
			listener.reportProgress( "Writing to " + backup_folder.getAbsolutePath());
			
			if ( !backup_folder.exists() && !backup_folder.mkdirs()){
				
				throw( new Exception( "Failed to create '" + backup_folder.getAbsolutePath() + "'" ));
			}
			
			listener.reportProgress( "Syncing current state" );
						
			AzureusCoreFactory.getSingleton().saveState();
			
			try{
				File user_dir = new File( SystemProperties.getUserPath());
				
				listener.reportProgress( "Reading configuration data from " + user_dir.getAbsolutePath());
				
				File[] user_files = user_dir.listFiles();
				
				for ( File f: user_files ){
					
					String	name = f.getName();
					
					if ( f.isDirectory()){
					
						if ( 	name.equals( "cache" ) ||
								name.equals( "tmp" ) ||
								name.equals( "logs" ) ||
								name.equals( "updates" ) ||
								name.equals( "debug")){
							
							continue;
						}
					}else if ( 	name.equals( ".lock" ) ||
								name.endsWith( ".log" )){
						
						continue;
					}
					
					File	dest_file = new File( backup_folder, name );
					
					listener.reportProgress( "Copying '" + name  + "' ..." );
					
					long[]	result = copyFiles( f, dest_file );
					
					String	result_str = DisplayFormatters.formatByteCountToKiBEtc( result[1] );
					
					if ( result[0] > 1 ){
						
						result_str = result[0] + " files, " + result_str;
					}
					
					listener.reportProgress( result_str );
				}
				
				listener.reportComplete();
				
			}catch( Throwable e ){
				
				throw( e );
			}
		}catch( Throwable e ){
			
			listener.reportError( e );
		}
	}
	
	public void
	restore(
		final File				backup_folder,
		final BackupListener	listener )
	{
		dispatcher.dispatch(
				new AERunnable()
				{
					public void
					runSupport()
					{
						restoreSupport( backup_folder, listener );
					}
				});
	}
	
	private void
	restoreSupport(
		File				backup_folder,
		BackupListener		listener )
	{
		try{
			listener.reportProgress( "Reading from " + backup_folder.getAbsolutePath());
			
			if ( !backup_folder.isDirectory()){
				
				throw( new Exception( "Location '" + backup_folder.getAbsolutePath() + "' must be a directory" ));
			}
			
			listener.reportProgress( "Analysing backup" );
			
			File	config = new File( backup_folder, "azureus.config" );
			
			if ( !config.exists()){
				
				throw( new Exception( "Invalid backup: azureus.config not found" ));
			}
			
			Map config_map = BDecoder.decode( FileUtil.readFileAsByteArray( config ));
			
			byte[]	temp = (byte[])config_map.get( "azureus.user.directory" );
			
			if ( temp == null ){
				
				throw( new Exception( "Invalid backup: azureus.config doesn't contain user directory details" ));
			}
			
			File current_user_dir	= new File( SystemProperties.getUserPath());
			File backup_user_dir 	= new File( new String( temp, "UTF-8" ));
			
			listener.reportProgress( "Current user directory:\t"  + current_user_dir.getAbsolutePath());
			listener.reportProgress( "Backup's user directory:\t" + backup_user_dir.getAbsolutePath());
			
			if ( current_user_dir.equals( backup_user_dir )){
				
				listener.reportProgress( "Directories are the same, no patching required" );
				
			}else{
				
				listener.reportProgress( "Directories are different, backup requires patching" );

				throw( new Exception( "Patching isn't implemented yet" ));
			}
			
			
			listener.reportProgress( "THIS ISN'T COMPLETELY IMPLEMENTED YET!" );
			
			listener.reportComplete();
			
		}catch( Throwable e ){
			
			listener.reportError( e );
		}
	}
}
