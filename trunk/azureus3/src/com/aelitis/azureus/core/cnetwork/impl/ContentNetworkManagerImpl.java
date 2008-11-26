/*
 * Created on Nov 20, 2008
 * Created by Paul Gardner
 * 
 * Copyright 2008 Vuze, Inc.  All rights reserved.
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


package com.aelitis.azureus.core.cnetwork.impl;

import java.io.IOException;
import java.util.*;

import org.gudy.azureus2.core3.util.Debug;
import org.gudy.azureus2.core3.util.FileUtil;

import com.aelitis.azureus.core.cnetwork.ContentNetwork;
import com.aelitis.azureus.core.cnetwork.ContentNetworkListener;
import com.aelitis.azureus.core.cnetwork.ContentNetworkManager;

import com.aelitis.azureus.core.util.CopyOnWriteList;
import com.aelitis.azureus.core.vuzefile.VuzeFile;
import com.aelitis.azureus.core.vuzefile.VuzeFileComponent;
import com.aelitis.azureus.core.vuzefile.VuzeFileHandler;
import com.aelitis.azureus.core.vuzefile.VuzeFileProcessor;

public class 
ContentNetworkManagerImpl 
	implements ContentNetworkManager
{
	private static final String CONFIG_FILE		= "cnetworks.config";
	
	private static ContentNetworkManagerImpl singleton = new ContentNetworkManagerImpl();
	
	public static void
	preInitialise()
	{
		VuzeFileHandler.getSingleton().addProcessor(
			new VuzeFileProcessor()
			{
				public void
				process(
					VuzeFile[]		files,
					int				expected_types )
				{
					for (int i=0;i<files.length;i++){
						
						VuzeFile	vf = files[i];
						
						VuzeFileComponent[] comps = vf.getComponents();
						
						for (int j=0;j<comps.length;j++){
							
							VuzeFileComponent comp = comps[j];
							
							if ( comp.getType() == VuzeFileComponent.COMP_TYPE_CONTENT_NETWORK ){
								
								try{								
									((ContentNetworkManagerImpl)getSingleton()).importNetwork( comp.getContent());
								
									comp.setProcessed();
									
								}catch( Throwable e ){
									
									Debug.out( e );
								}
							}
						}
					}
				}
			});		
	}
	
	public static ContentNetworkManager
	getSingleton()
	{
		return( singleton );
	}
	
	private List<ContentNetworkImpl>	networks = new ArrayList<ContentNetworkImpl>();
	
	private CopyOnWriteList			listeners = new CopyOnWriteList();
	
	protected
	ContentNetworkManagerImpl()
	{
		loadConfig();
		
		addNetwork( new ContentNetworkVuze());
	}
	
	protected void
	importNetwork(
		Map		content )
	
		throws IOException
	{
		ContentNetworkImpl network = ContentNetworkImpl.importFromBencodedMap( content );
		
		addNetwork( network );
	}
	
	public ContentNetwork[] 
	getContentNetworks() 
	{
		synchronized( this ){
			
			return((ContentNetwork[])networks.toArray( new ContentNetworkImpl[ networks.size()] ));
		}
	}
	
	public ContentNetworkImpl 
	getContentNetwork(
		long id ) 
	{
		synchronized( this ){
			
			for ( int i=0;i<networks.size();i++ ){
					
				ContentNetworkImpl network = networks.get(i);
				
				if ( network.getID() == id ){
				
					return( network );
				}
			}
			
			return( null );
		}
	}
	
	protected void
	addNetwork(
		ContentNetworkImpl		network )
	{
		boolean	replace = false;
		
		synchronized( this ){
		
			Iterator<ContentNetworkImpl> it = networks.iterator();
			
			while( it.hasNext()){
				
				ContentNetworkImpl existing_network = it.next();
				
				if ( existing_network.getID() == network.getID()){
					
					if ( network.getVersion() > existing_network.getVersion()){
						
						it.remove();
						
						break;
						
					}else{
						
						return;
					}
				}
			}

			networks.add( network );
		
				// we never persist the vuze network
			
			if ( network.getID() != ContentNetwork.CONTENT_NETWORK_VUZE ){
				
				saveConfig();
			}
		}
		
		Iterator<ContentNetworkListener>	 it = (Iterator<ContentNetworkListener>)listeners.iterator();
		
		while( it.hasNext()){
			
			try{
				if ( replace ){
					
					it.next().networkChanged( network );
					
				}else{
					
					it.next().networkAdded( network );
				}
			}catch( Throwable e ){
				
				Debug.out( e );
			}
		}
	}
	
	protected void
	removeNetwork(
		ContentNetwork		network )
	{
		synchronized( this ){

			if ( !networks.remove( network )){
				
				return;
			}
		
			saveConfig();
		}
		
		Iterator<ContentNetworkListener>	 it = (Iterator<ContentNetworkListener>)listeners.iterator();
		
		while( it.hasNext()){
			
			try{
				it.next().networkRemoved( network );
				
			}catch( Throwable e ){
				
				Debug.out( e );
			}
		}
	}
	
	protected void
	loadConfig()
	{
		if ( FileUtil.resilientConfigFileExists( CONFIG_FILE )){
			
			Map	map = FileUtil.readResilientConfigFile( CONFIG_FILE );
			
			List list = (List)map.get( "networks" );
			
			if ( list != null ){
				
				for (int i=0;i<list.size();i++){
					
					Map	cnet_map = (Map)list.get(i);
					
					try{
						
						ContentNetworkImpl cn = ContentNetworkImpl.importFromBencodedMap( cnet_map );
						
						if ( cn.getID() != ContentNetwork.CONTENT_NETWORK_VUZE ){
							
							networks.add( cn );
						}
					}catch( Throwable e ){
						
						Debug.out( "Failed to import " + cnet_map, e );
					}
				}
			}
		}
	}
	
	protected void
	saveConfig()
	{
		Map	map = new HashMap();
		
		List list = new ArrayList();
		
		map.put( "networks", list );
		
		Iterator<ContentNetworkImpl> it = networks.iterator();
		
		while( it.hasNext()){
			
			ContentNetworkImpl network = it.next();

			if ( network.getID() == ContentNetwork.CONTENT_NETWORK_VUZE ){
				
				continue;
			}
			
			Map	cnet_map = new HashMap();
			
			try{
				network.exportToBencodedMap( cnet_map );
			
				list.add( cnet_map );
				
			}catch( Throwable e ){
				
				Debug.out( "Failed to export " + network.getName(), e );
			}
		}
		
		if ( list.size() == 0 ){
			
			FileUtil.deleteResilientConfigFile( CONFIG_FILE );
			
		}else{
			
			FileUtil.writeResilientConfigFile( CONFIG_FILE, map );
		}
	}
	
	public void
	addListener(
		ContentNetworkListener		listener )
	{
		listeners.add( listener );
	}
	
	public void
	removeListener(
		ContentNetworkListener		listener )
	{
		listeners.remove( listener );
	}
}
