/*
 * Created on Apr 16, 2004
 * Created by Paul Gardner
 * Copyright (C) 2004, 2005, 2006 Aelitis, All Rights Reserved.
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
 * AELITIS, SAS au capital de 46,603.30 euros
 * 8 Allee Lenotre, La Grille Royale, 78600 Le Mesnil le Roi, France.
 *
 */

package org.gudy.azureus2.platform.win32.access.impl;

/**
 * @author parg
 *
 */

import java.util.*;

import org.gudy.azureus2.platform.win32.access.*;
import org.gudy.azureus2.core3.util.*;

public class 
AEWin32AccessImpl
	implements AEWin32Access, AEWin32AccessCallback
{
	protected static AEWin32AccessImpl	singleton;
	protected static AEMonitor			class_mon	= new AEMonitor( "AEWin32Access" );
	
	public static AEWin32Access
	getSingleton()
	{
		try{
			class_mon.enter();
		
			if ( singleton == null ){
				
				singleton = new AEWin32AccessImpl();
			}
			
			return( singleton );
			
		}finally{
			
			class_mon.exit();
		}
	}
	
	private List	listeners = new ArrayList();
	
	protected
	AEWin32AccessImpl()
	{
		if ( isEnabled()){
			
			AEWin32AccessInterface.load( this );
		}
	}
	
	public boolean
	isEnabled()
	{
		return( AEWin32AccessInterface.isEnabled());
	}
	
	public long
	windowsMessage(
		int		msg,
		int		param1,
		long	param2 )
	{
		for (int i=0;i<listeners.size();i++){
			
			try{
				((AEWin32AccessListener)listeners.get(i)).eventOccurred( msg );
				
			}catch( Throwable e ){
				
				Debug.printStackTrace(e);
			}
		}
		
		return( -1 );
	}
	
	public String
	getVersion()
	{
		return( AEWin32AccessInterface.getVersion());		
	}
	
	public String
	readStringValue(
		int		type,		
		String	subkey,
		String	value_name )
	
		throws AEWin32AccessException
	{
		return( AEWin32AccessInterface.readStringValue( type, subkey, value_name ));
	}
	
	public void
	writeStringValue(
		int		type,		
		String	subkey,
		String	value_name,
		String	value_value )
	
		throws AEWin32AccessException
	{
		AEWin32AccessInterface.writeStringValue( type, subkey, value_name, value_value );
	}

	
	public int
	readWordValue(
		int		type,		
		String	subkey,
		String	value_name )
	
		throws AEWin32AccessException
	{
		return( AEWin32AccessInterface.readWordValue( type, subkey, value_name ));
	}
	
	public void
	writeWordValue(
		int		type,		
		String	subkey,
		String	value_name,
		int		value_value )
	
		throws AEWin32AccessException
	{
		AEWin32AccessInterface.writeWordValue( type, subkey, value_name, value_value );
	}
	
	
	public void
	deleteKey(
		int		type,
		String	subkey )
	
		throws AEWin32AccessException
	{
		deleteKey( type, subkey, false );
	}
	
	public void
	deleteKey(
		int		type,
		String	subkey,
		boolean	recursive )
	
		throws AEWin32AccessException
	{
		AEWin32AccessInterface.deleteKey( type, subkey, recursive );
	}
	
	public void
	deleteValue(
		int			type,
		String		subkey,
		String		value_name )
	
		throws AEWin32AccessException
	{
		AEWin32AccessInterface.deleteValue( type, subkey, value_name );	
	}
	
	public String
	getUserAppData()
	
		throws AEWin32AccessException
	{
		String	app_data_key	= "software\\microsoft\\windows\\currentversion\\explorer\\shell folders";
		String	app_data_name 	= "appdata";
		
		return(	readStringValue(
					HKEY_CURRENT_USER,
					app_data_key,
					app_data_name ));

	}
	
	public String
	getApplicationInstallDir(
		String	app_name )
		
		throws AEWin32AccessException
	{
		return(	readStringValue(
				HKEY_LOCAL_MACHINE,
				"software\\" + app_name,
				null ));		
	}
	
	public void
	createProcess(
		String		command_line,
		boolean		inherit_handles )
	
		throws AEWin32AccessException
	{
		AEWin32AccessInterface.createProcess( command_line, inherit_handles );
	}	
	
	public void
	moveToRecycleBin(
		String	file_name )
	
		throws AEWin32AccessException
	{
		AEWin32AccessInterface.moveToRecycleBin( file_name );
	}
	
	public void
    copyFilePermissions(
		String	from_file_name,
		String	to_file_name )
	
		throws AEWin32AccessException
	{
		AEWin32AccessInterface.copyPermission( from_file_name, to_file_name ); 
	}
	
	public boolean
	testNativeAvailability(
		String	name )
	
		throws AEWin32AccessException
	{
		return( AEWin32AccessInterface.testNativeAvailability( name ));
	}
	
	public void
    addListener(
    	AEWin32AccessListener		listener )
    {
    	listeners.add( listener );
    }
    
    public void
    removeListener(
    	AEWin32AccessListener		listener )
    {
    	listeners.remove( listener );
    }
}
