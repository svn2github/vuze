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
	
	private List					networks = new ArrayList();
	
	private CopyOnWriteList			listeners = new CopyOnWriteList();
	
	protected
	ContentNetworkManagerImpl()
	{
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
	
	public ContentNetwork 
	getContentNetwork(
		long id ) 
	{
		synchronized( this ){
			
			for ( int i=0;i<networks.size();i++ ){
					
				ContentNetwork network = (ContentNetwork)networks.get(i);
				
				if ( network.getID() == id ){
				
					return( network );
				}
			}
			
			return( null );
		}
	}
	
	protected void
	addNetwork(
		ContentNetwork		network )
	{
		synchronized( this ){
		
			for ( int i=0;i<networks.size();i++ ){
				
				if ( ((ContentNetwork)networks.get(i)).getID() == network.getID()){
					
					return;
				}
			}
			
			networks.add( network );
		}
		
		Iterator	 it = listeners.iterator();
		
		while( it.hasNext()){
			
			try{
				((ContentNetworkListener)it.next()).networkAdded( network );
				
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
		}
		
		Iterator	 it = listeners.iterator();
		
		while( it.hasNext()){
			
			try{
				((ContentNetworkListener)it.next()).networkRemoved( network );
				
			}catch( Throwable e ){
				
				Debug.out( e );
			}
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
