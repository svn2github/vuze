/*
 * Created on 18-Apr-2004
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

package org.gudy.azureus2.platform.win32;

/**
 * @author parg
 *
 */

import java.io.File;

import org.gudy.azureus2.platform.*;
import org.gudy.azureus2.platform.win32.access.*;
import org.gudy.azureus2.core3.logging.*;


public class 
PlatformManagerImpl
	implements PlatformManager
{
	protected static boolean					init_tried;
	
	protected static PlatformManagerImpl		singleton;
	
	public static synchronized PlatformManagerImpl
	getSingleton()
	
		throws PlatformManagerException	
	{
		if ( singleton == null && !init_tried ){
			
			init_tried	= true;
			
			try{
				singleton	= new PlatformManagerImpl( AEWin32Manager.getAccessor());
				
			}catch( Throwable e ){
				
				LGLogger.log( "Win32Platform: failed to initialise", e );
				
				if ( e instanceof PlatformManagerException ){
					
					throw((PlatformManagerException)e);
				}
				
				throw( new PlatformManagerException( "Win32Platform: failed to initialise", e ));
			}
		}
		
		return( singleton );
	}
	
	protected AEWin32Access		access;
	
	protected File				_az_exe;
	
	protected
	PlatformManagerImpl(
		AEWin32Access		_access )
	
		throws PlatformManagerException
	{
		access	= _access;
	}
	
	protected File
	getAureusEXELocation()
		throws PlatformManagerException
	{
		if ( _az_exe == null ){
			
			String az_home;
			
			try{
				az_home = access.getAzureusInstallDir();
				
				_az_exe = new File( az_home + File.separator + "Azureus.exe" ).getAbsoluteFile();
			
			}catch( Throwable e ){
					
				throw( new PlatformManagerException( "Failed to read Azureus install location from the registry, please re-install"));
				
			}
			
			if ( !_az_exe.exists()){
				
				_az_exe = null;
				
				throw( new PlatformManagerException( "Azureus.exe not found in " + az_home + ", please re-install"));
			}
		}
		
		return( _az_exe );
	}
	
	public int
	getPlatformType()
	{
		return( PT_WINDOWS );
	}
	
	public String
	getUserDataDirectory()
	
		throws PlatformManagerException
	{
		try{
			return( access.getUserAppData());
			
		}catch( Throwable e ){
			
			throw( new PlatformManagerException( "Failed to read registry details", e ));
		}		
	}
	
	public boolean
	isApplicationRegistered()
	
		throws PlatformManagerException
	{
		String	az_exe_str = getAureusEXELocation().toString();
		
		try{
			String	test1 = 
				access.readStringValue( 	
					AEWin32Access.HKEY_CLASSES_ROOT,
					"BitTorrent\\shell\\open\\command",
					"" );
			
			if ( !test1.equals( "\"" + az_exe_str + "\" \"%1\"" )){
				
				return( false );
			}
			
				// MRU list is just that, to remove the "always open with" we need to kill
				// the "application" entry, if it exists
			
			try{
				String	always_open_with = 
					access.readStringValue( 
						AEWin32Access.HKEY_CURRENT_USER,
						"Software\\Microsoft\\Windows\\CurrentVersion\\Explorer\\FileExts\\.torrent",
						"Application" );
				
				//System.out.println( "mru_list = " + mru_list );

				if ( always_open_with.length() > 0 ){
				
					// AZ is default so if this entry exists it denotes another (non-AZ) app
					
					return( false );
				}
			}catch( Throwable e ){
				
				// e.printStackTrace();
				
				// failure means things are OK
			}
			
			/*
			try{
				String	mru_list = 
					access.readStringValue( 
						AEWin32Access.HKEY_CURRENT_USER,
						"Software\\Microsoft\\Windows\\CurrentVersion\\Explorer\\FileExts\\.torrent\\OpenWithList",
						"MRUList" );
				
				//System.out.println( "mru_list = " + mru_list );

				if ( mru_list.length() > 0 ){
				
					String	mru = 
						access.readStringValue( 
							AEWin32Access.HKEY_CURRENT_USER,
							"Software\\Microsoft\\Windows\\CurrentVersion\\Explorer\\FileExts\\.torrent\\OpenWithList",
							"" + mru_list.charAt(0) );
					
					//System.out.println( "mru = " + mru );
					
					return( mru.equalsIgnoreCase("Azureus.exe"));
				}
			}catch( Throwable e ){
				
				// e.printStackTrace();
				
				// failure means things are OK
			}
			*/
			
			return( true );
			
		}catch( Throwable e ){
			
			if ( 	e.getMessage() == null || 
					e.getMessage().indexOf("RegOpenKey failed") == -1 ){
				
				e.printStackTrace();
			}

			return( false );
		}
	}
	
	public void
	registerApplication()
	
		throws PlatformManagerException
	{
		// 	WriteRegStr HKCR ".torrent" "" "BitTorrent"
		// 	WriteRegStr HKCR "BitTorrent" "" "Bittorrent File"
		// 	WriteRegStr HKCR "BitTorrent\shell" "" "open"
		// 	WriteRegStr HKCR "BitTorrent\DefaultIcon" "" $INSTDIR\Azureus.exe,1
		// 	WriteRegStr HKCR "BitTorrent\shell\open\command" "" '"$INSTDIR\Azureus.exe" "%1"'
		// 	WriteRegStr HKCR "BitTorrent\Content Type" "" "application/x-bittorrent"
		

		try{
			String	az_exe_string	= getAureusEXELocation().toString();
			
			try{
		
				access.deleteValue( 	
					AEWin32Access.HKEY_CURRENT_USER,
					"Software\\Microsoft\\Windows\\CurrentVersion\\Explorer\\FileExts\\.torrent",
					"Application" );
				
			}catch( Throwable e ){
				
				e.printStackTrace();
			}
			
			try{
				access.deleteKey( 	
					AEWin32Access.HKEY_CLASSES_ROOT,
					".torrent" );
				
			}catch( Throwable e ){
				
				e.printStackTrace();
			}
			
			try{
				access.deleteKey( 	
					AEWin32Access.HKEY_CLASSES_ROOT,
					"BitTorrent",
					true );
				
			}catch( Throwable e ){
				
				e.printStackTrace();
			}

			access.writeStringValue( 	
					AEWin32Access.HKEY_CLASSES_ROOT,
					".torrent",
					"",
					"BitTorrent" );
		
			access.writeStringValue( 	
					AEWin32Access.HKEY_CLASSES_ROOT,
					"BitTorrent",
					"",
					"Bittorrent File" );
			
			access.writeStringValue( 	
					AEWin32Access.HKEY_CLASSES_ROOT,
					"BitTorrent\\shell",
					"",
					"open" );
			
			access.writeStringValue( 	
					AEWin32Access.HKEY_CLASSES_ROOT,
					"BitTorrent\\DefaultIcon",
					"",
					az_exe_string + ",0" );	// this was 1 but it seems it should be 0
			
			access.writeStringValue( 	
					AEWin32Access.HKEY_CLASSES_ROOT,
					"BitTorrent\\shell\\open\\command",
					"",
					"\"" + az_exe_string + "\" \"%1\"" );
					
			access.writeStringValue( 	
					AEWin32Access.HKEY_CLASSES_ROOT,
					"BitTorrent\\Content Type" ,
					"",
					"application/x-bittorrent" );
			
		}catch( PlatformManagerException e ){
			
			throw(e );
			
		}catch( Throwable e ){
			
			throw( new PlatformManagerException( "Failed to write registry details", e ));
		}
	}
}
