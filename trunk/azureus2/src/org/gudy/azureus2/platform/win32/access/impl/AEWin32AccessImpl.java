/*
 * Created on Apr 16, 2004
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

package org.gudy.azureus2.platform.win32.access.impl;

/**
 * @author parg
 *
 */

import org.gudy.azureus2.platform.win32.access.*;

public class 
AEWin32AccessImpl
	implements AEWin32Access
{
	protected static AEWin32AccessImpl	singleton;
	
	public static synchronized AEWin32Access
	getSingleton()
	{
		if ( singleton == null ){
			
			singleton = new AEWin32AccessImpl();
		}
		
		return( singleton );
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
	getAzureusInstallDir()
		
		throws AEWin32AccessException
	{
		return(	readStringValue(
				HKEY_LOCAL_MACHINE,
				"software\\azureus",
				null ));		
	}
}
