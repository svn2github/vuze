/*
 * Created on 18-Feb-2005
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

package org.gudy.azureus2.pluginsimpl.local.ddb;

import org.gudy.azureus2.core3.util.AEMonitor;
import org.gudy.azureus2.plugins.PluginInterface;
import org.gudy.azureus2.plugins.ddb.*;
import org.gudy.azureus2.pluginsimpl.local.download.DownloadManagerImpl;

import com.aelitis.azureus.core.AzureusCore;
import com.aelitis.azureus.plugins.dht.DHTPlugin;

/**
 * @author parg
 *
 */

public class 
DDBaseImpl 
	implements DistributedDatabase
{
	private static DDBaseImpl	singleton;
	
	protected static AEMonitor		class_mon	= new AEMonitor( "DDBaseImpl:class");
	
	public static DistributedDatabase
	getSingleton(
		AzureusCore	azureus_core )
	{
		try{
			class_mon.enter();
	
			if ( singleton == null ){
				
				singleton = new DDBaseImpl( azureus_core );
			}
		}finally{
			
			class_mon.exit();
		}
		
		return( singleton );
	}
	
	
	private DHTPlugin		dht;
	
	
	protected
	DDBaseImpl(
		AzureusCore	azureus_core )
	{
		PluginInterface dht_pi = 
			azureus_core.getPluginManager().getPluginInterfaceByClass(
						DHTPlugin.class );
		
		if ( dht_pi != null ){
			
			dht = (DHTPlugin)dht_pi.getPlugin();
		}
	}
	
	public boolean
	isAvailable()
	{
		if ( dht == null ){
			
			return( false );
		}
		
		return( dht.isEnabled());
	}
	
	public DistributedDatabaseKey
	createKey(
		Object			key )
	
		throws DistributedDatabaseException
	{
		return( new DDBKeyImpl( key ));
	}
	
	public DistributedDatabaseValue
	createValue(
		Object			value )
	
		throws DistributedDatabaseException
	{
		return( new DDBValueImpl( value ));
	}
	
	public void
	write(
		DistributedDatabaseListener		listener,
		DistributedDatabaseKey			key,
		DistributedDatabaseValue		value )
	
		throws DistributedDatabaseException
	{
		
	}
		
	public void
	read(
		DistributedDatabaseListener		listener,
		DistributedDatabaseKey			key )
	
		throws DistributedDatabaseException
	{
		
	}
	
	public void
	delete(
		DistributedDatabaseListener		listener,
		DistributedDatabaseKey			key )
	
		throws DistributedDatabaseException
	{
		
	}
	
	public void
	addTransferHandler(
		DistributedDatabaseTransferType		type,
		DistributedDatabaseTransferHandler	handler )
	
		throws DistributedDatabaseException
	{
	
	}
}