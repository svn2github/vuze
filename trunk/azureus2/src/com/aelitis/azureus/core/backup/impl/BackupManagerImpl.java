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
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;

import org.gudy.azureus2.core3.config.COConfigurationListener;
import org.gudy.azureus2.core3.config.COConfigurationManager;
import org.gudy.azureus2.core3.config.ParameterListener;
import org.gudy.azureus2.core3.logging.LogAlert;
import org.gudy.azureus2.core3.logging.Logger;
import org.gudy.azureus2.core3.util.AERunnable;
import org.gudy.azureus2.core3.util.AETemporaryFileHandler;
import org.gudy.azureus2.core3.util.AsyncDispatcher;
import org.gudy.azureus2.core3.util.BDecoder;
import org.gudy.azureus2.core3.util.Debug;
import org.gudy.azureus2.core3.util.DisplayFormatters;
import org.gudy.azureus2.core3.util.FileUtil;
import org.gudy.azureus2.core3.util.SimpleTimer;
import org.gudy.azureus2.core3.util.SystemProperties;
import org.gudy.azureus2.core3.util.SystemTime;
import org.gudy.azureus2.core3.util.TimeFormatter;
import org.gudy.azureus2.core3.util.TimerEvent;
import org.gudy.azureus2.core3.util.TimerEventPerformer;
import org.gudy.azureus2.plugins.PluginInterface;
import org.gudy.azureus2.plugins.update.UpdateInstaller;


import com.aelitis.azureus.core.AzureusCore;
import com.aelitis.azureus.core.AzureusCoreLifecycleAdapter;
import com.aelitis.azureus.core.backup.BackupManager;

public class 
BackupManagerImpl
	implements BackupManager
{
	private static BackupManagerImpl	singleton;
		
	public static synchronized BackupManager
	getSingleton(
		AzureusCore		core )
	{
		if ( singleton == null ){
			
			singleton = new BackupManagerImpl( core );
		}
		
		return( singleton );
	}
	
	private AzureusCore			core;
	
	private AsyncDispatcher		dispatcher = new AsyncDispatcher();

	private boolean		first_schedule_check = true;
	private TimerEvent	backup_event;
	private long		last_auto_backup;
	
	
	private volatile boolean	closing;
	
	private 
	BackupManagerImpl(
		AzureusCore		_core )
	{
		core	= _core;
				
		COConfigurationManager.addAndFireParameterListeners(
			new String[]{
				"br.backup.auto.enable",
				"br.backup.auto.everydays",
				"br.backup.auto.retain",
			},
			new ParameterListener()
			{
				private COConfigurationListener	save_listener;
				
				private Object	lock = this;
				
				public void 
				parameterChanged(
					String parameter ) 
				{
					synchronized( lock ){
						
						if ( save_listener == null ){
							
							save_listener = 
								new COConfigurationListener()
								{
									public void
									configurationSaved()
									{
										checkSchedule();
										
										COConfigurationManager.removeListener( this );
										
										synchronized( lock ){
											
											if ( save_listener == this ){
										
												save_listener = null;
											}
										}
									}
								};
						
								COConfigurationManager.addListener( save_listener );
						}
					}
				}
			});
		
		checkSchedule();

		core.addLifecycleListener(
			new AzureusCoreLifecycleAdapter()
			{
				@Override
				public void
				stopping(
					AzureusCore		core )
				{
					closing = true;
				}
			});
	}
	
	private void
	checkSchedule()
	{
		checkSchedule( null, false );
	}
	
	private void
	checkSchedule(
		final BackupListener 	listener,
		boolean					force )
	{
		boolean	enabled = COConfigurationManager.getBooleanParameter( "br.backup.auto.enable" );
		
		boolean	do_backup = false;
		
		synchronized( this ){
			
			if ( backup_event != null ){
				
				backup_event.cancel();
				
				backup_event = null;
			}
			
			if ( first_schedule_check ){
				
				first_schedule_check = false;
				
				if ( !force ){
					
					if ( enabled ){
					
						backup_event = 
							SimpleTimer.addEvent(
								"BM:startup",
								SystemTime.getCurrentTime() + 5*60*1000,
								new TimerEventPerformer()
								{
									public void 
									perform(
										TimerEvent event )
									{
										checkSchedule();
									}
								});
					}
					
					return;
				}
			}
			
			if ( !enabled ){
				
				System.out.println( "Auto backup is disabled" );
				
				if ( listener != null ){
					
					listener.reportError( new Exception( "Auto-backup not enabled" ));
				}
				
				return;
			}
			
			long	now_utc = SystemTime.getCurrentTime();
			
			int offset = TimeZone.getDefault().getOffset( now_utc );
			
			long	now_local = now_utc + offset;
			
			long	DAY = 24*60*60*1000L;
			
			long	local_day_index = now_local/DAY;
			
			long	last_auto_backup_day = COConfigurationManager.getLongParameter( "br.backup.auto.last_backup_day", 0 );
			
			if ( last_auto_backup_day > local_day_index ){
				
				last_auto_backup_day = local_day_index;
			}
			
			long	backup_every_days = COConfigurationManager.getLongParameter( "br.backup.auto.everydays" );
			
			backup_every_days = Math.max( 1, backup_every_days );
			
			long	utc_next_backup =  ( last_auto_backup_day + backup_every_days ) * DAY;
			
			long	time_to_next_backup = utc_next_backup - now_local;
			
			if ( time_to_next_backup <= 0 || force ){
				
				if ( now_utc - last_auto_backup >= 4*60*60*1000 || force ){
				
					do_backup = true;
					
					last_auto_backup	= now_utc;

					COConfigurationManager.setParameter( "br.backup.auto.last_backup_day", local_day_index );

				}else{
					
					time_to_next_backup	= 4*60*60*1000;
				}
			}
			
			if ( !do_backup ){
				
				time_to_next_backup = Math.max( time_to_next_backup, 60*1000 );
				
				System.out.println( "Scheduling next backup in " + TimeFormatter.format( time_to_next_backup/1000 ));
				
				backup_event = 
					SimpleTimer.addEvent(
						"BM:auto",
						now_utc + time_to_next_backup,
						new TimerEventPerformer()
						{
							public void 
							perform(
								TimerEvent event )
							{
								checkSchedule();
							}
						});
			}
		}
		
		if ( do_backup ){
			
			String backup_dir = COConfigurationManager.getStringParameter( "br.backup.auto.dir", "" );
						
			System.out.println( "Auto backup starting: folder=" + backup_dir );
			
			final File target_dir = new File( backup_dir );
			
			backup(
				target_dir,
				new BackupListener()
				{
					public boolean
					reportProgress(
						String		str )
					{
						if ( listener != null ){
							
							try{
								return( listener.reportProgress( str ));
								
							}catch( Throwable e ){
								
								Debug.out( e );
							}
						}
						
						return( true );
					}
					
					public void
					reportComplete()
					{
						try{
							System.out.println( "Auto backup completed" );
							
							COConfigurationManager.save();
														
							Logger.log(
								new LogAlert(
									true,
									LogAlert.AT_INFORMATION,
									"Backup completed at " + new Date()));
							
							int	backup_retain = COConfigurationManager.getIntParameter( "br.backup.auto.retain" );

							backup_retain = Math.max( 1, backup_retain );
							
							File[] backups = target_dir.listFiles();
							
							List<File>	backup_dirs = new ArrayList<File>();
							
							for ( File f: backups ){
								
								if ( f.isDirectory() && getBackupDirTime( f ) > 0 ){
									
									File	test_file = new File( f, "azureus.config" );
									
									if ( test_file.exists()){
										
										backup_dirs.add( f );
									}
								}
							}
							
							Collections.sort(
								backup_dirs,
								new Comparator<File>()
								{
									public int 
									compare(
										File o1, 
										File o2 )
									{
										long	t1 = getBackupDirTime( o1 );
										long	t2 = getBackupDirTime( o2 );
										
										long res = t2 - t1;
										
										if ( res < 0 ){
											return( -1 );
										}else if ( res > 0 ){
											return( 1 );
										}else{
											Debug.out( "hmm: " + o1 + "/" + o2 );
											
											return( 0 );
										}
									}
								});
							
							for ( int i=backup_retain;i< backup_dirs.size();i++){
								
								File f = backup_dirs.get( i );
								
								System.out.println( "Deleting old backup: " + f );
								
								FileUtil.recursiveDeleteNoCheck( f );
							}
						}finally{
							
							if ( listener != null ){
								
								try{
									
									listener.reportComplete();
									
								}catch( Throwable e ){
									
									Debug.out( e );
								}
							}
							
							checkSchedule();
						}
					}
					
					public void
					reportError(
						Throwable 	error )
					{
						try{
							System.out.println( "Auto backup failed" );
																					
							Logger.log(
									new LogAlert(
										true,
										LogAlert.AT_ERROR,
										"Backup failed at " + new Date(),
										error ));
						}finally{
							
							if ( listener != null ){
								
								try{
									
									listener.reportError( error );
									
								}catch( Throwable e ){
									
									Debug.out( e );
								}
							}

							checkSchedule();
						}
					}
				});
				
		}else{
			
			if ( listener != null ){
				
				listener.reportError( new Exception( "Backup not scheduled to run now" ));
			}
		}
	}
	
	public void
	runAutoBackup(
		BackupListener			listener )
	{
		checkSchedule( listener, true );
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
	
	private void
	checkClosing()
	
		throws Exception
	{
		if ( closing ){
			
			throw( new Exception( "operation cancelled, app is closing" ));
		}
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
				
				checkClosing();
				
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
	
	private long
	getBackupDirTime(
		File		file )
	{
		String	name = file.getName();
		
		int	pos = name.indexOf( "." );
		
		long	suffix = 0;
		
		if ( pos != -1 ){
		
			try{
				suffix = Integer.parseInt( name.substring( pos+1 ));
				
				name = name.substring( 0, pos );
				
			}catch( Throwable e ){
				
				return( -1 );
			}
		}
		
		try{
			return( new SimpleDateFormat( "yyyy-MM-dd" ).parse( name ).getTime() + suffix );
			
		}catch( Throwable e ){
			
			return( -1 );
		}
	}
	
	private void
	backupSupport(
		File						parent_folder,
		final BackupListener		_listener )
	{		
		try{
			String date_dir = new SimpleDateFormat( "yyyy-MM-dd" ).format( new Date());
			
			File 		backup_folder 	= null;
			boolean		ok 				= false;
			
			try{
				checkClosing();

				if ( 	parent_folder.getName().length() == 0 ||
						!parent_folder.isDirectory()){
					
					throw( new Exception( "Backup folder '" + parent_folder + "' is invalid" ));
				}
				
				BackupListener listener = new 
					BackupListener()
					{
						public boolean
						reportProgress(
							String		str )
						{
							if ( !_listener.reportProgress( str )){
								
								throw( new RuntimeException( "Operation abandoned by listener" ));
							}
							
							return( true );
						}
						
						public void
						reportComplete()
						{
							_listener.reportComplete();
						}
						
						public void
						reportError(
							Throwable 	error )
						{
							_listener.reportError( error );
						}
					};
					
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
							
				core.saveState();
								
				try{
					File user_dir = new File( SystemProperties.getUserPath());
					
					listener.reportProgress( "Reading configuration data from " + user_dir.getAbsolutePath());
					
					File[] user_files = user_dir.listFiles();
					
					for ( File f: user_files ){
						
						checkClosing();

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
									name.equals( "update.properties" ) ||								
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
					
					ok	= true;

				}catch( Throwable e ){
					
					throw( e );
				}
			}finally{
				
				if ( !ok ){
					
					if ( backup_folder != null ){
						
						FileUtil.recursiveDeleteNoCheck( backup_folder );
					}
				}
			}
		}catch( Throwable e ){
			
			_listener.reportError( e );
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
	addActions(
		UpdateInstaller	installer,
		File			source,
		File			target )
	
		throws Exception
	{
		if ( source.isDirectory()){
			
			File[]	files = source.listFiles();
			
			for ( File f: files ){
				
				addActions( installer, f, new File( target, f.getName()));
			}
		}else{
			
			installer.addMoveAction(
					source.getAbsolutePath(),
					target.getAbsolutePath());
		}
	}
	
	private void
	restoreSupport(
		File				backup_folder,
		BackupListener		listener )
	{
		try{
			UpdateInstaller installer 	= null;
			File 			temp_dir 	= null;
			
			boolean	ok = false;
			
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
				
				temp_dir = AETemporaryFileHandler.createTempDir();
				
				if ( current_user_dir.equals( backup_user_dir )){
					
					listener.reportProgress( "Directories are the same, no patching required" );
					
					PluginInterface pi = core.getPluginManager().getDefaultPluginInterface();
					
					installer = pi.getUpdateManager().createInstaller();
				
					File[] files = backup_folder.listFiles();
					
					for ( File f: files ){
						
						File source = new File( temp_dir, f.getName());
						
						listener.reportProgress( "Creating restore action for '" + f.getName() + "'" );
	
						copyFiles( f, source );
						
						File target = new File( current_user_dir, f.getName());
						
						addActions( installer, source, target );
					}
					
					listener.reportProgress( "Restore action creation complete, restart required to complete the operation" );
	
				}else{
					
					listener.reportProgress( "Directories are different, backup requires patching" );
	
					throw( new Exception( "Patching isn't implemented yet" ));
				}
							
				listener.reportComplete();
			
				ok = true;
				
			}finally{
				
				if ( !ok ){
				
					if ( installer != null ){
						
						installer.destroy();
					}
					
					if ( temp_dir != null ){
						
						FileUtil.recursiveDeleteNoCheck( temp_dir );
					}
				}
			}
		}catch( Throwable e ){
			
			listener.reportError( e );
		}
	}
}
