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
	
	protected File				az_exe;
	
	protected
	PlatformManagerImpl(
		AEWin32Access		_access )
	
		throws PlatformManagerException
	{
		access	= _access;
	
		try{
			String az_home = access.getAzureusInstallDir();
			
			az_exe = new File( az_home + File.separator + "Azureus.exe" ).getAbsoluteFile();
		
		}catch( Throwable e ){
				
			throw( new PlatformManagerException( "Failed to read Azureus install location"));
			
		}
		
		if ( !az_exe.exists()){
			
			throw( new PlatformManagerException( "Azureus.exe not found"));
		}
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
		try{
			String	test = 
				access.readStringValue( 	
					AEWin32Access.HKEY_CLASSES_ROOT,
					"BitTorrent\\shell\\open\\command",
					"" );
			
			return( test.equals( "\"" + az_exe.toString() + "\" \"%1\"" ));
			
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
			try{
				access.deleteKey( 	
					AEWin32Access.HKEY_CURRENT_USER,
					"Software\\Microsoft\\Windows\\CurrentVersion\\Explorer\\FileExts\\.torrent",
					true );
				
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
					az_exe.toString() + ",0" );	// this was 1 but it seems it should be 0
			
			access.writeStringValue( 	
					AEWin32Access.HKEY_CLASSES_ROOT,
					"BitTorrent\\shell\\open\\command",
					"",
					"\"" + az_exe.toString() + "\" \"%1\"" );
					
			access.writeStringValue( 	
					AEWin32Access.HKEY_CLASSES_ROOT,
					"BitTorrent\\Content Type" ,
					"",
					"application/x-bittorrent" );
			
		}catch( Throwable e ){
			
			throw( new PlatformManagerException( "Failed to write registry details", e ));
		}
	}
}
