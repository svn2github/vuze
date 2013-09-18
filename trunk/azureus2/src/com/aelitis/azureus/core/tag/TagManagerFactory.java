/*
 * Created on Mar 20, 2013
 * Created by Paul Gardner
 * 
 * Copyright 2013 Azureus Software, Inc.  All rights reserved.
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


package com.aelitis.azureus.core.tag;

import org.gudy.azureus2.core3.util.Debug;

public class 
TagManagerFactory 
{	
	final private static Class<TagManager> impl_class; 

	static{
	
		String impl = System.getProperty( "az.factory.tagmanager.impl", "com.aelitis.azureus.core.tag.impl.TagManagerImpl" );
		
		Class<TagManager> temp = null;
		
		try{
			temp = (Class<TagManager>)TagManagerFactory.class.getClassLoader().loadClass( impl );
			
		}catch( Throwable e ){
			
			Debug.out( "Failed to load TagManagerFactory class: " + impl );
		}
		
		impl_class = temp;
	}

	private static TagManager	singleton;

	public static TagManager
	getTagManager()
	{
		synchronized( TagManagerFactory.class ){
			
			if ( singleton != null ){
				
				return( singleton );
			}
		
			if ( impl_class == null ){
				
				throw( new RuntimeException( "No Implementation" ));
			}
			
			try{
				singleton = (TagManager)impl_class.getMethod( "getSingleton" ).invoke( null, (Object[])null );
			
				return( singleton );
				
			}catch( Throwable e ){
				
				throw( new RuntimeException( "No Implementation", e ));
			}
		}
	}
}
